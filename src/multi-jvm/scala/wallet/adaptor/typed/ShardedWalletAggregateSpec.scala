package wallet.adaptor.typed
import java.time.Instant

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ ActorIdentity, Identify, Props }
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.persistence.Persistence
import akka.persistence.journal.leveldb.{ SharedLeveldbJournal, SharedLeveldbStore }
import akka.remote.testkit.MultiNodeSpec
import akka.testkit.ImplicitSender
import wallet.adaptor.MultiNodeSampleConfig.{ controller, node1, node2 }
import wallet.adaptor.typed.WalletProtocol._
import wallet.adaptor.{ MultiNodeSampleConfig, STMultiNodeSpecSupport }
import wallet.domain.Money
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

  implicit def typedSystem[T]: ActorSystem[T] = system.toTyped.asInstanceOf[ActorSystem[T]]

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
        ShardedWalletAggregates.initEntityActor(ClusterSharding(typedSystem), 10, 1 hours)
      }
      join(node2, node1) {
        ShardedWalletAggregates.initEntityActor(ClusterSharding(typedSystem), 10, 1 hours)
      }
      enterBarrier("after-2")
    }
    "createWallet" in {
      runOn(node1) {
        val walletId                  = newULID
        val walletRef                 = ClusterSharding(typedSystem).entityRefFor(ShardedWalletAggregates.TypeKey, walletId.toString)
        val createWalletResponseProbe = TestProbe[CreateWalletResponse]
        walletRef ! CreateWalletRequest(newULID, walletId, Instant.now, Some(createWalletResponseProbe.ref))
        createWalletResponseProbe.expectMessage(CreateWalletSucceeded)

        val depositResponseProbe = TestProbe[DepositResponse]
        val money                = Money(BigDecimal(100))
        walletRef ! DepositRequest(newULID, walletId, money, Instant.now, Some(depositResponseProbe.ref))
        depositResponseProbe.expectMessage(DepositSucceeded)

        val getBalanceResponseProbe = TestProbe[GetBalanceResponse]
        walletRef ! GetBalanceRequest(newULID, walletId, getBalanceResponseProbe.ref)
        getBalanceResponseProbe.expectMessageType[GetBalanceResponse]

      }
      enterBarrier("after-3")
      runOn(node2) {
        val walletId                  = newULID
        val walletRef                 = ClusterSharding(typedSystem).entityRefFor(ShardedWalletAggregates.TypeKey, walletId.toString)
        val createWalletResponseProbe = TestProbe[CreateWalletResponse]
        walletRef ! CreateWalletRequest(newULID, walletId, Instant.now, Some(createWalletResponseProbe.ref))
        createWalletResponseProbe.expectMessage(CreateWalletSucceeded)

        val depositResponseProbe = TestProbe[DepositResponse]
        val money                = Money(BigDecimal(100))
        walletRef ! DepositRequest(newULID, walletId, money, Instant.now, Some(depositResponseProbe.ref))
        depositResponseProbe.expectMessage(DepositSucceeded)

        val getBalanceResponseProbe = TestProbe[GetBalanceResponse]
        walletRef ! GetBalanceRequest(newULID, walletId, getBalanceResponseProbe.ref)
        getBalanceResponseProbe.expectMessageType[GetBalanceResponse]
      }
      enterBarrier("after-4")

    }
  }
}
