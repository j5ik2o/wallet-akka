package wallet.adaptor.typed

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityContext, EntityTypeKey }
import wallet.adaptor.typed.WalletProtocol.{ CommandRequest, Idle, Stop }

import scala.concurrent.duration.FiniteDuration

/**
  * WalletAggregatesアクターをクラスターシャーディングするアクター。
  */
object ShardedWalletAggregates {

  val TypeKey: EntityTypeKey[CommandRequest] = EntityTypeKey[CommandRequest]("wallets")

  private def behavior(chargesLimit: Int, receiveTimeout: FiniteDuration): EntityContext => Behavior[CommandRequest] = {
    entityContext =>
      Behaviors.setup[CommandRequest] { ctx =>
        val childRef = ctx.spawn(
          WalletAggregates.behavior(WalletAggregate.name, chargesLimit)(PersistentWalletAggregate.behavior),
          name = WalletAggregates.name
        )
        ctx.setReceiveTimeout(receiveTimeout, Idle)
        Behaviors.receiveMessage[CommandRequest] {
          case Idle =>
            entityContext.shard ! ClusterSharding.Passivate(ctx.self)
            Behaviors.same
          case Stop =>
            Behaviors.stopped
          case msg =>
            childRef ! msg
            Behaviors.same
        }
      }
  }

  /**
    *
    *
    * @param clusterSharding [[ClusterSharding]]
    * @param chargesLimit
    * @param receiveTimeout
    * @return
    */
  def initEntityActor(
      clusterSharding: ClusterSharding,
      chargesLimit: Int,
      receiveTimeout: FiniteDuration
  ): ActorRef[ShardingEnvelope[CommandRequest]] =
    clusterSharding.init(
      Entity(typeKey = TypeKey, createBehavior = behavior(chargesLimit, receiveTimeout)).withStopMessage(Stop)
    )

}
