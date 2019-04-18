package object wallet {
  type ULID = de.huxhorn.sulky.ulid.ULID.Value

  def newULID = new de.huxhorn.sulky.ulid.ULID().nextValue()

  type WalletId  = ULID
  type ChargeId  = ULID
  type CommandId = ULID
}
