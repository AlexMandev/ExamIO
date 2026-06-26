package utils

import org.mindrot.jbcrypt.BCrypt

object HashUtils:
  def hashPassword(pwd: String): String =
    BCrypt.hashpw(pwd, BCrypt.gensalt())
