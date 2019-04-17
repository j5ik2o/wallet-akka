package wallet.adaptor.untyped

import akka.actor.{ ActorIdentity, Identify, Props }
import akka.cluster.Cluster
import akka.persistence.Persistence
import akka.persistence.journal.leveldb.{ SharedLeveldbJournal, SharedLeveldbStore }
import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeSpec
import akka.testkit.ImplicitSender
import wallet._
import wallet.adaptor.untyped.WalletProtocol.{ CreateWalletRequest, CreateWalletSucceeded }
import wallet.adaptor.{ MultiNodeSampleConfig, STMultiNodeSpecSupport }

import scala.concurrent.duration._

class ShardedWalletAggregateMultiJvmNode1 extends ShardedWalletAggregateSpec
class ShardedWalletAggregateMultiJvmNode2 extends ShardedWalletAggregateSpec
class ShardedWalletAggregateMultiJvmNode3 extends ShardedWalletAggregateSpec

class ShardedWalletAggregateSpec
    extends MultiNodeSpec(MultiNodeSampleConfig)
    with STMultiNodeSpecSupport
    with ImplicitSender {
  import MultiNodeSampleConfig._
  override def initialParticipants: Int = roles.size

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
      join(node1, node1) {
        ShardedWalletAggregatesRegion.startClusterSharding(system)(10)
      }
      join(node2, node1) {
        ShardedWalletAggregatesRegion.startClusterSharding(system)(10)
      }
      enterBarrier("after-2")
    }
    "createWallet" in {
      runOn(node1) {
        val region   = ShardedWalletAggregatesRegion.getShardRegion(system)
        val walletId = newULID
        region ! CreateWalletRequest(newULID, walletId)
        expectMsg(CreateWalletSucceeded)
      }
      enterBarrier("after-3")
      runOn(node2) {
        val region   = ShardedWalletAggregatesRegion.getShardRegion(system)
        val walletId = newULID
        region ! CreateWalletRequest(newULID, walletId)
        expectMsg(CreateWalletSucceeded)
      }
      enterBarrier("after-4")
    }
  }

}