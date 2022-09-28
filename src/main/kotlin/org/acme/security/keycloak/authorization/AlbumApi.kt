package org.acme.security.keycloak.authorization

import io.quarkus.logging.Log
import io.quarkus.security.identity.SecurityIdentity
import javax.inject.Inject
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.ws.rs.*

@Produces("application/json")
@Consumes("application/json")
@Path("/api/albums")
class AlbumApi @Inject constructor(
  private val albumAuthz: AlbumAuthz,
  private val securityIdentity: SecurityIdentity,
) {

  private val repository = mutableMapOf<Long, Album>()
  private var lastId: Long = 0

  @POST
  fun post(@QueryParam("name") name: String): Album {
    val album = Album(++lastId, name)
    repository[album.id] = album
    Log.info("Created album $album")

    createAuthz(album)

    return album
  }

  @PUT
  @Path("/{id}")
  fun put(
    @PathParam("id") id: Long,
    @Valid @NotNull album: Album,
  ): Album {

    val newAlbum = album.copy(id = id)
    lastId = id
    repository[id] = newAlbum
    Log.info("Created album $newAlbum")

    createAuthz(album)

    return album
  }

  @GET
  @Path("/{id}")
  fun get(id: Long): Album {
    return repository[id] ?: throw NotFoundException()
  }

  private fun createAuthz(album: Album) {
    val resource = albumAuthz.createResource(album)
    val policy = albumAuthz.createReadersPolicy(album, resource.id, securityIdentity.principal.name)
    albumAuthz.createReaderPermission(album, resource.id, policy.id)
  }

}

