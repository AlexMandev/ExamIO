package utils

import cats.syntax.all.*
import doobie.util.meta.Meta
import io.circe.Json
import io.circe.parser.parse
import org.postgresql.util.PGobject

object DoobieUtils:
  given Meta[Json] =
    Meta.Advanced
      .other[PGobject]("json")
      .timap[Json](a => parse(a.getValue).leftMap[Json](e => throw e).merge)(a =>
        val o = new PGobject
        o.setType("json")
        o.setValue(a.noSpaces)
        o
      )
