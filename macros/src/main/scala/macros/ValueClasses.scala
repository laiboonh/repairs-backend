package macros

import derevo.circe.{decoder, encoder}
import derevo.derive
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

object ValueClasses {

  import io.circe.refined._ //Do not remove

  @derive(encoder, decoder)
  @newtype
  case class Name(value: NonEmptyString)

}
