package wallet.adaptor.untyped

import akka.actor.{ Actor, ActorLogging, Props }
import wallet._
import wallet.adaptor.utils.ChildActorLookup

object WalletAggregates {

  def props(chargesLimit: Int = Int.MaxValue)(
      propsF: (WalletId, Int) => Props
  ): Props =
    Props(new WalletAggregates(chargesLimit, propsF))

}

private[untyped] class WalletAggregates(
    chargesLimit: Int,
    propsF: (WalletId, Int) => Props
) extends Actor
    with ActorLogging
    with ChildActorLookup {

  override type ID             = WalletId
  override type CommandRequest = WalletProtocol.CommandRequest

  override def receive: Receive = forwardToActor

  override protected def childName(childId: WalletId): String = WalletAggregate.name(childId)

  override protected def childProps(childId: WalletId): Props = propsF(childId, chargesLimit)

  override protected def toChildId(commandRequest: CommandRequest): WalletId = commandRequest.walletId

}
