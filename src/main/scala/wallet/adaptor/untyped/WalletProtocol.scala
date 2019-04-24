package wallet.adaptor.untyped

import java.time.Instant

import akka.actor.ActorRef
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

  // 作成
  case class CreateWalletRequest(id: CommandId, walletId: WalletId, createdAt: Instant, noReply: Boolean = false)
      extends CommandRequest
      with ToEvent {
    override def toEvent: Event = WalletCreated(walletId, createdAt)
  }

  sealed trait CreateWalletResponse extends CommandResponse

  case object CreateWalletSucceeded extends CreateWalletResponse

  case class CreateWalletFailed(message: String) extends CreateWalletResponse

  case class WalletCreated(walletId: WalletId, occurredAt: Instant) extends Event {
    override def toCommandRequest: CommandRequest = CreateWalletRequest(newULID, walletId, occurredAt, noReply = true)
  }

  // 入金
  case class DepositRequest(
      id: CommandId,
      walletId: WalletId,
      money: Money,
      createdAt: Instant,
      noReply: Boolean = false
  ) extends CommandRequest
      with ToEvent {
    override def toEvent: Event = WalletDeposited(walletId, money, createdAt)
  }

  sealed trait DepositResponse extends CommandResponse

  case object DepositSucceeded extends DepositResponse

  case class DepositFailed(message: String) extends DepositResponse

  case class WalletDeposited(walletId: WalletId, money: Money, occurredAt: Instant) extends Event {
    override def toCommandRequest: CommandRequest = DepositRequest(newULID, walletId, money, occurredAt, noReply = true)
  }

  // 支払い
  case class WithdrawRequest(
      id: CommandId,
      walletId: WalletId,
      toWalletId: WalletId,
      money: Money,
      chargeId: Option[ChargeId],
      createdAt: Instant,
      noReply: Boolean = false
  ) extends CommandRequest
      with ToEvent {
    override def toEvent: Event = WalletWithdrawed(walletId, toWalletId, money, chargeId, createdAt)
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
      WithdrawRequest(newULID, walletId, toWalletId, money, chargeId, occurredAt, noReply = true)
  }

  // 請求
  case class ChargeRequest(
      id: CommandId,
      walletId: WalletId,
      toWalletId: WalletId,
      chargeId: ChargeId,
      money: Money,
      createdAt: Instant,
      noReply: Boolean = false
  ) extends CommandRequest
      with ToEvent {
    override def toEvent: Event = WalletCharged(walletId, toWalletId, chargeId, money, createdAt)
  }

  sealed trait ChargeResponse extends CommandResponse

  case object ChargeSucceeded extends ChargeResponse

  case class ChargeFailed(message: String) extends ChargeResponse

  case class WalletCharged(
      walletId: WalletId,
      toWalletId: WalletId,
      chargeId: ChargeId,
      money: Money,
      occurredAt: Instant
  ) extends Event {
    override def toCommandRequest: CommandRequest =
      ChargeRequest(newULID, walletId, toWalletId, chargeId, money, occurredAt, noReply = true)
  }

  // 残高確認
  case class GetBalanceRequest(id: CommandId, walletId: WalletId) extends CommandRequest

  case class GetBalanceResponse(balance: Balance) extends CommandResponse

  // 購読者
  case class AddSubscribers(id: CommandId, walletId: WalletId, subscribers: Vector[ActorRef]) extends CommandRequest

}
