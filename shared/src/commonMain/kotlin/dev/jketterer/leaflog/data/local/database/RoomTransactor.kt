package dev.jketterer.leaflog.data.local.database

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import dev.jketterer.leaflog.domain.repositories.Transactor

/** DAO calls made inside the block run on the transaction's connection. */
class RoomTransactor(private val database: LeafLogDatabase) : Transactor {
    override suspend fun <T> invoke(block: suspend () -> T): T =
        database.useWriterConnection { it.immediateTransaction { block() } }
}
