package wallet.adaptor.untyped

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{ ActorLogging, OneForOneStrategy, Props, SupervisorStrategy, Terminated }
import akka.persistence.{ PersistentActor, RecoveryCompleted }
import wallet._
import wallet.adaptor.untyped.WalletProtocol._

object PersistentWalletAggregate {

  def props(id: WalletId, requestsLimit: Int = Int.MaxValue): Props =
    Props(new PersistentWalletAggregate(id, requestsLimit)(WalletAggregate.props))
}

/**
  * 委譲によってWalletAggregateに永続化機能を付与するアクター。
  *
  * @param id
  * @param requestsLimit
  */
private[untyped] final class PersistentWalletAggregate(id: WalletId, requestsLimit: Int)(
    propsF: (WalletId, Int) => Props
) extends PersistentActor
    with ActorLogging {

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: Throwable =>
      Stop
  }

  private val childRef =
    context.actorOf(propsF(id, requestsLimit), name = WalletAggregate.name(id))

  context.watch(childRef)

  override def persistenceId: String = "p-" + WalletAggregate.name(id)

  // TODO: ドメインイベントにコマンドを生成するメソッドがあればロジックがすっきりするかも
  override def receiveRecover: Receive = {
    case e: WalletCreated =>
      childRef ! CreateWalletRequest(newULID, e.walletId, e.occurredAt)
    case e: WalletDeposited =>
      childRef ! DepositRequest(newULID, e.walletId, e.money, e.occurredAt)
    case e: WalletRequested =>
      childRef ! RequestRequest(newULID, e.requestId, e.walletId, e.money, e.occurredAt)
    case e: WalletPayed =>
      childRef ! PayRequest(newULID, e.walletId, e.money, e.requestId, e.occurredAt)
    case RecoveryCompleted =>
      log.debug("recovery completed")
  }

  // TODO: コマンドリクエストにドメインイベントを生成するメソッドがあればロジックがすっきりするかも
  override def receiveCommand: Receive = {
    case Terminated(c) if c == childRef =>
      context.stop(self)
    case m: CreateWalletRequest =>
      persist(WalletCreated(m.walletId, m.createdAt)) { _ =>
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
