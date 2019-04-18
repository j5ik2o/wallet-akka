package wallet.adaptor.typed

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.{ ScalaTestWithActorTestKit, TestProbe }
import akka.actor.typed.ActorSystem
import akka.cluster.MemberStatus
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.{ Cluster, Join }
import org.scalatest.FreeSpecLike
import wallet.adaptor.typed.WalletProtocol.{ CreateWalletRequest, CreateWalletResponse, CreateWalletSucceeded }
import wallet.newULID

import scala.concurrent.duration._

/**
  * クラスターシャーディングの単体テスト。
  */
class ShardedWalletAggregatesSpec
    extends ScalaTestWithActorTestKit("""
                                    |akka.loglevel = DEBUG
                                    |akka.loggers = ["akka.event.slf4j.Slf4jLogger"]
                                    |akka.logging-filter = "akka.event.slf4j.Slf4jLoggingFilter"
                                    |akka.actor.debug.receive = on
                                    |
                                    |akka.actor.provider = cluster
                                    |
                                    |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
                                    |passivate-timeout = 60 seconds
      """.stripMargin)
    with FreeSpecLike
    with ActorSpecSupport {

  def typedSystem[T]: ActorSystem[T] = system.asInstanceOf[ActorSystem[T]]

  val cluster         = Cluster(system)
  val clusterSharding = ClusterSharding(typedSystem)

  "ShardedWalletAggregatesSpec" - {
    "sharding" in {
      cluster.manager ! Join(cluster.selfMember.address)
      eventually {
        cluster.selfMember.status shouldEqual MemberStatus.Up
      }
      ShardedWalletAggregates.initClusterSharding(clusterSharding, 10, 1 hours)

      val probe     = TestProbe[CreateWalletResponse]()(typedSystem)
      val walletId  = newULID
      val walletRef = clusterSharding.entityRefFor(ShardedWalletAggregates.TypeKey, walletId.toString)
      walletRef ! CreateWalletRequest(newULID, walletId, Instant.now, Some(probe.ref))
      probe.expectMessage(CreateWalletSucceeded)
    }
  }

}
