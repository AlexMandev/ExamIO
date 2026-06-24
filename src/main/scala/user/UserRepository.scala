package user

import cats.effect.IO
import doobie.postgres.implicits.UuidType
import doobie.util.*
import doobie.implicits.*
import doobie.util.fragment.Fragment
import infrastructure.db.DBDoobie.DBTransactor

import java.util.UUID

class UserRepository(dbTransactor: DBTransactor):
  def getById(id: UUID): IO[Option[User]] = getByUniqueWhereCondition(sql"id = ${id}")

  private def getByUniqueWhereCondition(condition: Fragment) =
    sql"""
         SELECT id, email, password_hash, firstname, lastname, role
         FROM users
         WHERE ${condition}"""
      .query[User]
      .option
      .transact(dbTransactor)
