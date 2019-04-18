package wallet.adaptor.typed

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityTypeKey }
import wallet.adaptor.typed.WalletProtocol.{ CommandRequest, Idle, Stop }

import scala.concurrent.duration.FiniteDuration

object ShardedWalletAggregates {

  val TypeKey = EntityTypeKey[CommandRequest]("wallets")

  def initClusterSharding(
      clusterSharding: ClusterSharding,
      requestLimit: Int,
      receiveTimeout: FiniteDuration
  ): ActorRef[ShardingEnvelope[CommandRequest]] = {
    val shardRegion: ActorRef[ShardingEnvelope[CommandRequest]] =
      clusterSharding.init(
        Entity(
          typeKey = TypeKey,
          createBehavior = { entityContext =>
            Behaviors.setup[CommandRequest] {
              ctx =>
                val childRef = ctx.spawn(
                  WalletAggregates.behavior(requestLimit)(PersistentWalletAggregate.behavior),
                  name = WalletAggregates.name
                )
                ctx.watch(childRef)
                ctx.setReceiveTimeout(receiveTimeout, Idle)
                Behaviors.receiveMessage[CommandRequest] {
                  case Idle =>
                    entityContext.shard ! ClusterSharding.Passivate(ctx.self)
                    Behaviors.same
                  case Stop =>
                    Behaviors.stopped
                  case msg =>
                    ctx.log.debug(s"msg = $msg")
                    childRef ! msg
                    Behaviors.same
                }
            }
          }
        ).withStopMessage(Stop)
      )
    shardRegion
  }

}
