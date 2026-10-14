package dev.jketterer.leaflog.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import dev.jketterer.leaflog.data.local.database.dao.RECOMPUTE_CONFIGURATION_STATS
import dev.jketterer.leaflog.data.local.database.dao.RECOMPUTE_TEA_STATS

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = OFF")

        // ── tea table: drop defaultBrewingTime ──────────────────────────
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS `tea_new` (
                `id` TEXT NOT NULL, `name` TEXT NOT NULL, `teaTypeId` TEXT NOT NULL,
                `origin` TEXT, `producer` TEXT, `purchaseDate` INTEGER,
                `purchasePrice` REAL, `stockAmount` INTEGER,
                `defaultTemperatureCelsius` INTEGER,
                `description` TEXT, `photos` TEXT NOT NULL, `isFavorite` INTEGER NOT NULL,
                `totalSessions` INTEGER NOT NULL, `averageRating` REAL,
                `lastBrewedAt` INTEGER, `userId` TEXT, `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, `syncStatus` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`teaTypeId`) REFERENCES `tea_type`(`id`)
                    ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """)
        connection.execSQL("""
            INSERT INTO `tea_new`
                SELECT `id`, `name`, `teaTypeId`, `origin`, `producer`, `purchaseDate`,
                       `purchasePrice`, `stockAmount`, `defaultTemperatureCelsius`,
                       `description`, `photos`, `isFavorite`,
                       `totalSessions`, `averageRating`, `lastBrewedAt`, `userId`,
                       `createdAt`, `updatedAt`, `deletedAt`, `syncStatus`
                FROM `tea`
        """)
        connection.execSQL("DROP TABLE `tea`")
        connection.execSQL("ALTER TABLE `tea_new` RENAME TO `tea`")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_tea_teaTypeId` ON `tea` (`teaTypeId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_tea_name` ON `tea` (`name`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_tea_isFavorite` ON `tea` (`isFavorite`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_tea_userId` ON `tea` (`userId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_tea_deletedAt` ON `tea` (`deletedAt`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_tea_lastBrewedAt` ON `tea` (`lastBrewedAt`)")

        // ── tea_type table: drop defaultBrewingTime ──────────────────────
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS `tea_type_new` (
                `id` TEXT NOT NULL, `name` TEXT NOT NULL,
                `defaultTemperatureCelsius` INTEGER, `colorHex` TEXT NOT NULL,
                `isSystemDefault` INTEGER NOT NULL, `displayOrder` INTEGER NOT NULL,
                `userId` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                PRIMARY KEY(`id`)
            )
        """)
        connection.execSQL("""
            INSERT INTO `tea_type_new`
                SELECT `id`, `name`, `defaultTemperatureCelsius`, `colorHex`,
                       `isSystemDefault`, `displayOrder`, `userId`,
                       `createdAt`, `updatedAt`, `deletedAt`
                FROM `tea_type`
        """)
        connection.execSQL("DROP TABLE `tea_type`")
        connection.execSQL("ALTER TABLE `tea_type_new` RENAME TO `tea_type`")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_tea_type_displayOrder` ON `tea_type` (`displayOrder`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_tea_type_userId` ON `tea_type` (`userId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_tea_type_deletedAt` ON `tea_type` (`deletedAt`)")

        connection.execSQL("PRAGMA foreign_keys = ON")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        // Add isArchived column with default value of 0 (false)
        connection.execSQL(
            "ALTER TABLE `brewing_vessel` ADD COLUMN `isArchived` INTEGER NOT NULL DEFAULT 0"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_brewing_vessel_isArchived` ON `brewing_vessel` (`isArchived`)"
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = OFF")

        // ── tea_type table: defaultTemperatureCelsius INTEGER → REAL ────────
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS `tea_type_new` (
                `id` TEXT NOT NULL, `name` TEXT NOT NULL,
                `defaultTemperatureCelsius` REAL, `colorHex` TEXT NOT NULL,
                `isSystemDefault` INTEGER NOT NULL, `displayOrder` INTEGER NOT NULL,
                `userId` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                PRIMARY KEY(`id`)
            )
        """)
        connection.execSQL("""
            INSERT INTO `tea_type_new`
                SELECT `id`, `name`, `defaultTemperatureCelsius`, `colorHex`,
                       `isSystemDefault`, `displayOrder`, `userId`,
                       `createdAt`, `updatedAt`, `deletedAt`
                FROM `tea_type`
        """)
        connection.execSQL("DROP TABLE `tea_type`")
        connection.execSQL("ALTER TABLE `tea_type_new` RENAME TO `tea_type`")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_tea_type_displayOrder` ON `tea_type` (`displayOrder`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_tea_type_userId` ON `tea_type` (`userId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_tea_type_deletedAt` ON `tea_type` (`deletedAt`)")

        // ── tea table: defaultTemperatureCelsius INTEGER → REAL ─────────────
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS `tea_new` (
                `id` TEXT NOT NULL, `name` TEXT NOT NULL, `teaTypeId` TEXT NOT NULL,
                `origin` TEXT, `producer` TEXT, `purchaseDate` INTEGER,
                `purchasePrice` REAL, `stockAmount` INTEGER,
                `defaultTemperatureCelsius` REAL,
                `description` TEXT, `photos` TEXT NOT NULL, `isFavorite` INTEGER NOT NULL,
                `totalSessions` INTEGER NOT NULL, `averageRating` REAL,
                `lastBrewedAt` INTEGER, `userId` TEXT, `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, `syncStatus` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`teaTypeId`) REFERENCES `tea_type`(`id`)
                    ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """)
        connection.execSQL("""
            INSERT INTO `tea_new`
                SELECT `id`, `name`, `teaTypeId`, `origin`, `producer`, `purchaseDate`,
                       `purchasePrice`, `stockAmount`, `defaultTemperatureCelsius`,
                       `description`, `photos`, `isFavorite`,
                       `totalSessions`, `averageRating`, `lastBrewedAt`, `userId`,
                       `createdAt`, `updatedAt`, `deletedAt`, `syncStatus`
                FROM `tea`
        """)
        connection.execSQL("DROP TABLE `tea`")
        connection.execSQL("ALTER TABLE `tea_new` RENAME TO `tea`")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_tea_teaTypeId` ON `tea` (`teaTypeId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_tea_name` ON `tea` (`name`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_tea_isFavorite` ON `tea` (`isFavorite`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_tea_userId` ON `tea` (`userId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_tea_deletedAt` ON `tea` (`deletedAt`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_tea_lastBrewedAt` ON `tea` (`lastBrewedAt`)")

        connection.execSQL("PRAGMA foreign_keys = ON")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE `tea_session` ADD COLUMN `timerTotalMs` INTEGER DEFAULT NULL"
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        // Add pin fields to brewing_configurations
        connection.execSQL(
            "ALTER TABLE `brewing_configurations` ADD COLUMN `is_pinned` INTEGER NOT NULL DEFAULT 0"
        )
        connection.execSQL(
            "ALTER TABLE `brewing_configurations` ADD COLUMN `pinned_sort_order` INTEGER NOT NULL DEFAULT 0"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_brewing_configurations_is_pinned` ON `brewing_configurations` (`is_pinned`)"
        )
    }
}

/**
 * Session fields move to the parent row only, and a parent's status comes to mean its session's.
 * Drops `averageRating` (session ratings are computed) and adds `actualBrewingTimeMs`, unfilled:
 * an old row can't tell a natural finish from Complete Now.
 *
 * The old table is renamed away before the copy rather than dropped after it. Dropping
 * `tea_session` while the new table's self-reference points at that name would cascade-delete
 * every child steep if foreign keys are enforced during the migration.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `tea_session` RENAME TO `tea_session_v6`")
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tea_session` (`id` TEXT NOT NULL, `teaId` TEXT NOT NULL,
                `parentSessionId` TEXT, `steepNumber` INTEGER NOT NULL, `status` TEXT NOT NULL,
                `teaQuantityGrams` REAL, `vesselId` TEXT, `waterType` TEXT, `location` TEXT,
                `usedConfigurationId` TEXT, `rating` REAL, `timestamp` INTEGER NOT NULL,
                `brewingTime` INTEGER NOT NULL, `temperatureCelsius` REAL NOT NULL,
                `waterQuantityMl` REAL NOT NULL, `notes` TEXT, `photos` TEXT NOT NULL,
                `userId` TEXT, `syncStatus` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, `timerStatus` TEXT,
                `timerStartedAt` INTEGER, `timerPausedAt` INTEGER, `timerRemainingMs` INTEGER,
                `timerTotalMs` INTEGER, `actualBrewingTimeMs` INTEGER, PRIMARY KEY(`id`),
                FOREIGN KEY(`teaId`) REFERENCES `tea`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE ,
                FOREIGN KEY(`vesselId`) REFERENCES `brewing_vessel`(`id`)
                    ON UPDATE NO ACTION ON DELETE RESTRICT ,
                FOREIGN KEY(`parentSessionId`) REFERENCES `tea_session`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE )
            """
        )
        // Version 6 completed the parent as soon as steep 2 started, so a session is in
        // progress if its parent or any live steep still is. Parents go first for the
        // self-reference.
        connection.execSQL(
            """
            INSERT INTO `tea_session` (`id`, `teaId`, `parentSessionId`, `steepNumber`, `status`,
                `teaQuantityGrams`, `vesselId`, `waterType`, `location`, `usedConfigurationId`,
                `rating`, `timestamp`, `brewingTime`, `temperatureCelsius`, `waterQuantityMl`,
                `notes`, `photos`, `userId`, `syncStatus`, `createdAt`, `updatedAt`, `deletedAt`,
                `timerStatus`, `timerStartedAt`, `timerPausedAt`, `timerRemainingMs`,
                `timerTotalMs`)
            SELECT `id`, `teaId`, `parentSessionId`, `steepNumber`,
                CASE
                    WHEN `parentSessionId` IS NOT NULL THEN `status`
                    WHEN `status` = 'IN_PROGRESS' OR EXISTS (
                        SELECT 1 FROM `tea_session_v6` c
                        WHERE c.`parentSessionId` = s.`id` AND c.`deletedAt` IS NULL
                        AND c.`status` = 'IN_PROGRESS'
                    ) THEN 'IN_PROGRESS'
                    ELSE 'COMPLETED'
                END,
                CASE WHEN `parentSessionId` IS NULL THEN `teaQuantityGrams` END,
                CASE WHEN `parentSessionId` IS NULL THEN `vesselId` END,
                CASE WHEN `parentSessionId` IS NULL THEN `waterType` END,
                CASE WHEN `parentSessionId` IS NULL THEN `location` END,
                CASE WHEN `parentSessionId` IS NULL THEN `usedConfigurationId` END,
                `rating`, `timestamp`, `brewingTime`, `temperatureCelsius`, `waterQuantityMl`,
                `notes`, `photos`, `userId`, `syncStatus`, `createdAt`, `updatedAt`, `deletedAt`,
                `timerStatus`, `timerStartedAt`, `timerPausedAt`, `timerRemainingMs`,
                `timerTotalMs`
            FROM `tea_session_v6` s
            ORDER BY `parentSessionId` IS NOT NULL
            """
        )
        connection.execSQL("DROP TABLE `tea_session_v6`")
        listOf(
            "teaId", "vesselId", "parentSessionId", "steepNumber", "status", "timestamp",
            "userId", "deletedAt", "usedConfigurationId",
        ).forEach { column ->
            connection.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_tea_session_$column` " +
                    "ON `tea_session` (`$column`)"
            )
        }

        connection.execSQL(RECOMPUTE_TEA_STATS)
        connection.execSQL(RECOMPUTE_CONFIGURATION_STATS)
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `tea` ADD COLUMN `finishedAt` INTEGER")
        connection.execSQL("ALTER TABLE `tea` DROP COLUMN `purchasePrice`")
        connection.execSQL("ALTER TABLE `tea` DROP COLUMN `stockAmount`")
        connection.execSQL("ALTER TABLE `brewing_configurations` ADD COLUMN `deleted_at` INTEGER")
        connection.execSQL(
            "UPDATE `brewing_configurations` SET `deleted_at` = `updated_at` WHERE `is_active` = 0"
        )
        connection.execSQL("ALTER TABLE `brewing_configurations` DROP COLUMN `is_active`")

        // Deletes used to ignore what still pointed at the row. Bring back whatever live rows
        // use, teas before types so a restored tea keeps its type.
        connection.execSQL(
            """
            UPDATE `tea` SET `finishedAt` = `deletedAt`, `deletedAt` = NULL
            WHERE `deletedAt` IS NOT NULL AND EXISTS (
                SELECT 1 FROM `tea_session` s
                WHERE s.`teaId` = `tea`.`id` AND s.`parentSessionId` IS NULL
                AND s.`deletedAt` IS NULL
            )
            """
        )
        connection.execSQL(
            """
            UPDATE `brewing_vessel` SET `deletedAt` = NULL, `isArchived` = 1
            WHERE `deletedAt` IS NOT NULL AND EXISTS (
                SELECT 1 FROM `tea_session` s
                WHERE s.`vesselId` = `brewing_vessel`.`id` AND s.`deletedAt` IS NULL
            )
            """
        )
        connection.execSQL(
            """
            UPDATE `tea_type` SET `deletedAt` = NULL
            WHERE `deletedAt` IS NOT NULL AND EXISTS (
                SELECT 1 FROM `tea` t
                WHERE t.`teaTypeId` = `tea_type`.`id` AND t.`deletedAt` IS NULL
            )
            """
        )
        // A method goes with its tea or vessel, stamped the same so they restore together
        connection.execSQL(
            """
            UPDATE `brewing_configurations` SET `deleted_at` = COALESCE(
                (SELECT `deletedAt` FROM `tea` WHERE `id` = `tea_id`),
                (SELECT `deletedAt` FROM `brewing_vessel` WHERE `id` = `vessel_id`)
            )
            WHERE `deleted_at` IS NULL
            """
        )
    }
}
