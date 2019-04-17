package wallet.adaptor.untyped

import java.time.Instant

import akka.actor.{ ActorRef, ActorSystem }
import akka.testkit.{ ImplicitSender, TestKit }
import org.scalatest.{ BeforeAndAfterAll, FreeSpecLike, Matchers }
import wallet.adaptor.untyped.WalletProtocol._
import wallet.domain.Money
import wallet.utils.ULID

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
      val walletId = ULID.generate
      // 永続化アクターを起動
      val walletRef = system.actorOf(PersistentWalletAggregate.props(walletId, 1 hours))

      walletRef ! CreateWalletRequest(ULID.generate, walletId)
      expectMsg(CreateWalletSucceeded)

      val money = Money(BigDecimal(100))
      walletRef ! DepositRequest(ULID.generate, walletId, money, Instant.now)
      expectMsg(DepositSucceeded)

      // アクターを停止する
      killActors(walletRef)

      // 状態を復元する
      val expectedWalletRef = system.actorOf(PersistentWalletAggregate.props(walletId, 1 hours))

      expectedWalletRef ! GetBalanceRequest(ULID.generate, walletId)
      expectMsgType[GetBalanceResponse]

    }
  }

}
