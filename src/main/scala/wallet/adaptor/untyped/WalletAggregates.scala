package wallet.adaptor.untyped

import akka.actor.{ Actor, Props }
import wallet.CommandId
import wallet.adaptor.utils.ChildActorLookup
import wallet.utils.ULID

import scala.concurrent.duration.FiniteDuration

object WalletAggregates {

  def props(receiveTimeout: FiniteDuration, requestsLimit: Int = Int.MaxValue): Props =
    Props(new WalletAggregates(receiveTimeout, requestsLimit))

}

private[untyped] final class WalletAggregates(receiveTimeout: FiniteDuration, requestsLimit: Int)
    extends Actor
    with ChildActorLookup {

  override type ID             = CommandId
  override type CommandRequest = WalletProtocol.CommandRequest

  override def receive: Receive = forwardToActor

  override protected def childName(childId: ULID): String = WalletAggregate.name(childId)

  override protected def childProps(childId: ULID): Props =
    WalletAggregate.props(childId, receiveTimeout, requestsLimit)

  override protected def toChildId(commandRequest: CommandRequest): CommandId = commandRequest.walletId

}
