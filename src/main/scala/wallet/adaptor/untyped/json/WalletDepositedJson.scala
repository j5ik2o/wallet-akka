package wallet.adaptor.untyped.json

import java.time.Instant

import de.huxhorn.sulky.ulid.ULID
import monocle.Iso
import wallet.adaptor.json.MoneyJson
import wallet.adaptor.untyped.WalletProtocol.WalletDeposited
import wallet.domain.WalletId

final case class WalletDepositedJson(walletId: String, money: MoneyJson, occurredAt: Long)

object WalletDepositedJson {

  import MoneyJson._

  implicit val walletDepositedJsonIso: Iso[WalletDeposited, WalletDepositedJson] =
    Iso[WalletDeposited, WalletDepositedJson] { event =>
      WalletDepositedJson(
        walletId = event.walletId.value.toString,
        money = moneyJsonIso.get(event.money),
        occurredAt = event.occurredAt.toEpochMilli
      )
    } { json =>
      WalletDeposited(
        walletId = WalletId(ULID.parseULID(json.walletId)),
        money = moneyJsonIso.reverseGet(json.money),
        occurredAt = Instant.ofEpochMilli(json.occurredAt)
      )
    }

}
