package wallet.adaptor.typed

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import wallet._
import wallet.adaptor.typed.WalletProtocol._
import wallet.domain.{ Balance, Charge, Wallet }

/**
  * 永続化に対応したウォレット集約アクター。
  */
object PersistentWalletAggregate {

  sealed trait State
  case object EmptyState                  extends State
  case class DefinedState(wallet: Wallet) extends State

  def behavior(id: WalletId, chargesLimit: Int): Behavior[CommandRequest] = Behaviors.setup[CommandRequest] { ctx =>
    EventSourcedBehavior[CommandRequest, Event, State](
      persistenceId = PersistenceId("p-" + id.value.toString),
      emptyState = EmptyState,
      commandHandler = {
        case (EmptyState, c @ CreateWalletRequest(_, walletId, _, replyTo)) if walletId == id =>
          Effect.persist(c.toEvent).thenRun { _ =>
            replyTo.foreach(_ ! CreateWalletSucceeded)
          }
        case (DefinedState(wallet), c @ DepositRequest(_, walletId, _, money, createdAt, replyTo)) if walletId == id =>
          Effect.persist(c.toEvent).thenRun { _ =>
            wallet.deposit(money, createdAt) match {
              case Left(t) =>
                replyTo.foreach(_ ! DepositFailed(t.getMessage))
              case Right(_) =>
                replyTo.foreach(_ ! DepositSucceeded)
            }
          }
        case (DefinedState(wallet), c @ ChargeRequest(_, walletId, toWalletId, chargeId, money, createdAt, replyTo))
            if walletId == id =>
          Effect.persist(c.toEvent).thenRun { _ =>
            wallet.addCharge(Charge(chargeId, walletId, toWalletId, money, createdAt), createdAt) match {
              case Left(t) =>
                replyTo.foreach(_ ! ChargeFailed(t.getMessage))
              case Right(_) =>
                replyTo.foreach(_ ! ChargeSucceeded)
            }
          }
        // 支払う
        case (DefinedState(wallet), c @ WithdrawRequest(_, walletId, _, money, maybeChargeId, createdAt, replyTo))
            if walletId == id =>
          Effect.persist(c.toEvent).thenRun { _ =>
            wallet.withdraw(money, maybeChargeId, createdAt) match {
              case Left(t) =>
                replyTo.foreach(_ ! WithdrawFailed(t.getMessage))
                Behaviors.same
              case Right(_) =>
                replyTo.foreach(_ ! WithdrawSucceeded$)
            }
          }
        case (DefinedState(wallet), GetBalanceRequest(_, walletId, replyTo)) if walletId == id =>
          replyTo ! GetBalanceResponse(wallet.balance)
          Effect.none
      },
      eventHandler = {
        case (EmptyState, e: WalletCreated) =>
          DefinedState(Wallet(e.walletId, chargesLimit, Balance.zero, Vector.empty, e.occurredAt, e.occurredAt))
        case (DefinedState(wallet), e: WalletDeposited) =>
          DefinedState(wallet.deposit(e.money, e.occurredAt).right.get)
        case (DefinedState(wallet), e: WalletCharged) =>
          DefinedState(
            wallet
              .addCharge(Charge(e.chargeId, e.walletId, e.toWalletId, e.money, e.occurredAt), e.occurredAt).right.get
          )
        case (DefinedState(wallet), e: WalletWithdrawed) =>
          DefinedState(wallet.withdraw(e.money, e.chargeId, e.occurredAt).right.get)
        case (state, _) =>
          state
      }
    )
  }

}
