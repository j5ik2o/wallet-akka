package wallet.adaptor.untyped.json

import java.time.Instant

import de.huxhorn.sulky.ulid.ULID
import monocle.Iso
import wallet.adaptor.json.MoneyJson
import wallet.adaptor.untyped.WalletProtocol.WalletRequested

final case class WalletRequestedJson(requestId: String, walletId: String, money: MoneyJson, occurredAt: Long)

object WalletRequestedJson {

  import MoneyJson._

  implicit val walletRequestedJsonIso = Iso[WalletRequested, WalletRequestedJson] { event =>
    WalletRequestedJson(
      requestId = event.requestId.toString,
      walletId = event.walletId.toString,
      money = moneyJsonIso.get(event.money),
      occurredAt = event.occurredAt.toEpochMilli
    )
  } { json =>
    WalletRequested(
      requestId = ULID.parseULID(json.requestId),
      walletId = ULID.parseULID(json.walletId),
      money = moneyJsonIso.reverseGet(json.money),
      occurredAt = Instant.ofEpochMilli(json.occurredAt)
    )
  }

}
