package wallet.adaptor.untyped

import java.time.Instant

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit, TestProbe }
import org.scalatest.{ BeforeAndAfterAll, FreeSpecLike, Matchers }
import wallet._
import wallet.adaptor.untyped.WalletProtocol._
import wallet.domain.Money

class WalletAggregatesSpec
    extends TestKit(ActorSystem("WalletAggregatesSpec"))
    with FreeSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "WalletAggregate" - {
    "create" in {
      val walletRef = system.actorOf(WalletAggregates.props()(WalletAggregate.props))

      val walletId = newULID
      walletRef ! CreateWalletRequest(newULID, walletId)
      expectMsg(CreateWalletSucceeded)
    }
    "addSubscribers" in {
      val walletRef = system.actorOf(WalletAggregates.props()(WalletAggregate.props))

      val walletId    = newULID
      val eventProbes = for (_ <- 1 to 5) yield TestProbe()
      walletRef ! AddSubscribers(newULID, walletId, eventProbes.map(_.ref).toVector)

      walletRef ! CreateWalletRequest(newULID, walletId)
      expectMsg(CreateWalletSucceeded)
      eventProbes.foreach { eventProbe =>
        eventProbe.expectMsgType[WalletCreated].walletId shouldBe walletId
      }
    }
    "deposit" in {
      val walletRef = system.actorOf(WalletAggregates.props()(WalletAggregate.props))
      val walletId  = newULID

      walletRef ! CreateWalletRequest(newULID, walletId)
      expectMsg(CreateWalletSucceeded)

      val money = Money(BigDecimal(100))
      walletRef ! DepositRequest(newULID, walletId, money, Instant.now)
      expectMsg(DepositSucceeded)
    }
    "request" in {
      val walletRef = system.actorOf(WalletAggregates.props()(WalletAggregate.props))
      val walletId  = newULID

      walletRef ! CreateWalletRequest(newULID, walletId)
      expectMsg(CreateWalletSucceeded)

      val requestId = newULID
      val money     = Money(BigDecimal(100))
      walletRef ! RequestRequest(newULID, requestId, walletId, money, Instant.now)
      expectMsg(RequestSucceeded)
    }
  }
}
