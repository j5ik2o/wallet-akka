package wallet.adaptor.untyped

import akka.actor.ActorSystem
import akka.cluster.{ Cluster, MemberStatus }
import akka.testkit.{ ImplicitSender, TestKit }
import com.typesafe.config.ConfigFactory
import org.scalatest.{ BeforeAndAfterAll, FreeSpecLike, Matchers }
import wallet.adaptor.untyped.WalletProtocol.{ CreateWalletRequest, CreateWalletSucceeded }
import wallet.newULID

import scala.concurrent.duration._

class ShardedWalletAggregatesSpec
    extends TestKit(
      ActorSystem(
        "ShardedWalletAggregatesSpec",
        ConfigFactory.parseString("""
                                  |akka.loggers = ["akka.event.slf4j.Slf4jLogger"]
                                  |akka.loglevel = "DEBUG"
                                  |akka.logging-filter = "akka.event.slf4j.Slf4jLoggingFilter"
                                  |akka.actor.debug.receive = on
                                  |
                                  |akka.actor.provider = cluster
                                  |
                                  |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
                                  |passivate-timeout = 60 seconds
                                """.stripMargin)
      )
    )
    with FreeSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender
    with ActorTestSupport {

  val cluster = Cluster(system)

  "ShardedWalletAggregatesSpec" - {
    "sharding" in within(15 seconds) {
      cluster.join(cluster.selfAddress)
      awaitAssert {
        cluster.selfMember.status shouldEqual MemberStatus.Up
      }

      ShardedWalletAggregatesRegion.startClusterSharding(system)(10)
      val regionRef = ShardedWalletAggregatesRegion.getShardRegion(system)

      val walletId = newULID
      regionRef ! CreateWalletRequest(newULID, walletId)
      expectMsg(CreateWalletSucceeded)
    }
  }

}
