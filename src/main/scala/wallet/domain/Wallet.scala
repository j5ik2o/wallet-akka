package wallet.domain

import wallet.ULID

final case class Wallet(id: ULID, balance: Balance) {

  def withBalance(value: Balance): Wallet = {
    copy(balance = value)
  }

  def addBalance(other: Money): Wallet = {
    copy(balance = balance.add(other))
  }

  def subBalance(other: Money): Wallet = {
    copy(balance = balance.sub(other))
  }

}
