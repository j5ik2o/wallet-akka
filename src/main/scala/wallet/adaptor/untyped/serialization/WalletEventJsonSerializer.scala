package wallet.adaptor.untyped.serialization

import akka.actor.ExtendedActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.serialization.SerializerWithStringManifest
import wallet.adaptor.serialization.CirceJsonSerialization
import wallet.adaptor.untyped.WalletProtocol.{ WalletCharged, WalletCreated, WalletDeposited, WalletWithdrawed }
import wallet.adaptor.untyped.json.{ WalletChargedJson, WalletCreatedJson, WalletDepositedJson, WalletWithdrawedJson }

class WalletEventJsonSerializer(system: ExtendedActorSystem) extends SerializerWithStringManifest {
  import io.circe.generic.auto._
  import wallet.adaptor.untyped.json.WalletChargedJson._
  import wallet.adaptor.untyped.json.WalletCreatedJson._
  import wallet.adaptor.untyped.json.WalletDepositedJson._
  import wallet.adaptor.untyped.json.WalletWithdrawedJson._
  final val WalletCreatedManifest     = classOf[WalletCreated].getName
  final val WalletDepositedManifest   = classOf[WalletDeposited].getName
  final val WalletChargedManifest     = classOf[WalletCharged].getName
  final val WalletWithdraweddManifest = classOf[WalletWithdrawed].getName

  private implicit val log: LoggingAdapter = Logging.getLogger(system, getClass)

  override def identifier: Int = 1

  override def manifest(o: AnyRef): String = o.getClass.getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case e: WalletCreated    => CirceJsonSerialization.toBinary(e, log.isDebugEnabled)
    case e: WalletDeposited  => CirceJsonSerialization.toBinary(e, log.isDebugEnabled)
    case e: WalletCharged    => CirceJsonSerialization.toBinary(e, log.isDebugEnabled)
    case e: WalletWithdrawed => CirceJsonSerialization.toBinary(e, log.isDebugEnabled)
    case x                   => throw new NotImplementedError(s"Cannot serialize: ${x.toString}")
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest match {
    case WalletCreatedManifest =>
      CirceJsonSerialization.fromBinary[WalletCreated, WalletCreatedJson](bytes, log.isDebugEnabled)
    case WalletDepositedManifest =>
      CirceJsonSerialization.fromBinary[WalletDeposited, WalletDepositedJson](bytes, log.isDebugEnabled)
    case WalletChargedManifest =>
      CirceJsonSerialization.fromBinary[WalletCharged, WalletChargedJson](bytes, log.isDebugEnabled)
    case WalletWithdraweddManifest =>
      CirceJsonSerialization.fromBinary[WalletWithdrawed, WalletWithdrawedJson](bytes, log.isDebugEnabled)
    case x => throw new NotImplementedError(s"Cannot deserialize: ${x.toString}")
  }
}
