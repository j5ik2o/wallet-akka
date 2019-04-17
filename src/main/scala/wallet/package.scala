import wallet.utils.ULID
package object wallet {
  type WalletId  = ULID
  type RequestId = ULID
  type CommandId = ULID
}
