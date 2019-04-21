package wallet.adaptor.untyped

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{ ActorLogging, OneForOneStrategy, Props, SupervisorStrategy, Terminated }
import akka.persistence.{ PersistentActor, RecoveryCompleted }
import wallet._
import wallet.adaptor.untyped.WalletProtocol.{ CommandRequest, Event, ToEvent }

object PersistentWalletAggregate {

  def props(id: WalletId, chargesLimit: Int = Int.MaxValue): Props =
    Props(new PersistentWalletAggregate(id, chargesLimit)(WalletAggregate.props))
}

/**
  * 永続化に対応したウォレット集約アクター。
  *
  * @param chargesLimit 受け付けられる最大の請求件数
  */
private[untyped] final class PersistentWalletAggregate(id: WalletId, chargesLimit: Int)(
    propsF: (WalletId, Int) => Props
) extends PersistentActor
    with ActorLogging {

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: Throwable =>
      Stop
  }

  private val childRef =
    context.actorOf(propsF(id, chargesLimit), name = WalletAggregate.name(id))

  context.watch(childRef)

  override def persistenceId: String = "p-" + WalletAggregate.name(id)

  override def receiveRecover: Receive = {
    case e: Event =>
      childRef ! e.toCommandRequest
    case RecoveryCompleted =>
      log.debug("recovery completed")
  }

  override def receiveCommand: Receive = {
    case Terminated(c) if c == childRef =>
      context.stop(self)
    case m: CommandRequest with ToEvent =>
      persist(m.toEvent) { _ =>
        childRef forward m
      }
    case m =>
      childRef forward m
  }

}
