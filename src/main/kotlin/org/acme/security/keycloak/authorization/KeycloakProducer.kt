package org.acme.security.keycloak.authorization

import io.quarkus.logging.Log
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.keycloak.OAuth2Constants.CLIENT_CREDENTIALS
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class KeycloakProducer @Inject constructor(
  @ConfigProperty(name = "keycloak.realm") private val realm: String,
  @ConfigProperty(name = "quarkus.oidc.auth-server-url") private val url: String,
  @ConfigProperty(name = "quarkus.oidc.client-id") private val clientId: String,
  @ConfigProperty(name = "quarkus.oidc.credentials.secret") private val clientSecret: String,
) {

  @ApplicationScoped
  fun keycloak(): Keycloak {
    Log.debug("Setup Keycloak admin client with url: $url, realm: $realm, clientId: $clientId, clientSecret: $clientSecret")
    return KeycloakBuilder.builder()
      .serverUrl(if (url.contains("realms/")) url.substring(0, url.indexOf("realms/")) else url)
      .realm(realm)
      .grantType(CLIENT_CREDENTIALS)
      .clientId(clientId)
      .clientSecret(clientSecret)
      .build()
  }

}