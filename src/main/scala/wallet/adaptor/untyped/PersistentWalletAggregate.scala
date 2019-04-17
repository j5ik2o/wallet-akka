package wallet.adaptor.untyped

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{ ActorLogging, OneForOneStrategy, Props, SupervisorStrategy, Terminated }
import akka.persistence.{ PersistentActor, RecoveryCompleted }
import wallet._
import wallet.adaptor.untyped.WalletProtocol._

import scala.concurrent.duration.FiniteDuration

object PersistentWalletAggregate {

  def props(id: WalletId, requestsLimit: Int = Int.MaxValue): Props =
    Props(new PersistentWalletAggregate(id, requestsLimit))
}

private[untyped] final class PersistentWalletAggregate(id: WalletId, requestsLimit: Int)
    extends PersistentActor
    with ActorLogging {

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: Throwable =>
      Stop
  }

  private val childRef =
    context.actorOf(WalletAggregate.props(id, requestsLimit), name = WalletAggregate.name(id))

  context.watch(childRef)

  override def persistenceId: String = "p-" + WalletAggregate.name(id)

  override def receiveRecover: Receive = {
    case e: WalletCreated =>
      childRef ! CreateWalletRequest(newULID, e.walletId)
    case e: WalletDeposited =>
      childRef ! DepositRequest(newULID, e.walletId, e.money, e.createdAt)
    case e: WalletRequested =>
      childRef ! RequestRequest(newULID, e.requestId, e.walletId, e.money, e.createdAt)
    case e: WalletPayed =>
      childRef ! PayRequest(newULID, e.walletId, e.money, e.requestId, e.createdAt)
    case RecoveryCompleted =>
      log.debug("recovery completed")
  }

  override def receiveCommand: Receive = {
    case Terminated(c) if c == childRef =>
      context.stop(self)
    case m: CreateWalletRequest =>
      persist(WalletCreated(m.walletId)) { _ =>
        childRef forward m
      }
    case m: DepositRequest =>
      persist(WalletDeposited(m.walletId, m.money, m.createdAt)) { _ =>
        childRef forward m
      }
    case m: RequestRequest =>
      persist(WalletRequested(m.requestId, m.walletId, m.money, m.createdAt)) { _ =>
        childRef forward m
      }
    case m: PayRequest =>
      persist(WalletPayed(m.walletId, m.money, m.requestId, m.createdAt)) { _ =>
        childRef forward m
      }
    case m =>
      childRef forward m
  }

}
