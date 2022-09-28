package org.acme.security.keycloak.authorization

import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.keycloak.admin.client.resource.ClientResource
import org.keycloak.authorization.client.AuthzClient
import javax.inject.Inject
import kotlin.random.Random

@QuarkusTest
internal class AlbumAuthzTest {

  @Inject
  lateinit var albumAuthz: AlbumAuthz

  @Inject
  lateinit var authzClient: AuthzClient

  @Inject
  lateinit var clientResource: ClientResource

  @Test
  fun test() {

    val album = Album(Random.nextLong(), "My Album")

    albumAuthz.createResource(album)

    val resource = authzClient.protection().resource().findById(album.id.toString())
    assertNotNull(resource)
    assertEquals(album.id.toString(), resource.name)
    assertTrue(resource.uris.contains("/api/albums/${album.id}"))
    assertTrue(resource.scopes.map { it.name }.contains("GET"))

    albumAuthz.createReadersPolicy(album, resource.id, "alice")
    val policy = clientResource.authorization().policies().findByName("${album.id}-readers")
    assertNotNull(policy)

    val permission = albumAuthz.createReaderPermission(album, resource.id, policy.id)
    assertNotNull(permission)
  }

}