package wallet.adaptor.untyped

import java.time.Instant

import wallet._
import wallet.adaptor.untyped.WalletProtocol._
import wallet.domain.{ Money, WalletId }

/**
  * PesistentActorの単体テスト。
  */
class PersistentWalletAggregateSpec extends AkkaSpec with PersistenceCleanup {

  override protected def atStartup(): Unit = deleteStorageLocations()

  override protected def beforeTermination(): Unit = deleteStorageLocations()

  "PersistentWalletAggregate" - {
    "directly" - {
      "deposit" in {
        val walletId = WalletId(newULID)
        // 永続化アクターを起動
        val walletRef = system.actorOf(PersistentWalletAggregate.props(walletId))

        walletRef ! CreateWalletRequest(newULID, walletId, Instant.now)
        expectMsg(CreateWalletSucceeded)

        val money = Money(BigDecimal(100))
        walletRef ! DepositRequest(newULID, walletId, money, Instant.now)
        expectMsg(DepositSucceeded)

        walletRef ! GetBalanceRequest(newULID, walletId)
        expectMsgType[GetBalanceResponse]

        // アクターを停止する
        killActors(walletRef)

        // 状態を復元する
        val expectedWalletRef = system.actorOf(PersistentWalletAggregate.props(walletId))

        expectedWalletRef ! GetBalanceRequest(newULID, walletId)
        expectMsgType[GetBalanceResponse]

      }
      // TODO: 子アクターが例外を発生した場合に、永続化アクターが停止すること
    }
    "via WalletAggregates" - {
      "deposit" in {
        val walletId = WalletId(newULID)
        // 永続化アクターを起動
        val walletRef = system.actorOf(WalletAggregates.props()(PersistentWalletAggregate.props))

        walletRef ! CreateWalletRequest(newULID, walletId, Instant.now)
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
    }
  }

}
