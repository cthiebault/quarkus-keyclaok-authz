package org.acme.security.keycloak.authorization

import io.quarkus.logging.Log
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.resource.ClientResource
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class ClientResourceProducer @Inject constructor(
  private val keycloak: Keycloak,
  @ConfigProperty(name = "keycloak.realm") private val realm: String,
  @ConfigProperty(name = "quarkus.oidc.client-id") private val clientId: String,
) {

  @ApplicationScoped
  fun clientResource(): ClientResource {
    Log.debug("Setup Keycloak ClientResource with realm: $realm, clientId: $clientId")
    val clientsResource = keycloak.realm(realm).clients()
    return clientsResource.findByClientId(clientId)
      .map { clientsResource[it.id] }
      .firstOrNull()
      ?: throw IllegalStateException("Keycloak client $clientId not found")
  }

}