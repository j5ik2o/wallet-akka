package wallet.adaptor.untyped

import java.time.Instant

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import wallet.adaptor.untyped.WalletProtocol._
import wallet.domain.{ Balance, Charge, ChargeId, Wallet }
import wallet.{ newULID, WalletId }

object WalletAggregate {

  def props(id: WalletId, chargesLimit: Int = Int.MaxValue): Props =
    Props(new WalletAggregate(id, chargesLimit))

  def name(id: WalletId): String = "wallet-untyped-" + id.value.toString

}

/**
  * ウォレット集約アクター。
  */
private[untyped] final class WalletAggregate(id: WalletId, chargesLimit: Int) extends Actor with ActorLogging {

  private def fireEvent(subscribers: Vector[ActorRef])(event: Event): Unit =
    subscribers.foreach(_ ! event)

  override def receive: Receive = onUninitialized(id, chargesLimit, Vector.empty)

  private def onUninitialized(id: WalletId, chargesLimit: Int, subscribers: Vector[ActorRef]): Receive = {
    case m @ AddSubscribers(_, walletId, s) if walletId == id =>
      log.debug(s"message = $m")
      context.become(onUninitialized(id, chargesLimit, subscribers ++ s))

    case m @ CreateWalletRequest(_, walletId, createdAt, noReply) if walletId == id =>
      log.debug(s"message = $m")
      if (!noReply) {
        sender() ! CreateWalletSucceeded
      }
      fireEvent(subscribers)(WalletCreated(walletId, createdAt))
      val now = Instant.now
      context.become(onInitialized(Wallet(id, chargesLimit, Balance.zero, Vector.empty, now, now), subscribers))
  }

  private def onInitialized(
      wallet: Wallet,
      subscribers: Vector[ActorRef]
  ): Receive = {
    case m @ GetBalanceRequest(_, walletId) if walletId == id =>
      log.debug(s"message = $m")
      sender() ! GetBalanceResponse(wallet.balance)

    case m @ CreateWalletRequest(_, walletId, _, noReply) if walletId == id =>
      log.debug(s"message = $m")
      if (!noReply)
        sender() ! CreateWalletFailed("Already created")

    case m @ DepositRequest(_, walletId, money, instant, noReply) if walletId == id =>
      log.debug(s"message = $m")
      wallet.deposit(money, instant) match {
        case Left(t) =>
          if (!noReply)
            sender() ! DepositFailed(t.getMessage)
        case Right(newWallet) =>
          if (!noReply)
            sender() ! DepositSucceeded
          fireEvent(subscribers)(m.toEvent)
          context.become(
            onInitialized(
              newWallet,
              subscribers
            )
          )
      }

    case m @ WithdrawRequest(_, walletId, toWalletId, money, maybeChargeId, instant, noReply) if walletId == id =>
      log.debug(s"message = $m")
      wallet.withdraw(money, maybeChargeId, instant) match {
        case Left(t) =>
          if (!noReply)
            sender() ! WithdrawFailed(t.getMessage)
        case Right(newWallet) =>
          if (!noReply)
            sender() ! WithdrawSucceeded$
          fireEvent(subscribers)(m.toEvent)
          context.become(
            onInitialized(
              newWallet,
              subscribers
            )
          )

      }

    case rr @ ChargeRequest(_, walletId, toWalletId, chargeId, money, instant, noReply) if walletId == id =>
      log.debug(s"message = $rr")
      wallet.addCharge(Charge(ChargeId(newULID), walletId, toWalletId, money, instant), instant) match {
        case Left(t) =>
          if (!noReply)
            sender() ! ChargeFailed(t.getMessage)
        case Right(newWallet) =>
          if (!noReply)
            sender() ! ChargeSucceeded
          fireEvent(subscribers)(rr.toEvent)
          context.become(
            onInitialized(
              newWallet,
              subscribers
            )
          )

      }

  }

}
