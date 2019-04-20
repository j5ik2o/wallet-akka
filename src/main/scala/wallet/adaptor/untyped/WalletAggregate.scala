package wallet.adaptor.untyped

import java.time.Instant

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import wallet.WalletId
import wallet.adaptor.untyped.WalletProtocol._
import wallet.domain.{ Balance, Charge, Wallet }
import wallet.newULID

object WalletAggregate {

  def props(id: WalletId, requestsLimit: Int = Int.MaxValue): Props =
    Props(new WalletAggregate(id, requestsLimit))

  def name(id: WalletId): String = "wallet-untyped-" + id.toString

}

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
          fireEvent(subscribers)(WalletDeposited(walletId, money, instant))
          context.become(
            onInitialized(
              newWallet,
              subscribers
            )
          )
      }

    case m @ PayRequest(_, walletId, toWalletId, money, maybeChargeId, instant, noReply) if walletId == id =>
      log.debug(s"message = $m")
      wallet.pay(money, maybeChargeId, instant) match {
        case Left(t) =>
          if (!noReply)
            sender() ! PayFailed(t.getMessage)
        case Right(newWallet) =>
          if (!noReply)
            sender() ! PaySucceeded
          fireEvent(subscribers)(WalletPayed(walletId, toWalletId, money, maybeChargeId, instant))
          context.become(
            onInitialized(
              newWallet,
              subscribers
            )
          )

      }

    case rr @ ChargeRequest(_, chargeId, walletId, money, instant, noReply) if walletId == id =>
      log.debug(s"message = $rr")
      wallet.addCharge(Charge(newULID, walletId, money, instant), instant) match {
        case Left(t) =>
          if (!noReply)
            sender() ! ChargeFailed(t.getMessage)
        case Right(newWallet) =>
          if (!noReply)
            sender() ! ChargeSucceeded
          fireEvent(subscribers)(WalletCharged(chargeId, walletId, money, instant))
          context.become(
            onInitialized(
              newWallet,
              subscribers
            )
          )

      }

  }

}
