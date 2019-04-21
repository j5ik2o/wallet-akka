package wallet.adaptor.typed

import java.time.Instant

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import wallet._
import wallet.adaptor.typed.WalletProtocol._
import wallet.domain.{ Balance, Charge, Wallet }

/**
  * ウォレット集約アクター。
  */
object WalletAggregate {

  private def fireEvent(subscribers: Vector[ActorRef[Event]])(event: Event): Unit =
    subscribers.foreach(_ ! event)

  def name(id: WalletId): String = "wallet-typed-" + id.toString

  private def onUninitialized(
      id: WalletId,
      chargesLimit: Int,
      subscribers: Vector[ActorRef[Event]]
  ): Behaviors.Receive[CommandRequest] = {
    val fireEventToSubscribers = fireEvent(subscribers) _
    Behaviors.receiveMessage[CommandRequest] {
      case AddSubscribers(_, walletId, s) if walletId == id =>
        onUninitialized(id, chargesLimit, subscribers ++ s)
      case CreateWalletRequest(_, walletId, createdAt, replyTo) if walletId == id =>
        replyTo.foreach(_ ! CreateWalletSucceeded)
        fireEventToSubscribers(WalletCreated(walletId, createdAt))
        val now = Instant.now
        onInitialized(id, Wallet(walletId, chargesLimit, Balance.zero, Vector.empty, now, now), subscribers)
    }
  }

  private def onInitialized(
      id: WalletId,
      wallet: Wallet,
      subscribers: Vector[ActorRef[Event]]
  ): Behaviors.Receive[CommandRequest] = {
    val fireEventToSubscribers = fireEvent(subscribers) _
    Behaviors.receiveMessage[CommandRequest] {
      case GetBalanceRequest(_, walletId, replyTo) if walletId == id =>
        replyTo ! GetBalanceResponse(wallet.balance)
        Behaviors.same

      case AddSubscribers(_, walletId, s) if walletId == id =>
        onInitialized(id, wallet, subscribers ++ s)

      case CreateWalletRequest(_, _, _, replyTo) =>
        replyTo.foreach(_ ! CreateWalletFailed("Already created"))
        Behaviors.same

      case DepositRequest(_, walletId, money, instant, replyTo) if walletId == id =>
        wallet.deposit(money, instant) match {
          case Left(t) =>
            replyTo.foreach(_ ! DepositFailed(t.getMessage))
            Behaviors.same
          case Right(newWallet) =>
            replyTo.foreach(_ ! DepositSucceeded)
            fireEventToSubscribers(WalletDeposited(walletId, money, instant))
            onInitialized(
              id,
              newWallet,
              subscribers
            )
        }

      case PayRequest(_, walletId, toWalletId, money, maybeChargeId, instant, replyTo) if walletId == id =>
        wallet.pay(money, maybeChargeId, instant) match {
          case Left(t) =>
            replyTo.foreach(_ ! PayFailed(t.getMessage))
            Behaviors.same
          case Right(newWallet) =>
            replyTo.foreach(_ ! PaySucceeded)
            fireEventToSubscribers(WalletPayed(walletId, toWalletId, money, maybeChargeId, instant))
            onInitialized(
              id,
              newWallet,
              subscribers
            )
        }

      case ChargeRequest(_, questId, walletId, money, instant, replyTo) if walletId == id =>
        wallet.addCharge(Charge(newULID, walletId, money, instant), instant) match {
          case Left(t) =>
            replyTo.foreach(_ ! ChargeFailed(t.getMessage))
            Behaviors.same
          case Right(newWallet) =>
            replyTo.foreach(_ ! ChargeSucceeded)
            fireEventToSubscribers(WalletCharged(questId, walletId, money, instant))
            onInitialized(
              id,
              newWallet,
              subscribers
            )
        }
    }

  }

  def behavior(
      id: WalletId,
      chargesLimit: Int = Int.MaxValue
  ): Behavior[CommandRequest] =
    onUninitialized(id, chargesLimit, Vector.empty)

}
