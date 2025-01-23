package code.api.util


import code.api.OAuth2Login.Keycloak
import net.liftweb.common.{Box, Failure, Full}
import okhttp3._
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.LoggerFactory


object KeycloakAdmin extends App {

  // Initialize Logback logger
  private val logger = LoggerFactory.getLogger("okhttp3")

  // Define variables (replace with actual values)
  private val keycloakHost = Keycloak.keycloakHost
  private val realm = "master"
  private val accessToken = ""

  def createHttpClientWithLogback(): OkHttpClient = {
    val builder = new OkHttpClient.Builder()
    val logging = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger {
      override def log(message: String): Unit = logger.debug(message)
    })
    logging.setLevel(HttpLoggingInterceptor.Level.BODY) // Log full request/response details
    builder.addInterceptor(logging)
    builder.build()
  }
  // Create OkHttp client with logging
  val client = createHttpClientWithLogback()

  createClient(
    "my-consumer-client",
    "My Consume",
    "Client for accessing API resources",
    isPublic = false
  )

  def createClient(clientId: String,
                   name: String,
                   description: String,
                   isPublic: Boolean,
                   realm: String = realm
                  ): Unit = {
    val url = s"$keycloakHost/admin/realms/$realm/clients"
    // JSON request body
    val jsonBody =
      s"""{
        |  "clientId": "$clientId",
        |  "name": "$name",
        |  "description": "$description",
        |  "enabled": true,
        |  "clientAuthenticatorType": "client-secret",
        |  "directAccessGrantsEnabled": true,
        |  "standardFlowEnabled": true,
        |  "implicitFlowEnabled": false,
        |  "serviceAccountsEnabled": true,
        |  "publicClient": false,
        |  "secret": "$isPublic"
        |}""".stripMargin

    // Define the request with headers and JSON body
    val requestBody = RequestBody.create(MediaType.get("application/json; charset=utf-8"), jsonBody)

    val request = new Request.Builder()
      .url(url)
      .post(requestBody)
      .addHeader("Authorization", s"Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .build()

    makeAndHandleHttpCall(request)
  }

  private def makeAndHandleHttpCall(request: Request): Box[Boolean] = {
    // Execute the request
    try {
      val response = client.newCall(request).execute()
      if (response.isSuccessful) {
        logger.debug(s"Response: ${response.body.string}")
        Full(response.isSuccessful)
      } else {
        logger.error(s"Request failed with status code: ${response.code}")
        logger.debug(s"Response: ${response}")
        Failure(s"code: ${response.code} message: ${response.message}")
      }
    } catch {
      case e: Exception =>
        logger.error(s"Error occurred: ${e.getMessage}")
        Failure(e.getMessage)
    }
  }
}