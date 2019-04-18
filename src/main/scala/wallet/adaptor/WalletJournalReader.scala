package wallet.adaptor

import akka.NotUsed
import akka.actor.ActorSystem
import akka.persistence.query.{ EventEnvelope, PersistenceQuery }
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.persistence.query.scaladsl._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source

object WalletJournalReader {

  type ReadJournalType =
    ReadJournal
      with CurrentPersistenceIdsQuery
      with PersistenceIdsQuery
      with CurrentEventsByPersistenceIdQuery
      with EventsByPersistenceIdQuery
      with CurrentEventsByTagQuery
      with EventsByTagQuery

}

class WalletJournalReader(implicit system: ActorSystem) {
  implicit val mat = ActorMaterializer()(system)

  private val readJournal: WalletJournalReader.ReadJournalType =
    PersistenceQuery(system).readJournalFor[LeveldbReadJournal](LeveldbReadJournal.Identifier)

  def eventsByPersistenceId(pid: String): Source[EventEnvelope, NotUsed] =
    readJournal.eventsByPersistenceId(pid, 0L, Long.MaxValue)

}
