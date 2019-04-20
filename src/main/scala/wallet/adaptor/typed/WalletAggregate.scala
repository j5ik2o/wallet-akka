package wallet.adaptor.typed

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import wallet._
import wallet.adaptor.typed.WalletProtocol._
import wallet.domain.{ Balance, Wallet }

object WalletAggregate {

  private def getWallet(w: Option[Wallet]): Wallet =
    w.getOrElse(throw new IllegalStateException("Invalid state"))

  private def fireEvent(subscribers: Vector[ActorRef[Event]])(event: Event): Unit =
    subscribers.foreach(_ ! event)

  def name(id: WalletId): String = "wallet-typed-" + id.toString

  def isReceive(maybeWallet: Option[Wallet], id: WalletId, walletId: WalletId): Boolean =
    maybeWallet.nonEmpty && walletId == id

  def behavior(
      id: WalletId,
      requestsLimit: Int = Int.MaxValue
  ): Behavior[CommandRequest] =
    Behaviors.setup[CommandRequest] { ctx =>
      def onUninitialized(
          subscribers: Vector[ActorRef[Event]]
      ): Behaviors.Receive[CommandRequest] = {
        val fireEventToSubscribers = fireEvent(subscribers) _
        Behaviors.receiveMessage[CommandRequest] {
          case AddSubscribers(_, walletId, s) if walletId == id =>
            onUninitialized(subscribers ++ s)
          case CreateWalletRequest(_, walletId, createdAt, replyTo) if walletId == id =>
            replyTo.foreach(_ ! CreateWalletSucceeded)
            fireEventToSubscribers(WalletCreated(walletId, createdAt))
            onInitialized(Wallet(walletId, Balance.zero), Vector.empty, subscribers)
        }
      }
      def onInitialized(
          wallet: Wallet,
          requests: Vector[ChargeRequest],
          subscribers: Vector[ActorRef[Event]]
      ): Behaviors.Receive[CommandRequest] = {
        val fireEventToSubscribers = fireEvent(subscribers) _
        Behaviors.receiveMessage[CommandRequest] {
          case GetBalanceRequest(_, walletId, replyTo) if walletId == id =>
            replyTo ! GetBalanceResponse(wallet.balance)
            Behaviors.same

          case AddSubscribers(_, walletId, s) if walletId == id =>
            onInitialized(wallet, requests, subscribers ++ s)

          case CreateWalletRequest(_, _, _, replyTo) =>
            replyTo.foreach(_ ! CreateWalletFailed("Already created"))
            Behaviors.same

          case DepositRequest(_, walletId, money, instant, replyTo) if walletId == id =>
            val currentBalance = wallet.balance
            if (currentBalance.add(money) < Balance.zero)
              replyTo.foreach(_ ! DepositFailed("Can not trade because the balance after trading is less than 0"))
            else
              replyTo.foreach(_ ! DepositSucceeded)
            fireEventToSubscribers(WalletDeposited(walletId, money, instant))
            onInitialized(
              wallet.add(money),
              requests,
              subscribers
            )

          case PayRequest(_, walletId, toWalletId, money, maybeChargeId, instant, replyTo)
              if walletId == id && maybeChargeId.fold(true)(requests.map(_.walletId).contains) =>
            val currentBalance = wallet.balance
            if (currentBalance.sub(money) < Balance.zero)
              replyTo.foreach(_ ! PayFailed("Can not trade because the balance after trading is less than 0"))
            else
              replyTo.foreach(_ ! PaySucceeded)
            fireEventToSubscribers(WalletPayed(walletId, toWalletId, money, maybeChargeId, instant))
            onInitialized(
              wallet.subtract(money),
              requests.filterNot(maybeChargeId.contains),
              subscribers
            )

          case rr @ ChargeRequest(_, questId, walletId, money, instant, replyTo) if walletId == id =>
            if (requests.size > requestsLimit)
              replyTo.foreach(_ ! ChargeFailed("Limit over"))
            else
              replyTo.foreach(_ ! ChargeSucceeded)
            fireEventToSubscribers(WalletCharged(questId, walletId, money, instant))
            onInitialized(
              wallet,
              requests :+ rr,
              subscribers
            )

        }
      }
      onUninitialized(Vector.empty)
    }

}
