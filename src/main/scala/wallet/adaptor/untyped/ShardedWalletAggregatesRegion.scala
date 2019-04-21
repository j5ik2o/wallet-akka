package wallet.adaptor.untyped

import akka.actor.{ ActorRef, ActorSystem }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings }

object ShardedWalletAggregatesRegion {

  def startClusterSharding(chargesLimit: Int)(implicit system: ActorSystem): ActorRef =
    ClusterSharding(system).start(
      ShardedWalletAggregates.shardName,
      ShardedWalletAggregates.props(chargesLimit, PersistentWalletAggregate.props),
      ClusterShardingSettings(system),
      ShardedWalletAggregates.extractEntityId,
      ShardedWalletAggregates.extractShardId
    )

  def shardRegion(implicit system: ActorSystem): ActorRef =
    ClusterSharding(system).shardRegion(ShardedWalletAggregates.shardName)

}
