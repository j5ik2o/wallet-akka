package wallet.adaptor.typed

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior, SupervisorStrategy, Terminated }
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import wallet._
import wallet.adaptor.typed.WalletProtocol._

object PersistentWalletAggregate {

  // TODO: ドメインイベントにコマンドを生成するメソッドがあればロジックがすっきりするかも
  private val eventHandler: (State, Event) => State = { (state, event) =>
    event match {
      case e: WalletCreated =>
        state.childRef ! CreateWalletRequest(newULID, e.walletId, e.occurredAt)
        state
      case e: WalletDeposited =>
        state.childRef ! DepositRequest(newULID, e.walletId, e.money, e.occurredAt)
        state
      case e: WalletCharged =>
        state.childRef ! ChargeRequest(newULID, e.chargeId, e.walletId, e.money, e.occurredAt)
        state
      case e: WalletPayed =>
        state.childRef ! PayRequest(newULID, e.walletId, e.toWalletId, e.money, e.chargeId, e.occurredAt)
        state
      case e =>
        state
    }
  }

  // TODO: コマンドリクエストにドメインイベントを生成するメソッドがあればロジックがすっきりするかも
  private val commandHandler: (State, CommandRequest) => Effect[Event, State] = { (state, command) =>
    command match {
      case m: CreateWalletRequest =>
        state.childRef ! m
        Effect.persist(WalletCreated(m.walletId, m.createdAt))
      case m: DepositRequest =>
        state.childRef ! m
        Effect.persist(WalletDeposited(m.walletId, m.money, m.createdAt))
      case m: ChargeRequest =>
        state.childRef ! m
        Effect.persist(WalletCharged(m.chargeId, m.walletId, m.money, m.createdAt))
      case m: PayRequest =>
        state.childRef ! m
        Effect.persist(WalletPayed(m.walletId, m.toWalletId, m.money, m.chargeId, m.createdAt))
      case m =>
        state.childRef ! m
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
