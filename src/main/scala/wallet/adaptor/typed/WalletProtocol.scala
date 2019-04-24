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
      toWalletId: WalletId,
      money: Money,
      createdAt: Instant,
      replyTo: Option[ActorRef[DepositResponse]] = None
  ) extends CommandRequest
      with ToEvent {
    override def toEvent: WalletDeposited = WalletDeposited(walletId, toWalletId, money, Instant.now)
  }

  sealed trait DepositResponse extends CommandResponse

  case object DepositSucceeded extends DepositResponse

  case class DepositFailed(message: String) extends DepositResponse

  case class WalletDeposited(walletId: WalletId, toWalletId: WalletId, money: Money, occurredAt: Instant)
      extends Event {
    override def toCommandRequest: CommandRequest = DepositRequest(newULID, walletId, toWalletId, money, Instant.now)
  }

  // 出金
  case class WithdrawRequest(
      id: CommandId,
      walletId: WalletId, // from
      toWalletId: WalletId,
      money: Money,
      chargeId: Option[ChargeId],
      createdAt: Instant,
      replyTo: Option[ActorRef[WithdrawResponse]] = None
  ) extends CommandRequest
      with ToEvent {
    override def toEvent: WalletWithdrawed = WalletWithdrawed(walletId, toWalletId, money, chargeId, Instant.now)
  }

  sealed trait WithdrawResponse extends CommandResponse

  case object WithdrawSucceeded$ extends WithdrawResponse

  case class WithdrawFailed(message: String) extends WithdrawResponse

  case class WalletWithdrawed(
      walletId: WalletId,
      toWalletId: WalletId,
      money: Money,
      chargeId: Option[ChargeId],
      occurredAt: Instant
  ) extends Event {
    override def toCommandRequest: CommandRequest =
      WithdrawRequest(newULID, walletId, toWalletId, money, chargeId, Instant.now)
  }

  // 請求
  case class ChargeRequest(
      id: CommandId,
      walletId: WalletId,
      toWalletId: WalletId,
      chargeId: ChargeId,
      money: Money,
      createdAt: Instant,
      replyTo: Option[ActorRef[ChargeResponse]] = None
  ) extends CommandRequest
      with ToEvent {
    override def toEvent: Event = WalletCharged(chargeId, walletId, toWalletId, money, Instant.now)
  }

  sealed trait ChargeResponse extends CommandResponse

  case object ChargeSucceeded extends ChargeResponse

  case class ChargeFailed(message: String) extends ChargeResponse

  case class WalletCharged(
      chargeId: ChargeId,
      walletId: WalletId,
      toWalletId: WalletId,
      money: Money,
      occurredAt: Instant
  ) extends Event {
    override def toCommandRequest: CommandRequest =
      ChargeRequest(newULID, walletId, toWalletId, chargeId, money, Instant.now)
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
