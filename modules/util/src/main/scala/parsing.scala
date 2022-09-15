package io.blindnet.pce
package util

import java.util.regex.Pattern

import io.circe.*

object parsing {

// encoder

  private val snakeCaseTransformation: String => String = s => {
    val basePattern: Pattern = Pattern.compile("([A-Z]+)([A-Z][a-z])")
    val swapPattern: Pattern = Pattern.compile("([a-z\\d])([A-Z])")
    val partial              = basePattern.matcher(s).replaceAll("$1_$2")
    swapPattern.matcher(partial).replaceAll("$1_$2").toLowerCase
  }

  def snakeCaseIfy[A](encoder: Encoder.AsObject[A]): Encoder.AsObject[A] =
    encoder.mapJsonObject(
      obj =>
        JsonObject.fromIterable(obj.toIterable.map {
          case (k, v) => (snakeCaseTransformation(k), v)
        })
    )

// decoder

  private val snakePattern = "_([a-z\\d])".r

  private val snakeToCamel: String => String = s => {
    snakePattern.replaceAllIn(
      s,
      m => m.group(1).toUpperCase()
    )
  }

  private def transform(o: JsonObject): JsonObject =
    JsonObject.fromIterable(o.toVector.map {
      case (k, v) => snakeToCamel(k) -> v
    })

  def unSnakeCaseIfy[A](decoder: Decoder[A]): Decoder[A] = (c: HCursor) => {
    decoder.tryDecode(c.withFocus(_.mapObject(transform)))
  }

}
