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
import code.api.util.CustomJsonFormats
import code.api.v5_0_0.{APIMethods500, ConsentRequestResponseJson}
import code.api.v3_1_0.{APIMethods310, ConsentChallengeJsonV310}
import code.consent.ConsentStatus
import code.util.Helper.{MdcLoggable, ObpS}
import net.liftweb.common.Full
import net.liftweb.http.rest.RestHelper
import net.liftweb.http.{GetRequest, PostRequest, RequestVar, S, SHtml}
import net.liftweb.json
import net.liftweb.json.Formats
import net.liftweb.util.Helpers._

class VrpConsentCreation extends MdcLoggable with RestHelper with APIMethods500 with APIMethods310 {
  protected implicit override def formats: Formats = CustomJsonFormats.formats

  private object otpValue extends RequestVar("123456")
  
  def confirmVrpConsentRequest = {
    getConsentRequest match {
      case Left(error) => {
        "#confirm-vrp-consent-request-form-title *" #> s"Please enter your consent request info:" &
          "#confirm-vrp-consent-request-response-json *" #> s"""$error""" &
          "type=submit" #> ""
      }
      case Right(response) => {
        tryo {json.parse(response).extract[ConsentRequestResponseJson]} match {
          case Full(consentRequestResponseJson) =>
            "#confirm-vrp-consent-request-form-title *" #> s"Please enter your consent request info:" &
              "#confirm-vrp-consent-request-response-json *" #> s"""${json.prettyRender(json.Extraction.decompose(consentRequestResponseJson.payload))}""" &
              "#confirm-vrp-consent-request-confirm-submit-button" #> SHtml.onSubmitUnit(confirmConsentRequestProcess)&
              "#confirm-vrp-consent-request-deny-submit-button" #> SHtml.onSubmitUnit(denyConsentRequestProcess)
          case _ =>
            "#confirm-vrp-consent-request-form-title *" #> s"Please enter your consent request info:" & 
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
    
    //2nd: we need to redirect to confirm page to fill the OTP
    
    S.redirectTo(
      s"/confirm-vrp-consent" 
    )
  }
  private def denyConsentRequestProcess() ={
    S.redirectTo(
      s"/" // if click deny, we just redirect to Home page.
    )
  }

  private def callAnswerConsentChallenge: Either[(String, Int), String] = {

    val requestParam = List(
      ObpS.param("BANK_ID"),
      ObpS.param("CONSENT_ID")
    )

    if(requestParam.count(_.isDefined) < requestParam.size) {
      return Left(("There are one or many mandatory request parameter not present, please check request parameter: BANK_ID, CONSENT_ID", 500))
    }

    val pathOfEndpoint = List(
      "banks",
      ObpS.param("BANK_ID")openOr(""),
      "consents",
      ObpS.param("CONSENT_ID")openOr(""),
      "challenge"
    )

    val requestBody = s"""{"answer":"${otpValue.get}"}"""
    val authorisationsResult = callEndpoint(Implementations3_1_0.answerConsentChallenge, pathOfEndpoint, PostRequest, requestBody)

    authorisationsResult

  }
  
  private def confirmVrpConsentProcess() ={
    callAnswerConsentChallenge match {
      case Left(error) => S.error("otp-value-error",error._1)
      case Right(response) => {
        tryo {json.parse(response).extract[ConsentChallengeJsonV310]} match {
          case Full(consentChallengeJsonV310) if (consentChallengeJsonV310.status.equals(ConsentStatus.ACCEPTED.toString)) =>
            S.redirectTo("/")
          case Full(consentChallengeJsonV310) =>
            S.error("otp-value-error",s"Current SCA status is ${consentChallengeJsonV310.status}. Please double check OTP value.")
          case _ => S.error("otp-value-error",s"$InvalidJsonFormat The Json body should be the $ConsentChallengeJsonV310. " +
            s"Please check `Create User Auth Context Update Request` endpoint separately! ")
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
