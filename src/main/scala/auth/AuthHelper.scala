package auth

import cats.data.OptionT
import cats.effect.Sync
import cats.{Id, MonadError}
import models.User
import ports.UserRepo
import tsec.authentication.{AugmentedJWT, BackingStore, JWTAuthenticator, SecuredRequestHandler}
import tsec.common.SecureRandomId
import tsec.mac.jca.{HMACSHA256, MacSigningKey}
import utils.GenericMemoryStore

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class AuthHelper[F[_]](val userRepo: UserRepo[F])(implicit F: MonadError[F, Throwable], S: Sync[F]) {

  private val jwtStore =
    GenericMemoryStore[F, SecureRandomId, AugmentedJWT[HMACSHA256, UUID]](s => SecureRandomId.coerce(s.id))

  private val userStore: BackingStore[F, UUID, User] = new BackingStore[F, UUID, User] {
    override def put(user: User): F[User] = userRepo.create(user)

    override def update(user: User): F[User] = userRepo.update(user)

    override def delete(id: UUID): F[Unit] = userRepo.delete(id)

    override def get(id: UUID): OptionT[F, User] = OptionT(
      userRepo.retrieve(id)
    )
  }

  private val signingKey: MacSigningKey[HMACSHA256] = HMACSHA256.generateKey[Id]

  val jwtStatefulAuth: JWTAuthenticator[F, UUID, User, HMACSHA256] =
    JWTAuthenticator.backed.inBearerToken(
      expiryDuration = 1 minute, //Absolute expiration time
      maxIdle = None,
      tokenStore = jwtStore,
      identityStore = userStore,
      signingKey = signingKey
    )

  val auth: SecuredRequestHandler[F, UUID, User, AugmentedJWT[HMACSHA256, UUID]] =
    SecuredRequestHandler(jwtStatefulAuth)
}