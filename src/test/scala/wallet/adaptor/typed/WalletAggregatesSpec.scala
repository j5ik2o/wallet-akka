package wallet.adaptor.typed

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.{ ScalaTestWithActorTestKit, TestProbe }
import org.scalatest.{ FreeSpecLike, Matchers }
import wallet.adaptor.typed.WalletProtocol._
import wallet.domain.Money
import wallet._

class WalletAggregatesSpec extends ScalaTestWithActorTestKit with FreeSpecLike with Matchers {

  "WalletAggregate" - {
    "create" in {
      val walletRef = spawn(WalletAggregates.behavior()(WalletAggregate.behavior))

      val createWalletResponseProbe = TestProbe[CreateWalletResponse]
      val walletId                  = newULID
      walletRef ! CreateWalletRequest(newULID, walletId, Instant.now, Some(createWalletResponseProbe.ref))
      createWalletResponseProbe.expectMessage(CreateWalletSucceeded)
    }
    "addSubscribers" in {
      val walletRef = spawn(WalletAggregates.behavior()(WalletAggregate.behavior))

      val walletId    = newULID
      val eventProbes = for (_ <- 1 to 5) yield TestProbe[Event]
      walletRef ! AddSubscribers(newULID, walletId, eventProbes.map(_.ref).toVector)

      val probe = TestProbe[CreateWalletResponse]
      walletRef ! CreateWalletRequest(newULID, walletId, Instant.now, Some(probe.ref))
      probe.expectMessage(CreateWalletSucceeded)
      eventProbes.foreach { eventProbe =>
        eventProbe.expectMessageType[WalletCreated].walletId shouldBe walletId
      }
    }
    "deposit" in {
      val walletRef = spawn(WalletAggregates.behavior()(WalletAggregate.behavior))

      val walletId                  = newULID
      val createWalletResponseProbe = TestProbe[CreateWalletResponse]
      walletRef ! CreateWalletRequest(newULID, walletId, Instant.now, Some(createWalletResponseProbe.ref))
      createWalletResponseProbe.expectMessage(CreateWalletSucceeded)

      val money               = Money(BigDecimal(100))
      val depositRequestProbe = TestProbe[DepositResponse]
      walletRef ! DepositRequest(newULID, walletId, money, Instant.now, Some(depositRequestProbe.ref))
      depositRequestProbe.expectMessage(DepositSucceeded)
    }
    "charge" in {
      val walletRef = spawn(WalletAggregates.behavior()(WalletAggregate.behavior))

      val walletId                  = newULID
      val createWalletResponseProbe = TestProbe[CreateWalletResponse]
      walletRef ! CreateWalletRequest(newULID, walletId, Instant.now, Some(createWalletResponseProbe.ref))
      createWalletResponseProbe.expectMessage(CreateWalletSucceeded)

      val chargeId            = newULID
      val money               = Money(BigDecimal(100))
      val requestRequestProbe = TestProbe[ChargeResponse]
      walletRef ! ChargeRequest(newULID, chargeId, walletId, money, Instant.now, Some(requestRequestProbe.ref))
      requestRequestProbe.expectMessage(ChargeSucceeded$)
    }
  }

}
