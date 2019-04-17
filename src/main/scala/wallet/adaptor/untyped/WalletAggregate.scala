package wallet.adaptor.untyped

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import wallet.WalletId
import wallet.adaptor.untyped.WalletProtocol._
import wallet.domain.{ Balance, Money, Wallet }

import scala.concurrent.duration.FiniteDuration

object WalletAggregate {

  def props(id: WalletId, receiveTimeout: FiniteDuration, requestsLimit: Int = Int.MaxValue): Props =
    Props(new WalletAggregate(id, receiveTimeout, requestsLimit))

  def name(id: WalletId): String = "wallet-untyped-" + id.asString

}

private[untyped] final class WalletAggregate(id: WalletId, receiveTimeout: FiniteDuration, requestsLimit: Int)
    extends Actor
    with ActorLogging {

  context.setReceiveTimeout(receiveTimeout)

  private def getWallet(w: Option[Wallet]): Wallet =
    w.getOrElse(throw new IllegalStateException("Invalid state"))

  private def fireEvent(subscribers: Vector[ActorRef])(event: Event): Unit =
    subscribers.foreach(_ ! event)

  override def receive: Receive = onMessage(None, Vector.empty, Vector.empty)

  private def onMessage(
      maybeWallet: Option[Wallet],
      requests: Vector[RequestRequest],
      subscribers: Vector[ActorRef]
  ): Receive = {
    case m @ GetBalanceRequest(_, walletId) if walletId == id =>
      sender() ! GetBalanceResponse(getWallet(maybeWallet).balance)

    case AddSubscribers(_, walletId, s) if walletId == id =>
      context.become(onMessage(maybeWallet, requests, subscribers ++ s))

    case m @ CreateWalletRequest(_, walletId) if walletId == id =>
      if (maybeWallet.isEmpty)
        sender() ! CreateWalletSucceeded
      else
        sender() ! CreateWalletFailed("Already created")
      fireEvent(subscribers)(WalletCreated(id))
      context.become(onMessage(Some(Wallet(id, Balance(Money.zero))), requests, subscribers))

    case m @ DepositRequest(_, walletId, money, instant) if walletId == id =>
      val currentBalance = getWallet(maybeWallet).balance
      if (currentBalance.add(money) < Balance.zero)
        sender() ! DepositFailed("Can not trade because the balance after trading is less than 0")
      else
        sender() ! DepositSucceeded
      fireEvent(subscribers)(WalletDeposited(walletId, money, instant))
      context.become(
        onMessage(
          maybeWallet.map(_.withBalance(currentBalance.add(money))),
          requests,
          subscribers
        )
      )

    case m @ PayRequest(_, walletId, money, requestId, instant)
        if walletId == id && requestId.fold(true)(requests.contains) =>
      val currentBalance = getWallet(maybeWallet).balance
      if (currentBalance.sub(money) < Balance.zero)
        sender() ! PayFailed("Can not trade because the balance after trading is less than 0")
      else
        sender() ! PaySucceeded
      fireEvent(subscribers)(WalletPayed(walletId, money, requestId, instant))
      context.become(
        onMessage(
          maybeWallet.map(_.withBalance(currentBalance.sub(money))),
          requests.filterNot(requestId.contains),
          subscribers
        )
      )

    case rr @ RequestRequest(_, requestId, walletId, money, instant) if walletId == id =>
      if (requests.size > requestsLimit)
        sender() ! RequestFailed("Limit over")
      else
        sender() ! RequestSucceeded
      fireEvent(subscribers)(WalletRequested(requestId, walletId, money, instant))
      context.become(
        onMessage(
          maybeWallet,
          requests :+ rr,
          subscribers
        )
      )

    case Shutdown(_, walletId) if walletId == id =>
      context.stop(self)
  }

}
