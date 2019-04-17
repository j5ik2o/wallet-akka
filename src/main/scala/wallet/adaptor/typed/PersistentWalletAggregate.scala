package wallet.adaptor.typed

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior, SupervisorStrategy, Terminated }
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import de.huxhorn.sulky.ulid.ULID
import wallet._
import wallet.adaptor.typed.WalletProtocol._

import scala.concurrent.duration._

object PersistentWalletAggregate {

  private val eventHandler: (State, Event) => State = { (state, event) =>
    event match {
      case e: WalletCreated =>
        state.childRef ! CreateWalletRequest(newULID, e.walletId)
        state
      case e: WalletDeposited =>
        state.childRef ! DepositRequest(newULID, e.walletId, e.money, e.createdAt)
        state
      case e: WalletRequested =>
        state.childRef ! RequestRequest(newULID, e.requestId, e.walletId, e.money, e.createdAt)
        state
      case e: WalletPayed =>
        state.childRef ! PayRequest(newULID, e.walletId, e.money, e.requestId, e.createdAt)
        state
    }
  }

  private val commandHandler: (State, Message) => Effect[Event, State] = { (state, command) =>
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
      case m =>
        state.childRef ! m
        Effect.none
    }
  }

  case class State(childRef: ActorRef[Message])

  def behavior(
      id: WalletId,
      receiveTimeout: FiniteDuration,
      requestsLimit: Int = Int.MaxValue
  ): Behavior[Message] =
    Behaviors
      .supervise(Behaviors.setup[Message] { context =>
        val childRef: ActorRef[Message] =
          context.spawn(WalletAggregate.behavior(id, receiveTimeout, requestsLimit), WalletAggregate.name(id))
        context.watch(childRef)
        EventSourcedBehavior[Message, Event, State](
          persistenceId = PersistenceId("abc"),
          emptyState = State(childRef),
          commandHandler,
          eventHandler
        ).receiveSignal {
          case (_, Terminated(c)) if c.compareTo(childRef) == 0 =>
            Behaviors.stopped
        }
      }).onFailure[Throwable](SupervisorStrategy.stop)
}
