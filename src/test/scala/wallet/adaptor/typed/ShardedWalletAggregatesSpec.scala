//package wallet.adaptor.typed
//
//import java.util.UUID
//
//import akka.actor.typed.scaladsl.adapter._
//import akka.pattern.ask
//import akka.actor.{ ActorIdentity, ActorPath, Identify, Props, ActorSystem => UntypedSystem }
//import akka.actor.testkit.typed.scaladsl.{ ActorTestKit, ScalaTestWithActorTestKit }
//import akka.actor.typed.ActorSystem
//import akka.persistence.journal.leveldb.{ SharedLeveldbJournal, SharedLeveldbStore }
//import org.scalatest.{ FreeSpecLike, Matchers }
//import akka.actor.typed.scaladsl.adapter._
//import com.typesafe.config.ConfigFactory
//
//import scala.concurrent.ExecutionContext
//import scala.concurrent.duration._
//
//class ShardedWalletAggregatesSpec
//    extends ScalaTestWithActorTestKit(
//      ActorTestKit(
//        "ShardedWalletAggregatesSpec",
//        ConfigFactory.parseString("""
//                                                                                                              |akka.loggers = ["akka.event.slf4j.Slf4jLogger"]
//                                                                                                              |akka.loglevel = "DEBUG"
//                                                                                                              |akka.logging-filter = "akka.event.slf4j.Slf4jLoggingFilter"
//                                                                                                              |akka.actor.debug.receive = on
//                                                                                                              |
//                                                                                                              |akka.cluster.metrics.enabled=off
//                                                                                                              |akka.actor.provider = "akka.cluster.ClusterActorRefProvider"
//                                                                                                              |akka.persistence.journal.plugin = "akka.persistence.journal.leveldb-shared"
//                                                                                                              |akka.persistence.journal.leveldb-shared.store {
//                                                                                                              |  native = off
//                                                                                                              |  dir = "target/test-shared-journal"
//                                                                                                              |}
//                                                                                                              |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
//                                                                                                              |akka.persistence.snapshot-store.local.dir = "target/test-snapshots"
//                                                                                                              |passivate-timeout = 60 seconds
//      """.stripMargin)
//      )
//    )
//    with FreeSpecLike
//    with Matchers
//    with ActorTestSupport
//    with PersistenceCleanup {
//
//  def typedSystem[T]: ActorSystem[T] = system.asInstanceOf[ActorSystem[T]]
//
//  override protected def beforeAll(): Unit = {
//    deleteStorageLocations()
//    super.beforeAll()
//    startupSharedJournal(true, system.path / "user" / "store")(system.toUntyped, system.executionContext)
//  }
//
//  override protected def afterAll(): Unit = {
//    deleteStorageLocations()
//    super.afterAll()
//  }
//
//  "ShardedWalletAggregatesSpec" - {
//    "sharding" in {
//      val sharding = ShardedWalletAggregates.newClusterSharding(typedSystem)
//      ShardedWalletAggregates.initClusterSharding(sharding, 10, 1 hours)
//    }
//  }
//
//  def startupSharedJournal(startStore: Boolean, path: ActorPath)(implicit system: UntypedSystem,
//                                                                 ctx: ExecutionContext): Unit = {
//    if (startStore)
//      system.actorOf(Props[SharedLeveldbStore], "/user/store")
//
//    val actorSelection = system.actorSelection(path)
//    val future         = actorSelection ? Identify(UUID.randomUUID())
//    future.onSuccess {
//      case ActorIdentity(_, Some(ref)) =>
//        SharedLeveldbJournal.setStore(ref, system)
//      case x ⇒
//        system.log.error("Shared journal not started at {}", path)
//        system.terminate()
//    }
//    future.onFailure {
//      case _ ⇒
//        system.log.error("Lookup of shared journal at {} timed out", path)
//        system.terminate()
//    }
//  }
//
//}
