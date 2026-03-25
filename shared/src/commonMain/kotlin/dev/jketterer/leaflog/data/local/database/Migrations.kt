package dev.jketterer.leaflog.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

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
