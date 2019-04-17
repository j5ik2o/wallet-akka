package wallet.adaptor.untyped

import akka.actor.{ ActorLogging, Props }
import akka.persistence.{ PersistentActor, RecoveryCompleted }
import wallet.WalletId
import wallet.adaptor.untyped.WalletProtocol._
import wallet.utils.ULID

import scala.concurrent.duration.FiniteDuration

object PersistentWalletAggregate {

  def props(id: WalletId, receiveTimeout: FiniteDuration, requestsLimit: Int = Int.MaxValue): Props =
    Props(new PersistentWalletAggregate(id, receiveTimeout, requestsLimit))
}

final class PersistentWalletAggregate(id: WalletId, receiveTimeout: FiniteDuration, requestsLimit: Int)
    extends PersistentActor
    with ActorLogging {

  private val childRef =
    context.actorOf(WalletAggregate.props(id, receiveTimeout, requestsLimit), name = WalletAggregate.name(id))

  override def persistenceId: String = "p-" + WalletAggregate.name(id)

  override def receiveRecover: Receive = {
    case e: WalletCreated =>
      childRef ! CreateWalletRequest(ULID.generate, e.walletId)
    case e: WalletDeposited =>
      childRef ! DepositRequest(ULID.generate, e.walletId, e.money, e.createdAt)
    case e: WalletRequested =>
      childRef ! RequestRequest(ULID.generate, e.requestId, e.walletId, e.money, e.createdAt)
    case e: WalletPayed =>
      childRef ! PayRequest(ULID.generate, e.walletId, e.money, e.requestId, e.createdAt)
    case RecoveryCompleted =>
      log.debug("recovery completed")
  }

  override def receiveCommand: Receive = {
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
