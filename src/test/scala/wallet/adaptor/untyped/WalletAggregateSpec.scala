package wallet.adaptor.untyped

import java.time.Instant

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit, TestProbe }
import wallet.adaptor.untyped.WalletProtocol._
import wallet.domain.Money
import wallet.utils.ULID

import scala.concurrent.duration._
import org.scalatest._

class WalletAggregateSpec
    extends TestKit(ActorSystem("WalletAggregateSpec"))
    with FreeSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "WalletAggregate" - {
    "create" in {
      val walletRef = system.actorOf(WalletAggregate.props(1 hours))

      val id = ULID.generate
      walletRef ! CreateWalletRequest(id)
      expectMsg(CreateWalletSucceeded)
    }
    "addSubscribers" in {
      val walletRef   = system.actorOf(WalletAggregate.props(1 hours))
      val eventProbes = for (_ <- 1 to 5) yield TestProbe()
      walletRef ! AddSubscribers(eventProbes.map(_.ref).toVector)

      val id = ULID.generate
      walletRef ! CreateWalletRequest(id)
      expectMsg(CreateWalletSucceeded)
      eventProbes.foreach { eventProbe =>
        eventProbe.expectMsgType[WalletCreated].walletId shouldBe id
      }
    }
    "deposit" in {
      val walletRef = system.actorOf(WalletAggregate.props(1 hours))
      val id        = ULID.generate

      walletRef ! CreateWalletRequest(id)
      expectMsg(CreateWalletSucceeded)

      val money = Money(BigDecimal(100))
      walletRef ! DepositRequest(id, money, Instant.now)
      expectMsg(DepositSucceeded)
    }
    "request" in {
      val walletRef = system.actorOf(WalletAggregate.props(1 hours))
      val id        = ULID.generate

      walletRef ! CreateWalletRequest(id)
      expectMsg(CreateWalletSucceeded)

      val requestId = ULID.generate
      val money     = Money(BigDecimal(100))
      walletRef ! RequestRequest(requestId, id, money, Instant.now)
      expectMsg(RequestSucceeded)
    }
  }
}
