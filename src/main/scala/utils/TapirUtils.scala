package utils

import io.circe.Decoder.Result
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json, JsonObject}
import sttp.tapir.Schema.SName
import sttp.tapir.{EndpointIO, FieldName, Schema, SchemaType}
import sttp.tapir.generic.Configuration as TapirConfiguration
import sttp.tapir.json.circe.jsonBody

import scala.reflect.ClassTag

def jsonBodyTypedError[T: {Encoder.AsObject as encoder, Decoder, Schema as tSchema, ClassTag as classTag}](
  using tc: TapirConfiguration
): EndpointIO.Body[String, T] =
  val typeClassName = classTag.runtimeClass.getSimpleName
  val typeName = tc.toDiscriminatorValue(SName(typeClassName))
  val typeDiscriminator = tc.discriminator.getOrElse("type")

  val extendedEncoder = new Encoder.AsObject[T]:
    def encodeObject(a: T): JsonObject =
      encoder
        .encodeObject(a)
        .add(typeDiscriminator, Json.fromString(typeName))

  val extendedDecoder = new Decoder[T]:
    def apply(c: HCursor): Result[T] =
      for
        _ <- c
          .downField(typeDiscriminator)
          .as[String]
          .filterOrElse(
            _ == typeName,
            DecodingFailure(s"'$typeDiscriminator' must be $typeName", c.downField(typeDiscriminator).history)
          )
        value <- c.as[T]
      yield value
    end apply

  val extendedSchemaType = tSchema.schemaType match
    case product: SchemaType.SProduct[T] =>
      product.copy(fields =
        product.fields ++ List(
          SchemaType.SProductField(
            FieldName(typeDiscriminator, typeDiscriminator),
            Schema.schemaForString.encodedDiscriminatorValue(typeName),
            _ => Some(typeName)
          )
        )
      )
    case _ => throw new RuntimeException("Schema must be SProduct")
  val extendedSchema = tSchema.copy(schemaType = extendedSchemaType)

  jsonBody[T](using extendedEncoder, extendedDecoder, extendedSchema)
end jsonBodyTypedError
