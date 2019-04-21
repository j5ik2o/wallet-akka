package wallet.adaptor.typed

import java.time.Instant

import akka.actor.typed.ActorRef
import wallet.domain.{ Balance, Money }
import wallet.{ ChargeId, CommandId, WalletId }
import wallet.newULID

object WalletProtocol {

  sealed trait Message
  sealed trait Event extends Message {
    def occurredAt: Instant
    def toCommandRequest: CommandRequest
  }
  sealed trait CommandMessage extends Message
  sealed trait CommandRequest extends CommandMessage {
    def id: CommandId
    def walletId: WalletId
  }

  trait ToEvent { this: CommandRequest =>
    def toEvent: Event
  }
  sealed trait CommandResponse extends CommandMessage

  // ウォレットの作成
  case class CreateWalletRequest(
      id: CommandId,
      walletId: WalletId,
      createdAt: Instant,
      replyTo: Option[ActorRef[CreateWalletResponse]] = None
  ) extends CommandRequest
      with ToEvent {
    override def toEvent: Event = WalletCreated(walletId, Instant.now)
  }

  sealed trait CreateWalletResponse extends Message

  case object CreateWalletSucceeded extends CreateWalletResponse

  case class CreateWalletFailed(message: String) extends CreateWalletResponse

  case class WalletCreated(walletId: WalletId, occurredAt: Instant) extends Event {
    override def toCommandRequest: CommandRequest = CreateWalletRequest(newULID, walletId, Instant.now)
  }

  // 入金
  case class DepositRequest(
      id: CommandId,
      walletId: WalletId,
      money: Money,
      createdAt: Instant,
      replyTo: Option[ActorRef[DepositResponse]] = None
  ) extends CommandRequest
      with ToEvent {
    override def toEvent: WalletDeposited = WalletDeposited(walletId, money, Instant.now)
  }

  sealed trait DepositResponse extends CommandResponse

  case object DepositSucceeded extends DepositResponse

  case class DepositFailed(message: String) extends DepositResponse

  case class WalletDeposited(walletId: WalletId, money: Money, occurredAt: Instant) extends Event {
    override def toCommandRequest: CommandRequest = DepositRequest(newULID, walletId, money, Instant.now)
  }

  // 支払い
  case class PayRequest(
      id: CommandId,
      walletId: WalletId,
      toWalletId: WalletId,
      money: Money,
      chargeId: Option[ChargeId],
      createdAt: Instant,
      replyTo: Option[ActorRef[PayResponse]] = None
  ) extends CommandRequest
      with ToEvent {
    override def toEvent: WalletPayed = WalletPayed(walletId, toWalletId, money, chargeId, Instant.now)
  }

  sealed trait PayResponse extends CommandResponse

  case object PaySucceeded extends PayResponse

  case class PayFailed(message: String) extends PayResponse

  case class WalletPayed(
      walletId: WalletId,
      toWalletId: WalletId,
      money: Money,
      chargeId: Option[ChargeId],
      occurredAt: Instant
  ) extends Event {
    override def toCommandRequest: CommandRequest =
      PayRequest(newULID, walletId, toWalletId, money, chargeId, Instant.now)
  }

  // 請求
  case class ChargeRequest(
      id: CommandId,
      walletId: WalletId,
      chargeId: ChargeId,
      money: Money,
      createdAt: Instant,
      replyTo: Option[ActorRef[ChargeResponse]] = None
  ) extends CommandRequest
      with ToEvent {
    override def toEvent: Event = WalletCharged(chargeId, walletId, money, Instant.now)
  }

  sealed trait ChargeResponse extends CommandResponse

  case object ChargeSucceeded extends ChargeResponse

  case class ChargeFailed(message: String) extends ChargeResponse

  case class WalletCharged(chargeId: ChargeId, walletId: WalletId, money: Money, occurredAt: Instant) extends Event {
    override def toCommandRequest: CommandRequest = ChargeRequest(newULID, walletId, chargeId, money, Instant.now)
  }

  // 残高確認
  case class GetBalanceRequest(id: CommandId, walletId: WalletId, replyTo: ActorRef[GetBalanceResponse])
      extends CommandRequest

  case class GetBalanceResponse(balance: Balance) extends CommandResponse

  // 購読者
  case class AddSubscribers(id: CommandId, walletId: WalletId, subscribers: Vector[ActorRef[Event]])
      extends CommandRequest

  case object Stop extends CommandRequest {
    override def id: CommandId      = throw new UnsupportedOperationException
    override def walletId: WalletId = throw new UnsupportedOperationException
  }

  case object Idle extends CommandRequest {
    override def id: CommandId      = throw new UnsupportedOperationException
    override def walletId: WalletId = throw new UnsupportedOperationException
  }
}
