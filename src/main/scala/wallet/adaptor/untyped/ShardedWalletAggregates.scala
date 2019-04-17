package wallet.adaptor.untyped

import akka.actor._
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import wallet.{ ULID, WalletId }
import wallet.adaptor.Settings
import wallet.adaptor.untyped.WalletProtocol.CommandRequest

object ShardedWalletAggregates {

  def props(requestsLimit: Int, propsF: (ULID, Int) => Props): Props =
    Props(new ShardedWalletAggregates(requestsLimit, propsF))

  def name(id: WalletId): String = id.toString

  val shardName = "wallets"

  case object StopWallet

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: CommandRequest => (cmd.walletId.toString, cmd)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: CommandRequest =>
      val mostSignificantBits  = cmd.walletId.getMostSignificantBits  % 12
      val leastSignificantBits = cmd.walletId.getLeastSignificantBits % 12
      s"$mostSignificantBits:$leastSignificantBits"
  }
}

final class ShardedWalletAggregates(
    requestsLimit: Int,
    propsF: (ULID, Int) => Props
) extends WalletAggregates(requestsLimit, propsF) {
  import ShardedWalletAggregates._

  context.setReceiveTimeout(Settings(context.system).passivateTimeout)

  override def unhandled(message: Any): Unit = message match {
    case ReceiveTimeout =>
      log.debug("ReceiveTimeout")
      context.parent ! Passivate(stopMessage = StopWallet)
    case StopWallet =>
      log.debug("StopWallet")
      context.stop(self)
  }
}
