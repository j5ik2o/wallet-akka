package wallet.adaptor.typed

import java.io.File

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.apache.commons.io.FileUtils

import scala.util.Try

trait PersistenceCleanup {

  val testKit: ActorTestKit

  val storageLocations = List(
    "akka.persistence.journal.leveldb.dir",
    "akka.persistence.journal.leveldb-shared.store.dir",
    "akka.persistence.snapshot-store.local.dir"
  ).map { s =>
    new File(testKit.system.settings.config.getString(s))
  }

  def deleteStorageLocations(): Unit = {
    storageLocations.foreach(dir => Try(FileUtils.deleteDirectory(dir)))
  }
}
