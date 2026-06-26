package user

import cats.effect.IO
import doobie.postgres.implicits.UuidType
import doobie.util.*
import doobie.implicits.*
import cats.syntax.all.*
import doobie.postgres.*
import doobie.util.fragment.Fragment
import infrastructure.db.DBDoobie.DBTransactor

import java.util.UUID

class UserRepository(dbTransactor: DBTransactor):
  def registerUser(user: User): IO[Either[UserRegistrationError, User]] =
    sql"""
          INSERT INTO users (id, email, password_hash, firstname, lastname, role)
         VALUES (${user.id}, ${user.email}, ${user.passwordHash}, ${user.firstName}, ${user.lastName}, ${user.role})
         """.update.run
      .as(user)
      .attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
        UserAlreadyExistsError(user.email)
      }
      .transact(dbTransactor)

  def getById(id: UUID): IO[Option[User]] = getByUniqueWhereCondition(sql"id = ${id}")

  private def getByUniqueWhereCondition(condition: Fragment): IO[Option[User]] =
    sql"""
         SELECT id, email, password_hash, firstname, lastname, role
         FROM users
         WHERE ${condition}"""
      .query[User]
      .option
      .transact(dbTransactor)
