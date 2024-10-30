package code.bankconnectors.vMay2019

/*
Open Bank Project - API
Copyright (C) 2011-2019, TESOBE GmbH

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program. If not, see http://www.gnu.org/licenses/.

Email: contact@tesobe.com
TESOBE GmbH
Osloerstrasse 16/17
Berlin 13359, Germany
*/

import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID.randomUUID
import code.api.APIFailure
import code.api.JSONFactoryGateway.PayloadOfJwtJSON
import code.api.ResourceDocs1_4_0.{MessageDocsSwaggerDefinitions, SwaggerDefinitionsJSON}
import code.api.cache.Caching
import code.api.util.APIUtil._
import code.api.util.ErrorMessages._
import code.api.util.ExampleValue._
import code.api.util._
import code.api.v2_1_0.TransactionRequestBodyCommonJSON
import code.bankconnectors._
import code.bankconnectors.vSept2018.KafkaMappedConnector_vSept2018
import code.customer._
import code.kafka.{KafkaHelper, Topics}
import code.model._
import code.model.dataAccess._
import code.users.Users
import code.util.Helper.MdcLoggable
import code.views.Views
import com.openbankproject.commons.dto._
import com.openbankproject.commons.model._
import com.sksamuel.avro4s.SchemaFor
import com.tesobe.{CacheKeyFromArguments, CacheKeyOmit}
import net.liftweb
import net.liftweb.common._
import net.liftweb.json.{MappingException, parse}
import net.liftweb.util.Helpers.tryo
import com.openbankproject.commons.ExecutionContext.Implicits.global
import com.openbankproject.commons.dto.{InBoundTrait, _}
import com.openbankproject.commons.model.enums.StrongCustomerAuthenticationStatus.SCAStatus
import com.openbankproject.commons.model.enums._
import scala.collection.immutable.{List, Nil}
import com.openbankproject.commons.ExecutionContext.Implicits.global

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import com.github.dwickern.macros.NameOf.nameOf
import com.openbankproject.commons.model.enums.{AccountAttributeType, CardAttributeType, ProductAttributeType, StrongCustomerAuthentication}
import com.openbankproject.commons.model.enums.StrongCustomerAuthentication.SCA
import com.openbankproject.commons.util.{ApiVersion, RequiredFieldValidation}
import com.openbankproject.commons.ExecutionContext.Implicits.global
import com.openbankproject.commons.dto.{InBoundTrait, _}
import com.openbankproject.commons.model.enums.StrongCustomerAuthenticationStatus.SCAStatus
import com.openbankproject.commons.model.enums._
import scala.reflect.runtime.universe._

trait KafkaMappedConnector_vMay2019 extends Connector with KafkaHelper with MdcLoggable {
  //this one import is for implicit convert, don't delete
  import com.openbankproject.commons.model.{CustomerFaceImage, CreditLimit, CreditRating, AmountOfMoney}

  implicit override val nameOfConnector = KafkaMappedConnector_vMay2019.toString

  // "Versioning" of the messages sent by this or similar connector works like this:
  // Use Case Classes (e.g. KafkaInbound... KafkaOutbound...) are defined below to describe the message structures.
  // Each connector has a separate file like this one.
  // Once the message format is STABLE, freeze the key/value pair names there. For now, new keys may be added but none modified.
  // If we want to add a new message format, create a new file e.g. March2017_messages.scala
  // Then add a suffix to the connector value i.e. instead of kafka we might have kafka_march_2017.
  // Then in this file, populate the different case classes depending on the connector name and send to Kafka
  val messageFormat: String = "May2019"


//---------------- dynamic start -------------------please don't modify this line
// ---------- created on 2024-10-30T11:52:35Z

  messageDocs += getAdapterInfoDoc
  def getAdapterInfoDoc = MessageDoc(
    process = "obp.getAdapterInfo",
    messageFormat = messageFormat,
    description = "Get Adapter Info",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetAdapterInfo").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetAdapterInfo").request),
    exampleOutboundMessage = (
          OutBoundGetAdapterInfo(MessageDocsSwaggerDefinitions.outboundAdapterCallContext)
    ),
    exampleInboundMessage = (
     InBoundGetAdapterInfo(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= InboundAdapterInfoInternal(errorCode=inboundAdapterInfoInternalErrorCodeExample.value,
      backendMessages=List( InboundStatusMessage(source=sourceExample.value,
      status=inboundStatusMessageStatusExample.value,
      errorCode=inboundStatusMessageErrorCodeExample.value,
      text=inboundStatusMessageTextExample.value,
      duration=Some(BigDecimal(durationExample.value)))),
      name=inboundAdapterInfoInternalNameExample.value,
      version=inboundAdapterInfoInternalVersionExample.value,
      git_commit=inboundAdapterInfoInternalGit_commitExample.value,
      date=inboundAdapterInfoInternalDateExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getAdapterInfo(callContext: Option[CallContext]): Future[Box[(InboundAdapterInfoInternal, Option[CallContext])]] = {
        import com.openbankproject.commons.dto.{InBoundGetAdapterInfo => InBound, OutBoundGetAdapterInfo => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[InboundAdapterInfoInternal](callContext))        
  }
          
  messageDocs += validateAndCheckIbanNumberDoc
  def validateAndCheckIbanNumberDoc = MessageDoc(
    process = "obp.validateAndCheckIbanNumber",
    messageFormat = messageFormat,
    description = "Validate And Check Iban Number",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundValidateAndCheckIbanNumber").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundValidateAndCheckIbanNumber").request),
    exampleOutboundMessage = (
     OutBoundValidateAndCheckIbanNumber(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      iban=ibanExample.value)
    ),
    exampleInboundMessage = (
     InBoundValidateAndCheckIbanNumber(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= IbanChecker(isValid=true,
      details=Some( IbanDetails(bic=bicExample.value,
      bank=bankExample.value,
      branch="string",
      address=addressExample.value,
      city=cityExample.value,
      zip="string",
      phone=phoneExample.value,
      country=countryExample.value,
      countryIso="string",
      sepaCreditTransfer=sepaCreditTransferExample.value,
      sepaDirectDebit=sepaDirectDebitExample.value,
      sepaSddCore=sepaSddCoreExample.value,
      sepaB2b=sepaB2bExample.value,
      sepaCardClearing=sepaCardClearingExample.value))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def validateAndCheckIbanNumber(iban: String, callContext: Option[CallContext]): OBPReturnType[Box[IbanChecker]] = {
        import com.openbankproject.commons.dto.{InBoundValidateAndCheckIbanNumber => InBound, OutBoundValidateAndCheckIbanNumber => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, iban)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[IbanChecker](callContext))        
  }
          
  messageDocs += getChallengeThresholdDoc
  def getChallengeThresholdDoc = MessageDoc(
    process = "obp.getChallengeThreshold",
    messageFormat = messageFormat,
    description = "Get Challenge Threshold",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetChallengeThreshold").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetChallengeThreshold").request),
    exampleOutboundMessage = (
     OutBoundGetChallengeThreshold(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=bankIdExample.value,
      accountId=accountIdExample.value,
      viewId=viewIdExample.value,
      transactionRequestType=transactionRequestTypeExample.value,
      currency=currencyExample.value,
      userId=userIdExample.value,
      username=usernameExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetChallengeThreshold(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= AmountOfMoney(currency=currencyExample.value,
      amount=amountExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getChallengeThreshold(bankId: String, accountId: String, viewId: String, transactionRequestType: String, currency: String, userId: String, username: String, callContext: Option[CallContext]): OBPReturnType[Box[AmountOfMoney]] = {
        import com.openbankproject.commons.dto.{InBoundGetChallengeThreshold => InBound, OutBoundGetChallengeThreshold => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, accountId, viewId, transactionRequestType, currency, userId, username)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[AmountOfMoney](callContext))        
  }
          
  messageDocs += getChargeLevelDoc
  def getChargeLevelDoc = MessageDoc(
    process = "obp.getChargeLevel",
    messageFormat = messageFormat,
    description = "Get Charge Level",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetChargeLevel").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetChargeLevel").request),
    exampleOutboundMessage = (
     OutBoundGetChargeLevel(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value),
      viewId=ViewId(viewIdExample.value),
      userId=userIdExample.value,
      username=usernameExample.value,
      transactionRequestType=transactionRequestTypeExample.value,
      currency=currencyExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetChargeLevel(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= AmountOfMoney(currency=currencyExample.value,
      amount=amountExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getChargeLevel(bankId: BankId, accountId: AccountId, viewId: ViewId, userId: String, username: String, transactionRequestType: String, currency: String, callContext: Option[CallContext]): OBPReturnType[Box[AmountOfMoney]] = {
        import com.openbankproject.commons.dto.{InBoundGetChargeLevel => InBound, OutBoundGetChargeLevel => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, accountId, viewId, userId, username, transactionRequestType, currency)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[AmountOfMoney](callContext))        
  }
          
  messageDocs += getChargeLevelC2Doc
  def getChargeLevelC2Doc = MessageDoc(
    process = "obp.getChargeLevelC2",
    messageFormat = messageFormat,
    description = "Get Charge Level C2",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetChargeLevelC2").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetChargeLevelC2").request),
    exampleOutboundMessage = (
     OutBoundGetChargeLevelC2(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value),
      viewId=ViewId(viewIdExample.value),
      userId=userIdExample.value,
      username=usernameExample.value,
      transactionRequestType=transactionRequestTypeExample.value,
      currency=currencyExample.value,
      amount=amountExample.value,
      toAccountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      customAttributes=List( CustomAttribute(name=nameExample.value,
      attributeType=com.openbankproject.commons.model.enums.AttributeType.example,
      value=valueExample.value)))
    ),
    exampleInboundMessage = (
     InBoundGetChargeLevelC2(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= AmountOfMoney(currency=currencyExample.value,
      amount=amountExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getChargeLevelC2(bankId: BankId, accountId: AccountId, viewId: ViewId, userId: String, username: String, transactionRequestType: String, currency: String, amount: String, toAccountRoutings: List[AccountRouting], customAttributes: List[CustomAttribute], callContext: Option[CallContext]): OBPReturnType[Box[AmountOfMoney]] = {
        import com.openbankproject.commons.dto.{InBoundGetChargeLevelC2 => InBound, OutBoundGetChargeLevelC2 => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, accountId, viewId, userId, username, transactionRequestType, currency, amount, toAccountRoutings, customAttributes)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[AmountOfMoney](callContext))        
  }
          
  messageDocs += createChallengeDoc
  def createChallengeDoc = MessageDoc(
    process = "obp.createChallenge",
    messageFormat = messageFormat,
    description = "Create Challenge",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateChallenge").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateChallenge").request),
    exampleOutboundMessage = (
     OutBoundCreateChallenge(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value),
      userId=userIdExample.value,
      transactionRequestType=TransactionRequestType(transactionRequestTypeExample.value),
      transactionRequestId=transactionRequestIdExample.value,
      scaMethod=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthentication.SMS))
    ),
    exampleInboundMessage = (
     InBoundCreateChallenge(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data="string")
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createChallenge(bankId: BankId, accountId: AccountId, userId: String, transactionRequestType: TransactionRequestType, transactionRequestId: String, scaMethod: Option[StrongCustomerAuthentication.SCA], callContext: Option[CallContext]): OBPReturnType[Box[String]] = {
        import com.openbankproject.commons.dto.{InBoundCreateChallenge => InBound, OutBoundCreateChallenge => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, accountId, userId, transactionRequestType, transactionRequestId, scaMethod)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[String](callContext))        
  }
          
  messageDocs += createChallengesDoc
  def createChallengesDoc = MessageDoc(
    process = "obp.createChallenges",
    messageFormat = messageFormat,
    description = "Create Challenges",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateChallenges").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateChallenges").request),
    exampleOutboundMessage = (
     OutBoundCreateChallenges(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value),
      userIds=listExample.value.replace("[","").replace("]","").split(",").toList,
      transactionRequestType=TransactionRequestType(transactionRequestTypeExample.value),
      transactionRequestId=transactionRequestIdExample.value,
      scaMethod=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthentication.SMS))
    ),
    exampleInboundMessage = (
     InBoundCreateChallenges(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=listExample.value.replace("[","").replace("]","").split(",").toList)
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createChallenges(bankId: BankId, accountId: AccountId, userIds: List[String], transactionRequestType: TransactionRequestType, transactionRequestId: String, scaMethod: Option[StrongCustomerAuthentication.SCA], callContext: Option[CallContext]): OBPReturnType[Box[List[String]]] = {
        import com.openbankproject.commons.dto.{InBoundCreateChallenges => InBound, OutBoundCreateChallenges => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, accountId, userIds, transactionRequestType, transactionRequestId, scaMethod)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[String]](callContext))        
  }
          
  messageDocs += createChallengesC2Doc
  def createChallengesC2Doc = MessageDoc(
    process = "obp.createChallengesC2",
    messageFormat = messageFormat,
    description = "Create Challenges C2",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateChallengesC2").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateChallengesC2").request),
    exampleOutboundMessage = (
     OutBoundCreateChallengesC2(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      userIds=listExample.value.replace("[","").replace("]","").split(",").toList,
      challengeType=com.openbankproject.commons.model.enums.ChallengeType.example,
      transactionRequestId=Some(transactionRequestIdExample.value),
      scaMethod=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthentication.SMS),
      scaStatus=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthenticationStatus.example),
      consentId=Some(consentIdExample.value),
      authenticationMethodId=Some("string"))
    ),
    exampleInboundMessage = (
     InBoundCreateChallengesC2(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( ChallengeCommons(challengeId=challengeIdExample.value,
      transactionRequestId=transactionRequestIdExample.value,
      expectedAnswer="string",
      expectedUserId="string",
      salt="string",
      successful=true,
      challengeType=challengeTypeExample.value,
      consentId=Some(consentIdExample.value),
      basketId=Some(basketIdExample.value),
      scaMethod=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthentication.SMS),
      scaStatus=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthenticationStatus.example),
      authenticationMethodId=Some("string"),
      attemptCounter=123)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createChallengesC2(userIds: List[String], challengeType: ChallengeType.Value, transactionRequestId: Option[String], scaMethod: Option[StrongCustomerAuthentication.SCA], scaStatus: Option[SCAStatus], consentId: Option[String], authenticationMethodId: Option[String], callContext: Option[CallContext]): OBPReturnType[Box[List[ChallengeTrait]]] = {
        import com.openbankproject.commons.dto.{InBoundCreateChallengesC2 => InBound, OutBoundCreateChallengesC2 => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, userIds, challengeType, transactionRequestId, scaMethod, scaStatus, consentId, authenticationMethodId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[ChallengeCommons]](callContext))        
  }
          
  messageDocs += createChallengesC3Doc
  def createChallengesC3Doc = MessageDoc(
    process = "obp.createChallengesC3",
    messageFormat = messageFormat,
    description = "Create Challenges C3",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateChallengesC3").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateChallengesC3").request),
    exampleOutboundMessage = (
     OutBoundCreateChallengesC3(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      userIds=listExample.value.replace("[","").replace("]","").split(",").toList,
      challengeType=com.openbankproject.commons.model.enums.ChallengeType.example,
      transactionRequestId=Some(transactionRequestIdExample.value),
      scaMethod=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthentication.SMS),
      scaStatus=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthenticationStatus.example),
      consentId=Some(consentIdExample.value),
      basketId=Some(basketIdExample.value),
      authenticationMethodId=Some("string"))
    ),
    exampleInboundMessage = (
     InBoundCreateChallengesC3(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( ChallengeCommons(challengeId=challengeIdExample.value,
      transactionRequestId=transactionRequestIdExample.value,
      expectedAnswer="string",
      expectedUserId="string",
      salt="string",
      successful=true,
      challengeType=challengeTypeExample.value,
      consentId=Some(consentIdExample.value),
      basketId=Some(basketIdExample.value),
      scaMethod=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthentication.SMS),
      scaStatus=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthenticationStatus.example),
      authenticationMethodId=Some("string"),
      attemptCounter=123)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createChallengesC3(userIds: List[String], challengeType: ChallengeType.Value, transactionRequestId: Option[String], scaMethod: Option[StrongCustomerAuthentication.SCA], scaStatus: Option[SCAStatus], consentId: Option[String], basketId: Option[String], authenticationMethodId: Option[String], callContext: Option[CallContext]): OBPReturnType[Box[List[ChallengeTrait]]] = {
        import com.openbankproject.commons.dto.{InBoundCreateChallengesC3 => InBound, OutBoundCreateChallengesC3 => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, userIds, challengeType, transactionRequestId, scaMethod, scaStatus, consentId, basketId, authenticationMethodId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[ChallengeCommons]](callContext))        
  }
          
  messageDocs += validateChallengeAnswerDoc
  def validateChallengeAnswerDoc = MessageDoc(
    process = "obp.validateChallengeAnswer",
    messageFormat = messageFormat,
    description = "Validate Challenge Answer",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundValidateChallengeAnswer").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundValidateChallengeAnswer").request),
    exampleOutboundMessage = (
     OutBoundValidateChallengeAnswer(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      challengeId=challengeIdExample.value,
      hashOfSuppliedAnswer=hashOfSuppliedAnswerExample.value)
    ),
    exampleInboundMessage = (
     InBoundValidateChallengeAnswer(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=true)
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def validateChallengeAnswer(challengeId: String, hashOfSuppliedAnswer: String, callContext: Option[CallContext]): OBPReturnType[Box[Boolean]] = {
        import com.openbankproject.commons.dto.{InBoundValidateChallengeAnswer => InBound, OutBoundValidateChallengeAnswer => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, challengeId, hashOfSuppliedAnswer)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[Boolean](callContext))        
  }
          
  messageDocs += validateChallengeAnswerV2Doc
  def validateChallengeAnswerV2Doc = MessageDoc(
    process = "obp.validateChallengeAnswerV2",
    messageFormat = messageFormat,
    description = "Validate Challenge Answer V2",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundValidateChallengeAnswerV2").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundValidateChallengeAnswerV2").request),
    exampleOutboundMessage = (
     OutBoundValidateChallengeAnswerV2(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      challengeId=challengeIdExample.value,
      suppliedAnswer=suppliedAnswerExample.value,
      suppliedAnswerType=com.openbankproject.commons.model.enums.SuppliedAnswerType.example)
    ),
    exampleInboundMessage = (
     InBoundValidateChallengeAnswerV2(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=true)
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def validateChallengeAnswerV2(challengeId: String, suppliedAnswer: String, suppliedAnswerType: SuppliedAnswerType.Value, callContext: Option[CallContext]): OBPReturnType[Box[Boolean]] = {
        import com.openbankproject.commons.dto.{InBoundValidateChallengeAnswerV2 => InBound, OutBoundValidateChallengeAnswerV2 => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, challengeId, suppliedAnswer, suppliedAnswerType)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[Boolean](callContext))        
  }
          
  messageDocs += validateChallengeAnswerC2Doc
  def validateChallengeAnswerC2Doc = MessageDoc(
    process = "obp.validateChallengeAnswerC2",
    messageFormat = messageFormat,
    description = "Validate Challenge Answer C2",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundValidateChallengeAnswerC2").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundValidateChallengeAnswerC2").request),
    exampleOutboundMessage = (
     OutBoundValidateChallengeAnswerC2(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      transactionRequestId=Some(transactionRequestIdExample.value),
      consentId=Some(consentIdExample.value),
      challengeId=challengeIdExample.value,
      hashOfSuppliedAnswer=hashOfSuppliedAnswerExample.value)
    ),
    exampleInboundMessage = (
     InBoundValidateChallengeAnswerC2(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= ChallengeCommons(challengeId=challengeIdExample.value,
      transactionRequestId=transactionRequestIdExample.value,
      expectedAnswer="string",
      expectedUserId="string",
      salt="string",
      successful=true,
      challengeType=challengeTypeExample.value,
      consentId=Some(consentIdExample.value),
      basketId=Some(basketIdExample.value),
      scaMethod=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthentication.SMS),
      scaStatus=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthenticationStatus.example),
      authenticationMethodId=Some("string"),
      attemptCounter=123))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def validateChallengeAnswerC2(transactionRequestId: Option[String], consentId: Option[String], challengeId: String, hashOfSuppliedAnswer: String, callContext: Option[CallContext]): OBPReturnType[Box[ChallengeTrait]] = {
        import com.openbankproject.commons.dto.{InBoundValidateChallengeAnswerC2 => InBound, OutBoundValidateChallengeAnswerC2 => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, transactionRequestId, consentId, challengeId, hashOfSuppliedAnswer)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[ChallengeCommons](callContext))        
  }
          
  messageDocs += validateChallengeAnswerC3Doc
  def validateChallengeAnswerC3Doc = MessageDoc(
    process = "obp.validateChallengeAnswerC3",
    messageFormat = messageFormat,
    description = "Validate Challenge Answer C3",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundValidateChallengeAnswerC3").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundValidateChallengeAnswerC3").request),
    exampleOutboundMessage = (
     OutBoundValidateChallengeAnswerC3(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      transactionRequestId=Some(transactionRequestIdExample.value),
      consentId=Some(consentIdExample.value),
      basketId=Some(basketIdExample.value),
      challengeId=challengeIdExample.value,
      hashOfSuppliedAnswer=hashOfSuppliedAnswerExample.value)
    ),
    exampleInboundMessage = (
     InBoundValidateChallengeAnswerC3(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= ChallengeCommons(challengeId=challengeIdExample.value,
      transactionRequestId=transactionRequestIdExample.value,
      expectedAnswer="string",
      expectedUserId="string",
      salt="string",
      successful=true,
      challengeType=challengeTypeExample.value,
      consentId=Some(consentIdExample.value),
      basketId=Some(basketIdExample.value),
      scaMethod=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthentication.SMS),
      scaStatus=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthenticationStatus.example),
      authenticationMethodId=Some("string"),
      attemptCounter=123))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def validateChallengeAnswerC3(transactionRequestId: Option[String], consentId: Option[String], basketId: Option[String], challengeId: String, hashOfSuppliedAnswer: String, callContext: Option[CallContext]): OBPReturnType[Box[ChallengeTrait]] = {
        import com.openbankproject.commons.dto.{InBoundValidateChallengeAnswerC3 => InBound, OutBoundValidateChallengeAnswerC3 => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, transactionRequestId, consentId, basketId, challengeId, hashOfSuppliedAnswer)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[ChallengeCommons](callContext))        
  }
          
  messageDocs += validateChallengeAnswerC4Doc
  def validateChallengeAnswerC4Doc = MessageDoc(
    process = "obp.validateChallengeAnswerC4",
    messageFormat = messageFormat,
    description = "Validate Challenge Answer C4",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundValidateChallengeAnswerC4").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundValidateChallengeAnswerC4").request),
    exampleOutboundMessage = (
     OutBoundValidateChallengeAnswerC4(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      transactionRequestId=Some(transactionRequestIdExample.value),
      consentId=Some(consentIdExample.value),
      challengeId=challengeIdExample.value,
      suppliedAnswer=suppliedAnswerExample.value,
      suppliedAnswerType=com.openbankproject.commons.model.enums.SuppliedAnswerType.example)
    ),
    exampleInboundMessage = (
     InBoundValidateChallengeAnswerC4(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= ChallengeCommons(challengeId=challengeIdExample.value,
      transactionRequestId=transactionRequestIdExample.value,
      expectedAnswer="string",
      expectedUserId="string",
      salt="string",
      successful=true,
      challengeType=challengeTypeExample.value,
      consentId=Some(consentIdExample.value),
      basketId=Some(basketIdExample.value),
      scaMethod=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthentication.SMS),
      scaStatus=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthenticationStatus.example),
      authenticationMethodId=Some("string"),
      attemptCounter=123))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def validateChallengeAnswerC4(transactionRequestId: Option[String], consentId: Option[String], challengeId: String, suppliedAnswer: String, suppliedAnswerType: SuppliedAnswerType.Value, callContext: Option[CallContext]): OBPReturnType[Box[ChallengeTrait]] = {
        import com.openbankproject.commons.dto.{InBoundValidateChallengeAnswerC4 => InBound, OutBoundValidateChallengeAnswerC4 => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, transactionRequestId, consentId, challengeId, suppliedAnswer, suppliedAnswerType)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[ChallengeCommons](callContext))        
  }
          
  messageDocs += validateChallengeAnswerC5Doc
  def validateChallengeAnswerC5Doc = MessageDoc(
    process = "obp.validateChallengeAnswerC5",
    messageFormat = messageFormat,
    description = "Validate Challenge Answer C5",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundValidateChallengeAnswerC5").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundValidateChallengeAnswerC5").request),
    exampleOutboundMessage = (
     OutBoundValidateChallengeAnswerC5(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      transactionRequestId=Some(transactionRequestIdExample.value),
      consentId=Some(consentIdExample.value),
      basketId=Some(basketIdExample.value),
      challengeId=challengeIdExample.value,
      suppliedAnswer=suppliedAnswerExample.value,
      suppliedAnswerType=com.openbankproject.commons.model.enums.SuppliedAnswerType.example)
    ),
    exampleInboundMessage = (
     InBoundValidateChallengeAnswerC5(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= ChallengeCommons(challengeId=challengeIdExample.value,
      transactionRequestId=transactionRequestIdExample.value,
      expectedAnswer="string",
      expectedUserId="string",
      salt="string",
      successful=true,
      challengeType=challengeTypeExample.value,
      consentId=Some(consentIdExample.value),
      basketId=Some(basketIdExample.value),
      scaMethod=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthentication.SMS),
      scaStatus=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthenticationStatus.example),
      authenticationMethodId=Some("string"),
      attemptCounter=123))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def validateChallengeAnswerC5(transactionRequestId: Option[String], consentId: Option[String], basketId: Option[String], challengeId: String, suppliedAnswer: String, suppliedAnswerType: SuppliedAnswerType.Value, callContext: Option[CallContext]): OBPReturnType[Box[ChallengeTrait]] = {
        import com.openbankproject.commons.dto.{InBoundValidateChallengeAnswerC5 => InBound, OutBoundValidateChallengeAnswerC5 => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, transactionRequestId, consentId, basketId, challengeId, suppliedAnswer, suppliedAnswerType)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[ChallengeCommons](callContext))        
  }
          
  messageDocs += getChallengesByTransactionRequestIdDoc
  def getChallengesByTransactionRequestIdDoc = MessageDoc(
    process = "obp.getChallengesByTransactionRequestId",
    messageFormat = messageFormat,
    description = "Get Challenges By Transaction Request Id",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetChallengesByTransactionRequestId").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetChallengesByTransactionRequestId").request),
    exampleOutboundMessage = (
     OutBoundGetChallengesByTransactionRequestId(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      transactionRequestId=transactionRequestIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetChallengesByTransactionRequestId(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( ChallengeCommons(challengeId=challengeIdExample.value,
      transactionRequestId=transactionRequestIdExample.value,
      expectedAnswer="string",
      expectedUserId="string",
      salt="string",
      successful=true,
      challengeType=challengeTypeExample.value,
      consentId=Some(consentIdExample.value),
      basketId=Some(basketIdExample.value),
      scaMethod=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthentication.SMS),
      scaStatus=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthenticationStatus.example),
      authenticationMethodId=Some("string"),
      attemptCounter=123)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getChallengesByTransactionRequestId(transactionRequestId: String, callContext: Option[CallContext]): OBPReturnType[Box[List[ChallengeTrait]]] = {
        import com.openbankproject.commons.dto.{InBoundGetChallengesByTransactionRequestId => InBound, OutBoundGetChallengesByTransactionRequestId => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, transactionRequestId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[ChallengeCommons]](callContext))        
  }
          
  messageDocs += getChallengesByConsentIdDoc
  def getChallengesByConsentIdDoc = MessageDoc(
    process = "obp.getChallengesByConsentId",
    messageFormat = messageFormat,
    description = "Get Challenges By Consent Id",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetChallengesByConsentId").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetChallengesByConsentId").request),
    exampleOutboundMessage = (
     OutBoundGetChallengesByConsentId(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      consentId=consentIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetChallengesByConsentId(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( ChallengeCommons(challengeId=challengeIdExample.value,
      transactionRequestId=transactionRequestIdExample.value,
      expectedAnswer="string",
      expectedUserId="string",
      salt="string",
      successful=true,
      challengeType=challengeTypeExample.value,
      consentId=Some(consentIdExample.value),
      basketId=Some(basketIdExample.value),
      scaMethod=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthentication.SMS),
      scaStatus=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthenticationStatus.example),
      authenticationMethodId=Some("string"),
      attemptCounter=123)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getChallengesByConsentId(consentId: String, callContext: Option[CallContext]): OBPReturnType[Box[List[ChallengeTrait]]] = {
        import com.openbankproject.commons.dto.{InBoundGetChallengesByConsentId => InBound, OutBoundGetChallengesByConsentId => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, consentId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[ChallengeCommons]](callContext))        
  }
          
  messageDocs += getChallengesByBasketIdDoc
  def getChallengesByBasketIdDoc = MessageDoc(
    process = "obp.getChallengesByBasketId",
    messageFormat = messageFormat,
    description = "Get Challenges By Basket Id",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetChallengesByBasketId").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetChallengesByBasketId").request),
    exampleOutboundMessage = (
     OutBoundGetChallengesByBasketId(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      basketId=basketIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetChallengesByBasketId(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( ChallengeCommons(challengeId=challengeIdExample.value,
      transactionRequestId=transactionRequestIdExample.value,
      expectedAnswer="string",
      expectedUserId="string",
      salt="string",
      successful=true,
      challengeType=challengeTypeExample.value,
      consentId=Some(consentIdExample.value),
      basketId=Some(basketIdExample.value),
      scaMethod=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthentication.SMS),
      scaStatus=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthenticationStatus.example),
      authenticationMethodId=Some("string"),
      attemptCounter=123)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getChallengesByBasketId(basketId: String, callContext: Option[CallContext]): OBPReturnType[Box[List[ChallengeTrait]]] = {
        import com.openbankproject.commons.dto.{InBoundGetChallengesByBasketId => InBound, OutBoundGetChallengesByBasketId => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, basketId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[ChallengeCommons]](callContext))        
  }
          
  messageDocs += getChallengeDoc
  def getChallengeDoc = MessageDoc(
    process = "obp.getChallenge",
    messageFormat = messageFormat,
    description = "Get Challenge",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetChallenge").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetChallenge").request),
    exampleOutboundMessage = (
     OutBoundGetChallenge(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      challengeId=challengeIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetChallenge(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= ChallengeCommons(challengeId=challengeIdExample.value,
      transactionRequestId=transactionRequestIdExample.value,
      expectedAnswer="string",
      expectedUserId="string",
      salt="string",
      successful=true,
      challengeType=challengeTypeExample.value,
      consentId=Some(consentIdExample.value),
      basketId=Some(basketIdExample.value),
      scaMethod=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthentication.SMS),
      scaStatus=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthenticationStatus.example),
      authenticationMethodId=Some("string"),
      attemptCounter=123))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getChallenge(challengeId: String, callContext: Option[CallContext]): OBPReturnType[Box[ChallengeTrait]] = {
        import com.openbankproject.commons.dto.{InBoundGetChallenge => InBound, OutBoundGetChallenge => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, challengeId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[ChallengeCommons](callContext))        
  }
          
  messageDocs += getBankDoc
  def getBankDoc = MessageDoc(
    process = "obp.getBank",
    messageFormat = messageFormat,
    description = "Get Bank",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetBank").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetBank").request),
    exampleOutboundMessage = (
     OutBoundGetBank(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value))
    ),
    exampleInboundMessage = (
     InBoundGetBank(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= BankCommons(bankId=BankId(bankIdExample.value),
      shortName=bankShortNameExample.value,
      fullName=bankFullNameExample.value,
      logoUrl=bankLogoUrlExample.value,
      websiteUrl=bankWebsiteUrlExample.value,
      bankRoutingScheme=bankRoutingSchemeExample.value,
      bankRoutingAddress=bankRoutingAddressExample.value,
      swiftBic=bankSwiftBicExample.value,
      nationalIdentifier=bankNationalIdentifierExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getBank(bankId: BankId, callContext: Option[CallContext]): Future[Box[(Bank, Option[CallContext])]] = {
        import com.openbankproject.commons.dto.{InBoundGetBank => InBound, OutBoundGetBank => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[BankCommons](callContext))        
  }
          
  messageDocs += getBanksDoc
  def getBanksDoc = MessageDoc(
    process = "obp.getBanks",
    messageFormat = messageFormat,
    description = "Get Banks",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetBanks").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetBanks").request),
    exampleOutboundMessage = (
          OutBoundGetBanks(MessageDocsSwaggerDefinitions.outboundAdapterCallContext)
    ),
    exampleInboundMessage = (
     InBoundGetBanks(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( BankCommons(bankId=BankId(bankIdExample.value),
      shortName=bankShortNameExample.value,
      fullName=bankFullNameExample.value,
      logoUrl=bankLogoUrlExample.value,
      websiteUrl=bankWebsiteUrlExample.value,
      bankRoutingScheme=bankRoutingSchemeExample.value,
      bankRoutingAddress=bankRoutingAddressExample.value,
      swiftBic=bankSwiftBicExample.value,
      nationalIdentifier=bankNationalIdentifierExample.value)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getBanks(callContext: Option[CallContext]): Future[Box[(List[Bank], Option[CallContext])]] = {
        import com.openbankproject.commons.dto.{InBoundGetBanks => InBound, OutBoundGetBanks => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[BankCommons]](callContext))        
  }
          
  messageDocs += getBankAccountsForUserDoc
  def getBankAccountsForUserDoc = MessageDoc(
    process = "obp.getBankAccountsForUser",
    messageFormat = messageFormat,
    description = "Get Bank Accounts For User",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetBankAccountsForUser").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetBankAccountsForUser").request),
    exampleOutboundMessage = (
     OutBoundGetBankAccountsForUser(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      provider=providerExample.value,
      username=usernameExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetBankAccountsForUser(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( InboundAccountCommons(bankId=bankIdExample.value,
      branchId=branchIdExample.value,
      accountId=accountIdExample.value,
      accountNumber=accountNumberExample.value,
      accountType=accountTypeExample.value,
      balanceAmount=balanceAmountExample.value,
      balanceCurrency=balanceCurrencyExample.value,
      owners=inboundAccountOwnersExample.value.replace("[","").replace("]","").split(",").toList,
      viewsToGenerate=inboundAccountViewsToGenerateExample.value.replace("[","").replace("]","").split(",").toList,
      bankRoutingScheme=bankRoutingSchemeExample.value,
      bankRoutingAddress=bankRoutingAddressExample.value,
      branchRoutingScheme=branchRoutingSchemeExample.value,
      branchRoutingAddress=branchRoutingAddressExample.value,
      accountRoutingScheme=accountRoutingSchemeExample.value,
      accountRoutingAddress=accountRoutingAddressExample.value)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getBankAccountsForUser(provider: String, username: String, callContext: Option[CallContext]): Future[Box[(List[InboundAccount], Option[CallContext])]] = {
        import com.openbankproject.commons.dto.{InBoundGetBankAccountsForUser => InBound, OutBoundGetBankAccountsForUser => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, provider, username)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[InboundAccountCommons]](callContext))        
  }
          
  messageDocs += getBankAccountByIbanDoc
  def getBankAccountByIbanDoc = MessageDoc(
    process = "obp.getBankAccountByIban",
    messageFormat = messageFormat,
    description = "Get Bank Account By Iban",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetBankAccountByIban").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetBankAccountByIban").request),
    exampleOutboundMessage = (
     OutBoundGetBankAccountByIban(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      iban=ibanExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetBankAccountByIban(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getBankAccountByIban(iban: String, callContext: Option[CallContext]): OBPReturnType[Box[BankAccount]] = {
        import com.openbankproject.commons.dto.{InBoundGetBankAccountByIban => InBound, OutBoundGetBankAccountByIban => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, iban)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[BankAccountCommons](callContext))        
  }
          
  messageDocs += getBankAccountByRoutingDoc
  def getBankAccountByRoutingDoc = MessageDoc(
    process = "obp.getBankAccountByRouting",
    messageFormat = messageFormat,
    description = "Get Bank Account By Routing",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetBankAccountByRouting").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetBankAccountByRouting").request),
    exampleOutboundMessage = (
     OutBoundGetBankAccountByRouting(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=Some(BankId(bankIdExample.value)),
      scheme=schemeExample.value,
      address=addressExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetBankAccountByRouting(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getBankAccountByRouting(bankId: Option[BankId], scheme: String, address: String, callContext: Option[CallContext]): OBPReturnType[Box[BankAccount]] = {
        import com.openbankproject.commons.dto.{InBoundGetBankAccountByRouting => InBound, OutBoundGetBankAccountByRouting => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, scheme, address)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[BankAccountCommons](callContext))        
  }
          
  messageDocs += getBankAccountsDoc
  def getBankAccountsDoc = MessageDoc(
    process = "obp.getBankAccounts",
    messageFormat = messageFormat,
    description = "Get Bank Accounts",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetBankAccounts").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetBankAccounts").request),
    exampleOutboundMessage = (
     OutBoundGetBankAccounts(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankIdAccountIds=List( BankIdAccountId(bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value))))
    ),
    exampleInboundMessage = (
     InBoundGetBankAccounts(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value))))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getBankAccounts(bankIdAccountIds: List[BankIdAccountId], callContext: Option[CallContext]): OBPReturnType[Box[List[BankAccount]]] = {
        import com.openbankproject.commons.dto.{InBoundGetBankAccounts => InBound, OutBoundGetBankAccounts => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankIdAccountIds)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[BankAccountCommons]](callContext))        
  }
          
  messageDocs += getBankAccountsBalancesDoc
  def getBankAccountsBalancesDoc = MessageDoc(
    process = "obp.getBankAccountsBalances",
    messageFormat = messageFormat,
    description = "Get Bank Accounts Balances",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetBankAccountsBalances").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetBankAccountsBalances").request),
    exampleOutboundMessage = (
     OutBoundGetBankAccountsBalances(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankIdAccountIds=List( BankIdAccountId(bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value))))
    ),
    exampleInboundMessage = (
     InBoundGetBankAccountsBalances(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= AccountsBalances(accounts=List( AccountBalance(id=idExample.value,
      label=labelExample.value,
      bankId=bankIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      balance= AmountOfMoney(currency=balanceCurrencyExample.value,
      amount=balanceAmountExample.value))),
      overallBalance= AmountOfMoney(currency=currencyExample.value,
      amount=amountExample.value),
      overallBalanceDate=toDate(overallBalanceDateExample)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getBankAccountsBalances(bankIdAccountIds: List[BankIdAccountId], callContext: Option[CallContext]): OBPReturnType[Box[AccountsBalances]] = {
        import com.openbankproject.commons.dto.{InBoundGetBankAccountsBalances => InBound, OutBoundGetBankAccountsBalances => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankIdAccountIds)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[AccountsBalances](callContext))        
  }
          
  messageDocs += getBankAccountBalancesDoc
  def getBankAccountBalancesDoc = MessageDoc(
    process = "obp.getBankAccountBalances",
    messageFormat = messageFormat,
    description = "Get Bank Account Balances",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetBankAccountBalances").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetBankAccountBalances").request),
    exampleOutboundMessage = (
     OutBoundGetBankAccountBalances(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankIdAccountId= BankIdAccountId(bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value)))
    ),
    exampleInboundMessage = (
     InBoundGetBankAccountBalances(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= AccountBalances(id=idExample.value,
      label=labelExample.value,
      bankId=bankIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      balances=List( BankAccountBalance(balance= AmountOfMoney(currency=balanceCurrencyExample.value,
      amount=balanceAmountExample.value),
      balanceType="string")),
      overallBalance= AmountOfMoney(currency=currencyExample.value,
      amount=amountExample.value),
      overallBalanceDate=toDate(overallBalanceDateExample)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getBankAccountBalances(bankIdAccountId: BankIdAccountId, callContext: Option[CallContext]): OBPReturnType[Box[AccountBalances]] = {
        import com.openbankproject.commons.dto.{InBoundGetBankAccountBalances => InBound, OutBoundGetBankAccountBalances => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankIdAccountId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[AccountBalances](callContext))        
  }
          
  messageDocs += getCoreBankAccountsDoc
  def getCoreBankAccountsDoc = MessageDoc(
    process = "obp.getCoreBankAccounts",
    messageFormat = messageFormat,
    description = "Get Core Bank Accounts",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCoreBankAccounts").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCoreBankAccounts").request),
    exampleOutboundMessage = (
     OutBoundGetCoreBankAccounts(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankIdAccountIds=List( BankIdAccountId(bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value))))
    ),
    exampleInboundMessage = (
     InBoundGetCoreBankAccounts(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( CoreAccount(id=accountIdExample.value,
      label=labelExample.value,
      bankId=bankIdExample.value,
      accountType=accountTypeExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getCoreBankAccounts(bankIdAccountIds: List[BankIdAccountId], callContext: Option[CallContext]): Future[Box[(List[CoreAccount], Option[CallContext])]] = {
        import com.openbankproject.commons.dto.{InBoundGetCoreBankAccounts => InBound, OutBoundGetCoreBankAccounts => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankIdAccountIds)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[CoreAccount]](callContext))        
  }
          
  messageDocs += getBankAccountsHeldDoc
  def getBankAccountsHeldDoc = MessageDoc(
    process = "obp.getBankAccountsHeld",
    messageFormat = messageFormat,
    description = "Get Bank Accounts Held",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetBankAccountsHeld").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetBankAccountsHeld").request),
    exampleOutboundMessage = (
     OutBoundGetBankAccountsHeld(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankIdAccountIds=List( BankIdAccountId(bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value))))
    ),
    exampleInboundMessage = (
     InBoundGetBankAccountsHeld(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( AccountHeld(id=idExample.value,
      label=labelExample.value,
      bankId=bankIdExample.value,
      number=numberExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getBankAccountsHeld(bankIdAccountIds: List[BankIdAccountId], callContext: Option[CallContext]): OBPReturnType[Box[List[AccountHeld]]] = {
        import com.openbankproject.commons.dto.{InBoundGetBankAccountsHeld => InBound, OutBoundGetBankAccountsHeld => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankIdAccountIds)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[AccountHeld]](callContext))        
  }
          
  messageDocs += checkBankAccountExistsDoc
  def checkBankAccountExistsDoc = MessageDoc(
    process = "obp.checkBankAccountExists",
    messageFormat = messageFormat,
    description = "Check Bank Account Exists",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCheckBankAccountExists").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCheckBankAccountExists").request),
    exampleOutboundMessage = (
     OutBoundCheckBankAccountExists(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value))
    ),
    exampleInboundMessage = (
     InBoundCheckBankAccountExists(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def checkBankAccountExists(bankId: BankId, accountId: AccountId, callContext: Option[CallContext]): OBPReturnType[Box[BankAccount]] = {
        import com.openbankproject.commons.dto.{InBoundCheckBankAccountExists => InBound, OutBoundCheckBankAccountExists => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, accountId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[BankAccountCommons](callContext))        
  }
          
  messageDocs += getCounterpartyTraitDoc
  def getCounterpartyTraitDoc = MessageDoc(
    process = "obp.getCounterpartyTrait",
    messageFormat = messageFormat,
    description = "Get Counterparty Trait",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCounterpartyTrait").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCounterpartyTrait").request),
    exampleOutboundMessage = (
     OutBoundGetCounterpartyTrait(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value),
      couterpartyId="string")
    ),
    exampleInboundMessage = (
     InBoundGetCounterpartyTrait(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= CounterpartyTraitCommons(createdByUserId=createdByUserIdExample.value,
      name=nameExample.value,
      description=descriptionExample.value,
      currency=currencyExample.value,
      thisBankId=thisBankIdExample.value,
      thisAccountId=thisAccountIdExample.value,
      thisViewId=thisViewIdExample.value,
      counterpartyId=counterpartyIdExample.value,
      otherAccountRoutingScheme=otherAccountRoutingSchemeExample.value,
      otherAccountRoutingAddress=otherAccountRoutingAddressExample.value,
      otherAccountSecondaryRoutingScheme=otherAccountSecondaryRoutingSchemeExample.value,
      otherAccountSecondaryRoutingAddress=otherAccountSecondaryRoutingAddressExample.value,
      otherBankRoutingScheme=otherBankRoutingSchemeExample.value,
      otherBankRoutingAddress=otherBankRoutingAddressExample.value,
      otherBranchRoutingScheme=otherBranchRoutingSchemeExample.value,
      otherBranchRoutingAddress=otherBranchRoutingAddressExample.value,
      isBeneficiary=isBeneficiaryExample.value.toBoolean,
      bespoke=List( CounterpartyBespoke(key=keyExample.value,
      value=valueExample.value))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getCounterpartyTrait(bankId: BankId, accountId: AccountId, couterpartyId: String, callContext: Option[CallContext]): OBPReturnType[Box[CounterpartyTrait]] = {
        import com.openbankproject.commons.dto.{InBoundGetCounterpartyTrait => InBound, OutBoundGetCounterpartyTrait => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, accountId, couterpartyId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[CounterpartyTraitCommons](callContext))        
  }
          
  messageDocs += getCounterpartyByCounterpartyIdDoc
  def getCounterpartyByCounterpartyIdDoc = MessageDoc(
    process = "obp.getCounterpartyByCounterpartyId",
    messageFormat = messageFormat,
    description = "Get Counterparty By Counterparty Id",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCounterpartyByCounterpartyId").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCounterpartyByCounterpartyId").request),
    exampleOutboundMessage = (
     OutBoundGetCounterpartyByCounterpartyId(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      counterpartyId=CounterpartyId(counterpartyIdExample.value))
    ),
    exampleInboundMessage = (
     InBoundGetCounterpartyByCounterpartyId(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= CounterpartyTraitCommons(createdByUserId=createdByUserIdExample.value,
      name=nameExample.value,
      description=descriptionExample.value,
      currency=currencyExample.value,
      thisBankId=thisBankIdExample.value,
      thisAccountId=thisAccountIdExample.value,
      thisViewId=thisViewIdExample.value,
      counterpartyId=counterpartyIdExample.value,
      otherAccountRoutingScheme=otherAccountRoutingSchemeExample.value,
      otherAccountRoutingAddress=otherAccountRoutingAddressExample.value,
      otherAccountSecondaryRoutingScheme=otherAccountSecondaryRoutingSchemeExample.value,
      otherAccountSecondaryRoutingAddress=otherAccountSecondaryRoutingAddressExample.value,
      otherBankRoutingScheme=otherBankRoutingSchemeExample.value,
      otherBankRoutingAddress=otherBankRoutingAddressExample.value,
      otherBranchRoutingScheme=otherBranchRoutingSchemeExample.value,
      otherBranchRoutingAddress=otherBranchRoutingAddressExample.value,
      isBeneficiary=isBeneficiaryExample.value.toBoolean,
      bespoke=List( CounterpartyBespoke(key=keyExample.value,
      value=valueExample.value))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getCounterpartyByCounterpartyId(counterpartyId: CounterpartyId, callContext: Option[CallContext]): OBPReturnType[Box[CounterpartyTrait]] = {
        import com.openbankproject.commons.dto.{InBoundGetCounterpartyByCounterpartyId => InBound, OutBoundGetCounterpartyByCounterpartyId => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, counterpartyId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[CounterpartyTraitCommons](callContext))        
  }
          
  messageDocs += getCounterpartyByIbanDoc
  def getCounterpartyByIbanDoc = MessageDoc(
    process = "obp.getCounterpartyByIban",
    messageFormat = messageFormat,
    description = "Get Counterparty By Iban",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCounterpartyByIban").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCounterpartyByIban").request),
    exampleOutboundMessage = (
     OutBoundGetCounterpartyByIban(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      iban=ibanExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetCounterpartyByIban(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= CounterpartyTraitCommons(createdByUserId=createdByUserIdExample.value,
      name=nameExample.value,
      description=descriptionExample.value,
      currency=currencyExample.value,
      thisBankId=thisBankIdExample.value,
      thisAccountId=thisAccountIdExample.value,
      thisViewId=thisViewIdExample.value,
      counterpartyId=counterpartyIdExample.value,
      otherAccountRoutingScheme=otherAccountRoutingSchemeExample.value,
      otherAccountRoutingAddress=otherAccountRoutingAddressExample.value,
      otherAccountSecondaryRoutingScheme=otherAccountSecondaryRoutingSchemeExample.value,
      otherAccountSecondaryRoutingAddress=otherAccountSecondaryRoutingAddressExample.value,
      otherBankRoutingScheme=otherBankRoutingSchemeExample.value,
      otherBankRoutingAddress=otherBankRoutingAddressExample.value,
      otherBranchRoutingScheme=otherBranchRoutingSchemeExample.value,
      otherBranchRoutingAddress=otherBranchRoutingAddressExample.value,
      isBeneficiary=isBeneficiaryExample.value.toBoolean,
      bespoke=List( CounterpartyBespoke(key=keyExample.value,
      value=valueExample.value))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getCounterpartyByIban(iban: String, callContext: Option[CallContext]): OBPReturnType[Box[CounterpartyTrait]] = {
        import com.openbankproject.commons.dto.{InBoundGetCounterpartyByIban => InBound, OutBoundGetCounterpartyByIban => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, iban)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[CounterpartyTraitCommons](callContext))        
  }
          
  messageDocs += getCounterpartyByIbanAndBankAccountIdDoc
  def getCounterpartyByIbanAndBankAccountIdDoc = MessageDoc(
    process = "obp.getCounterpartyByIbanAndBankAccountId",
    messageFormat = messageFormat,
    description = "Get Counterparty By Iban And Bank Account Id",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCounterpartyByIbanAndBankAccountId").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCounterpartyByIbanAndBankAccountId").request),
    exampleOutboundMessage = (
     OutBoundGetCounterpartyByIbanAndBankAccountId(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      iban=ibanExample.value,
      bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value))
    ),
    exampleInboundMessage = (
     InBoundGetCounterpartyByIbanAndBankAccountId(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= CounterpartyTraitCommons(createdByUserId=createdByUserIdExample.value,
      name=nameExample.value,
      description=descriptionExample.value,
      currency=currencyExample.value,
      thisBankId=thisBankIdExample.value,
      thisAccountId=thisAccountIdExample.value,
      thisViewId=thisViewIdExample.value,
      counterpartyId=counterpartyIdExample.value,
      otherAccountRoutingScheme=otherAccountRoutingSchemeExample.value,
      otherAccountRoutingAddress=otherAccountRoutingAddressExample.value,
      otherAccountSecondaryRoutingScheme=otherAccountSecondaryRoutingSchemeExample.value,
      otherAccountSecondaryRoutingAddress=otherAccountSecondaryRoutingAddressExample.value,
      otherBankRoutingScheme=otherBankRoutingSchemeExample.value,
      otherBankRoutingAddress=otherBankRoutingAddressExample.value,
      otherBranchRoutingScheme=otherBranchRoutingSchemeExample.value,
      otherBranchRoutingAddress=otherBranchRoutingAddressExample.value,
      isBeneficiary=isBeneficiaryExample.value.toBoolean,
      bespoke=List( CounterpartyBespoke(key=keyExample.value,
      value=valueExample.value))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getCounterpartyByIbanAndBankAccountId(iban: String, bankId: BankId, accountId: AccountId, callContext: Option[CallContext]): OBPReturnType[Box[CounterpartyTrait]] = {
        import com.openbankproject.commons.dto.{InBoundGetCounterpartyByIbanAndBankAccountId => InBound, OutBoundGetCounterpartyByIbanAndBankAccountId => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, iban, bankId, accountId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[CounterpartyTraitCommons](callContext))        
  }
          
  messageDocs += getCounterpartiesDoc
  def getCounterpartiesDoc = MessageDoc(
    process = "obp.getCounterparties",
    messageFormat = messageFormat,
    description = "Get Counterparties",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCounterparties").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCounterparties").request),
    exampleOutboundMessage = (
     OutBoundGetCounterparties(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      thisBankId=BankId(thisBankIdExample.value),
      thisAccountId=AccountId(thisAccountIdExample.value),
      viewId=ViewId(viewIdExample.value))
    ),
    exampleInboundMessage = (
     InBoundGetCounterparties(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( CounterpartyTraitCommons(createdByUserId=createdByUserIdExample.value,
      name=nameExample.value,
      description=descriptionExample.value,
      currency=currencyExample.value,
      thisBankId=thisBankIdExample.value,
      thisAccountId=thisAccountIdExample.value,
      thisViewId=thisViewIdExample.value,
      counterpartyId=counterpartyIdExample.value,
      otherAccountRoutingScheme=otherAccountRoutingSchemeExample.value,
      otherAccountRoutingAddress=otherAccountRoutingAddressExample.value,
      otherAccountSecondaryRoutingScheme=otherAccountSecondaryRoutingSchemeExample.value,
      otherAccountSecondaryRoutingAddress=otherAccountSecondaryRoutingAddressExample.value,
      otherBankRoutingScheme=otherBankRoutingSchemeExample.value,
      otherBankRoutingAddress=otherBankRoutingAddressExample.value,
      otherBranchRoutingScheme=otherBranchRoutingSchemeExample.value,
      otherBranchRoutingAddress=otherBranchRoutingAddressExample.value,
      isBeneficiary=isBeneficiaryExample.value.toBoolean,
      bespoke=List( CounterpartyBespoke(key=keyExample.value,
      value=valueExample.value)))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getCounterparties(thisBankId: BankId, thisAccountId: AccountId, viewId: ViewId, callContext: Option[CallContext]): OBPReturnType[Box[List[CounterpartyTrait]]] = {
        import com.openbankproject.commons.dto.{InBoundGetCounterparties => InBound, OutBoundGetCounterparties => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, thisBankId, thisAccountId, viewId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[CounterpartyTraitCommons]](callContext))        
  }
          
  messageDocs += getTransactionsDoc
  def getTransactionsDoc = MessageDoc(
    process = "obp.getTransactions",
    messageFormat = messageFormat,
    description = "Get Transactions",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetTransactions").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetTransactions").request),
    exampleOutboundMessage = (
     OutBoundGetTransactions(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value),
      limit=limitExample.value.toInt,
      offset=offsetExample.value.toInt,
      fromDate=outBoundGetTransactionsFromDateExample.value,
      toDate=outBoundGetTransactionsToDateExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetTransactions(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( Transaction(uuid=transactionUuidExample.value,
      id=TransactionId(transactionIdExample.value),
      thisAccount= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))),
      otherAccount= Counterparty(nationalIdentifier=counterpartyNationalIdentifierExample.value,
      kind=counterpartyKindExample.value,
      counterpartyId=counterpartyIdExample.value,
      counterpartyName=counterpartyNameExample.value,
      thisBankId=BankId(thisBankIdExample.value),
      thisAccountId=AccountId(thisAccountIdExample.value),
      otherBankRoutingScheme=counterpartyOtherBankRoutingSchemeExample.value,
      otherBankRoutingAddress=Some(counterpartyOtherBankRoutingAddressExample.value),
      otherAccountRoutingScheme=counterpartyOtherAccountRoutingSchemeExample.value,
      otherAccountRoutingAddress=Some(counterpartyOtherAccountRoutingAddressExample.value),
      otherAccountProvider=counterpartyOtherAccountProviderExample.value,
      isBeneficiary=isBeneficiaryExample.value.toBoolean),
      transactionType=transactionTypeExample.value,
      amount=BigDecimal(transactionAmountExample.value),
      currency=currencyExample.value,
      description=Some(transactionDescriptionExample.value),
      startDate=toDate(transactionStartDateExample),
      finishDate=toDate(transactionFinishDateExample),
      balance=BigDecimal(balanceExample.value))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getTransactions(bankId: BankId, accountId: AccountId, callContext: Option[CallContext], queryParams: List[OBPQueryParam]): OBPReturnType[Box[List[Transaction]]] = {
        import com.openbankproject.commons.dto.{InBoundGetTransactions => InBound, OutBoundGetTransactions => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, accountId, OBPQueryParam.getLimit(queryParams), OBPQueryParam.getOffset(queryParams), OBPQueryParam.getFromDate(queryParams), OBPQueryParam.getToDate(queryParams))
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[Transaction]](callContext))        
  }
          
  messageDocs += getTransactionsCoreDoc
  def getTransactionsCoreDoc = MessageDoc(
    process = "obp.getTransactionsCore",
    messageFormat = messageFormat,
    description = "Get Transactions Core",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetTransactionsCore").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetTransactionsCore").request),
    exampleOutboundMessage = (
     OutBoundGetTransactionsCore(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value),
      limit=limitExample.value.toInt,
      offset=offsetExample.value.toInt,
      fromDate=fromDateExample.value,
      toDate=toDateExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetTransactionsCore(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( TransactionCore(id=TransactionId(idExample.value),
      thisAccount= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))),
      otherAccount= CounterpartyCore(kind=kindExample.value,
      counterpartyId=counterpartyIdExample.value,
      counterpartyName=counterpartyNameExample.value,
      thisBankId=BankId(thisBankIdExample.value),
      thisAccountId=AccountId(thisAccountIdExample.value),
      otherBankRoutingScheme=otherBankRoutingSchemeExample.value,
      otherBankRoutingAddress=Some(otherBankRoutingAddressExample.value),
      otherAccountRoutingScheme=otherAccountRoutingSchemeExample.value,
      otherAccountRoutingAddress=Some(otherAccountRoutingAddressExample.value),
      otherAccountProvider=otherAccountProviderExample.value,
      isBeneficiary=isBeneficiaryExample.value.toBoolean),
      transactionType=transactionTypeExample.value,
      amount=BigDecimal(amountExample.value),
      currency=currencyExample.value,
      description=Some(descriptionExample.value),
      startDate=toDate(startDateExample),
      finishDate=toDate(finishDateExample),
      balance=BigDecimal(balanceExample.value))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getTransactionsCore(bankId: BankId, accountId: AccountId, queryParams: List[OBPQueryParam], callContext: Option[CallContext]): OBPReturnType[Box[List[TransactionCore]]] = {
        import com.openbankproject.commons.dto.{InBoundGetTransactionsCore => InBound, OutBoundGetTransactionsCore => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, accountId, OBPQueryParam.getLimit(queryParams), OBPQueryParam.getOffset(queryParams), OBPQueryParam.getFromDate(queryParams), OBPQueryParam.getToDate(queryParams))
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[TransactionCore]](callContext))        
  }
          
  messageDocs += getTransactionDoc
  def getTransactionDoc = MessageDoc(
    process = "obp.getTransaction",
    messageFormat = messageFormat,
    description = "Get Transaction",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetTransaction").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetTransaction").request),
    exampleOutboundMessage = (
     OutBoundGetTransaction(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value),
      transactionId=TransactionId(transactionIdExample.value))
    ),
    exampleInboundMessage = (
     InBoundGetTransaction(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= Transaction(uuid=transactionUuidExample.value,
      id=TransactionId(transactionIdExample.value),
      thisAccount= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))),
      otherAccount= Counterparty(nationalIdentifier=counterpartyNationalIdentifierExample.value,
      kind=counterpartyKindExample.value,
      counterpartyId=counterpartyIdExample.value,
      counterpartyName=counterpartyNameExample.value,
      thisBankId=BankId(thisBankIdExample.value),
      thisAccountId=AccountId(thisAccountIdExample.value),
      otherBankRoutingScheme=counterpartyOtherBankRoutingSchemeExample.value,
      otherBankRoutingAddress=Some(counterpartyOtherBankRoutingAddressExample.value),
      otherAccountRoutingScheme=counterpartyOtherAccountRoutingSchemeExample.value,
      otherAccountRoutingAddress=Some(counterpartyOtherAccountRoutingAddressExample.value),
      otherAccountProvider=counterpartyOtherAccountProviderExample.value,
      isBeneficiary=isBeneficiaryExample.value.toBoolean),
      transactionType=transactionTypeExample.value,
      amount=BigDecimal(transactionAmountExample.value),
      currency=currencyExample.value,
      description=Some(transactionDescriptionExample.value),
      startDate=toDate(transactionStartDateExample),
      finishDate=toDate(transactionFinishDateExample),
      balance=BigDecimal(balanceExample.value)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getTransaction(bankId: BankId, accountId: AccountId, transactionId: TransactionId, callContext: Option[CallContext]): OBPReturnType[Box[Transaction]] = {
        import com.openbankproject.commons.dto.{InBoundGetTransaction => InBound, OutBoundGetTransaction => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, accountId, transactionId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[Transaction](callContext))        
  }
          
  messageDocs += getPhysicalCardsForUserDoc
  def getPhysicalCardsForUserDoc = MessageDoc(
    process = "obp.getPhysicalCardsForUser",
    messageFormat = messageFormat,
    description = "Get Physical Cards For User",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetPhysicalCardsForUser").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetPhysicalCardsForUser").request),
    exampleOutboundMessage = (
     OutBoundGetPhysicalCardsForUser(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      user= UserCommons(userPrimaryKey=UserPrimaryKey(123),
      userId=userIdExample.value,
      idGivenByProvider="string",
      provider=providerExample.value,
      emailAddress=emailAddressExample.value,
      name=userNameExample.value,
      createdByConsentId=Some("string"),
      createdByUserInvitationId=Some("string"),
      isDeleted=Some(true),
      lastMarketingAgreementSignedDate=Some(toDate(dateExample))))
    ),
    exampleInboundMessage = (
     InBoundGetPhysicalCardsForUser(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( PhysicalCard(cardId=cardIdExample.value,
      bankId=bankIdExample.value,
      bankCardNumber=bankCardNumberExample.value,
      cardType=cardTypeExample.value,
      nameOnCard=nameOnCardExample.value,
      issueNumber=issueNumberExample.value,
      serialNumber=serialNumberExample.value,
      validFrom=toDate(validFromExample),
      expires=toDate(expiresDateExample),
      enabled=enabledExample.value.toBoolean,
      cancelled=cancelledExample.value.toBoolean,
      onHotList=onHotListExample.value.toBoolean,
      technology=technologyExample.value,
      networks=networksExample.value.replace("[","").replace("]","").split(",").toList,
      allows=List(com.openbankproject.commons.model.CardAction.DEBIT),
      account= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=accountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))),
      replacement=Some( CardReplacementInfo(requestedDate=toDate(requestedDateExample),
      reasonRequested=com.openbankproject.commons.model.CardReplacementReason.FIRST)),
      pinResets=List( PinResetInfo(requestedDate=toDate(requestedDateExample),
      reasonRequested=com.openbankproject.commons.model.PinResetReason.FORGOT)),
      collected=Some(CardCollectionInfo(toDate(collectedExample))),
      posted=Some(CardPostedInfo(toDate(postedExample))),
      customerId=customerIdExample.value,
      cvv=Some(cvvExample.value),
      brand=Some(brandExample.value))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getPhysicalCardsForUser(user: User, callContext: Option[CallContext]): OBPReturnType[Box[List[PhysicalCard]]] = {
        import com.openbankproject.commons.dto.{InBoundGetPhysicalCardsForUser => InBound, OutBoundGetPhysicalCardsForUser => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, user)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[PhysicalCard]](callContext))        
  }
          
  messageDocs += getPhysicalCardForBankDoc
  def getPhysicalCardForBankDoc = MessageDoc(
    process = "obp.getPhysicalCardForBank",
    messageFormat = messageFormat,
    description = "Get Physical Card For Bank",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetPhysicalCardForBank").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetPhysicalCardForBank").request),
    exampleOutboundMessage = (
     OutBoundGetPhysicalCardForBank(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      cardId=cardIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetPhysicalCardForBank(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= PhysicalCard(cardId=cardIdExample.value,
      bankId=bankIdExample.value,
      bankCardNumber=bankCardNumberExample.value,
      cardType=cardTypeExample.value,
      nameOnCard=nameOnCardExample.value,
      issueNumber=issueNumberExample.value,
      serialNumber=serialNumberExample.value,
      validFrom=toDate(validFromExample),
      expires=toDate(expiresDateExample),
      enabled=enabledExample.value.toBoolean,
      cancelled=cancelledExample.value.toBoolean,
      onHotList=onHotListExample.value.toBoolean,
      technology=technologyExample.value,
      networks=networksExample.value.replace("[","").replace("]","").split(",").toList,
      allows=List(com.openbankproject.commons.model.CardAction.DEBIT),
      account= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=accountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))),
      replacement=Some( CardReplacementInfo(requestedDate=toDate(requestedDateExample),
      reasonRequested=com.openbankproject.commons.model.CardReplacementReason.FIRST)),
      pinResets=List( PinResetInfo(requestedDate=toDate(requestedDateExample),
      reasonRequested=com.openbankproject.commons.model.PinResetReason.FORGOT)),
      collected=Some(CardCollectionInfo(toDate(collectedExample))),
      posted=Some(CardPostedInfo(toDate(postedExample))),
      customerId=customerIdExample.value,
      cvv=Some(cvvExample.value),
      brand=Some(brandExample.value)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getPhysicalCardForBank(bankId: BankId, cardId: String, callContext: Option[CallContext]): OBPReturnType[Box[PhysicalCardTrait]] = {
        import com.openbankproject.commons.dto.{InBoundGetPhysicalCardForBank => InBound, OutBoundGetPhysicalCardForBank => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, cardId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[PhysicalCard](callContext))        
  }
          
  messageDocs += deletePhysicalCardForBankDoc
  def deletePhysicalCardForBankDoc = MessageDoc(
    process = "obp.deletePhysicalCardForBank",
    messageFormat = messageFormat,
    description = "Delete Physical Card For Bank",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundDeletePhysicalCardForBank").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundDeletePhysicalCardForBank").request),
    exampleOutboundMessage = (
     OutBoundDeletePhysicalCardForBank(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      cardId=cardIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundDeletePhysicalCardForBank(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=true)
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def deletePhysicalCardForBank(bankId: BankId, cardId: String, callContext: Option[CallContext]): OBPReturnType[Box[Boolean]] = {
        import com.openbankproject.commons.dto.{InBoundDeletePhysicalCardForBank => InBound, OutBoundDeletePhysicalCardForBank => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, cardId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[Boolean](callContext))        
  }
          
  messageDocs += getPhysicalCardsForBankDoc
  def getPhysicalCardsForBankDoc = MessageDoc(
    process = "obp.getPhysicalCardsForBank",
    messageFormat = messageFormat,
    description = "Get Physical Cards For Bank",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetPhysicalCardsForBank").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetPhysicalCardsForBank").request),
    exampleOutboundMessage = (
     OutBoundGetPhysicalCardsForBank(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bank= BankCommons(bankId=BankId(bankIdExample.value),
      shortName=bankShortNameExample.value,
      fullName=bankFullNameExample.value,
      logoUrl=bankLogoUrlExample.value,
      websiteUrl=bankWebsiteUrlExample.value,
      bankRoutingScheme=bankRoutingSchemeExample.value,
      bankRoutingAddress=bankRoutingAddressExample.value,
      swiftBic=bankSwiftBicExample.value,
      nationalIdentifier=bankNationalIdentifierExample.value),
      user= UserCommons(userPrimaryKey=UserPrimaryKey(123),
      userId=userIdExample.value,
      idGivenByProvider="string",
      provider=providerExample.value,
      emailAddress=emailAddressExample.value,
      name=userNameExample.value,
      createdByConsentId=Some("string"),
      createdByUserInvitationId=Some("string"),
      isDeleted=Some(true),
      lastMarketingAgreementSignedDate=Some(toDate(dateExample))),
      limit=limitExample.value.toInt,
      offset=offsetExample.value.toInt,
      fromDate=fromDateExample.value,
      toDate=toDateExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetPhysicalCardsForBank(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( PhysicalCard(cardId=cardIdExample.value,
      bankId=bankIdExample.value,
      bankCardNumber=bankCardNumberExample.value,
      cardType=cardTypeExample.value,
      nameOnCard=nameOnCardExample.value,
      issueNumber=issueNumberExample.value,
      serialNumber=serialNumberExample.value,
      validFrom=toDate(validFromExample),
      expires=toDate(expiresDateExample),
      enabled=enabledExample.value.toBoolean,
      cancelled=cancelledExample.value.toBoolean,
      onHotList=onHotListExample.value.toBoolean,
      technology=technologyExample.value,
      networks=networksExample.value.replace("[","").replace("]","").split(",").toList,
      allows=List(com.openbankproject.commons.model.CardAction.DEBIT),
      account= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=accountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))),
      replacement=Some( CardReplacementInfo(requestedDate=toDate(requestedDateExample),
      reasonRequested=com.openbankproject.commons.model.CardReplacementReason.FIRST)),
      pinResets=List( PinResetInfo(requestedDate=toDate(requestedDateExample),
      reasonRequested=com.openbankproject.commons.model.PinResetReason.FORGOT)),
      collected=Some(CardCollectionInfo(toDate(collectedExample))),
      posted=Some(CardPostedInfo(toDate(postedExample))),
      customerId=customerIdExample.value,
      cvv=Some(cvvExample.value),
      brand=Some(brandExample.value))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getPhysicalCardsForBank(bank: Bank, user: User, queryParams: List[OBPQueryParam], callContext: Option[CallContext]): OBPReturnType[Box[List[PhysicalCard]]] = {
        import com.openbankproject.commons.dto.{InBoundGetPhysicalCardsForBank => InBound, OutBoundGetPhysicalCardsForBank => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bank, user, OBPQueryParam.getLimit(queryParams), OBPQueryParam.getOffset(queryParams), OBPQueryParam.getFromDate(queryParams), OBPQueryParam.getToDate(queryParams))
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[PhysicalCard]](callContext))        
  }
          
  messageDocs += createPhysicalCardDoc
  def createPhysicalCardDoc = MessageDoc(
    process = "obp.createPhysicalCard",
    messageFormat = messageFormat,
    description = "Create Physical Card",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreatePhysicalCard").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreatePhysicalCard").request),
    exampleOutboundMessage = (
     OutBoundCreatePhysicalCard(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankCardNumber=bankCardNumberExample.value,
      nameOnCard=nameOnCardExample.value,
      cardType=cardTypeExample.value,
      issueNumber=issueNumberExample.value,
      serialNumber=serialNumberExample.value,
      validFrom=toDate(validFromExample),
      expires=toDate(expiresDateExample),
      enabled=enabledExample.value.toBoolean,
      cancelled=cancelledExample.value.toBoolean,
      onHotList=onHotListExample.value.toBoolean,
      technology=technologyExample.value,
      networks=networksExample.value.replace("[","").replace("]","").split(",").toList,
      allows=allowsExample.value.replace("[","").replace("]","").split(",").toList,
      accountId=accountIdExample.value,
      bankId=bankIdExample.value,
      replacement=Some( CardReplacementInfo(requestedDate=toDate(requestedDateExample),
      reasonRequested=com.openbankproject.commons.model.CardReplacementReason.FIRST)),
      pinResets=List( PinResetInfo(requestedDate=toDate(requestedDateExample),
      reasonRequested=com.openbankproject.commons.model.PinResetReason.FORGOT)),
      collected=Some(CardCollectionInfo(toDate(collectedExample))),
      posted=Some(CardPostedInfo(toDate(postedExample))),
      customerId=customerIdExample.value,
      cvv=cvvExample.value,
      brand=brandExample.value)
    ),
    exampleInboundMessage = (
     InBoundCreatePhysicalCard(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= PhysicalCard(cardId=cardIdExample.value,
      bankId=bankIdExample.value,
      bankCardNumber=bankCardNumberExample.value,
      cardType=cardTypeExample.value,
      nameOnCard=nameOnCardExample.value,
      issueNumber=issueNumberExample.value,
      serialNumber=serialNumberExample.value,
      validFrom=toDate(validFromExample),
      expires=toDate(expiresDateExample),
      enabled=enabledExample.value.toBoolean,
      cancelled=cancelledExample.value.toBoolean,
      onHotList=onHotListExample.value.toBoolean,
      technology=technologyExample.value,
      networks=networksExample.value.replace("[","").replace("]","").split(",").toList,
      allows=List(com.openbankproject.commons.model.CardAction.DEBIT),
      account= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=accountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))),
      replacement=Some( CardReplacementInfo(requestedDate=toDate(requestedDateExample),
      reasonRequested=com.openbankproject.commons.model.CardReplacementReason.FIRST)),
      pinResets=List( PinResetInfo(requestedDate=toDate(requestedDateExample),
      reasonRequested=com.openbankproject.commons.model.PinResetReason.FORGOT)),
      collected=Some(CardCollectionInfo(toDate(collectedExample))),
      posted=Some(CardPostedInfo(toDate(postedExample))),
      customerId=customerIdExample.value,
      cvv=Some(cvvExample.value),
      brand=Some(brandExample.value)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createPhysicalCard(bankCardNumber: String, nameOnCard: String, cardType: String, issueNumber: String, serialNumber: String, validFrom: Date, expires: Date, enabled: Boolean, cancelled: Boolean, onHotList: Boolean, technology: String, networks: List[String], allows: List[String], accountId: String, bankId: String, replacement: Option[CardReplacementInfo], pinResets: List[PinResetInfo], collected: Option[CardCollectionInfo], posted: Option[CardPostedInfo], customerId: String, cvv: String, brand: String, callContext: Option[CallContext]): OBPReturnType[Box[PhysicalCard]] = {
        import com.openbankproject.commons.dto.{InBoundCreatePhysicalCard => InBound, OutBoundCreatePhysicalCard => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankCardNumber, nameOnCard, cardType, issueNumber, serialNumber, validFrom, expires, enabled, cancelled, onHotList, technology, networks, allows, accountId, bankId, replacement, pinResets, collected, posted, customerId, cvv, brand)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[PhysicalCard](callContext))        
  }
          
  messageDocs += updatePhysicalCardDoc
  def updatePhysicalCardDoc = MessageDoc(
    process = "obp.updatePhysicalCard",
    messageFormat = messageFormat,
    description = "Update Physical Card",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundUpdatePhysicalCard").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundUpdatePhysicalCard").request),
    exampleOutboundMessage = (
     OutBoundUpdatePhysicalCard(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      cardId=cardIdExample.value,
      bankCardNumber=bankCardNumberExample.value,
      nameOnCard=nameOnCardExample.value,
      cardType=cardTypeExample.value,
      issueNumber=issueNumberExample.value,
      serialNumber=serialNumberExample.value,
      validFrom=toDate(validFromExample),
      expires=toDate(expiresDateExample),
      enabled=enabledExample.value.toBoolean,
      cancelled=cancelledExample.value.toBoolean,
      onHotList=onHotListExample.value.toBoolean,
      technology=technologyExample.value,
      networks=networksExample.value.replace("[","").replace("]","").split(",").toList,
      allows=allowsExample.value.replace("[","").replace("]","").split(",").toList,
      accountId=accountIdExample.value,
      bankId=bankIdExample.value,
      replacement=Some( CardReplacementInfo(requestedDate=toDate(requestedDateExample),
      reasonRequested=com.openbankproject.commons.model.CardReplacementReason.FIRST)),
      pinResets=List( PinResetInfo(requestedDate=toDate(requestedDateExample),
      reasonRequested=com.openbankproject.commons.model.PinResetReason.FORGOT)),
      collected=Some(CardCollectionInfo(toDate(collectedExample))),
      posted=Some(CardPostedInfo(toDate(postedExample))),
      customerId=customerIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundUpdatePhysicalCard(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= PhysicalCard(cardId=cardIdExample.value,
      bankId=bankIdExample.value,
      bankCardNumber=bankCardNumberExample.value,
      cardType=cardTypeExample.value,
      nameOnCard=nameOnCardExample.value,
      issueNumber=issueNumberExample.value,
      serialNumber=serialNumberExample.value,
      validFrom=toDate(validFromExample),
      expires=toDate(expiresDateExample),
      enabled=enabledExample.value.toBoolean,
      cancelled=cancelledExample.value.toBoolean,
      onHotList=onHotListExample.value.toBoolean,
      technology=technologyExample.value,
      networks=networksExample.value.replace("[","").replace("]","").split(",").toList,
      allows=List(com.openbankproject.commons.model.CardAction.DEBIT),
      account= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=accountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))),
      replacement=Some( CardReplacementInfo(requestedDate=toDate(requestedDateExample),
      reasonRequested=com.openbankproject.commons.model.CardReplacementReason.FIRST)),
      pinResets=List( PinResetInfo(requestedDate=toDate(requestedDateExample),
      reasonRequested=com.openbankproject.commons.model.PinResetReason.FORGOT)),
      collected=Some(CardCollectionInfo(toDate(collectedExample))),
      posted=Some(CardPostedInfo(toDate(postedExample))),
      customerId=customerIdExample.value,
      cvv=Some(cvvExample.value),
      brand=Some(brandExample.value)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def updatePhysicalCard(cardId: String, bankCardNumber: String, nameOnCard: String, cardType: String, issueNumber: String, serialNumber: String, validFrom: Date, expires: Date, enabled: Boolean, cancelled: Boolean, onHotList: Boolean, technology: String, networks: List[String], allows: List[String], accountId: String, bankId: String, replacement: Option[CardReplacementInfo], pinResets: List[PinResetInfo], collected: Option[CardCollectionInfo], posted: Option[CardPostedInfo], customerId: String, callContext: Option[CallContext]): OBPReturnType[Box[PhysicalCardTrait]] = {
        import com.openbankproject.commons.dto.{InBoundUpdatePhysicalCard => InBound, OutBoundUpdatePhysicalCard => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, cardId, bankCardNumber, nameOnCard, cardType, issueNumber, serialNumber, validFrom, expires, enabled, cancelled, onHotList, technology, networks, allows, accountId, bankId, replacement, pinResets, collected, posted, customerId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[PhysicalCard](callContext))        
  }
          
  messageDocs += makePaymentv210Doc
  def makePaymentv210Doc = MessageDoc(
    process = "obp.makePaymentv210",
    messageFormat = messageFormat,
    description = "Make Paymentv210",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundMakePaymentv210").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundMakePaymentv210").request),
    exampleOutboundMessage = (
     OutBoundMakePaymentv210(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      fromAccount= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))),
      toAccount= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))),
      transactionRequestId=TransactionRequestId(transactionRequestIdExample.value),
      transactionRequestCommonBody= TransactionRequestCommonBodyJSONCommons(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value),
      amount=BigDecimal(amountExample.value),
      description=descriptionExample.value,
      transactionRequestType=TransactionRequestType(transactionRequestTypeExample.value),
      chargePolicy=chargePolicyExample.value)
    ),
    exampleInboundMessage = (
     InBoundMakePaymentv210(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=TransactionId(transactionIdExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def makePaymentv210(fromAccount: BankAccount, toAccount: BankAccount, transactionRequestId: TransactionRequestId, transactionRequestCommonBody: TransactionRequestCommonBodyJSON, amount: BigDecimal, description: String, transactionRequestType: TransactionRequestType, chargePolicy: String, callContext: Option[CallContext]): OBPReturnType[Box[TransactionId]] = {
        import com.openbankproject.commons.dto.{InBoundMakePaymentv210 => InBound, OutBoundMakePaymentv210 => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, fromAccount, toAccount, transactionRequestId, transactionRequestCommonBody, amount, description, transactionRequestType, chargePolicy)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[TransactionId](callContext))        
  }
          
  messageDocs += getChargeValueDoc
  def getChargeValueDoc = MessageDoc(
    process = "obp.getChargeValue",
    messageFormat = messageFormat,
    description = "Get Charge Value",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetChargeValue").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetChargeValue").request),
    exampleOutboundMessage = (
     OutBoundGetChargeValue(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      chargeLevelAmount=BigDecimal("123.321"),
      transactionRequestCommonBodyAmount=BigDecimal("123.321"))
    ),
    exampleInboundMessage = (
     InBoundGetChargeValue(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data="string")
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getChargeValue(chargeLevelAmount: BigDecimal, transactionRequestCommonBodyAmount: BigDecimal, callContext: Option[CallContext]): OBPReturnType[Box[String]] = {
        import com.openbankproject.commons.dto.{InBoundGetChargeValue => InBound, OutBoundGetChargeValue => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, chargeLevelAmount, transactionRequestCommonBodyAmount)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[String](callContext))        
  }
          
  messageDocs += createTransactionRequestv210Doc
  def createTransactionRequestv210Doc = MessageDoc(
    process = "obp.createTransactionRequestv210",
    messageFormat = messageFormat,
    description = "Create Transaction Requestv210",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateTransactionRequestv210").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateTransactionRequestv210").request),
    exampleOutboundMessage = (
     OutBoundCreateTransactionRequestv210(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      initiator= UserCommons(userPrimaryKey=UserPrimaryKey(123),
      userId=userIdExample.value,
      idGivenByProvider="string",
      provider=providerExample.value,
      emailAddress=emailAddressExample.value,
      name=userNameExample.value,
      createdByConsentId=Some("string"),
      createdByUserInvitationId=Some("string"),
      isDeleted=Some(true),
      lastMarketingAgreementSignedDate=Some(toDate(dateExample))),
      viewId=ViewId(viewIdExample.value),
      fromAccount= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))),
      toAccount= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))),
      transactionRequestType=TransactionRequestType(transactionRequestTypeExample.value),
      transactionRequestCommonBody= TransactionRequestCommonBodyJSONCommons(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value),
      detailsPlain="string",
      chargePolicy=chargePolicyExample.value,
      challengeType=Some(challengeTypeExample.value),
      scaMethod=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthentication.SMS))
    ),
    exampleInboundMessage = (
     InBoundCreateTransactionRequestv210(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= TransactionRequest(id=TransactionRequestId(transactionRequestIdExample.value),
      `type`=transactionRequestTypeExample.value,
      from= TransactionRequestAccount(bank_id=bank_idExample.value,
      account_id=account_idExample.value),
      body= TransactionRequestBodyAllTypes(to_sandbox_tan=Some( TransactionRequestAccount(bank_id=bank_idExample.value,
      account_id=account_idExample.value)),
      to_sepa=Some(TransactionRequestIban(transactionRequestIban.value)),
      to_counterparty=Some(TransactionRequestCounterpartyId(transactionRequestCounterpartyIdExample.value)),
      to_simple=Some( TransactionRequestSimple(otherBankRoutingScheme=otherBankRoutingSchemeExample.value,
      otherBankRoutingAddress=otherBankRoutingAddressExample.value,
      otherBranchRoutingScheme=otherBranchRoutingSchemeExample.value,
      otherBranchRoutingAddress=otherBranchRoutingAddressExample.value,
      otherAccountRoutingScheme=otherAccountRoutingSchemeExample.value,
      otherAccountRoutingAddress=otherAccountRoutingAddressExample.value,
      otherAccountSecondaryRoutingScheme=otherAccountSecondaryRoutingSchemeExample.value,
      otherAccountSecondaryRoutingAddress=otherAccountSecondaryRoutingAddressExample.value)),
      to_transfer_to_phone=Some( TransactionRequestTransferToPhone(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      message=messageExample.value,
      from= FromAccountTransfer(mobile_phone_number="string",
      nickname=nicknameExample.value),
      to=ToAccountTransferToPhone(toExample.value))),
      to_transfer_to_atm=Some( TransactionRequestTransferToAtm(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      message=messageExample.value,
      from= FromAccountTransfer(mobile_phone_number="string",
      nickname=nicknameExample.value),
      to= ToAccountTransferToAtm(legal_name="string",
      date_of_birth="string",
      mobile_phone_number="string",
      kyc_document= ToAccountTransferToAtmKycDocument(`type`=typeExample.value,
      number=numberExample.value)))),
      to_transfer_to_account=Some( TransactionRequestTransferToAccount(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      transfer_type="string",
      future_date="string",
      to= ToAccountTransferToAccount(name=nameExample.value,
      bank_code="string",
      branch_number="string",
      account= ToAccountTransferToAccountAccount(number=accountNumberExample.value,
      iban=ibanExample.value)))),
      to_sepa_credit_transfers=Some( SepaCreditTransfers(debtorAccount=PaymentAccount("string"),
      instructedAmount= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      creditorAccount=PaymentAccount("string"),
      creditorName="string")),
      value= AmountOfMoney(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value),
      transaction_ids="string",
      status=statusExample.value,
      start_date=toDate(transactionRequestStartDateExample),
      end_date=toDate(transactionRequestEndDateExample),
      challenge= TransactionRequestChallenge(id=challengeIdExample.value,
      allowed_attempts=123,
      challenge_type="string"),
      charge= TransactionRequestCharge(summary=summaryExample.value,
      value= AmountOfMoney(currency=currencyExample.value,
      amount=amountExample.value)),
      charge_policy="string",
      counterparty_id=CounterpartyId(transactionRequestCounterpartyIdExample.value),
      name=nameExample.value,
      this_bank_id=BankId(bankIdExample.value),
      this_account_id=AccountId(accountIdExample.value),
      this_view_id=ViewId(viewIdExample.value),
      other_account_routing_scheme="string",
      other_account_routing_address="string",
      other_bank_routing_scheme="string",
      other_bank_routing_address="string",
      is_beneficiary=true,
      future_date=Some("string"),
      payment_start_date=Some(toDate(dateExample)),
      payment_end_date=Some(toDate(dateExample)),
      payment_execution_Rule=Some("string"),
      payment_frequency=Some("string"),
      payment_day_of_execution=Some("string")))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createTransactionRequestv210(initiator: User, viewId: ViewId, fromAccount: BankAccount, toAccount: BankAccount, transactionRequestType: TransactionRequestType, transactionRequestCommonBody: TransactionRequestCommonBodyJSON, detailsPlain: String, chargePolicy: String, challengeType: Option[String], scaMethod: Option[StrongCustomerAuthentication.SCA], callContext: Option[CallContext]): OBPReturnType[Box[TransactionRequest]] = {
        import com.openbankproject.commons.dto.{InBoundCreateTransactionRequestv210 => InBound, OutBoundCreateTransactionRequestv210 => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, initiator, viewId, fromAccount, toAccount, transactionRequestType, transactionRequestCommonBody, detailsPlain, chargePolicy, challengeType, scaMethod)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[TransactionRequest](callContext))        
  }
          
  messageDocs += createTransactionRequestv400Doc
  def createTransactionRequestv400Doc = MessageDoc(
    process = "obp.createTransactionRequestv400",
    messageFormat = messageFormat,
    description = "Create Transaction Requestv400",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateTransactionRequestv400").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateTransactionRequestv400").request),
    exampleOutboundMessage = (
     OutBoundCreateTransactionRequestv400(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      initiator= UserCommons(userPrimaryKey=UserPrimaryKey(123),
      userId=userIdExample.value,
      idGivenByProvider="string",
      provider=providerExample.value,
      emailAddress=emailAddressExample.value,
      name=userNameExample.value,
      createdByConsentId=Some("string"),
      createdByUserInvitationId=Some("string"),
      isDeleted=Some(true),
      lastMarketingAgreementSignedDate=Some(toDate(dateExample))),
      viewId=ViewId(viewIdExample.value),
      fromAccount= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))),
      toAccount= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))),
      transactionRequestType=TransactionRequestType(transactionRequestTypeExample.value),
      transactionRequestCommonBody= TransactionRequestCommonBodyJSONCommons(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value),
      detailsPlain="string",
      chargePolicy=chargePolicyExample.value,
      challengeType=Some(challengeTypeExample.value),
      scaMethod=Some(com.openbankproject.commons.model.enums.StrongCustomerAuthentication.SMS),
      reasons=Some(List( TransactionRequestReason(code=codeExample.value,
      documentNumber=Some(documentNumberExample.value),
      amount=Some(amountExample.value),
      currency=Some(currencyExample.value),
      description=Some(descriptionExample.value)))))
    ),
    exampleInboundMessage = (
     InBoundCreateTransactionRequestv400(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= TransactionRequest(id=TransactionRequestId(transactionRequestIdExample.value),
      `type`=transactionRequestTypeExample.value,
      from= TransactionRequestAccount(bank_id=bank_idExample.value,
      account_id=account_idExample.value),
      body= TransactionRequestBodyAllTypes(to_sandbox_tan=Some( TransactionRequestAccount(bank_id=bank_idExample.value,
      account_id=account_idExample.value)),
      to_sepa=Some(TransactionRequestIban(transactionRequestIban.value)),
      to_counterparty=Some(TransactionRequestCounterpartyId(transactionRequestCounterpartyIdExample.value)),
      to_simple=Some( TransactionRequestSimple(otherBankRoutingScheme=otherBankRoutingSchemeExample.value,
      otherBankRoutingAddress=otherBankRoutingAddressExample.value,
      otherBranchRoutingScheme=otherBranchRoutingSchemeExample.value,
      otherBranchRoutingAddress=otherBranchRoutingAddressExample.value,
      otherAccountRoutingScheme=otherAccountRoutingSchemeExample.value,
      otherAccountRoutingAddress=otherAccountRoutingAddressExample.value,
      otherAccountSecondaryRoutingScheme=otherAccountSecondaryRoutingSchemeExample.value,
      otherAccountSecondaryRoutingAddress=otherAccountSecondaryRoutingAddressExample.value)),
      to_transfer_to_phone=Some( TransactionRequestTransferToPhone(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      message=messageExample.value,
      from= FromAccountTransfer(mobile_phone_number="string",
      nickname=nicknameExample.value),
      to=ToAccountTransferToPhone(toExample.value))),
      to_transfer_to_atm=Some( TransactionRequestTransferToAtm(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      message=messageExample.value,
      from= FromAccountTransfer(mobile_phone_number="string",
      nickname=nicknameExample.value),
      to= ToAccountTransferToAtm(legal_name="string",
      date_of_birth="string",
      mobile_phone_number="string",
      kyc_document= ToAccountTransferToAtmKycDocument(`type`=typeExample.value,
      number=numberExample.value)))),
      to_transfer_to_account=Some( TransactionRequestTransferToAccount(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      transfer_type="string",
      future_date="string",
      to= ToAccountTransferToAccount(name=nameExample.value,
      bank_code="string",
      branch_number="string",
      account= ToAccountTransferToAccountAccount(number=accountNumberExample.value,
      iban=ibanExample.value)))),
      to_sepa_credit_transfers=Some( SepaCreditTransfers(debtorAccount=PaymentAccount("string"),
      instructedAmount= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      creditorAccount=PaymentAccount("string"),
      creditorName="string")),
      value= AmountOfMoney(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value),
      transaction_ids="string",
      status=statusExample.value,
      start_date=toDate(transactionRequestStartDateExample),
      end_date=toDate(transactionRequestEndDateExample),
      challenge= TransactionRequestChallenge(id=challengeIdExample.value,
      allowed_attempts=123,
      challenge_type="string"),
      charge= TransactionRequestCharge(summary=summaryExample.value,
      value= AmountOfMoney(currency=currencyExample.value,
      amount=amountExample.value)),
      charge_policy="string",
      counterparty_id=CounterpartyId(transactionRequestCounterpartyIdExample.value),
      name=nameExample.value,
      this_bank_id=BankId(bankIdExample.value),
      this_account_id=AccountId(accountIdExample.value),
      this_view_id=ViewId(viewIdExample.value),
      other_account_routing_scheme="string",
      other_account_routing_address="string",
      other_bank_routing_scheme="string",
      other_bank_routing_address="string",
      is_beneficiary=true,
      future_date=Some("string"),
      payment_start_date=Some(toDate(dateExample)),
      payment_end_date=Some(toDate(dateExample)),
      payment_execution_Rule=Some("string"),
      payment_frequency=Some("string"),
      payment_day_of_execution=Some("string")))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createTransactionRequestv400(initiator: User, viewId: ViewId, fromAccount: BankAccount, toAccount: BankAccount, transactionRequestType: TransactionRequestType, transactionRequestCommonBody: TransactionRequestCommonBodyJSON, detailsPlain: String, chargePolicy: String, challengeType: Option[String], scaMethod: Option[StrongCustomerAuthentication.SCA], reasons: Option[List[TransactionRequestReason]], callContext: Option[CallContext]): OBPReturnType[Box[TransactionRequest]] = {
        import com.openbankproject.commons.dto.{InBoundCreateTransactionRequestv400 => InBound, OutBoundCreateTransactionRequestv400 => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, initiator, viewId, fromAccount, toAccount, transactionRequestType, transactionRequestCommonBody, detailsPlain, chargePolicy, challengeType, scaMethod, reasons)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[TransactionRequest](callContext))        
  }
          
  messageDocs += createTransactionRequestSepaCreditTransfersBGV1Doc
  def createTransactionRequestSepaCreditTransfersBGV1Doc = MessageDoc(
    process = "obp.createTransactionRequestSepaCreditTransfersBGV1",
    messageFormat = messageFormat,
    description = "Create Transaction Request Sepa Credit Transfers BG V1",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateTransactionRequestSepaCreditTransfersBGV1").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateTransactionRequestSepaCreditTransfersBGV1").request),
    exampleOutboundMessage = (
     OutBoundCreateTransactionRequestSepaCreditTransfersBGV1(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      initiator= UserCommons(userPrimaryKey=UserPrimaryKey(123),
      userId=userIdExample.value,
      idGivenByProvider="string",
      provider=providerExample.value,
      emailAddress=emailAddressExample.value,
      name=userNameExample.value,
      createdByConsentId=Some("string"),
      createdByUserInvitationId=Some("string"),
      isDeleted=Some(true),
      lastMarketingAgreementSignedDate=Some(toDate(dateExample))),
      paymentServiceType=com.openbankproject.commons.model.enums.PaymentServiceTypes.example,
      transactionRequestType=com.openbankproject.commons.model.enums.TransactionRequestTypes.example,
      transactionRequestBody= SepaCreditTransfersBerlinGroupV13(endToEndIdentification=Some("string"),
      instructionIdentification=Some("string"),
      debtorName=Some("string"),
      debtorAccount=PaymentAccount("string"),
      debtorId=Some("string"),
      ultimateDebtor=Some("string"),
      instructedAmount= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      currencyOfTransfer=Some("string"),
      exchangeRateInformation=Some("string"),
      creditorAccount=PaymentAccount("string"),
      creditorAgent=Some("string"),
      creditorAgentName=Some("string"),
      creditorName="string",
      creditorId=Some("string"),
      creditorAddress=Some("string"),
      creditorNameAndAddress=Some("string"),
      ultimateCreditor=Some("string"),
      purposeCode=Some("string"),
      chargeBearer=Some("string"),
      serviceLevel=Some("string"),
      remittanceInformationUnstructured=Some("string"),
      remittanceInformationUnstructuredArray=Some("string"),
      remittanceInformationStructured=Some("string"),
      remittanceInformationStructuredArray=Some("string"),
      requestedExecutionDate=Some("string"),
      requestedExecutionTime=Some("string")))
    ),
    exampleInboundMessage = (
     InBoundCreateTransactionRequestSepaCreditTransfersBGV1(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= TransactionRequestBGV1(id=TransactionRequestId(idExample.value),
      status=statusExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createTransactionRequestSepaCreditTransfersBGV1(initiator: User, paymentServiceType: PaymentServiceTypes, transactionRequestType: TransactionRequestTypes, transactionRequestBody: SepaCreditTransfersBerlinGroupV13, callContext: Option[CallContext]): OBPReturnType[Box[TransactionRequestBGV1]] = {
        import com.openbankproject.commons.dto.{InBoundCreateTransactionRequestSepaCreditTransfersBGV1 => InBound, OutBoundCreateTransactionRequestSepaCreditTransfersBGV1 => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, initiator, paymentServiceType, transactionRequestType, transactionRequestBody)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[TransactionRequestBGV1](callContext))        
  }
          
  messageDocs += createTransactionRequestPeriodicSepaCreditTransfersBGV1Doc
  def createTransactionRequestPeriodicSepaCreditTransfersBGV1Doc = MessageDoc(
    process = "obp.createTransactionRequestPeriodicSepaCreditTransfersBGV1",
    messageFormat = messageFormat,
    description = "Create Transaction Request Periodic Sepa Credit Transfers BG V1",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateTransactionRequestPeriodicSepaCreditTransfersBGV1").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateTransactionRequestPeriodicSepaCreditTransfersBGV1").request),
    exampleOutboundMessage = (
     OutBoundCreateTransactionRequestPeriodicSepaCreditTransfersBGV1(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      initiator= UserCommons(userPrimaryKey=UserPrimaryKey(123),
      userId=userIdExample.value,
      idGivenByProvider="string",
      provider=providerExample.value,
      emailAddress=emailAddressExample.value,
      name=userNameExample.value,
      createdByConsentId=Some("string"),
      createdByUserInvitationId=Some("string"),
      isDeleted=Some(true),
      lastMarketingAgreementSignedDate=Some(toDate(dateExample))),
      paymentServiceType=com.openbankproject.commons.model.enums.PaymentServiceTypes.example,
      transactionRequestType=com.openbankproject.commons.model.enums.TransactionRequestTypes.example,
      transactionRequestBody= PeriodicSepaCreditTransfersBerlinGroupV13(endToEndIdentification=Some("string"),
      instructionIdentification=Some("string"),
      debtorName=Some("string"),
      debtorAccount=PaymentAccount("string"),
      debtorId=Some("string"),
      ultimateDebtor=Some("string"),
      instructedAmount= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      currencyOfTransfer=Some("string"),
      exchangeRateInformation=Some("string"),
      creditorAccount=PaymentAccount("string"),
      creditorAgent=Some("string"),
      creditorAgentName=Some("string"),
      creditorName="string",
      creditorId=Some("string"),
      creditorAddress=Some("string"),
      creditorNameAndAddress=Some("string"),
      ultimateCreditor=Some("string"),
      purposeCode=Some("string"),
      chargeBearer=Some("string"),
      serviceLevel=Some("string"),
      remittanceInformationUnstructured=Some("string"),
      remittanceInformationUnstructuredArray=Some("string"),
      remittanceInformationStructured=Some("string"),
      remittanceInformationStructuredArray=Some("string"),
      requestedExecutionDate=Some("string"),
      requestedExecutionTime=Some("string"),
      startDate=startDateExample.value,
      executionRule=Some("string"),
      endDate=Some(endDateExample.value),
      frequency=frequencyExample.value,
      dayOfExecution=Some("string")))
    ),
    exampleInboundMessage = (
     InBoundCreateTransactionRequestPeriodicSepaCreditTransfersBGV1(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= TransactionRequestBGV1(id=TransactionRequestId(idExample.value),
      status=statusExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createTransactionRequestPeriodicSepaCreditTransfersBGV1(initiator: User, paymentServiceType: PaymentServiceTypes, transactionRequestType: TransactionRequestTypes, transactionRequestBody: PeriodicSepaCreditTransfersBerlinGroupV13, callContext: Option[CallContext]): OBPReturnType[Box[TransactionRequestBGV1]] = {
        import com.openbankproject.commons.dto.{InBoundCreateTransactionRequestPeriodicSepaCreditTransfersBGV1 => InBound, OutBoundCreateTransactionRequestPeriodicSepaCreditTransfersBGV1 => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, initiator, paymentServiceType, transactionRequestType, transactionRequestBody)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[TransactionRequestBGV1](callContext))        
  }
          
  messageDocs += saveTransactionRequestTransactionDoc
  def saveTransactionRequestTransactionDoc = MessageDoc(
    process = "obp.saveTransactionRequestTransaction",
    messageFormat = messageFormat,
    description = "Save Transaction Request Transaction",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundSaveTransactionRequestTransaction").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundSaveTransactionRequestTransaction").request),
    exampleOutboundMessage = (
     OutBoundSaveTransactionRequestTransaction(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      transactionRequestId=TransactionRequestId(transactionRequestIdExample.value),
      transactionId=TransactionId(transactionIdExample.value))
    ),
    exampleInboundMessage = (
     InBoundSaveTransactionRequestTransaction(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=true)
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def saveTransactionRequestTransaction(transactionRequestId: TransactionRequestId, transactionId: TransactionId, callContext: Option[CallContext]): OBPReturnType[Box[Boolean]] = {
        import com.openbankproject.commons.dto.{InBoundSaveTransactionRequestTransaction => InBound, OutBoundSaveTransactionRequestTransaction => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, transactionRequestId, transactionId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[Boolean](callContext))        
  }
          
  messageDocs += saveTransactionRequestChallengeDoc
  def saveTransactionRequestChallengeDoc = MessageDoc(
    process = "obp.saveTransactionRequestChallenge",
    messageFormat = messageFormat,
    description = "Save Transaction Request Challenge",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundSaveTransactionRequestChallenge").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundSaveTransactionRequestChallenge").request),
    exampleOutboundMessage = (
     OutBoundSaveTransactionRequestChallenge(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      transactionRequestId=TransactionRequestId(transactionRequestIdExample.value),
      challenge= TransactionRequestChallenge(id=challengeIdExample.value,
      allowed_attempts=123,
      challenge_type="string"))
    ),
    exampleInboundMessage = (
     InBoundSaveTransactionRequestChallenge(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=true)
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def saveTransactionRequestChallenge(transactionRequestId: TransactionRequestId, challenge: TransactionRequestChallenge, callContext: Option[CallContext]): OBPReturnType[Box[Boolean]] = {
        import com.openbankproject.commons.dto.{InBoundSaveTransactionRequestChallenge => InBound, OutBoundSaveTransactionRequestChallenge => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, transactionRequestId, challenge)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[Boolean](callContext))        
  }
          
  messageDocs += saveTransactionRequestStatusImplDoc
  def saveTransactionRequestStatusImplDoc = MessageDoc(
    process = "obp.saveTransactionRequestStatusImpl",
    messageFormat = messageFormat,
    description = "Save Transaction Request Status Impl",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundSaveTransactionRequestStatusImpl").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundSaveTransactionRequestStatusImpl").request),
    exampleOutboundMessage = (
     OutBoundSaveTransactionRequestStatusImpl(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      transactionRequestId=TransactionRequestId(transactionRequestIdExample.value),
      status=statusExample.value)
    ),
    exampleInboundMessage = (
     InBoundSaveTransactionRequestStatusImpl(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=true)
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def saveTransactionRequestStatusImpl(transactionRequestId: TransactionRequestId, status: String, callContext: Option[CallContext]): OBPReturnType[Box[Boolean]] = {
        import com.openbankproject.commons.dto.{InBoundSaveTransactionRequestStatusImpl => InBound, OutBoundSaveTransactionRequestStatusImpl => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, transactionRequestId, status)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[Boolean](callContext))        
  }
          
  messageDocs += getTransactionRequests210Doc
  def getTransactionRequests210Doc = MessageDoc(
    process = "obp.getTransactionRequests210",
    messageFormat = messageFormat,
    description = "Get Transaction Requests210",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetTransactionRequests210").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetTransactionRequests210").request),
    exampleOutboundMessage = (
     OutBoundGetTransactionRequests210(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      initiator= UserCommons(userPrimaryKey=UserPrimaryKey(123),
      userId=userIdExample.value,
      idGivenByProvider="string",
      provider=providerExample.value,
      emailAddress=emailAddressExample.value,
      name=userNameExample.value,
      createdByConsentId=Some("string"),
      createdByUserInvitationId=Some("string"),
      isDeleted=Some(true),
      lastMarketingAgreementSignedDate=Some(toDate(dateExample))),
      fromAccount= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))))
    ),
    exampleInboundMessage = (
     InBoundGetTransactionRequests210(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( TransactionRequest(id=TransactionRequestId(transactionRequestIdExample.value),
      `type`=transactionRequestTypeExample.value,
      from= TransactionRequestAccount(bank_id=bank_idExample.value,
      account_id=account_idExample.value),
      body= TransactionRequestBodyAllTypes(to_sandbox_tan=Some( TransactionRequestAccount(bank_id=bank_idExample.value,
      account_id=account_idExample.value)),
      to_sepa=Some(TransactionRequestIban(transactionRequestIban.value)),
      to_counterparty=Some(TransactionRequestCounterpartyId(transactionRequestCounterpartyIdExample.value)),
      to_simple=Some( TransactionRequestSimple(otherBankRoutingScheme=otherBankRoutingSchemeExample.value,
      otherBankRoutingAddress=otherBankRoutingAddressExample.value,
      otherBranchRoutingScheme=otherBranchRoutingSchemeExample.value,
      otherBranchRoutingAddress=otherBranchRoutingAddressExample.value,
      otherAccountRoutingScheme=otherAccountRoutingSchemeExample.value,
      otherAccountRoutingAddress=otherAccountRoutingAddressExample.value,
      otherAccountSecondaryRoutingScheme=otherAccountSecondaryRoutingSchemeExample.value,
      otherAccountSecondaryRoutingAddress=otherAccountSecondaryRoutingAddressExample.value)),
      to_transfer_to_phone=Some( TransactionRequestTransferToPhone(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      message=messageExample.value,
      from= FromAccountTransfer(mobile_phone_number="string",
      nickname=nicknameExample.value),
      to=ToAccountTransferToPhone(toExample.value))),
      to_transfer_to_atm=Some( TransactionRequestTransferToAtm(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      message=messageExample.value,
      from= FromAccountTransfer(mobile_phone_number="string",
      nickname=nicknameExample.value),
      to= ToAccountTransferToAtm(legal_name="string",
      date_of_birth="string",
      mobile_phone_number="string",
      kyc_document= ToAccountTransferToAtmKycDocument(`type`=typeExample.value,
      number=numberExample.value)))),
      to_transfer_to_account=Some( TransactionRequestTransferToAccount(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      transfer_type="string",
      future_date="string",
      to= ToAccountTransferToAccount(name=nameExample.value,
      bank_code="string",
      branch_number="string",
      account= ToAccountTransferToAccountAccount(number=accountNumberExample.value,
      iban=ibanExample.value)))),
      to_sepa_credit_transfers=Some( SepaCreditTransfers(debtorAccount=PaymentAccount("string"),
      instructedAmount= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      creditorAccount=PaymentAccount("string"),
      creditorName="string")),
      value= AmountOfMoney(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value),
      transaction_ids="string",
      status=statusExample.value,
      start_date=toDate(transactionRequestStartDateExample),
      end_date=toDate(transactionRequestEndDateExample),
      challenge= TransactionRequestChallenge(id=challengeIdExample.value,
      allowed_attempts=123,
      challenge_type="string"),
      charge= TransactionRequestCharge(summary=summaryExample.value,
      value= AmountOfMoney(currency=currencyExample.value,
      amount=amountExample.value)),
      charge_policy="string",
      counterparty_id=CounterpartyId(transactionRequestCounterpartyIdExample.value),
      name=nameExample.value,
      this_bank_id=BankId(bankIdExample.value),
      this_account_id=AccountId(accountIdExample.value),
      this_view_id=ViewId(viewIdExample.value),
      other_account_routing_scheme="string",
      other_account_routing_address="string",
      other_bank_routing_scheme="string",
      other_bank_routing_address="string",
      is_beneficiary=true,
      future_date=Some("string"),
      payment_start_date=Some(toDate(dateExample)),
      payment_end_date=Some(toDate(dateExample)),
      payment_execution_Rule=Some("string"),
      payment_frequency=Some("string"),
      payment_day_of_execution=Some("string"))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getTransactionRequests210(initiator: User, fromAccount: BankAccount, callContext: Option[CallContext]): Box[(List[TransactionRequest], Option[CallContext])] = {
        import com.openbankproject.commons.dto.{InBoundGetTransactionRequests210 => InBound, OutBoundGetTransactionRequests210 => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, initiator, fromAccount)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[TransactionRequest]](callContext))        
  }
          
  messageDocs += getTransactionRequestImplDoc
  def getTransactionRequestImplDoc = MessageDoc(
    process = "obp.getTransactionRequestImpl",
    messageFormat = messageFormat,
    description = "Get Transaction Request Impl",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetTransactionRequestImpl").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetTransactionRequestImpl").request),
    exampleOutboundMessage = (
     OutBoundGetTransactionRequestImpl(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      transactionRequestId=TransactionRequestId(transactionRequestIdExample.value))
    ),
    exampleInboundMessage = (
     InBoundGetTransactionRequestImpl(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= TransactionRequest(id=TransactionRequestId(transactionRequestIdExample.value),
      `type`=transactionRequestTypeExample.value,
      from= TransactionRequestAccount(bank_id=bank_idExample.value,
      account_id=account_idExample.value),
      body= TransactionRequestBodyAllTypes(to_sandbox_tan=Some( TransactionRequestAccount(bank_id=bank_idExample.value,
      account_id=account_idExample.value)),
      to_sepa=Some(TransactionRequestIban(transactionRequestIban.value)),
      to_counterparty=Some(TransactionRequestCounterpartyId(transactionRequestCounterpartyIdExample.value)),
      to_simple=Some( TransactionRequestSimple(otherBankRoutingScheme=otherBankRoutingSchemeExample.value,
      otherBankRoutingAddress=otherBankRoutingAddressExample.value,
      otherBranchRoutingScheme=otherBranchRoutingSchemeExample.value,
      otherBranchRoutingAddress=otherBranchRoutingAddressExample.value,
      otherAccountRoutingScheme=otherAccountRoutingSchemeExample.value,
      otherAccountRoutingAddress=otherAccountRoutingAddressExample.value,
      otherAccountSecondaryRoutingScheme=otherAccountSecondaryRoutingSchemeExample.value,
      otherAccountSecondaryRoutingAddress=otherAccountSecondaryRoutingAddressExample.value)),
      to_transfer_to_phone=Some( TransactionRequestTransferToPhone(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      message=messageExample.value,
      from= FromAccountTransfer(mobile_phone_number="string",
      nickname=nicknameExample.value),
      to=ToAccountTransferToPhone(toExample.value))),
      to_transfer_to_atm=Some( TransactionRequestTransferToAtm(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      message=messageExample.value,
      from= FromAccountTransfer(mobile_phone_number="string",
      nickname=nicknameExample.value),
      to= ToAccountTransferToAtm(legal_name="string",
      date_of_birth="string",
      mobile_phone_number="string",
      kyc_document= ToAccountTransferToAtmKycDocument(`type`=typeExample.value,
      number=numberExample.value)))),
      to_transfer_to_account=Some( TransactionRequestTransferToAccount(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      transfer_type="string",
      future_date="string",
      to= ToAccountTransferToAccount(name=nameExample.value,
      bank_code="string",
      branch_number="string",
      account= ToAccountTransferToAccountAccount(number=accountNumberExample.value,
      iban=ibanExample.value)))),
      to_sepa_credit_transfers=Some( SepaCreditTransfers(debtorAccount=PaymentAccount("string"),
      instructedAmount= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      creditorAccount=PaymentAccount("string"),
      creditorName="string")),
      value= AmountOfMoney(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value),
      transaction_ids="string",
      status=statusExample.value,
      start_date=toDate(transactionRequestStartDateExample),
      end_date=toDate(transactionRequestEndDateExample),
      challenge= TransactionRequestChallenge(id=challengeIdExample.value,
      allowed_attempts=123,
      challenge_type="string"),
      charge= TransactionRequestCharge(summary=summaryExample.value,
      value= AmountOfMoney(currency=currencyExample.value,
      amount=amountExample.value)),
      charge_policy="string",
      counterparty_id=CounterpartyId(transactionRequestCounterpartyIdExample.value),
      name=nameExample.value,
      this_bank_id=BankId(bankIdExample.value),
      this_account_id=AccountId(accountIdExample.value),
      this_view_id=ViewId(viewIdExample.value),
      other_account_routing_scheme="string",
      other_account_routing_address="string",
      other_bank_routing_scheme="string",
      other_bank_routing_address="string",
      is_beneficiary=true,
      future_date=Some("string"),
      payment_start_date=Some(toDate(dateExample)),
      payment_end_date=Some(toDate(dateExample)),
      payment_execution_Rule=Some("string"),
      payment_frequency=Some("string"),
      payment_day_of_execution=Some("string")))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getTransactionRequestImpl(transactionRequestId: TransactionRequestId, callContext: Option[CallContext]): Box[(TransactionRequest, Option[CallContext])] = {
        import com.openbankproject.commons.dto.{InBoundGetTransactionRequestImpl => InBound, OutBoundGetTransactionRequestImpl => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, transactionRequestId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[TransactionRequest](callContext))        
  }
          
  messageDocs += getTransactionRequestTypesDoc
  def getTransactionRequestTypesDoc = MessageDoc(
    process = "obp.getTransactionRequestTypes",
    messageFormat = messageFormat,
    description = "Get Transaction Request Types",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetTransactionRequestTypes").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetTransactionRequestTypes").request),
    exampleOutboundMessage = (
     OutBoundGetTransactionRequestTypes(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      initiator= UserCommons(userPrimaryKey=UserPrimaryKey(123),
      userId=userIdExample.value,
      idGivenByProvider="string",
      provider=providerExample.value,
      emailAddress=emailAddressExample.value,
      name=userNameExample.value,
      createdByConsentId=Some("string"),
      createdByUserInvitationId=Some("string"),
      isDeleted=Some(true),
      lastMarketingAgreementSignedDate=Some(toDate(dateExample))),
      fromAccount= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))))
    ),
    exampleInboundMessage = (
     InBoundGetTransactionRequestTypes(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List(TransactionRequestType(transactionRequestTypeExample.value)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getTransactionRequestTypes(initiator: User, fromAccount: BankAccount, callContext: Option[CallContext]): Box[(List[TransactionRequestType], Option[CallContext])] = {
        import com.openbankproject.commons.dto.{InBoundGetTransactionRequestTypes => InBound, OutBoundGetTransactionRequestTypes => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, initiator, fromAccount)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[TransactionRequestType]](callContext))        
  }
          
  messageDocs += createTransactionAfterChallengeV210Doc
  def createTransactionAfterChallengeV210Doc = MessageDoc(
    process = "obp.createTransactionAfterChallengeV210",
    messageFormat = messageFormat,
    description = "Create Transaction After Challenge V210",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateTransactionAfterChallengeV210").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateTransactionAfterChallengeV210").request),
    exampleOutboundMessage = (
     OutBoundCreateTransactionAfterChallengeV210(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      fromAccount= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))),
      transactionRequest= TransactionRequest(id=TransactionRequestId(transactionRequestIdExample.value),
      `type`=transactionRequestTypeExample.value,
      from= TransactionRequestAccount(bank_id=bank_idExample.value,
      account_id=account_idExample.value),
      body= TransactionRequestBodyAllTypes(to_sandbox_tan=Some( TransactionRequestAccount(bank_id=bank_idExample.value,
      account_id=account_idExample.value)),
      to_sepa=Some(TransactionRequestIban(transactionRequestIban.value)),
      to_counterparty=Some(TransactionRequestCounterpartyId(transactionRequestCounterpartyIdExample.value)),
      to_simple=Some( TransactionRequestSimple(otherBankRoutingScheme=otherBankRoutingSchemeExample.value,
      otherBankRoutingAddress=otherBankRoutingAddressExample.value,
      otherBranchRoutingScheme=otherBranchRoutingSchemeExample.value,
      otherBranchRoutingAddress=otherBranchRoutingAddressExample.value,
      otherAccountRoutingScheme=otherAccountRoutingSchemeExample.value,
      otherAccountRoutingAddress=otherAccountRoutingAddressExample.value,
      otherAccountSecondaryRoutingScheme=otherAccountSecondaryRoutingSchemeExample.value,
      otherAccountSecondaryRoutingAddress=otherAccountSecondaryRoutingAddressExample.value)),
      to_transfer_to_phone=Some( TransactionRequestTransferToPhone(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      message=messageExample.value,
      from= FromAccountTransfer(mobile_phone_number="string",
      nickname=nicknameExample.value),
      to=ToAccountTransferToPhone(toExample.value))),
      to_transfer_to_atm=Some( TransactionRequestTransferToAtm(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      message=messageExample.value,
      from= FromAccountTransfer(mobile_phone_number="string",
      nickname=nicknameExample.value),
      to= ToAccountTransferToAtm(legal_name="string",
      date_of_birth="string",
      mobile_phone_number="string",
      kyc_document= ToAccountTransferToAtmKycDocument(`type`=typeExample.value,
      number=numberExample.value)))),
      to_transfer_to_account=Some( TransactionRequestTransferToAccount(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      transfer_type="string",
      future_date="string",
      to= ToAccountTransferToAccount(name=nameExample.value,
      bank_code="string",
      branch_number="string",
      account= ToAccountTransferToAccountAccount(number=accountNumberExample.value,
      iban=ibanExample.value)))),
      to_sepa_credit_transfers=Some( SepaCreditTransfers(debtorAccount=PaymentAccount("string"),
      instructedAmount= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      creditorAccount=PaymentAccount("string"),
      creditorName="string")),
      value= AmountOfMoney(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value),
      transaction_ids="string",
      status=statusExample.value,
      start_date=toDate(transactionRequestStartDateExample),
      end_date=toDate(transactionRequestEndDateExample),
      challenge= TransactionRequestChallenge(id=challengeIdExample.value,
      allowed_attempts=123,
      challenge_type="string"),
      charge= TransactionRequestCharge(summary=summaryExample.value,
      value= AmountOfMoney(currency=currencyExample.value,
      amount=amountExample.value)),
      charge_policy="string",
      counterparty_id=CounterpartyId(transactionRequestCounterpartyIdExample.value),
      name=nameExample.value,
      this_bank_id=BankId(bankIdExample.value),
      this_account_id=AccountId(accountIdExample.value),
      this_view_id=ViewId(viewIdExample.value),
      other_account_routing_scheme="string",
      other_account_routing_address="string",
      other_bank_routing_scheme="string",
      other_bank_routing_address="string",
      is_beneficiary=true,
      future_date=Some("string"),
      payment_start_date=Some(toDate(dateExample)),
      payment_end_date=Some(toDate(dateExample)),
      payment_execution_Rule=Some("string"),
      payment_frequency=Some("string"),
      payment_day_of_execution=Some("string")))
    ),
    exampleInboundMessage = (
     InBoundCreateTransactionAfterChallengeV210(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= TransactionRequest(id=TransactionRequestId(transactionRequestIdExample.value),
      `type`=transactionRequestTypeExample.value,
      from= TransactionRequestAccount(bank_id=bank_idExample.value,
      account_id=account_idExample.value),
      body= TransactionRequestBodyAllTypes(to_sandbox_tan=Some( TransactionRequestAccount(bank_id=bank_idExample.value,
      account_id=account_idExample.value)),
      to_sepa=Some(TransactionRequestIban(transactionRequestIban.value)),
      to_counterparty=Some(TransactionRequestCounterpartyId(transactionRequestCounterpartyIdExample.value)),
      to_simple=Some( TransactionRequestSimple(otherBankRoutingScheme=otherBankRoutingSchemeExample.value,
      otherBankRoutingAddress=otherBankRoutingAddressExample.value,
      otherBranchRoutingScheme=otherBranchRoutingSchemeExample.value,
      otherBranchRoutingAddress=otherBranchRoutingAddressExample.value,
      otherAccountRoutingScheme=otherAccountRoutingSchemeExample.value,
      otherAccountRoutingAddress=otherAccountRoutingAddressExample.value,
      otherAccountSecondaryRoutingScheme=otherAccountSecondaryRoutingSchemeExample.value,
      otherAccountSecondaryRoutingAddress=otherAccountSecondaryRoutingAddressExample.value)),
      to_transfer_to_phone=Some( TransactionRequestTransferToPhone(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      message=messageExample.value,
      from= FromAccountTransfer(mobile_phone_number="string",
      nickname=nicknameExample.value),
      to=ToAccountTransferToPhone(toExample.value))),
      to_transfer_to_atm=Some( TransactionRequestTransferToAtm(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      message=messageExample.value,
      from= FromAccountTransfer(mobile_phone_number="string",
      nickname=nicknameExample.value),
      to= ToAccountTransferToAtm(legal_name="string",
      date_of_birth="string",
      mobile_phone_number="string",
      kyc_document= ToAccountTransferToAtmKycDocument(`type`=typeExample.value,
      number=numberExample.value)))),
      to_transfer_to_account=Some( TransactionRequestTransferToAccount(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      transfer_type="string",
      future_date="string",
      to= ToAccountTransferToAccount(name=nameExample.value,
      bank_code="string",
      branch_number="string",
      account= ToAccountTransferToAccountAccount(number=accountNumberExample.value,
      iban=ibanExample.value)))),
      to_sepa_credit_transfers=Some( SepaCreditTransfers(debtorAccount=PaymentAccount("string"),
      instructedAmount= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      creditorAccount=PaymentAccount("string"),
      creditorName="string")),
      value= AmountOfMoney(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value),
      transaction_ids="string",
      status=statusExample.value,
      start_date=toDate(transactionRequestStartDateExample),
      end_date=toDate(transactionRequestEndDateExample),
      challenge= TransactionRequestChallenge(id=challengeIdExample.value,
      allowed_attempts=123,
      challenge_type="string"),
      charge= TransactionRequestCharge(summary=summaryExample.value,
      value= AmountOfMoney(currency=currencyExample.value,
      amount=amountExample.value)),
      charge_policy="string",
      counterparty_id=CounterpartyId(transactionRequestCounterpartyIdExample.value),
      name=nameExample.value,
      this_bank_id=BankId(bankIdExample.value),
      this_account_id=AccountId(accountIdExample.value),
      this_view_id=ViewId(viewIdExample.value),
      other_account_routing_scheme="string",
      other_account_routing_address="string",
      other_bank_routing_scheme="string",
      other_bank_routing_address="string",
      is_beneficiary=true,
      future_date=Some("string"),
      payment_start_date=Some(toDate(dateExample)),
      payment_end_date=Some(toDate(dateExample)),
      payment_execution_Rule=Some("string"),
      payment_frequency=Some("string"),
      payment_day_of_execution=Some("string")))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createTransactionAfterChallengeV210(fromAccount: BankAccount, transactionRequest: TransactionRequest, callContext: Option[CallContext]): OBPReturnType[Box[TransactionRequest]] = {
        import com.openbankproject.commons.dto.{InBoundCreateTransactionAfterChallengeV210 => InBound, OutBoundCreateTransactionAfterChallengeV210 => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, fromAccount, transactionRequest)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[TransactionRequest](callContext))        
  }
          
  messageDocs += updateBankAccountDoc
  def updateBankAccountDoc = MessageDoc(
    process = "obp.updateBankAccount",
    messageFormat = messageFormat,
    description = "Update Bank Account",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundUpdateBankAccount").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundUpdateBankAccount").request),
    exampleOutboundMessage = (
     OutBoundUpdateBankAccount(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      accountLabel="string",
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)))
    ),
    exampleInboundMessage = (
     InBoundUpdateBankAccount(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def updateBankAccount(bankId: BankId, accountId: AccountId, accountType: String, accountLabel: String, branchId: String, accountRoutings: List[AccountRouting], callContext: Option[CallContext]): OBPReturnType[Box[BankAccount]] = {
        import com.openbankproject.commons.dto.{InBoundUpdateBankAccount => InBound, OutBoundUpdateBankAccount => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, accountId, accountType, accountLabel, branchId, accountRoutings)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[BankAccountCommons](callContext))        
  }
          
  messageDocs += createBankAccountDoc
  def createBankAccountDoc = MessageDoc(
    process = "obp.createBankAccount",
    messageFormat = messageFormat,
    description = "Create Bank Account",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateBankAccount").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateBankAccount").request),
    exampleOutboundMessage = (
     OutBoundCreateBankAccount(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      accountLabel="string",
      currency=currencyExample.value,
      initialBalance=BigDecimal("123.321"),
      accountHolderName="string",
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)))
    ),
    exampleInboundMessage = (
     InBoundCreateBankAccount(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createBankAccount(bankId: BankId, accountId: AccountId, accountType: String, accountLabel: String, currency: String, initialBalance: BigDecimal, accountHolderName: String, branchId: String, accountRoutings: List[AccountRouting], callContext: Option[CallContext]): OBPReturnType[Box[BankAccount]] = {
        import com.openbankproject.commons.dto.{InBoundCreateBankAccount => InBound, OutBoundCreateBankAccount => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, accountId, accountType, accountLabel, currency, initialBalance, accountHolderName, branchId, accountRoutings)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[BankAccountCommons](callContext))        
  }
          
  messageDocs += updateAccountLabelDoc
  def updateAccountLabelDoc = MessageDoc(
    process = "obp.updateAccountLabel",
    messageFormat = messageFormat,
    description = "Update Account Label",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundUpdateAccountLabel").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundUpdateAccountLabel").request),
    exampleOutboundMessage = (
     OutBoundUpdateAccountLabel(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value),
      label=labelExample.value)
    ),
    exampleInboundMessage = (
     InBoundUpdateAccountLabel(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=true)
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def updateAccountLabel(bankId: BankId, accountId: AccountId, label: String, callContext: Option[CallContext]): OBPReturnType[Box[Boolean]] = {
        import com.openbankproject.commons.dto.{InBoundUpdateAccountLabel => InBound, OutBoundUpdateAccountLabel => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, accountId, label)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[Boolean](callContext))        
  }
          
  messageDocs += getProductsDoc
  def getProductsDoc = MessageDoc(
    process = "obp.getProducts",
    messageFormat = messageFormat,
    description = "Get Products",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetProducts").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetProducts").request),
    exampleOutboundMessage = (
     OutBoundGetProducts(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      params=List( GetProductsParam(name=nameExample.value,
      value=valueExample.value.replace("[","").replace("]","").split(",").toList)))
    ),
    exampleInboundMessage = (
     InBoundGetProducts(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( ProductCommons(bankId=BankId(bankIdExample.value),
      code=ProductCode(productCodeExample.value),
      parentProductCode=ProductCode(parentProductCodeExample.value),
      name=productNameExample.value,
      category=categoryExample.value,
      family=familyExample.value,
      superFamily=superFamilyExample.value,
      moreInfoUrl=moreInfoUrlExample.value,
      termsAndConditionsUrl=termsAndConditionsUrlExample.value,
      details=detailsExample.value,
      description=descriptionExample.value,
      meta=Meta( License(id=licenseIdExample.value,
      name=licenseNameExample.value)))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getProducts(bankId: BankId, params: List[GetProductsParam], callContext: Option[CallContext]): OBPReturnType[Box[List[Product]]] = {
        import com.openbankproject.commons.dto.{InBoundGetProducts => InBound, OutBoundGetProducts => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, params)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[ProductCommons]](callContext))        
  }
          
  messageDocs += getProductDoc
  def getProductDoc = MessageDoc(
    process = "obp.getProduct",
    messageFormat = messageFormat,
    description = "Get Product",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetProduct").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetProduct").request),
    exampleOutboundMessage = (
     OutBoundGetProduct(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      productCode=ProductCode(productCodeExample.value))
    ),
    exampleInboundMessage = (
     InBoundGetProduct(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= ProductCommons(bankId=BankId(bankIdExample.value),
      code=ProductCode(productCodeExample.value),
      parentProductCode=ProductCode(parentProductCodeExample.value),
      name=productNameExample.value,
      category=categoryExample.value,
      family=familyExample.value,
      superFamily=superFamilyExample.value,
      moreInfoUrl=moreInfoUrlExample.value,
      termsAndConditionsUrl=termsAndConditionsUrlExample.value,
      details=detailsExample.value,
      description=descriptionExample.value,
      meta=Meta( License(id=licenseIdExample.value,
      name=licenseNameExample.value))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getProduct(bankId: BankId, productCode: ProductCode, callContext: Option[CallContext]): OBPReturnType[Box[Product]] = {
        import com.openbankproject.commons.dto.{InBoundGetProduct => InBound, OutBoundGetProduct => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, productCode)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[ProductCommons](callContext))        
  }
          
  messageDocs += getBranchDoc
  def getBranchDoc = MessageDoc(
    process = "obp.getBranch",
    messageFormat = messageFormat,
    description = "Get Branch",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetBranch").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetBranch").request),
    exampleOutboundMessage = (
     OutBoundGetBranch(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      branchId=BranchId(branchIdExample.value))
    ),
    exampleInboundMessage = (
     InBoundGetBranch(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= BranchTCommons(branchId=BranchId(branchIdExample.value),
      bankId=BankId(bankIdExample.value),
      name=nameExample.value,
      address= Address(line1=line1Example.value,
      line2=line2Example.value,
      line3=line3Example.value,
      city=cityExample.value,
      county=Some(countyExample.value),
      state=stateExample.value,
      postCode=postCodeExample.value,
      countryCode=countryCodeExample.value),
      location= Location(latitude=latitudeExample.value.toDouble,
      longitude=longitudeExample.value.toDouble,
      date=Some(toDate(dateExample)),
      user=Some( BasicResourceUser(userId=userIdExample.value,
      provider=providerExample.value,
      username=usernameExample.value))),
      lobbyString=Some(LobbyString("string")),
      driveUpString=Some(DriveUpString("string")),
      meta=Meta( License(id=licenseIdExample.value,
      name=licenseNameExample.value)),
      branchRouting=Some( Routing(scheme=branchRoutingSchemeExample.value,
      address=branchRoutingAddressExample.value)),
      lobby=Some( Lobby(monday=List( OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value)),
      tuesday=List( OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value)),
      wednesday=List( OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value)),
      thursday=List( OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value)),
      friday=List( OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value)),
      saturday=List( OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value)),
      sunday=List( OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value)))),
      driveUp=Some( DriveUp(monday= OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value),
      tuesday= OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value),
      wednesday= OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value),
      thursday= OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value),
      friday= OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value),
      saturday= OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value),
      sunday= OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value))),
      isAccessible=Some(isAccessibleExample.value.toBoolean),
      accessibleFeatures=Some("string"),
      branchType=Some(branchTypeExample.value),
      moreInfo=Some(moreInfoExample.value),
      phoneNumber=Some(phoneNumberExample.value),
      isDeleted=Some(true)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getBranch(bankId: BankId, branchId: BranchId, callContext: Option[CallContext]): Future[Box[(BranchT, Option[CallContext])]] = {
        import com.openbankproject.commons.dto.{InBoundGetBranch => InBound, OutBoundGetBranch => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, branchId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[BranchTCommons](callContext))        
  }
          
  messageDocs += getBranchesDoc
  def getBranchesDoc = MessageDoc(
    process = "obp.getBranches",
    messageFormat = messageFormat,
    description = "Get Branches",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetBranches").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetBranches").request),
    exampleOutboundMessage = (
     OutBoundGetBranches(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      limit=limitExample.value.toInt,
      offset=offsetExample.value.toInt,
      fromDate=fromDateExample.value,
      toDate=toDateExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetBranches(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( BranchTCommons(branchId=BranchId(branchIdExample.value),
      bankId=BankId(bankIdExample.value),
      name=nameExample.value,
      address= Address(line1=line1Example.value,
      line2=line2Example.value,
      line3=line3Example.value,
      city=cityExample.value,
      county=Some(countyExample.value),
      state=stateExample.value,
      postCode=postCodeExample.value,
      countryCode=countryCodeExample.value),
      location= Location(latitude=latitudeExample.value.toDouble,
      longitude=longitudeExample.value.toDouble,
      date=Some(toDate(dateExample)),
      user=Some( BasicResourceUser(userId=userIdExample.value,
      provider=providerExample.value,
      username=usernameExample.value))),
      lobbyString=Some(LobbyString("string")),
      driveUpString=Some(DriveUpString("string")),
      meta=Meta( License(id=licenseIdExample.value,
      name=licenseNameExample.value)),
      branchRouting=Some( Routing(scheme=branchRoutingSchemeExample.value,
      address=branchRoutingAddressExample.value)),
      lobby=Some( Lobby(monday=List( OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value)),
      tuesday=List( OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value)),
      wednesday=List( OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value)),
      thursday=List( OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value)),
      friday=List( OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value)),
      saturday=List( OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value)),
      sunday=List( OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value)))),
      driveUp=Some( DriveUp(monday= OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value),
      tuesday= OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value),
      wednesday= OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value),
      thursday= OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value),
      friday= OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value),
      saturday= OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value),
      sunday= OpeningTimes(openingTime=openingTimeExample.value,
      closingTime=closingTimeExample.value))),
      isAccessible=Some(isAccessibleExample.value.toBoolean),
      accessibleFeatures=Some("string"),
      branchType=Some(branchTypeExample.value),
      moreInfo=Some(moreInfoExample.value),
      phoneNumber=Some(phoneNumberExample.value),
      isDeleted=Some(true))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getBranches(bankId: BankId, callContext: Option[CallContext], queryParams: List[OBPQueryParam]): Future[Box[(List[BranchT], Option[CallContext])]] = {
        import com.openbankproject.commons.dto.{InBoundGetBranches => InBound, OutBoundGetBranches => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, OBPQueryParam.getLimit(queryParams), OBPQueryParam.getOffset(queryParams), OBPQueryParam.getFromDate(queryParams), OBPQueryParam.getToDate(queryParams))
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[BranchTCommons]](callContext))        
  }
          
  messageDocs += getAtmDoc
  def getAtmDoc = MessageDoc(
    process = "obp.getAtm",
    messageFormat = messageFormat,
    description = "Get Atm",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetAtm").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetAtm").request),
    exampleOutboundMessage = (
     OutBoundGetAtm(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      atmId=AtmId(atmIdExample.value))
    ),
    exampleInboundMessage = (
     InBoundGetAtm(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= AtmTCommons(atmId=AtmId(atmIdExample.value),
      bankId=BankId(bankIdExample.value),
      name=nameExample.value,
      address= Address(line1=line1Example.value,
      line2=line2Example.value,
      line3=line3Example.value,
      city=cityExample.value,
      county=Some(countyExample.value),
      state=stateExample.value,
      postCode=postCodeExample.value,
      countryCode=countryCodeExample.value),
      location= Location(latitude=latitudeExample.value.toDouble,
      longitude=longitudeExample.value.toDouble,
      date=Some(toDate(dateExample)),
      user=Some( BasicResourceUser(userId=userIdExample.value,
      provider=providerExample.value,
      username=usernameExample.value))),
      meta=Meta( License(id=licenseIdExample.value,
      name=licenseNameExample.value)),
      OpeningTimeOnMonday=Some("string"),
      ClosingTimeOnMonday=Some("string"),
      OpeningTimeOnTuesday=Some("string"),
      ClosingTimeOnTuesday=Some("string"),
      OpeningTimeOnWednesday=Some("string"),
      ClosingTimeOnWednesday=Some("string"),
      OpeningTimeOnThursday=Some("string"),
      ClosingTimeOnThursday=Some("string"),
      OpeningTimeOnFriday=Some("string"),
      ClosingTimeOnFriday=Some("string"),
      OpeningTimeOnSaturday=Some("string"),
      ClosingTimeOnSaturday=Some("string"),
      OpeningTimeOnSunday=Some("string"),
      ClosingTimeOnSunday=Some("string"),
      isAccessible=Some(isAccessibleExample.value.toBoolean),
      locatedAt=Some(locatedAtExample.value),
      moreInfo=Some(moreInfoExample.value),
      hasDepositCapability=Some(hasDepositCapabilityExample.value.toBoolean),
      supportedLanguages=Some(supportedLanguagesExample.value.replace("[","").replace("]","").split(",").toList),
      services=Some(listExample.value.replace("[","").replace("]","").split(",").toList),
      accessibilityFeatures=Some(accessibilityFeaturesExample.value.replace("[","").replace("]","").split(",").toList),
      supportedCurrencies=Some(supportedCurrenciesExample.value.replace("[","").replace("]","").split(",").toList),
      notes=Some(listExample.value.replace("[","").replace("]","").split(",").toList),
      locationCategories=Some(listExample.value.replace("[","").replace("]","").split(",").toList),
      minimumWithdrawal=Some("string"),
      branchIdentification=Some("string"),
      siteIdentification=Some(siteIdentification.value),
      siteName=Some("string"),
      cashWithdrawalNationalFee=Some(cashWithdrawalNationalFeeExample.value),
      cashWithdrawalInternationalFee=Some(cashWithdrawalInternationalFeeExample.value),
      balanceInquiryFee=Some(balanceInquiryFeeExample.value),
      atmType=Some(atmTypeExample.value),
      phone=Some(phoneExample.value)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getAtm(bankId: BankId, atmId: AtmId, callContext: Option[CallContext]): Future[Box[(AtmT, Option[CallContext])]] = {
        import com.openbankproject.commons.dto.{InBoundGetAtm => InBound, OutBoundGetAtm => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, atmId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[AtmTCommons](callContext))        
  }
          
  messageDocs += getAtmsDoc
  def getAtmsDoc = MessageDoc(
    process = "obp.getAtms",
    messageFormat = messageFormat,
    description = "Get Atms",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetAtms").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetAtms").request),
    exampleOutboundMessage = (
     OutBoundGetAtms(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      limit=limitExample.value.toInt,
      offset=offsetExample.value.toInt,
      fromDate=fromDateExample.value,
      toDate=toDateExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetAtms(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( AtmTCommons(atmId=AtmId(atmIdExample.value),
      bankId=BankId(bankIdExample.value),
      name=nameExample.value,
      address= Address(line1=line1Example.value,
      line2=line2Example.value,
      line3=line3Example.value,
      city=cityExample.value,
      county=Some(countyExample.value),
      state=stateExample.value,
      postCode=postCodeExample.value,
      countryCode=countryCodeExample.value),
      location= Location(latitude=latitudeExample.value.toDouble,
      longitude=longitudeExample.value.toDouble,
      date=Some(toDate(dateExample)),
      user=Some( BasicResourceUser(userId=userIdExample.value,
      provider=providerExample.value,
      username=usernameExample.value))),
      meta=Meta( License(id=licenseIdExample.value,
      name=licenseNameExample.value)),
      OpeningTimeOnMonday=Some("string"),
      ClosingTimeOnMonday=Some("string"),
      OpeningTimeOnTuesday=Some("string"),
      ClosingTimeOnTuesday=Some("string"),
      OpeningTimeOnWednesday=Some("string"),
      ClosingTimeOnWednesday=Some("string"),
      OpeningTimeOnThursday=Some("string"),
      ClosingTimeOnThursday=Some("string"),
      OpeningTimeOnFriday=Some("string"),
      ClosingTimeOnFriday=Some("string"),
      OpeningTimeOnSaturday=Some("string"),
      ClosingTimeOnSaturday=Some("string"),
      OpeningTimeOnSunday=Some("string"),
      ClosingTimeOnSunday=Some("string"),
      isAccessible=Some(isAccessibleExample.value.toBoolean),
      locatedAt=Some(locatedAtExample.value),
      moreInfo=Some(moreInfoExample.value),
      hasDepositCapability=Some(hasDepositCapabilityExample.value.toBoolean),
      supportedLanguages=Some(supportedLanguagesExample.value.replace("[","").replace("]","").split(",").toList),
      services=Some(listExample.value.replace("[","").replace("]","").split(",").toList),
      accessibilityFeatures=Some(accessibilityFeaturesExample.value.replace("[","").replace("]","").split(",").toList),
      supportedCurrencies=Some(supportedCurrenciesExample.value.replace("[","").replace("]","").split(",").toList),
      notes=Some(listExample.value.replace("[","").replace("]","").split(",").toList),
      locationCategories=Some(listExample.value.replace("[","").replace("]","").split(",").toList),
      minimumWithdrawal=Some("string"),
      branchIdentification=Some("string"),
      siteIdentification=Some(siteIdentification.value),
      siteName=Some("string"),
      cashWithdrawalNationalFee=Some(cashWithdrawalNationalFeeExample.value),
      cashWithdrawalInternationalFee=Some(cashWithdrawalInternationalFeeExample.value),
      balanceInquiryFee=Some(balanceInquiryFeeExample.value),
      atmType=Some(atmTypeExample.value),
      phone=Some(phoneExample.value))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getAtms(bankId: BankId, callContext: Option[CallContext], queryParams: List[OBPQueryParam]): Future[Box[(List[AtmT], Option[CallContext])]] = {
        import com.openbankproject.commons.dto.{InBoundGetAtms => InBound, OutBoundGetAtms => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, OBPQueryParam.getLimit(queryParams), OBPQueryParam.getOffset(queryParams), OBPQueryParam.getFromDate(queryParams), OBPQueryParam.getToDate(queryParams))
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[AtmTCommons]](callContext))        
  }
          
  messageDocs += createTransactionAfterChallengev300Doc
  def createTransactionAfterChallengev300Doc = MessageDoc(
    process = "obp.createTransactionAfterChallengev300",
    messageFormat = messageFormat,
    description = "Create Transaction After Challengev300",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateTransactionAfterChallengev300").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateTransactionAfterChallengev300").request),
    exampleOutboundMessage = (
     OutBoundCreateTransactionAfterChallengev300(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      initiator= UserCommons(userPrimaryKey=UserPrimaryKey(123),
      userId=userIdExample.value,
      idGivenByProvider="string",
      provider=providerExample.value,
      emailAddress=emailAddressExample.value,
      name=userNameExample.value,
      createdByConsentId=Some("string"),
      createdByUserInvitationId=Some("string"),
      isDeleted=Some(true),
      lastMarketingAgreementSignedDate=Some(toDate(dateExample))),
      fromAccount= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))),
      transReqId=TransactionRequestId(transactionRequestIdExample.value),
      transactionRequestType=TransactionRequestType(transactionRequestTypeExample.value))
    ),
    exampleInboundMessage = (
     InBoundCreateTransactionAfterChallengev300(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= TransactionRequest(id=TransactionRequestId(transactionRequestIdExample.value),
      `type`=transactionRequestTypeExample.value,
      from= TransactionRequestAccount(bank_id=bank_idExample.value,
      account_id=account_idExample.value),
      body= TransactionRequestBodyAllTypes(to_sandbox_tan=Some( TransactionRequestAccount(bank_id=bank_idExample.value,
      account_id=account_idExample.value)),
      to_sepa=Some(TransactionRequestIban(transactionRequestIban.value)),
      to_counterparty=Some(TransactionRequestCounterpartyId(transactionRequestCounterpartyIdExample.value)),
      to_simple=Some( TransactionRequestSimple(otherBankRoutingScheme=otherBankRoutingSchemeExample.value,
      otherBankRoutingAddress=otherBankRoutingAddressExample.value,
      otherBranchRoutingScheme=otherBranchRoutingSchemeExample.value,
      otherBranchRoutingAddress=otherBranchRoutingAddressExample.value,
      otherAccountRoutingScheme=otherAccountRoutingSchemeExample.value,
      otherAccountRoutingAddress=otherAccountRoutingAddressExample.value,
      otherAccountSecondaryRoutingScheme=otherAccountSecondaryRoutingSchemeExample.value,
      otherAccountSecondaryRoutingAddress=otherAccountSecondaryRoutingAddressExample.value)),
      to_transfer_to_phone=Some( TransactionRequestTransferToPhone(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      message=messageExample.value,
      from= FromAccountTransfer(mobile_phone_number="string",
      nickname=nicknameExample.value),
      to=ToAccountTransferToPhone(toExample.value))),
      to_transfer_to_atm=Some( TransactionRequestTransferToAtm(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      message=messageExample.value,
      from= FromAccountTransfer(mobile_phone_number="string",
      nickname=nicknameExample.value),
      to= ToAccountTransferToAtm(legal_name="string",
      date_of_birth="string",
      mobile_phone_number="string",
      kyc_document= ToAccountTransferToAtmKycDocument(`type`=typeExample.value,
      number=numberExample.value)))),
      to_transfer_to_account=Some( TransactionRequestTransferToAccount(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      transfer_type="string",
      future_date="string",
      to= ToAccountTransferToAccount(name=nameExample.value,
      bank_code="string",
      branch_number="string",
      account= ToAccountTransferToAccountAccount(number=accountNumberExample.value,
      iban=ibanExample.value)))),
      to_sepa_credit_transfers=Some( SepaCreditTransfers(debtorAccount=PaymentAccount("string"),
      instructedAmount= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      creditorAccount=PaymentAccount("string"),
      creditorName="string")),
      value= AmountOfMoney(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value),
      transaction_ids="string",
      status=statusExample.value,
      start_date=toDate(transactionRequestStartDateExample),
      end_date=toDate(transactionRequestEndDateExample),
      challenge= TransactionRequestChallenge(id=challengeIdExample.value,
      allowed_attempts=123,
      challenge_type="string"),
      charge= TransactionRequestCharge(summary=summaryExample.value,
      value= AmountOfMoney(currency=currencyExample.value,
      amount=amountExample.value)),
      charge_policy="string",
      counterparty_id=CounterpartyId(transactionRequestCounterpartyIdExample.value),
      name=nameExample.value,
      this_bank_id=BankId(bankIdExample.value),
      this_account_id=AccountId(accountIdExample.value),
      this_view_id=ViewId(viewIdExample.value),
      other_account_routing_scheme="string",
      other_account_routing_address="string",
      other_bank_routing_scheme="string",
      other_bank_routing_address="string",
      is_beneficiary=true,
      future_date=Some("string"),
      payment_start_date=Some(toDate(dateExample)),
      payment_end_date=Some(toDate(dateExample)),
      payment_execution_Rule=Some("string"),
      payment_frequency=Some("string"),
      payment_day_of_execution=Some("string")))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createTransactionAfterChallengev300(initiator: User, fromAccount: BankAccount, transReqId: TransactionRequestId, transactionRequestType: TransactionRequestType, callContext: Option[CallContext]): OBPReturnType[Box[TransactionRequest]] = {
        import com.openbankproject.commons.dto.{InBoundCreateTransactionAfterChallengev300 => InBound, OutBoundCreateTransactionAfterChallengev300 => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, initiator, fromAccount, transReqId, transactionRequestType)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[TransactionRequest](callContext))        
  }
          
  messageDocs += makePaymentv300Doc
  def makePaymentv300Doc = MessageDoc(
    process = "obp.makePaymentv300",
    messageFormat = messageFormat,
    description = "Make Paymentv300",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundMakePaymentv300").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundMakePaymentv300").request),
    exampleOutboundMessage = (
     OutBoundMakePaymentv300(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      initiator= UserCommons(userPrimaryKey=UserPrimaryKey(123),
      userId=userIdExample.value,
      idGivenByProvider="string",
      provider=providerExample.value,
      emailAddress=emailAddressExample.value,
      name=userNameExample.value,
      createdByConsentId=Some("string"),
      createdByUserInvitationId=Some("string"),
      isDeleted=Some(true),
      lastMarketingAgreementSignedDate=Some(toDate(dateExample))),
      fromAccount= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))),
      toAccount= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))),
      toCounterparty= CounterpartyTraitCommons(createdByUserId=createdByUserIdExample.value,
      name=counterpartyNameExample.value,
      description=descriptionExample.value,
      currency=currencyExample.value,
      thisBankId=thisBankIdExample.value,
      thisAccountId=thisAccountIdExample.value,
      thisViewId=thisViewIdExample.value,
      counterpartyId=counterpartyIdExample.value,
      otherAccountRoutingScheme=counterpartyOtherAccountRoutingSchemeExample.value,
      otherAccountRoutingAddress=counterpartyOtherAccountRoutingAddressExample.value,
      otherAccountSecondaryRoutingScheme=counterpartyOtherAccountSecondaryRoutingSchemeExample.value,
      otherAccountSecondaryRoutingAddress=counterpartyOtherAccountSecondaryRoutingAddressExample.value,
      otherBankRoutingScheme=counterpartyOtherBankRoutingSchemeExample.value,
      otherBankRoutingAddress=counterpartyOtherBankRoutingAddressExample.value,
      otherBranchRoutingScheme=counterpartyOtherBranchRoutingSchemeExample.value,
      otherBranchRoutingAddress=counterpartyOtherBranchRoutingAddressExample.value,
      isBeneficiary=isBeneficiaryExample.value.toBoolean,
      bespoke=List( CounterpartyBespoke(key=keyExample.value,
      value=valueExample.value))),
      transactionRequestCommonBody= TransactionRequestCommonBodyJSONCommons(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value),
      transactionRequestType=TransactionRequestType(transactionRequestTypeExample.value),
      chargePolicy=chargePolicyExample.value)
    ),
    exampleInboundMessage = (
     InBoundMakePaymentv300(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=TransactionId(transactionIdExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def makePaymentv300(initiator: User, fromAccount: BankAccount, toAccount: BankAccount, toCounterparty: CounterpartyTrait, transactionRequestCommonBody: TransactionRequestCommonBodyJSON, transactionRequestType: TransactionRequestType, chargePolicy: String, callContext: Option[CallContext]): Future[Box[(TransactionId, Option[CallContext])]] = {
        import com.openbankproject.commons.dto.{InBoundMakePaymentv300 => InBound, OutBoundMakePaymentv300 => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, initiator, fromAccount, toAccount, toCounterparty, transactionRequestCommonBody, transactionRequestType, chargePolicy)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[TransactionId](callContext))        
  }
          
  messageDocs += createTransactionRequestv300Doc
  def createTransactionRequestv300Doc = MessageDoc(
    process = "obp.createTransactionRequestv300",
    messageFormat = messageFormat,
    description = "Create Transaction Requestv300",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateTransactionRequestv300").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateTransactionRequestv300").request),
    exampleOutboundMessage = (
     OutBoundCreateTransactionRequestv300(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      initiator= UserCommons(userPrimaryKey=UserPrimaryKey(123),
      userId=userIdExample.value,
      idGivenByProvider="string",
      provider=providerExample.value,
      emailAddress=emailAddressExample.value,
      name=userNameExample.value,
      createdByConsentId=Some("string"),
      createdByUserInvitationId=Some("string"),
      isDeleted=Some(true),
      lastMarketingAgreementSignedDate=Some(toDate(dateExample))),
      viewId=ViewId(viewIdExample.value),
      fromAccount= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))),
      toAccount= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))),
      toCounterparty= CounterpartyTraitCommons(createdByUserId=createdByUserIdExample.value,
      name=counterpartyNameExample.value,
      description=descriptionExample.value,
      currency=currencyExample.value,
      thisBankId=thisBankIdExample.value,
      thisAccountId=thisAccountIdExample.value,
      thisViewId=thisViewIdExample.value,
      counterpartyId=counterpartyIdExample.value,
      otherAccountRoutingScheme=counterpartyOtherAccountRoutingSchemeExample.value,
      otherAccountRoutingAddress=counterpartyOtherAccountRoutingAddressExample.value,
      otherAccountSecondaryRoutingScheme=counterpartyOtherAccountSecondaryRoutingSchemeExample.value,
      otherAccountSecondaryRoutingAddress=counterpartyOtherAccountSecondaryRoutingAddressExample.value,
      otherBankRoutingScheme=counterpartyOtherBankRoutingSchemeExample.value,
      otherBankRoutingAddress=counterpartyOtherBankRoutingAddressExample.value,
      otherBranchRoutingScheme=counterpartyOtherBranchRoutingSchemeExample.value,
      otherBranchRoutingAddress=counterpartyOtherBranchRoutingAddressExample.value,
      isBeneficiary=isBeneficiaryExample.value.toBoolean,
      bespoke=List( CounterpartyBespoke(key=keyExample.value,
      value=valueExample.value))),
      transactionRequestType=TransactionRequestType(transactionRequestTypeExample.value),
      transactionRequestCommonBody= TransactionRequestCommonBodyJSONCommons(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value),
      detailsPlain="string",
      chargePolicy=chargePolicyExample.value)
    ),
    exampleInboundMessage = (
     InBoundCreateTransactionRequestv300(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= TransactionRequest(id=TransactionRequestId(transactionRequestIdExample.value),
      `type`=transactionRequestTypeExample.value,
      from= TransactionRequestAccount(bank_id=bank_idExample.value,
      account_id=account_idExample.value),
      body= TransactionRequestBodyAllTypes(to_sandbox_tan=Some( TransactionRequestAccount(bank_id=bank_idExample.value,
      account_id=account_idExample.value)),
      to_sepa=Some(TransactionRequestIban(transactionRequestIban.value)),
      to_counterparty=Some(TransactionRequestCounterpartyId(transactionRequestCounterpartyIdExample.value)),
      to_simple=Some( TransactionRequestSimple(otherBankRoutingScheme=otherBankRoutingSchemeExample.value,
      otherBankRoutingAddress=otherBankRoutingAddressExample.value,
      otherBranchRoutingScheme=otherBranchRoutingSchemeExample.value,
      otherBranchRoutingAddress=otherBranchRoutingAddressExample.value,
      otherAccountRoutingScheme=otherAccountRoutingSchemeExample.value,
      otherAccountRoutingAddress=otherAccountRoutingAddressExample.value,
      otherAccountSecondaryRoutingScheme=otherAccountSecondaryRoutingSchemeExample.value,
      otherAccountSecondaryRoutingAddress=otherAccountSecondaryRoutingAddressExample.value)),
      to_transfer_to_phone=Some( TransactionRequestTransferToPhone(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      message=messageExample.value,
      from= FromAccountTransfer(mobile_phone_number="string",
      nickname=nicknameExample.value),
      to=ToAccountTransferToPhone(toExample.value))),
      to_transfer_to_atm=Some( TransactionRequestTransferToAtm(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      message=messageExample.value,
      from= FromAccountTransfer(mobile_phone_number="string",
      nickname=nicknameExample.value),
      to= ToAccountTransferToAtm(legal_name="string",
      date_of_birth="string",
      mobile_phone_number="string",
      kyc_document= ToAccountTransferToAtmKycDocument(`type`=typeExample.value,
      number=numberExample.value)))),
      to_transfer_to_account=Some( TransactionRequestTransferToAccount(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      transfer_type="string",
      future_date="string",
      to= ToAccountTransferToAccount(name=nameExample.value,
      bank_code="string",
      branch_number="string",
      account= ToAccountTransferToAccountAccount(number=accountNumberExample.value,
      iban=ibanExample.value)))),
      to_sepa_credit_transfers=Some( SepaCreditTransfers(debtorAccount=PaymentAccount("string"),
      instructedAmount= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      creditorAccount=PaymentAccount("string"),
      creditorName="string")),
      value= AmountOfMoney(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value),
      transaction_ids="string",
      status=statusExample.value,
      start_date=toDate(transactionRequestStartDateExample),
      end_date=toDate(transactionRequestEndDateExample),
      challenge= TransactionRequestChallenge(id=challengeIdExample.value,
      allowed_attempts=123,
      challenge_type="string"),
      charge= TransactionRequestCharge(summary=summaryExample.value,
      value= AmountOfMoney(currency=currencyExample.value,
      amount=amountExample.value)),
      charge_policy="string",
      counterparty_id=CounterpartyId(transactionRequestCounterpartyIdExample.value),
      name=nameExample.value,
      this_bank_id=BankId(bankIdExample.value),
      this_account_id=AccountId(accountIdExample.value),
      this_view_id=ViewId(viewIdExample.value),
      other_account_routing_scheme="string",
      other_account_routing_address="string",
      other_bank_routing_scheme="string",
      other_bank_routing_address="string",
      is_beneficiary=true,
      future_date=Some("string"),
      payment_start_date=Some(toDate(dateExample)),
      payment_end_date=Some(toDate(dateExample)),
      payment_execution_Rule=Some("string"),
      payment_frequency=Some("string"),
      payment_day_of_execution=Some("string")))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createTransactionRequestv300(initiator: User, viewId: ViewId, fromAccount: BankAccount, toAccount: BankAccount, toCounterparty: CounterpartyTrait, transactionRequestType: TransactionRequestType, transactionRequestCommonBody: TransactionRequestCommonBodyJSON, detailsPlain: String, chargePolicy: String, callContext: Option[CallContext]): Future[Box[(TransactionRequest, Option[CallContext])]] = {
        import com.openbankproject.commons.dto.{InBoundCreateTransactionRequestv300 => InBound, OutBoundCreateTransactionRequestv300 => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, initiator, viewId, fromAccount, toAccount, toCounterparty, transactionRequestType, transactionRequestCommonBody, detailsPlain, chargePolicy)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[TransactionRequest](callContext))        
  }
          
  messageDocs += makePaymentV400Doc
  def makePaymentV400Doc = MessageDoc(
    process = "obp.makePaymentV400",
    messageFormat = messageFormat,
    description = "Make Payment V400",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundMakePaymentV400").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundMakePaymentV400").request),
    exampleOutboundMessage = (
     OutBoundMakePaymentV400(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      transactionRequest= TransactionRequest(id=TransactionRequestId(transactionRequestIdExample.value),
      `type`=transactionRequestTypeExample.value,
      from= TransactionRequestAccount(bank_id=bank_idExample.value,
      account_id=account_idExample.value),
      body= TransactionRequestBodyAllTypes(to_sandbox_tan=Some( TransactionRequestAccount(bank_id=bank_idExample.value,
      account_id=account_idExample.value)),
      to_sepa=Some(TransactionRequestIban(transactionRequestIban.value)),
      to_counterparty=Some(TransactionRequestCounterpartyId(transactionRequestCounterpartyIdExample.value)),
      to_simple=Some( TransactionRequestSimple(otherBankRoutingScheme=otherBankRoutingSchemeExample.value,
      otherBankRoutingAddress=otherBankRoutingAddressExample.value,
      otherBranchRoutingScheme=otherBranchRoutingSchemeExample.value,
      otherBranchRoutingAddress=otherBranchRoutingAddressExample.value,
      otherAccountRoutingScheme=otherAccountRoutingSchemeExample.value,
      otherAccountRoutingAddress=otherAccountRoutingAddressExample.value,
      otherAccountSecondaryRoutingScheme=otherAccountSecondaryRoutingSchemeExample.value,
      otherAccountSecondaryRoutingAddress=otherAccountSecondaryRoutingAddressExample.value)),
      to_transfer_to_phone=Some( TransactionRequestTransferToPhone(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      message=messageExample.value,
      from= FromAccountTransfer(mobile_phone_number="string",
      nickname=nicknameExample.value),
      to=ToAccountTransferToPhone(toExample.value))),
      to_transfer_to_atm=Some( TransactionRequestTransferToAtm(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      message=messageExample.value,
      from= FromAccountTransfer(mobile_phone_number="string",
      nickname=nicknameExample.value),
      to= ToAccountTransferToAtm(legal_name="string",
      date_of_birth="string",
      mobile_phone_number="string",
      kyc_document= ToAccountTransferToAtmKycDocument(`type`=typeExample.value,
      number=numberExample.value)))),
      to_transfer_to_account=Some( TransactionRequestTransferToAccount(value= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value,
      transfer_type="string",
      future_date="string",
      to= ToAccountTransferToAccount(name=nameExample.value,
      bank_code="string",
      branch_number="string",
      account= ToAccountTransferToAccountAccount(number=accountNumberExample.value,
      iban=ibanExample.value)))),
      to_sepa_credit_transfers=Some( SepaCreditTransfers(debtorAccount=PaymentAccount("string"),
      instructedAmount= AmountOfMoneyJsonV121(currency=currencyExample.value,
      amount=amountExample.value),
      creditorAccount=PaymentAccount("string"),
      creditorName="string")),
      value= AmountOfMoney(currency=currencyExample.value,
      amount=amountExample.value),
      description=descriptionExample.value),
      transaction_ids="string",
      status=statusExample.value,
      start_date=toDate(transactionRequestStartDateExample),
      end_date=toDate(transactionRequestEndDateExample),
      challenge= TransactionRequestChallenge(id=challengeIdExample.value,
      allowed_attempts=123,
      challenge_type="string"),
      charge= TransactionRequestCharge(summary=summaryExample.value,
      value= AmountOfMoney(currency=currencyExample.value,
      amount=amountExample.value)),
      charge_policy="string",
      counterparty_id=CounterpartyId(transactionRequestCounterpartyIdExample.value),
      name=nameExample.value,
      this_bank_id=BankId(bankIdExample.value),
      this_account_id=AccountId(accountIdExample.value),
      this_view_id=ViewId(viewIdExample.value),
      other_account_routing_scheme="string",
      other_account_routing_address="string",
      other_bank_routing_scheme="string",
      other_bank_routing_address="string",
      is_beneficiary=true,
      future_date=Some("string"),
      payment_start_date=Some(toDate(dateExample)),
      payment_end_date=Some(toDate(dateExample)),
      payment_execution_Rule=Some("string"),
      payment_frequency=Some("string"),
      payment_day_of_execution=Some("string")),
      reasons=Some(List( TransactionRequestReason(code=codeExample.value,
      documentNumber=Some(documentNumberExample.value),
      amount=Some(amountExample.value),
      currency=Some(currencyExample.value),
      description=Some(descriptionExample.value)))))
    ),
    exampleInboundMessage = (
     InBoundMakePaymentV400(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=TransactionId(transactionIdExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def makePaymentV400(transactionRequest: TransactionRequest, reasons: Option[List[TransactionRequestReason]], callContext: Option[CallContext]): Future[Box[(TransactionId, Option[CallContext])]] = {
        import com.openbankproject.commons.dto.{InBoundMakePaymentV400 => InBound, OutBoundMakePaymentV400 => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, transactionRequest, reasons)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[TransactionId](callContext))        
  }
          
  messageDocs += cancelPaymentV400Doc
  def cancelPaymentV400Doc = MessageDoc(
    process = "obp.cancelPaymentV400",
    messageFormat = messageFormat,
    description = "Cancel Payment V400",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCancelPaymentV400").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCancelPaymentV400").request),
    exampleOutboundMessage = (
     OutBoundCancelPaymentV400(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      transactionId=TransactionId(transactionIdExample.value))
    ),
    exampleInboundMessage = (
     InBoundCancelPaymentV400(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= CancelPayment(canBeCancelled=true,
      startSca=Some(true)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def cancelPaymentV400(transactionId: TransactionId, callContext: Option[CallContext]): OBPReturnType[Box[CancelPayment]] = {
        import com.openbankproject.commons.dto.{InBoundCancelPaymentV400 => InBound, OutBoundCancelPaymentV400 => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, transactionId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[CancelPayment](callContext))        
  }
          
  messageDocs += getTransactionRequestTypeChargesDoc
  def getTransactionRequestTypeChargesDoc = MessageDoc(
    process = "obp.getTransactionRequestTypeCharges",
    messageFormat = messageFormat,
    description = "Get Transaction Request Type Charges",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetTransactionRequestTypeCharges").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetTransactionRequestTypeCharges").request),
    exampleOutboundMessage = (
     OutBoundGetTransactionRequestTypeCharges(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value),
      viewId=ViewId(viewIdExample.value),
      transactionRequestTypes=List(TransactionRequestType(transactionRequestTypesExample.value)))
    ),
    exampleInboundMessage = (
     InBoundGetTransactionRequestTypeCharges(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( TransactionRequestTypeChargeCommons(transactionRequestTypeId="string",
      bankId=bankIdExample.value,
      chargeCurrency="string",
      chargeAmount="string",
      chargeSummary="string")))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getTransactionRequestTypeCharges(bankId: BankId, accountId: AccountId, viewId: ViewId, transactionRequestTypes: List[TransactionRequestType], callContext: Option[CallContext]): OBPReturnType[Box[List[TransactionRequestTypeCharge]]] = {
        import com.openbankproject.commons.dto.{InBoundGetTransactionRequestTypeCharges => InBound, OutBoundGetTransactionRequestTypeCharges => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, accountId, viewId, transactionRequestTypes)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[TransactionRequestTypeChargeCommons]](callContext))        
  }
          
  messageDocs += createCounterpartyDoc
  def createCounterpartyDoc = MessageDoc(
    process = "obp.createCounterparty",
    messageFormat = messageFormat,
    description = "Create Counterparty",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateCounterparty").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateCounterparty").request),
    exampleOutboundMessage = (
     OutBoundCreateCounterparty(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      name=nameExample.value,
      description=descriptionExample.value,
      currency=currencyExample.value,
      createdByUserId=createdByUserIdExample.value,
      thisBankId=thisBankIdExample.value,
      thisAccountId=thisAccountIdExample.value,
      thisViewId=thisViewIdExample.value,
      otherAccountRoutingScheme=otherAccountRoutingSchemeExample.value,
      otherAccountRoutingAddress=otherAccountRoutingAddressExample.value,
      otherAccountSecondaryRoutingScheme=otherAccountSecondaryRoutingSchemeExample.value,
      otherAccountSecondaryRoutingAddress=otherAccountSecondaryRoutingAddressExample.value,
      otherBankRoutingScheme=otherBankRoutingSchemeExample.value,
      otherBankRoutingAddress=otherBankRoutingAddressExample.value,
      otherBranchRoutingScheme=otherBranchRoutingSchemeExample.value,
      otherBranchRoutingAddress=otherBranchRoutingAddressExample.value,
      isBeneficiary=isBeneficiaryExample.value.toBoolean,
      bespoke=List( CounterpartyBespoke(key=keyExample.value,
      value=valueExample.value)))
    ),
    exampleInboundMessage = (
     InBoundCreateCounterparty(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= CounterpartyTraitCommons(createdByUserId=createdByUserIdExample.value,
      name=nameExample.value,
      description=descriptionExample.value,
      currency=currencyExample.value,
      thisBankId=thisBankIdExample.value,
      thisAccountId=thisAccountIdExample.value,
      thisViewId=thisViewIdExample.value,
      counterpartyId=counterpartyIdExample.value,
      otherAccountRoutingScheme=otherAccountRoutingSchemeExample.value,
      otherAccountRoutingAddress=otherAccountRoutingAddressExample.value,
      otherAccountSecondaryRoutingScheme=otherAccountSecondaryRoutingSchemeExample.value,
      otherAccountSecondaryRoutingAddress=otherAccountSecondaryRoutingAddressExample.value,
      otherBankRoutingScheme=otherBankRoutingSchemeExample.value,
      otherBankRoutingAddress=otherBankRoutingAddressExample.value,
      otherBranchRoutingScheme=otherBranchRoutingSchemeExample.value,
      otherBranchRoutingAddress=otherBranchRoutingAddressExample.value,
      isBeneficiary=isBeneficiaryExample.value.toBoolean,
      bespoke=List( CounterpartyBespoke(key=keyExample.value,
      value=valueExample.value))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createCounterparty(name: String, description: String, currency: String, createdByUserId: String, thisBankId: String, thisAccountId: String, thisViewId: String, otherAccountRoutingScheme: String, otherAccountRoutingAddress: String, otherAccountSecondaryRoutingScheme: String, otherAccountSecondaryRoutingAddress: String, otherBankRoutingScheme: String, otherBankRoutingAddress: String, otherBranchRoutingScheme: String, otherBranchRoutingAddress: String, isBeneficiary: Boolean, bespoke: List[CounterpartyBespoke], callContext: Option[CallContext]): Box[(CounterpartyTrait, Option[CallContext])] = {
        import com.openbankproject.commons.dto.{InBoundCreateCounterparty => InBound, OutBoundCreateCounterparty => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, name, description, currency, createdByUserId, thisBankId, thisAccountId, thisViewId, otherAccountRoutingScheme, otherAccountRoutingAddress, otherAccountSecondaryRoutingScheme, otherAccountSecondaryRoutingAddress, otherBankRoutingScheme, otherBankRoutingAddress, otherBranchRoutingScheme, otherBranchRoutingAddress, isBeneficiary, bespoke)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[CounterpartyTraitCommons](callContext))        
  }
          
  messageDocs += checkCustomerNumberAvailableDoc
  def checkCustomerNumberAvailableDoc = MessageDoc(
    process = "obp.checkCustomerNumberAvailable",
    messageFormat = messageFormat,
    description = "Check Customer Number Available",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCheckCustomerNumberAvailable").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCheckCustomerNumberAvailable").request),
    exampleOutboundMessage = (
     OutBoundCheckCustomerNumberAvailable(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      customerNumber=customerNumberExample.value)
    ),
    exampleInboundMessage = (
     InBoundCheckCustomerNumberAvailable(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=true)
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def checkCustomerNumberAvailable(bankId: BankId, customerNumber: String, callContext: Option[CallContext]): OBPReturnType[Box[Boolean]] = {
        import com.openbankproject.commons.dto.{InBoundCheckCustomerNumberAvailable => InBound, OutBoundCheckCustomerNumberAvailable => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, customerNumber)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[Boolean](callContext))        
  }
          
  messageDocs += createCustomerDoc
  def createCustomerDoc = MessageDoc(
    process = "obp.createCustomer",
    messageFormat = messageFormat,
    description = "Create Customer",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateCustomer").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateCustomer").request),
    exampleOutboundMessage = (
     OutBoundCreateCustomer(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      legalName=legalNameExample.value,
      mobileNumber=mobileNumberExample.value,
      email=emailExample.value,
      faceImage= CustomerFaceImage(date=toDate(customerFaceImageDateExample),
      url=urlExample.value),
      dateOfBirth=toDate(dateOfBirthExample),
      relationshipStatus=relationshipStatusExample.value,
      dependents=dependentsExample.value.toInt,
      dobOfDependents=dobOfDependentsExample.value.replace("[","").replace("]","").split(",").map(parseDate).flatMap(_.toSeq).toList,
      highestEducationAttained=highestEducationAttainedExample.value,
      employmentStatus=employmentStatusExample.value,
      kycStatus=kycStatusExample.value.toBoolean,
      lastOkDate=toDate(outBoundCreateCustomerLastOkDateExample),
      creditRating=Some( CreditRating(rating=ratingExample.value,
      source=sourceExample.value)),
      creditLimit=Some( AmountOfMoney(currency=currencyExample.value,
      amount=creditLimitAmountExample.value)),
      title=titleExample.value,
      branchId=branchIdExample.value,
      nameSuffix=nameSuffixExample.value)
    ),
    exampleInboundMessage = (
     InBoundCreateCustomer(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= CustomerCommons(customerId=customerIdExample.value,
      bankId=bankIdExample.value,
      number=customerNumberExample.value,
      legalName=legalNameExample.value,
      mobileNumber=mobileNumberExample.value,
      email=emailExample.value,
      faceImage= CustomerFaceImage(date=toDate(customerFaceImageDateExample),
      url=urlExample.value),
      dateOfBirth=toDate(dateOfBirthExample),
      relationshipStatus=relationshipStatusExample.value,
      dependents=dependentsExample.value.toInt,
      dobOfDependents=dobOfDependentsExample.value.replace("[","").replace("]","").split(",").map(parseDate).flatMap(_.toSeq).toList,
      highestEducationAttained=highestEducationAttainedExample.value,
      employmentStatus=employmentStatusExample.value,
      creditRating= CreditRating(rating=ratingExample.value,
      source=sourceExample.value),
      creditLimit= CreditLimit(currency=currencyExample.value,
      amount=creditLimitAmountExample.value),
      kycStatus=kycStatusExample.value.toBoolean,
      lastOkDate=toDate(customerLastOkDateExample),
      title=customerTitleExample.value,
      branchId=branchIdExample.value,
      nameSuffix=nameSuffixExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createCustomer(bankId: BankId, legalName: String, mobileNumber: String, email: String, faceImage: CustomerFaceImageTrait, dateOfBirth: Date, relationshipStatus: String, dependents: Int, dobOfDependents: List[Date], highestEducationAttained: String, employmentStatus: String, kycStatus: Boolean, lastOkDate: Date, creditRating: Option[CreditRatingTrait], creditLimit: Option[AmountOfMoneyTrait], title: String, branchId: String, nameSuffix: String, callContext: Option[CallContext]): OBPReturnType[Box[Customer]] = {
        import com.openbankproject.commons.dto.{InBoundCreateCustomer => InBound, OutBoundCreateCustomer => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, legalName, mobileNumber, email, faceImage, dateOfBirth, relationshipStatus, dependents, dobOfDependents, highestEducationAttained, employmentStatus, kycStatus, lastOkDate, creditRating, creditLimit, title, branchId, nameSuffix)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[CustomerCommons](callContext))        
  }
          
  messageDocs += updateCustomerScaDataDoc
  def updateCustomerScaDataDoc = MessageDoc(
    process = "obp.updateCustomerScaData",
    messageFormat = messageFormat,
    description = "Update Customer Sca Data",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundUpdateCustomerScaData").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundUpdateCustomerScaData").request),
    exampleOutboundMessage = (
     OutBoundUpdateCustomerScaData(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      customerId=customerIdExample.value,
      mobileNumber=Some(mobileNumberExample.value),
      email=Some(emailExample.value),
      customerNumber=Some(customerNumberExample.value))
    ),
    exampleInboundMessage = (
     InBoundUpdateCustomerScaData(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= CustomerCommons(customerId=customerIdExample.value,
      bankId=bankIdExample.value,
      number=customerNumberExample.value,
      legalName=legalNameExample.value,
      mobileNumber=mobileNumberExample.value,
      email=emailExample.value,
      faceImage= CustomerFaceImage(date=toDate(customerFaceImageDateExample),
      url=urlExample.value),
      dateOfBirth=toDate(dateOfBirthExample),
      relationshipStatus=relationshipStatusExample.value,
      dependents=dependentsExample.value.toInt,
      dobOfDependents=dobOfDependentsExample.value.replace("[","").replace("]","").split(",").map(parseDate).flatMap(_.toSeq).toList,
      highestEducationAttained=highestEducationAttainedExample.value,
      employmentStatus=employmentStatusExample.value,
      creditRating= CreditRating(rating=ratingExample.value,
      source=sourceExample.value),
      creditLimit= CreditLimit(currency=currencyExample.value,
      amount=creditLimitAmountExample.value),
      kycStatus=kycStatusExample.value.toBoolean,
      lastOkDate=toDate(customerLastOkDateExample),
      title=customerTitleExample.value,
      branchId=branchIdExample.value,
      nameSuffix=nameSuffixExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def updateCustomerScaData(customerId: String, mobileNumber: Option[String], email: Option[String], customerNumber: Option[String], callContext: Option[CallContext]): OBPReturnType[Box[Customer]] = {
        import com.openbankproject.commons.dto.{InBoundUpdateCustomerScaData => InBound, OutBoundUpdateCustomerScaData => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, customerId, mobileNumber, email, customerNumber)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[CustomerCommons](callContext))        
  }
          
  messageDocs += updateCustomerCreditDataDoc
  def updateCustomerCreditDataDoc = MessageDoc(
    process = "obp.updateCustomerCreditData",
    messageFormat = messageFormat,
    description = "Update Customer Credit Data",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundUpdateCustomerCreditData").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundUpdateCustomerCreditData").request),
    exampleOutboundMessage = (
     OutBoundUpdateCustomerCreditData(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      customerId=customerIdExample.value,
      creditRating=Some(creditRatingExample.value),
      creditSource=Some("string"),
      creditLimit=Some( AmountOfMoney(currency=currencyExample.value,
      amount=creditLimitAmountExample.value)))
    ),
    exampleInboundMessage = (
     InBoundUpdateCustomerCreditData(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= CustomerCommons(customerId=customerIdExample.value,
      bankId=bankIdExample.value,
      number=customerNumberExample.value,
      legalName=legalNameExample.value,
      mobileNumber=mobileNumberExample.value,
      email=emailExample.value,
      faceImage= CustomerFaceImage(date=toDate(customerFaceImageDateExample),
      url=urlExample.value),
      dateOfBirth=toDate(dateOfBirthExample),
      relationshipStatus=relationshipStatusExample.value,
      dependents=dependentsExample.value.toInt,
      dobOfDependents=dobOfDependentsExample.value.replace("[","").replace("]","").split(",").map(parseDate).flatMap(_.toSeq).toList,
      highestEducationAttained=highestEducationAttainedExample.value,
      employmentStatus=employmentStatusExample.value,
      creditRating= CreditRating(rating=ratingExample.value,
      source=sourceExample.value),
      creditLimit= CreditLimit(currency=currencyExample.value,
      amount=creditLimitAmountExample.value),
      kycStatus=kycStatusExample.value.toBoolean,
      lastOkDate=toDate(customerLastOkDateExample),
      title=customerTitleExample.value,
      branchId=branchIdExample.value,
      nameSuffix=nameSuffixExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def updateCustomerCreditData(customerId: String, creditRating: Option[String], creditSource: Option[String], creditLimit: Option[AmountOfMoney], callContext: Option[CallContext]): OBPReturnType[Box[Customer]] = {
        import com.openbankproject.commons.dto.{InBoundUpdateCustomerCreditData => InBound, OutBoundUpdateCustomerCreditData => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, customerId, creditRating, creditSource, creditLimit)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[CustomerCommons](callContext))        
  }
          
  messageDocs += updateCustomerGeneralDataDoc
  def updateCustomerGeneralDataDoc = MessageDoc(
    process = "obp.updateCustomerGeneralData",
    messageFormat = messageFormat,
    description = "Update Customer General Data",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundUpdateCustomerGeneralData").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundUpdateCustomerGeneralData").request),
    exampleOutboundMessage = (
     OutBoundUpdateCustomerGeneralData(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      customerId=customerIdExample.value,
      legalName=Some(legalNameExample.value),
      faceImage=Some( CustomerFaceImage(date=toDate(customerFaceImageDateExample),
      url=urlExample.value)),
      dateOfBirth=Some(toDate(dateOfBirthExample)),
      relationshipStatus=Some(relationshipStatusExample.value),
      dependents=Some(dependentsExample.value.toInt),
      highestEducationAttained=Some(highestEducationAttainedExample.value),
      employmentStatus=Some(employmentStatusExample.value),
      title=Some(titleExample.value),
      branchId=Some(branchIdExample.value),
      nameSuffix=Some(nameSuffixExample.value))
    ),
    exampleInboundMessage = (
     InBoundUpdateCustomerGeneralData(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= CustomerCommons(customerId=customerIdExample.value,
      bankId=bankIdExample.value,
      number=customerNumberExample.value,
      legalName=legalNameExample.value,
      mobileNumber=mobileNumberExample.value,
      email=emailExample.value,
      faceImage= CustomerFaceImage(date=toDate(customerFaceImageDateExample),
      url=urlExample.value),
      dateOfBirth=toDate(dateOfBirthExample),
      relationshipStatus=relationshipStatusExample.value,
      dependents=dependentsExample.value.toInt,
      dobOfDependents=dobOfDependentsExample.value.replace("[","").replace("]","").split(",").map(parseDate).flatMap(_.toSeq).toList,
      highestEducationAttained=highestEducationAttainedExample.value,
      employmentStatus=employmentStatusExample.value,
      creditRating= CreditRating(rating=ratingExample.value,
      source=sourceExample.value),
      creditLimit= CreditLimit(currency=currencyExample.value,
      amount=creditLimitAmountExample.value),
      kycStatus=kycStatusExample.value.toBoolean,
      lastOkDate=toDate(customerLastOkDateExample),
      title=customerTitleExample.value,
      branchId=branchIdExample.value,
      nameSuffix=nameSuffixExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def updateCustomerGeneralData(customerId: String, legalName: Option[String], faceImage: Option[CustomerFaceImageTrait], dateOfBirth: Option[Date], relationshipStatus: Option[String], dependents: Option[Int], highestEducationAttained: Option[String], employmentStatus: Option[String], title: Option[String], branchId: Option[String], nameSuffix: Option[String], callContext: Option[CallContext]): OBPReturnType[Box[Customer]] = {
        import com.openbankproject.commons.dto.{InBoundUpdateCustomerGeneralData => InBound, OutBoundUpdateCustomerGeneralData => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, customerId, legalName, faceImage, dateOfBirth, relationshipStatus, dependents, highestEducationAttained, employmentStatus, title, branchId, nameSuffix)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[CustomerCommons](callContext))        
  }
          
  messageDocs += getCustomersByUserIdDoc
  def getCustomersByUserIdDoc = MessageDoc(
    process = "obp.getCustomersByUserId",
    messageFormat = messageFormat,
    description = "Get Customers By User Id",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCustomersByUserId").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCustomersByUserId").request),
    exampleOutboundMessage = (
     OutBoundGetCustomersByUserId(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      userId=userIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetCustomersByUserId(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( CustomerCommons(customerId=customerIdExample.value,
      bankId=bankIdExample.value,
      number=customerNumberExample.value,
      legalName=legalNameExample.value,
      mobileNumber=mobileNumberExample.value,
      email=emailExample.value,
      faceImage= CustomerFaceImage(date=toDate(customerFaceImageDateExample),
      url=urlExample.value),
      dateOfBirth=toDate(dateOfBirthExample),
      relationshipStatus=relationshipStatusExample.value,
      dependents=dependentsExample.value.toInt,
      dobOfDependents=dobOfDependentsExample.value.replace("[","").replace("]","").split(",").map(parseDate).flatMap(_.toSeq).toList,
      highestEducationAttained=highestEducationAttainedExample.value,
      employmentStatus=employmentStatusExample.value,
      creditRating= CreditRating(rating=ratingExample.value,
      source=sourceExample.value),
      creditLimit= CreditLimit(currency=currencyExample.value,
      amount=creditLimitAmountExample.value),
      kycStatus=kycStatusExample.value.toBoolean,
      lastOkDate=toDate(customerLastOkDateExample),
      title=customerTitleExample.value,
      branchId=branchIdExample.value,
      nameSuffix=nameSuffixExample.value)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getCustomersByUserId(userId: String, callContext: Option[CallContext]): Future[Box[(List[Customer], Option[CallContext])]] = {
        import com.openbankproject.commons.dto.{InBoundGetCustomersByUserId => InBound, OutBoundGetCustomersByUserId => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, userId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[CustomerCommons]](callContext))        
  }
          
  messageDocs += getCustomerByCustomerIdDoc
  def getCustomerByCustomerIdDoc = MessageDoc(
    process = "obp.getCustomerByCustomerId",
    messageFormat = messageFormat,
    description = "Get Customer By Customer Id",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCustomerByCustomerId").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCustomerByCustomerId").request),
    exampleOutboundMessage = (
     OutBoundGetCustomerByCustomerId(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      customerId=customerIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetCustomerByCustomerId(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= CustomerCommons(customerId=customerIdExample.value,
      bankId=bankIdExample.value,
      number=customerNumberExample.value,
      legalName=legalNameExample.value,
      mobileNumber=mobileNumberExample.value,
      email=emailExample.value,
      faceImage= CustomerFaceImage(date=toDate(customerFaceImageDateExample),
      url=urlExample.value),
      dateOfBirth=toDate(dateOfBirthExample),
      relationshipStatus=relationshipStatusExample.value,
      dependents=dependentsExample.value.toInt,
      dobOfDependents=dobOfDependentsExample.value.replace("[","").replace("]","").split(",").map(parseDate).flatMap(_.toSeq).toList,
      highestEducationAttained=highestEducationAttainedExample.value,
      employmentStatus=employmentStatusExample.value,
      creditRating= CreditRating(rating=ratingExample.value,
      source=sourceExample.value),
      creditLimit= CreditLimit(currency=currencyExample.value,
      amount=creditLimitAmountExample.value),
      kycStatus=kycStatusExample.value.toBoolean,
      lastOkDate=toDate(customerLastOkDateExample),
      title=customerTitleExample.value,
      branchId=branchIdExample.value,
      nameSuffix=nameSuffixExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getCustomerByCustomerId(customerId: String, callContext: Option[CallContext]): Future[Box[(Customer, Option[CallContext])]] = {
        import com.openbankproject.commons.dto.{InBoundGetCustomerByCustomerId => InBound, OutBoundGetCustomerByCustomerId => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, customerId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[CustomerCommons](callContext))        
  }
          
  messageDocs += getCustomerByCustomerNumberDoc
  def getCustomerByCustomerNumberDoc = MessageDoc(
    process = "obp.getCustomerByCustomerNumber",
    messageFormat = messageFormat,
    description = "Get Customer By Customer Number",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCustomerByCustomerNumber").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCustomerByCustomerNumber").request),
    exampleOutboundMessage = (
     OutBoundGetCustomerByCustomerNumber(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      customerNumber=customerNumberExample.value,
      bankId=BankId(bankIdExample.value))
    ),
    exampleInboundMessage = (
     InBoundGetCustomerByCustomerNumber(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= CustomerCommons(customerId=customerIdExample.value,
      bankId=bankIdExample.value,
      number=customerNumberExample.value,
      legalName=legalNameExample.value,
      mobileNumber=mobileNumberExample.value,
      email=emailExample.value,
      faceImage= CustomerFaceImage(date=toDate(customerFaceImageDateExample),
      url=urlExample.value),
      dateOfBirth=toDate(dateOfBirthExample),
      relationshipStatus=relationshipStatusExample.value,
      dependents=dependentsExample.value.toInt,
      dobOfDependents=dobOfDependentsExample.value.replace("[","").replace("]","").split(",").map(parseDate).flatMap(_.toSeq).toList,
      highestEducationAttained=highestEducationAttainedExample.value,
      employmentStatus=employmentStatusExample.value,
      creditRating= CreditRating(rating=ratingExample.value,
      source=sourceExample.value),
      creditLimit= CreditLimit(currency=currencyExample.value,
      amount=creditLimitAmountExample.value),
      kycStatus=kycStatusExample.value.toBoolean,
      lastOkDate=toDate(customerLastOkDateExample),
      title=customerTitleExample.value,
      branchId=branchIdExample.value,
      nameSuffix=nameSuffixExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getCustomerByCustomerNumber(customerNumber: String, bankId: BankId, callContext: Option[CallContext]): Future[Box[(Customer, Option[CallContext])]] = {
        import com.openbankproject.commons.dto.{InBoundGetCustomerByCustomerNumber => InBound, OutBoundGetCustomerByCustomerNumber => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, customerNumber, bankId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[CustomerCommons](callContext))        
  }
          
  messageDocs += getCustomerAddressDoc
  def getCustomerAddressDoc = MessageDoc(
    process = "obp.getCustomerAddress",
    messageFormat = messageFormat,
    description = "Get Customer Address",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCustomerAddress").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCustomerAddress").request),
    exampleOutboundMessage = (
     OutBoundGetCustomerAddress(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      customerId=customerIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetCustomerAddress(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( CustomerAddressCommons(customerId=customerIdExample.value,
      customerAddressId=customerAddressIdExample.value,
      line1=line1Example.value,
      line2=line2Example.value,
      line3=line3Example.value,
      city=cityExample.value,
      county=countyExample.value,
      state=stateExample.value,
      postcode=postcodeExample.value,
      countryCode=countryCodeExample.value,
      status=statusExample.value,
      tags=tagsExample.value,
      insertDate=toDate(insertDateExample))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getCustomerAddress(customerId: String, callContext: Option[CallContext]): OBPReturnType[Box[List[CustomerAddress]]] = {
        import com.openbankproject.commons.dto.{InBoundGetCustomerAddress => InBound, OutBoundGetCustomerAddress => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, customerId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[CustomerAddressCommons]](callContext))        
  }
          
  messageDocs += createCustomerAddressDoc
  def createCustomerAddressDoc = MessageDoc(
    process = "obp.createCustomerAddress",
    messageFormat = messageFormat,
    description = "Create Customer Address",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateCustomerAddress").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateCustomerAddress").request),
    exampleOutboundMessage = (
     OutBoundCreateCustomerAddress(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      customerId=customerIdExample.value,
      line1=line1Example.value,
      line2=line2Example.value,
      line3=line3Example.value,
      city=cityExample.value,
      county=countyExample.value,
      state=stateExample.value,
      postcode=postcodeExample.value,
      countryCode=countryCodeExample.value,
      tags=tagsExample.value,
      status=statusExample.value)
    ),
    exampleInboundMessage = (
     InBoundCreateCustomerAddress(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= CustomerAddressCommons(customerId=customerIdExample.value,
      customerAddressId=customerAddressIdExample.value,
      line1=line1Example.value,
      line2=line2Example.value,
      line3=line3Example.value,
      city=cityExample.value,
      county=countyExample.value,
      state=stateExample.value,
      postcode=postcodeExample.value,
      countryCode=countryCodeExample.value,
      status=statusExample.value,
      tags=tagsExample.value,
      insertDate=toDate(insertDateExample)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createCustomerAddress(customerId: String, line1: String, line2: String, line3: String, city: String, county: String, state: String, postcode: String, countryCode: String, tags: String, status: String, callContext: Option[CallContext]): OBPReturnType[Box[CustomerAddress]] = {
        import com.openbankproject.commons.dto.{InBoundCreateCustomerAddress => InBound, OutBoundCreateCustomerAddress => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, customerId, line1, line2, line3, city, county, state, postcode, countryCode, tags, status)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[CustomerAddressCommons](callContext))        
  }
          
  messageDocs += updateCustomerAddressDoc
  def updateCustomerAddressDoc = MessageDoc(
    process = "obp.updateCustomerAddress",
    messageFormat = messageFormat,
    description = "Update Customer Address",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundUpdateCustomerAddress").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundUpdateCustomerAddress").request),
    exampleOutboundMessage = (
     OutBoundUpdateCustomerAddress(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      customerAddressId=customerAddressIdExample.value,
      line1=line1Example.value,
      line2=line2Example.value,
      line3=line3Example.value,
      city=cityExample.value,
      county=countyExample.value,
      state=stateExample.value,
      postcode=postcodeExample.value,
      countryCode=countryCodeExample.value,
      tags=tagsExample.value,
      status=statusExample.value)
    ),
    exampleInboundMessage = (
     InBoundUpdateCustomerAddress(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= CustomerAddressCommons(customerId=customerIdExample.value,
      customerAddressId=customerAddressIdExample.value,
      line1=line1Example.value,
      line2=line2Example.value,
      line3=line3Example.value,
      city=cityExample.value,
      county=countyExample.value,
      state=stateExample.value,
      postcode=postcodeExample.value,
      countryCode=countryCodeExample.value,
      status=statusExample.value,
      tags=tagsExample.value,
      insertDate=toDate(insertDateExample)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def updateCustomerAddress(customerAddressId: String, line1: String, line2: String, line3: String, city: String, county: String, state: String, postcode: String, countryCode: String, tags: String, status: String, callContext: Option[CallContext]): OBPReturnType[Box[CustomerAddress]] = {
        import com.openbankproject.commons.dto.{InBoundUpdateCustomerAddress => InBound, OutBoundUpdateCustomerAddress => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, customerAddressId, line1, line2, line3, city, county, state, postcode, countryCode, tags, status)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[CustomerAddressCommons](callContext))        
  }
          
  messageDocs += deleteCustomerAddressDoc
  def deleteCustomerAddressDoc = MessageDoc(
    process = "obp.deleteCustomerAddress",
    messageFormat = messageFormat,
    description = "Delete Customer Address",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundDeleteCustomerAddress").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundDeleteCustomerAddress").request),
    exampleOutboundMessage = (
     OutBoundDeleteCustomerAddress(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      customerAddressId=customerAddressIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundDeleteCustomerAddress(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=true)
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def deleteCustomerAddress(customerAddressId: String, callContext: Option[CallContext]): OBPReturnType[Box[Boolean]] = {
        import com.openbankproject.commons.dto.{InBoundDeleteCustomerAddress => InBound, OutBoundDeleteCustomerAddress => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, customerAddressId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[Boolean](callContext))        
  }
          
  messageDocs += createTaxResidenceDoc
  def createTaxResidenceDoc = MessageDoc(
    process = "obp.createTaxResidence",
    messageFormat = messageFormat,
    description = "Create Tax Residence",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateTaxResidence").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateTaxResidence").request),
    exampleOutboundMessage = (
     OutBoundCreateTaxResidence(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      customerId=customerIdExample.value,
      domain=domainExample.value,
      taxNumber=taxNumberExample.value)
    ),
    exampleInboundMessage = (
     InBoundCreateTaxResidence(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= TaxResidenceCommons(customerId=customerIdExample.value,
      taxResidenceId=taxResidenceIdExample.value,
      domain=domainExample.value,
      taxNumber=taxNumberExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createTaxResidence(customerId: String, domain: String, taxNumber: String, callContext: Option[CallContext]): OBPReturnType[Box[TaxResidence]] = {
        import com.openbankproject.commons.dto.{InBoundCreateTaxResidence => InBound, OutBoundCreateTaxResidence => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, customerId, domain, taxNumber)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[TaxResidenceCommons](callContext))        
  }
          
  messageDocs += getTaxResidenceDoc
  def getTaxResidenceDoc = MessageDoc(
    process = "obp.getTaxResidence",
    messageFormat = messageFormat,
    description = "Get Tax Residence",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetTaxResidence").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetTaxResidence").request),
    exampleOutboundMessage = (
     OutBoundGetTaxResidence(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      customerId=customerIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetTaxResidence(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( TaxResidenceCommons(customerId=customerIdExample.value,
      taxResidenceId=taxResidenceIdExample.value,
      domain=domainExample.value,
      taxNumber=taxNumberExample.value)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getTaxResidence(customerId: String, callContext: Option[CallContext]): OBPReturnType[Box[List[TaxResidence]]] = {
        import com.openbankproject.commons.dto.{InBoundGetTaxResidence => InBound, OutBoundGetTaxResidence => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, customerId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[TaxResidenceCommons]](callContext))        
  }
          
  messageDocs += deleteTaxResidenceDoc
  def deleteTaxResidenceDoc = MessageDoc(
    process = "obp.deleteTaxResidence",
    messageFormat = messageFormat,
    description = "Delete Tax Residence",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundDeleteTaxResidence").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundDeleteTaxResidence").request),
    exampleOutboundMessage = (
     OutBoundDeleteTaxResidence(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      taxResourceId="string")
    ),
    exampleInboundMessage = (
     InBoundDeleteTaxResidence(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=true)
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def deleteTaxResidence(taxResourceId: String, callContext: Option[CallContext]): OBPReturnType[Box[Boolean]] = {
        import com.openbankproject.commons.dto.{InBoundDeleteTaxResidence => InBound, OutBoundDeleteTaxResidence => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, taxResourceId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[Boolean](callContext))        
  }
          
  messageDocs += getCustomersDoc
  def getCustomersDoc = MessageDoc(
    process = "obp.getCustomers",
    messageFormat = messageFormat,
    description = "Get Customers",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCustomers").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCustomers").request),
    exampleOutboundMessage = (
     OutBoundGetCustomers(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      limit=limitExample.value.toInt,
      offset=offsetExample.value.toInt,
      fromDate=fromDateExample.value,
      toDate=toDateExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetCustomers(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( CustomerCommons(customerId=customerIdExample.value,
      bankId=bankIdExample.value,
      number=customerNumberExample.value,
      legalName=legalNameExample.value,
      mobileNumber=mobileNumberExample.value,
      email=emailExample.value,
      faceImage= CustomerFaceImage(date=toDate(customerFaceImageDateExample),
      url=urlExample.value),
      dateOfBirth=toDate(dateOfBirthExample),
      relationshipStatus=relationshipStatusExample.value,
      dependents=dependentsExample.value.toInt,
      dobOfDependents=dobOfDependentsExample.value.replace("[","").replace("]","").split(",").map(parseDate).flatMap(_.toSeq).toList,
      highestEducationAttained=highestEducationAttainedExample.value,
      employmentStatus=employmentStatusExample.value,
      creditRating= CreditRating(rating=ratingExample.value,
      source=sourceExample.value),
      creditLimit= CreditLimit(currency=currencyExample.value,
      amount=creditLimitAmountExample.value),
      kycStatus=kycStatusExample.value.toBoolean,
      lastOkDate=toDate(customerLastOkDateExample),
      title=customerTitleExample.value,
      branchId=branchIdExample.value,
      nameSuffix=nameSuffixExample.value)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getCustomers(bankId: BankId, callContext: Option[CallContext], queryParams: List[OBPQueryParam]): Future[Box[List[Customer]]] = {
        import com.openbankproject.commons.dto.{InBoundGetCustomers => InBound, OutBoundGetCustomers => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, OBPQueryParam.getLimit(queryParams), OBPQueryParam.getOffset(queryParams), OBPQueryParam.getFromDate(queryParams), OBPQueryParam.getToDate(queryParams))
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[CustomerCommons]](callContext))        
  }
          
  messageDocs += getCustomersByCustomerPhoneNumberDoc
  def getCustomersByCustomerPhoneNumberDoc = MessageDoc(
    process = "obp.getCustomersByCustomerPhoneNumber",
    messageFormat = messageFormat,
    description = "Get Customers By Customer Phone Number",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCustomersByCustomerPhoneNumber").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCustomersByCustomerPhoneNumber").request),
    exampleOutboundMessage = (
     OutBoundGetCustomersByCustomerPhoneNumber(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      phoneNumber=phoneNumberExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetCustomersByCustomerPhoneNumber(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( CustomerCommons(customerId=customerIdExample.value,
      bankId=bankIdExample.value,
      number=customerNumberExample.value,
      legalName=legalNameExample.value,
      mobileNumber=mobileNumberExample.value,
      email=emailExample.value,
      faceImage= CustomerFaceImage(date=toDate(customerFaceImageDateExample),
      url=urlExample.value),
      dateOfBirth=toDate(dateOfBirthExample),
      relationshipStatus=relationshipStatusExample.value,
      dependents=dependentsExample.value.toInt,
      dobOfDependents=dobOfDependentsExample.value.replace("[","").replace("]","").split(",").map(parseDate).flatMap(_.toSeq).toList,
      highestEducationAttained=highestEducationAttainedExample.value,
      employmentStatus=employmentStatusExample.value,
      creditRating= CreditRating(rating=ratingExample.value,
      source=sourceExample.value),
      creditLimit= CreditLimit(currency=currencyExample.value,
      amount=creditLimitAmountExample.value),
      kycStatus=kycStatusExample.value.toBoolean,
      lastOkDate=toDate(customerLastOkDateExample),
      title=customerTitleExample.value,
      branchId=branchIdExample.value,
      nameSuffix=nameSuffixExample.value)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getCustomersByCustomerPhoneNumber(bankId: BankId, phoneNumber: String, callContext: Option[CallContext]): OBPReturnType[Box[List[Customer]]] = {
        import com.openbankproject.commons.dto.{InBoundGetCustomersByCustomerPhoneNumber => InBound, OutBoundGetCustomersByCustomerPhoneNumber => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, phoneNumber)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[CustomerCommons]](callContext))        
  }
          
  messageDocs += getCheckbookOrdersDoc
  def getCheckbookOrdersDoc = MessageDoc(
    process = "obp.getCheckbookOrders",
    messageFormat = messageFormat,
    description = "Get Checkbook Orders",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCheckbookOrders").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCheckbookOrders").request),
    exampleOutboundMessage = (
     OutBoundGetCheckbookOrders(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=bankIdExample.value,
      accountId=accountIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetCheckbookOrders(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= CheckbookOrdersJson(account= AccountV310Json(bank_id=bank_idExample.value,
      account_id=account_idExample.value,
      account_type="string",
      account_routings=List( AccountRoutingJsonV121(scheme=schemeExample.value,
      address=addressExample.value)),
      branch_routings=List( BranchRoutingJsonV141(scheme=schemeExample.value,
      address=addressExample.value))),
      orders=List(OrderJson( OrderObjectJson(order_id="string",
      order_date="string",
      number_of_checkbooks="string",
      distribution_channel="string",
      status=statusExample.value,
      first_check_number="string",
      shipping_code="string")))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getCheckbookOrders(bankId: String, accountId: String, callContext: Option[CallContext]): Future[Box[(CheckbookOrdersJson, Option[CallContext])]] = {
        import com.openbankproject.commons.dto.{InBoundGetCheckbookOrders => InBound, OutBoundGetCheckbookOrders => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, accountId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[CheckbookOrdersJson](callContext))        
  }
          
  messageDocs += getStatusOfCreditCardOrderDoc
  def getStatusOfCreditCardOrderDoc = MessageDoc(
    process = "obp.getStatusOfCreditCardOrder",
    messageFormat = messageFormat,
    description = "Get Status Of Credit Card Order",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetStatusOfCreditCardOrder").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetStatusOfCreditCardOrder").request),
    exampleOutboundMessage = (
     OutBoundGetStatusOfCreditCardOrder(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=bankIdExample.value,
      accountId=accountIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetStatusOfCreditCardOrder(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( CardObjectJson(card_type="string",
      card_description="string",
      use_type="string")))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getStatusOfCreditCardOrder(bankId: String, accountId: String, callContext: Option[CallContext]): Future[Box[(List[CardObjectJson], Option[CallContext])]] = {
        import com.openbankproject.commons.dto.{InBoundGetStatusOfCreditCardOrder => InBound, OutBoundGetStatusOfCreditCardOrder => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, accountId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[CardObjectJson]](callContext))        
  }
          
  messageDocs += createUserAuthContextDoc
  def createUserAuthContextDoc = MessageDoc(
    process = "obp.createUserAuthContext",
    messageFormat = messageFormat,
    description = "Create User Auth Context",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateUserAuthContext").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateUserAuthContext").request),
    exampleOutboundMessage = (
     OutBoundCreateUserAuthContext(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      userId=userIdExample.value,
      key=keyExample.value,
      value=valueExample.value)
    ),
    exampleInboundMessage = (
     InBoundCreateUserAuthContext(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= UserAuthContextCommons(userAuthContextId=userAuthContextIdExample.value,
      userId=userIdExample.value,
      key=keyExample.value,
      value=valueExample.value,
      timeStamp=toDate(timeStampExample),
      consumerId=consumerIdExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createUserAuthContext(userId: String, key: String, value: String, callContext: Option[CallContext]): OBPReturnType[Box[UserAuthContext]] = {
        import com.openbankproject.commons.dto.{InBoundCreateUserAuthContext => InBound, OutBoundCreateUserAuthContext => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, userId, key, value)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[UserAuthContextCommons](callContext))        
  }
          
  messageDocs += createUserAuthContextUpdateDoc
  def createUserAuthContextUpdateDoc = MessageDoc(
    process = "obp.createUserAuthContextUpdate",
    messageFormat = messageFormat,
    description = "Create User Auth Context Update",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateUserAuthContextUpdate").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateUserAuthContextUpdate").request),
    exampleOutboundMessage = (
     OutBoundCreateUserAuthContextUpdate(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      userId=userIdExample.value,
      key=keyExample.value,
      value=valueExample.value)
    ),
    exampleInboundMessage = (
     InBoundCreateUserAuthContextUpdate(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= UserAuthContextUpdateCommons(userAuthContextUpdateId=userAuthContextUpdateIdExample.value,
      userId=userIdExample.value,
      key=keyExample.value,
      value=valueExample.value,
      challenge=challengeExample.value,
      status=statusExample.value,
      consumerId=consumerIdExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createUserAuthContextUpdate(userId: String, key: String, value: String, callContext: Option[CallContext]): OBPReturnType[Box[UserAuthContextUpdate]] = {
        import com.openbankproject.commons.dto.{InBoundCreateUserAuthContextUpdate => InBound, OutBoundCreateUserAuthContextUpdate => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, userId, key, value)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[UserAuthContextUpdateCommons](callContext))        
  }
          
  messageDocs += deleteUserAuthContextsDoc
  def deleteUserAuthContextsDoc = MessageDoc(
    process = "obp.deleteUserAuthContexts",
    messageFormat = messageFormat,
    description = "Delete User Auth Contexts",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundDeleteUserAuthContexts").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundDeleteUserAuthContexts").request),
    exampleOutboundMessage = (
     OutBoundDeleteUserAuthContexts(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      userId=userIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundDeleteUserAuthContexts(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=true)
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def deleteUserAuthContexts(userId: String, callContext: Option[CallContext]): OBPReturnType[Box[Boolean]] = {
        import com.openbankproject.commons.dto.{InBoundDeleteUserAuthContexts => InBound, OutBoundDeleteUserAuthContexts => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, userId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[Boolean](callContext))        
  }
          
  messageDocs += deleteUserAuthContextByIdDoc
  def deleteUserAuthContextByIdDoc = MessageDoc(
    process = "obp.deleteUserAuthContextById",
    messageFormat = messageFormat,
    description = "Delete User Auth Context By Id",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundDeleteUserAuthContextById").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundDeleteUserAuthContextById").request),
    exampleOutboundMessage = (
     OutBoundDeleteUserAuthContextById(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      userAuthContextId=userAuthContextIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundDeleteUserAuthContextById(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=true)
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def deleteUserAuthContextById(userAuthContextId: String, callContext: Option[CallContext]): OBPReturnType[Box[Boolean]] = {
        import com.openbankproject.commons.dto.{InBoundDeleteUserAuthContextById => InBound, OutBoundDeleteUserAuthContextById => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, userAuthContextId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[Boolean](callContext))        
  }
          
  messageDocs += getUserAuthContextsDoc
  def getUserAuthContextsDoc = MessageDoc(
    process = "obp.getUserAuthContexts",
    messageFormat = messageFormat,
    description = "Get User Auth Contexts",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetUserAuthContexts").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetUserAuthContexts").request),
    exampleOutboundMessage = (
     OutBoundGetUserAuthContexts(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      userId=userIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetUserAuthContexts(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( UserAuthContextCommons(userAuthContextId=userAuthContextIdExample.value,
      userId=userIdExample.value,
      key=keyExample.value,
      value=valueExample.value,
      timeStamp=toDate(timeStampExample),
      consumerId=consumerIdExample.value)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getUserAuthContexts(userId: String, callContext: Option[CallContext]): OBPReturnType[Box[List[UserAuthContext]]] = {
        import com.openbankproject.commons.dto.{InBoundGetUserAuthContexts => InBound, OutBoundGetUserAuthContexts => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, userId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[UserAuthContextCommons]](callContext))        
  }
          
  messageDocs += createOrUpdateProductAttributeDoc
  def createOrUpdateProductAttributeDoc = MessageDoc(
    process = "obp.createOrUpdateProductAttribute",
    messageFormat = messageFormat,
    description = "Create Or Update Product Attribute",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateOrUpdateProductAttribute").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateOrUpdateProductAttribute").request),
    exampleOutboundMessage = (
     OutBoundCreateOrUpdateProductAttribute(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      productCode=ProductCode(productCodeExample.value),
      productAttributeId=Some(productAttributeIdExample.value),
      name=nameExample.value,
      productAttributeType=com.openbankproject.commons.model.enums.ProductAttributeType.example,
      value=valueExample.value,
      isActive=Some(isActiveExample.value.toBoolean))
    ),
    exampleInboundMessage = (
     InBoundCreateOrUpdateProductAttribute(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= ProductAttributeCommons(bankId=BankId(bankIdExample.value),
      productCode=ProductCode(productCodeExample.value),
      productAttributeId=productAttributeIdExample.value,
      name=nameExample.value,
      attributeType=com.openbankproject.commons.model.enums.ProductAttributeType.example,
      value=valueExample.value,
      isActive=Some(isActiveExample.value.toBoolean)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createOrUpdateProductAttribute(bankId: BankId, productCode: ProductCode, productAttributeId: Option[String], name: String, productAttributeType: ProductAttributeType.Value, value: String, isActive: Option[Boolean], callContext: Option[CallContext]): OBPReturnType[Box[ProductAttribute]] = {
        import com.openbankproject.commons.dto.{InBoundCreateOrUpdateProductAttribute => InBound, OutBoundCreateOrUpdateProductAttribute => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, productCode, productAttributeId, name, productAttributeType, value, isActive)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[ProductAttributeCommons](callContext))        
  }
          
  messageDocs += getProductAttributeByIdDoc
  def getProductAttributeByIdDoc = MessageDoc(
    process = "obp.getProductAttributeById",
    messageFormat = messageFormat,
    description = "Get Product Attribute By Id",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetProductAttributeById").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetProductAttributeById").request),
    exampleOutboundMessage = (
     OutBoundGetProductAttributeById(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      productAttributeId=productAttributeIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetProductAttributeById(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= ProductAttributeCommons(bankId=BankId(bankIdExample.value),
      productCode=ProductCode(productCodeExample.value),
      productAttributeId=productAttributeIdExample.value,
      name=nameExample.value,
      attributeType=com.openbankproject.commons.model.enums.ProductAttributeType.example,
      value=valueExample.value,
      isActive=Some(isActiveExample.value.toBoolean)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getProductAttributeById(productAttributeId: String, callContext: Option[CallContext]): OBPReturnType[Box[ProductAttribute]] = {
        import com.openbankproject.commons.dto.{InBoundGetProductAttributeById => InBound, OutBoundGetProductAttributeById => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, productAttributeId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[ProductAttributeCommons](callContext))        
  }
          
  messageDocs += getProductAttributesByBankAndCodeDoc
  def getProductAttributesByBankAndCodeDoc = MessageDoc(
    process = "obp.getProductAttributesByBankAndCode",
    messageFormat = messageFormat,
    description = "Get Product Attributes By Bank And Code",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetProductAttributesByBankAndCode").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetProductAttributesByBankAndCode").request),
    exampleOutboundMessage = (
     OutBoundGetProductAttributesByBankAndCode(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bank=BankId(bankExample.value),
      productCode=ProductCode(productCodeExample.value))
    ),
    exampleInboundMessage = (
     InBoundGetProductAttributesByBankAndCode(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( ProductAttributeCommons(bankId=BankId(bankIdExample.value),
      productCode=ProductCode(productCodeExample.value),
      productAttributeId=productAttributeIdExample.value,
      name=nameExample.value,
      attributeType=com.openbankproject.commons.model.enums.ProductAttributeType.example,
      value=valueExample.value,
      isActive=Some(isActiveExample.value.toBoolean))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getProductAttributesByBankAndCode(bank: BankId, productCode: ProductCode, callContext: Option[CallContext]): OBPReturnType[Box[List[ProductAttribute]]] = {
        import com.openbankproject.commons.dto.{InBoundGetProductAttributesByBankAndCode => InBound, OutBoundGetProductAttributesByBankAndCode => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bank, productCode)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[ProductAttributeCommons]](callContext))        
  }
          
  messageDocs += deleteProductAttributeDoc
  def deleteProductAttributeDoc = MessageDoc(
    process = "obp.deleteProductAttribute",
    messageFormat = messageFormat,
    description = "Delete Product Attribute",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundDeleteProductAttribute").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundDeleteProductAttribute").request),
    exampleOutboundMessage = (
     OutBoundDeleteProductAttribute(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      productAttributeId=productAttributeIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundDeleteProductAttribute(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=true)
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def deleteProductAttribute(productAttributeId: String, callContext: Option[CallContext]): OBPReturnType[Box[Boolean]] = {
        import com.openbankproject.commons.dto.{InBoundDeleteProductAttribute => InBound, OutBoundDeleteProductAttribute => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, productAttributeId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[Boolean](callContext))        
  }
          
  messageDocs += getAccountAttributeByIdDoc
  def getAccountAttributeByIdDoc = MessageDoc(
    process = "obp.getAccountAttributeById",
    messageFormat = messageFormat,
    description = "Get Account Attribute By Id",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetAccountAttributeById").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetAccountAttributeById").request),
    exampleOutboundMessage = (
     OutBoundGetAccountAttributeById(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      accountAttributeId=accountAttributeIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetAccountAttributeById(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= AccountAttributeCommons(bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value),
      productCode=ProductCode(productCodeExample.value),
      accountAttributeId=accountAttributeIdExample.value,
      name=nameExample.value,
      attributeType=com.openbankproject.commons.model.enums.AccountAttributeType.example,
      value=valueExample.value,
      productInstanceCode=Some("string")))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getAccountAttributeById(accountAttributeId: String, callContext: Option[CallContext]): OBPReturnType[Box[AccountAttribute]] = {
        import com.openbankproject.commons.dto.{InBoundGetAccountAttributeById => InBound, OutBoundGetAccountAttributeById => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, accountAttributeId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[AccountAttributeCommons](callContext))        
  }
          
  messageDocs += getTransactionAttributeByIdDoc
  def getTransactionAttributeByIdDoc = MessageDoc(
    process = "obp.getTransactionAttributeById",
    messageFormat = messageFormat,
    description = "Get Transaction Attribute By Id",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetTransactionAttributeById").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetTransactionAttributeById").request),
    exampleOutboundMessage = (
     OutBoundGetTransactionAttributeById(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      transactionAttributeId=transactionAttributeIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetTransactionAttributeById(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= TransactionAttributeCommons(bankId=BankId(bankIdExample.value),
      transactionId=TransactionId(transactionIdExample.value),
      transactionAttributeId=transactionAttributeIdExample.value,
      attributeType=com.openbankproject.commons.model.enums.TransactionAttributeType.example,
      name=transactionAttributeNameExample.value,
      value=transactionAttributeValueExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getTransactionAttributeById(transactionAttributeId: String, callContext: Option[CallContext]): OBPReturnType[Box[TransactionAttribute]] = {
        import com.openbankproject.commons.dto.{InBoundGetTransactionAttributeById => InBound, OutBoundGetTransactionAttributeById => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, transactionAttributeId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[TransactionAttributeCommons](callContext))        
  }
          
  messageDocs += createOrUpdateAccountAttributeDoc
  def createOrUpdateAccountAttributeDoc = MessageDoc(
    process = "obp.createOrUpdateAccountAttribute",
    messageFormat = messageFormat,
    description = "Create Or Update Account Attribute",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateOrUpdateAccountAttribute").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateOrUpdateAccountAttribute").request),
    exampleOutboundMessage = (
     OutBoundCreateOrUpdateAccountAttribute(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value),
      productCode=ProductCode(productCodeExample.value),
      productAttributeId=Some(productAttributeIdExample.value),
      name=nameExample.value,
      accountAttributeType=com.openbankproject.commons.model.enums.AccountAttributeType.example,
      value=valueExample.value,
      productInstanceCode=Some("string"))
    ),
    exampleInboundMessage = (
     InBoundCreateOrUpdateAccountAttribute(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= AccountAttributeCommons(bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value),
      productCode=ProductCode(productCodeExample.value),
      accountAttributeId=accountAttributeIdExample.value,
      name=nameExample.value,
      attributeType=com.openbankproject.commons.model.enums.AccountAttributeType.example,
      value=valueExample.value,
      productInstanceCode=Some("string")))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createOrUpdateAccountAttribute(bankId: BankId, accountId: AccountId, productCode: ProductCode, productAttributeId: Option[String], name: String, accountAttributeType: AccountAttributeType.Value, value: String, productInstanceCode: Option[String], callContext: Option[CallContext]): OBPReturnType[Box[AccountAttribute]] = {
        import com.openbankproject.commons.dto.{InBoundCreateOrUpdateAccountAttribute => InBound, OutBoundCreateOrUpdateAccountAttribute => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, accountId, productCode, productAttributeId, name, accountAttributeType, value, productInstanceCode)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[AccountAttributeCommons](callContext))        
  }
          
  messageDocs += createOrUpdateCustomerAttributeDoc
  def createOrUpdateCustomerAttributeDoc = MessageDoc(
    process = "obp.createOrUpdateCustomerAttribute",
    messageFormat = messageFormat,
    description = "Create Or Update Customer Attribute",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateOrUpdateCustomerAttribute").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateOrUpdateCustomerAttribute").request),
    exampleOutboundMessage = (
     OutBoundCreateOrUpdateCustomerAttribute(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      customerId=CustomerId(customerIdExample.value),
      customerAttributeId=Some(customerAttributeIdExample.value),
      name=nameExample.value,
      attributeType=com.openbankproject.commons.model.enums.CustomerAttributeType.example,
      value=valueExample.value)
    ),
    exampleInboundMessage = (
     InBoundCreateOrUpdateCustomerAttribute(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= CustomerAttributeCommons(bankId=BankId(bankIdExample.value),
      customerId=CustomerId(customerIdExample.value),
      customerAttributeId=customerAttributeIdExample.value,
      attributeType=com.openbankproject.commons.model.enums.CustomerAttributeType.example,
      name=customerAttributeNameExample.value,
      value=customerAttributeValueExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createOrUpdateCustomerAttribute(bankId: BankId, customerId: CustomerId, customerAttributeId: Option[String], name: String, attributeType: CustomerAttributeType.Value, value: String, callContext: Option[CallContext]): OBPReturnType[Box[CustomerAttribute]] = {
        import com.openbankproject.commons.dto.{InBoundCreateOrUpdateCustomerAttribute => InBound, OutBoundCreateOrUpdateCustomerAttribute => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, customerId, customerAttributeId, name, attributeType, value)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[CustomerAttributeCommons](callContext))        
  }
          
  messageDocs += createOrUpdateTransactionAttributeDoc
  def createOrUpdateTransactionAttributeDoc = MessageDoc(
    process = "obp.createOrUpdateTransactionAttribute",
    messageFormat = messageFormat,
    description = "Create Or Update Transaction Attribute",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateOrUpdateTransactionAttribute").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateOrUpdateTransactionAttribute").request),
    exampleOutboundMessage = (
     OutBoundCreateOrUpdateTransactionAttribute(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      transactionId=TransactionId(transactionIdExample.value),
      transactionAttributeId=Some(transactionAttributeIdExample.value),
      name=nameExample.value,
      attributeType=com.openbankproject.commons.model.enums.TransactionAttributeType.example,
      value=valueExample.value)
    ),
    exampleInboundMessage = (
     InBoundCreateOrUpdateTransactionAttribute(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= TransactionAttributeCommons(bankId=BankId(bankIdExample.value),
      transactionId=TransactionId(transactionIdExample.value),
      transactionAttributeId=transactionAttributeIdExample.value,
      attributeType=com.openbankproject.commons.model.enums.TransactionAttributeType.example,
      name=transactionAttributeNameExample.value,
      value=transactionAttributeValueExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createOrUpdateTransactionAttribute(bankId: BankId, transactionId: TransactionId, transactionAttributeId: Option[String], name: String, attributeType: TransactionAttributeType.Value, value: String, callContext: Option[CallContext]): OBPReturnType[Box[TransactionAttribute]] = {
        import com.openbankproject.commons.dto.{InBoundCreateOrUpdateTransactionAttribute => InBound, OutBoundCreateOrUpdateTransactionAttribute => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, transactionId, transactionAttributeId, name, attributeType, value)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[TransactionAttributeCommons](callContext))        
  }
          
  messageDocs += createAccountAttributesDoc
  def createAccountAttributesDoc = MessageDoc(
    process = "obp.createAccountAttributes",
    messageFormat = messageFormat,
    description = "Create Account Attributes",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateAccountAttributes").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateAccountAttributes").request),
    exampleOutboundMessage = (
     OutBoundCreateAccountAttributes(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value),
      productCode=ProductCode(productCodeExample.value),
      accountAttributes=List( ProductAttributeCommons(bankId=BankId(bankIdExample.value),
      productCode=ProductCode(productCodeExample.value),
      productAttributeId=productAttributeIdExample.value,
      name=nameExample.value,
      attributeType=com.openbankproject.commons.model.enums.ProductAttributeType.example,
      value=valueExample.value,
      isActive=Some(isActiveExample.value.toBoolean))),
      productInstanceCode=Some("string"))
    ),
    exampleInboundMessage = (
     InBoundCreateAccountAttributes(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( AccountAttributeCommons(bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value),
      productCode=ProductCode(productCodeExample.value),
      accountAttributeId=accountAttributeIdExample.value,
      name=nameExample.value,
      attributeType=com.openbankproject.commons.model.enums.AccountAttributeType.example,
      value=valueExample.value,
      productInstanceCode=Some("string"))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createAccountAttributes(bankId: BankId, accountId: AccountId, productCode: ProductCode, accountAttributes: List[ProductAttribute], productInstanceCode: Option[String], callContext: Option[CallContext]): OBPReturnType[Box[List[AccountAttribute]]] = {
        import com.openbankproject.commons.dto.{InBoundCreateAccountAttributes => InBound, OutBoundCreateAccountAttributes => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, accountId, productCode, accountAttributes, productInstanceCode)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[AccountAttributeCommons]](callContext))        
  }
          
  messageDocs += getAccountAttributesByAccountDoc
  def getAccountAttributesByAccountDoc = MessageDoc(
    process = "obp.getAccountAttributesByAccount",
    messageFormat = messageFormat,
    description = "Get Account Attributes By Account",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetAccountAttributesByAccount").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetAccountAttributesByAccount").request),
    exampleOutboundMessage = (
     OutBoundGetAccountAttributesByAccount(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value))
    ),
    exampleInboundMessage = (
     InBoundGetAccountAttributesByAccount(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( AccountAttributeCommons(bankId=BankId(bankIdExample.value),
      accountId=AccountId(accountIdExample.value),
      productCode=ProductCode(productCodeExample.value),
      accountAttributeId=accountAttributeIdExample.value,
      name=nameExample.value,
      attributeType=com.openbankproject.commons.model.enums.AccountAttributeType.example,
      value=valueExample.value,
      productInstanceCode=Some("string"))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getAccountAttributesByAccount(bankId: BankId, accountId: AccountId, callContext: Option[CallContext]): OBPReturnType[Box[List[AccountAttribute]]] = {
        import com.openbankproject.commons.dto.{InBoundGetAccountAttributesByAccount => InBound, OutBoundGetAccountAttributesByAccount => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, accountId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[AccountAttributeCommons]](callContext))        
  }
          
  messageDocs += getCustomerAttributesDoc
  def getCustomerAttributesDoc = MessageDoc(
    process = "obp.getCustomerAttributes",
    messageFormat = messageFormat,
    description = "Get Customer Attributes",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCustomerAttributes").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCustomerAttributes").request),
    exampleOutboundMessage = (
     OutBoundGetCustomerAttributes(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      customerId=CustomerId(customerIdExample.value))
    ),
    exampleInboundMessage = (
     InBoundGetCustomerAttributes(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( CustomerAttributeCommons(bankId=BankId(bankIdExample.value),
      customerId=CustomerId(customerIdExample.value),
      customerAttributeId=customerAttributeIdExample.value,
      attributeType=com.openbankproject.commons.model.enums.CustomerAttributeType.example,
      name=customerAttributeNameExample.value,
      value=customerAttributeValueExample.value)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getCustomerAttributes(bankId: BankId, customerId: CustomerId, callContext: Option[CallContext]): OBPReturnType[Box[List[CustomerAttribute]]] = {
        import com.openbankproject.commons.dto.{InBoundGetCustomerAttributes => InBound, OutBoundGetCustomerAttributes => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, customerId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[CustomerAttributeCommons]](callContext))        
  }
          
  messageDocs += getCustomerIdsByAttributeNameValuesDoc
  def getCustomerIdsByAttributeNameValuesDoc = MessageDoc(
    process = "obp.getCustomerIdsByAttributeNameValues",
    messageFormat = messageFormat,
    description = "Get Customer Ids By Attribute Name Values",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCustomerIdsByAttributeNameValues").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCustomerIdsByAttributeNameValues").request),
    exampleOutboundMessage = (
     OutBoundGetCustomerIdsByAttributeNameValues(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      nameValues=Map("some_name" -> List("name1", "name2")))
    ),
    exampleInboundMessage = (
     InBoundGetCustomerIdsByAttributeNameValues(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=listExample.value.replace("[","").replace("]","").split(",").toList)
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getCustomerIdsByAttributeNameValues(bankId: BankId, nameValues: Map[String,List[String]], callContext: Option[CallContext]): OBPReturnType[Box[List[String]]] = {
        import com.openbankproject.commons.dto.{InBoundGetCustomerIdsByAttributeNameValues => InBound, OutBoundGetCustomerIdsByAttributeNameValues => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, nameValues)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[String]](callContext))        
  }
          
  messageDocs += getCustomerAttributesForCustomersDoc
  def getCustomerAttributesForCustomersDoc = MessageDoc(
    process = "obp.getCustomerAttributesForCustomers",
    messageFormat = messageFormat,
    description = "Get Customer Attributes For Customers",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCustomerAttributesForCustomers").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCustomerAttributesForCustomers").request),
    exampleOutboundMessage = (
     OutBoundGetCustomerAttributesForCustomers(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      customers=List( CustomerCommons(customerId=customerIdExample.value,
      bankId=bankIdExample.value,
      number=customerNumberExample.value,
      legalName=legalNameExample.value,
      mobileNumber=mobileNumberExample.value,
      email=emailExample.value,
      faceImage= CustomerFaceImage(date=toDate(customerFaceImageDateExample),
      url=urlExample.value),
      dateOfBirth=toDate(dateOfBirthExample),
      relationshipStatus=relationshipStatusExample.value,
      dependents=dependentsExample.value.toInt,
      dobOfDependents=dobOfDependentsExample.value.replace("[","").replace("]","").split(",").map(parseDate).flatMap(_.toSeq).toList,
      highestEducationAttained=highestEducationAttainedExample.value,
      employmentStatus=employmentStatusExample.value,
      creditRating= CreditRating(rating=ratingExample.value,
      source=sourceExample.value),
      creditLimit= CreditLimit(currency=currencyExample.value,
      amount=creditLimitAmountExample.value),
      kycStatus=kycStatusExample.value.toBoolean,
      lastOkDate=toDate(customerLastOkDateExample),
      title=customerTitleExample.value,
      branchId=branchIdExample.value,
      nameSuffix=nameSuffixExample.value)))
    ),
    exampleInboundMessage = (
     InBoundGetCustomerAttributesForCustomers(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= List(
         CustomerAndAttribute(
             MessageDocsSwaggerDefinitions.customerCommons,
             List(MessageDocsSwaggerDefinitions.customerAttribute)
          )
         )
      )
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getCustomerAttributesForCustomers(customers: List[Customer], callContext: Option[CallContext]): OBPReturnType[Box[List[CustomerAndAttribute]]] = {
        import com.openbankproject.commons.dto.{InBoundGetCustomerAttributesForCustomers => InBound, OutBoundGetCustomerAttributesForCustomers => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, customers)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[CustomerAndAttribute]](callContext))        
  }
          
  messageDocs += getTransactionIdsByAttributeNameValuesDoc
  def getTransactionIdsByAttributeNameValuesDoc = MessageDoc(
    process = "obp.getTransactionIdsByAttributeNameValues",
    messageFormat = messageFormat,
    description = "Get Transaction Ids By Attribute Name Values",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetTransactionIdsByAttributeNameValues").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetTransactionIdsByAttributeNameValues").request),
    exampleOutboundMessage = (
     OutBoundGetTransactionIdsByAttributeNameValues(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      nameValues=Map("some_name" -> List("name1", "name2")))
    ),
    exampleInboundMessage = (
     InBoundGetTransactionIdsByAttributeNameValues(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=listExample.value.replace("[","").replace("]","").split(",").toList)
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getTransactionIdsByAttributeNameValues(bankId: BankId, nameValues: Map[String,List[String]], callContext: Option[CallContext]): OBPReturnType[Box[List[String]]] = {
        import com.openbankproject.commons.dto.{InBoundGetTransactionIdsByAttributeNameValues => InBound, OutBoundGetTransactionIdsByAttributeNameValues => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, nameValues)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[String]](callContext))        
  }
          
  messageDocs += getTransactionAttributesDoc
  def getTransactionAttributesDoc = MessageDoc(
    process = "obp.getTransactionAttributes",
    messageFormat = messageFormat,
    description = "Get Transaction Attributes",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetTransactionAttributes").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetTransactionAttributes").request),
    exampleOutboundMessage = (
     OutBoundGetTransactionAttributes(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      transactionId=TransactionId(transactionIdExample.value))
    ),
    exampleInboundMessage = (
     InBoundGetTransactionAttributes(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( TransactionAttributeCommons(bankId=BankId(bankIdExample.value),
      transactionId=TransactionId(transactionIdExample.value),
      transactionAttributeId=transactionAttributeIdExample.value,
      attributeType=com.openbankproject.commons.model.enums.TransactionAttributeType.example,
      name=transactionAttributeNameExample.value,
      value=transactionAttributeValueExample.value)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getTransactionAttributes(bankId: BankId, transactionId: TransactionId, callContext: Option[CallContext]): OBPReturnType[Box[List[TransactionAttribute]]] = {
        import com.openbankproject.commons.dto.{InBoundGetTransactionAttributes => InBound, OutBoundGetTransactionAttributes => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, transactionId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[TransactionAttributeCommons]](callContext))        
  }
          
  messageDocs += getCustomerAttributeByIdDoc
  def getCustomerAttributeByIdDoc = MessageDoc(
    process = "obp.getCustomerAttributeById",
    messageFormat = messageFormat,
    description = "Get Customer Attribute By Id",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCustomerAttributeById").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCustomerAttributeById").request),
    exampleOutboundMessage = (
     OutBoundGetCustomerAttributeById(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      customerAttributeId=customerAttributeIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetCustomerAttributeById(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= CustomerAttributeCommons(bankId=BankId(bankIdExample.value),
      customerId=CustomerId(customerIdExample.value),
      customerAttributeId=customerAttributeIdExample.value,
      attributeType=com.openbankproject.commons.model.enums.CustomerAttributeType.example,
      name=customerAttributeNameExample.value,
      value=customerAttributeValueExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getCustomerAttributeById(customerAttributeId: String, callContext: Option[CallContext]): OBPReturnType[Box[CustomerAttribute]] = {
        import com.openbankproject.commons.dto.{InBoundGetCustomerAttributeById => InBound, OutBoundGetCustomerAttributeById => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, customerAttributeId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[CustomerAttributeCommons](callContext))        
  }
          
  messageDocs += createOrUpdateCardAttributeDoc
  def createOrUpdateCardAttributeDoc = MessageDoc(
    process = "obp.createOrUpdateCardAttribute",
    messageFormat = messageFormat,
    description = "Create Or Update Card Attribute",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateOrUpdateCardAttribute").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateOrUpdateCardAttribute").request),
    exampleOutboundMessage = (
     OutBoundCreateOrUpdateCardAttribute(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=Some(BankId(bankIdExample.value)),
      cardId=Some(cardIdExample.value),
      cardAttributeId=Some(cardAttributeIdExample.value),
      name=nameExample.value,
      cardAttributeType=com.openbankproject.commons.model.enums.CardAttributeType.example,
      value=valueExample.value)
    ),
    exampleInboundMessage = (
     InBoundCreateOrUpdateCardAttribute(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= CardAttributeCommons(bankId=Some(BankId(bankIdExample.value)),
      cardId=Some(cardIdExample.value),
      cardAttributeId=Some(cardAttributeIdExample.value),
      name=cardAttributeNameExample.value,
      attributeType=com.openbankproject.commons.model.enums.CardAttributeType.example,
      value=cardAttributeValueExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createOrUpdateCardAttribute(bankId: Option[BankId], cardId: Option[String], cardAttributeId: Option[String], name: String, cardAttributeType: CardAttributeType.Value, value: String, callContext: Option[CallContext]): OBPReturnType[Box[CardAttribute]] = {
        import com.openbankproject.commons.dto.{InBoundCreateOrUpdateCardAttribute => InBound, OutBoundCreateOrUpdateCardAttribute => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, cardId, cardAttributeId, name, cardAttributeType, value)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[CardAttributeCommons](callContext))        
  }
          
  messageDocs += getCardAttributeByIdDoc
  def getCardAttributeByIdDoc = MessageDoc(
    process = "obp.getCardAttributeById",
    messageFormat = messageFormat,
    description = "Get Card Attribute By Id",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCardAttributeById").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCardAttributeById").request),
    exampleOutboundMessage = (
     OutBoundGetCardAttributeById(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      cardAttributeId=cardAttributeIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetCardAttributeById(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= CardAttributeCommons(bankId=Some(BankId(bankIdExample.value)),
      cardId=Some(cardIdExample.value),
      cardAttributeId=Some(cardAttributeIdExample.value),
      name=cardAttributeNameExample.value,
      attributeType=com.openbankproject.commons.model.enums.CardAttributeType.example,
      value=cardAttributeValueExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getCardAttributeById(cardAttributeId: String, callContext: Option[CallContext]): OBPReturnType[Box[CardAttribute]] = {
        import com.openbankproject.commons.dto.{InBoundGetCardAttributeById => InBound, OutBoundGetCardAttributeById => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, cardAttributeId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[CardAttributeCommons](callContext))        
  }
          
  messageDocs += getCardAttributesFromProviderDoc
  def getCardAttributesFromProviderDoc = MessageDoc(
    process = "obp.getCardAttributesFromProvider",
    messageFormat = messageFormat,
    description = "Get Card Attributes From Provider",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCardAttributesFromProvider").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetCardAttributesFromProvider").request),
    exampleOutboundMessage = (
     OutBoundGetCardAttributesFromProvider(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      cardId=cardIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetCardAttributesFromProvider(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( CardAttributeCommons(bankId=Some(BankId(bankIdExample.value)),
      cardId=Some(cardIdExample.value),
      cardAttributeId=Some(cardAttributeIdExample.value),
      name=cardAttributeNameExample.value,
      attributeType=com.openbankproject.commons.model.enums.CardAttributeType.example,
      value=cardAttributeValueExample.value)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getCardAttributesFromProvider(cardId: String, callContext: Option[CallContext]): OBPReturnType[Box[List[CardAttribute]]] = {
        import com.openbankproject.commons.dto.{InBoundGetCardAttributesFromProvider => InBound, OutBoundGetCardAttributesFromProvider => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, cardId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[CardAttributeCommons]](callContext))        
  }
          
  messageDocs += createAccountApplicationDoc
  def createAccountApplicationDoc = MessageDoc(
    process = "obp.createAccountApplication",
    messageFormat = messageFormat,
    description = "Create Account Application",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateAccountApplication").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateAccountApplication").request),
    exampleOutboundMessage = (
     OutBoundCreateAccountApplication(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      productCode=ProductCode(productCodeExample.value),
      userId=Some(userIdExample.value),
      customerId=Some(customerIdExample.value))
    ),
    exampleInboundMessage = (
     InBoundCreateAccountApplication(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= AccountApplicationCommons(accountApplicationId=accountApplicationIdExample.value,
      productCode=ProductCode(productCodeExample.value),
      userId=userIdExample.value,
      customerId=customerIdExample.value,
      dateOfApplication=toDate(dateOfApplicationExample),
      status=statusExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createAccountApplication(productCode: ProductCode, userId: Option[String], customerId: Option[String], callContext: Option[CallContext]): OBPReturnType[Box[AccountApplication]] = {
        import com.openbankproject.commons.dto.{InBoundCreateAccountApplication => InBound, OutBoundCreateAccountApplication => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, productCode, userId, customerId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[AccountApplicationCommons](callContext))        
  }
          
  messageDocs += getAllAccountApplicationDoc
  def getAllAccountApplicationDoc = MessageDoc(
    process = "obp.getAllAccountApplication",
    messageFormat = messageFormat,
    description = "Get All Account Application",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetAllAccountApplication").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetAllAccountApplication").request),
    exampleOutboundMessage = (
          OutBoundGetAllAccountApplication(MessageDocsSwaggerDefinitions.outboundAdapterCallContext)
    ),
    exampleInboundMessage = (
     InBoundGetAllAccountApplication(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( AccountApplicationCommons(accountApplicationId=accountApplicationIdExample.value,
      productCode=ProductCode(productCodeExample.value),
      userId=userIdExample.value,
      customerId=customerIdExample.value,
      dateOfApplication=toDate(dateOfApplicationExample),
      status=statusExample.value)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getAllAccountApplication(callContext: Option[CallContext]): OBPReturnType[Box[List[AccountApplication]]] = {
        import com.openbankproject.commons.dto.{InBoundGetAllAccountApplication => InBound, OutBoundGetAllAccountApplication => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[AccountApplicationCommons]](callContext))        
  }
          
  messageDocs += getAccountApplicationByIdDoc
  def getAccountApplicationByIdDoc = MessageDoc(
    process = "obp.getAccountApplicationById",
    messageFormat = messageFormat,
    description = "Get Account Application By Id",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetAccountApplicationById").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetAccountApplicationById").request),
    exampleOutboundMessage = (
     OutBoundGetAccountApplicationById(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      accountApplicationId=accountApplicationIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetAccountApplicationById(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= AccountApplicationCommons(accountApplicationId=accountApplicationIdExample.value,
      productCode=ProductCode(productCodeExample.value),
      userId=userIdExample.value,
      customerId=customerIdExample.value,
      dateOfApplication=toDate(dateOfApplicationExample),
      status=statusExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getAccountApplicationById(accountApplicationId: String, callContext: Option[CallContext]): OBPReturnType[Box[AccountApplication]] = {
        import com.openbankproject.commons.dto.{InBoundGetAccountApplicationById => InBound, OutBoundGetAccountApplicationById => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, accountApplicationId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[AccountApplicationCommons](callContext))        
  }
          
  messageDocs += updateAccountApplicationStatusDoc
  def updateAccountApplicationStatusDoc = MessageDoc(
    process = "obp.updateAccountApplicationStatus",
    messageFormat = messageFormat,
    description = "Update Account Application Status",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundUpdateAccountApplicationStatus").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundUpdateAccountApplicationStatus").request),
    exampleOutboundMessage = (
     OutBoundUpdateAccountApplicationStatus(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      accountApplicationId=accountApplicationIdExample.value,
      status=statusExample.value)
    ),
    exampleInboundMessage = (
     InBoundUpdateAccountApplicationStatus(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= AccountApplicationCommons(accountApplicationId=accountApplicationIdExample.value,
      productCode=ProductCode(productCodeExample.value),
      userId=userIdExample.value,
      customerId=customerIdExample.value,
      dateOfApplication=toDate(dateOfApplicationExample),
      status=statusExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def updateAccountApplicationStatus(accountApplicationId: String, status: String, callContext: Option[CallContext]): OBPReturnType[Box[AccountApplication]] = {
        import com.openbankproject.commons.dto.{InBoundUpdateAccountApplicationStatus => InBound, OutBoundUpdateAccountApplicationStatus => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, accountApplicationId, status)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[AccountApplicationCommons](callContext))        
  }
          
  messageDocs += getOrCreateProductCollectionDoc
  def getOrCreateProductCollectionDoc = MessageDoc(
    process = "obp.getOrCreateProductCollection",
    messageFormat = messageFormat,
    description = "Get Or Create Product Collection",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetOrCreateProductCollection").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetOrCreateProductCollection").request),
    exampleOutboundMessage = (
     OutBoundGetOrCreateProductCollection(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      collectionCode=collectionCodeExample.value,
      productCodes=listExample.value.replace("[","").replace("]","").split(",").toList)
    ),
    exampleInboundMessage = (
     InBoundGetOrCreateProductCollection(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( ProductCollectionCommons(collectionCode=collectionCodeExample.value,
      productCode=productCodeExample.value)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getOrCreateProductCollection(collectionCode: String, productCodes: List[String], callContext: Option[CallContext]): OBPReturnType[Box[List[ProductCollection]]] = {
        import com.openbankproject.commons.dto.{InBoundGetOrCreateProductCollection => InBound, OutBoundGetOrCreateProductCollection => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, collectionCode, productCodes)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[ProductCollectionCommons]](callContext))        
  }
          
  messageDocs += getProductCollectionDoc
  def getProductCollectionDoc = MessageDoc(
    process = "obp.getProductCollection",
    messageFormat = messageFormat,
    description = "Get Product Collection",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetProductCollection").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetProductCollection").request),
    exampleOutboundMessage = (
     OutBoundGetProductCollection(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      collectionCode=collectionCodeExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetProductCollection(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( ProductCollectionCommons(collectionCode=collectionCodeExample.value,
      productCode=productCodeExample.value)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getProductCollection(collectionCode: String, callContext: Option[CallContext]): OBPReturnType[Box[List[ProductCollection]]] = {
        import com.openbankproject.commons.dto.{InBoundGetProductCollection => InBound, OutBoundGetProductCollection => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, collectionCode)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[ProductCollectionCommons]](callContext))        
  }
          
  messageDocs += getOrCreateProductCollectionItemDoc
  def getOrCreateProductCollectionItemDoc = MessageDoc(
    process = "obp.getOrCreateProductCollectionItem",
    messageFormat = messageFormat,
    description = "Get Or Create Product Collection Item",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetOrCreateProductCollectionItem").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetOrCreateProductCollectionItem").request),
    exampleOutboundMessage = (
     OutBoundGetOrCreateProductCollectionItem(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      collectionCode=collectionCodeExample.value,
      memberProductCodes=listExample.value.replace("[","").replace("]","").split(",").toList)
    ),
    exampleInboundMessage = (
     InBoundGetOrCreateProductCollectionItem(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( ProductCollectionItemCommons(collectionCode=collectionCodeExample.value,
      memberProductCode=memberProductCodeExample.value)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getOrCreateProductCollectionItem(collectionCode: String, memberProductCodes: List[String], callContext: Option[CallContext]): OBPReturnType[Box[List[ProductCollectionItem]]] = {
        import com.openbankproject.commons.dto.{InBoundGetOrCreateProductCollectionItem => InBound, OutBoundGetOrCreateProductCollectionItem => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, collectionCode, memberProductCodes)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[ProductCollectionItemCommons]](callContext))        
  }
          
  messageDocs += getProductCollectionItemDoc
  def getProductCollectionItemDoc = MessageDoc(
    process = "obp.getProductCollectionItem",
    messageFormat = messageFormat,
    description = "Get Product Collection Item",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetProductCollectionItem").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetProductCollectionItem").request),
    exampleOutboundMessage = (
     OutBoundGetProductCollectionItem(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      collectionCode=collectionCodeExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetProductCollectionItem(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( ProductCollectionItemCommons(collectionCode=collectionCodeExample.value,
      memberProductCode=memberProductCodeExample.value)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getProductCollectionItem(collectionCode: String, callContext: Option[CallContext]): OBPReturnType[Box[List[ProductCollectionItem]]] = {
        import com.openbankproject.commons.dto.{InBoundGetProductCollectionItem => InBound, OutBoundGetProductCollectionItem => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, collectionCode)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[ProductCollectionItemCommons]](callContext))        
  }
          
  messageDocs += getProductCollectionItemsTreeDoc
  def getProductCollectionItemsTreeDoc = MessageDoc(
    process = "obp.getProductCollectionItemsTree",
    messageFormat = messageFormat,
    description = "Get Product Collection Items Tree",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetProductCollectionItemsTree").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetProductCollectionItemsTree").request),
    exampleOutboundMessage = (
     OutBoundGetProductCollectionItemsTree(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      collectionCode=collectionCodeExample.value,
      bankId=bankIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetProductCollectionItemsTree(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( ProductCollectionItemsTree(productCollectionItem= ProductCollectionItemCommons(collectionCode=collectionCodeExample.value,
      memberProductCode=memberProductCodeExample.value),
      product= ProductCommons(bankId=BankId(bankIdExample.value),
      code=ProductCode(productCodeExample.value),
      parentProductCode=ProductCode(parentProductCodeExample.value),
      name=productNameExample.value,
      category=categoryExample.value,
      family=familyExample.value,
      superFamily=superFamilyExample.value,
      moreInfoUrl=moreInfoUrlExample.value,
      termsAndConditionsUrl=termsAndConditionsUrlExample.value,
      details=detailsExample.value,
      description=descriptionExample.value,
      meta=Meta( License(id=licenseIdExample.value,
      name=licenseNameExample.value))),
      attributes=List( ProductAttributeCommons(bankId=BankId(bankIdExample.value),
      productCode=ProductCode(productCodeExample.value),
      productAttributeId=productAttributeIdExample.value,
      name=nameExample.value,
      attributeType=com.openbankproject.commons.model.enums.ProductAttributeType.example,
      value=valueExample.value,
      isActive=Some(isActiveExample.value.toBoolean))))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getProductCollectionItemsTree(collectionCode: String, bankId: String, callContext: Option[CallContext]): OBPReturnType[Box[List[ProductCollectionItemsTree]]] = {
        import com.openbankproject.commons.dto.{InBoundGetProductCollectionItemsTree => InBound, OutBoundGetProductCollectionItemsTree => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, collectionCode, bankId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[ProductCollectionItemsTree]](callContext))        
  }
          
  messageDocs += createMeetingDoc
  def createMeetingDoc = MessageDoc(
    process = "obp.createMeeting",
    messageFormat = messageFormat,
    description = "Create Meeting",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateMeeting").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateMeeting").request),
    exampleOutboundMessage = (
     OutBoundCreateMeeting(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      staffUser= UserCommons(userPrimaryKey=UserPrimaryKey(123),
      userId=userIdExample.value,
      idGivenByProvider="string",
      provider=providerExample.value,
      emailAddress=emailAddressExample.value,
      name=userNameExample.value,
      createdByConsentId=Some("string"),
      createdByUserInvitationId=Some("string"),
      isDeleted=Some(true),
      lastMarketingAgreementSignedDate=Some(toDate(dateExample))),
      customerUser= UserCommons(userPrimaryKey=UserPrimaryKey(123),
      userId=userIdExample.value,
      idGivenByProvider="string",
      provider=providerExample.value,
      emailAddress=emailAddressExample.value,
      name=userNameExample.value,
      createdByConsentId=Some("string"),
      createdByUserInvitationId=Some("string"),
      isDeleted=Some(true),
      lastMarketingAgreementSignedDate=Some(toDate(dateExample))),
      providerId=providerIdExample.value,
      purposeId=purposeIdExample.value,
      when=toDate(whenExample),
      sessionId=sessionIdExample.value,
      customerToken=customerTokenExample.value,
      staffToken=staffTokenExample.value,
      creator= ContactDetails(name=nameExample.value,
      phone=phoneExample.value,
      email=emailExample.value),
      invitees=List( Invitee(contactDetails= ContactDetails(name=nameExample.value,
      phone=phoneExample.value,
      email=emailExample.value),
      status=statusExample.value)))
    ),
    exampleInboundMessage = (
     InBoundCreateMeeting(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= MeetingCommons(meetingId=meetingIdExample.value,
      providerId=providerIdExample.value,
      purposeId=purposeIdExample.value,
      bankId=bankIdExample.value,
      present= MeetingPresent(staffUserId=staffUserIdExample.value,
      customerUserId=customerUserIdExample.value),
      keys= MeetingKeys(sessionId=sessionIdExample.value,
      customerToken=customerTokenExample.value,
      staffToken=staffTokenExample.value),
      when=toDate(whenExample),
      creator= ContactDetails(name=nameExample.value,
      phone=phoneExample.value,
      email=emailExample.value),
      invitees=List( Invitee(contactDetails= ContactDetails(name=nameExample.value,
      phone=phoneExample.value,
      email=emailExample.value),
      status=statusExample.value))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createMeeting(bankId: BankId, staffUser: User, customerUser: User, providerId: String, purposeId: String, when: Date, sessionId: String, customerToken: String, staffToken: String, creator: ContactDetails, invitees: List[Invitee], callContext: Option[CallContext]): OBPReturnType[Box[Meeting]] = {
        import com.openbankproject.commons.dto.{InBoundCreateMeeting => InBound, OutBoundCreateMeeting => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, staffUser, customerUser, providerId, purposeId, when, sessionId, customerToken, staffToken, creator, invitees)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[MeetingCommons](callContext))        
  }
          
  messageDocs += getMeetingsDoc
  def getMeetingsDoc = MessageDoc(
    process = "obp.getMeetings",
    messageFormat = messageFormat,
    description = "Get Meetings",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetMeetings").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetMeetings").request),
    exampleOutboundMessage = (
     OutBoundGetMeetings(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      user= UserCommons(userPrimaryKey=UserPrimaryKey(123),
      userId=userIdExample.value,
      idGivenByProvider="string",
      provider=providerExample.value,
      emailAddress=emailAddressExample.value,
      name=userNameExample.value,
      createdByConsentId=Some("string"),
      createdByUserInvitationId=Some("string"),
      isDeleted=Some(true),
      lastMarketingAgreementSignedDate=Some(toDate(dateExample))))
    ),
    exampleInboundMessage = (
     InBoundGetMeetings(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( MeetingCommons(meetingId=meetingIdExample.value,
      providerId=providerIdExample.value,
      purposeId=purposeIdExample.value,
      bankId=bankIdExample.value,
      present= MeetingPresent(staffUserId=staffUserIdExample.value,
      customerUserId=customerUserIdExample.value),
      keys= MeetingKeys(sessionId=sessionIdExample.value,
      customerToken=customerTokenExample.value,
      staffToken=staffTokenExample.value),
      when=toDate(whenExample),
      creator= ContactDetails(name=nameExample.value,
      phone=phoneExample.value,
      email=emailExample.value),
      invitees=List( Invitee(contactDetails= ContactDetails(name=nameExample.value,
      phone=phoneExample.value,
      email=emailExample.value),
      status=statusExample.value)))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getMeetings(bankId: BankId, user: User, callContext: Option[CallContext]): OBPReturnType[Box[List[Meeting]]] = {
        import com.openbankproject.commons.dto.{InBoundGetMeetings => InBound, OutBoundGetMeetings => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, user)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[MeetingCommons]](callContext))        
  }
          
  messageDocs += getMeetingDoc
  def getMeetingDoc = MessageDoc(
    process = "obp.getMeeting",
    messageFormat = messageFormat,
    description = "Get Meeting",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetMeeting").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetMeeting").request),
    exampleOutboundMessage = (
     OutBoundGetMeeting(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=BankId(bankIdExample.value),
      user= UserCommons(userPrimaryKey=UserPrimaryKey(123),
      userId=userIdExample.value,
      idGivenByProvider="string",
      provider=providerExample.value,
      emailAddress=emailAddressExample.value,
      name=userNameExample.value,
      createdByConsentId=Some("string"),
      createdByUserInvitationId=Some("string"),
      isDeleted=Some(true),
      lastMarketingAgreementSignedDate=Some(toDate(dateExample))),
      meetingId=meetingIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetMeeting(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= MeetingCommons(meetingId=meetingIdExample.value,
      providerId=providerIdExample.value,
      purposeId=purposeIdExample.value,
      bankId=bankIdExample.value,
      present= MeetingPresent(staffUserId=staffUserIdExample.value,
      customerUserId=customerUserIdExample.value),
      keys= MeetingKeys(sessionId=sessionIdExample.value,
      customerToken=customerTokenExample.value,
      staffToken=staffTokenExample.value),
      when=toDate(whenExample),
      creator= ContactDetails(name=nameExample.value,
      phone=phoneExample.value,
      email=emailExample.value),
      invitees=List( Invitee(contactDetails= ContactDetails(name=nameExample.value,
      phone=phoneExample.value,
      email=emailExample.value),
      status=statusExample.value))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getMeeting(bankId: BankId, user: User, meetingId: String, callContext: Option[CallContext]): OBPReturnType[Box[Meeting]] = {
        import com.openbankproject.commons.dto.{InBoundGetMeeting => InBound, OutBoundGetMeeting => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, user, meetingId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[MeetingCommons](callContext))        
  }
          
  messageDocs += createOrUpdateKycCheckDoc
  def createOrUpdateKycCheckDoc = MessageDoc(
    process = "obp.createOrUpdateKycCheck",
    messageFormat = messageFormat,
    description = "Create Or Update Kyc Check",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateOrUpdateKycCheck").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateOrUpdateKycCheck").request),
    exampleOutboundMessage = (
     OutBoundCreateOrUpdateKycCheck(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=bankIdExample.value,
      customerId=customerIdExample.value,
      id=idExample.value,
      customerNumber=customerNumberExample.value,
      date=toDate(dateExample),
      how=howExample.value,
      staffUserId=staffUserIdExample.value,
      mStaffName="string",
      mSatisfied=true,
      comments=commentsExample.value)
    ),
    exampleInboundMessage = (
     InBoundCreateOrUpdateKycCheck(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= KycCheckCommons(bankId=bankIdExample.value,
      customerId=customerIdExample.value,
      idKycCheck="string",
      customerNumber=customerNumberExample.value,
      date=toDate(dateExample),
      how=howExample.value,
      staffUserId=staffUserIdExample.value,
      staffName=staffNameExample.value,
      satisfied=satisfiedExample.value.toBoolean,
      comments=commentsExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createOrUpdateKycCheck(bankId: String, customerId: String, id: String, customerNumber: String, date: Date, how: String, staffUserId: String, mStaffName: String, mSatisfied: Boolean, comments: String, callContext: Option[CallContext]): OBPReturnType[Box[KycCheck]] = {
        import com.openbankproject.commons.dto.{InBoundCreateOrUpdateKycCheck => InBound, OutBoundCreateOrUpdateKycCheck => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, customerId, id, customerNumber, date, how, staffUserId, mStaffName, mSatisfied, comments)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[KycCheckCommons](callContext))        
  }
          
  messageDocs += createOrUpdateKycDocumentDoc
  def createOrUpdateKycDocumentDoc = MessageDoc(
    process = "obp.createOrUpdateKycDocument",
    messageFormat = messageFormat,
    description = "Create Or Update Kyc Document",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateOrUpdateKycDocument").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateOrUpdateKycDocument").request),
    exampleOutboundMessage = (
     OutBoundCreateOrUpdateKycDocument(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=bankIdExample.value,
      customerId=customerIdExample.value,
      id=idExample.value,
      customerNumber=customerNumberExample.value,
      `type`=typeExample.value,
      number=numberExample.value,
      issueDate=toDate(issueDateExample),
      issuePlace=issuePlaceExample.value,
      expiryDate=toDate(expiryDateExample))
    ),
    exampleInboundMessage = (
     InBoundCreateOrUpdateKycDocument(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= KycDocumentCommons(bankId=bankIdExample.value,
      customerId=customerIdExample.value,
      idKycDocument="string",
      customerNumber=customerNumberExample.value,
      `type`=typeExample.value,
      number=numberExample.value,
      issueDate=toDate(issueDateExample),
      issuePlace=issuePlaceExample.value,
      expiryDate=toDate(expiryDateExample)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createOrUpdateKycDocument(bankId: String, customerId: String, id: String, customerNumber: String, `type`: String, number: String, issueDate: Date, issuePlace: String, expiryDate: Date, callContext: Option[CallContext]): OBPReturnType[Box[KycDocument]] = {
        import com.openbankproject.commons.dto.{InBoundCreateOrUpdateKycDocument => InBound, OutBoundCreateOrUpdateKycDocument => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, customerId, id, customerNumber, `type`, number, issueDate, issuePlace, expiryDate)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[KycDocument](callContext))        
  }
          
  messageDocs += createOrUpdateKycMediaDoc
  def createOrUpdateKycMediaDoc = MessageDoc(
    process = "obp.createOrUpdateKycMedia",
    messageFormat = messageFormat,
    description = "Create Or Update Kyc Media",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateOrUpdateKycMedia").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateOrUpdateKycMedia").request),
    exampleOutboundMessage = (
     OutBoundCreateOrUpdateKycMedia(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=bankIdExample.value,
      customerId=customerIdExample.value,
      id=idExample.value,
      customerNumber=customerNumberExample.value,
      `type`=typeExample.value,
      url=urlExample.value,
      date=toDate(dateExample),
      relatesToKycDocumentId=relatesToKycDocumentIdExample.value,
      relatesToKycCheckId=relatesToKycCheckIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundCreateOrUpdateKycMedia(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= KycMediaCommons(bankId=bankIdExample.value,
      customerId=customerIdExample.value,
      idKycMedia="string",
      customerNumber=customerNumberExample.value,
      `type`=typeExample.value,
      url=urlExample.value,
      date=toDate(dateExample),
      relatesToKycDocumentId=relatesToKycDocumentIdExample.value,
      relatesToKycCheckId=relatesToKycCheckIdExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createOrUpdateKycMedia(bankId: String, customerId: String, id: String, customerNumber: String, `type`: String, url: String, date: Date, relatesToKycDocumentId: String, relatesToKycCheckId: String, callContext: Option[CallContext]): OBPReturnType[Box[KycMedia]] = {
        import com.openbankproject.commons.dto.{InBoundCreateOrUpdateKycMedia => InBound, OutBoundCreateOrUpdateKycMedia => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, customerId, id, customerNumber, `type`, url, date, relatesToKycDocumentId, relatesToKycCheckId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[KycMediaCommons](callContext))        
  }
          
  messageDocs += createOrUpdateKycStatusDoc
  def createOrUpdateKycStatusDoc = MessageDoc(
    process = "obp.createOrUpdateKycStatus",
    messageFormat = messageFormat,
    description = "Create Or Update Kyc Status",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateOrUpdateKycStatus").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateOrUpdateKycStatus").request),
    exampleOutboundMessage = (
     OutBoundCreateOrUpdateKycStatus(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=bankIdExample.value,
      customerId=customerIdExample.value,
      customerNumber=customerNumberExample.value,
      ok=okExample.value.toBoolean,
      date=toDate(dateExample))
    ),
    exampleInboundMessage = (
     InBoundCreateOrUpdateKycStatus(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= KycStatusCommons(bankId=bankIdExample.value,
      customerId=customerIdExample.value,
      customerNumber=customerNumberExample.value,
      ok=okExample.value.toBoolean,
      date=toDate(dateExample)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createOrUpdateKycStatus(bankId: String, customerId: String, customerNumber: String, ok: Boolean, date: Date, callContext: Option[CallContext]): OBPReturnType[Box[KycStatus]] = {
        import com.openbankproject.commons.dto.{InBoundCreateOrUpdateKycStatus => InBound, OutBoundCreateOrUpdateKycStatus => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, customerId, customerNumber, ok, date)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[KycStatusCommons](callContext))        
  }
          
  messageDocs += getKycChecksDoc
  def getKycChecksDoc = MessageDoc(
    process = "obp.getKycChecks",
    messageFormat = messageFormat,
    description = "Get Kyc Checks",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetKycChecks").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetKycChecks").request),
    exampleOutboundMessage = (
     OutBoundGetKycChecks(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      customerId=customerIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetKycChecks(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( KycCheckCommons(bankId=bankIdExample.value,
      customerId=customerIdExample.value,
      idKycCheck="string",
      customerNumber=customerNumberExample.value,
      date=toDate(dateExample),
      how=howExample.value,
      staffUserId=staffUserIdExample.value,
      staffName=staffNameExample.value,
      satisfied=satisfiedExample.value.toBoolean,
      comments=commentsExample.value)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getKycChecks(customerId: String, callContext: Option[CallContext]): OBPReturnType[Box[List[KycCheck]]] = {
        import com.openbankproject.commons.dto.{InBoundGetKycChecks => InBound, OutBoundGetKycChecks => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, customerId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[KycCheckCommons]](callContext))        
  }
          
  messageDocs += getKycDocumentsDoc
  def getKycDocumentsDoc = MessageDoc(
    process = "obp.getKycDocuments",
    messageFormat = messageFormat,
    description = "Get Kyc Documents",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetKycDocuments").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetKycDocuments").request),
    exampleOutboundMessage = (
     OutBoundGetKycDocuments(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      customerId=customerIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetKycDocuments(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( KycDocumentCommons(bankId=bankIdExample.value,
      customerId=customerIdExample.value,
      idKycDocument="string",
      customerNumber=customerNumberExample.value,
      `type`=typeExample.value,
      number=numberExample.value,
      issueDate=toDate(issueDateExample),
      issuePlace=issuePlaceExample.value,
      expiryDate=toDate(expiryDateExample))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getKycDocuments(customerId: String, callContext: Option[CallContext]): OBPReturnType[Box[List[KycDocument]]] = {
        import com.openbankproject.commons.dto.{InBoundGetKycDocuments => InBound, OutBoundGetKycDocuments => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, customerId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[KycDocumentCommons]](callContext))        
  }
          
  messageDocs += getKycMediasDoc
  def getKycMediasDoc = MessageDoc(
    process = "obp.getKycMedias",
    messageFormat = messageFormat,
    description = "Get Kyc Medias",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetKycMedias").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetKycMedias").request),
    exampleOutboundMessage = (
     OutBoundGetKycMedias(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      customerId=customerIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetKycMedias(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( KycMediaCommons(bankId=bankIdExample.value,
      customerId=customerIdExample.value,
      idKycMedia="string",
      customerNumber=customerNumberExample.value,
      `type`=typeExample.value,
      url=urlExample.value,
      date=toDate(dateExample),
      relatesToKycDocumentId=relatesToKycDocumentIdExample.value,
      relatesToKycCheckId=relatesToKycCheckIdExample.value)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getKycMedias(customerId: String, callContext: Option[CallContext]): OBPReturnType[Box[List[KycMedia]]] = {
        import com.openbankproject.commons.dto.{InBoundGetKycMedias => InBound, OutBoundGetKycMedias => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, customerId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[KycMediaCommons]](callContext))        
  }
          
  messageDocs += getKycStatusesDoc
  def getKycStatusesDoc = MessageDoc(
    process = "obp.getKycStatuses",
    messageFormat = messageFormat,
    description = "Get Kyc Statuses",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundGetKycStatuses").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundGetKycStatuses").request),
    exampleOutboundMessage = (
     OutBoundGetKycStatuses(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      customerId=customerIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundGetKycStatuses(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=List( KycStatusCommons(bankId=bankIdExample.value,
      customerId=customerIdExample.value,
      customerNumber=customerNumberExample.value,
      ok=okExample.value.toBoolean,
      date=toDate(dateExample))))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def getKycStatuses(customerId: String, callContext: Option[CallContext]): OBPReturnType[Box[List[KycStatus]]] = {
        import com.openbankproject.commons.dto.{InBoundGetKycStatuses => InBound, OutBoundGetKycStatuses => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, customerId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[List[KycStatusCommons]](callContext))        
  }
          
  messageDocs += createMessageDoc
  def createMessageDoc = MessageDoc(
    process = "obp.createMessage",
    messageFormat = messageFormat,
    description = "Create Message",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateMessage").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateMessage").request),
    exampleOutboundMessage = (
     OutBoundCreateMessage(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      user= UserCommons(userPrimaryKey=UserPrimaryKey(123),
      userId=userIdExample.value,
      idGivenByProvider="string",
      provider=providerExample.value,
      emailAddress=emailAddressExample.value,
      name=userNameExample.value,
      createdByConsentId=Some("string"),
      createdByUserInvitationId=Some("string"),
      isDeleted=Some(true),
      lastMarketingAgreementSignedDate=Some(toDate(dateExample))),
      bankId=BankId(bankIdExample.value),
      message=messageExample.value,
      fromDepartment=fromDepartmentExample.value,
      fromPerson=fromPersonExample.value)
    ),
    exampleInboundMessage = (
     InBoundCreateMessage(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= CustomerMessageCommons(messageId="string",
      date=toDate(dateExample),
      message=messageExample.value,
      fromDepartment=fromDepartmentExample.value,
      fromPerson=fromPersonExample.value,
      transport=Some(transportExample.value)))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createMessage(user: User, bankId: BankId, message: String, fromDepartment: String, fromPerson: String, callContext: Option[CallContext]): OBPReturnType[Box[CustomerMessage]] = {
        import com.openbankproject.commons.dto.{InBoundCreateMessage => InBound, OutBoundCreateMessage => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, user, bankId, message, fromDepartment, fromPerson)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[CustomerMessageCommons](callContext))        
  }
          
  messageDocs += makeHistoricalPaymentDoc
  def makeHistoricalPaymentDoc = MessageDoc(
    process = "obp.makeHistoricalPayment",
    messageFormat = messageFormat,
    description = "Make Historical Payment",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundMakeHistoricalPayment").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundMakeHistoricalPayment").request),
    exampleOutboundMessage = (
     OutBoundMakeHistoricalPayment(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      fromAccount= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))),
      toAccount= BankAccountCommons(accountId=AccountId(accountIdExample.value),
      accountType=accountTypeExample.value,
      balance=BigDecimal(balanceExample.value),
      currency=currencyExample.value,
      name=bankAccountNameExample.value,
      label=labelExample.value,
      number=bankAccountNumberExample.value,
      bankId=BankId(bankIdExample.value),
      lastUpdate=toDate(bankAccountLastUpdateExample),
      branchId=branchIdExample.value,
      accountRoutings=List( AccountRouting(scheme=accountRoutingSchemeExample.value,
      address=accountRoutingAddressExample.value)),
      accountRules=List( AccountRule(scheme=accountRuleSchemeExample.value,
      value=accountRuleValueExample.value)),
      accountHolder=bankAccountAccountHolderExample.value,
      attributes=Some(List( Attribute(name=attributeNameExample.value,
      `type`=attributeTypeExample.value,
      value=attributeValueExample.value)))),
      posted=toDate(postedExample),
      completed=toDate(completedExample),
      amount=BigDecimal(amountExample.value),
      currency=currencyExample.value,
      description=descriptionExample.value,
      transactionRequestType=transactionRequestTypeExample.value,
      chargePolicy=chargePolicyExample.value)
    ),
    exampleInboundMessage = (
     InBoundMakeHistoricalPayment(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=TransactionId(transactionIdExample.value))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def makeHistoricalPayment(fromAccount: BankAccount, toAccount: BankAccount, posted: Date, completed: Date, amount: BigDecimal, currency: String, description: String, transactionRequestType: String, chargePolicy: String, callContext: Option[CallContext]): OBPReturnType[Box[TransactionId]] = {
        import com.openbankproject.commons.dto.{InBoundMakeHistoricalPayment => InBound, OutBoundMakeHistoricalPayment => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, fromAccount, toAccount, posted, completed, amount, currency, description, transactionRequestType, chargePolicy)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[TransactionId](callContext))        
  }
          
  messageDocs += createDirectDebitDoc
  def createDirectDebitDoc = MessageDoc(
    process = "obp.createDirectDebit",
    messageFormat = messageFormat,
    description = "Create Direct Debit",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateDirectDebit").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundCreateDirectDebit").request),
    exampleOutboundMessage = (
     OutBoundCreateDirectDebit(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      bankId=bankIdExample.value,
      accountId=accountIdExample.value,
      customerId=customerIdExample.value,
      userId=userIdExample.value,
      counterpartyId=counterpartyIdExample.value,
      dateSigned=toDate(dateSignedExample),
      dateStarts=toDate(dateStartsExample),
      dateExpires=Some(toDate(dateExpiresExample)))
    ),
    exampleInboundMessage = (
     InBoundCreateDirectDebit(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data= DirectDebitTraitCommons(directDebitId=directDebitIdExample.value,
      bankId=bankIdExample.value,
      accountId=accountIdExample.value,
      customerId=customerIdExample.value,
      userId=userIdExample.value,
      counterpartyId=counterpartyIdExample.value,
      dateSigned=toDate(dateSignedExample),
      dateCancelled=toDate(dateCancelledExample),
      dateStarts=toDate(dateStartsExample),
      dateExpires=toDate(dateExpiresExample),
      active=activeExample.value.toBoolean))
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def createDirectDebit(bankId: String, accountId: String, customerId: String, userId: String, counterpartyId: String, dateSigned: Date, dateStarts: Date, dateExpires: Option[Date], callContext: Option[CallContext]): OBPReturnType[Box[DirectDebitTrait]] = {
        import com.openbankproject.commons.dto.{InBoundCreateDirectDebit => InBound, OutBoundCreateDirectDebit => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, bankId, accountId, customerId, userId, counterpartyId, dateSigned, dateStarts, dateExpires)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[DirectDebitTraitCommons](callContext))        
  }
          
  messageDocs += deleteCustomerAttributeDoc
  def deleteCustomerAttributeDoc = MessageDoc(
    process = "obp.deleteCustomerAttribute",
    messageFormat = messageFormat,
    description = "Delete Customer Attribute",
    outboundTopic = Some(Topics.createTopicByClassName("OutBoundDeleteCustomerAttribute").request),
    inboundTopic = Some(Topics.createTopicByClassName("OutBoundDeleteCustomerAttribute").request),
    exampleOutboundMessage = (
     OutBoundDeleteCustomerAttribute(outboundAdapterCallContext=MessageDocsSwaggerDefinitions.outboundAdapterCallContext,
      customerAttributeId=customerAttributeIdExample.value)
    ),
    exampleInboundMessage = (
     InBoundDeleteCustomerAttribute(inboundAdapterCallContext=MessageDocsSwaggerDefinitions.inboundAdapterCallContext,
      status=MessageDocsSwaggerDefinitions.inboundStatus,
      data=true)
    ),
    adapterImplementation = Some(AdapterImplementation("- Core", 1))
  )

  override def deleteCustomerAttribute(customerAttributeId: String, callContext: Option[CallContext]): OBPReturnType[Box[Boolean]] = {
        import com.openbankproject.commons.dto.{InBoundDeleteCustomerAttribute => InBound, OutBoundDeleteCustomerAttribute => OutBound}  
        val req = OutBound(callContext.map(_.toOutboundAdapterCallContext).orNull, customerAttributeId)
        val response: Future[Box[InBound]] = processRequest[InBound](req)
        response.map(convertToTuple[Boolean](callContext))        
  }
          
// ---------- created on 2024-10-30T11:52:35Z
//---------------- dynamic end ---------------------please don't modify this line 
}
object KafkaMappedConnector_vMay2019 extends KafkaMappedConnector_vMay2019{

}





