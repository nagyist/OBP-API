package code.bankconnectors

import code.api.Constant._
import code.api.util.APIUtil._
import code.api.util.ErrorMessages._
import code.api.util._
import code.bankconnectors.LocalMappedConnector.{logger, _}
import code.management.ImporterAPI.ImporterTransaction
import code.model.dataAccess.{BankAccountRouting, MappedBank, MappedBankAccount}
import code.transaction.MappedTransaction
import code.transactionrequests._
import code.util.Helper
import code.util.Helper._
import com.openbankproject.commons.ExecutionContext.Implicits.global
import com.openbankproject.commons.model._
import com.openbankproject.commons.model.enums.TransactionRequestStatus
import com.openbankproject.commons.model.enums.TransactionRequestTypes
import com.openbankproject.commons.model.enums.PaymentServiceTypes
import net.liftweb.common._
import net.liftweb.json.Serialization.write
import net.liftweb.json.{NoTypeHints, Serialization}
import net.liftweb.mapper.By
import net.liftweb.util.Helpers
import net.liftweb.util.Helpers.tryo

import java.util.Date
import scala.collection.immutable.List
import scala.concurrent._
import scala.language.postfixOps
import scala.util.Random



//Try to keep LocalMappedConnector smaller, so put OBP internal code here. these methods will not be exposed to CBS side.
object LocalMappedConnectorInternal extends MdcLoggable {
  
  def createTransactionRequestBGInternal(
    initiator: User,
    paymentServiceType: PaymentServiceTypes,
    transactionRequestType: TransactionRequestTypes,
    transactionRequestBody: BerlinGroupTransactionRequestCommonBodyJson,
    callContext: Option[CallContext]
  ): Future[(Full[TransactionRequestBGV1], Option[CallContext])] = {
    for {
      transDetailsSerialized <- NewStyle.function.tryons(s"$UnknownError Can not serialize in request Json ", 400, callContext) {
        write(transactionRequestBody)(Serialization.formats(NoTypeHints))
      }

      //for Berlin Group, the account routing address is the IBAN.
      fromAccountIban = transactionRequestBody.debtorAccount.iban
      toAccountIban = transactionRequestBody.creditorAccount.iban

      (fromAccount, callContext) <- NewStyle.function.getBankAccountByIban(fromAccountIban, callContext)
      (ibanChecker, callContext) <- NewStyle.function.validateAndCheckIbanNumber(toAccountIban, callContext)
      _ <- Helper.booleanToFuture(invalidIban, cc = callContext) {
        ibanChecker.isValid == true
      }
      (toAccount, callContext) <- NewStyle.function.getToBankAccountByIban(toAccountIban, callContext)

      viewId = ViewId(SYSTEM_INITIATE_PAYMENTS_BERLIN_GROUP_VIEW_ID)
      fromBankIdAccountId = BankIdAccountId(fromAccount.bankId, fromAccount.accountId)
      view <- NewStyle.function.checkAccountAccessAndGetView(viewId, fromBankIdAccountId, Full(initiator), callContext)
      _ <- Helper.booleanToFuture(InsufficientAuthorisationToCreateTransactionRequest, cc = callContext) {
        view.canAddTransactionRequestToAnyAccount
      }

      (paymentLimit, callContext) <- Connector.connector.vend.getPaymentLimit(
        fromAccount.bankId.value,
        fromAccount.accountId.value,
        viewId.value,
        transactionRequestType.toString,
        transactionRequestBody.instructedAmount.currency,
        initiator.userId,
        initiator.name,
        callContext
      ) map { i =>
        (unboxFullOrFail(i._1, callContext, s"$InvalidConnectorResponseForGetPaymentLimit ", 400), i._2)
      }

      paymentLimitAmount <- NewStyle.function.tryons(s"$InvalidConnectorResponseForGetPaymentLimit. payment limit amount ${paymentLimit.amount} not convertible to number", 400, callContext) {
        BigDecimal(paymentLimit.amount)
      }

      //We already checked the value in API level.
      transactionAmount = BigDecimal(transactionRequestBody.instructedAmount.amount)

      _ <- Helper.booleanToFuture(s"$InvalidJsonValue the payment amount is over the payment limit($paymentLimit)", 400, callContext) {
        transactionAmount <= paymentLimitAmount
      }

      // Prevent default value for transaction request type (at least).
      _ <- Helper.booleanToFuture(s"From Account Currency is ${fromAccount.currency}, but Requested instructedAmount.currency is: ${transactionRequestBody.instructedAmount.currency}", cc = callContext) {
        transactionRequestBody.instructedAmount.currency == fromAccount.currency
      }

      // Get the threshold for a challenge. i.e. over what value do we require an out of Band security challenge to be sent?
      (challengeThreshold, callContext) <- Connector.connector.vend.getChallengeThreshold(
        fromAccount.bankId.value,
        fromAccount.accountId.value,
        viewId.value,
        transactionRequestType.toString,
        transactionRequestBody.instructedAmount.currency,
        initiator.userId, initiator.name,
        callContext
      ) map { i =>
        (unboxFullOrFail(i._1, callContext, s"$InvalidConnectorResponseForGetChallengeThreshold ", 400), i._2)
      }
      challengeThresholdAmount <- NewStyle.function.tryons(s"$InvalidConnectorResponseForGetChallengeThreshold. challengeThreshold amount ${challengeThreshold.amount} not convertible to number", 400, callContext) {
        BigDecimal(challengeThreshold.amount)
      }
      status <- getStatus(
        challengeThresholdAmount,
        transactionAmount,
        TransactionRequestType(transactionRequestType.toString)
      )
      (chargeLevel, callContext) <- Connector.connector.vend.getChargeLevelC2(
        BankId(fromAccount.bankId.value),
        AccountId(fromAccount.accountId.value),
        viewId,
        initiator.userId,
        initiator.name,
        transactionRequestType.toString,
        transactionRequestBody.instructedAmount.currency,
        transactionRequestBody.instructedAmount.amount,
        toAccount.accountRoutings,
        Nil,
        callContext
      ) map { i =>
        (unboxFullOrFail(i._1, callContext, s"$InvalidConnectorResponseForGetChargeLevel ", 400), i._2)
      }

      chargeLevelAmount <- NewStyle.function.tryons(s"$InvalidNumber chargeLevel.amount: ${chargeLevel.amount} can not be transferred to decimal !", 400, callContext) {
        BigDecimal(chargeLevel.amount)
      }

      (chargeValue, callContext) <- NewStyle.function.getChargeValue(chargeLevelAmount, transactionAmount, callContext)
      
      charge = TransactionRequestCharge("Total charges for completed transaction", AmountOfMoney(transactionRequestBody.instructedAmount.currency, chargeValue))

      // Always create a new Transaction Request
      transactionRequest <- Future {
        val transactionRequest = TransactionRequests.transactionRequestProvider.vend.createTransactionRequestImpl210(
          TransactionRequestId(generateUUID()),
          TransactionRequestType(transactionRequestType.toString),
          fromAccount,
          toAccount,
          TransactionRequestCommonBodyJSONCommons(
            transactionRequestBody.instructedAmount,
            ""
          ),
          transDetailsSerialized,
          status.toString,
          charge,
          "", // chargePolicy is not used in BG so far.
          Some(paymentServiceType.toString),
          Some(transactionRequestBody)
        )
        transactionRequest
      } map {
        unboxFullOrFail(_, callContext, s"$InvalidConnectorResponseForCreateTransactionRequestImpl210")
      }

      // If no challenge necessary, create Transaction immediately and put in data store and object to return
      (transactionRequest, callContext) <- status match {
        case TransactionRequestStatus.COMPLETED =>
          for {
            (createdTransactionId, callContext) <- NewStyle.function.makePaymentv210(
              fromAccount,
              toAccount,
              transactionRequest.id,
              TransactionRequestCommonBodyJSONCommons(
                transactionRequestBody.instructedAmount,
                "" //BG no description so far
              ),
              transactionAmount,
              "", //BG no description so far
              TransactionRequestType(transactionRequestType.toString),
              "", // chargePolicy is not used in BG so far.,
              callContext
            )
            //set challenge to null, otherwise it have the default value "challenge": {"id": "","allowed_attempts": 0,"challenge_type": ""}
            transactionRequest <- Future(transactionRequest.copy(challenge = null))

            //save transaction_id into database
            _ <- saveTransactionRequestTransaction(transactionRequest.id, createdTransactionId,callContext)
            //update transaction_id field for variable 'transactionRequest'
            transactionRequest <- Future(transactionRequest.copy(transaction_ids = createdTransactionId.value))

          } yield {
            logger.debug(s"createTransactionRequestv210.createdTransactionId return: $transactionRequest")
            (transactionRequest, callContext)
          }
        case _ => Future(transactionRequest, callContext)
      }
    } yield {
      logger.debug(transactionRequest)
      (Full(TransactionRequestBGV1(transactionRequest.id, transactionRequest.status)), callContext)
    }
  }



  /*
    Bank account creation
   */

  //creates a bank account (if it doesn't exist) and creates a bank (if it doesn't exist)
  //again assume national identifier is unique
  def createBankAndAccount(
    bankName: String,
    bankNationalIdentifier: String,
    accountNumber: String,
    accountType: String,
    accountLabel: String,
    currency: String,
    accountHolderName: String,
    branchId: String,
    accountRoutingScheme: String,
    accountRoutingAddress: String,
    callContext: Option[CallContext]
  ): Box[(Bank, BankAccount)] = {
    //don't require and exact match on the name, just the identifier
    val bank = MappedBank.find(By(MappedBank.national_identifier, bankNationalIdentifier)) match {
      case Full(b) =>
        logger.debug(s"bank with id ${b.bankId} and national identifier ${b.nationalIdentifier} found")
        b
      case _ =>
        logger.debug(s"creating bank with national identifier $bankNationalIdentifier")
        //TODO: need to handle the case where generatePermalink returns a permalink that is already used for another bank
        MappedBank.create
          .permalink(Helper.generatePermalink(bankName))
          .fullBankName(bankName)
          .shortBankName(bankName)
          .national_identifier(bankNationalIdentifier)
          .saveMe()
    }

    //TODO: pass in currency as a parameter?
    val account = createAccountIfNotExisting(
      bank.bankId,
      AccountId(APIUtil.generateUUID()),
      accountNumber, accountType,
      accountLabel, currency,
      0L, accountHolderName,
      "",
      List.empty
    )

    account.map(account => (bank, account))
  }


  def createAccountIfNotExisting(
    bankId: BankId,
    accountId: AccountId,
    accountNumber: String,
    accountType: String,
    accountLabel: String,
    currency: String,
    balanceInSmallestCurrencyUnits: Long,
    accountHolderName: String,
    branchId: String,
    accountRoutings: List[AccountRouting],
  ): Box[BankAccount] = {
    getBankAccountLegacy(bankId, accountId, None).map(_._1) match {
      case Full(a) =>
        logger.debug(s"account with id $accountId at bank with id $bankId already exists. No need to create a new one.")
        Full(a)
      case _ => tryo {
        accountRoutings.map(accountRouting =>
          BankAccountRouting.create
            .BankId(bankId.value)
            .AccountId(accountId.value)
            .AccountRoutingScheme(accountRouting.scheme)
            .AccountRoutingAddress(accountRouting.address)
            .saveMe()
        )
        MappedBankAccount.create
          .bank(bankId.value)
          .theAccountId(accountId.value)
          .accountNumber(accountNumber)
          .kind(accountType)
          .accountLabel(accountLabel)
          .accountCurrency(currency.toUpperCase)
          .accountBalance(balanceInSmallestCurrencyUnits)
          .holder(accountHolderName)
          .mBranchId(branchId)
          .saveMe()
      }
    }
  }


  //transaction import api uses bank national identifiers to uniquely indentify banks,
  //which is unfortunate as theoretically the national identifier is unique to a bank within
  //one country
  private def getBankByNationalIdentifier(nationalIdentifier: String): Box[Bank] = {
    MappedBank.find(By(MappedBank.national_identifier, nationalIdentifier))
  }

  private def getAccountByNumber(bankId: BankId, number: String): Box[BankAccount] = {
    MappedBankAccount.find(
      By(MappedBankAccount.bank, bankId.value),
      By(MappedBankAccount.accountNumber, number))
  }

  private val bigDecimalFailureHandler: PartialFunction[Throwable, Unit] = {
    case ex: NumberFormatException => {
      logger.warn(s"could not convert amount to a BigDecimal: $ex")
    }
  }

  //used by transaction import api call to check for duplicates
  def getMatchingTransactionCount(bankNationalIdentifier: String, accountNumber: String, amount: String, completed: Date, otherAccountHolder: String): Box[Int] = {
    //we need to convert from the legacy bankNationalIdentifier to BankId, and from the legacy accountNumber to AccountId
    val count = for {
      bankId <- getBankByNationalIdentifier(bankNationalIdentifier).map(_.bankId)
      account <- getAccountByNumber(bankId, accountNumber)
      amountAsBigDecimal <- tryo(bigDecimalFailureHandler)(BigDecimal(amount))
    } yield {

      val amountInSmallestCurrencyUnits =
        Helper.convertToSmallestCurrencyUnits(amountAsBigDecimal, account.currency)

      MappedTransaction.count(
        By(MappedTransaction.bank, bankId.value),
        By(MappedTransaction.account, account.accountId.value),
        By(MappedTransaction.amount, amountInSmallestCurrencyUnits),
        By(MappedTransaction.tFinishDate, completed),
        By(MappedTransaction.counterpartyAccountHolder, otherAccountHolder))
    }

    //icky
    Full(count.map(_.toInt) getOrElse 0)
  }
  
  
  def createImportedTransaction(transaction: ImporterTransaction): Box[Transaction] = {
    //we need to convert from the legacy bankNationalIdentifier to BankId, and from the legacy accountNumber to AccountId
    val obpTransaction = transaction.obp_transaction
    val thisAccount = obpTransaction.this_account
    val nationalIdentifier = thisAccount.bank.national_identifier
    val accountNumber = thisAccount.number
    for {
      bank <- getBankByNationalIdentifier(transaction.obp_transaction.this_account.bank.national_identifier) ?~!
        s"No bank found with national identifier $nationalIdentifier"
      bankId = bank.bankId
      account <- getAccountByNumber(bankId, accountNumber)
      details = obpTransaction.details
      amountAsBigDecimal <- tryo(bigDecimalFailureHandler)(BigDecimal(details.value.amount))
      newBalanceAsBigDecimal <- tryo(bigDecimalFailureHandler)(BigDecimal(details.new_balance.amount))
      amountInSmallestCurrencyUnits = Helper.convertToSmallestCurrencyUnits(amountAsBigDecimal, account.currency)
      newBalanceInSmallestCurrencyUnits = Helper.convertToSmallestCurrencyUnits(newBalanceAsBigDecimal, account.currency)
      otherAccount = obpTransaction.other_account
      mappedTransaction = MappedTransaction.create
        .bank(bankId.value)
        .account(account.accountId.value)
        .transactionType(details.kind)
        .amount(amountInSmallestCurrencyUnits)
        .newAccountBalance(newBalanceInSmallestCurrencyUnits)
        .currency(account.currency)
        .tStartDate(details.posted.`$dt`)
        .tFinishDate(details.completed.`$dt`)
        .description(details.label)
        .counterpartyAccountNumber(otherAccount.number)
        .counterpartyAccountHolder(otherAccount.holder)
        .counterpartyAccountKind(otherAccount.kind)
        .counterpartyNationalId(otherAccount.bank.national_identifier)
        .counterpartyBankName(otherAccount.bank.name)
        .counterpartyIban(otherAccount.bank.IBAN)
        .saveMe()
      transaction <- mappedTransaction.toTransaction(account)
    } yield transaction
  }

  //used by the transaction import api
  def updateAccountBalance(bankId: BankId, accountId: AccountId, newBalance: BigDecimal): Box[Boolean] = {
    //this will be Full(true) if everything went well
    val result = for {
      (bank, _) <- getBankLegacy(bankId, None)
      account <- getBankAccountLegacy(bankId, accountId, None).map(_._1).map(_.asInstanceOf[MappedBankAccount])
    } yield {
      account.accountBalance(Helper.convertToSmallestCurrencyUnits(newBalance, account.currency)).save
      setBankAccountLastUpdated(bank.nationalIdentifier, account.number, Helpers.now).openOrThrowException(attemptedToOpenAnEmptyBox)
    }

    Full(result.getOrElse(false))
  }

  def setBankAccountLastUpdated(bankNationalIdentifier: String, accountNumber: String, updateDate: Date): Box[Boolean] = {
    val result = for {
      bankId <- getBankByNationalIdentifier(bankNationalIdentifier).map(_.bankId)
      account <- getAccountByNumber(bankId, accountNumber)
    } yield {
      val acc = MappedBankAccount.find(
        By(MappedBankAccount.bank, bankId.value),
        By(MappedBankAccount.theAccountId, account.accountId.value)
      )
      acc match {
        case Full(a) => a.accountLastUpdate(updateDate).save
        case _ => logger.warn("can't set bank account.lastUpdated because the account was not found"); false
      }
    }
    Full(result.getOrElse(false))
  }


  //creates a bank account for an existing bank, with the appropriate values set. Can fail if the bank doesn't exist
  def createSandboxBankAccount(
    bankId: BankId,
    accountId: AccountId,
    accountNumber: String,
    accountType: String,
    accountLabel: String,
    currency: String,
    initialBalance: BigDecimal,
    accountHolderName: String,
    branchId: String,
    accountRoutings: List[AccountRouting]
  ): Box[BankAccount] = {

    for {
      (_, _) <- getBankLegacy(bankId, None) //bank is not really used, but doing this will ensure account creations fails if the bank doesn't
      balanceInSmallestCurrencyUnits = Helper.convertToSmallestCurrencyUnits(initialBalance, currency)
      account <- LocalMappedConnectorInternal.createAccountIfNotExisting (
        bankId,
        accountId,
        accountNumber,
        accountType,
        accountLabel,
        currency,
        balanceInSmallestCurrencyUnits,
        accountHolderName,
        branchId,
        accountRoutings
      ) ?~! AccountRoutingAlreadyExist
    } yield {
      account
    }

  }

  //generates an unused account number and then creates the sandbox account using that number
  @deprecated("This return Box, not a future, try to use @createBankAccount instead. ", "10-05-2019")
  def createBankAccountLegacy(
    bankId: BankId,
    accountId: AccountId,
    accountType: String,
    accountLabel: String,
    currency: String,
    initialBalance: BigDecimal,
    accountHolderName: String,
    branchId: String,
    accountRoutings: List[AccountRouting]
  ): Box[BankAccount] = {
    val uniqueAccountNumber = {
      def exists(number: String) = LocalMappedConnectorInternal.accountExists(bankId, number).openOrThrowException(attemptedToOpenAnEmptyBox)

      def appendUntilOkay(number: String): String = {
        val newNumber = number + Random.nextInt(10)
        if (!exists(newNumber)) newNumber
        else appendUntilOkay(newNumber)
      }

      //generates a random 8 digit account number
      val firstTry = (Random.nextDouble() * 10E8).toInt.toString
      appendUntilOkay(firstTry)
    }

    LocalMappedConnectorInternal.createSandboxBankAccount(
      bankId,
      accountId,
      uniqueAccountNumber,
      accountType,
      accountLabel,
      currency,
      initialBalance,
      accountHolderName,
      branchId: String, //added field in V220
      accountRoutings
    )

  }

  //for sandbox use -> allows us to check if we can generate a new test account with the given number
  def accountExists(bankId : BankId, accountNumber : String) : Box[Boolean] = {
    Full(MappedBankAccount.count(
      By(MappedBankAccount.bank, bankId.value),
      By(MappedBankAccount.accountNumber, accountNumber)) > 0)
  }
  
}
