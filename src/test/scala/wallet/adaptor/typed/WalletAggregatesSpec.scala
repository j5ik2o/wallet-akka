package wallet.adaptor.typed

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.{ ScalaTestWithActorTestKit, TestProbe }
import org.scalatest.{ FreeSpecLike, Matchers }
import wallet.adaptor.typed.WalletProtocol._
import wallet.domain.Money
import wallet.utils.ULID

import scala.concurrent.duration._

class WalletAggregatesSpec extends ScalaTestWithActorTestKit with FreeSpecLike with Matchers {

  "WalletAggregate" - {
    "create" in {
      val walletRef = spawn(WalletAggregates.behavior(1 hours))

      val createWalletResponseProbe = TestProbe[CreateWalletResponse]
      val walletId                  = ULID.generate
      walletRef ! CreateWalletRequest(ULID.generate, walletId, Some(createWalletResponseProbe.ref))
      createWalletResponseProbe.expectMessage(CreateWalletSucceeded)
    }
    "addSubscribers" in {
      val walletRef = spawn(WalletAggregates.behavior(1 hours))

      val walletId    = ULID.generate
      val eventProbes = for (_ <- 1 to 5) yield TestProbe[Event]
      walletRef ! AddSubscribers(ULID.generate, walletId, eventProbes.map(_.ref).toVector)

      val probe = TestProbe[CreateWalletResponse]
      walletRef ! CreateWalletRequest(ULID.generate, walletId, Some(probe.ref))
      probe.expectMessage(CreateWalletSucceeded)
      eventProbes.foreach { eventProbe =>
        eventProbe.expectMessageType[WalletCreated].walletId shouldBe walletId
      }
    }
    "deposit" in {
      val walletRef = spawn(WalletAggregates.behavior(1 hours))

      val walletId                  = ULID.generate
      val createWalletResponseProbe = TestProbe[CreateWalletResponse]
      walletRef ! CreateWalletRequest(ULID.generate, walletId, Some(createWalletResponseProbe.ref))
      createWalletResponseProbe.expectMessage(CreateWalletSucceeded)

      val money               = Money(BigDecimal(100))
      val depositRequestProbe = TestProbe[DepositResponse]
      walletRef ! DepositRequest(ULID.generate, walletId, money, Instant.now, Some(depositRequestProbe.ref))
      depositRequestProbe.expectMessage(DepositSucceeded)
    }
    "request" in {
      val walletRef = spawn(WalletAggregates.behavior(1 hours))

      val walletId                  = ULID.generate
      val createWalletResponseProbe = TestProbe[CreateWalletResponse]
      walletRef ! CreateWalletRequest(ULID.generate, walletId, Some(createWalletResponseProbe.ref))
      createWalletResponseProbe.expectMessage(CreateWalletSucceeded)

      val requestId           = ULID.generate
      val money               = Money(BigDecimal(100))
      val requestRequestProbe = TestProbe[RequestResponse]
      walletRef ! RequestRequest(ULID.generate, requestId, walletId, money, Instant.now, Some(requestRequestProbe.ref))
      requestRequestProbe.expectMessage(RequestSucceeded)
    }
  }

}
