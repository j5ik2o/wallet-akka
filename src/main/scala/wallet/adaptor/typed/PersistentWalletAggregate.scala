package wallet.adaptor.typed

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior, SupervisorStrategy, Terminated }
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import wallet._
import wallet.adaptor.typed.WalletProtocol._

object PersistentWalletAggregate {

  private val eventHandler: (State, Event) => State = { (state, event) =>
    state.childRef ! event.toCommandRequest
    state
  }

  private val commandHandler: (State, CommandRequest) => Effect[Event, State] = { (state, command) =>
    command match {
      case commandRequest: CommandRequest with ToEvent =>
        state.childRef ! commandRequest
        Effect.persist(commandRequest.toEvent)
      case commandRequest: CommandRequest =>
        state.childRef ! commandRequest
        Effect.none
    }
  }

  case class State(childRef: ActorRef[CommandRequest])

  def behavior(id: WalletId): Behavior[CommandRequest] = behavior(id, Int.MaxValue)

  def behavior(
      id: WalletId,
      requestsLimit: Int
  ): Behavior[CommandRequest] =
    Behaviors
      .supervise(Behaviors.setup[CommandRequest] { context =>
        val childRef: ActorRef[CommandRequest] =
          context.spawn(WalletAggregate.behavior(id, requestsLimit), WalletAggregate.name(id))
        context.watch(childRef)
        EventSourcedBehavior[CommandRequest, Event, State](
          persistenceId = PersistenceId("p-" + id.toString),
          emptyState = State(childRef),
          commandHandler,
          eventHandler
        ).receiveSignal {
          case (_, Terminated(c)) if c.compareTo(childRef) == 0 =>
            Behaviors.stopped
        }
      }).onFailure[Throwable](SupervisorStrategy.stop)
}
