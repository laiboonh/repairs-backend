package utils

import cats.MonadError
import cats.data.OptionT
import cats.effect.Sync
import tsec.authentication.BackingStore

import scala.collection.mutable

object GenericMemoryStore {
  def apply[F[_], I, V](getId: V => I)(implicit F: MonadError[F, Throwable]): BackingStore[F, I, V] = new BackingStore[F, I, V] {
    private val storageMap = mutable.HashMap.empty[I, V]

    def put(elem: V): F[V] = {
      val id = getId(elem)
      storageMap.put(id, elem) match {
        case None => F.pure(elem)
        case Some(_) => F.raiseError(new IllegalArgumentException(s"elem $id is already present"))
      }
    }

    def get(id: I): OptionT[F, V] =
      OptionT.fromOption[F](storageMap.get(id))

    def update(v: V): F[V] = {
      storageMap.update(getId(v), v)
      F.pure(v)
    }

    def delete(id: I): F[Unit] =
      storageMap.remove(id) match {
        case Some(_) => F.unit
        case None => F.raiseError(new IllegalArgumentException(s"Fail to delete elem $id"))
      }
  }
}
