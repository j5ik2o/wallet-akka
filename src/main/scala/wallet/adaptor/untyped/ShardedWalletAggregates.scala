package wallet.adaptor.untyped

import akka.actor._
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import wallet.WalletId
import wallet.adaptor.Settings
import wallet.adaptor.untyped.WalletProtocol.CommandRequest

object ShardedWalletAggregates {

  def props(chargesLimit: Int, propsF: (WalletId, Int) => Props): Props =
    Props(new ShardedWalletAggregates(chargesLimit, propsF))

  def name(id: WalletId): String = id.value.toString

  val shardName = "wallets"

  case object StopWallet

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: CommandRequest => (cmd.walletId.value.toString, cmd)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: CommandRequest =>
      val mostSignificantBits  = cmd.walletId.getMostSignificantBits  % 12
      val leastSignificantBits = cmd.walletId.getLeastSignificantBits % 12
      s"$mostSignificantBits:$leastSignificantBits"
  }
}

final class ShardedWalletAggregates(
    chargesLimit: Int,
    propsF: (WalletId, Int) => Props
) extends WalletAggregates(chargesLimit, propsF) {
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
