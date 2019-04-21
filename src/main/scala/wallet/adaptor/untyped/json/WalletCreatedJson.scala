package wallet.adaptor.untyped.json

import java.time.Instant

import de.huxhorn.sulky.ulid.ULID
import monocle.Iso
import wallet.adaptor.untyped.WalletProtocol.WalletCreated
import wallet.domain.WalletId

final case class WalletCreatedJson(walletId: String, occurredAt: Long)

object WalletCreatedJson {

  implicit val walletCreatedJsonIso: Iso[WalletCreated, WalletCreatedJson] = Iso[WalletCreated, WalletCreatedJson] {
    event =>
      WalletCreatedJson(
        walletId = event.walletId.value.toString,
        occurredAt = event.occurredAt.toEpochMilli
      )
  } { json =>
    WalletCreated(
      walletId = WalletId(ULID.parseULID(json.walletId)),
      occurredAt = Instant.ofEpochMilli(json.occurredAt)
    )
  }

}
