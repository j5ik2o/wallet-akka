package wallet.adaptor.serialization

import java.nio.charset.StandardCharsets

import akka.event.LoggingAdapter
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import monocle.Iso

object StringToByteConversion {

  implicit class StringToByte(text: String) {
    def toUTF8Byte: Array[Byte] = text.getBytes(StandardCharsets.UTF_8)
  }

}

class CirceDeserializationException(message: String, cause: Throwable) extends Exception(message, cause)

object CirceJsonSerialization {

  import StringToByteConversion._

  def toBinary[Event, JsonRepr](
      orig: Event,
      isDebugEnabled: Boolean = false
  )(implicit iso: Iso[Event, JsonRepr], encoder: Encoder[JsonRepr], log: LoggingAdapter): Array[Byte] = {
    val event      = iso.get(orig)
    val jsonString = event.asJson.noSpaces
    if (isDebugEnabled)
      log.debug(s"toBinary: jsonString = $jsonString")
    jsonString.toUTF8Byte
  }

  def fromBinary[Event, JsonRepr](
      bytes: Array[Byte],
      isDebugEnabled: Boolean = false
  )(implicit iso: Iso[Event, JsonRepr], d: Decoder[JsonRepr], log: LoggingAdapter): Event = {
    val jsonString = new String(bytes, StandardCharsets.UTF_8)
    if (isDebugEnabled)
      log.debug(s"fromBinary: jsonString = $jsonString")
    val result = for {
      json       <- parse(jsonString).right
      resultJson <- json.as[JsonRepr].right
    } yield iso.reverseGet(resultJson)
    result match {
      case Left(failure) => throw new CirceDeserializationException(failure.getMessage, failure)
      case Right(event)  => event
    }
  }

}
