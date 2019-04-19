package wallet.adaptor.typed

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import wallet._
import wallet.adaptor.typed.WalletProtocol._
import wallet.domain.{ Balance, Money, Wallet }

object WalletAggregate {

  private def getWallet(w: Option[Wallet]): Wallet =
    w.getOrElse(throw new IllegalStateException("Invalid state"))

  private def fireEvent(subscribers: Vector[ActorRef[Event]])(event: Event): Unit =
    subscribers.foreach(_ ! event)

  def name(id: WalletId): String = "wallet-typed-" + id.toString

  def behavior(
      id: WalletId,
      requestsLimit: Int = Int.MaxValue
  ): Behavior[CommandRequest] =
    Behaviors.setup[CommandRequest] { ctx =>
      def onMessage(
          maybeWallet: Option[Wallet],
          requests: Vector[ChargeRequest],
          subscribers: Vector[ActorRef[Event]]
      ): Behaviors.Receive[CommandRequest] = {
        val fireEventToSubscribers = fireEvent(subscribers) _
        Behaviors.receiveMessage[CommandRequest] {
          case GetBalanceRequest(_, walletId, replyTo) if walletId == id =>
            replyTo ! GetBalanceResponse(getWallet(maybeWallet).balance)
            Behaviors.same

          case AddSubscribers(_, walletId, s) if walletId == id =>
            onMessage(maybeWallet, requests, subscribers ++ s)

          case CreateWalletRequest(_, walletId, createdAt, replyTo) =>
            if (maybeWallet.isEmpty)
              replyTo.foreach(_ ! CreateWalletSucceeded)
            else
              replyTo.foreach(_ ! CreateWalletFailed("Already created"))
            fireEventToSubscribers(WalletCreated(walletId, createdAt))
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

          case PayRequest(_, walletId, money, maybeChargeId, instant, replyTo)
              if walletId == id && maybeChargeId.fold(true)(requests.contains) =>
            val currentBalance = getWallet(maybeWallet).balance
            if (currentBalance.sub(money) < Balance.zero)
              replyTo.foreach(_ ! PayFailed("Can not trade because the balance after trading is less than 0"))
            else
              replyTo.foreach(_ ! PaySucceeded)
            fireEventToSubscribers(WalletPayed(walletId, money, maybeChargeId, instant))
            onMessage(
              maybeWallet.map(_.subBalance(money)),
              requests.filterNot(maybeChargeId.contains),
              subscribers
            )

          case rr @ ChargeRequest(_, questId, walletId, money, instant, replyTo) if walletId == id =>
            if (requests.size > requestsLimit)
              replyTo.foreach(_ ! ChargeFailed("Limit over"))
            else
              replyTo.foreach(_ ! ChargeSucceeded$)
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
