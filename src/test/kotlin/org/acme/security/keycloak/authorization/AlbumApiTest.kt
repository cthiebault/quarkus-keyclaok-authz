package org.acme.security.keycloak.authorization

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.logging.Log
import io.quarkus.oidc.runtime.OidcUtils
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.vertx.core.json.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import javax.inject.Inject
import javax.ws.rs.core.MediaType.APPLICATION_JSON


@QuarkusTest
internal class AlbumApiTest {

  init {
    RestAssured.useRelaxedHTTPSValidation()
  }

  @Inject
  lateinit var objectMapper: ObjectMapper

  private val keycloakClient = KeycloakTestClient()

  @Test
  fun testPostAndGetAlbum() {
    given()
      .auth().oauth2(keycloakClient.getAccessToken("alice"))
      .header("Content-Type", APPLICATION_JSON)
      .`when`().post("/api/albums?name=MyAlbum")
      .then()
      .statusCode(200)

    val token = keycloakClient.getToken("alice")
    val rtp = keycloakClient.getRequestingPartyToken(token.token)
    val jwtContent: JsonObject = OidcUtils.decodeJwtContent(rtp.token)
    Log.info("JWT content: ${jwtContent.encodePrettily()}")

    val authorization = jwtContent.map["authorization"] as Map<String, Any>
    val permissions = authorization["permissions"] as List<Map<String, Any>>
    val album1Permission: Map<String, Any> = permissions.first { it["rsname"] == "album:1" }

    assertNotNull(album1Permission)
    assertEquals((album1Permission["scopes"] as List<String>)[0], "GET")

    given()
      .auth().oauth2(keycloakClient.getAccessToken("alice"))
      .`when`().get("/api/albums/1")
      .then()
      .statusCode(200)

    given()
      .auth().oauth2(keycloakClient.getAccessToken("admin"))
      .`when`().get("/api/albums/1")
      .then()
      .statusCode(200)
  }

  @Test
  fun testPutAndGetAlbum() {
    val album = Album(10, "Toto")
    given()
      .auth().oauth2(keycloakClient.getAccessToken("alice"))
      .header("Content-Type", APPLICATION_JSON)
      .body(album)
      .`when`().put("/api/albums/${album.id}")
      .then()
      .statusCode(200)

    val token = keycloakClient.getToken("alice")
    val rtp = keycloakClient.getRequestingPartyToken(token.token)
    val jwtContent = OidcUtils.decodeJwtContent(rtp.token)

    Log.info("rtp:\n${objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rtp)}")


    //  "authorization": {
    //    "permissions": [
    //      {
    //        "scopes": [ "POST" ],
    //        "rsid": "a7603402-05ec-43ab-bc47-e8e7808769fd",
    //        "rsname": "Albums resource"
    //      },
    //      {
    //        "scopes": [ "PUT" ],
    //        "rsid": "561a2d9d-8654-40c6-89df-7868599db78f",
    //        "rsname": "Create albums resource"
    //      },
    //      {
    //        "scopes": [ "GET" ],
    //        "rsid": "10",
    //        "rsname": "album:10"
    //      }
    //    ]
    //  }

    given()
      .auth().oauth2(keycloakClient.getAccessToken("alice"))
      .`when`().get("/api/albums/10")
      .then()
      .statusCode(200)

    given()
      .auth().oauth2(keycloakClient.getAccessToken("admin"))
      .`when`().get("/api/albums/10")
      .then()
      .statusCode(200)
  }


}