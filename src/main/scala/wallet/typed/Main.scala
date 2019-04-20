package wallet.typed

import java.time.Instant

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.{ actor => untyped }
import wallet.adaptor.typed.{ ShardedWalletAggregates, WalletProtocol }
import wallet.adaptor.typed.WalletProtocol.{ CreateWalletRequest, DepositRequest }
import wallet.domain.Money

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App {

  val system          = ActorSystem.wrap(untyped.ActorSystem("wallet-system"))
  val clusterSharding = ClusterSharding(system)

  ShardedWalletAggregates.initEntityActor(clusterSharding, 256, 1 hours)

  val walletId = wallet.newULID

  val walletRef: EntityRef[WalletProtocol.CommandRequest] =
    clusterSharding.entityRefFor(ShardedWalletAggregates.TypeKey, walletId.toString)

  walletRef ! CreateWalletRequest(wallet.newULID, walletId, Instant.now, None)
  walletRef ! DepositRequest(wallet.newULID, walletId, Money(BigDecimal(100)), Instant.now, None)

  sys.addShutdownHook {
    system.terminate()
    Await.result(system.whenTerminated, Duration.Inf)
  }
}
