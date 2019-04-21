package wallet.adaptor.typed

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior, SupervisorStrategy, Terminated }
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import wallet._
import wallet.adaptor.typed.WalletProtocol._

/**
  * 永続化に対応したウォレット集約アクター。
  */
object PersistentWalletAggregate {

  case class State(childRef: ActorRef[CommandRequest])

  def behavior(
      id: WalletId,
      chargesLimit: Int = Int.MaxValue
  ): Behavior[CommandRequest] =
    Behaviors
      .supervise(Behaviors.setup[CommandRequest] { ctx =>
        val childRef: ActorRef[CommandRequest] =
          ctx.spawn(WalletAggregate.behavior(id, chargesLimit), WalletAggregate.name(id))
        ctx.watch(childRef)
        EventSourcedBehavior[CommandRequest, Event, State](
          persistenceId = PersistenceId("p-" + id.value.toString),
          emptyState = State(childRef),
          commandHandler = {
            case (state, commandRequest: CommandRequest with ToEvent) =>
              state.childRef ! commandRequest
              Effect.persist(commandRequest.toEvent)
            case (state, commandRequest: CommandRequest) =>
              state.childRef ! commandRequest
              Effect.none
          },
          eventHandler = { (state, event) =>
            state.childRef ! event.toCommandRequest
            state
          }
        ).receiveSignal {
          case (_, Terminated(c)) if c == childRef =>
            Behaviors.stopped
        }
      }).onFailure[Throwable](SupervisorStrategy.stop)
}
