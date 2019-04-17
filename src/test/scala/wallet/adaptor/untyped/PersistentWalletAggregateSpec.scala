package wallet.adaptor.untyped

import java.time.Instant

import akka.actor.{ ActorRef, ActorSystem }
import akka.testkit.{ ImplicitSender, TestKit }
import org.scalatest.{ BeforeAndAfterAll, FreeSpecLike, Matchers }
import wallet.adaptor.untyped.WalletProtocol._
import wallet.domain.Money
import wallet._

import scala.concurrent.duration._

class PersistentWalletAggregateSpec
    extends TestKit(ActorSystem("PersistentWalletAggregateSpec"))
    with FreeSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender
    with ActorTestSupport
    with PersistenceCleanup {

  override protected def beforeAll(): Unit = deleteStorageLocations()

  override protected def afterAll(): Unit = {
    deleteStorageLocations()
    TestKit.shutdownActorSystem(system)
  }

  "PersistentWalletAggregate" - {
    "deposit" in {
      val walletId = newULID
      // 永続化アクターを起動
      val walletRef = system.actorOf(PersistentWalletAggregate.props(walletId))

      walletRef ! CreateWalletRequest(newULID, walletId)
      expectMsg(CreateWalletSucceeded)

      val money = Money(BigDecimal(100))
      walletRef ! DepositRequest(newULID, walletId, money, Instant.now)
      expectMsg(DepositSucceeded)

      // アクターを停止する
      killActors(walletRef)

      // 状態を復元する
      val expectedWalletRef = system.actorOf(PersistentWalletAggregate.props(walletId))

      expectedWalletRef ! GetBalanceRequest(newULID, walletId)
      expectMsgType[GetBalanceResponse]

    }
    // TODO: 子アクターが例外を発生した場合に、永続化アクターが停止すること
  }

}
