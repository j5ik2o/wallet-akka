package wallet.adaptor.untyped

import akka.remote.testkit.MultiNodeConfig
import com.typesafe.config.ConfigFactory

object MultiNodeSampleConfig extends MultiNodeConfig {
  val controller = role("controller")
  val node1      = role("node1")
  val node2      = role("node2")

  testTransport(on = true)

  commonConfig(
    ConfigFactory
      .parseString(
        """
      |akka.cluster.metrics.enabled=off
      |akka.actor.provider = "akka.cluster.ClusterActorRefProvider"
      |akka.persistence.journal.plugin = "akka.persistence.journal.leveldb-shared"
      |akka.persistence.journal.leveldb-shared.store {
      |  native = off
      |  dir = "target/test-shared-journal"
      |}
      |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      |akka.persistence.snapshot-store.local.dir = "target/test-snapshots"
      |passivate-timeout = 60 seconds
    """.stripMargin
      )
  )

}
