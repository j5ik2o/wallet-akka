package wallet.adaptor.typed

import akka.actor.testkit.typed.scaladsl.{ ActorTestKit, ScalaTestWithActorTestKit, TestProbe }
import akka.actor.typed.ActorSystem
import akka.cluster.MemberStatus
import akka.cluster.typed.{ Cluster, Join }
import com.typesafe.config.ConfigFactory
import org.scalatest.{ FreeSpecLike, Matchers }
import wallet.adaptor.typed.WalletProtocol.{ CreateWalletRequest, CreateWalletResponse, CreateWalletSucceeded }
import wallet.newULID

import scala.concurrent.duration._

class ShardedWalletAggregatesSpec
    extends ScalaTestWithActorTestKit(
      ActorTestKit(
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
    with ActorTestSupport {

  def typedSystem[T]: ActorSystem[T] = system.asInstanceOf[ActorSystem[T]]

  val cluster = Cluster(system)

  "ShardedWalletAggregatesSpec" - {
    "sharding" in {
      cluster.manager ! Join(cluster.selfMember.address)
      eventually {
        cluster.selfMember.status shouldEqual MemberStatus.Up
      }
      val sharding = ShardedWalletAggregates.newClusterSharding(typedSystem)
      ShardedWalletAggregates.initClusterSharding(sharding, 10, 1 hours)

      val probe     = TestProbe[CreateWalletResponse]()(typedSystem)
      val walletId  = newULID
      val entityRef = sharding.entityRefFor(ShardedWalletAggregates.TypeKey, walletId.toString)
      entityRef ! CreateWalletRequest(newULID, walletId, Some(probe.ref))
      probe.expectMessage(CreateWalletSucceeded)
    }
  }

}
