package wallet.adaptor.untyped

import akka.actor.{ Actor, ActorLogging, Props }
import wallet._
import wallet.adaptor.utils.ChildActorLookup

object WalletAggregates {

  def props(requestsLimit: Int = Int.MaxValue)(
      propsF: (ULID, Int) => Props
  ): Props =
    Props(new WalletAggregates(requestsLimit, propsF))

}

private[untyped] class WalletAggregates(
    requestsLimit: Int,
    propsF: (ULID, Int) => Props
) extends Actor
    with ActorLogging
    with ChildActorLookup {

  override type ID             = CommandId
  override type CommandRequest = WalletProtocol.CommandRequest

  override def receive: Receive = forwardToActor

  override protected def childName(childId: ULID): String = WalletAggregate.name(childId)

  override protected def childProps(childId: ULID): Props = propsF(childId, requestsLimit)

  override protected def toChildId(commandRequest: CommandRequest): CommandId = commandRequest.walletId

}
