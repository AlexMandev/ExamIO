package utils

import cats.syntax.all.*
import cats.data.ValidatedNec

object ValidationUtils:
  def validateToNec[E, A](a: => A, ifNotValid: => E)(isValid: A => Boolean): ValidatedNec[E, A] =
    if isValid(a)
    then a.validNec
    else ifNotValid.invalidNec
