package dev.jketterer.leaflog.domain.repositories

/**
 * Runs [block] as one database transaction: if it throws, none of its writes land.
 *
 * Keep side effects (timers, reminders, files) outside the block, after it returns, so a
 * rollback can't leave them half-done.
 */
interface Transactor {
    suspend operator fun <T> invoke(block: suspend () -> T): T
}
