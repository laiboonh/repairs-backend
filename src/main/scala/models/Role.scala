package models

import cats.kernel.Eq
import doobie.Meta
import doobie.postgres.implicits.pgEnumStringOpt
import skunk.Codec
import skunk.codec.all.`enum`
import skunk.data.Type
import tsec.authorization.{AuthGroup, SimpleAuthEnum}

case class Role(roleRepr: String)

object Role extends SimpleAuthEnum[Role, String] {

  val codec: Codec[Role] = enum[Role](Role.getRepr, Role.getRole, Type("role")) //Type("role") refers to the created PG type

  val Administrator: Role = Role("Administrator")
  val BasicUser: Role = Role("BasicUser")

  implicit val E: Eq[Role] = Eq.fromUniversalEquals[Role]

  override def getRepr(t: Role): String = t.roleRepr

  def getRole(repr: String): Option[Role] = if (values.contains(Role(repr)))
    Some(Role(repr))
  else
    None

  override protected val values: AuthGroup[Role] = AuthGroup(Administrator, BasicUser)

  //doobie
  def toEnum(e: Role): String =
    getRepr(e)

  def fromEnum(s: String): Option[Role] =
    getRole(s)

  implicit val RoleMeta: Meta[Role] =
    pgEnumStringOpt("role", Role.fromEnum, Role.toEnum)
}

