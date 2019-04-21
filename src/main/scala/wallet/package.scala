package object wallet {
  type ULID = de.huxhorn.sulky.ulid.ULID.Value

  def newULID = new de.huxhorn.sulky.ulid.ULID().nextValue()

  type WalletId  = domain.WalletId
  type ChargeId  = domain.ChargeId
  type CommandId = ULID
}
