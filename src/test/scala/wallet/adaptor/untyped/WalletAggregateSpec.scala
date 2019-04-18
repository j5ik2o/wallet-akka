package wallet.adaptor.untyped

import java.time.Instant

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit, TestProbe }
import org.scalatest._
import wallet._
import wallet.adaptor.untyped.WalletProtocol._
import wallet.domain.Money

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
      val walletId  = newULID
      val walletRef = system.actorOf(WalletAggregate.props(walletId))

      walletRef ! CreateWalletRequest(newULID, walletId)
      expectMsg(CreateWalletSucceeded)
    }
    "addSubscribers" in {
      val walletId    = newULID
      val walletRef   = system.actorOf(WalletAggregate.props(walletId))
      val eventProbes = for (_ <- 1 to 5) yield TestProbe()
      walletRef ! AddSubscribers(newULID, walletId, eventProbes.map(_.ref).toVector)

      walletRef ! CreateWalletRequest(newULID, walletId)
      expectMsg(CreateWalletSucceeded)
      eventProbes.foreach { eventProbe =>
        eventProbe.expectMsgType[WalletCreated].walletId shouldBe walletId
      }
    }
    "deposit" in {
      val walletId  = newULID
      val walletRef = system.actorOf(WalletAggregate.props(walletId))

      walletRef ! CreateWalletRequest(newULID, walletId)
      expectMsg(CreateWalletSucceeded)

      val money = Money(BigDecimal(100))
      walletRef ! DepositRequest(newULID, walletId, money, Instant.now)
      expectMsg(DepositSucceeded)
    }
    "request" in {
      val walletId  = newULID
      val walletRef = system.actorOf(WalletAggregate.props(walletId))

      walletRef ! CreateWalletRequest(newULID, walletId)
      expectMsg(CreateWalletSucceeded)

      val requestId = newULID
      val money     = Money(BigDecimal(100))
      walletRef ! RequestRequest(newULID, requestId, walletId, money, Instant.now)
      expectMsg(RequestSucceeded)
    }
  }
}
