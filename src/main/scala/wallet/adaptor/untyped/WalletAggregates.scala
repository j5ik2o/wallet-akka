package wallet.adaptor.untyped

import akka.actor.{ Actor, Props }
import wallet.CommandId
import wallet.adaptor.utils.ChildActorLookup
import wallet.utils.ULID

import scala.concurrent.duration.FiniteDuration

object WalletAggregates {

  def props(receiveTimeout: FiniteDuration, requestsLimit: Int = Int.MaxValue)(
      propsF: (ULID, FiniteDuration, Int) => Props
  ): Props =
    Props(new WalletAggregates(receiveTimeout, requestsLimit, propsF))

}

private[untyped] final class WalletAggregates(
    receiveTimeout: FiniteDuration,
    requestsLimit: Int,
    propsF: (ULID, FiniteDuration, Int) => Props
) extends Actor
    with ChildActorLookup {

  override type ID             = CommandId
  override type CommandRequest = WalletProtocol.CommandRequest

  override def receive: Receive = forwardToActor

  override protected def childName(childId: ULID): String = WalletAggregate.name(childId)

  override protected def childProps(childId: ULID): Props = propsF(childId, receiveTimeout, requestsLimit)

  override protected def toChildId(commandRequest: CommandRequest): CommandId = commandRequest.walletId

}
