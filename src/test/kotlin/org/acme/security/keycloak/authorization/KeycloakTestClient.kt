package org.acme.security.keycloak.authorization

import io.quarkus.logging.Log
import io.quarkus.runtime.configuration.ConfigurationException
import io.quarkus.test.common.DevServicesContext
import io.quarkus.test.common.DevServicesContext.ContextAware
import io.restassured.RestAssured
import org.eclipse.microprofile.config.ConfigProvider
import org.keycloak.representations.AccessTokenResponse
import java.net.URI

// Copied from io.quarkus.test.keycloak.client.KeycloakTestClient
class KeycloakTestClient : ContextAware {

  companion object {
    const val CLIENT_AUTH_SERVER_URL_PROP = "client.quarkus.oidc.auth-server-url"
    const val AUTH_SERVER_URL_PROP = "quarkus.oidc.auth-server-url"
    const val CLIENT_ID_PROP = "quarkus.oidc.client-id"
    const val CLIENT_SECRET_PROP = "quarkus.oidc.credentials.secret"
  }

  init {
    RestAssured.useRelaxedHTTPSValidation()
  }

  private var testContext: DevServicesContext? = null

  private val clientId: String
    get() = getPropertyValue(CLIENT_ID_PROP, "quarkus-app")!!

  private val clientSecret: String
    get() = getPropertyValue(CLIENT_SECRET_PROP, "secret")!!

  /**
   * Return URL string pointing to a Keycloak base endpoint.
   * For example: 'http://localhost:8081/auth'.
   */
  private val authServerBaseUrl: String
    get() {
      val uri = URI(authServerUrl)
      // Keycloak-X does not have the `/auth` path segment by default.
      return URI(
        uri.scheme,
        uri.userInfo,
        uri.host,
        uri.port,
        if (uri.path.startsWith("/auth")) "/auth" else null,
        null,
        null
      ).toString()
    }

  /**
   * Return URL string pointing to a Keycloak authentication endpoint configured with a 'quarkus.oidc.auth-server'
   * property.
   * For example: 'http://localhost:8081/auth/realms/quarkus'.
   */
  val authServerUrl: String
    get() {
      var authServerUrl: String? = getPropertyValue(CLIENT_AUTH_SERVER_URL_PROP, null)
      if (authServerUrl == null) {
        authServerUrl = getPropertyValue(AUTH_SERVER_URL_PROP, null)
      }
      if (authServerUrl == null) {
        throw ConfigurationException(
          "Unable to obtain the Auth Server URL as neither '$CLIENT_AUTH_SERVER_URL_PROP' or '$AUTH_SERVER_URL_PROP' is set"
        )
      }
      return authServerUrl
    }


  /**
   * Get an access token using a password grant with a provided user name.
   * User secret will be the same as the user name, client id will be set to 'quarkus-app' and client secret to 'secret'.
   */
  fun getAccessToken(
    userName: String,
    userSecret: String = userName,
    clientId: String = this.clientId,
    clientSecret: String? = this.clientSecret,
    authServerUrl: String = this.authServerUrl
  ): String =
    getToken(userName, userSecret, clientId, clientSecret, authServerUrl).token

  fun getToken(
    userName: String,
    userSecret: String = userName,
    clientId: String = this.clientId,
    clientSecret: String? = this.clientSecret,
    authServerUrl: String = this.authServerUrl
  ): AccessTokenResponse {
    var requestSpec = RestAssured.given()
      .param("grant_type", "password")
      .param("username", userName)
      .param("password", userSecret)
      .param("client_id", clientId)
    if (!clientSecret.isNullOrBlank()) {
      requestSpec = requestSpec.param("client_secret", clientSecret)
    }
    return requestSpec
      .`when`().post("$authServerUrl/protocol/openid-connect/token")
      .`as`(AccessTokenResponse::class.java)
  }

  fun getAccessTokenFromRefreshToken(
    refreshToken: String,
    clientId: String = this.clientId,
    clientSecret: String? = this.clientSecret,
    authServerUrl: String = this.authServerUrl
  ): AccessTokenResponse {
    var requestSpec = RestAssured.given()
      .param("grant_type", "refresh_token")
      .param("refresh_token", refreshToken)
      .param("client_id", clientId)
    if (!clientSecret.isNullOrBlank()) {
      requestSpec = requestSpec.param("client_secret", clientSecret)
    }
    return requestSpec
      .`when`().post("$authServerUrl/protocol/openid-connect/token")
      .`as`(AccessTokenResponse::class.java)
  }

  fun getRequestingPartyToken(
    accessToken: String,
    clientId: String = this.clientId,
    clientSecret: String? = this.clientSecret,
    authServerUrl: String = this.authServerUrl
  ) : AccessTokenResponse{
    var requestSpec = RestAssured.given()
      .auth().oauth2(accessToken)
      .param("grant_type", "urn:ietf:params:oauth:grant-type:uma-ticket")
      .param("client_id", clientId)
      .param("audience", clientId)
    if (!clientSecret.isNullOrBlank()) {
      requestSpec = requestSpec.param("client_secret", clientSecret)
    }
    return requestSpec
      .`when`().post("$authServerUrl/protocol/openid-connect/token")
      .`as`(AccessTokenResponse::class.java)
  }


  /**
   * Get an admin access token which can be used to create Keycloak realms and perform other Keycloak administration tasks.
   */
  fun getAdminAccessToken(): String =
    getAccessToken(
      userName = "admin",
      userSecret = "admin",
      clientId = "admin-cli",
      clientSecret = null,
      authServerUrl = "$authServerBaseUrl/realms/master"
    )


  fun logout(
    userName: String,
    clientId: String = this.clientId,
    clientSecret: String? = this.clientSecret,
    authServerUrl: String = this.authServerUrl
  ) {

    val token = getToken(userName, userName, clientId, clientSecret, authServerUrl)

    val logout = RestAssured.given()
      .auth().oauth2(token.token)
      .param("client_id", clientId)
      .param("client_secret", clientSecret)
      .param("refresh_token", token.refreshToken)
      .`when`().post("${authServerUrl}/protocol/openid-connect/logout")
      .asPrettyString()

    Log.info("logout: $logout")
  }

  private fun getPropertyValue(prop: String, defaultValue: String?): String? =
    ConfigProvider.getConfig().getOptionalValue(prop, String::class.java)
      .orElseGet { getDevProperty(prop, defaultValue) }

  private fun getDevProperty(prop: String, defaultValue: String?): String? =
    testContext?.devServicesProperties()?.get(prop)
      ?: defaultValue

  override fun setIntegrationTestContext(testContext: DevServicesContext?) {
    this.testContext = testContext
  }
}