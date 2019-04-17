package wallet.adaptor.typed

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.{ ScalaTestWithActorTestKit, TestProbe }
import org.scalatest.{ FreeSpecLike, Matchers }
import wallet.adaptor.typed.WalletProtocol._
import wallet.domain.Money
import wallet.utils.ULID

import scala.concurrent.duration._

class PersistentWalletAggregateSpec
    extends ScalaTestWithActorTestKit
    with FreeSpecLike
    with Matchers
    with ActorTestSupport
    with PersistenceCleanup {

  override protected def beforeAll(): Unit = {
    deleteStorageLocations()
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    deleteStorageLocations()
    super.afterAll()
  }

  "PersistentWalletAggregate" - {
    "deposit" in {
      val walletId = ULID.generate
      // 永続化アクターを起動
      val walletRef = spawn(PersistentWalletAggregate.behavior(walletId, 1 hours))

      val createWalletResponseProbe = TestProbe[CreateWalletResponse]
      walletRef ! CreateWalletRequest(ULID.generate, walletId, Some(createWalletResponseProbe.ref))
      createWalletResponseProbe.expectMessage(CreateWalletSucceeded)

      val depositResponseProbe = TestProbe[DepositResponse]
      val money                = Money(BigDecimal(100))
      walletRef ! DepositRequest(ULID.generate, walletId, money, Instant.now, Some(depositResponseProbe.ref))
      depositResponseProbe.expectMessage(DepositSucceeded)

      // アクターを停止する
      killActors(walletRef)

      // 状態を復元する
      val expectedWalletRef = spawn(PersistentWalletAggregate.behavior(walletId, 1 hours))

      val getBalanceResponseProbe = TestProbe[GetBalanceResponse]
      expectedWalletRef ! GetBalanceRequest(ULID.generate, walletId, getBalanceResponseProbe.ref)
      getBalanceResponseProbe.expectMessageType[GetBalanceResponse]

    }
  }

}
