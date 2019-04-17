package wallet.adaptor.typed

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import wallet.domain.{ Balance, Money, Wallet }
import WalletProtocol._

import scala.concurrent.duration.FiniteDuration

object WalletAggregate {

  private def getWallet(w: Option[Wallet]): Wallet =
    w.getOrElse(throw new IllegalStateException("Invalid state"))

  private def fireEvent(subscribers: Vector[ActorRef[Event]])(event: Event): Unit =
    subscribers.foreach(_ ! event)

  def behavior(receiveTimeout: FiniteDuration, requestsLimit: Int = Int.MaxValue): Behavior[Message] = Behaviors.setup {
    ctx =>
      ctx.setReceiveTimeout(receiveTimeout, Shutdown)
      def onMessage(
          maybeWallet: Option[Wallet],
          requests: Vector[RequestRequest],
          subscribers: Vector[ActorRef[Event]]
      ): Behaviors.Receive[Message] = {
        val fireEventToSubscribers = fireEvent(subscribers) _
        Behaviors.receiveMessage[Message] {
          case Shutdown =>
            Behaviors.stopped

          case GetBalanceRequest(id, replyTo) if id == getWallet(maybeWallet).id =>
            replyTo ! GetBalanceResponse(getWallet(maybeWallet).balance)
            Behaviors.same

          case AddSubscribers(s) =>
            onMessage(maybeWallet, requests, subscribers ++ s)

          case CreateWalletRequest(walletId, replyTo) =>
            if (maybeWallet.isEmpty)
              replyTo ! CreateWalletSucceeded
            else
              replyTo ! CreateWalletFailed("Already created")
            fireEventToSubscribers(WalletCreated(walletId))
            onMessage(Some(Wallet(walletId, Balance(Money.zero))), requests, subscribers)

          case DepositRequest(walletId, money, instant, replyTo) if walletId == getWallet(maybeWallet).id =>
            val currentBalance = getWallet(maybeWallet).balance
            if (currentBalance.add(money) < Balance.zero)
              replyTo ! DepositFailed("Can not trade because the balance after trading is less than 0")
            else
              replyTo ! DepositSucceeded
            fireEventToSubscribers(WalletDeposited(walletId, money, instant))
            onMessage(
              maybeWallet.map(_.addBalance(money)),
              requests,
              subscribers
            )

          case PayRequest(walletId, money, requestId, instant, replyTo)
              if walletId == getWallet(maybeWallet).id && requestId.fold(true)(requests.contains) =>
            val currentBalance = getWallet(maybeWallet).balance
            if (currentBalance.sub(money) < Balance.zero)
              replyTo ! PayFailed("Can not trade because the balance after trading is less than 0")
            else
              replyTo ! PaySucceeded
            fireEventToSubscribers(WalletPayed(walletId, money, requestId, instant))
            onMessage(
              maybeWallet.map(_.subBalance(money)),
              requests.filterNot(requestId.contains),
              subscribers
            )

          case rr @ RequestRequest(id, walletId, money, instant, replyTo) if walletId == getWallet(maybeWallet).id =>
            if (requests.size > requestsLimit)
              replyTo ! RequestFailed("Limit over")
            else
              replyTo ! RequestSucceeded
            fireEventToSubscribers(WalletRequested(id, walletId, money, instant))
            onMessage(
              maybeWallet,
              requests :+ rr,
              subscribers
            )

        }
      }
      onMessage(None, Vector.empty, Vector.empty)
  }

}
