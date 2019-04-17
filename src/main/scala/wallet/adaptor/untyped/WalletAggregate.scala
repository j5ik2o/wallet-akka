package wallet.adaptor.untyped

import akka.actor.{Actor, ActorRef, Props}
import wallet.WalletId
import wallet.adaptor.untyped.WalletProtocol._
import wallet.domain.{Balance, Money, Wallet}

import scala.concurrent.duration.FiniteDuration

object WalletAggregate {

  def props(receiveTimeout: FiniteDuration, requestsLimit: Int = Int.MaxValue): Props =
    Props(new WalletAggregate(receiveTimeout, requestsLimit))

  def name(id: WalletId): String = "wallet-untyped-" + id.asString

}

final class WalletAggregate(receiveTimeout: FiniteDuration, requestsLimit: Int) extends Actor {

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
    case GetBalanceRequest(id) if id == getWallet(maybeWallet).id =>
      sender() ! GetBalanceResponse(getWallet(maybeWallet).balance)

    case AddSubscribers(s) =>
      context.become(onMessage(maybeWallet, requests, subscribers ++ s))

    case CreateWalletRequest(walletId) =>
      if (maybeWallet.isEmpty)
        sender() ! CreateWalletSucceeded
      else
        sender() ! CreateWalletFailed("Already created")
      fireEvent(subscribers)(WalletCreated(walletId))
      context.become(onMessage(Some(Wallet(walletId, Balance(Money.zero))), requests, subscribers))

    case DepositRequest(walletId, money, instant) if walletId == getWallet(maybeWallet).id =>
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

    case PayRequest(walletId, money, requestId, instant)
        if walletId == getWallet(maybeWallet).id && requestId.fold(true)(requests.contains) =>
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

    case rr @ RequestRequest(id, walletId, money, instant) if walletId == getWallet(maybeWallet).id =>
      if (requests.size > requestsLimit)
        sender() ! RequestFailed("Limit over")
      else
        sender() ! RequestSucceeded
      fireEvent(subscribers)(WalletRequested(id, walletId, money, instant))
      context.become(
        onMessage(
          maybeWallet,
          requests :+ rr,
          subscribers
        )
      )
  }

}
