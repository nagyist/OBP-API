package code.api.v5_1_0


import code.api.Constant
import code.api.ResourceDocs1_4_0.SwaggerDefinitionsJSON._
import code.api.berlin.group.v1_3.JSONFactory_BERLIN_GROUP_1_3.{ConsentAccessAccountsJson, ConsentAccessJson}
import code.api.util.APIUtil._
import code.api.util.ApiRole._
import code.api.util.ApiTag._
import code.api.util.ErrorMessages.{$UserNotLoggedIn, BankNotFound, ConsentNotFound, InvalidJsonFormat, UnknownError, UserNotFoundByUserId, UserNotLoggedIn, _}
import code.api.util.FutureUtil.{EndpointContext, EndpointTimeout}
import code.api.util.JwtUtil.{getSignedPayloadAsJson, verifyJwt}
import code.api.util.NewStyle.HttpCode
import code.api.util.NewStyle.function.extractQueryParams
import code.api.util.X509.{getCommonName, getEmailAddress, getOrganization}
import code.api.util._
import code.api.util.newstyle.BalanceNewStyle
import code.api.util.newstyle.Consumer.createConsumerNewStyle
import code.api.util.newstyle.RegulatedEntityNewStyle.{createRegulatedEntityNewStyle, deleteRegulatedEntityNewStyle, getRegulatedEntitiesNewStyle, getRegulatedEntityByEntityIdNewStyle}
import code.api.v2_0_0.AccountsHelper.{accountTypeFilterText, getFilteredCoreAccounts}
import code.api.v2_1_0.ConsumerRedirectUrlJSON
import code.api.v3_0_0.JSONFactory300
import code.api.v3_0_0.JSONFactory300.createAggregateMetricJson
import code.api.v3_1_0.{ConsentChallengeJsonV310, ConsentJsonV310}
import code.api.v3_1_0.JSONFactory310.{createBadLoginStatusJson, createRefreshUserJson}
import code.api.v4_0_0.JSONFactory400.{createAccountBalancesJson, createBalancesJson, createNewCoreBankAccountJson}
import code.api.v4_0_0.{JSONFactory400, PostAccountAccessJsonV400, PostApiCollectionJson400, PutConsentStatusJsonV400, RevokedJsonV400}
import code.api.v5_0_0.JSONFactory500
import code.api.v5_1_0.JSONFactory510.{createConsentsInfoJsonV510, createConsentsJsonV510, createRegulatedEntitiesJson, createRegulatedEntityJson}
import code.atmattribute.AtmAttribute
import code.bankconnectors.Connector
import code.consent.{ConsentRequests, ConsentStatus, Consents, MappedConsent}
import code.consumer.Consumers
import code.loginattempts.LoginAttempt
import code.metrics.APIMetrics
import code.metrics.MappedMetric.userId
import code.model.AppType
import code.model.dataAccess.{AuthUser, MappedBankAccount}
import code.regulatedentities.MappedRegulatedEntityProvider
import code.userlocks.UserLocksProvider
import code.users.Users
import code.util.Helper
import code.util.Helper.ObpS
import code.views.Views
import code.views.system.{AccountAccess, ViewDefinition}
import com.github.dwickern.macros.NameOf.nameOf
import com.openbankproject.commons.ExecutionContext.Implicits.global
import com.openbankproject.commons.model._
import com.openbankproject.commons.model.enums.{AtmAttributeType, ConsentType, UserAttributeType}
import com.openbankproject.commons.util.{ApiVersion, ScannedApiVersion}
import net.liftweb.common.Full
import net.liftweb.http.rest.RestHelper
import net.liftweb.json
import net.liftweb.json.{compactRender, parse, prettyRender}
import net.liftweb.mapper.By
import net.liftweb.util.Helpers.tryo
import net.liftweb.util.{Helpers, StringHelpers}

import java.text.SimpleDateFormat
import java.time.{LocalDate, ZoneId}
import java.util.Date
import scala.collection.immutable.{List, Nil}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

trait APIMethods510 {
  self: RestHelper =>

  val Implementations5_1_0 = new Implementations510()

  class Implementations510 {

    val implementedInApiVersion: ScannedApiVersion = ApiVersion.v5_1_0

    private val staticResourceDocs = ArrayBuffer[ResourceDoc]()
    def resourceDocs = staticResourceDocs 

    val apiRelations = ArrayBuffer[ApiRelation]()
    val codeContext = CodeContext(staticResourceDocs, apiRelations)



    staticResourceDocs += ResourceDoc(
      root,
      implementedInApiVersion,
      "root",
      "GET",
      "/root",
      "Get API Info (root)",
      """Returns information about:
        |
        |* API version
        |* Hosted by information
        |* Hosted at information
        |* Energy source information
        |* Git Commit""",
      EmptyBody,
      apiInfoJson400,
      List(UnknownError, "no connector set"),
      apiTagApi  :: Nil)

    lazy val root: OBPEndpoint = {
      case (Nil | "root" :: Nil) JsonGet _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            _ <- Future() // Just start async call
          } yield {
            (JSONFactory510.getApiInfoJSON(OBPAPI5_1_0.version,OBPAPI5_1_0.versionStatus), HttpCode.`200`(cc.callContext))
          }
      }
    }

    staticResourceDocs += ResourceDoc(
      suggestedSessionTimeout,
      implementedInApiVersion,
      nameOf(suggestedSessionTimeout),
      "GET",
      "/ui/suggested-session-timeout",
      "Get Suggested Session Timeout",
      """Returns information about:
        |
        |* Suggested session timeout in case of a user inactivity
        """,
      EmptyBody,
      SuggestedSessionTimeoutV510("300"),
      List(UnknownError),
      apiTagApi  :: Nil)

    lazy val suggestedSessionTimeout: OBPEndpoint = {
      case "ui" :: "suggested-session-timeout" :: Nil JsonGet _ =>
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            timeoutInSeconds: Int <- Future(APIUtil.getPropsAsIntValue("session_inactivity_timeout_in_seconds", 300))
          } yield {
            (SuggestedSessionTimeoutV510(timeoutInSeconds.toString), HttpCode.`200`(cc.callContext))
          }
    }


    staticResourceDocs += ResourceDoc(
      regulatedEntities,
      implementedInApiVersion,
      nameOf(regulatedEntities),
      "GET",
      "/regulated-entities",
      "Get Regulated Entities",
      """Returns information about:
        |
        |* Regulated Entities
        """,
      EmptyBody,
      regulatedEntitiesJsonV510,
      List(UnknownError),
      apiTagDirectory :: apiTagApi  :: Nil)

    lazy val regulatedEntities: OBPEndpoint = {
      case "regulated-entities" :: Nil JsonGet _ =>
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (entities, callContext) <- getRegulatedEntitiesNewStyle(cc.callContext)
          } yield {
            (createRegulatedEntitiesJson(entities), HttpCode.`200`(callContext))
          }
    }

    staticResourceDocs += ResourceDoc(
      getRegulatedEntityById,
      implementedInApiVersion,
      nameOf(getRegulatedEntityById),
      "GET",
      "/regulated-entities/REGULATED_ENTITY_ID",
      "Get Regulated Entity",
      """Get Regulated Entity By REGULATED_ENTITY_ID
        """,
      EmptyBody,
      regulatedEntitiesJsonV510,
      List(UnknownError),
      apiTagDirectory :: apiTagApi  :: Nil)

    lazy val getRegulatedEntityById: OBPEndpoint = {
      case "regulated-entities" :: regulatedEntityId :: Nil JsonGet _ =>
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (entity, callContext)  <- getRegulatedEntityByEntityIdNewStyle(regulatedEntityId, cc.callContext)
          } yield {
            (createRegulatedEntityJson(entity), HttpCode.`200`(callContext))
          }
    }


    staticResourceDocs += ResourceDoc(
      createRegulatedEntity,
      implementedInApiVersion,
      nameOf(createRegulatedEntity),
      "POST",
      "/regulated-entities",
      "Create Regulated Entity",
      s"""Create Regulated Entity
         |
         |${authenticationRequiredMessage(true)}
         |
         |""",
      regulatedEntityPostJsonV510,
      regulatedEntitiesJsonV510,
      List(
        $UserNotLoggedIn,
        UserHasMissingRoles,
        InvalidJsonFormat,
        UnknownError
      ),
      List(apiTagDirectory, apiTagApi),
      Some(List(canCreateRegulatedEntity))
    )

    lazy val createRegulatedEntity: OBPEndpoint = {
      case "regulated-entities" :: Nil JsonPost json -> _ => {
        cc =>
          implicit val ec = EndpointContext(Some(cc))
          val failMsg = s"$InvalidJsonFormat The Json body should be the $RegulatedEntityPostJsonV510 "
          for {
            postedData <- NewStyle.function.tryons(failMsg, 400, cc.callContext) {
              json.extract[RegulatedEntityPostJsonV510]
            }
            failMsg = s"$InvalidJsonFormat The `services` field is not valid JSON"
            servicesString <- NewStyle.function.tryons(failMsg, 400, cc.callContext) {
              prettyRender(postedData.services)
            }
            (entity, callContext) <- createRegulatedEntityNewStyle(
              certificateAuthorityCaOwnerId = Some(postedData.certificate_authority_ca_owner_id),
              entityCertificatePublicKey = Some(postedData.entity_certificate_public_key),
              entityName = Some(postedData.entity_name),
              entityCode = Some(postedData.entity_code),
              entityType = Some(postedData.entity_type),
              entityAddress = Some(postedData.entity_address),
              entityTownCity = Some(postedData.entity_town_city),
              entityPostCode = Some(postedData.entity_post_code),
              entityCountry = Some(postedData.entity_country),
              entityWebSite = Some(postedData.entity_web_site),
              services = Some(servicesString),
              cc.callContext
            )
          } yield {
            (createRegulatedEntityJson(entity), HttpCode.`201`(callContext))
          }
      }
    }

    resourceDocs += ResourceDoc(
      deleteRegulatedEntity,
      implementedInApiVersion,
      nameOf(deleteRegulatedEntity),
      "DELETE",
      "/regulated-entities/REGULATED_ENTITY_ID",
      "Delete Regulated Entity",
      s"""Delete Regulated Entity specified by REGULATED_ENTITY_ID
         |
         |${authenticationRequiredMessage(true)}
         |""".stripMargin,
      EmptyBody,
      EmptyBody,
      List(
        $UserNotLoggedIn,
        UserHasMissingRoles,
        InvalidConnectorResponse,
        UnknownError
      ),
      List(apiTagDirectory, apiTagApi),
      Some(List(canDeleteRegulatedEntity)))

    lazy val deleteRegulatedEntity: OBPEndpoint = {
      case "regulated-entities" :: regulatedEntityId :: Nil JsonDelete _ => {
        cc =>
          implicit val ec = EndpointContext(Some(cc))
          for {
            (deleted, callContext) <- deleteRegulatedEntityNewStyle(
              regulatedEntityId: String,
              cc.callContext: Option[CallContext]
            )
          } yield {
            org.scalameta.logger.elem(deleted)
            (Full(deleted), HttpCode.`200`(callContext))
          }
      }
    }

    
    staticResourceDocs += ResourceDoc(
      waitingForGodot,
      implementedInApiVersion,
      nameOf(waitingForGodot),
      "GET",
      "/waiting-for-godot",
      "Waiting For Godot",
      """Waiting For Godot
        |
        |Uses query parameter "sleep" in milliseconds.
        |For instance: .../waiting-for-godot?sleep=50 means postpone response in 50 milliseconds.
        |""".stripMargin,
      EmptyBody,
      WaitingForGodotJsonV510(sleep_in_milliseconds = 50),
      List(UnknownError, "no connector set"),
      apiTagApi  :: Nil)

    lazy val waitingForGodot: OBPEndpoint = {
      case "waiting-for-godot" :: Nil JsonGet _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          implicit val timeout = EndpointTimeout(Constant.mediumEndpointTimeoutInMillis) // Set endpoint timeout explicitly
          for {
            httpParams <- NewStyle.function.extractHttpParamsFromUrl(cc.url)
            sleep: String = httpParams.filter(_.name == "sleep").headOption
              .map(_.values.headOption.getOrElse("0")).getOrElse("0")
            sleepInMillis: Long = tryo(sleep.trim.toLong).getOrElse(0)
            _ <- Future(Thread.sleep(sleepInMillis))
          } yield {
            (JSONFactory510.waitingForGodot(sleepInMillis), HttpCode.`200`(cc.callContext))
          }
      }
    }
    
    staticResourceDocs += ResourceDoc(
      getAllApiCollections,
      implementedInApiVersion,
      nameOf(getAllApiCollections),
      "GET",
      "/management/api-collections",
      "Get All API Collections",
      s"""Get All API Collections.
         |
         |${authenticationRequiredMessage(true)}
         |""".stripMargin,
      EmptyBody,
      apiCollectionsJson400,
      List(
        UserHasMissingRoles,
        UnknownError
      ),
      List(apiTagApiCollection),
      Some(canGetAllApiCollections :: Nil)
    )

    lazy val getAllApiCollections: OBPEndpoint = {
      case "management" :: "api-collections" :: Nil JsonGet _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (apiCollections, callContext) <- NewStyle.function.getAllApiCollections(cc.callContext)
          } yield {
            (JSONFactory400.createApiCollectionsJsonV400(apiCollections), HttpCode.`200`(callContext))
          }
      }
    }

    staticResourceDocs += ResourceDoc(
      createAgent,
      implementedInApiVersion,
      nameOf(createAgent),
      "POST",
      "/banks/BANK_ID/agents",
      "Create Agent",
      s"""
         |${authenticationRequiredMessage(true)}
         |""",
      postAgentJsonV510,
      agentJsonV510,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        InvalidJsonFormat,
        AgentNumberAlreadyExists,
        CreateAgentError,
        UnknownError
      ),
      List(apiTagCustomer, apiTagPerson)
    )
    
    lazy val createAgent : OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "agents" :: Nil JsonPost json -> _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            putData <- NewStyle.function.tryons(s"$InvalidJsonFormat The Json body should be the $PostAgentJsonV510 ", 400, cc.callContext) {
              json.extract[PostAgentJsonV510]
            }
            (agentNumberIsAvailable, callContext) <- NewStyle.function.checkAgentNumberAvailable(bankId, putData.agent_number, cc.callContext)
            _ <- Helper.booleanToFuture(failMsg= s"$AgentNumberAlreadyExists Current agent_number(${putData.agent_number}) and Current bank_id(${bankId.value})", cc=callContext) {agentNumberIsAvailable}
            (agent, callContext) <- NewStyle.function.createAgent(
              bankId = bankId.value,
              legalName = putData.legal_name,
              mobileNumber = putData.mobile_phone_number,
              agentNumber = putData.agent_number,
              callContext,
            )
            (bankAccount, callContext) <- NewStyle.function.createBankAccount(
              bankId,
              AccountId(APIUtil.generateUUID()),
              "AGENT",
              "AGENT",
              putData.currency,
              0,
              putData.legal_name,
              null,
              Nil,
              callContext
            )
            (_, callContext) <- NewStyle.function.createAgentAccountLink(agent.agentId, bankAccount.bankId.value, bankAccount.accountId.value, callContext)
            
          } yield {
            (JSONFactory510.createAgentJson(agent, bankAccount), HttpCode.`201`(callContext))
          }
      }
    }

    staticResourceDocs += ResourceDoc(
      updateAgentStatus,
      implementedInApiVersion,
      nameOf(updateAgentStatus),
      "PUT",
      "/banks/BANK_ID/agents/AGENT_ID",
      "Update Agent status",
      s"""
         |${authenticationRequiredMessage(true)}
         |""",
      putAgentJsonV510,
      agentJsonV510,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        InvalidJsonFormat,
        AgentNotFound,
        AgentAccountLinkNotFound,
        UnknownError
      ),
      List(apiTagCustomer, apiTagPerson),
      Some(canUpdateAgentStatusAtAnyBank :: canUpdateAgentStatusAtOneBank :: Nil)
    )
    
    lazy val updateAgentStatus : OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "agents"  :: agentId  :: Nil JsonPut json -> _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            postedData <- NewStyle.function.tryons(s"$InvalidJsonFormat The Json body should be the $PostAgentJsonV510 ", 400, cc.callContext) {
              json.extract[PutAgentJsonV510]
            }
            (agent, callContext) <- NewStyle.function.getAgentByAgentId(agentId, cc.callContext)
            (agentAccountLinks, callContext) <- NewStyle.function.getAgentAccountLinksByAgentId(agentId, callContext)
            agentAccountLink <- NewStyle.function.tryons(AgentAccountLinkNotFound, 400, callContext) {
              agentAccountLinks.head
            }
            (bankAccount, callContext) <- NewStyle.function.getBankAccount(BankId(agentAccountLink.bankId), AccountId(agentAccountLink.accountId), callContext)
            (agent, callContext) <- NewStyle.function.updateAgentStatus(
              agentId,
              postedData.is_pending_agent,
              postedData.is_confirmed_agent,
              callContext)
          } yield {
            (JSONFactory510.createAgentJson(agent, bankAccount), HttpCode.`200`(callContext))
          }
      }
    }
    
    staticResourceDocs += ResourceDoc(
      getAgent,
      implementedInApiVersion,
      nameOf(getAgent),
      "GET",
      "/banks/BANK_ID/agents/AGENT_ID",
      "Get Agent",
      s"""Get Agent.
         |
         |${authenticationRequiredMessage(true)}
         |""".stripMargin,
      EmptyBody,
      agentJsonV510,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        AgentNotFound,
        AgentAccountLinkNotFound,
        UnknownError
      ),
      List(apiTagAccount)
    )

    lazy val getAgent: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "agents" :: agentId  :: Nil JsonGet _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (agent, callContext) <- NewStyle.function.getAgentByAgentId(agentId, cc.callContext)
            (agentAccountLinks, callContext) <- NewStyle.function.getAgentAccountLinksByAgentId(agentId, callContext)
            agentAccountLink <- NewStyle.function.tryons(AgentAccountLinkNotFound, 400, callContext) {
              agentAccountLinks.head
            }
            (bankAccount, callContext) <- NewStyle.function.getBankAccount(BankId(agentAccountLink.bankId), AccountId(agentAccountLink.accountId), callContext)
          } yield {
            (JSONFactory510.createAgentJson(agent, bankAccount), HttpCode.`200`(callContext))
          }
      }
    }
    
    staticResourceDocs += ResourceDoc(
      createNonPersonalUserAttribute,
      implementedInApiVersion,
      nameOf(createNonPersonalUserAttribute),
      "POST",
      "/users/USER_ID/non-personal/attributes",
      "Create Non Personal User Attribute",
      s""" Create Non Personal User Attribute
         |
         |The type field must be one of "STRING", "INTEGER", "DOUBLE" or DATE_WITH_DAY"
         |
         |${authenticationRequiredMessage(true)}
         |
         |""",
      userAttributeJsonV510,
      userAttributeResponseJsonV510,
      List(
        $UserNotLoggedIn,
        UserHasMissingRoles,
        InvalidJsonFormat,
        UnknownError
      ),
      List(apiTagUser),
      Some(List(canCreateNonPersonalUserAttribute))
    )

    lazy val createNonPersonalUserAttribute: OBPEndpoint = {
      case "users" :: userId ::"non-personal":: "attributes" :: Nil JsonPost json -> _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          val failMsg = s"$InvalidJsonFormat The Json body should be the $UserAttributeJsonV510 "
          for {
            (user, callContext) <- NewStyle.function.getUserByUserId(userId, cc.callContext)
            postedData <- NewStyle.function.tryons(failMsg, 400, callContext) {
              json.extract[UserAttributeJsonV510]
            }
            failMsg = s"$InvalidJsonFormat The `Type` field can only accept the following field: " +
              s"${UserAttributeType.DOUBLE}(12.1234), ${UserAttributeType.STRING}(TAX_NUMBER), ${UserAttributeType.INTEGER} (123)and ${UserAttributeType.DATE_WITH_DAY}(2012-04-23)"
            userAttributeType <- NewStyle.function.tryons(failMsg, 400, callContext) {
              UserAttributeType.withName(postedData.`type`)
            }
            (userAttribute, callContext) <- NewStyle.function.createOrUpdateUserAttribute(
              user.userId,
              None,
              postedData.name,
              userAttributeType,
              postedData.value,
              false,
              callContext
              )
          } yield {
            (JSONFactory510.createUserAttributeJson(userAttribute), HttpCode.`201`(callContext))
          }
      }
    }
    
    resourceDocs += ResourceDoc(
      deleteNonPersonalUserAttribute,
      implementedInApiVersion,
      nameOf(deleteNonPersonalUserAttribute),
      "DELETE",
      "/users/USER_ID/non-personal/attributes/USER_ATTRIBUTE_ID",
      "Delete Non Personal User Attribute",
      s"""Delete the Non Personal User Attribute specified by ENTITLEMENT_REQUEST_ID for a user specified by USER_ID
         |
         |${authenticationRequiredMessage(true)}
         |""".stripMargin,
      EmptyBody,
      EmptyBody,
      List(
        UserNotLoggedIn,
        UserHasMissingRoles,
        InvalidConnectorResponse,
        UnknownError
      ),
      List(apiTagUser),
      Some(List(canDeleteNonPersonalUserAttribute)))

    lazy val deleteNonPersonalUserAttribute: OBPEndpoint = {
      case "users" :: userId :: "non-personal" :: "attributes" :: userAttributeId :: Nil JsonDelete _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (_, callContext) <- authenticatedAccess(cc)
            (_, callContext) <- NewStyle.function.getUserByUserId(userId, callContext)
            (deleted,callContext) <- Connector.connector.vend.deleteUserAttribute(
              userAttributeId: String,
              callContext: Option[CallContext]
            ) map {
            i => (connectorEmptyResponse (i._1, callContext), i._2)
          }
          } yield {
            (Full(deleted), HttpCode.`204`(callContext))
          }
      }
    }
    
    resourceDocs += ResourceDoc(
      getNonPersonalUserAttributes,
      implementedInApiVersion,
      nameOf(getNonPersonalUserAttributes),
      "GET",
      "/users/USER_ID/non-personal/attributes",
      "Get Non Personal User Attributes",
      s"""Get Non Personal User Attribute for a user specified by USER_ID
         |
         |${authenticationRequiredMessage(true)}
         |""".stripMargin,
      EmptyBody,
      EmptyBody,
      List(
        UserNotLoggedIn,
        UserHasMissingRoles,
        InvalidConnectorResponse,
        UnknownError
      ),
      List(apiTagUser),
      Some(List(canGetNonPersonalUserAttributes)))

    lazy val getNonPersonalUserAttributes: OBPEndpoint = {
      case "users" :: userId :: "non-personal" ::"attributes" :: Nil JsonGet _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (_, callContext) <- authenticatedAccess(cc)
            (user, callContext) <- NewStyle.function.getUserByUserId(userId, callContext)
            (userAttributes,callContext) <- NewStyle.function.getNonPersonalUserAttributes(
              user.userId,
              callContext,
            ) 
          } yield {
            (JSONFactory510.createUserAttributesJson(userAttributes), HttpCode.`200`(callContext))
          }
      }
    }



    staticResourceDocs += ResourceDoc(
      syncExternalUser,
      implementedInApiVersion,
      nameOf(syncExternalUser),
      "POST",
      "/users/PROVIDER/PROVIDER_ID/sync",
      "Sync User",
      s"""The endpoint is used to create or sync an OBP User with User from an external identity provider.
      |PROVIDER is the host of the provider e.g. a Keycloak Host.
      |PROVIDER_ID is the unique identifier for the User at the PROVIDER.
      |At the end of the process, a User will exist in OBP with the Account Access records defined by the CBS.
      |
      |${authenticationRequiredMessage(true)}
      |
      |""",
      EmptyBody,
      refresUserJson,
      List(
        $UserNotLoggedIn,
        UserHasMissingRoles,
        UnknownError
      ),
      List(apiTagUser),
      Some(List(canSyncUser))
    )

    lazy val syncExternalUser : OBPEndpoint = {
      case "users" :: provider :: providerId :: "sync" :: Nil JsonPost _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (user: User, callContext) <- NewStyle.function.getOrCreateResourceUser(provider, providerId, cc.callContext)
            _ <- AuthUser.refreshUser(user, callContext)
          } yield {
            (JSONFactory510.getSyncedUser(user), HttpCode.`201`(callContext))
          }
      }
    }



    staticResourceDocs += ResourceDoc(
      getAccountsHeldByUserAtBank,
      implementedInApiVersion,
      nameOf(getAccountsHeldByUserAtBank),
      "GET",
      "/users/USER_ID/banks/BANK_ID/accounts-held",
      "Get Accounts Held By User",
      s"""Get Accounts held by the User if even the User has not been assigned the owner View yet.
         |
         |Can be used to onboard the account to the API - since all other account and transaction endpoints require views to be assigned.
         |
         |${accountTypeFilterText("/users/USER_ID/banks/BANK_ID/accounts-held")}
         |
         |
         |
        """.stripMargin,
      EmptyBody,
      coreAccountsHeldJsonV300,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        UserNotFoundByUserId,
        UnknownError
      ),
      List(apiTagAccount),
      Some(List(canGetAccountsHeldAtOneBank, canGetAccountsHeldAtAnyBank))
    )

    lazy val getAccountsHeldByUserAtBank: OBPEndpoint = {
      case "users" :: userId :: "banks" :: BankId(bankId) :: "accounts-held" :: Nil JsonGet req => {
        cc =>
          implicit val ec = EndpointContext(Some(cc))
          for {
            (u, callContext) <- NewStyle.function.getUserByUserId(userId, cc.callContext)
            (availableAccounts, callContext) <- NewStyle.function.getAccountsHeld(bankId, u, callContext)
            (accounts, callContext) <- NewStyle.function.getBankAccountsHeldFuture(availableAccounts.toList, callContext)

            accountHelds <- getFilteredCoreAccounts(availableAccounts, req, callContext).map { it =>
              val coreAccountIds: List[String] = it._1.map(_.id)
              accounts.filter(accountHeld => coreAccountIds.contains(accountHeld.id))
            }
          } yield {
            (JSONFactory300.createCoreAccountsByCoreAccountsJSON(accountHelds), HttpCode.`200`(callContext))
          }
      }
    }

    staticResourceDocs += ResourceDoc(
      getAccountsHeldByUser,
      implementedInApiVersion,
      nameOf(getAccountsHeldByUser),
      "GET",
      "/users/USER_ID/accounts-held",
      "Get Accounts Held By User",
      s"""Get Accounts held by the User if even the User has not been assigned the owner View yet.
         |
         |Can be used to onboard the account to the API - since all other account and transaction endpoints require views to be assigned.
         |
         |${accountTypeFilterText("/users/USER_ID/accounts-held")}
         |
         |
         |
        """.stripMargin,
      EmptyBody,
      coreAccountsHeldJsonV300,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        UserNotFoundByUserId,
        UnknownError
      ),
      List(apiTagAccount),
      Some(List(canGetAccountsHeldAtAnyBank))
    )

    lazy val getAccountsHeldByUser: OBPEndpoint = {
      case "users" :: userId :: "accounts-held" :: Nil JsonGet req => {
        cc =>
          implicit val ec = EndpointContext(Some(cc))
          for {
            (u, callContext) <- NewStyle.function.getUserByUserId(userId, cc.callContext)
            (availableAccounts, callContext) <- NewStyle.function.getAccountsHeldByUser(u, callContext)
            (accounts, callContext) <- NewStyle.function.getBankAccountsHeldFuture(availableAccounts, callContext)

            accountHelds <- getFilteredCoreAccounts(availableAccounts, req, callContext).map { it =>
              val coreAccountIds: List[String] = it._1.map(_.id)
              accounts.filter(accountHeld => coreAccountIds.contains(accountHeld.id))
            }
          } yield {
            (JSONFactory300.createCoreAccountsByCoreAccountsJSON(accountHelds), HttpCode.`200`(callContext))
          }
      }
    }



    staticResourceDocs += ResourceDoc(
      getEntitlementsAndPermissions,
      implementedInApiVersion,
      "getEntitlementsAndPermissions",
      "GET",
      "/users/USER_ID/entitlements-and-permissions",
      "Get Entitlements and Permissions for a User",
      s"""
         |
         |
      """.stripMargin,
      EmptyBody,
      userJsonV300,
      List(
        $UserNotLoggedIn,
        UserNotFoundByUserId, 
        UserHasMissingRoles, 
        UnknownError),
      List(apiTagRole, apiTagEntitlement, apiTagUser),
      Some(List(canGetEntitlementsForAnyUserAtAnyBank)))


    lazy val getEntitlementsAndPermissions: OBPEndpoint = {
      case "users" :: userId :: "entitlements-and-permissions" :: Nil JsonGet _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (user, callContext) <- NewStyle.function.getUserByUserId(userId, cc.callContext)
            entitlements <- NewStyle.function.getEntitlementsByUserId(userId, callContext)
          } yield {
            val permissions: Option[Permission] = Views.views.vend.getPermissionForUser(user).toOption
            (JSONFactory300.createUserInfoJSON (user, entitlements, permissions), HttpCode.`200`(callContext))
          }
      }
    }
    
    
    staticResourceDocs += ResourceDoc(
      customViewNamesCheck,
      implementedInApiVersion,
      nameOf(customViewNamesCheck),
      "GET",
      "/management/system/integrity/custom-view-names-check",
      "Check Custom View Names",
      s"""Check custom view names.
         |
         |${authenticationRequiredMessage(true)}
         |""".stripMargin,
      EmptyBody,
      CheckSystemIntegrityJsonV510(true),
      List(
        $UserNotLoggedIn,
        UserHasMissingRoles,
        UnknownError
      ),
      List(apiTagSystemIntegrity),
      Some(canGetSystemIntegrity :: Nil)
    )

    lazy val customViewNamesCheck: OBPEndpoint = {
      case "management" :: "system" :: "integrity" :: "custom-view-names-check" :: Nil JsonGet _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            incorrectViews: List[ViewDefinition] <- Future {
              ViewDefinition.getCustomViews().filter { view =>
                view.viewId.value.startsWith("_") == false
              }
            }
          } yield {
            (JSONFactory510.getCustomViewNamesCheck(incorrectViews), HttpCode.`200`(cc.callContext))
          }
      }
    }    
    staticResourceDocs += ResourceDoc(
      systemViewNamesCheck,
      implementedInApiVersion,
      nameOf(systemViewNamesCheck),
      "GET",
      "/management/system/integrity/system-view-names-check",
      "Check System View Names",
      s"""Check system view names.
         |
         |${authenticationRequiredMessage(true)}
         |""".stripMargin,
      EmptyBody,
      CheckSystemIntegrityJsonV510(true),
      List(
        $UserNotLoggedIn,
        UserHasMissingRoles,
        UnknownError
      ),
      List(apiTagSystemIntegrity),
      Some(canGetSystemIntegrity :: Nil)
    )

    lazy val systemViewNamesCheck: OBPEndpoint = {
      case "management" :: "system" :: "integrity" :: "system-view-names-check" :: Nil JsonGet _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            incorrectViews: List[ViewDefinition] <- Future {
              ViewDefinition.getSystemViews().filter { view =>
                view.viewId.value.startsWith("_") == true
              }
            }
          } yield {
            (JSONFactory510.getSystemViewNamesCheck(incorrectViews), HttpCode.`200`(cc.callContext))
          }
      }
    }
    
    staticResourceDocs += ResourceDoc(
      accountAccessUniqueIndexCheck,
      implementedInApiVersion,
      nameOf(accountAccessUniqueIndexCheck),
      "GET",
      "/management/system/integrity/account-access-unique-index-1-check",
      "Check Unique Index at Account Access",
      s"""Check unique index at account access table.
         |
         |${authenticationRequiredMessage(true)}
         |""".stripMargin,
      EmptyBody,
      CheckSystemIntegrityJsonV510(true),
      List(
        $UserNotLoggedIn,
        UserHasMissingRoles,
        UnknownError
      ),
      List(apiTagSystemIntegrity),
      Some(canGetSystemIntegrity :: Nil)
    )

    lazy val accountAccessUniqueIndexCheck: OBPEndpoint = {
      case "management" :: "system" :: "integrity" :: "account-access-unique-index-1-check" :: Nil JsonGet _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            groupedRows: Map[String, List[AccountAccess]] <- Future {
              AccountAccess.findAll().groupBy { a => 
                s"${a.bank_id.get}-${a.account_id.get}-${a.view_id.get}-${a.user_fk.get}-${a.consumer_id.get}"
              }.filter(_._2.size > 1) // Extract only duplicated rows
            }
          } yield {
            (JSONFactory510.getAccountAccessUniqueIndexCheck(groupedRows), HttpCode.`200`(cc.callContext))
          }
      }
    }    
    staticResourceDocs += ResourceDoc(
      accountCurrencyCheck,
      implementedInApiVersion,
      nameOf(accountCurrencyCheck),
      "GET",
      "/management/system/integrity/banks/BANK_ID/account-currency-check",
      "Check for Sensible Currencies",
      s"""Check for sensible currencies at Bank Account model
         |
         |${authenticationRequiredMessage(true)}
         |""".stripMargin,
      EmptyBody,
      CheckSystemIntegrityJsonV510(true),
      List(
        $UserNotLoggedIn,
        UserHasMissingRoles,
        UnknownError
      ),
      List(apiTagSystemIntegrity),
      Some(canGetSystemIntegrity :: Nil)
    )

    lazy val accountCurrencyCheck: OBPEndpoint = {
      case "management" :: "system" :: "integrity"  :: "banks" :: BankId(bankId) :: "account-currency-check" :: Nil JsonGet _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            currencies: List[String] <- Future {
              MappedBankAccount.findAll().map(_.accountCurrency.get).distinct
            }
            (bankCurrencies, callContext) <- NewStyle.function.getCurrentCurrencies(bankId, cc.callContext)
          } yield {
            (JSONFactory510.getSensibleCurrenciesCheck(bankCurrencies, currencies), HttpCode.`200`(callContext))
          }
      }
    }


    staticResourceDocs += ResourceDoc(
      getCurrenciesAtBank,
      implementedInApiVersion,
      nameOf(getCurrenciesAtBank),
      "GET",
      "/banks/BANK_ID/currencies",
      "Get Currencies at a Bank",
      """Get Currencies specified by BANK_ID
        |
      """.stripMargin,
      EmptyBody,
      currenciesJsonV510,
      List(
        $UserNotLoggedIn,
        UnknownError
      ),
      List(apiTagFx)
    )

    lazy val getCurrenciesAtBank: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "currencies" :: Nil JsonGet _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            _ <- Helper.booleanToFuture(failMsg = ConsumerHasMissingRoles + CanReadFx, cc=cc.callContext) {
              checkScope(bankId.value, getConsumerPrimaryKey(cc.callContext), ApiRole.canReadFx)
            }
            (_, callContext) <- NewStyle.function.getBank(bankId, cc.callContext)
            (currencies, callContext) <- NewStyle.function.getCurrentCurrencies(bankId, callContext)
          } yield {
            val json = CurrenciesJsonV510(currencies.map(CurrencyJsonV510(_)))
            (json, HttpCode.`200`(callContext))
          }

      }
    }


    staticResourceDocs += ResourceDoc(
      orphanedAccountCheck,
      implementedInApiVersion,
      nameOf(orphanedAccountCheck),
      "GET",
      "/management/system/integrity/banks/BANK_ID/orphaned-account-check",
      "Check for Orphaned Accounts",
      s"""Check for orphaned accounts at Bank Account model
         |
         |${authenticationRequiredMessage(true)}
         |""".stripMargin,
      EmptyBody,
      CheckSystemIntegrityJsonV510(true),
      List(
        $UserNotLoggedIn,
        UserHasMissingRoles,
        UnknownError
      ),
      List(apiTagSystemIntegrity),
      Some(canGetSystemIntegrity :: Nil)
    )

    lazy val orphanedAccountCheck: OBPEndpoint = {
      case "management" :: "system" :: "integrity"  :: "banks" :: BankId(bankId) :: "orphaned-account-check" :: Nil JsonGet _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            accountAccesses: List[String] <- Future {
              AccountAccess.findAll(By(AccountAccess.bank_id, bankId.value)).map(_.account_id.get)
            }
            bankAccounts <- Future {
              MappedBankAccount.findAll(By(MappedBankAccount.bank, bankId.value)).map(_.accountId.value)
            }
          } yield {
            val orphanedAccounts: List[String] = accountAccesses.filterNot { accountAccess =>
              bankAccounts.contains(accountAccess)
            }
            (JSONFactory510.getOrphanedAccountsCheck(orphanedAccounts), HttpCode.`200`(cc.callContext))
          }
      }
    }








    staticResourceDocs += ResourceDoc(
      createAtmAttribute,
      implementedInApiVersion,
      nameOf(createAtmAttribute),
      "POST",
      "/banks/BANK_ID/atms/ATM_ID/attributes",
      "Create ATM Attribute",
      s""" Create ATM Attribute
         |
         |The type field must be one of "STRING", "INTEGER", "DOUBLE" or DATE_WITH_DAY"
         |
         |${authenticationRequiredMessage(true)}
         |
         |""",
      atmAttributeJsonV510,
      atmAttributeResponseJsonV510,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        InvalidJsonFormat,
        UnknownError
      ),
      List(apiTagATM),
      Some(List(canCreateAtmAttribute, canCreateAtmAttributeAtAnyBank))
    )

    lazy val createAtmAttribute : OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "atms" :: AtmId(atmId) :: "attributes" :: Nil JsonPost json -> _=> {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (_, callContext) <- NewStyle.function.getAtm(bankId, atmId, cc.callContext)
            failMsg = s"$InvalidJsonFormat The Json body should be the $AtmAttributeJsonV510 "
            postedData <- NewStyle.function.tryons(failMsg, 400, callContext) {
              json.extract[AtmAttributeJsonV510]
            }
            failMsg = s"$InvalidJsonFormat The `Type` field can only accept the following field: " +
              s"${AtmAttributeType.DOUBLE}(12.1234), ${AtmAttributeType.STRING}(TAX_NUMBER), ${AtmAttributeType.INTEGER}(123) and ${AtmAttributeType.DATE_WITH_DAY}(2012-04-23)"
            bankAttributeType <- NewStyle.function.tryons(failMsg, 400, callContext) {
              AtmAttributeType.withName(postedData.`type`)
            }
            (atmAttribute, callContext) <- NewStyle.function.createOrUpdateAtmAttribute(
              bankId,
              atmId,
              None,
              postedData.name,
              bankAttributeType,
              postedData.value,
              postedData.is_active,
              callContext: Option[CallContext]
            )
          } yield {
            (JSONFactory510.createAtmAttributeJson(atmAttribute), HttpCode.`201`(callContext))
          }
      }
    }
    staticResourceDocs += ResourceDoc(
      getAgents,
      implementedInApiVersion,
      nameOf(getAgents),
      "GET",
      "/banks/BANK_ID/agents",
      "Get Agents at Bank",
      s"""Get Agents at Bank.
         |
         |${authenticationRequiredMessage(false)}
         |
         |${urlParametersDocument(true, true)}
         |""".stripMargin,
      EmptyBody,
      minimalAgentsJsonV510,
      List(
        $BankNotFound,
        AgentsNotFound,
        UnknownError
      ),
      List(apiTagAccount)
    )

    lazy val getAgents: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "agents"  :: Nil JsonGet _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (requestParams, callContext) <- extractQueryParams(cc.url, List("limit","offset","sort_direction"), cc.callContext)
            (agents, callContext) <- NewStyle.function.getAgents(bankId.value, requestParams, callContext)
          } yield {
            (JSONFactory510.createMinimalAgentsJson(agents), HttpCode.`200`(callContext))
          }
      }
    }

    staticResourceDocs += ResourceDoc(
      getAtmAttributes,
      implementedInApiVersion,
      nameOf(getAtmAttributes),
      "GET",
      "/banks/BANK_ID/atms/ATM_ID/attributes",
      "Get ATM Attributes",
      s""" Get ATM Attributes
         |
         |${authenticationRequiredMessage(true)}
         |
         |""",
      EmptyBody,
      atmAttributesResponseJsonV510,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        InvalidJsonFormat,
        UnknownError
      ),
      List(apiTagATM),
      Some(List(canGetAtmAttribute, canGetAtmAttributeAtAnyBank))
    )

    lazy val getAtmAttributes : OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "atms" :: AtmId(atmId) :: "attributes" :: Nil JsonGet _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (_, callContext) <- NewStyle.function.getAtm(bankId, atmId, cc.callContext)
            (attributes, callContext) <- NewStyle.function.getAtmAttributesByAtm(bankId, atmId, callContext)
          } yield {
            (JSONFactory510.createAtmAttributesJson(attributes), HttpCode.`200`(callContext))
          }
      }
    }

    staticResourceDocs += ResourceDoc(
      getAtmAttribute,
      implementedInApiVersion,
      nameOf(getAtmAttribute),
      "GET",
      "/banks/BANK_ID/atms/ATM_ID/attributes/ATM_ATTRIBUTE_ID",
      "Get ATM Attribute By ATM_ATTRIBUTE_ID",
      s""" Get ATM Attribute By ATM_ATTRIBUTE_ID
         |
         |${authenticationRequiredMessage(true)}
         |
         |""",
      EmptyBody,
      atmAttributeResponseJsonV510,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        InvalidJsonFormat,
        UnknownError
      ),
      List(apiTagATM),
      Some(List(canGetAtmAttribute, canGetAtmAttributeAtAnyBank))
    )

    lazy val getAtmAttribute : OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "atms" :: AtmId(atmId) :: "attributes" :: atmAttributeId :: Nil JsonGet _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (_, callContext) <- NewStyle.function.getAtm(bankId, atmId, cc.callContext)
            (attribute, callContext) <- NewStyle.function.getAtmAttributeById(atmAttributeId, callContext)
          } yield {
            (JSONFactory510.createAtmAttributeJson(attribute), HttpCode.`200`(callContext))
          }
      }
    }


    staticResourceDocs += ResourceDoc(
      updateAtmAttribute,
      implementedInApiVersion,
      nameOf(updateAtmAttribute),
      "PUT",
      "/banks/BANK_ID/atms/ATM_ID/attributes/ATM_ATTRIBUTE_ID",
      "Update ATM Attribute",
      s""" Update ATM Attribute. 
         |
         |Update an ATM Attribute by its id.
         |
         |${authenticationRequiredMessage(true)}
         |
         |""",
      atmAttributeJsonV510,
      atmAttributeResponseJsonV510,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        UserHasMissingRoles,
        UnknownError
      ),
      List(apiTagATM),
      Some(List(canUpdateAtmAttribute, canUpdateAtmAttributeAtAnyBank))
    )

    lazy val updateAtmAttribute : OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "atms" :: AtmId(atmId) :: "attributes" :: atmAttributeId :: Nil JsonPut json -> _ =>{
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (_, callContext) <- NewStyle.function.getAtm(bankId, atmId, cc.callContext)
            failMsg = s"$InvalidJsonFormat The Json body should be the $AtmAttributeJsonV510 "
            postedData <- NewStyle.function.tryons(failMsg, 400, callContext) {
              json.extract[AtmAttributeJsonV510]
            }
            failMsg = s"$InvalidJsonFormat The `Type` field can only accept the following field: " +
              s"${AtmAttributeType.DOUBLE}(12.1234), ${AtmAttributeType.STRING}(TAX_NUMBER), ${AtmAttributeType.INTEGER}(123) and ${AtmAttributeType.DATE_WITH_DAY}(2012-04-23)"
            atmAttributeType <- NewStyle.function.tryons(failMsg, 400, cc.callContext) {
              AtmAttributeType.withName(postedData.`type`)
            }
            (_, callContext) <- NewStyle.function.getAtmAttributeById(atmAttributeId, cc.callContext)
            (atmAttribute, callContext) <- NewStyle.function.createOrUpdateAtmAttribute(
              bankId,
              atmId,
              Some(atmAttributeId),
              postedData.name,
              atmAttributeType,
              postedData.value,
              postedData.is_active,
              callContext: Option[CallContext]
            )
          } yield {
            (JSONFactory510.createAtmAttributeJson(atmAttribute), HttpCode.`200`(callContext))
          }
      }
    }


    staticResourceDocs += ResourceDoc(
      deleteAtmAttribute,
      implementedInApiVersion,
      nameOf(deleteAtmAttribute),
      "DELETE",
      "/banks/BANK_ID/atms/ATM_ID/attributes/ATM_ATTRIBUTE_ID",
      "Delete ATM Attribute",
      s""" Delete ATM Attribute
         |
         |Delete a Atm Attribute by its id.
         |
         |${authenticationRequiredMessage(true)}
         |
         |""",
      EmptyBody,
      EmptyBody,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        UserHasMissingRoles,
        UnknownError
      ),
      List(apiTagATM),
      Some(List(canDeleteAtmAttribute, canDeleteAtmAttributeAtAnyBank))
    )

    lazy val deleteAtmAttribute : OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "atms" :: AtmId(atmId) :: "attributes" :: atmAttributeId ::  Nil JsonDelete _=> {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (_, callContext) <- NewStyle.function.getAtm(bankId, atmId, cc.callContext)
            (atmAttribute, callContext) <- NewStyle.function.deleteAtmAttribute(atmAttributeId, callContext)
          } yield {
            (Full(atmAttribute), HttpCode.`204`(callContext))
          }
      }
    }


    staticResourceDocs += ResourceDoc(
      updateConsentStatusByConsent,
      implementedInApiVersion,
      nameOf(updateConsentStatusByConsent),
      "PUT",
      "/management/banks/BANK_ID/consents/CONSENT_ID",
      "Update Consent Status by CONSENT_ID",
      s"""
         |
         |
         |This endpoint is used to update the Status of Consent.
         |
         |Each Consent has one of the following states: ${ConsentStatus.values.toList.sorted.mkString(", ")}.
         |
         |${authenticationRequiredMessage(true)}
         |
         |""",
      PutConsentStatusJsonV400(status = "AUTHORISED"),
      ConsentChallengeJsonV310(
        consent_id = "9d429899-24f5-42c8-8565-943ffa6a7945",
        jwt = "eyJhbGciOiJIUzI1NiJ9.eyJlbnRpdGxlbWVudHMiOltdLCJjcmVhdGVkQnlVc2VySWQiOiJhYjY1MzlhOS1iMTA1LTQ0ODktYTg4My0wYWQ4ZDZjNjE2NTciLCJzdWIiOiIyMWUxYzhjYy1mOTE4LTRlYWMtYjhlMy01ZTVlZWM2YjNiNGIiLCJhdWQiOiJlanpuazUwNWQxMzJyeW9tbmhieDFxbXRvaHVyYnNiYjBraWphanNrIiwibmJmIjoxNTUzNTU0ODk5LCJpc3MiOiJodHRwczpcL1wvd3d3Lm9wZW5iYW5rcHJvamVjdC5jb20iLCJleHAiOjE1NTM1NTg0OTksImlhdCI6MTU1MzU1NDg5OSwianRpIjoiMDlmODhkNWYtZWNlNi00Mzk4LThlOTktNjYxMWZhMWNkYmQ1Iiwidmlld3MiOlt7ImFjY291bnRfaWQiOiJtYXJrb19wcml2aXRlXzAxIiwiYmFua19pZCI6ImdoLjI5LnVrLngiLCJ2aWV3X2lkIjoib3duZXIifSx7ImFjY291bnRfaWQiOiJtYXJrb19wcml2aXRlXzAyIiwiYmFua19pZCI6ImdoLjI5LnVrLngiLCJ2aWV3X2lkIjoib3duZXIifV19.8cc7cBEf2NyQvJoukBCmDLT7LXYcuzTcSYLqSpbxLp4",
        status = "AUTHORISED"
      ),
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        InvalidJsonFormat,
        ConsentNotFound,
        InvalidConnectorResponse,
        UnknownError
      ),
      apiTagConsent :: apiTagPSD2AIS :: Nil,
      Some(List(canUpdateConsentStatusAtOneBank, canUpdateConsentStatusAtAnyBank))
    )

    lazy val updateConsentStatusByConsent: OBPEndpoint = {
      case "management" :: "banks" :: BankId(bankId) :: "consents" :: consentId :: Nil JsonPut json -> _ => {
        cc =>
          implicit val ec = EndpointContext(Some(cc))
          for {
            consentJson <- NewStyle.function.tryons(s"$InvalidJsonFormat The Json body should be the $PutConsentStatusJsonV400 ", 400, cc.callContext) {
              json.extract[PutConsentStatusJsonV400]
            }
            _ <- Future(Consents.consentProvider.vend.getConsentByConsentId(consentId)) map {
              unboxFullOrFail(_, cc.callContext, s"$ConsentNotFound ($consentId)", 404)
            }
            status = ConsentStatus.withName(consentJson.status)
            consent <- Future(Consents.consentProvider.vend.updateConsentStatus(consentId, status)) map {
              i => connectorEmptyResponse(i, cc.callContext)
            }
          } yield {
            (ConsentJsonV310(consent.consentId, consent.jsonWebToken, consent.status), HttpCode.`200`(cc.callContext))
          }
      }
    }

    staticResourceDocs += ResourceDoc(
      updateConsentAccountAccessByConsentId,
      implementedInApiVersion,
      nameOf(updateConsentAccountAccessByConsentId),
      "PUT",
      "/management/banks/BANK_ID/consents/CONSENT_ID/account-access",
      "Update Consent Account Access by CONSENT_ID",
      s"""
         |
         |This endpoint is used to update the Account Access of Consent.
         |
         |${authenticationRequiredMessage(true)}
         |
         |""",
      PutConsentPayloadJsonV510(
        access = ConsentAccessJson(
          accounts = Option(List(ConsentAccessAccountsJson(
            iban = Some(ExampleValue.ibanExample.value),
            bban = None,
            pan = None,
            maskedPan = None,
            msisdn = None,
            currency = None,
          )))
        )
      ),
      ConsentChallengeJsonV310(
        consent_id = "9d429899-24f5-42c8-8565-943ffa6a7945",
        jwt = "eyJhbGciOiJIUzI1NiJ9.eyJlbnRpdGxlbWVudHMiOltdLCJjcmVhdGVkQnlVc2VySWQiOiJhYjY1MzlhOS1iMTA1LTQ0ODktYTg4My0wYWQ4ZDZjNjE2NTciLCJzdWIiOiIyMWUxYzhjYy1mOTE4LTRlYWMtYjhlMy01ZTVlZWM2YjNiNGIiLCJhdWQiOiJlanpuazUwNWQxMzJyeW9tbmhieDFxbXRvaHVyYnNiYjBraWphanNrIiwibmJmIjoxNTUzNTU0ODk5LCJpc3MiOiJodHRwczpcL1wvd3d3Lm9wZW5iYW5rcHJvamVjdC5jb20iLCJleHAiOjE1NTM1NTg0OTksImlhdCI6MTU1MzU1NDg5OSwianRpIjoiMDlmODhkNWYtZWNlNi00Mzk4LThlOTktNjYxMWZhMWNkYmQ1Iiwidmlld3MiOlt7ImFjY291bnRfaWQiOiJtYXJrb19wcml2aXRlXzAxIiwiYmFua19pZCI6ImdoLjI5LnVrLngiLCJ2aWV3X2lkIjoib3duZXIifSx7ImFjY291bnRfaWQiOiJtYXJrb19wcml2aXRlXzAyIiwiYmFua19pZCI6ImdoLjI5LnVrLngiLCJ2aWV3X2lkIjoib3duZXIifV19.8cc7cBEf2NyQvJoukBCmDLT7LXYcuzTcSYLqSpbxLp4",
        status = "AUTHORISED"
      ),
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        InvalidJsonFormat,
        ConsentNotFound,
        InvalidConnectorResponse,
        UnknownError
      ),
      apiTagConsent :: apiTagPSD2AIS :: Nil,
      Some(List(canUpdateConsentAccountAccessAtOneBank, canUpdateConsentAccountAccessAtAnyBank))
    )

    lazy val updateConsentAccountAccessByConsentId: OBPEndpoint = {
      case "management" :: "banks" :: BankId(bankId) :: "consents" :: consentId :: "account-access" :: Nil JsonPut json -> _ =>
        cc =>
          implicit val ec = EndpointContext(Some(cc))
          for {
            consent: MappedConsent <- Future(Consents.consentProvider.vend.getConsentByConsentId(consentId)) map {
              unboxFullOrFail(_, cc.callContext, s"$ConsentNotFound ($consentId)", 404)
            }
            consentJson <- NewStyle.function.tryons(s"$InvalidJsonFormat The Json body should be the $PutConsentPayloadJsonV510 ", 400, cc.callContext) {
              json.extract[PutConsentPayloadJsonV510]
            }
            _ <- Helper.booleanToFuture(s"$InvalidJsonFormat The Json body should be the $PutConsentPayloadJsonV510 ",400, cc.callContext) {
              // Add custom validation
              !{
                consentJson.access.accounts.isEmpty &&
                consentJson.access.balances.isEmpty &&
                consentJson.access.transactions.isEmpty
              }
            }
            consentJWT <- Consent.updateBerlinGroupConsentJWT(
                consentJson.access,
                consent,
                cc.callContext
              ) map {
                i => connectorEmptyResponse(i, cc.callContext)
              }
            updatedConsent <- Future(Consents.consentProvider.vend.setJsonWebToken(consent.consentId, consentJWT)) map {
              i => connectorEmptyResponse(i, cc.callContext)
            }
          } yield {
            (
              ConsentJsonV310(
                updatedConsent.consentId,
                updatedConsent.jsonWebToken,
                updatedConsent.status
              ),
              HttpCode.`200`(cc.callContext)
            )
          }
    }


    staticResourceDocs += ResourceDoc(
      getMyConsents,
      implementedInApiVersion,
      nameOf(getMyConsents),
      "GET",
      "/banks/BANK_ID/my/consents",
      "Get My Consents",
      s"""
         |
         |This endpoint gets the Consents created by a current User.
         |
         |${authenticationRequiredMessage(true)}
         |
      """.stripMargin,
      EmptyBody,
      consentsJsonV400,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        UnknownError
      ),
      List(apiTagConsent, apiTagPSD2AIS, apiTagPsd2))

    lazy val getMyConsents: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "my" :: "consents" :: Nil JsonGet _ => {
        cc =>
          implicit val ec = EndpointContext(Some(cc))
          for {
            consents <- Future {
              Consents.consentProvider.vend.getConsentsByUser(cc.userId)
                .sortBy(i => (i.creationDateTime, i.apiStandard)).reverse
            }
          } yield {
            val consentsOfBank = Consent.filterByBankId(consents, bankId)
            (createConsentsInfoJsonV510(consentsOfBank), HttpCode.`200`(cc))
          }
      }
    }


    staticResourceDocs += ResourceDoc(
      getConsentsAtBank,
      implementedInApiVersion,
      nameOf(getConsentsAtBank),
      "GET",
      "/management/consents/banks/BANK_ID",
      "Get Consents at Bank",
      s"""
         |
         |This endpoint gets the Consents at Bank by BANK_ID.
         |
         |${authenticationRequiredMessage(true)}
         |
         |1 limit (for pagination: defaults to 50)  eg:limit=200
         |
         |2 offset (for pagination: zero index, defaults to 0) eg: offset=10
         |
         |3 consumer_id  (ignore if omitted)
         |
         |4 user_id  (ignore if omitted)
         |
         |5 status  (ignore if omitted)
         |
         |eg: /management/consents/banks/BANK_ID?&consumer_id=78&limit=10&offset=10
         |
      """.stripMargin,
      EmptyBody,
      consentsJsonV510,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        UnknownError
      ),
      List(apiTagConsent, apiTagPSD2AIS, apiTagPsd2),
      Some(List(canGetConsentsAtOneBank, canGetConsentsAtAnyBank)),
    )

    lazy val getConsentsAtBank: OBPEndpoint = {
      case "management" :: "consents" :: "banks" :: BankId(bankId) :: Nil JsonGet _ => {
        cc =>
          implicit val ec = EndpointContext(Some(cc))
          for {
            httpParams <- NewStyle.function.extractHttpParamsFromUrl(cc.url)
            (obpQueryParams, callContext) <- createQueriesByHttpParamsFuture(httpParams, cc.callContext)
            consents <- Future {
              Consents.consentProvider.vend.getConsents(obpQueryParams)
            }
          } yield {
            val consentsOfBank = Consent.filterByBankId(consents, bankId)
            (createConsentsJsonV510(consentsOfBank), HttpCode.`200`(callContext))
          }
      }
    }


    staticResourceDocs += ResourceDoc(
      getConsentByConsentId,
      implementedInApiVersion,
      nameOf(getConsentByConsentId),
      "GET",
      "/user/current/consents/CONSENT_ID",
      "Get Consent By Consent Id",
      s"""
         |
         |This endpoint gets the Consent By consent id.
         |
         |${authenticationRequiredMessage(true)}
         |
      """.stripMargin,
      EmptyBody,
      consentJsonV500,
      List(
        $UserNotLoggedIn,
        UnknownError
      ),
      List(apiTagConsent, apiTagPSD2AIS, apiTagPsd2))
    lazy val getConsentByConsentId: OBPEndpoint = {
      case "user" :: "current" :: "consents" :: consentId :: Nil  JsonGet _  => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            consent <- Future { Consents.consentProvider.vend.getConsentByConsentId(consentId)} map {
              unboxFullOrFail(_, cc.callContext, ConsentNotFound, 404)
            }
            _ <- Helper.booleanToFuture(failMsg = ConsentNotFound, failCode = 404, cc = cc.callContext) {
              consent.mUserId == cc.userId
            }
          } yield {
            (JSONFactory510.getConsentInfoJson(consent), HttpCode.`200`(cc))
          }
      }
    }


    staticResourceDocs += ResourceDoc(
      getConsentByConsentIdViaConsumer,
      implementedInApiVersion,
      nameOf(getConsentByConsentIdViaConsumer),
      "GET",
      "/consumer/current/consents/CONSENT_ID",
      "Get Consent By Consent Id",
      s"""
         |
         |This endpoint gets the Consent By consent id.
         |
         |${authenticationRequiredMessage(true)}
         |
      """.stripMargin,
      EmptyBody,
      consentJsonV500,
      List(
        $UserNotLoggedIn,
        UnknownError
      ),
      List(apiTagConsent, apiTagPSD2AIS, apiTagPsd2))
    lazy val getConsentByConsentIdViaConsumer: OBPEndpoint = {
      case "consumer" :: "current"  :: "consents" :: consentId :: Nil  JsonGet _  => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            consent <- Future { Consents.consentProvider.vend.getConsentByConsentId(consentId)} map {
              unboxFullOrFail(_, cc.callContext, ConsentNotFound, 404)
            }
            _ <- Helper.booleanToFuture(failMsg = s"${consent.mConsumerId.get} != ${cc.consumer.map(_.consumerId.get).getOrElse("None")}", failCode = 404, cc = cc.callContext) {
              consent.mConsumerId.get == cc.consumer.map(_.consumerId.get).getOrElse("None")
            }
          } yield {
            (JSONFactory510.getConsentInfoJson(consent), HttpCode.`200`(cc))
          }
      }
    }
    
    staticResourceDocs += ResourceDoc(
      revokeConsentAtBank,
      implementedInApiVersion,
      nameOf(revokeConsentAtBank),
      "DELETE",
      "/banks/BANK_ID/consents/CONSENT_ID",
      "Revoke Consent at Bank",
      s"""
         |Revoke Consent specified by CONSENT_ID
         |
         |There are a few reasons you might need to revoke an application’s access to a user’s account:
         |  - The user explicitly wishes to revoke the application’s access
         |  - You as the service provider have determined an application is compromised or malicious, and want to disable it
         |  - etc.
         ||
         |OBP as a resource server stores access tokens in a database, then it is relatively easy to revoke some token that belongs to a particular user.
         |The status of the token is changed to "REVOKED" so the next time the revoked client makes a request, their token will fail to validate.
         |
         |${authenticationRequiredMessage(true)}
         |
      """.stripMargin,
      EmptyBody,
      revokedConsentJsonV310,
      List(
        UserNotLoggedIn,
        BankNotFound,
        UnknownError
      ),
      List(apiTagConsent, apiTagPSD2AIS, apiTagPsd2), 
      Some(List(canRevokeConsentAtBank))
    )

    lazy val revokeConsentAtBank: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "consents" :: consentId :: Nil JsonDelete _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (Full(user), callContext) <- authenticatedAccess(cc)
            (_, callContext) <- NewStyle.function.getBank(bankId, callContext)
            consent <- Future(Consents.consentProvider.vend.getConsentByConsentId(consentId)) map {
              unboxFullOrFail(_, callContext, ConsentNotFound)
            }
            _ <- Helper.booleanToFuture(failMsg = ConsentNotFound, cc=callContext) {
              consent.mUserId == user.userId
            }
            consent <- Future(Consents.consentProvider.vend.revoke(consentId)) map {
              i => connectorEmptyResponse(i, callContext)
            }
          } yield {
            (ConsentJsonV310(consent.consentId, consent.jsonWebToken, consent.status), HttpCode.`200`(callContext))
          }
      }
    }
    
   staticResourceDocs += ResourceDoc(
     selfRevokeConsent,
      implementedInApiVersion,
      nameOf(selfRevokeConsent),
      "DELETE",
      "/my/consent/current",
      "Revoke Consent used in the Current Call",
      s"""
         |Revoke Consent specified by Consent-Id at Request Header
         |
         |There are a few reasons you might need to revoke an application’s access to a user’s account:
         |  - The user explicitly wishes to revoke the application’s access
         |  - You as the service provider have determined an application is compromised or malicious, and want to disable it
         |  - etc.
         ||
         |OBP as a resource server stores access tokens in a database, then it is relatively easy to revoke some token that belongs to a particular user.
         |The status of the token is changed to "REVOKED" so the next time the revoked client makes a request, their token will fail to validate.
         |
         |${authenticationRequiredMessage(true)}
         |
      """.stripMargin,
      EmptyBody,
      revokedConsentJsonV310,
      List(
        UserNotLoggedIn,
        BankNotFound,
        UnknownError
      ),
      List(apiTagConsent, apiTagPSD2AIS, apiTagPsd2)
    )
    lazy val selfRevokeConsent: OBPEndpoint = {
      case "my" :: "consent" :: "current" :: Nil JsonDelete _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (Full(user), callContext) <- authenticatedAccess(cc)
            consentId = getConsentIdRequestHeaderValue(cc.requestHeaders).getOrElse("")
            _ <- Future(Consents.consentProvider.vend.getConsentByConsentId(consentId)) map {
              unboxFullOrFail(_, callContext, ConsentNotFound, 404)
            }
            consent <- Future(Consents.consentProvider.vend.revoke(consentId)) map {
              i => connectorEmptyResponse(i, callContext)
            }
          } yield {
            (ConsentJsonV310(consent.consentId, consent.jsonWebToken, consent.status), HttpCode.`200`(callContext))
          }
      }
    }
    
    
   staticResourceDocs += ResourceDoc(
     mtlsClientCertificateInfo,
      implementedInApiVersion,
      nameOf(mtlsClientCertificateInfo),
      "GET",
      "/my/mtls/certificate/current",
      "Provide client's certificate info of a current call",
      s"""
         |Provide client's certificate info of a current call specified by PSD2-CERT value at Request Header
         |
         |${authenticationRequiredMessage(true)}
         |
      """.stripMargin,
      EmptyBody,
      certificateInfoJsonV510,
      List(
        UserNotLoggedIn,
        BankNotFound,
        UnknownError
      ),
      List(apiTagConsent, apiTagPSD2AIS, apiTagPsd2)
    )
    lazy val mtlsClientCertificateInfo: OBPEndpoint = {
      case "my" :: "mtls" :: "certificate" :: "current" :: Nil JsonGet _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (Full(_), callContext) <- authenticatedAccess(cc)
            info <- Future(X509.getCertificateInfo(APIUtil.`getPSD2-CERT`(cc.requestHeaders))) map {
              unboxFullOrFail(_, callContext, X509GeneralError)
            }
          } yield {
            (info, HttpCode.`200`(callContext))
          }
      }
    }


    staticResourceDocs += ResourceDoc(
      updateMyApiCollection,
      implementedInApiVersion,
      nameOf(updateMyApiCollection),
      "PUT",
      "/my/api-collections/API_COLLECTION_ID",
      "Update My Api Collection By API_COLLECTION_ID",
      s"""Update Api Collection for logged in user.
         |
         |${authenticationRequiredMessage(true)}
         |""".stripMargin,
      postApiCollectionJson400,
      apiCollectionJson400,
      List(
        $UserNotLoggedIn,
        InvalidJsonFormat,
        UserNotFoundByUserId,
        UnknownError
      ),
      List(apiTagApiCollection)
    )

    lazy val updateMyApiCollection: OBPEndpoint = {
      case "my" :: "api-collections" :: apiCollectionId :: Nil JsonPut json -> _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            putJson <- NewStyle.function.tryons(s"$InvalidJsonFormat The Json body should be the $PostApiCollectionJson400", 400, cc.callContext) {
              json.extract[PostApiCollectionJson400]
            }
            (_, callContext) <- NewStyle.function.getApiCollectionById(apiCollectionId, cc.callContext)
            (apiCollection, callContext) <- NewStyle.function.updateApiCollection(
              apiCollectionId,
              putJson.api_collection_name,
              putJson.is_sharable,
              putJson.description.getOrElse(""),
              callContext
            )
          } yield {
            (JSONFactory400.createApiCollectionJsonV400(apiCollection), HttpCode.`200`(callContext))
          }
      }
    }


    staticResourceDocs += ResourceDoc(
      getUserByProviderAndUsername,
      implementedInApiVersion,
      nameOf(getUserByProviderAndUsername),
      "GET",
      "/users/provider/PROVIDER/username/USERNAME",
      "Get User by USERNAME",
      s"""Get user by PROVIDER and USERNAME
         |
         |${authenticationRequiredMessage(true)}
         |
         |CanGetAnyUser entitlement is required,
         |
      """.stripMargin,
      EmptyBody,
      userJsonV400,
      List($UserNotLoggedIn, UserHasMissingRoles, UserNotFoundByProviderAndUsername, UnknownError),
      List(apiTagUser),
      Some(List(canGetAnyUser))
    )
    
    lazy val getUserByProviderAndUsername: OBPEndpoint = {
      case "users" :: "provider" :: provider :: "username" :: username :: Nil JsonGet _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            user <- Users.users.vend.getUserByProviderAndUsernameFuture(provider, username) map {
              x => unboxFullOrFail(x, cc.callContext, UserNotFoundByProviderAndUsername, 404)
            }
            entitlements <- NewStyle.function.getEntitlementsByUserId(user.userId, cc.callContext)
            isLocked = LoginAttempt.userIsLocked(user.provider, user.name)
          } yield {
            (JSONFactory400.createUserInfoJSON(user, entitlements, None, isLocked), HttpCode.`200`(cc.callContext))
          }
      }
    }

    resourceDocs += ResourceDoc(
      getUserLockStatus,
      implementedInApiVersion,
      nameOf(getUserLockStatus),
      "GET",
      "/users/PROVIDER/USERNAME/lock-status",
      "Get User Lock Status",
      s"""
         |Get User Login Status.
         |${authenticationRequiredMessage(true)}
         |
         |""".stripMargin,
      EmptyBody,
      badLoginStatusJson,
      List(UserNotLoggedIn, UserNotFoundByProviderAndUsername, UserHasMissingRoles, UnknownError),
      List(apiTagUser),
      Some(List(canReadUserLockedStatus))
    )
    lazy val getUserLockStatus: OBPEndpoint = {
      //get private accounts for all banks
      case "users" ::provider :: username :: "lock-status" :: Nil JsonGet req => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (Full(u), callContext) <- SS.user
            _ <- NewStyle.function.hasEntitlement("", u.userId, ApiRole.canReadUserLockedStatus, callContext)
            _ <- Users.users.vend.getUserByProviderAndUsernameFuture(provider, username) map {
              x => unboxFullOrFail(x, callContext, UserNotFoundByProviderAndUsername, 404)
            }
            badLoginStatus <- Future {
              LoginAttempt.getOrCreateBadLoginStatus(provider, username)
            } map {
              unboxFullOrFail(_, callContext, s"$UserNotFoundByProviderAndUsername provider($provider), username($username)", 404)
            }
          } yield {
            (createBadLoginStatusJson(badLoginStatus), HttpCode.`200`(callContext))
          }
      }
    }

    resourceDocs += ResourceDoc(
      unlockUserByProviderAndUsername,
      implementedInApiVersion,
      nameOf(unlockUserByProviderAndUsername),
      "PUT",
      "/users/PROVIDER/USERNAME/lock-status",
      "Unlock the user",
      s"""
         |Unlock a User.
         |
         |(Perhaps the user was locked due to multiple failed login attempts)
         |
         |${authenticationRequiredMessage(true)}
         |
         |""".stripMargin,
      EmptyBody,
      badLoginStatusJson,
      List(UserNotLoggedIn, UserNotFoundByProviderAndUsername, UserHasMissingRoles, UnknownError),
      List(apiTagUser),
      Some(List(canUnlockUser)))
    lazy val unlockUserByProviderAndUsername: OBPEndpoint = {
      //get private accounts for all banks
      case "users" ::  provider :: username :: "lock-status" :: Nil JsonPut req => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (Full(u), callContext) <- SS.user
            _ <- NewStyle.function.hasEntitlement("", u.userId, ApiRole.canUnlockUser, callContext)
            _ <- Users.users.vend.getUserByProviderAndUsernameFuture(provider, username) map {
              x => unboxFullOrFail(x, callContext, UserNotFoundByProviderAndUsername, 404)
            }
            _ <- Future {
              LoginAttempt.resetBadLoginAttempts(provider, username)
            }
            _ <- Future {
              UserLocksProvider.unlockUser(provider, username)
            }
            badLoginStatus <- Future {
              LoginAttempt.getOrCreateBadLoginStatus(provider, username)
            } map {
              unboxFullOrFail(_, callContext, s"$UserNotFoundByProviderAndUsername provider($provider), username($username)", 404)
            }
          } yield {
            (createBadLoginStatusJson(badLoginStatus), HttpCode.`200`(callContext))
          }
      }
    }

    staticResourceDocs += ResourceDoc(
      lockUserByProviderAndUsername,
      implementedInApiVersion,
      nameOf(lockUserByProviderAndUsername),
      "POST",
      "/users/PROVIDER/USERNAME/locks",
      "Lock the user",
      s"""
         |Lock a User.
         |
         |${authenticationRequiredMessage(true)}
         |
         |""".stripMargin,
      EmptyBody,
      userLockStatusJson,
      List($UserNotLoggedIn, UserNotFoundByProviderAndUsername, UserHasMissingRoles, UnknownError),
      List(apiTagUser),
      Some(List(canLockUser)))
    lazy val lockUserByProviderAndUsername: OBPEndpoint = {
      case "users" :: provider :: username :: "locks" :: Nil JsonPost req => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (Full(u), callContext) <- SS.user
            userLocks <- Future {
              UserLocksProvider.lockUser(provider, username)
            } map {
              unboxFullOrFail(_, callContext, s"$UserNotFoundByProviderAndUsername provider($provider), username($username)", 404)
            }
          } yield {
            (JSONFactory400.createUserLockStatusJson(userLocks), HttpCode.`200`(callContext))
          }
      }
    }

    resourceDocs += ResourceDoc(
      getAggregateMetrics,
      implementedInApiVersion,
      nameOf(getAggregateMetrics),
      "GET",
      "/management/aggregate-metrics",
      "Get Aggregate Metrics",
      s"""Returns aggregate metrics on api usage eg. total count, response time (in ms), etc.
         |
         |Should be able to filter on the following fields
         |
         |eg: /management/aggregate-metrics?from_date=$DateWithMsExampleString&to_date=$DateWithMsExampleString&consumer_id=5
         |&user_id=66214b8e-259e-44ad-8868-3eb47be70646&implemented_by_partial_function=getTransactionsForBankAccount
         |&implemented_in_version=v3.0.0&url=/obp/v3.0.0/banks/gh.29.uk/accounts/8ca8a7e4-6d02-48e3-a029-0b2bf89de9f0/owner/transactions
         |&verb=GET&anon=false&app_name=MapperPostman
         |&exclude_app_names=API-EXPLORER,API-Manager,SOFI,null
         |
         |1 from_date (defaults to the day before the current date): eg:from_date=$DateWithMsExampleString
         |
         |2 to_date (defaults to the current date) eg:to_date=$DateWithMsExampleString
         |
         |3 consumer_id  (if null ignore)
         |
         |4 user_id (if null ignore)
         |
         |5 anon (if null ignore) only support two value : true (return where user_id is null.) or false (return where user_id is not null.)
         |
         |6 url (if null ignore), note: can not contain '&'.
         |
         |7 app_name (if null ignore)
         |
         |8 implemented_by_partial_function (if null ignore),
         |
         |9 implemented_in_version (if null ignore)
         |
         |10 verb (if null ignore)
         |
         |11 correlation_id (if null ignore)
         |
         |12 include_app_names (if null ignore).eg: &include_app_names=API-EXPLORER,API-Manager,SOFI,null
         |
         |13 include_url_patterns (if null ignore).you can design you own SQL LIKE pattern. eg: &include_url_patterns=%management/metrics%,%management/aggregate-metrics%
         |
         |14 include_implemented_by_partial_functions (if null ignore).eg: &include_implemented_by_partial_functions=getMetrics,getConnectorMetrics,getAggregateMetrics
         |
         |${authenticationRequiredMessage(true)}
         |
      """.stripMargin,
      EmptyBody,
      aggregateMetricsJSONV300,
      List(
        UserNotLoggedIn,
        UserHasMissingRoles,
        UnknownError
      ),
      List(apiTagMetric, apiTagAggregateMetrics),
      Some(List(canReadAggregateMetrics)))

    lazy val getAggregateMetrics: OBPEndpoint = {
      case "management" :: "aggregate-metrics" :: Nil JsonGet _ => {
        cc => {
          implicit val ec = EndpointContext(Some(cc))
          for {
            (Full(u), callContext) <- authenticatedAccess(cc)
            _ <- NewStyle.function.hasEntitlement("", u.userId, ApiRole.canReadAggregateMetrics, callContext)
            httpParams <- NewStyle.function.extractHttpParamsFromUrl(cc.url)
            (obpQueryParams, callContext) <- createQueriesByHttpParamsFuture(httpParams, callContext)
            aggregateMetrics <- APIMetrics.apiMetrics.vend.getAllAggregateMetricsFuture(obpQueryParams,true) map {
              x => unboxFullOrFail(x, callContext, GetAggregateMetricsError)
            }
          } yield {
            (createAggregateMetricJson(aggregateMetrics), HttpCode.`200`(callContext))
          }
        }

      }
    }


    staticResourceDocs += ResourceDoc(
      getMetrics,
      implementedInApiVersion,
      "getMetrics",
      "GET",
      "/management/metrics",
      "Get Metrics",
      s"""Get API metrics rows. These are records of each REST API call.
         |
         |require CanReadMetrics role
         |
         |Filters Part 1.*filtering* (no wilde cards etc.) parameters to GET /management/metrics
         |
         |You can filter by the following fields by applying url parameters
         |
         |eg: /management/metrics?from_date=$DateWithMsExampleString&to_date=$DateWithMsExampleString&limit=50&offset=2
         |
         |1 from_date e.g.:from_date=$DateWithMsExampleString Defaults to the Unix Epoch i.e. ${theEpochTime}
         |
         |2 to_date e.g.:to_date=$DateWithMsExampleString Defaults to a far future date i.e. ${APIUtil.ToDateInFuture}
         |
         |Note: it is recommended you send a valid from_date (e.g. 5 seconds ago) and to_date (now + 1 second) if you want to get the latest records
         | Otherwise you may receive stale cached results.
         |
         |3 limit (for pagination: defaults to 50)  eg:limit=200
         |
         |4 offset (for pagination: zero index, defaults to 0) eg: offset=10
         |
         |5 sort_by (defaults to date field) eg: sort_by=date
         |  possible values:
         |    "url",
         |    "date",
         |    "user_name",
         |    "app_name",
         |    "developer_email",
         |    "implemented_by_partial_function",
         |    "implemented_in_version",
         |    "consumer_id",
         |    "verb"
         |
         |6 direction (defaults to date desc) eg: direction=desc
         |
         |eg: /management/metrics?from_date=$DateWithMsExampleString&to_date=$DateWithMsExampleString&limit=10000&offset=0&anon=false&app_name=TeatApp&implemented_in_version=v2.1.0&verb=POST&user_id=c7b6cb47-cb96-4441-8801-35b57456753a&user_name=susan.uk.29@example.com&consumer_id=78
         |
         |Other filters:
         |
         |7 consumer_id  (if null ignore)
         |
         |8 user_id (if null ignore)
         |
         |9 anon (if null ignore) only support two value : true (return where user_id is null.) or false (return where user_id is not null.)
         |
         |10 url (if null ignore), note: can not contain '&'.
         |
         |11 app_name (if null ignore)
         |
         |12 implemented_by_partial_function (if null ignore),
         |
         |13 implemented_in_version (if null ignore)
         |
         |14 verb (if null ignore)
         |
         |15 correlation_id (if null ignore)
         |
         |16 duration (if null ignore) non digit chars will be silently omitted
         |
      """.stripMargin,
      EmptyBody,
      metricsJsonV510,
      List(
        UserNotLoggedIn,
        UserHasMissingRoles,
        UnknownError
      ),
      List(apiTagMetric, apiTagApi),
      Some(List(canReadMetrics)))

    lazy val getMetrics: OBPEndpoint = {
      case "management" :: "metrics" :: Nil JsonGet _ => {
        cc => {
          implicit val ec = EndpointContext(Some(cc))
          for {
            (Full(u), callContext) <- authenticatedAccess(cc)
            _ <- NewStyle.function.hasEntitlement("", u.userId, ApiRole.canReadMetrics, callContext)
            httpParams <- NewStyle.function.extractHttpParamsFromUrl(cc.url)
            (obpQueryParams, callContext) <- createQueriesByHttpParamsFuture(httpParams, callContext)
            metrics <- Future(APIMetrics.apiMetrics.vend.getAllMetrics(obpQueryParams))
          } yield {
            (JSONFactory510.createMetricsJson(metrics), HttpCode.`200`(callContext))
          }
        }
      }
    }



    staticResourceDocs += ResourceDoc(
      getCustomersForUserIdsOnly,
      implementedInApiVersion,
      nameOf(getCustomersForUserIdsOnly),
      "GET",
      "/users/current/customers/customer_ids",
      "Get Customers for Current User (IDs only)",
      s"""Gets all Customers Ids that are linked to a User.
         |
         |
         |${authenticationRequiredMessage(true)}
         |
         |""",
      EmptyBody,
      customersWithAttributesJsonV300,
      List(
        $UserNotLoggedIn,
        UserCustomerLinksNotFoundForUser,
        UnknownError
      ),
      List(apiTagCustomer, apiTagUser)
    )
    
    lazy val getCustomersForUserIdsOnly : OBPEndpoint = {
      case "users" :: "current" :: "customers" :: "customer_ids" :: Nil JsonGet _ => {
        cc => {
          implicit val ec = EndpointContext(Some(cc))
          for {
            (customers, callContext) <- Connector.connector.vend.getCustomersByUserId(cc.userId, cc.callContext) map {
              connectorEmptyResponse(_, cc.callContext)
            }
          } yield {
            (JSONFactory510.createCustomersIds(customers), HttpCode.`200`(callContext))
          }
        }
      }
    }


    resourceDocs += ResourceDoc(
      getCustomersByLegalName,
      implementedInApiVersion,
      nameOf(getCustomersByLegalName),
      "POST",
      "/banks/BANK_ID/customers/legal-name",
      "Get Customers by Legal Name",
      s"""Gets the Customers specified by Legal Name.
         |
         |
         |${authenticationRequiredMessage(true)}
         |
         |""",
      postCustomerLegalNameJsonV510,
      customerJsonV310,
      List(
        UserNotLoggedIn,
        UserCustomerLinksNotFoundForUser,
        UnknownError
      ),
      List(apiTagCustomer, apiTagKyc),
      Some(List(canGetCustomer))
    )

    lazy val getCustomersByLegalName: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "customers" :: "legal-name" :: Nil JsonPost json -> _ => {
        cc =>
          implicit val ec = EndpointContext(Some(cc))
          for {
            (Full(u), callContext) <- authenticatedAccess(cc)
            (bank, callContext) <- NewStyle.function.getBank(bankId, callContext)
            _ <- NewStyle.function.hasEntitlement(bankId.value, u.userId, canGetCustomer, callContext)
            failMsg = s"$InvalidJsonFormat The Json body should be the $PostCustomerLegalNameJsonV510 "
            postedData <- NewStyle.function.tryons(failMsg, 400, callContext) {
              json.extract[PostCustomerLegalNameJsonV510]
            }
            (customer, callContext) <- NewStyle.function.getCustomersByCustomerLegalName(bank.bankId, postedData.legal_name, callContext)
          } yield {
            (JSONFactory300.createCustomersJson(customer), HttpCode.`200`(callContext))
          }
      }
    }
    

    staticResourceDocs += ResourceDoc(
      createAtm,
      implementedInApiVersion,
      nameOf(createAtm),
      "POST",
      "/banks/BANK_ID/atms",
      "Create ATM",
      s"""Create ATM.""",
      postAtmJsonV510,
      atmJsonV510,
      List(
        $UserNotLoggedIn,
        InvalidJsonFormat,
        UnknownError
      ),
      List(apiTagATM),
      Some(List(canCreateAtm, canCreateAtmAtAnyBank))
    )
    lazy val createAtm: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "atms" :: Nil JsonPost json -> _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            atmJsonV510 <- NewStyle.function.tryons(s"$InvalidJsonFormat The Json body should be the ${classOf[AtmJsonV510]}", 400, cc.callContext) {
              val atm = json.extract[PostAtmJsonV510]
              //Make sure the Create contains proper ATM ID
              atm.id.get
              atm
            }
            _ <- Helper.booleanToFuture(s"$InvalidJsonValue BANK_ID has to be the same in the URL and Body", 400, cc.callContext) {
              atmJsonV510.bank_id == bankId.value
            }
            atm <- NewStyle.function.tryons(CouldNotTransformJsonToInternalModel + " Atm", 400, cc.callContext) {
              JSONFactory510.transformToAtmFromV510(atmJsonV510)
            }
            (atm, callContext) <- NewStyle.function.createOrUpdateAtm(atm, cc.callContext)
            (atmAttributes, callContext) <- NewStyle.function.getAtmAttributesByAtm(bankId, atm.atmId, callContext)
          } yield {
            (JSONFactory510.createAtmJsonV510(atm, atmAttributes), HttpCode.`201`(callContext))
          }
      }
    }

    staticResourceDocs += ResourceDoc(
      updateAtm,
      implementedInApiVersion,
      nameOf(updateAtm),
      "PUT",
      "/banks/BANK_ID/atms/ATM_ID",
      "UPDATE ATM",
      s"""Update ATM.""",
      atmJsonV510.copy(id = None, attributes = None),
      atmJsonV510,
      List(
        $UserNotLoggedIn,
        InvalidJsonFormat,
        UnknownError
      ),
      List(apiTagATM),
      Some(List(canUpdateAtm, canUpdateAtmAtAnyBank))
    )
    lazy val updateAtm: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "atms" :: AtmId(atmId) :: Nil JsonPut json -> _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (atm, callContext) <- NewStyle.function.getAtm(bankId, atmId, cc.callContext)
            atmJsonV510 <- NewStyle.function.tryons(s"$InvalidJsonFormat The Json body should be the ${classOf[AtmJsonV510]}", 400, callContext) {
              json.extract[AtmJsonV510]
            }
            _ <- Helper.booleanToFuture(s"$InvalidJsonValue BANK_ID has to be the same in the URL and Body", 400, callContext) {
              atmJsonV510.bank_id == bankId.value
            }
            atm <- NewStyle.function.tryons(CouldNotTransformJsonToInternalModel + " Atm", 400, callContext) {
              JSONFactory510.transformToAtmFromV510(atmJsonV510.copy(id = Some(atmId.value)))
            }
            (atm, callContext) <- NewStyle.function.createOrUpdateAtm(atm, callContext)
            (atmAttributes, callContext) <- NewStyle.function.getAtmAttributesByAtm(bankId, atm.atmId, callContext)
          } yield {
            (JSONFactory510.createAtmJsonV510(atm, atmAttributes), HttpCode.`201`(callContext))
          }
      }
    }

    staticResourceDocs += ResourceDoc(
      getAtms,
      implementedInApiVersion,
      nameOf(getAtms),
      "GET",
      "/banks/BANK_ID/atms",
      "Get Bank ATMS",
      s"""Returns information about ATMs for a single bank specified by BANK_ID including:
         |
         |* Address
         |* Geo Location
         |* License the data under this endpoint is released under
         |
         |Pagination:
         |
         |By default, 100 records are returned.
         |
         |You can use the url query parameters *limit* and *offset* for pagination
         |
         |${authenticationRequiredMessage(!getAtmsIsPublic)}""".stripMargin,
      EmptyBody,
      atmsJsonV510,
      List(
        $BankNotFound,
        UnknownError
      ),
      List(apiTagATM)
    )
    lazy val getAtms: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "atms" :: Nil JsonGet _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          val limit = ObpS.param("limit")
          val offset = ObpS.param("offset")
          for {
            (_, callContext) <- getAtmsIsPublic match {
              case false => authenticatedAccess(cc)
              case true => anonymousAccess(cc)
            }
            _ <- Helper.booleanToFuture(failMsg = s"${InvalidNumber} limit:${limit.getOrElse("")}", cc = callContext) {
              limit match {
                case Full(i) => i.toList.forall(c => Character.isDigit(c) == true)
                case _ => true
              }
            }
            _ <- Helper.booleanToFuture(failMsg = maximumLimitExceeded, cc = callContext) {
              limit match {
                case Full(i) if i.toInt > 10000 => false
                case _ => true
              }
            }
            (atms, callContext) <- NewStyle.function.getAtmsByBankId(bankId, offset, limit, callContext)

            atmAndAttributesTupleList: List[(AtmT, List[AtmAttribute])] <-  Future.sequence(atms.map(
              atm => NewStyle.function.getAtmAttributesByAtm(bankId, atm.atmId, callContext).map(_._1).map(
                attributes =>{
                   (atm-> attributes)
                }
              )))
            
          } yield {
            (JSONFactory510.createAtmsJsonV510(atmAndAttributesTupleList), HttpCode.`200`(callContext))
          }
      }
    }


    resourceDocs += ResourceDoc(
      getAtm,
      implementedInApiVersion,
      nameOf(getAtm),
      "GET",
      "/banks/BANK_ID/atms/ATM_ID",
      "Get Bank ATM",
      s"""Returns information about ATM for a single bank specified by BANK_ID and ATM_ID including:
         |
         |* Address
         |* Geo Location
         |* License the data under this endpoint is released under
         |* ATM Attributes
         |
         |
         |
         |${authenticationRequiredMessage(!getAtmsIsPublic)}""".stripMargin,
      EmptyBody,
      atmJsonV510,
      List(UserNotLoggedIn, BankNotFound, AtmNotFoundByAtmId, UnknownError),
      List(apiTagATM)
    )
    lazy val getAtm: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "atms" :: AtmId(atmId) :: Nil JsonGet req => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (_, callContext) <- getAtmsIsPublic match {
              case false => authenticatedAccess(cc)
              case true => anonymousAccess(cc)
            }
            (_, callContext) <- NewStyle.function.getBank(bankId, callContext)
            (atm, callContext) <- NewStyle.function.getAtm(bankId, atmId, callContext)
            (atmAttributes, callContext) <- NewStyle.function.getAtmAttributesByAtm(bankId, atmId, callContext)
          } yield {
            (JSONFactory510.createAtmJsonV510(atm, atmAttributes), HttpCode.`200`(callContext))
          }
      }
    }

    staticResourceDocs += ResourceDoc(
      deleteAtm,
      implementedInApiVersion,
      nameOf(deleteAtm),
      "DELETE",
      "/banks/BANK_ID/atms/ATM_ID",
      "Delete ATM",
      s"""Delete ATM.
         |
         |This will also delete all its attributes.
         |
         |""".stripMargin,
      EmptyBody,
      EmptyBody,
      List(
        $UserNotLoggedIn,
        UnknownError
      ),
      List(apiTagATM),
      Some(List(canDeleteAtmAtAnyBank, canDeleteAtm))
    )
    lazy val deleteAtm: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "atms" :: AtmId(atmId) :: Nil JsonDelete _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (atm, callContext) <- NewStyle.function.getAtm(bankId, atmId, cc.callContext)
            (deleted, callContext) <- NewStyle.function.deleteAtm(atm, callContext)
            (atmAttributes, callContext) <- NewStyle.function.deleteAtmAttributesByAtmId(atmId, callContext) 
          } yield {
            (Full(deleted && atmAttributes), HttpCode.`204`(callContext))
          }
      }
    }


    staticResourceDocs += ResourceDoc(
      createConsumerDynamicRegistraion,
      implementedInApiVersion,
      nameOf(createConsumerDynamicRegistraion),
      "POST",
      "/dynamic-registration/consumers",
      "Create a Consumer(Dynamic Registration)",
      s"""Create a Consumer (mTLS access).
         |
         | JWT payload:
         |  - minimal
         |    { "description":"Description" }
         |  - full
         |    {
         |     "description": "Description",
         |     "app_name": "Tesobe GmbH",
         |     "app_type": "Sofit",
         |     "developer_email": "marko@tesobe.com",
         |     "redirect_url": "http://localhost:8082"
         |    }
         | Please note that JWT must be signed with the counterpart private key of the public key used to establish mTLS
         |
         |""",
      ConsumerJwtPostJsonV510("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJkZXNjcmlwdGlvbiI6IlRQUCBkZXNjcmlwdGlvbiJ9.c5gPPsyUmnVW774y7h2xyLXg0wdtu25nbU2AvOmyzcWa7JTdCKuuy3CblxueGwqYkQDDQIya1Qny4blyAvh_a1Q28LgzEKBcH7Em9FZXerhkvR9v4FWbCC5AgNLdQ7sR8-rUQdShmJcGDKdVmsZjuO4XhY2Zx0nFnkcvYfsU9bccoAvkKpVJATXzwBqdoEOuFlplnbxsMH1wWbAd3hbcPPWTdvO43xavNZTB5ybgrXVDEYjw8D-98_ZkqxS0vfvhJ4cGefHViaFzp6zXm7msdBpcE__O9rFbdl9Gvup_bsMbrHJioIrmc2d15Yc-tTNTF9J4qjD_lNxMRlx5o2TZEw"),
      consumerJsonV510,
      List(
        InvalidJsonFormat,
        UnknownError
      ),
      List(apiTagDirectory, apiTagConsumer),
      Some(Nil))


    lazy val createConsumerDynamicRegistraion: OBPEndpoint = {
      case "dynamic-registration" :: "consumers" :: Nil JsonPost json -> _ => {
        cc =>
          implicit val ec = EndpointContext(Some(cc))
          for {
            postedJwt <- NewStyle.function.tryons(InvalidJsonFormat, 400, cc.callContext) {
              json.extract[ConsumerJwtPostJsonV510]
            }
            pem = APIUtil.`getPSD2-CERT`(cc.requestHeaders)
            _ <- Helper.booleanToFuture(PostJsonIsNotSigned, 400, cc.callContext) {
              verifyJwt(postedJwt.jwt, pem.getOrElse(""))
            }
            postedJson <- NewStyle.function.tryons(InvalidJsonFormat, 400, cc.callContext) {
              parse(getSignedPayloadAsJson(postedJwt.jwt).getOrElse("{}")).extract[ConsumerPostJsonV510]
            }
            certificateInfo: CertificateInfoJsonV510 <- Future(X509.getCertificateInfo(pem)) map {
              unboxFullOrFail(_, cc.callContext, X509GeneralError)
            }
            _ <- Helper.booleanToFuture(RegulatedEntityNotFoundByCertificate, 400, cc.callContext) {
              MappedRegulatedEntityProvider.getRegulatedEntities()
                .exists(_.entityCertificatePublicKey.replace("""\n""", "") == pem.getOrElse("").replace("""\n""", ""))
            }
            (consumer, callContext) <- createConsumerNewStyle(
              key = Some(Helpers.randomString(40).toLowerCase),
              secret = Some(Helpers.randomString(40).toLowerCase),
              isActive = Some(true),
              name = getCommonName(pem).or(postedJson.app_name) ,
              appType = postedJson.app_type.map(AppType.valueOf).orElse(Some(AppType.valueOf("Confidential"))),
              description = Some(postedJson.description),
              developerEmail = getEmailAddress(pem).or(postedJson.developer_email),
              company = getOrganization(pem),
              redirectURL = postedJson.redirect_url,
              createdByUserId = None,
              clientCertificate = pem,
              logoURL = None,
              cc.callContext
            )
          } yield {
            // Format the data as json
            val json = JSONFactory510.createConsumerJSON(consumer, Some(certificateInfo))
            // Return
            (json, HttpCode.`201`(callContext))
          }
      }
    }

    private def consumerDisabledText() = {
      if(APIUtil.getPropsAsBoolValue("consumers_enabled_by_default", false) == false) {
        "Please note: Your consumer may be disabled as a result of this action."
      } else {
        ""
      }
    }


    staticResourceDocs += ResourceDoc(
      createConsumer,
      implementedInApiVersion,
      nameOf(createConsumer),
      "POST",
      "/management/consumers",
      "Create a Consumer",
      s"""Create a Consumer (Authenticated access).
         |
         |""",
      CreateConsumerRequestJsonV510(
        "Test",
        "Test",
        "Description",
        "some@email.com",
        "company",
        "redirecturl",
        "createdby",
        true,
        new Date(),
        """-----BEGIN CERTIFICATE-----
          |client_certificate_content
          |-----END CERTIFICATE-----""".stripMargin,
        Some("logoUrl")
      ),
      consumerJsonV510,
      List(
        UserNotLoggedIn,
        UserHasMissingRoles,
        InvalidJsonFormat,
        UnknownError
      ),
      List(apiTagConsumer),
      Some(List(canCreateConsumer))
    )
    
    lazy val createConsumer: OBPEndpoint = {
      case "management" :: "consumers" :: Nil JsonPost json -> _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (Full(u), callContext) <- SS.user
            (postedJson, appType)<- NewStyle.function.tryons(InvalidJsonFormat, 400, callContext) {
              val createConsumerRequestJsonV510 = json.extract[CreateConsumerRequestJsonV510]
              val appType = if(createConsumerRequestJsonV510.app_type.equals("Confidential")) AppType.valueOf("Confidential") else AppType.valueOf("Public")
              (createConsumerRequestJsonV510,appType)
            }
            (consumer, callContext) <- createConsumerNewStyle(
              key = Some(Helpers.randomString(40).toLowerCase),
              secret = Some(Helpers.randomString(40).toLowerCase),
              isActive = Some(postedJson.enabled),
              name = Some(postedJson.app_name),
              appType = Some(appType),
              description = Some(postedJson.description),
              developerEmail = Some(postedJson.developer_email),
              company = Some(postedJson.company),
              redirectURL = Some(postedJson.redirect_url),
              createdByUserId = Some(u.userId),
              clientCertificate = Some(postedJson.client_certificate),
              logoURL = postedJson.logo_url,
              callContext
            )
          } yield {
            (JSONFactory510.createConsumerJSON(consumer, None), HttpCode.`201`(callContext))
          }
      }
    }
    
    
    staticResourceDocs += ResourceDoc(
      updateConsumerRedirectURL,
      implementedInApiVersion,
      "updateConsumerRedirectUrl",
      "PUT",
      "/management/consumers/CONSUMER_ID/consumer/redirect_url",
      "Update Consumer RedirectURL",
      s"""Update an existing redirectUrl for a Consumer specified by CONSUMER_ID.
         |
         | ${consumerDisabledText()}
         |
         | CONSUMER_ID can be obtained after you register the application.
         |
         | Or use the endpoint 'Get Consumers' to get it
         |
       """.stripMargin,
      consumerRedirectUrlJSON,
      consumerJSON,
      List(
        UserNotLoggedIn,
        UserHasMissingRoles,
        UnknownError
      ),
      List(apiTagConsumer),
      Some(List(canUpdateConsumerRedirectUrl))
    )

    lazy val updateConsumerRedirectURL: OBPEndpoint = {
      case "management" :: "consumers" :: consumerId :: "consumer" :: "redirect_url" :: Nil JsonPut json -> _ => {
        cc =>
          implicit val ec = EndpointContext(Some(cc))
          for {
            (Full(u), callContext) <- authenticatedAccess(cc)
            _ <- APIUtil.getPropsAsBoolValue("consumers_enabled_by_default", false) match {
              case true => Future(Full(Unit))
              case false => NewStyle.function.hasEntitlement("", u.userId, ApiRole.canUpdateConsumerRedirectUrl, callContext)
            }
            postJson <- NewStyle.function.tryons(InvalidJsonFormat, 400, callContext) {
              json.extract[ConsumerRedirectUrlJSON]
            }
            consumer <- NewStyle.function.getConsumerByConsumerId(consumerId, callContext)
            //only the developer that created the Consumer should be able to edit it
            _ <- Helper.booleanToFuture(UserNoPermissionUpdateConsumer, 400, callContext) {
              consumer.createdByUserId.equals(u.userId)
            }
            //update the redirectURL and isactive (set to false when change redirectUrl) field in consumer table
            updatedConsumer <- NewStyle.function.updateConsumer(
              consumer.id.get, None, None, Some(APIUtil.getPropsAsBoolValue("consumers_enabled_by_default", false)), 
              None, None, None, None, 
              Some(postJson.redirect_url), 
              None, 
              None, 
              callContext
            )
          } yield {
            val json = JSONFactory510.createConsumerJSON(updatedConsumer)
            (json, HttpCode.`200`(callContext))
          }
      }
    }   
    
    staticResourceDocs += ResourceDoc(
      updateConsumerLogoURL,
      implementedInApiVersion,
      nameOf(updateConsumerLogoURL),
      "PUT",
      "/management/consumers/CONSUMER_ID/consumer/logo_url",
      "Update Consumer LogoURL",
      s"""Update an existing logoURL for a Consumer specified by CONSUMER_ID.
         |
         | ${consumerDisabledText()}
         |
         | CONSUMER_ID can be obtained after you register the application.
         |
         | Or use the endpoint 'Get Consumers' to get it
         |
       """.stripMargin,
      consumerLogoUrlJson,
      consumerJsonV510,
      List(
        $UserNotLoggedIn,
        UserHasMissingRoles,
        UnknownError
      ),
      List(apiTagConsumer),
      Some(List(canUpdateConsumerLogoUrl))
    )

    lazy val updateConsumerLogoURL: OBPEndpoint = {
      case "management" :: "consumers" :: consumerId :: "consumer" :: "logo_url" :: Nil JsonPut json -> _ => {
        cc =>
          implicit val ec = EndpointContext(Some(cc))
          for {
            (Full(u), callContext) <- authenticatedAccess(cc)
            postJson <- NewStyle.function.tryons(InvalidJsonFormat, 400, callContext) {
              json.extract[ConsumerLogoUrlJson]
            }
            consumer <- NewStyle.function.getConsumerByConsumerId(consumerId, callContext)
            //only the developer that created the Consumer should be able to edit it
            _ <- Helper.booleanToFuture(UserNoPermissionUpdateConsumer, 400, callContext) {
              consumer.createdByUserId.equals(u.userId)
            }
            updatedConsumer <- NewStyle.function.updateConsumer(
              consumer.id.get, 
              None, 
              None,
              None,
              None, 
              None, 
              None,
              None, 
              None, 
              None,
              logoURL = Some(postJson.logo_url), 
              callContext)
          } yield {
            (JSONFactory510.createConsumerJSON(updatedConsumer), HttpCode.`200`(callContext))
          }
      }
    }
    resourceDocs += ResourceDoc(
      getConsumers,
      implementedInApiVersion,
      nameOf(getConsumers),
      "GET",
      "/management/consumers",
      "Get Consumers",
      s"""Get the all Consumers.
         |
         |${authenticationRequiredMessage(true)}
         |
         |${urlParametersDocument(true, true)}
         |
         |""",
      EmptyBody,
      consumersJsonV510,
      List(
        $UserNotLoggedIn,
        UserHasMissingRoles,
        UnknownError
      ),
      List(apiTagConsumer),
      Some(List(canGetConsumers))
    )


    lazy val getConsumers: OBPEndpoint = {
      case "management" :: "consumers" :: Nil JsonGet _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            httpParams <- NewStyle.function.extractHttpParamsFromUrl(cc.url)
            (obpQueryParams, callContext) <- createQueriesByHttpParamsFuture(httpParams, Some(cc))
            consumers <- Consumers.consumers.vend.getConsumersFuture(obpQueryParams, callContext)
          } yield {
            (JSONFactory510.createConsumersJson(consumers), HttpCode.`200`(callContext))
          }
      }
    }

    staticResourceDocs += ResourceDoc(
      grantUserAccessToViewById,
      implementedInApiVersion,
      nameOf(grantUserAccessToViewById),
      "POST",
      "/banks/BANK_ID/accounts/ACCOUNT_ID/views/VIEW_ID/account-access/grant",
      "Grant User access to View",
      s"""Grants the User identified by USER_ID access to the view identified.
         |
         |${authenticationRequiredMessage(true)} and the user needs to be account holder.
         |
         |""",
      postAccountAccessJsonV510,
      viewJsonV300,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        $BankAccountNotFound,
        $UserNoPermissionAccessView,
        UserLacksPermissionCanGrantAccessToSystemViewForTargetAccount,
        UserLacksPermissionCanGrantAccessToCustomViewForTargetAccount,
        InvalidJsonFormat,
        UserNotFoundById,
        SystemViewNotFound,
        ViewNotFound,
        CannotGrantAccountAccess,
        UnknownError
      ),
      List(apiTagAccountAccess, apiTagView, apiTagAccount, apiTagUser, apiTagOwnerRequired))

    lazy val grantUserAccessToViewById: OBPEndpoint = {
      //add access for specific user to a specific system view
      case "banks" :: BankId(bankId) :: "accounts" :: AccountId(accountId) ::"views":: ViewId(viewId):: "account-access" :: "grant" :: Nil JsonPost json -> _ => {
        cc =>
          implicit val ec = EndpointContext(Some(cc))
          val failMsg = s"$InvalidJsonFormat The Json body should be the $PostAccountAccessJsonV510 "
          for {
            (Full(u), callContext) <- SS.user
            postJson <- NewStyle.function.tryons(failMsg, 400, cc.callContext) {
              json.extract[PostAccountAccessJsonV510]
            }
            targetViewId = ViewId(postJson.view_id)
            msg = getUserLacksGrantPermissionErrorMessage(viewId, targetViewId)
            _ <- Helper.booleanToFuture(msg, 403, cc = cc.callContext) {
              APIUtil.canGrantAccessToView(BankIdAccountIdViewId(bankId,accountId,viewId),targetViewId, u, callContext)
            }
            (user, callContext) <- NewStyle.function.findByUserId(postJson.user_id, callContext)
            view <- isValidSystemViewId(targetViewId.value) match {
              case true => NewStyle.function.systemView(targetViewId, callContext)
              case false => NewStyle.function.customView(targetViewId, BankIdAccountId(bankId, accountId), callContext)
            }
            addedView <- JSONFactory400.grantAccountAccessToUser(bankId, accountId, user, view, callContext)
            
          } yield {
            val viewJson = JSONFactory300.createViewJSON(addedView)
            (viewJson, HttpCode.`201`(callContext))
          }
      }
    }


    staticResourceDocs += ResourceDoc(
      revokeUserAccessToViewById,
      implementedInApiVersion,
      nameOf(revokeUserAccessToViewById),
      "POST",
      "/banks/BANK_ID/accounts/ACCOUNT_ID/views/VIEW_ID/account-access/revoke",
      "Revoke User access to View",
      s"""Revoke the User identified by USER_ID access to the view identified.
         |
         |${authenticationRequiredMessage(true)}.
         |
         |""",
      postAccountAccessJsonV510,
      revokedJsonV400,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        $BankAccountNotFound,
        $UserNoPermissionAccessView,
        UserLacksPermissionCanRevokeAccessToCustomViewForTargetAccount,
        UserLacksPermissionCanRevokeAccessToSystemViewForTargetAccount,
        InvalidJsonFormat,
        UserNotFoundById,
        SystemViewNotFound,
        ViewNotFound,
        CannotRevokeAccountAccess,
        CannotFindAccountAccess,
        UnknownError
      ),
      List(apiTagAccountAccess, apiTagView, apiTagAccount, apiTagUser, apiTagOwnerRequired))

    lazy val revokeUserAccessToViewById: OBPEndpoint = {
      //add access for specific user to a specific system view
      case "banks" :: BankId(bankId) :: "accounts" :: AccountId(accountId) :: "views" ::ViewId(viewId) :: "account-access" :: "revoke" :: Nil JsonPost json -> _ => {
        cc =>
          implicit val ec = EndpointContext(Some(cc))
          val failMsg = s"$InvalidJsonFormat The Json body should be the $PostAccountAccessJsonV400 "
          for {
            (Full(u), callContext) <- SS.user
            postJson <- NewStyle.function.tryons(failMsg, 400, cc.callContext) {
              json.extract[PostAccountAccessJsonV510]
            }
            targetViewId = ViewId(postJson.view_id)
          
            msg = getUserLacksRevokePermissionErrorMessage(viewId, targetViewId)
            
            _ <- Helper.booleanToFuture(msg, 403, cc = cc.callContext) {
              APIUtil.canRevokeAccessToView(BankIdAccountIdViewId(bankId, accountId, viewId),targetViewId, u, callContext)
            }
            (user, callContext) <- NewStyle.function.findByUserId(postJson.user_id, cc.callContext)
            view <- isValidSystemViewId(targetViewId.value) match {
              case true => NewStyle.function.systemView(targetViewId, callContext)
              case false => NewStyle.function.customView(targetViewId, BankIdAccountId(bankId, accountId), callContext)
            }
            revoked <- isValidSystemViewId(targetViewId.value) match {
              case true => NewStyle.function.revokeAccessToSystemView(bankId, accountId, view, user, callContext)
              case false => NewStyle.function.revokeAccessToCustomView(view, user, callContext)
            }
          } yield {
            (RevokedJsonV400(revoked), HttpCode.`201`(callContext))
          }
      }
    }

    staticResourceDocs += ResourceDoc(
      createUserWithAccountAccessById,
      implementedInApiVersion,
      nameOf(createUserWithAccountAccessById),
      "POST",
      "/banks/BANK_ID/accounts/ACCOUNT_ID/views/VIEW_ID/user-account-access",
      "Create (DAuth) User with Account Access",
      s"""This endpoint is used as part of the DAuth solution to grant access to account and transaction data to a smart contract on the blockchain.
         |
         |Put the smart contract address in username
         |
         |For provider use "dauth"
         |
         |This endpoint will create the (DAuth) User with username and provider if the User does not already exist.
         |
         |${authenticationRequiredMessage(true)} and the logged in user needs to be account holder.
         |
         |For information about DAuth see below:
         |
         |${Glossary.getGlossaryItem("DAuth")}
         |
         |""",
      postCreateUserAccountAccessJsonV400,
      List(viewJsonV300),
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        $BankAccountNotFound,
        $UserNoPermissionAccessView,
        UserLacksPermissionCanGrantAccessToSystemViewForTargetAccount,
        UserLacksPermissionCanGrantAccessToCustomViewForTargetAccount,
        InvalidJsonFormat,
        SystemViewNotFound,
        ViewNotFound,
        CannotGrantAccountAccess,
        UnknownError
      ),
      List(apiTagAccountAccess, apiTagView, apiTagAccount, apiTagUser, apiTagOwnerRequired, apiTagDAuth))

    lazy val createUserWithAccountAccessById: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "accounts" :: AccountId(accountId) :: "views" ::ViewId(viewId) :: "user-account-access" :: Nil JsonPost json -> _ => {
        cc =>
          implicit val ec = EndpointContext(Some(cc))
          val failMsg = s"$InvalidJsonFormat The Json body should be the $PostCreateUserAccountAccessJsonV510 "
          for {
            (Full(u), callContext) <- SS.user
            postJson <- NewStyle.function.tryons(failMsg, 400, cc.callContext) {
              json.extract[PostCreateUserAccountAccessJsonV510]
            }
            //provider must start with dauth., can not create other provider users.
            _ <- Helper.booleanToFuture(s"$InvalidUserProvider The user.provider must be start with 'dauth.'", cc = Some(cc)) {
              postJson.provider.startsWith("dauth.")
            }
            targetViewId = ViewId(postJson.view_id)
            msg = getUserLacksGrantPermissionErrorMessage(viewId, targetViewId)
            
            _ <- Helper.booleanToFuture(msg, 403, cc = Some(cc)) {
              APIUtil.canGrantAccessToView(BankIdAccountIdViewId(bankId, accountId, viewId) ,targetViewId, u, callContext)
            }
            (targetUser, callContext) <- NewStyle.function.getOrCreateResourceUser(postJson.provider, postJson.username, cc.callContext)
            view <- isValidSystemViewId(targetViewId.value) match {
              case true => NewStyle.function.systemView(targetViewId, callContext)
              case false => NewStyle.function.customView(targetViewId, BankIdAccountId(bankId, accountId), callContext)
            }
            addedView <- isValidSystemViewId(targetViewId.value) match {
              case true => NewStyle.function.grantAccessToSystemView(bankId, accountId, view, targetUser, callContext)
              case false => NewStyle.function.grantAccessToCustomView(view, targetUser, callContext)
            }
          } yield {
            val viewsJson = JSONFactory300.createViewJSON(addedView)
            (viewsJson, HttpCode.`201`(callContext))
          }
      }
    }

    resourceDocs += ResourceDoc(
      getTransactionRequests,
      implementedInApiVersion,
      nameOf(getTransactionRequests),
      "GET",
      "/banks/BANK_ID/accounts/ACCOUNT_ID/VIEW_ID/transaction-requests",
      "Get Transaction Requests." ,
      """Returns transaction requests for account specified by ACCOUNT_ID at bank specified by BANK_ID.
        |
        |The VIEW_ID specified must be 'owner' and the user must have access to this view.
        |
        |Version 2.0.0 now returns charge information.
        |
        |Transaction Requests serve to initiate transactions that may or may not proceed. They contain information including:
        |
        |* Transaction Request Id
        |* Type
        |* Status (INITIATED, COMPLETED)
        |* Challenge (in order to confirm the request)
        |* From Bank / Account
        |* Details including Currency, Value, Description and other initiation information specific to each type. (Could potentialy include a list of future transactions.)
        |* Related Transactions
        |
        |PSD2 Context: PSD2 requires transparency of charges to the customer.
        |This endpoint provides the charge that would be applied if the Transaction Request proceeds - and a record of that charge there after.
        |The customer can proceed with the Transaction by answering the security challenge.
        |
        |We support query transaction request by attribute 
        |URL params example:/banks/BANK_ID/accounts/ACCOUNT_ID/VIEW_ID/transaction-requests?invoiceNumber=123&referenceNumber=456
        |
      """.stripMargin,
      EmptyBody,
      transactionRequestWithChargeJSONs210,
      List(
        UserNotLoggedIn,
        BankNotFound,
        BankAccountNotFound,
        UserNoPermissionAccessView,
        ViewDoesNotPermitAccess,
        GetTransactionRequestsException,
        UnknownError
      ),
      List(apiTagTransactionRequest, apiTagPSD2PIS))

    lazy val getTransactionRequests: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "accounts" :: AccountId(accountId) :: ViewId(viewId) :: "transaction-requests" :: Nil JsonGet req => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (Full(u), callContext) <- authenticatedAccess(cc)
            _ <- NewStyle.function.isEnabledTransactionRequests(callContext)
            (_, callContext) <- NewStyle.function.getBank(bankId, callContext)
            (fromAccount, callContext) <- NewStyle.function.checkBankAccountExists(bankId, accountId, callContext)
            view <- NewStyle.function.checkAccountAccessAndGetView(viewId, BankIdAccountId(bankId, accountId), Full(u), callContext)
            _ <- Helper.booleanToFuture(
              s"${ErrorMessages.ViewDoesNotPermitAccess} You need the `${StringHelpers.snakify(nameOf(ViewDefinition.canSeeTransactionRequests_)).dropRight(1)}` permission on the View(${viewId.value})",
              cc=callContext){
              view.canSeeTransactionRequests
            }
            (transactionRequests, callContext) <- Future(Connector.connector.vend.getTransactionRequests210(u, fromAccount, callContext)) map {
              unboxFullOrFail(_, callContext, GetTransactionRequestsException)
            }
            (transactionRequestAttributes, callContext) <- NewStyle.function.getByAttributeNameValues(bankId, req.params, true, callContext) 
            transactionRequestIds = transactionRequestAttributes.map(_.transactionRequestId) 
            
            transactionRequestsFiltered = if(req.params.isEmpty)
              transactionRequests
            else
              transactionRequests.filter(transactionRequest => transactionRequestIds.contains(transactionRequest.id)) 
              
          } yield {
            val json = JSONFactory510.createTransactionRequestJSONs(transactionRequestsFiltered, transactionRequestAttributes)
            
            (json, HttpCode.`200`(callContext))
          }
      }
    }
    
    staticResourceDocs += ResourceDoc(
      getAccountAccessByUserId,
      implementedInApiVersion,
      nameOf(getAccountAccessByUserId),
      "GET",
      "/users/USER_ID/account-access",
      "Get Account Access by USER_ID",
      s"""Get Account Access by USER_ID
         |
         |${authenticationRequiredMessage(true)}
         |
         |""",
      EmptyBody,
      accountsMinimalJson400,
      List(
        $UserNotLoggedIn,
        UserNotFoundByUserId,
        UnknownError
      ),
      List(apiTagAccount),
      Some(List(canSeeAccountAccessForAnyUser)))

    lazy val getAccountAccessByUserId : OBPEndpoint = {
      case "users" :: userId :: "account-access" :: Nil JsonGet _ =>
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (user, callContext) <- NewStyle.function.getUserByUserId(userId, cc.callContext)
            (_, accountAccess) <- Future(Views.views.vend.privateViewsUserCanAccess(user))
          } yield {
            (JSONFactory400.createAccountsMinimalJson400(accountAccess), HttpCode.`200`(callContext))
          }
    }
    
    
    staticResourceDocs += ResourceDoc(
      getApiTags,
      implementedInApiVersion,
      nameOf(getApiTags),
      "GET",
      "/tags",
      "Get API Tags",
      s"""Get API TagsGet API Tags
         |
         |${authenticationRequiredMessage(false)}
         |
         |""",
      EmptyBody,
      accountsMinimalJson400,
      List(
        UnknownError
      ),
      List(apiTagApi))

    lazy val getApiTags : OBPEndpoint = {
      case "tags" ::  Nil JsonGet _ =>
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            _ <- Future.successful() // Just start async call
          } yield {
            (APITags(ApiTag.allDisplayTagNames.toList), HttpCode.`200`(cc.callContext))
          }
    }



    staticResourceDocs += ResourceDoc(
      getCoreAccountByIdThroughView,
      implementedInApiVersion,
      nameOf(getCoreAccountByIdThroughView),
      "GET",
      "/banks/BANK_ID/accounts/ACCOUNT_ID/views/VIEW_ID",
      "Get Account by Id (Core) through the VIEW_ID",
      s"""Information returned about the account through VIEW_ID :
         |""".stripMargin,
      EmptyBody,
      moderatedCoreAccountJsonV400,
      List($UserNotLoggedIn, $BankAccountNotFound,UnknownError),
      apiTagAccount :: apiTagPSD2AIS :: apiTagPsd2  :: Nil
    )
    lazy val getCoreAccountByIdThroughView : OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "accounts" :: AccountId(accountId) :: "views" :: ViewId(viewId) :: Nil JsonGet req => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (user @Full(u), account, callContext) <- SS.userAccount
            bankIdAccountId = BankIdAccountId(account.bankId, account.accountId)
            view <- NewStyle.function.checkViewAccessAndReturnView(viewId , bankIdAccountId, user, callContext)
            moderatedAccount <- NewStyle.function.moderatedBankAccountCore(account, view, user, callContext)
          } yield {
            val availableViews: List[View] = Views.views.vend.privateViewsUserCanAccessForAccount(u, BankIdAccountId(account.bankId, account.accountId))
            (createNewCoreBankAccountJson(moderatedAccount, availableViews), HttpCode.`200`(callContext))
          }
      }
    }

    staticResourceDocs += ResourceDoc(
      getBankAccountBalances,
      implementedInApiVersion,
      nameOf(getBankAccountBalances),
      "GET",
      "/banks/BANK_ID/accounts/ACCOUNT_ID/views/VIEW_ID/balances",
      "Get Account Balances by BANK_ID and ACCOUNT_ID through the VIEW_ID",
      """Get the Balances for the Account specified by BANK_ID and ACCOUNT_ID through the VIEW_ID.""",
      EmptyBody,
      accountBalanceV400,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        $BankAccountNotFound,
        UserNoPermissionAccessView,
        UnknownError
      ),
      apiTagAccount :: apiTagPSD2AIS :: apiTagPsd2  :: Nil
    )

    lazy val getBankAccountBalances : OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "accounts" :: AccountId(accountId)  :: "views" :: ViewId(viewId) :: "balances" :: Nil JsonGet _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (Full(u), callContext) <- SS.user
            bankIdAccountId = BankIdAccountId(bankId, accountId)
            view <- NewStyle.function.checkViewAccessAndReturnView(viewId, bankIdAccountId, Full(u), callContext)
            // Note we do one explicit check here rather than use moderated account because this provides an explicit message
            failMsg = ViewDoesNotPermitAccess + s" You need the `${StringHelpers.snakify(nameOf(view.canSeeBankAccountBalance))}` permission on VIEW_ID(${viewId.value})"
            _ <- Helper.booleanToFuture(failMsg, 403, cc = callContext) {
              view.canSeeBankAccountBalance
            }
            (accountBalances, callContext) <- BalanceNewStyle.getBankAccountBalances(bankIdAccountId, callContext)
          } yield {
            (createAccountBalancesJson(accountBalances), HttpCode.`200`(callContext))
          }
      }
    }

    staticResourceDocs += ResourceDoc(
      getBankAccountsBalances,
      implementedInApiVersion,
      nameOf(getBankAccountsBalances),
      "GET",
      "/banks/BANK_ID/balances",
      "Get Account Balances by BANK_ID",
      """Get the Balances for the Account specified by BANK_ID.""",
      EmptyBody,
      accountBalancesV400Json,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        UnknownError
      ),
      apiTagAccount :: apiTagPSD2AIS :: apiTagPsd2  :: Nil
    )

    lazy val getBankAccountsBalances : OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "balances" :: Nil JsonGet _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (Full(u), callContext) <- SS.user
            (allowedAccounts, callContext) <- BalanceNewStyle.getAccountAccessAtBank(u, bankId, callContext)
            (accountsBalances, callContext) <- BalanceNewStyle.getBankAccountsBalances(allowedAccounts, callContext)
          } yield {
            (createBalancesJson(accountsBalances), HttpCode.`200`(callContext))
          }
      }
    }

    staticResourceDocs += ResourceDoc(
      getBankAccountsBalancesThroughView,
      implementedInApiVersion,
      nameOf(getBankAccountsBalancesThroughView),
      "GET",
      "/banks/BANK_ID/views/VIEW_ID/balances",
      "Get Account Balances by BANK_ID",
      """Get the Balances for the Account specified by BANK_ID.""",
      EmptyBody,
      accountBalancesV400Json,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        UnknownError
      ),
      apiTagAccount :: apiTagPSD2AIS :: apiTagPsd2  :: Nil
    )

    lazy val getBankAccountsBalancesThroughView : OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "views" :: ViewId(viewId) :: "balances" :: Nil JsonGet _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (Full(u), callContext) <- SS.user
            (allowedAccounts, callContext) <- BalanceNewStyle.getAccountAccessAtBankThroughView(u, bankId, viewId, callContext)
            (accountsBalances, callContext) <- BalanceNewStyle.getBankAccountsBalances(allowedAccounts, callContext)
          } yield {
            (createBalancesJson(accountsBalances), HttpCode.`200`(callContext))
          }
      }
    }


    lazy val counterPartyLimitIntro: String =
      """Counter Party Limits can be used to restrict the Transaction Request amounts and frequencies (per month and year) that can be made to a Counterparty (Beneficiary).
        |
        |In order to implement VRP (Variable Recurring Payments) perform the following steps:
        |1) Create a Custom View named e.g. VRP1.
        |2) Place a Beneficiary Counterparty on that view.
        |3) Add Counterparty Limits for that Counterparty.
        |4) Generate a Consent containing the bank, account and view (e.g. VRP1)
        |5) Let the App use the consent to trigger Transaction Requests.
        |""".stripMargin

    staticResourceDocs += ResourceDoc(
      createCounterpartyLimit,
      implementedInApiVersion,
      nameOf(createCounterpartyLimit),
      "POST",
      "/banks/BANK_ID/accounts/ACCOUNT_ID/views/VIEW_ID/counterparties/COUNTERPARTY_ID/limits",
      "Create Counterparty Limit",
      s"""Create limits (for single or recurring payments) for a counterparty specified by the COUNTERPARTY_ID.
         |
         |Using this endpoint, we can attach a limit record to a Counterparty referenced by its counterparty_id (a UUID).
         |
         |For more information on Counterparty Limits, see ${Glossary.getGlossaryItemLink("Counterparty-Limits")}
         |
         |For an introduction to Counterparties in OBP, see ${Glossary.getGlossaryItemLink("Counterparties")}
         |
         |You can automate the process of creating counterparty limits and consents for VRP with this ${Glossary.getApiExplorerLink("endpoint", "OBPv5.1.0-createVRPConsentRequest")}.
         |
         |
         |
         |""".stripMargin,
      postCounterpartyLimitV510,
      counterpartyLimitV510,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        $BankAccountNotFound,
        $UserNoPermissionAccessView,
        $CounterpartyNotFoundByCounterpartyId,
        InvalidJsonFormat,
        UnknownError
      ),
      List(apiTagCounterpartyLimits),
    )
    lazy val createCounterpartyLimit: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "accounts" :: AccountId(accountId) :: "views" :: ViewId(viewId) ::"counterparties" :: CounterpartyId(counterpartyId) ::"limits" :: Nil JsonPost json -> _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            postCounterpartyLimitV510 <- NewStyle.function.tryons(s"$InvalidJsonFormat The Json body should be the ${classOf[PostCounterpartyLimitV510]}", 400, cc.callContext) {
              json.extract[PostCounterpartyLimitV510]
            }
            _ <- Helper.booleanToFuture(s"${InvalidISOCurrencyCode} Current input is: '${postCounterpartyLimitV510.currency}'", cc=cc.callContext) {
              isValidCurrencyISOCode(postCounterpartyLimitV510.currency)
            }
            (counterpartyLimitBox, callContext) <- Connector.connector.vend.getCounterpartyLimit(
              bankId.value,
              accountId.value,
              viewId.value,
              counterpartyId.value,
              cc.callContext
            )
            failMsg = s"$CounterpartyLimitAlreadyExists Current BANK_ID($bankId), ACCOUNT_ID($accountId), VIEW_ID($viewId),COUNTERPARTY_ID($counterpartyId)"
            _ <- Helper.booleanToFuture(failMsg, cc = callContext) {
              counterpartyLimitBox.isEmpty
            }
            (counterpartyLimit,callContext) <- NewStyle.function.createOrUpdateCounterpartyLimit(
              bankId.value,
              accountId.value,
              viewId.value,
              counterpartyId.value,
              postCounterpartyLimitV510.currency,
              BigDecimal(postCounterpartyLimitV510.max_single_amount),
              BigDecimal(postCounterpartyLimitV510.max_monthly_amount),
              postCounterpartyLimitV510.max_number_of_monthly_transactions,
              BigDecimal(postCounterpartyLimitV510.max_yearly_amount),
              postCounterpartyLimitV510.max_number_of_yearly_transactions,
              BigDecimal(postCounterpartyLimitV510.max_total_amount),
              postCounterpartyLimitV510.max_number_of_transactions,
              cc.callContext
            )
          } yield {
            (counterpartyLimit.toJValue, HttpCode.`201`(callContext))
          }
      }
    }
    
    staticResourceDocs += ResourceDoc(
      updateCounterpartyLimit,
      implementedInApiVersion,
      nameOf(updateCounterpartyLimit),
      "PUT",
      "/banks/BANK_ID/accounts/ACCOUNT_ID/views/VIEW_ID/counterparties/COUNTERPARTY_ID/limits",
      "Update Counterparty Limit",
      s"""Update Counterparty Limit.""",
      postCounterpartyLimitV510,
      counterpartyLimitV510,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        $BankAccountNotFound,
        $UserNoPermissionAccessView,
        $CounterpartyNotFoundByCounterpartyId,
        InvalidJsonFormat,
        UnknownError
      ),
      List(apiTagCounterpartyLimits),
    )
    lazy val updateCounterpartyLimit: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "accounts" :: AccountId(accountId) :: "views" :: ViewId(viewId) ::"counterparties" :: CounterpartyId(counterpartyId) ::"limits" :: Nil JsonPut json -> _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            postCounterpartyLimitV510 <- NewStyle.function.tryons(s"$InvalidJsonFormat The Json body should be the ${classOf[PostCounterpartyLimitV510]}", 400, cc.callContext) {
              json.extract[PostCounterpartyLimitV510]
            }
            _ <- Helper.booleanToFuture(s"${InvalidISOCurrencyCode} Current input is: '${postCounterpartyLimitV510.currency}'", cc=cc.callContext) {
              isValidCurrencyISOCode(postCounterpartyLimitV510.currency)
            }
            (counterpartyLimit,callContext) <- NewStyle.function.createOrUpdateCounterpartyLimit(
              bankId.value,
              accountId.value,
              viewId.value,
              counterpartyId.value,
              postCounterpartyLimitV510.currency,
              BigDecimal(postCounterpartyLimitV510.max_single_amount),
              BigDecimal(postCounterpartyLimitV510.max_monthly_amount),
              postCounterpartyLimitV510.max_number_of_monthly_transactions,
              BigDecimal(postCounterpartyLimitV510.max_yearly_amount),
              postCounterpartyLimitV510.max_number_of_yearly_transactions,
              BigDecimal(postCounterpartyLimitV510.max_total_amount),
              postCounterpartyLimitV510.max_number_of_transactions,
              cc.callContext
            )
          } yield {
            (counterpartyLimit.toJValue, HttpCode.`200`(callContext))
          }
      }
    }

    staticResourceDocs += ResourceDoc(
      getCounterpartyLimit,
      implementedInApiVersion,
      nameOf(getCounterpartyLimit),
      "GET",
      "/banks/BANK_ID/accounts/ACCOUNT_ID/views/VIEW_ID/counterparties/COUNTERPARTY_ID/limits",
      "Get Counterparty Limit",
      s"""Get Counterparty Limit.""",
      EmptyBody,
      counterpartyLimitV510,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        $BankAccountNotFound,
        $UserNoPermissionAccessView,
        $CounterpartyNotFoundByCounterpartyId,
        InvalidJsonFormat,
        UnknownError
      ),
      List(apiTagCounterpartyLimits),
    )
    lazy val getCounterpartyLimit: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "accounts" :: AccountId(accountId) :: "views" :: ViewId(viewId) ::"counterparties" :: CounterpartyId(counterpartyId) ::"limits" :: Nil JsonGet _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (counterpartyLimit, callContext) <- NewStyle.function.getCounterpartyLimit(
              bankId.value,
              accountId.value,
              viewId.value,
              counterpartyId.value,
              cc.callContext
            )
          } yield {
            (counterpartyLimit.toJValue, HttpCode.`200`(callContext))
          }
      }
    }


    staticResourceDocs += ResourceDoc(
      getCounterpartyLimitStatus,
      implementedInApiVersion,
      nameOf(getCounterpartyLimitStatus),
      "GET",
      "/banks/BANK_ID/accounts/ACCOUNT_ID/views/VIEW_ID/counterparties/COUNTERPARTY_ID/limit-status",
      "Get Counterparty Limit Status",
      s"""Get Counterparty Limit Status.""",
      EmptyBody,
      counterpartyLimitStatusV510,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        $BankAccountNotFound,
        $UserNoPermissionAccessView,
        $CounterpartyNotFoundByCounterpartyId,
        InvalidJsonFormat,
        UnknownError
      ),
      List(apiTagCounterpartyLimits),
    )
    lazy val getCounterpartyLimitStatus: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "accounts" :: AccountId(accountId) :: "views" :: ViewId(viewId) ::"counterparties" :: CounterpartyId(counterpartyId) ::"limit-status" :: Nil JsonGet _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (counterpartyLimit, callContext) <- NewStyle.function.getCounterpartyLimit(
              bankId.value,
              accountId.value,
              viewId.value,
              counterpartyId.value,
              cc.callContext
            )
            // Get the first day of the current month
            firstDayOfMonth: LocalDate = LocalDate.now().withDayOfMonth(1)

            // Get the last day of the current month
            lastDayOfMonth: LocalDate = LocalDate.now().withDayOfMonth(
              LocalDate.now().lengthOfMonth()
            )
            // Get the first day of the current year
            firstDayOfYear: LocalDate = LocalDate.now().withDayOfYear(1)

            // Get the last day of the current year
            lastDayOfYear: LocalDate = LocalDate.now().withDayOfYear(
              LocalDate.now().lengthOfYear()
            )

            (fromBankAccount, callContext) <- NewStyle.function.getBankAccount(bankId, accountId, callContext)
            // Convert LocalDate to Date
            zoneId: ZoneId = ZoneId.systemDefault()
            firstCurrentMonthDate: Date = Date.from(firstDayOfMonth.atStartOfDay(zoneId).toInstant)
            lastCurrentMonthDate: Date = Date.from(lastDayOfMonth.atStartOfDay(zoneId).toInstant)

            firstCurrentYearDate: Date = Date.from(firstDayOfYear.atStartOfDay(zoneId).toInstant)
            lastCurrentYearDate: Date = Date.from(lastDayOfYear.atStartOfDay(zoneId).toInstant)

            defaultFromDate: Date = theEpochTime
            defaultToDate: Date = APIUtil.ToDateInFuture

            (sumOfTransactionsFromAccountToCounterpartyMonthly, callContext) <- NewStyle.function.getSumOfTransactionsFromAccountToCounterparty(
              bankId,
              accountId,
              counterpartyId,
              firstCurrentMonthDate: Date,
              lastCurrentMonthDate: Date,
              callContext: Option[CallContext]
            )

            (countOfTransactionsFromAccountToCounterpartyMonthly, callContext) <- NewStyle.function.getCountOfTransactionsFromAccountToCounterparty(
              bankId,
              accountId,
              counterpartyId,
              firstCurrentMonthDate: Date,
              lastCurrentMonthDate: Date,
              callContext: Option[CallContext]
            )

            (sumOfTransactionsFromAccountToCounterpartyYearly, callContext) <- NewStyle.function.getSumOfTransactionsFromAccountToCounterparty(
              bankId,
              accountId,
              counterpartyId,
              firstCurrentYearDate: Date,
              lastCurrentYearDate: Date,
              callContext: Option[CallContext]
            )

            (countOfTransactionsFromAccountToCounterpartyYearly, callContext) <- NewStyle.function.getCountOfTransactionsFromAccountToCounterparty(
              bankId,
              accountId,
              counterpartyId,
              firstCurrentYearDate: Date,
              lastCurrentYearDate: Date,
              callContext: Option[CallContext]
            )

            (sumOfAllTransactionsFromAccountToCounterparty, callContext) <- NewStyle.function.getSumOfTransactionsFromAccountToCounterparty(
              bankId,
              accountId,
              counterpartyId,
              defaultFromDate: Date,
              defaultToDate: Date,
              callContext: Option[CallContext]
            )

            (countOfAllTransactionsFromAccountToCounterparty, callContext) <- NewStyle.function.getCountOfTransactionsFromAccountToCounterparty(
              bankId,
              accountId,
              counterpartyId,
              defaultFromDate: Date,
              defaultToDate: Date,
              callContext: Option[CallContext]
            )
            
          } yield {
            (CounterpartyLimitStatusV510(
              counterparty_limit_id = counterpartyLimit.counterpartyLimitId: String,
              bank_id = counterpartyLimit.bankId: String,
              account_id = counterpartyLimit.accountId: String,
              view_id = counterpartyLimit.viewId: String,
              counterparty_id = counterpartyLimit.counterpartyId: String,
              currency = counterpartyLimit.currency: String,
              max_single_amount = counterpartyLimit.maxSingleAmount.toString(),
              max_monthly_amount = counterpartyLimit.maxMonthlyAmount.toString(),
              max_number_of_monthly_transactions = counterpartyLimit.maxNumberOfMonthlyTransactions: Int,
              max_yearly_amount = counterpartyLimit.maxYearlyAmount.toString(),
              max_number_of_yearly_transactions = counterpartyLimit.maxNumberOfYearlyTransactions: Int,
              max_total_amount = counterpartyLimit.maxTotalAmount.toString(),
              max_number_of_transactions = counterpartyLimit.maxNumberOfTransactions: Int,
              status = CounterpartyLimitStatus(
                currency_status = fromBankAccount.currency,
                max_monthly_amount_status = sumOfTransactionsFromAccountToCounterpartyMonthly.amount,
                max_number_of_monthly_transactions_status = countOfTransactionsFromAccountToCounterpartyMonthly,
                max_yearly_amount_status = sumOfTransactionsFromAccountToCounterpartyYearly.amount,
                max_number_of_yearly_transactions_status = countOfTransactionsFromAccountToCounterpartyYearly,
                max_total_amount_status = sumOfAllTransactionsFromAccountToCounterparty.amount,
                max_number_of_transactions_status = countOfAllTransactionsFromAccountToCounterparty
              )
            ), HttpCode.`200`(callContext))
          }
      }
    }

    staticResourceDocs += ResourceDoc(
      deleteCounterpartyLimit,
      implementedInApiVersion,
      nameOf(deleteCounterpartyLimit),
      "DELETE",
      "/banks/BANK_ID/accounts/ACCOUNT_ID/views/VIEW_ID/counterparties/COUNTERPARTY_ID/limits",
      "Delete Counterparty Limit",
      s"""Delete Counterparty Limit.""",
      EmptyBody,
      EmptyBody,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        $BankAccountNotFound,
        $UserNoPermissionAccessView,
        $CounterpartyNotFoundByCounterpartyId,
        InvalidJsonFormat,
        UnknownError
      ),
      List(apiTagCounterpartyLimits),
    )
    lazy val deleteCounterpartyLimit: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "accounts" :: AccountId(accountId) :: "views" :: ViewId(viewId) ::"counterparties" :: CounterpartyId(counterpartyId) ::"limits" :: Nil JsonDelete _ => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (counterpartyLimit, callContext)<- NewStyle.function.deleteCounterpartyLimit(
              bankId.value,
              accountId.value,
              viewId.value,
              counterpartyId.value,
              cc.callContext
            )
          } yield {
            (Full(counterpartyLimit), HttpCode.`204`(cc.callContext))
          }
      }
    }

    resourceDocs += ResourceDoc(
      createCustomView,
      implementedInApiVersion,
      nameOf(createCustomView),
      "POST",
      "/banks/BANK_ID/accounts/ACCOUNT_ID/views/VIEW_ID/target-views",
      "Create Custom View",
      s"""Create a custom view on bank account
         |
         | ${authenticationRequiredMessage(true)} and the user needs to have access to the owner view.
         | The 'alias' field in the JSON can take one of three values:
         |
         | * _public_: to use the public alias if there is one specified for the other account.
         | * _private_: to use the private alias if there is one specified for the other account.
         |
         | * _''(empty string)_: to use no alias; the view shows the real name of the other account.
         |
         | The 'hide_metadata_if_alias_used' field in the JSON can take boolean values. If it is set to `true` and there is an alias on the other account then the other accounts' metadata (like more_info, url, image_url, open_corporates_url, etc.) will be hidden. Otherwise the metadata will be shown.
         |
         | The 'allowed_actions' field is a list containing the name of the actions allowed on this view, all the actions contained will be set to `true` on the view creation, the rest will be set to `false`.
         |
         | You MUST use a leading _ (underscore) in the view name because other view names are reserved for OBP [system views](/index#group-View-System).
         | """,
      createCustomViewJson,
      customViewJsonV510,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        $BankAccountNotFound,
        $UserNoPermissionAccessView,
        InvalidJsonFormat,
        UnknownError
      ),
      List(apiTagView, apiTagAccount)
    )
    lazy val createCustomView: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "accounts" :: AccountId(accountId) :: "views" :: ViewId(viewId) ::"target-views" ::  Nil JsonPost json -> _ => {
        cc =>
          implicit val ec = EndpointContext(Some(cc))
          for {
            (_, _, _, view, callContext) <- SS.userBankAccountView
            createCustomViewJson <- NewStyle.function.tryons(s"$InvalidJsonFormat The Json body should be the ${classOf[CreateViewJson]}", 400, cc.callContext) {
              json.extract[CreateCustomViewJson]
            }
            //customer views are started ith `_`,eg _life, _work, and System views startWith letter, eg: owner
            _ <- Helper.booleanToFuture(failMsg = InvalidCustomViewFormat + s"Current view_name (${createCustomViewJson.name})", cc = callContext) {
              isValidCustomViewName(createCustomViewJson.name)
            }

            permissionsFromSource = APIUtil.getViewPermissions(view.asInstanceOf[ViewDefinition])
            permissionsFromTarget = createCustomViewJson.allowed_permissions

            _ <- Helper.booleanToFuture(failMsg = SourceViewHasLessPermission + s"Current source viewId($viewId) permissions ($permissionsFromSource), target viewName${createCustomViewJson.name} permissions ($permissionsFromTarget)", cc = callContext) {
              permissionsFromTarget.toSet.subsetOf(permissionsFromSource)
            }

            failMsg = s"${ErrorMessages.ViewDoesNotPermitAccess} You need the `${StringHelpers.snakify(nameOf(view.canCreateCustomView))}` permission on VIEW_ID(${viewId.value})"

            _ <- Helper.booleanToFuture(failMsg, cc = callContext) {
              view.canCreateCustomView
            }
            (view, callContext) <- NewStyle.function.createCustomView(BankIdAccountId(bankId, accountId), createCustomViewJson.toCreateViewJson, callContext)
          } yield {
            (JSONFactory510.createViewJson(view), HttpCode.`201`(callContext))
          }
      }
    }

    resourceDocs += ResourceDoc(
      updateCustomView,
      implementedInApiVersion,
      nameOf(updateCustomView),
      "PUT",
      "/banks/BANK_ID/accounts/ACCOUNT_ID/views/VIEW_ID/target-views/TARGET_VIEW_ID",
      "Update Custom View",
      s"""Update an existing custom view on a bank account
         |
         |${authenticationRequiredMessage(true)} and the user needs to have access to the owner view.
         |
         |The json sent is the same as during view creation (above), with one difference: the 'name' field
         |of a view is not editable (it is only set when a view is created)""",
      updateCustomViewJson,
      customViewJsonV510,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        $BankAccountNotFound,
        $UserNoPermissionAccessView,
        InvalidJsonFormat,
        UnknownError
      ),
      List(apiTagView, apiTagAccount)
    )
    lazy val updateCustomView: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "accounts" :: AccountId(accountId) :: "views" :: ViewId(viewId) :: "target-views" :: ViewId(targetViewId) :: Nil JsonPut json -> _ => {
        cc =>
          implicit val ec = EndpointContext(Some(cc))
          for {
            (_, _, _, view, callContext) <- SS.userBankAccountView
            targetCreateCustomViewJson <- NewStyle.function.tryons(s"$InvalidJsonFormat The Json body should be the ${classOf[UpdateCustomViewJson]}", 400, cc.callContext) {
              json.extract[UpdateCustomViewJson]
            }
            //customer views are started ith `_`,eg _life, _work, and System views startWith letter, eg: owner
            _ <- Helper.booleanToFuture(failMsg = InvalidCustomViewFormat + s"Current TARGET_VIEW_ID (${targetViewId})", cc = callContext) {
              isValidCustomViewId(targetViewId.value)
            }
            permissionsFromSource = APIUtil.getViewPermissions(view.asInstanceOf[ViewDefinition])
            permissionsFromTarget = targetCreateCustomViewJson.allowed_permissions

            _ <- Helper.booleanToFuture(failMsg = SourceViewHasLessPermission + s"Current source view permissions ($permissionsFromSource), target view permissions ($permissionsFromTarget)", cc = callContext) {
              permissionsFromTarget.toSet.subsetOf(permissionsFromSource)
            }

            failmsg = s"${ErrorMessages.ViewDoesNotPermitAccess} You need the `${StringHelpers.snakify(nameOf(view.canUpdateCustomView))}` permission on VIEW_ID(${viewId.value})"

            _ <- Helper.booleanToFuture(failmsg, cc = callContext) {
              view.canCreateCustomView
            }

            (view, callContext) <- NewStyle.function.updateCustomView(BankIdAccountId(bankId, accountId), targetViewId, targetCreateCustomViewJson.toUpdateViewJson, callContext)
          } yield {
            (JSONFactory510.createViewJson(view), HttpCode.`200`(callContext))
          }
      }
    }

    staticResourceDocs += ResourceDoc(
      getCustomView,
      implementedInApiVersion,
      nameOf(getCustomView),
      "GET",
      "/banks/BANK_ID/accounts/ACCOUNT_ID/views/VIEW_ID/target-views/TARGET_VIEW_ID",
      "Get Custom View",
      s"""#Views
         |
         |
         |Views in Open Bank Project provide a mechanism for fine grained access control and delegation to Accounts and Transactions. Account holders use the 'owner' view by default. Delegated access is made through other views for example 'accountants', 'share-holders' or 'tagging-application'. Views can be created via the API and each view has a list of entitlements.
         |
         |Views on accounts and transactions filter the underlying data to redact certain fields for certain users. For instance the balance on an account may be hidden from the public. The way to know what is possible on a view is determined in the following JSON.
         |
         |**Data:** When a view moderates a set of data, some fields my contain the value `null` rather than the original value. This indicates either that the user is not allowed to see the original data or the field is empty.
         |
         |There is currently one exception to this rule; the 'holder' field in the JSON contains always a value which is either an alias or the real name - indicated by the 'is_alias' field.
         |
         |**Action:** When a user performs an action like trying to post a comment (with POST API call), if he is not allowed, the body response will contain an error message.
         |
         |**Metadata:**
         |Transaction metadata (like images, tags, comments, etc.) will appears *ONLY* on the view where they have been created e.g. comments posted to the public view only appear on the public view.
         |
         |The other account metadata fields (like image_URL, more_info, etc.) are unique through all the views. Example, if a user edits the 'more_info' field in the 'team' view, then the view 'authorities' will show the new value (if it is allowed to do it).
         |
         |# All
         |*Optional*
         |
         |Returns the list of the views created for account ACCOUNT_ID at BANK_ID.
         |
         |${authenticationRequiredMessage(true)} and the user needs to have access to the owner view.""",
      EmptyBody,
      customViewJsonV510,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        $BankAccountNotFound,
        $UserNoPermissionAccessView,
        UnknownError
      ),
      List(apiTagView, apiTagAccount)
    )
    lazy val getCustomView: OBPEndpoint = {
      //get the available views on an bank account
      case "banks" :: BankId(bankId) :: "accounts" :: AccountId(accountId) :: "views" :: ViewId(viewId) :: "target-views" :: ViewId(targetViewId):: Nil JsonGet req => {
        cc =>
          implicit val ec = EndpointContext(Some(cc))
            for {
              (_, _, _, view, callContext) <- SS.userBankAccountView
              //customer views are started ith `_`,eg _life, _work, and System views startWith letter, eg: owner
              _ <- Helper.booleanToFuture(failMsg = InvalidCustomViewFormat + s"Current TARGET_VIEW_ID (${targetViewId.value})", cc = callContext) {
                isValidCustomViewId(targetViewId.value)
              }
              failmsg = s"${ErrorMessages.ViewDoesNotPermitAccess} You need the `${StringHelpers.snakify(nameOf(view.canGetCustomView))}`permission on any your views. Current VIEW_ID (${viewId.value})"
              _ <- Helper.booleanToFuture(failmsg, cc = callContext) {
                view.canGetCustomView
              }
              targetView <- NewStyle.function.customView(targetViewId, BankIdAccountId(bankId, accountId), callContext)
            } yield {
              (JSONFactory510.createViewJson(targetView), HttpCode.`200`(callContext))
            }
      }
    }


    staticResourceDocs += ResourceDoc(
      deleteCustomView,
      implementedInApiVersion,
      nameOf(deleteCustomView),
      "DELETE",
      "/banks/BANK_ID/accounts/ACCOUNT_ID/views/VIEW_ID/target-views/TARGET_VIEW_ID",
      "Delete Custom View",
      "Deletes the custom view specified by VIEW_ID on the bank account specified by ACCOUNT_ID at bank BANK_ID",
      EmptyBody,
      EmptyBody,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        $BankAccountNotFound,
        $UserNoPermissionAccessView,
        UnknownError
      ),
      List(apiTagView, apiTagAccount)
    )

    lazy val deleteCustomView: OBPEndpoint = {
      //deletes a view on an bank account
      case "banks" :: BankId(bankId) :: "accounts" :: AccountId(accountId ) :: "views" :: ViewId(viewId) :: "target-views" :: ViewId(targetViewId) :: Nil JsonDelete req => {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (_, _, _, view, callContext) <- SS.userBankAccountView
            //customer views are started ith `_`,eg _life, _work, and System views startWith letter, eg: owner
            _ <- Helper.booleanToFuture(failMsg = InvalidCustomViewFormat + s"Current TARGET_VIEW_ID (${targetViewId.value})", cc = callContext) {
              isValidCustomViewId(targetViewId.value)
            }
            failMsg = s"${ErrorMessages.ViewDoesNotPermitAccess} You need the `${StringHelpers.snakify(nameOf(view.canDeleteCustomView))}` permission on any your views.Current VIEW_ID (${viewId.value})"
            _ <- Helper.booleanToFuture(failMsg, cc = callContext) {
              view.canDeleteCustomView
            }
            _ <- NewStyle.function.customView(targetViewId, BankIdAccountId(bankId, accountId), callContext)
            deleted <- NewStyle.function.removeCustomView(targetViewId, BankIdAccountId(bankId, accountId), callContext)
          } yield {
            (Full(deleted), HttpCode.`204`(callContext))
          }
      }
    }



    staticResourceDocs += ResourceDoc(
      createVRPConsentRequest,
      implementedInApiVersion,
      nameOf(createVRPConsentRequest),
      "POST",
      "/consumer/vrp-consent-requests",
      "Create Consent Request VRP",
      s"""
         |This endpoint is used to begin the process of creating a consent that may be used for Variable Recurring Payments (VRPs).
         |
         |VRPs are useful in situations when a beneficiary needs to be paid different amounts on a regular basis.
         |
         |Once granted, the consent allows its holder to initiate multiple Transaction Requests to the Counterparty defined in this endpoint as long as the
         |Counterparty Limits are respected.
         |
         |Client, Consumer or Application Authentication is mandatory for this endpoint.
         |
         |i.e. the caller of this endpoint is the API Client, Consumer or Application rather than a specific User.
         |
         |At the end of the process the following objects are created in OBP or connected backend systems:
         | - An automatically generated View which controls access.
         | - A Counterparty that is the Beneficiary of the Variable Recurring Payments. The Counterparty specifies the Bank Account number or other routing address.
         | - Limits for the Counterparty which constrain the amount of money that can be sent to it in various periods (yearly, monthly, weekly).
         |
         |The Account holder may modify the Counterparty or Limits e.g. to increase or decrease the maximum possible payment amounts or the frequencey of the payments.
         |
         |
         |In the case of a public client we use the client_id and private key to obtain an access token, otherwise we use the client_id and client_secret.
         |The obtained access token is used in the HTTP Authorization header of the request as follows:
         |
         |Example:
         |Authorization: Bearer eXtneO-THbQtn3zvK_kQtXXfvOZyZFdBCItlPDbR2Bk.dOWqtXCtFX-tqGTVR0YrIjvAolPIVg7GZ-jz83y6nA0
         |
         |After successfully creating the VRP consent request, you need to call the `Create Consent By CONSENT_REQUEST_ID` endpoint to finalize the consent using the CONSENT_REQUEST_ID returned by this endpoint.
         |
         |""".stripMargin,
      postVRPConsentRequestJsonV510,
      vrpConsentRequestResponseJson,
      List(
        InvalidJsonFormat,
        ConsentMaxTTL,
        X509CannotGetCertificate,
        X509GeneralError,
        InvalidConnectorResponse,
        UnknownError
      ),
      apiTagConsent :: apiTagVrp :: apiTagTransactionRequest  :: Nil
    )

    lazy val createVRPConsentRequest : OBPEndpoint = {
      case  "consumer" :: "vrp-consent-requests" :: Nil JsonPost postJson -> _  =>  {
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (_, callContext) <- applicationAccess(cc)
            _ <- passesPsd2Aisp(callContext)
            failMsg = s"$InvalidJsonFormat The Json body should be the $PostVRPConsentRequestJsonV510 "
            postConsentRequestJsonV510: PostVRPConsentRequestJsonV510 <- NewStyle.function.tryons(failMsg, 400, callContext) {
              postJson.extract[PostVRPConsentRequestJsonV510]
            }
            maxTimeToLive = APIUtil.getPropsAsIntValue(nameOfProperty = "consents.max_time_to_live", defaultValue = 3600)
            _ <- Helper.booleanToFuture(s"$ConsentMaxTTL ($maxTimeToLive)", cc = callContext) {
              postConsentRequestJsonV510.time_to_live match {
                case Some(ttl) => ttl <= maxTimeToLive
                case _ => true
              }
            }

            // we need to add the consent_type internally, the user does not need to know it.
            consentType = json.parse(s"""{"consent_type": "${ConsentType.VRP}"}""")
            
            (_, callContext) <- NewStyle.function.checkBankAccountExists(
              BankId(postConsentRequestJsonV510.from_account.bank_routing.address),
              AccountId(postConsentRequestJsonV510.from_account.account_routing.address), 
              callContext
            )
            
            createdConsentRequest <- Future(ConsentRequests.consentRequestProvider.vend.createConsentRequest(
              callContext.flatMap(_.consumer),
              Some(compactRender(postJson merge consentType))
            )) map {
              i => connectorEmptyResponse(i, callContext)
            }
          } yield {
            (JSONFactory500.createConsentRequestResponseJson(createdConsentRequest), HttpCode.`201`(callContext))
          }
      }
    }

  }
}



object APIMethods510 extends RestHelper with APIMethods510 {
  lazy val newStyleEndpoints: List[(String, String)] = Implementations5_1_0.resourceDocs.map {
    rd => (rd.partialFunctionName, rd.implementedInApiVersion.toString())
  }.toList
}

