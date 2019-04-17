package wallet.adaptor.typed

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.{ ScalaTestWithActorTestKit, TestProbe }
import wallet.adaptor.typed.WalletProtocol._
import wallet.domain.Money
import wallet.utils.ULID

import scala.concurrent.duration._
import org.scalatest._

class WalletAggregateSpec extends ScalaTestWithActorTestKit with FreeSpecLike with Matchers {

  "WalletAggregate" - {
    "create" in {
      val walletRef                 = spawn(WalletAggregate.behavior(1 hours))
      val createWalletResponseProbe = TestProbe[CreateWalletResponse]

      val id = ULID.generate
      walletRef ! CreateWalletRequest(id, createWalletResponseProbe.ref)
      createWalletResponseProbe.expectMessage(CreateWalletSucceeded)
    }
    "addSubscribers" in {
      val walletRef   = spawn(WalletAggregate.behavior(1 hours))
      val eventProbes = for (_ <- 1 to 5) yield TestProbe[Event]
      walletRef ! AddSubscribers(eventProbes.map(_.ref).toVector)

      val probe = TestProbe[CreateWalletResponse]
      val id    = ULID.generate
      walletRef ! CreateWalletRequest(id, probe.ref)
      probe.expectMessage(CreateWalletSucceeded)
      eventProbes.foreach { eventProbe =>
        eventProbe.expectMessageType[WalletCreated].walletId shouldBe id
      }
    }
    "deposit" in {
      val walletRef = spawn(WalletAggregate.behavior(1 hours))
      val id        = ULID.generate

      val createWalletResponseProbe = TestProbe[CreateWalletResponse]
      walletRef ! CreateWalletRequest(id, createWalletResponseProbe.ref)
      createWalletResponseProbe.expectMessage(CreateWalletSucceeded)

      val money               = Money(BigDecimal(100))
      val depositRequestProbe = TestProbe[DepositResponse]
      walletRef ! DepositRequest(id, money, Instant.now, depositRequestProbe.ref)
      depositRequestProbe.expectMessage(DepositSucceeded)
    }
    "request" in {
      val walletRef = spawn(WalletAggregate.behavior(1 hours))
      val id        = ULID.generate

      val createWalletResponseProbe = TestProbe[CreateWalletResponse]
      walletRef ! CreateWalletRequest(id, createWalletResponseProbe.ref)
      createWalletResponseProbe.expectMessage(CreateWalletSucceeded)

      val requestId           = ULID.generate
      val money               = Money(BigDecimal(100))
      val requestRequestProbe = TestProbe[RequestResponse]
      walletRef ! RequestRequest(requestId, id, money, Instant.now, requestRequestProbe.ref)
      requestRequestProbe.expectMessage(RequestSucceeded)
    }
  }

}
