/**
Open Bank Project - API
Copyright (C) 2011-2019, TESOBE GmbH.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Email: contact@tesobe.com
TESOBE GmbH.
Osloer Strasse 16/17
Berlin 13359, Germany

This product includes software developed at
TESOBE (http://www.tesobe.com/)

  */
package code.snippet

import code.accountholders.AccountHolders
import code.api.RequestHeader
import code.api.berlin.group.v1_3.JSONFactory_BERLIN_GROUP_1_3.{ConsentAccessAccountsJson, ConsentAccessJson, GetConsentResponseJson, createGetConsentResponseJson}
import code.api.util.ErrorMessages.ConsentNotFound
import code.api.util._
import code.api.v3_1_0.APIMethods310
import code.api.v5_0_0.APIMethods500
import code.api.v5_1_0.APIMethods510
import code.consent.{ConsentStatus, Consents, MappedConsent}
import code.consumer.Consumers
import code.model.dataAccess.{AuthUser, BankAccountRouting}
import code.util.Helper.{MdcLoggable, ObpS}
import com.openbankproject.commons.ExecutionContext.Implicits.global
import com.openbankproject.commons.model.BankIdAccountId
import net.liftweb.common.{Box, Failure, Full}
import net.liftweb.http.js.JsCmds
import net.liftweb.http.rest.RestHelper
import net.liftweb.http.{S, SHtml, SessionVar}
import net.liftweb.json.{Formats, parse}
import net.liftweb.mapper.By
import net.liftweb.util.CssSel
import net.liftweb.util.Helpers._

import scala.collection.immutable
import scala.concurrent.Future
import scala.xml.NodeSeq

class BerlinGroupConsent extends MdcLoggable with RestHelper with APIMethods510 with APIMethods500 with APIMethods310 {
  protected implicit override def formats: Formats = CustomJsonFormats.formats

  private object otpValue extends SessionVar("123")
  private object redirectUriValue extends SessionVar("")
  private object updateConsentPayloadValue extends SessionVar(false)
  private object userIsOwnerOfAccountsValue extends SessionVar(true)

  // Separate session variables for accounts, balances, and transactions
  private object selectedAccountsIbansValue extends SessionVar[Set[String]](Set()) {
    override def set(value: Set[String]): Set[String] = {
      logger.debug(s"selectedAccountsIbansValue changed to: ${value.mkString(", ")}")
      super.set(value)
    }
  }

  private object selectedBalancesIbansValue extends SessionVar[Set[String]](Set())

  private object selectedTransactionsIbansValue extends SessionVar[Set[String]](Set())

  // Function to transform a list of IBANs into ConsentAccessJson
  def createConsentAccessJson(accounts: List[String], balances: List[String], transactions: List[String]): ConsentAccessJson = {
    val accountsList = accounts.map(iban => ConsentAccessAccountsJson(iban = Some(iban), None, None, None, None, None))
    val balancesList = balances.map(iban => ConsentAccessAccountsJson(iban = Some(iban), None, None, None, None, None))
    val transactionsList = transactions.map(iban => ConsentAccessAccountsJson(iban = Some(iban), None, None, None, None, None))

    ConsentAccessJson(
      accounts = Some(accountsList), // Populate accounts
      balances = Some(balancesList), // Populate balances
      transactions = Some(transactionsList) // Populate transactions
    )
  }

  private def updateConsent(consentId: String, ibansAccount: List[String], ibansBalance: List[String], ibansTransaction: List[String]): Future[MappedConsent] = {
    for {
      consent: MappedConsent <- Future(Consents.consentProvider.vend.getConsentByConsentId(consentId)) map {
        APIUtil.unboxFullOrFail(_, None, s"$ConsentNotFound ($consentId)", 404)
      }
      consentJWT <- Consent.updateAccountAccessOfBerlinGroupConsentJWT(
        createConsentAccessJson(ibansAccount, ibansBalance, ibansTransaction),
        consent,
        None
      ) map {
        i => APIUtil.connectorEmptyResponse(i, None)
      }
      updatedConsent <- Future(Consents.consentProvider.vend.setJsonWebToken(consent.consentId, consentJWT)) map {
        i => APIUtil.connectorEmptyResponse(i, None)
      }
    } yield {
      updatedConsent
    }
  }


  def confirmBerlinGroupConsentRequest: CssSel = {
    callGetConsentByConsentId() match {
      case Full(consent) =>
        otpValue.set(consent.challenge)
        val json: GetConsentResponseJson = createGetConsentResponseJson(consent)
        val consumer = Consumers.consumers.vend.getConsumerByConsumerId(consent.consumerId)
        val consentJwt: Box[ConsentJWT] = JwtUtil.getSignedPayloadAsJson(consent.jsonWebToken).map(parse(_)
          .extract[ConsentJWT])
        val tppRedirectUri: immutable.Seq[String] = consentJwt.map{ h =>
          h.request_headers.filter(h => h.name == RequestHeader.`TPP-Redirect-URL`)
        }.getOrElse(Nil).map((_.values.mkString("")))
        val consumerRedirectUri: Option[String] = consumer.map(_.redirectURL.get).toOption
        val uri: String = tppRedirectUri.headOption.orElse(consumerRedirectUri).getOrElse("https://not.defined.com")
        redirectUriValue.set(uri)

        //Get All OBP accounts from `Account Holder` table, source == null --> mean accounts are created by OBP endpoints, not from User Auth Context,
        // Step 1: Get all accounts held by the current user
        val userAccounts: Set[BankIdAccountId] =
        AccountHolders.accountHolders.vend.getAccountsHeldByUser(AuthUser.currentUser.flatMap(_.user.foreign).openOrThrowException(ErrorMessages.UserNotLoggedIn), Some(null)).toSet
        val userIbans: Set[String] = userAccounts.flatMap { acc =>
          BankAccountRouting.find(
            By(BankAccountRouting.BankId, acc.bankId.value),
            By(BankAccountRouting.AccountId, acc.accountId.value),
            By(BankAccountRouting.AccountRoutingScheme, "IBAN")
          ).map(_.AccountRoutingAddress.get)
        }


        val canReadAccountsIbans: List[String] = json.access.accounts match {
          case Some(accounts) if accounts.isEmpty =>
            updateConsentPayloadValue.set(true)
            userIbans.toList
          case Some(accounts) if accounts.flatMap(_.iban).toSet.subsetOf(userIbans) =>
            accounts.flatMap(_.iban)
          case Some(accounts) =>
            userIsOwnerOfAccountsValue.set(false)
            accounts.flatMap(_.iban)
          case None => List()
        }
        val canReadBalancesIbans: List[String] = json.access.balances match {
          case Some(balances) if balances.isEmpty =>
            updateConsentPayloadValue.set(true)
            userIbans.toList
          case Some(balances) if balances.flatMap(_.iban).toSet.subsetOf(userIbans) =>
            balances.flatMap(_.iban)
          case Some(balances) =>
            userIsOwnerOfAccountsValue.set(false)
            balances.flatMap(_.iban)
          case None => List()
        }
        val canReadTransactionsIbans: List[String] = json.access.transactions match {
          case Some(transactions) if transactions.isEmpty =>
            updateConsentPayloadValue.set(true)
            userIbans.toList
          case Some(transactions) if transactions.flatMap(_.iban).toSet.subsetOf(userIbans) =>
            transactions.flatMap(_.iban)
          case Some(transactions) =>
            userIsOwnerOfAccountsValue.set(false)
            transactions.flatMap(_.iban)
          case None => List()
        }

        /// Function to generate toggle switches for IBAN lists
        def generateCheckboxes(scope: String, ibans: List[String], selectedList: Set[String], sessionVar: SessionVar[Set[String]]): immutable.Seq[NodeSeq] = {
          ibans.map { iban =>
            if (updateConsentPayloadValue.is) {
              // Show toggle switch when updateConsentPayloadValue is true
              <div class="toggle-container">
                <label class="switch">
                  {SHtml.ajaxCheckbox(selectedList.contains(iban), checked => {
                  if (checked) {
                    sessionVar.set(selectedList + iban) // Add to selected
                  } else {
                    sessionVar.set(selectedList - iban) // Remove from selected
                  }
                  JsCmds.Noop // Prevents page reload
                }, "id" -> (iban + scope), "class" -> "toggle-input")}<span class="slider round"></span>
                </label>
                <span style="all: unset;" class="toggle-label">
                  {iban}
                </span>
              </div>
            } else {
              // Show only the IBAN text when updateConsentPayloadValue is false
              <span style="all: unset;" class="toggle-label">
                {iban}
              </span>
            }
          }
        }


        // Form text and user details
        val currentUser = AuthUser.currentUser
        val firstName = currentUser.map(_.firstName.get).getOrElse("")
        val lastName = currentUser.map(_.lastName.get).getOrElse("")
        val consumerName = consumer.map(_.name.get).getOrElse("")
        val formText =
          s"""I, $firstName $lastName, consent to the service provider <strong>$consumerName</strong> making the following actions on my behalf:
             |""".stripMargin

        // Converting formText into a NodeSeq for raw HTML
        val formTextHtml: NodeSeq = scala.xml.XML.loadString("<div>" + formText + "</div>")

        // Form rendering
        "#confirm-bg-consent-request-form-title *" #> "Please confirm or deny the following consent request:" &
          "#confirm-bg-consent-request-form-text *" #> (
            <div>
              <p>
                {formTextHtml}
              </p>

              <p>1) Read account (basic) details of:</p>
              <div style="padding-left: 20px">
                {generateCheckboxes("canReadAccountsIbans", canReadAccountsIbans, selectedAccountsIbansValue.is, selectedAccountsIbansValue)}
              </div>
              <br/>

              <p>2) Read account balances of:</p>
              <div style="padding-left: 20px">
                {generateCheckboxes("canReadBalancesIbans", canReadBalancesIbans, selectedBalancesIbansValue.is, selectedBalancesIbansValue)}
              </div>
              <br/>

              <p>3) Read transactions of:</p>
              <div style="padding-left: 20px">
                {generateCheckboxes("canReadTransactionsIbans", canReadTransactionsIbans, selectedTransactionsIbansValue.is, selectedTransactionsIbansValue)}
              </div>
              <br/>

              <p>This consent will end on date
                {json.validUntil}
                .</p>
              <p>I understand that I can revoke this consent at any time.</p>
            </div>
            ) & {
          if (userIsOwnerOfAccountsValue) {
            "#confirm-bg-consent-request-confirm-submit-button" #> SHtml.onSubmitUnit(confirmConsentRequestProcess) &
              "#confirm-bg-consent-request-deny-submit-button" #> SHtml.onSubmitUnit(denyConsentRequestProcess)
          } else {
            S.error(s"User $firstName $lastName is not owner of listed accounts")
            "#confirm-bg-consent-request-confirm-submit-button" #> "" &
              "#confirm-bg-consent-request-deny-submit-button" #> ""
          }}

      case everythingElse =>
        S.error(everythingElse.toString)
        "#confirm-bg-consent-request-form-title *" #> s"Please confirm or deny the following consent request:" &
          "type=submit" #> ""
    }
  }

  private def callGetConsentByConsentId(): Box[MappedConsent] = {
    val requestParam = List(
      ObpS.param("CONSENT_ID"),
    )
    if (requestParam.count(_.isDefined) < requestParam.size) {
      Failure("Parameter CONSENT_ID is missing, please set it in the URL")
    } else {
      val consentId = ObpS.param("CONSENT_ID") openOr ("")
      Consents.consentProvider.vend.getConsentByConsentId(consentId)
    }
  }

  private def confirmConsentRequestProcess() = {
    if(selectedAccountsIbansValue.is.isEmpty &&
      selectedBalancesIbansValue.is.isEmpty &&
      selectedTransactionsIbansValue.is.isEmpty)
    {
      S.error(s"Please select at least 1 account")
    } else {
      val consentId = ObpS.param("CONSENT_ID") openOr ("")
      if (updateConsentPayloadValue.is) {
        updateConsent(consentId, selectedAccountsIbansValue.is.toList, selectedBalancesIbansValue.is.toList, selectedTransactionsIbansValue.is.toList)
      }
      S.redirectTo(
        s"/confirm-bg-consent-request-sca?CONSENT_ID=${consentId}"
      )
    }

  }
  private def denyConsentRequestProcess() = {
    val consentId = ObpS.param("CONSENT_ID") openOr ("")
    Consents.consentProvider.vend.updateConsentStatus(consentId, ConsentStatus.rejected)
    S.redirectTo(
      s"$redirectUriValue?CONSENT_ID=${consentId}"
    )
  }
  private def confirmConsentRequestProcessSca() = {
    val consentId = ObpS.param("CONSENT_ID") openOr ("")
    Consents.consentProvider.vend.getConsentByConsentId(consentId) match {
      case Full(consent) if otpValue.is == consent.challenge =>
        Consents.consentProvider.vend.updateConsentStatus(consentId, ConsentStatus.valid)
        S.redirectTo(
          s"$redirectUriValue?CONSENT_ID=${consentId}"
        )
      case _ =>
        S.error("Wrong OTP value")
    }
  }


  def confirmBgConsentRequest: CssSel = {
    "#otp-value" #> SHtml.text(otpValue, otpValue(_)) &
      "type=submit" #> SHtml.onSubmitUnit(confirmConsentRequestProcessSca)
  }
  
}
