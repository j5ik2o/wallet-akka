package wallet.adaptor.untyped

import java.time.Instant

import akka.testkit.TestProbe
import wallet._
import wallet.adaptor.untyped.WalletProtocol._
import wallet.domain.Money

/**
  * Wallet集約アクターの単体テスト。
  */
class WalletAggregateSpec extends AkkaSpec {

  "WalletAggregate" - {
    // Walletの作成
    "create" in {
      val walletId  = newULID
      val walletRef = system.actorOf(WalletAggregate.props(walletId))

      walletRef ! CreateWalletRequest(newULID, walletId, Instant.now)
      expectMsg(CreateWalletSucceeded)
    }
    // TODO: 残高確認
    // 入金
    "deposit" in {
      val walletId  = newULID
      val walletRef = system.actorOf(WalletAggregate.props(walletId))

      walletRef ! CreateWalletRequest(newULID, walletId, Instant.now)
      expectMsg(CreateWalletSucceeded)

      val money = Money(BigDecimal(100))
      walletRef ! DepositRequest(newULID, walletId, money, Instant.now)
      expectMsg(DepositSucceeded)
    }
    // 請求
    "charge" in {
      val walletId  = newULID
      val walletRef = system.actorOf(WalletAggregate.props(walletId))

      walletRef ! CreateWalletRequest(newULID, walletId, Instant.now)
      expectMsg(CreateWalletSucceeded)

      val requestId = newULID
      val money     = Money(BigDecimal(100))
      walletRef ! ChargeRequest(newULID, requestId, walletId, money, Instant.now)
      expectMsg(ChargeSucceeded$)
    }
    // TODO: 支払い
    // TODO: 取引履歴の確認
    // akka-persistence-queryを使う
    // ドメインイベントの購読
    "addSubscribers" in {
      val walletId    = newULID
      val walletRef   = system.actorOf(WalletAggregate.props(walletId))
      val eventProbes = for (_ <- 1 to 5) yield TestProbe()
      walletRef ! AddSubscribers(newULID, walletId, eventProbes.map(_.ref).toVector)

      walletRef ! CreateWalletRequest(newULID, walletId, Instant.now)
      expectMsg(CreateWalletSucceeded)
      eventProbes.foreach { eventProbe =>
        eventProbe.expectMsgType[WalletCreated].walletId shouldBe walletId
      }
    }
  }
}
