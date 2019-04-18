package wallet.adaptor.typed
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.actor.{ ActorIdentity, Identify, Props }
import akka.cluster.Cluster
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.persistence.Persistence
import akka.persistence.journal.leveldb.{ SharedLeveldbJournal, SharedLeveldbStore }
import akka.remote.testkit.MultiNodeSpec
import akka.testkit.ImplicitSender
import wallet.adaptor.MultiNodeSampleConfig.{ controller, node1, node2 }
import wallet.adaptor.typed.WalletProtocol.{
  CommandRequest,
  CreateWalletRequest,
  CreateWalletResponse,
  CreateWalletSucceeded
}
import wallet.adaptor.{ MultiNodeSampleConfig, STMultiNodeSpecSupport }
import wallet.newULID

import scala.concurrent.duration._

class ShardedWalletAggregateMultiJvmNode1 extends ShardedWalletAggregateSpec
class ShardedWalletAggregateMultiJvmNode2 extends ShardedWalletAggregateSpec
class ShardedWalletAggregateMultiJvmNode3 extends ShardedWalletAggregateSpec

class ShardedWalletAggregateSpec
    extends MultiNodeSpec(MultiNodeSampleConfig)
    with STMultiNodeSpecSupport
    with ImplicitSender {

  override def initialParticipants: Int = roles.size

  def typedSystem[T]: ActorSystem[T] = system.toTyped.asInstanceOf[ActorSystem[T]]

  var node1Ref: ActorRef[ShardingEnvelope[CommandRequest]] = _
  var node2Ref: ActorRef[ShardingEnvelope[CommandRequest]] = _

  "ShardedWalletAggregate" - {
    "setup shared journal" in {
      Persistence(system)
      runOn(controller) {
        system.actorOf(Props[SharedLeveldbStore], "store")
      }
      enterBarrier("persistence-started")
      runOn(node1, node2) {
        system.actorSelection(node(controller) / "user" / "store") ! Identify(None)
        val sharedStore = expectMsgType[ActorIdentity].ref.get
        SharedLeveldbJournal.setStore(sharedStore, system)
      }
      enterBarrier("after-1")
    }
    "join cluster" in within(15 seconds) {
      join(node1, node1) {}
      join(node2, node1) {}
      enterBarrier("after-2")
    }
    "createWallet" in {
      runOn(node1) {
        val sharding: ClusterSharding = ShardedWalletAggregates.newClusterSharding(typedSystem)
        ShardedWalletAggregates.initClusterSharding(sharding, 10, 1 hours)

        val probe     = TestProbe[CreateWalletResponse]()(typedSystem)
        val walletId  = newULID
        val entityRef = sharding.entityRefFor(ShardedWalletAggregates.TypeKey, walletId.toString)
        entityRef ! CreateWalletRequest(newULID, walletId, Some(probe.ref))
        probe.expectMessage(CreateWalletSucceeded)
      }
      enterBarrier("after-3")
//      runOn(node2) {
//        val sharding = ShardedWalletAggregates.newClusterSharding(typedSystem)
//        ShardedWalletAggregates.initClusterSharding(sharding, 10, 1 hours)
//        val probe     = TestProbe[CreateWalletResponse]()(typedSystem)
//        val walletId  = newULID
//        val entityRef = sharding.entityRefFor(ShardedWalletAggregates.TypeKey, walletId.toString)
//        entityRef ! CreateWalletRequest(newULID, walletId, Some(probe.ref))
//        probe.expectMessage(CreateWalletSucceeded)
//      }
//      enterBarrier("after-4")

    }
  }
}
