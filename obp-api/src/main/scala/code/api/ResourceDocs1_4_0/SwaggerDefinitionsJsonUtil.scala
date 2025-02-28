package code.api.ResourceDocs1_4_0

import code.api.util.ConsentJWT
import net.liftweb.json._

object SwaggerDefinitionsJsonUtil {
  implicit val formats: Formats = DefaultFormats

  // General method to parse JSON into any case class
  def parseJsonToCaseClass[T](jsonString: String)(implicit mf: Manifest[T]): Option[T] = {
    parse(jsonString).extractOpt[T]
  }

  // Usage example with ConsentJWT, or replace ConsentJWT with any other case class
  val jwtPayload: Option[ConsentJWT] = parseJsonToCaseClass[ConsentJWT](
    """{
        "createdByUserId": "",
        "sub": "fcf346d2-153b-4430-9a8f-f35768533987",
        "iss": "https://127.0.0.1:8080",
        "aud": "2664650c-8090-481e-8bd8-03a92f9c87ea",
        "jti": "d33f7463-ee49-4e8a-8a92-6138dc183d16",
        "iat": 1730373271,
        "nbf": 1730373271,
        "exp": 1730937600,
        "request_headers":[],
        "entitlements": [],
        "views": [ {
          "bank_id": "nlbkb",
          "account_id": "95389297-45c4-40eb-9efd-331ba1943da4",
          "view_id": "ReadAccountsBerlinGroup"
        }],
        "access": {
          "accounts": [ {
            "iban": "RS35260005601001611379"
          }]
        }
      }"""
  )
}
