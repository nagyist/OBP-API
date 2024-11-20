package code.container

import code.api.v5_0_0.V500ServerSetup
import code.setup.DefaultUsers
import dasniko.testcontainers.keycloak.KeycloakContainer
import org.keycloak.TokenVerifier
import org.keycloak.admin.client.Keycloak
import org.keycloak.representations.{AccessToken, AccessTokenResponse}
import org.scalatest.Ignore

@Ignore
class EmbeddedKeycloak extends V500ServerSetup with DefaultUsers {

  val keycloakContainer = new KeycloakContainer("quay.io/keycloak/keycloak:26.0")
  // It registers a shutdown hook, which is a block of code (or function) that runs when the application terminates,
  // - either normally(e.g., when the main method completes)
  // - or due to an external signal(e.g., Ctrl + C or termination by the operating system).
  sys.addShutdownHook {
    keycloakContainer.stop()
  }
  override def beforeAll(): Unit = {
    super.beforeAll()
    // Start RabbitMQ container
    keycloakContainer.start()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    keycloakContainer.stop()
  }

  feature(s"test EmbeddedKeycloak") {
    scenario("Start and Stop") {
      val authServerUrl = keycloakContainer.getAuthServerUrl
      println(s"Keycloak server running at: $authServerUrl")
      val adminUsername = keycloakContainer.getAdminUsername
      println(s"Keycloak admin username: $adminUsername")
      val adminPassword = keycloakContainer.getAdminPassword
      println(s"Keycloak admin password: $adminPassword")


      // Assume KEYCLOAK is an instance of Keycloak
      val keycloakClient: Keycloak = keycloakContainer.getKeycloakAdminClient()

      // Grant token
      keycloakClient.tokenManager().grantToken()

      // Refresh token
      keycloakClient.tokenManager().refreshToken()

      // Get access token response
      val tokenResponse: AccessTokenResponse = keycloakClient.tokenManager().getAccessToken()

      // Parse the received access token
      val verifier = TokenVerifier.create(tokenResponse.getToken, classOf[AccessToken])
      verifier.parse()

      // Retrieve
      val accessToken: AccessToken = verifier.getToken

      println(s"Access Token Issuer: ${accessToken.getIssuer}")

    }
  }

}