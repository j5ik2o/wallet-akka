package wallet.adaptor.untyped

import java.time.Instant

import akka.actor.ActorRef
import akka.testkit.TestProbe
import wallet._
import wallet.adaptor.untyped.WalletProtocol._
import wallet.domain.{ Balance, Money }

/**
  * Wallet集約アクターの単体テスト。
  */
class WalletAggregateSpec extends AkkaSpec {

  def newWalletRef(id: WalletId): ActorRef = system.actorOf(WalletAggregate.props(id))

  "WalletAggregate" - {
    // 作成
    "create" in {
      val walletId  = newULID
      val walletRef = newWalletRef(walletId)

      walletRef ! CreateWalletRequest(newULID, walletId, Instant.now)
      expectMsg(CreateWalletSucceeded)

      walletRef ! GetBalanceRequest(newULID, walletId)
      expectMsg(GetBalanceResponse(Balance.zero))
    }
    // 入金
    "deposit" in {
      val walletId  = newULID
      val walletRef = newWalletRef(walletId)

      walletRef ! CreateWalletRequest(newULID, walletId, Instant.now)
      expectMsg(CreateWalletSucceeded)

      val money = Money(BigDecimal(100))
      walletRef ! DepositRequest(newULID, walletId, money, Instant.now)
      expectMsg(DepositSucceeded)

      walletRef ! GetBalanceRequest(newULID, walletId)
      expectMsg(GetBalanceResponse(Balance(money)))
    }
    // 残高確認
    "get balance" in {
      val walletId  = newULID
      val walletRef = newWalletRef(walletId)

      walletRef ! CreateWalletRequest(newULID, walletId, Instant.now)
      expectMsg(CreateWalletSucceeded)

      val money = Money(BigDecimal(100))
      walletRef ! DepositRequest(newULID, walletId, money, Instant.now)
      expectMsg(DepositSucceeded)

      walletRef ! GetBalanceRequest(newULID, walletId)
      expectMsg(GetBalanceResponse(Balance(money)))
    }
    // 請求
    "charge" in {
      val walletId  = newULID
      val walletRef = newWalletRef(walletId)

      walletRef ! CreateWalletRequest(newULID, walletId, Instant.now)
      expectMsg(CreateWalletSucceeded)

      val chargeId = newULID
      val money    = Money(BigDecimal(100))
      walletRef ! ChargeRequest(newULID, chargeId, walletId, money, Instant.now)
      expectMsg(ChargeSucceeded)

      walletRef ! GetBalanceRequest(newULID, walletId)
      expectMsg(GetBalanceResponse(Balance(Money.zero)))
    }
    // 支払
    "pay" in {
      val walletId  = newULID
      val walletRef = newWalletRef(walletId)

      walletRef ! CreateWalletRequest(newULID, walletId, Instant.now)
      expectMsg(CreateWalletSucceeded)

      val money = Money(BigDecimal(100))
      walletRef ! DepositRequest(newULID, walletId, money, Instant.now)
      expectMsg(DepositSucceeded)

      val toWalletId = newULID
      walletRef ! PayRequest(newULID, walletId, toWalletId, money, None, Instant.now)
      expectMsg(PaySucceeded)

      walletRef ! GetBalanceRequest(newULID, walletId)
      expectMsg(GetBalanceResponse(Balance(Money.zero)))
    }
    // TODO: 取引履歴の確認(akka-persistence-query)を使う
    // ドメインイベントの購読
    "addSubscribers" in {
      val walletId  = newULID
      val walletRef = newWalletRef(walletId)

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
