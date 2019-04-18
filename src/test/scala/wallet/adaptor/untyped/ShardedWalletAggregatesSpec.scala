package wallet.adaptor.untyped

import java.time.Instant

import akka.cluster.{ Cluster, MemberStatus }
import wallet.adaptor.untyped.WalletProtocol.{ CreateWalletRequest, CreateWalletSucceeded }
import wallet.newULID

import scala.concurrent.duration._

/**
  * クラスターシャーディングの単体テスト。
  */
class ShardedWalletAggregatesSpec extends AkkaSpec("""
                                  |akka.loggers = ["akka.event.slf4j.Slf4jLogger"]
                                  |akka.loglevel = "DEBUG"
                                  |akka.logging-filter = "akka.event.slf4j.Slf4jLoggingFilter"
                                  |akka.actor.debug.receive = on
                                  |
                                  |akka.actor.provider = cluster
                                  |
                                  |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
                                  |passivate-timeout = 60 seconds
                                """.stripMargin) {
  val cluster = Cluster(system)

  "ShardedWalletAggregatesSpec" - {
    "sharding" in within(15 seconds) {
      cluster.join(cluster.selfAddress)
      awaitAssert {
        cluster.selfMember.status shouldEqual MemberStatus.Up
      }

      ShardedWalletAggregatesRegion.startClusterSharding(10)
      val regionRef = ShardedWalletAggregatesRegion.shardRegion

      val walletId = newULID
      regionRef ! CreateWalletRequest(newULID, walletId, Instant.now)
      expectMsg(CreateWalletSucceeded)
    }
  }

}
