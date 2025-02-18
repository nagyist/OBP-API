package code.api.util

import java.io.{ByteArrayInputStream, FileInputStream}
import java.security.KeyStore
import java.security.cert._
import java.util.{Base64, Collections}
import javax.net.ssl.TrustManagerFactory
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

object CertificateVerifier {

  // Load trust store from configured path and password
  private def loadTrustStore(): Option[KeyStore] = {
    val trustStorePath = APIUtil.getPropsValue("truststore.path.tpp_signature", "")
    val trustStorePassword = APIUtil.getPropsValue("truststore.password.tpp_signature", "").toCharArray

    Try {
      val trustStore = KeyStore.getInstance("PKCS12") // Using `.p12` format
      val trustStoreInputStream = new FileInputStream(trustStorePath)
      trustStore.load(trustStoreInputStream, trustStorePassword)
      trustStoreInputStream.close()
      trustStore
    } match {
      case Success(store) =>
        println(s"✅ Loaded trust store from: $trustStorePath")
        Some(store)
      case Failure(e) =>
        println(s"❌ Failed to load trust store: ${e.getMessage}")
        None
    }
  }

  def verifyCertificate(pemCertificate: String): Boolean = {
    Try {
      val certificate = parsePemToX509Certificate(pemCertificate)

      // Load trust store
      val trustStore = loadTrustStore()
        .getOrElse(throw new Exception("Trust store could not be loaded."))

      val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
      trustManagerFactory.init(trustStore)

      // Get trusted CAs from the trust store
      val trustAnchors = trustStore.aliases().asScala
        .filter(trustStore.isCertificateEntry)
        .map(alias => trustStore.getCertificate(alias).asInstanceOf[X509Certificate])
        .map(cert => new TrustAnchor(cert, null))
        .toSet
        .asJava  // Convert Scala Set to Java Set

      if (trustAnchors.isEmpty) throw new Exception("No trusted certificates found in trust store.")

      // Set up PKIX parameters for validation
      val pkixParams = new PKIXParameters(trustAnchors)
      pkixParams.setRevocationEnabled(false) // Disable CRL checks

      // Validate certificate chain
      val certPath = CertificateFactory.getInstance("X.509").generateCertPath(Collections.singletonList(certificate))
      val validator = CertPathValidator.getInstance("PKIX")
      validator.validate(certPath, pkixParams)

      println("✅ Certificate is valid and trusted.")
      true
    } match {
      case Success(_) => true
      case Failure(e: CertPathValidatorException) =>
        println(s"❌ Certificate validation failed: ${e.getMessage}")
        false
      case Failure(e) =>
        println(s"❌ Error: ${e.getMessage}")
        false
    }
  }

  private def parsePemToX509Certificate(pem: String): X509Certificate = {
    val cleanedPem = pem.replaceAll("-----BEGIN CERTIFICATE-----", "")
      .replaceAll("-----END CERTIFICATE-----", "")
      .replaceAll("\\s", "")

    val decoded = Base64.getDecoder.decode(cleanedPem)
    val certFactory = CertificateFactory.getInstance("X.509")
    certFactory.generateCertificate(new ByteArrayInputStream(decoded)).asInstanceOf[X509Certificate]
  }

  def main(args: Array[String]): Unit = {
    val pemCertificate =
      """-----BEGIN CERTIFICATE-----
      MIIDFzCCAf+gAwIBAgIUPvfFnlyEm/bRwvPzhpfSxuI6XjkwDQYJKoZIhvcNAQELBQAwGzEZMBcGA1UEAwwQVGVzdCBDZXJ0aWZpY2F0ZTAeFw0yNTAyMTQwODM3NDhaFw0yNjAyMTQwODM3NDhaMBsxGTAXBgNVBAMMEFRlc3QgQ2VydGlmaWNhdGUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCk9Mj4YgJywaCduTLjAEd3o1OqzFaj2MuI/bcdKIwPlld0n8WHp+CMkbpCD8TSAlDrjLjxcL6Homw8SM3VYUJVP/5phRNgNx7E+KzquskPUsWvTUnylLF52jLjbKVXqs6DuukGAaJNudcuJCPuGd5xDTiymRdqFL1LFxSlaqt/qRS8DV9d3/Z0JwXuHebq17pjUGluq8nkJ0N1zF5hKLdQmo9PxVULY5Kubjf2cXoH09AgJUj3RSgeScRbFxgYOhU/5OaEfQuAST0Qa8lFI6SyWQp5G08wNZGITLh/66ZissNPYIUgqGccDFKWhUNDubFF+Qyl3Gy12g8Uou6FN1qrAgMBAAGjUzBRMB0GA1UdDgQWBBSN2MfohCTpCamhcyidj2w6z6tGXDAfBgNVHSMEGDAWgBSN2MfohCTpCamhcyidj2w6z6tGXDAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQBYXj3L5UN8PxJAMtLT9bU4FkxyXIQM+bzvAln1ZcAfHAGb2q49oJAXy4I22f9keuq3PV7OftsjZ888rjz9QU8vMSBejWT5GV4Ln5QmQXCHonmhq6DbP7BYb4DTOXfhvk+fdg0EDdqCpzDSCXdutOjjGU6P7L0769Zjpkrnk7uuqxZ8u/FslALeuq7cerBpsOUT5CJumpQxWcUCEbFxyZJTu5SXetgKJ9Dm62AfX5H69//z88W5TUzp66Mh4AWhEa/UByJGEw9SEsjFtYhkXluz5oFee5TGWTVZRlK08UrgH9JbiuyvPc9ZNL6Ek9fV54iajqsixZCfcICICtu8hZjZ
      -----END CERTIFICATE-----"""

    val isValid = verifyCertificate(pemCertificate)
    println(s"✅ Certificate verification result: $isValid")

    // Display loaded trust store info
    loadTrustStore().foreach { trustStore =>
      println(s"🔹 Trust Store contains ${trustStore.size()} certificates.")
    }
  }
}
