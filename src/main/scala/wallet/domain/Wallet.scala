package wallet.domain

import java.time.Instant

import wallet.ULID

final case class WalletId(value: ULID) extends IDSupport

final case class Wallet(
    id: WalletId,
    chargesLimit: Int,
    balance: Balance,
    charges: Vector[Charge],
    createdAt: Instant,
    updatedAt: Instant
) {

  def deposit(money: Money, updatedAt: Instant): Either[Throwable, Wallet] = {
    if (balance.add(money) < Balance.zero)
      Left(new IllegalArgumentException("Can not trade because the balance after trading is less than 0"))
    else
      Right(copy(balance = balance.add(money), updatedAt = updatedAt))
  }

  def withdraw(money: Money, maybeChargeId: Option[ChargeId], updatedAt: Instant): Either[Throwable, Wallet] = {
    if (maybeChargeId.nonEmpty && !charges.map(_.id).exists(v => maybeChargeId.contains(v)))
      Left(new IllegalArgumentException("ChargeId is not found"))
    else if (balance.sub(money) < Balance.zero)
      Left(new IllegalArgumentException("Can not trade because the balance after trading is less than 0"))
    else {
      Right(copy(balance = balance.sub(money), charges = maybeChargeId.fold(charges) { v =>
        charges.filterNot(_.id == v)
      }, updatedAt = updatedAt))
    }
  }

  def addCharge(charge: Charge, updatedAt: Instant): Either[Throwable, Wallet] = {
    if (charges.size < chargesLimit)
      Right(copy(charges = charges :+ charge, updatedAt = updatedAt))
    else
      Left(new IllegalArgumentException("The number of charges is limit over"))
  }

  def removeCharge(chargeId: ChargeId, updatedAt: Instant): Either[Throwable, Wallet] = {
    if (charges.nonEmpty)
      Right(copy(charges = charges.filterNot(_.id == chargeId), updatedAt = updatedAt))
    else
      Left(new IllegalArgumentException("The number of charges is limit over"))
  }

}
