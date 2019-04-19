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
      childRef ! CreateWalletRequest(newULID, e.walletId, e.occurredAt, noReply = true)
    case e: WalletDeposited =>
      childRef ! DepositRequest(newULID, e.walletId, e.money, e.occurredAt, noReply = true)
    case e: WalletCharged =>
      childRef ! ChargeRequest(newULID, e.chargeId, e.walletId, e.money, e.occurredAt, noReply = true)
    case e: WalletPayed =>
      childRef ! PayRequest(newULID, e.walletId, e.toWalletId, e.money, e.chargeId, e.occurredAt, noReply = true)
    case RecoveryCompleted =>
      log.debug("recovery completed")
  }

  // TODO: コマンドリクエストにドメインイベントを生成するメソッドがあればロジックがすっきりするかも
  override def receiveCommand: Receive = {
    case event: Event =>
      ()
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
    case m: ChargeRequest =>
      persist(WalletCharged(m.chargeId, m.walletId, m.money, m.createdAt)) { _ =>
        childRef forward m
      }
    case m: PayRequest =>
      persist(WalletPayed(m.walletId, m.toWalletId, m.money, m.chargeId, m.createdAt)) { _ =>
        childRef forward m
      }
    case m =>
      childRef forward m
  }

}
