package wallet.adaptor.untyped

import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings }
import wallet.adaptor.untyped.WalletProtocol.CommandRequest

object ShardedWalletAggregatesRegion {
  def props = Props(new ShardedWalletAggregatesRegion)
  def name  = "sharded-wallet-aggregates-proxy"

  def startClusterSharding(system: ActorSystem)(requestLimit: Int): ActorRef =
    ClusterSharding(system).start(
      ShardedWalletAggregates.shardName,
      ShardedWalletAggregates.props(requestLimit, PersistentWalletAggregate.props),
      ClusterShardingSettings(system),
      ShardedWalletAggregates.extractEntityId,
      ShardedWalletAggregates.extractShardId
    )

  def getShardRegion(system: ActorSystem): ActorRef =
    ClusterSharding(system).shardRegion(ShardedWalletAggregates.shardName)

}

class ShardedWalletAggregatesRegion extends Actor {

  override def receive: Receive = {
    case cmd: CommandRequest =>
      ShardedWalletAggregatesRegion.getShardRegion(context.system) forward cmd
  }
}
