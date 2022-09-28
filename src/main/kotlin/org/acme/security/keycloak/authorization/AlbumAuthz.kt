package org.acme.security.keycloak.authorization

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.logging.Log
import org.keycloak.admin.client.resource.ClientResource
import org.keycloak.authorization.client.AuthzClient
import org.keycloak.representations.idm.authorization.*
import java.util.UUID.randomUUID
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class AlbumAuthz @Inject constructor(
  private val authzClient: AuthzClient,
  private val clientResource: ClientResource,
  private val objectMapper: ObjectMapper,
) {

  companion object {
    private const val ANY_ADMIN_POLICY_ID = "6facf5a0-e14b-4af3-b3a1-0e3f544bf89f"
  }

  fun createResource(album: Album): ResourceRepresentation {
    // we don't set principal as owner here so the owner is the resource server itself.
    // So we can search resource by name
    val representation = ResourceRepresentation().apply {
      id = album.id.toString()
      name = "album:${album.id}"
      displayName = "[album] ${album.name}"
      uris = setOf("/api/albums/${album.id}")
      type = "album"
      scopes = setOf(ScopeRepresentation("GET"))
    }
    Log.info("Saving album resource: ${toJson(representation)}")
    return authzClient.protection().resource().create(representation)
  }

  fun createReadersPolicy(album: Album, resourceId: String, username: String): UserPolicyRepresentation {
    val policy = UserPolicyRepresentation().apply {
      id = randomUUID().toString()
      name = "album:$resourceId:readers"
      description = "Album ${album.name} readers"
      users = setOf(username)
    }
    Log.info("Saving album readers policy: ${toJson(policy)}")
    val userPoliciesResource = clientResource.authorization().policies().user()
    userPoliciesResource.create(policy)
    return userPoliciesResource.findById(policy.id).toRepresentation()
  }

  fun createReaderPermission(album: Album, resourceId: String, readerPolicyId: String): ScopePermissionRepresentation {
    val permission = ScopePermissionRepresentation().apply {
      id = randomUUID().toString()
      name = "album:$resourceId:read"
      description = "Permission to read Album ${album.name}"
      resources = setOf(resourceId)
      policies = setOf(readerPolicyId, ANY_ADMIN_POLICY_ID)
      scopes = setOf("GET")
      decisionStrategy = DecisionStrategy.AFFIRMATIVE
    }
    Log.info("Saving album reader permission: ${toJson(permission)}")
    val scopePermissionsResource = clientResource.authorization().permissions().scope()
    scopePermissionsResource.create(permission)
    return scopePermissionsResource.findById(permission.id).toRepresentation()
  }

  private fun toJson(any: Any): String = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(any)

}