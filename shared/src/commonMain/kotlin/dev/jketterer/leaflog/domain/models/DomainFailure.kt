package dev.jketterer.leaflog.domain.models

/** A failure the user can be told the reason for, instead of only that something failed. */
sealed class DomainFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** What the operation needed is gone, most likely deleted elsewhere. */
    class NotFound(what: String) : DomainFailure("Not found: $what")

    /** Refused: [count] [by] still refer to it. */
    class InUse(val count: Int, val by: Dependents) :
        DomainFailure("In use by $count ${by.name.lowercase()}")

    /** Refused: it's the last of its kind, and one must remain. */
    class LastRemaining(what: String) : DomainFailure("The last $what can't be deleted")

    /** A file couldn't be read or written. */
    class Io(cause: Throwable) : DomainFailure("File access failed", cause)

    class NotABackup : DomainFailure("Not a Leaf Log backup")

    class NewerBackup(version: Int) : DomainFailure("Backup version $version is newer than this app")

    enum class Dependents { SESSIONS, TEAS }
}
