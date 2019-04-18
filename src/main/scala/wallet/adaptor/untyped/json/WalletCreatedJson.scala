package wallet.adaptor.untyped.json

import java.time.Instant

import de.huxhorn.sulky.ulid.ULID
import monocle.Iso
import wallet.adaptor.untyped.WalletProtocol.WalletCreated

final case class WalletCreatedJson(walletId: String, occurredAt: Long)

object WalletCreatedJson {

  implicit val walletCreatedJsonIso = Iso[WalletCreated, WalletCreatedJson] { event =>
    WalletCreatedJson(
      walletId = event.walletId.toString,
      occurredAt = event.occurredAt.toEpochMilli
    )
  } { json =>
    WalletCreated(
      walletId = ULID.parseULID(json.walletId),
      occurredAt = Instant.ofEpochMilli(json.occurredAt)
    )
  }

}
