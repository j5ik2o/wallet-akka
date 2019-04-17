package wallet.adaptor.typed

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.persistence.RecoveryCompleted
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import wallet.WalletId
import wallet.adaptor.typed.WalletProtocol.{
  Command,
  CommandRequest,
  CreateWalletRequest,
  DepositRequest,
  Event,
  PayRequest,
  RequestRequest,
  WalletCreated,
  WalletDeposited,
  WalletPayed,
  WalletRequested
}
import wallet.utils.ULID

import scala.concurrent.duration._

object PersistentWalletAggregate {

  private val eventHandler: (State, Event) => State = { (state, event) =>
    event match {
      case e: WalletCreated =>
        state.childRef ! CreateWalletRequest(ULID.generate, e.walletId)
        state
      case e: WalletDeposited =>
        state.childRef ! DepositRequest(ULID.generate, e.walletId, e.money, e.createdAt)
        state
      case e: WalletRequested =>
        state.childRef ! RequestRequest(ULID.generate, e.requestId, e.walletId, e.money, e.createdAt)
        state
      case e: WalletPayed =>
        state.childRef ! PayRequest(ULID.generate, e.walletId, e.money, e.requestId, e.createdAt)
        state
    }
  }

  private val commandHandler: (State, CommandRequest) => Effect[Event, State] = { (state, command) =>
    command match {
      case m: CreateWalletRequest =>
        state.childRef ! m
        Effect.persist(WalletCreated(m.walletId))
      case m: DepositRequest =>
        state.childRef ! m
        Effect.persist(WalletDeposited(m.walletId, m.money, m.createdAt))
      case m: RequestRequest =>
        state.childRef ! m
        Effect.persist(WalletRequested(m.requestId, m.walletId, m.money, m.createdAt))
      case m: PayRequest =>
        state.childRef ! m
        Effect.persist(WalletPayed(m.walletId, m.money, m.requestId, m.createdAt))
      case m: CommandRequest =>
        state.childRef ! m
        Effect.none
    }
  }

  case class State(childRef: ActorRef[CommandRequest])

  def behavior(
      id: WalletId,
      receiveTimeout: FiniteDuration,
      requestsLimit: Int = Int.MaxValue
  ): Behavior[CommandRequest] =
    Behaviors.setup { implicit context =>
      val childRef: ActorRef[CommandRequest] =
        context.spawn(WalletAggregate.behavior(id, receiveTimeout, requestsLimit), WalletAggregate.name(id))
      EventSourcedBehavior[CommandRequest, Event, State](
        persistenceId = PersistenceId("abc"),
        emptyState = State(childRef),
        commandHandler,
        eventHandler
      )
    }
}
