package wallet.adaptor.typed

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.{ ScalaTestWithActorTestKit, TestProbe }
import org.scalatest.FreeSpecLike
import wallet._
import wallet.adaptor.typed.WalletProtocol._
import wallet.domain.{ Money, WalletId }

/**
  * PesistentActorの単体テスト。
  */
class PersistentWalletAggregateSpec
    extends ScalaTestWithActorTestKit
    with FreeSpecLike
    with ActorSpecSupport
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
    "directly" - {
      "deposit" in {
        val walletId = WalletId(newULID)
        // 永続化アクターを起動
        val walletRef = spawn(PersistentWalletAggregate.behavior(walletId, Int.MaxValue))

        val createWalletResponseProbe = TestProbe[CreateWalletResponse]
        walletRef ! CreateWalletRequest(newULID, walletId, Instant.now, Some(createWalletResponseProbe.ref))
        createWalletResponseProbe.expectMessage(CreateWalletSucceeded)

        val depositResponseProbe = TestProbe[DepositResponse]
        val money                = Money(BigDecimal(100))
        walletRef ! DepositRequest(newULID, walletId, walletId, money, Instant.now, Some(depositResponseProbe.ref))
        depositResponseProbe.expectMessage(DepositSucceeded)

        // アクターを停止する
        killActors(walletRef)

        // 状態を復元する
        val expectedWalletRef = spawn(PersistentWalletAggregate.behavior(walletId, Int.MaxValue))

        val getBalanceResponseProbe = TestProbe[GetBalanceResponse]
        expectedWalletRef ! GetBalanceRequest(newULID, walletId, getBalanceResponseProbe.ref)
        getBalanceResponseProbe.expectMessageType[GetBalanceResponse]
      }
    }
    // TODO: 子アクターが例外を発生した場合に、永続化アクターが停止すること
    "via WalletAggregates" - {
      "deposit" in {
        val walletId = WalletId(newULID)
        // 永続化アクターを起動
        val walletRef = spawn(WalletAggregates.behavior(WalletAggregate.name)(PersistentWalletAggregate.behavior))

        val createWalletResponseProbe = TestProbe[CreateWalletResponse]
        walletRef ! CreateWalletRequest(newULID, walletId, Instant.now, Some(createWalletResponseProbe.ref))
        createWalletResponseProbe.expectMessage(CreateWalletSucceeded)

        val depositResponseProbe = TestProbe[DepositResponse]
        val money                = Money(BigDecimal(100))
        walletRef ! DepositRequest(newULID, walletId, walletId, money, Instant.now, Some(depositResponseProbe.ref))
        depositResponseProbe.expectMessage(DepositSucceeded)

        // アクターを停止する
        killActors(walletRef)

        // 状態を復元する
        val expectedWalletRef = spawn(PersistentWalletAggregate.behavior(walletId, Int.MaxValue))

        val getBalanceResponseProbe = TestProbe[GetBalanceResponse]
        expectedWalletRef ! GetBalanceRequest(newULID, walletId, getBalanceResponseProbe.ref)
        getBalanceResponseProbe.expectMessageType[GetBalanceResponse]
      }
    }
  }

}
