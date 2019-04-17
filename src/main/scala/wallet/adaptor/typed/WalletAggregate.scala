package wallet.adaptor.typed

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior, SupervisorStrategy }
import wallet.domain.{ Balance, Money, Wallet }
import WalletProtocol._
import wallet.WalletId
import wallet.utils.ULID

import scala.concurrent.duration.FiniteDuration

object WalletAggregate {

  private def getWallet(w: Option[Wallet]): Wallet =
    w.getOrElse(throw new IllegalStateException("Invalid state"))

  private def fireEvent(subscribers: Vector[ActorRef[Event]])(event: Event): Unit =
    subscribers.foreach(_ ! event)

  def name(id: WalletId): String = "wallet-typed-" + id.asString

  def behavior(
      id: WalletId,
      receiveTimeout: FiniteDuration,
      requestsLimit: Int = Int.MaxValue
  ): Behavior[Message] =
    Behaviors.setup[Message] { ctx =>
      def onMessage(
          maybeWallet: Option[Wallet],
          requests: Vector[RequestRequest],
          subscribers: Vector[ActorRef[Event]]
      ): Behaviors.Receive[Message] = {
        val fireEventToSubscribers = fireEvent(subscribers) _
        Behaviors.receiveMessage[Message] {
          case Shutdown =>
            Behaviors.stopped

          case GetBalanceRequest(_, walletId, replyTo) if walletId == id =>
            replyTo ! GetBalanceResponse(getWallet(maybeWallet).balance)
            Behaviors.same

          case AddSubscribers(_, walletId, s) if walletId == id =>
            onMessage(maybeWallet, requests, subscribers ++ s)

          case CreateWalletRequest(_, walletId, replyTo) =>
            ctx.setReceiveTimeout(receiveTimeout, Shutdown)
            if (maybeWallet.isEmpty)
              replyTo.foreach(_ ! CreateWalletSucceeded)
            else
              replyTo.foreach(_ ! CreateWalletFailed("Already created"))
            fireEventToSubscribers(WalletCreated(walletId))
            onMessage(Some(Wallet(walletId, Balance(Money.zero))), requests, subscribers)

          case DepositRequest(_, walletId, money, instant, replyTo) if walletId == id =>
            val currentBalance = getWallet(maybeWallet).balance
            if (currentBalance.add(money) < Balance.zero)
              replyTo.foreach(_ ! DepositFailed("Can not trade because the balance after trading is less than 0"))
            else
              replyTo.foreach(_ ! DepositSucceeded)
            fireEventToSubscribers(WalletDeposited(walletId, money, instant))
            onMessage(
              maybeWallet.map(_.addBalance(money)),
              requests,
              subscribers
            )

          case PayRequest(_, walletId, money, requestId, instant, replyTo)
              if walletId == id && requestId.fold(true)(requests.contains) =>
            val currentBalance = getWallet(maybeWallet).balance
            if (currentBalance.sub(money) < Balance.zero)
              replyTo.foreach(_ ! PayFailed("Can not trade because the balance after trading is less than 0"))
            else
              replyTo.foreach(_ ! PaySucceeded)
            fireEventToSubscribers(WalletPayed(walletId, money, requestId, instant))
            onMessage(
              maybeWallet.map(_.subBalance(money)),
              requests.filterNot(requestId.contains),
              subscribers
            )

          case rr @ RequestRequest(_, questId, walletId, money, instant, replyTo) if walletId == id =>
            if (requests.size > requestsLimit)
              replyTo.foreach(_ ! RequestFailed("Limit over"))
            else
              replyTo.foreach(_ ! RequestSucceeded)
            fireEventToSubscribers(WalletRequested(questId, walletId, money, instant))
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
