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

import code.api.util.APIUtil._
import code.api.util.ErrorMessages.InvalidJsonFormat
import code.api.util.{APIUtil, CustomJsonFormats}
import code.api.v5_1_0.{APIMethods510, ConsentJsonV510}
import code.api.v5_0_0.{APIMethods500, ConsentJsonV500, ConsentRequestResponseJson}
import code.api.v3_1_0.{APIMethods310, ConsentChallengeJsonV310, ConsumerJsonV310}
import code.consent.{ConsentStatus}
import code.util.Helper.{MdcLoggable, ObpS}
import net.liftweb.common.Full
import net.liftweb.http.rest.RestHelper
import net.liftweb.http.{GetRequest, PostRequest, RequestVar, S, SHtml}
import net.liftweb.json
import net.liftweb.json.Formats
import net.liftweb.util.Helpers._

class VrpConsentCreation extends MdcLoggable with RestHelper with APIMethods510 with APIMethods500 with APIMethods310 {
  protected implicit override def formats: Formats = CustomJsonFormats.formats

  private object otpValue extends RequestVar("123456")
  
  def confirmVrpConsentRequest = {
    getConsentRequest match {
      case Left(error) => {
        S.error(error._1)
        "#confirm-vrp-consent-request-form-title *" #> s"Please confirm or deny the following consent request:" &
          "#confirm-vrp-consent-request-response-json *" #> s"""""" &
          "type=submit" #> ""
      }
      case Right(response) => {
        tryo {json.parse(response).extract[ConsentRequestResponseJson]} match {
          case Full(consentRequestResponseJson) =>
            "#confirm-vrp-consent-request-form-title *" #> s"Please confirm or deny the following consent request:" &
              "#confirm-vrp-consent-request-response-json *" #> s"""${json.prettyRender(json.Extraction.decompose(consentRequestResponseJson.payload))}""" &
              "#confirm-vrp-consent-request-confirm-submit-button" #> SHtml.onSubmitUnit(confirmConsentRequestProcess)
          case _ =>
            "#confirm-vrp-consent-request-form-title *" #> s"Please confirm or deny the following consent request:" & 
              "#confirm-vrp-consent-request-response-json *" #>
                s"""$InvalidJsonFormat The Json body should be the $ConsentRequestResponseJson. 
                   |Please check `Get Consent Request` endpoint separately! """.stripMargin &
              "type=submit" #> ""
        }
      }
    }
    
  }
  
  def confirmVrpConsent = {
    "#otp-value" #> SHtml.textElem(otpValue) &
      "type=submit" #> SHtml.onSubmitUnit(confirmVrpConsentProcess)
  }
  
  private def confirmConsentRequestProcess() ={
    //1st: we need to call `Create Consent By CONSENT_REQUEST_ID (IMPLICIT)`, this will send OTP to account owner.
    callCreateConsentByConsentRequestIdImplicit match {
      case Left(error) => {
        S.error(error._1)
      }
      case Right(response) => {
        tryo {json.parse(response).extract[ConsentJsonV500]} match {
          case Full(consentJsonV500) =>
            //2nd: we need to redirect to confirm page to fill the OTP
            S.redirectTo(
              s"/confirm-vrp-consent?CONSENT_ID=${consentJsonV500.consent_id}"
            )
          case _ =>
            S.error(s"$InvalidJsonFormat The Json body should be the $ConsentJsonV500. " +
              s"Please check `Create Consent By CONSENT_REQUEST_ID (IMPLICIT) !")
        }
      }
    }
  }

  private def callAnswerConsentChallenge: Either[(String, Int), String] = {

    val requestParam = List(
      ObpS.param("CONSENT_ID")
    )

    if(requestParam.count(_.isDefined) < requestParam.size) {
      return Left(("There are one or many mandatory request parameter not present, please check request parameter: CONSENT_ID", 500))
    }

    val pathOfEndpoint = List(
      "banks",
      APIUtil.defaultBankId,//we do not need to get this from URL, it will be easier for the developer.
      "consents",
      ObpS.param("CONSENT_ID")openOr(""),
      "challenge"
    )

    val requestBody = s"""{"answer":"${otpValue.get}"}"""
    val authorisationsResult = callEndpoint(Implementations3_1_0.answerConsentChallenge, pathOfEndpoint, PostRequest, requestBody)

    authorisationsResult

  }
  
  private def callGetConsentByConsentId(consentId: String): Either[(String, Int), String] = {
    
    val pathOfEndpoint = List(
      "user",
      "current",
      "consents",
      consentId,
    )

    val authorisationsResult = callEndpoint(Implementations5_1_0.getConsentByConsentId, pathOfEndpoint, GetRequest)

    authorisationsResult
  }
  
  private def callGetConsumer(consumerId: String): Either[(String, Int), String] = {
    
    val pathOfEndpoint = List(
      "management",
      "consumers",
      consumerId,
    )

    val authorisationsResult = callEndpoint(Implementations5_1_0.getConsumer, pathOfEndpoint, GetRequest)

    authorisationsResult
  }
  
  private def callCreateConsentByConsentRequestIdImplicit: Either[(String, Int), String] = {

    val requestParam = List(
      ObpS.param("CONSENT_REQUEST_ID"),
    )
    if(requestParam.count(_.isDefined) < requestParam.size) {
      return Left(("Parameter CONSENT_REQUEST_ID is missing, please set it in the URL", 500))
    }
    
    val pathOfEndpoint = List(
      "consumer",
      "consent-requests",
      ObpS.param("CONSENT_REQUEST_ID")openOr(""),
      "IMPLICIT",
      "consents",
    )

    val requestBody = s"""{}"""
    val authorisationsResult = callEndpoint(Implementations5_0_0.createConsentByConsentRequestIdImplicit, pathOfEndpoint, PostRequest, requestBody)

    authorisationsResult
  }
  
  private def confirmVrpConsentProcess() ={
    //1st: we need to answer challenge and create the consent properly.
    callAnswerConsentChallenge match {
      case Left(error) => S.error(error._1)
      case Right(response) => {
        tryo {json.parse(response).extract[ConsentChallengeJsonV310]} match {
          case Full(consentChallengeJsonV310) if (consentChallengeJsonV310.status.equals(ConsentStatus.ACCEPTED.toString)) =>
            //2nd: we need to call getConsent by consentId --> get the consumerId
            callGetConsentByConsentId(consentChallengeJsonV310.consent_id)  match {
              case Left(error) => S.error(error._1)
              case Right(response) => {
                tryo {json.parse(response).extract[ConsentJsonV510]} match {
                  case Full(consentJsonV510) =>
                    //3rd: get consumer by consumerId
                    callGetConsumer(consentJsonV510.consumer_id)  match {
                      case Left(error) => S.error(error._1)
                      case Right(response) => {
                        tryo {json.parse(response).extract[ConsumerJsonV310]} match {
                          case Full(consumerJsonV310) =>
                            //4th: get the redirect url.
                            val redirectURL =  consumerJsonV310.redirect_url 
                            S.redirectTo(redirectURL)
                          case _ =>
                            S.error(s"$InvalidJsonFormat The Json body should be the $ConsumerJsonV310. " +
                              s"Please check `Get Consumer` !")
                        }
                      }
                    }
                    
                  case _ =>
                    S.error(s"$InvalidJsonFormat The Json body should be the $ConsentJsonV510. " +
                      s"Please check `Get Consent By Consent Id` !")
                }
              }
            }
          case Full(consentChallengeJsonV310) =>
            S.error(s"Current SCA status is ${consentChallengeJsonV310.status}. Please double check OTP value.")
          case _ => S.error(s"$InvalidJsonFormat The Json body should be the $ConsentChallengeJsonV310. " +
            s"Please check `Answer Consent Challenge` ! ")
        }
      }
    }
  }

  private def getConsentRequest: Either[(String, Int), String] = {
    
    val requestParam = List(
      ObpS.param("CONSENT_REQUEST_ID"),
    )

    if(requestParam.count(_.isDefined) < requestParam.size) {
      return Left(("Parameter CONSENT_REQUEST_ID is missing, please set it in the URL", 500))
    }
    
    val pathOfEndpoint = List(
      "consumer",
      "consent-requests",
      ObpS.param("CONSENT_REQUEST_ID")openOr("")
    )

    val authorisationsResult = callEndpoint(Implementations5_0_0.getConsentRequest, pathOfEndpoint, GetRequest)

    authorisationsResult
  }
  
}
