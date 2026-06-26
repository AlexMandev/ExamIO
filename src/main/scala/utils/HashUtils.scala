package utils

import org.mindrot.jbcrypt.BCrypt

object HashUtils:
  def hashPassword(pwd: String): String =
    BCrypt.hashpw(pwd, BCrypt.gensalt())

  def checkPassword(plain: String, hashed: String): Boolean =
    BCrypt.checkpw(plain, hashed)
