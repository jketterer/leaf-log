package android.database

/**
 * Stands in for the mockable android.jar's, whose constructors drop the message. Room's `@Upsert`
 * reads the message to tell a key conflict from a real failure, so without this every upsert onto
 * an existing row throws on the host. The name must match the framework class exactly.
 */
@Suppress("ClassName")
open class SQLException(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    constructor() : this(null, null)
    constructor(message: String?) : this(message, null)
}
