package wallet.adaptor.typed

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.{ ScalaTestWithActorTestKit, TestProbe }
import akka.actor.typed.ActorRef
import org.scalatest._
import wallet._
import wallet.adaptor.typed.WalletProtocol._
import wallet.domain.{ Balance, ChargeId, Money, WalletId }

/**
  * Wallet集約アクターの単体テスト。
  */
class WalletAggregateSpec extends ScalaTestWithActorTestKit with FreeSpecLike with Matchers {

  def newWalletRef(id: WalletId): ActorRef[CommandRequest] = spawn(WalletAggregate.behavior(id))

  "WalletAggregate" - {
    // 作成
    "create" in {
      val walletId                  = WalletId(newULID)
      val walletRef                 = newWalletRef(walletId)
      val createWalletResponseProbe = TestProbe[CreateWalletResponse]

      walletRef ! CreateWalletRequest(newULID, walletId, Instant.now, Some(createWalletResponseProbe.ref))
      createWalletResponseProbe.expectMessage(CreateWalletSucceeded)

      val getBalanceResponseProbe = TestProbe[GetBalanceResponse]
      walletRef ! GetBalanceRequest(newULID, walletId, getBalanceResponseProbe.ref)
      getBalanceResponseProbe.expectMessage(GetBalanceResponse(Balance.zero))
    }
    // 入金
    "deposit" in {
      val walletId  = WalletId(newULID)
      val walletRef = newWalletRef(walletId)

      val createWalletResponseProbe = TestProbe[CreateWalletResponse]
      walletRef ! CreateWalletRequest(newULID, walletId, Instant.now, Some(createWalletResponseProbe.ref))
      createWalletResponseProbe.expectMessage(CreateWalletSucceeded)

      val depositRequestProbe = TestProbe[DepositResponse]
      val money               = Money(BigDecimal(100))
      walletRef ! DepositRequest(newULID, walletId, walletId, money, Instant.now, Some(depositRequestProbe.ref))
      depositRequestProbe.expectMessage(DepositSucceeded)

      val getBalanceResponseProbe = TestProbe[GetBalanceResponse]
      walletRef ! GetBalanceRequest(newULID, walletId, getBalanceResponseProbe.ref)
      getBalanceResponseProbe.expectMessage(GetBalanceResponse(Balance(money)))
    }
    // 残高確認
    "get balance" in {
      val walletId  = WalletId(newULID)
      val walletRef = newWalletRef(walletId)

      val createWalletResponseProbe = TestProbe[CreateWalletResponse]
      walletRef ! CreateWalletRequest(newULID, walletId, Instant.now, Some(createWalletResponseProbe.ref))
      createWalletResponseProbe.expectMessage(CreateWalletSucceeded)

      val depositResponseProbe = TestProbe[DepositResponse]
      val money                = Money(BigDecimal(100))
      walletRef ! DepositRequest(newULID, walletId, walletId, money, Instant.now, Some(depositResponseProbe.ref))
      depositResponseProbe.expectMessage(DepositSucceeded)

      val getBalanceResponseProbe = TestProbe[GetBalanceResponse]
      walletRef ! GetBalanceRequest(newULID, walletId, getBalanceResponseProbe.ref)
      getBalanceResponseProbe.expectMessage(GetBalanceResponse(Balance(money)))
    }
    // 請求
    "charge" in {
      val walletId  = WalletId(newULID)
      val walletRef = newWalletRef(walletId)

      val createWalletResponseProbe = TestProbe[CreateWalletResponse]
      walletRef ! CreateWalletRequest(newULID, walletId, Instant.now, Some(createWalletResponseProbe.ref))
      createWalletResponseProbe.expectMessage(CreateWalletSucceeded)

      val chargeId            = ChargeId(newULID)
      val toWalletId          = WalletId(newULID)
      val money               = Money(BigDecimal(100))
      val requestRequestProbe = TestProbe[ChargeResponse]
      walletRef ! ChargeRequest(
        newULID,
        walletId,
        toWalletId,
        chargeId,
        money,
        Instant.now,
        Some(requestRequestProbe.ref)
      )
      requestRequestProbe.expectMessage(ChargeSucceeded)

      val getBalanceResponseProbe = TestProbe[GetBalanceResponse]
      walletRef ! GetBalanceRequest(newULID, walletId, getBalanceResponseProbe.ref)
      getBalanceResponseProbe.expectMessage(GetBalanceResponse(Balance.zero))
    }
    // ドメインイベントの購読
    "addSubscribers" in {
      val walletId  = WalletId(newULID)
      val walletRef = newWalletRef(walletId)

      val eventProbes = for (_ <- 1 to 5) yield TestProbe[Event]
      walletRef ! AddSubscribers(newULID, walletId, eventProbes.map(_.ref).toVector)

      val probe = TestProbe[CreateWalletResponse]
      walletRef ! CreateWalletRequest(newULID, walletId, Instant.now, Some(probe.ref))
      probe.expectMessage(CreateWalletSucceeded)
      eventProbes.foreach { eventProbe =>
        eventProbe.expectMessageType[WalletCreated].walletId shouldBe walletId
      }
    }
  }

}
