package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ArtistBioDao
import com.example.data.local.dao.GenerationHistoryDao
import com.example.data.local.dao.MoodBoardConceptDao
import com.example.data.local.dao.MoodBoardReferenceDao
import com.example.data.local.dao.PortfolioProjectDao
import com.example.data.local.dao.ProjectDraftDao
import com.example.data.local.dao.ServiceCatalogReferenceDao
import com.example.data.local.dao.SessionHistoryDao
import com.example.data.local.dao.StudioInfoDao
import com.example.data.local.dao.StudioSessionProjectDao
import com.example.data.local.entity.ArtistBioEntity
import com.example.data.local.entity.GenerationHistoryEntity
import com.example.data.local.entity.MoodBoardConceptEntity
import com.example.data.local.entity.MoodBoardReferenceEntity
import com.example.data.local.entity.PortfolioProjectEntity
import com.example.data.local.entity.ProjectDraftEntity
import com.example.data.local.entity.ServiceCatalogReferenceEntity
import com.example.data.local.entity.SessionHistoryEntity
import com.example.data.local.entity.StudioInfoEntity
import com.example.data.local.entity.StudioProjectStatusConverters
import com.example.data.local.entity.StudioSessionProjectEntity

/**
 * Single-instance Room Database for STAR ARTIST STUDIO local persistence:
 * 1. Offline cached essential studio information and contact details
 * 2. Generation history of AI-designed photoshoot concepts and 4K storyboards
 * 3. Artist project drafts (concepts, moodboards, lighting setup plans, gear notes)
 * 4. Local session history (AI sessions, storyboard generation, drafts)
 * 5. Artist professional bio and specialty local cache
 * 6. Curated and custom photography & video portfolio projects with media thumbnails
 * 7. Artist-captured reference shots attached to Service Catalog packages
 * 8. Studio session workflow tracker ('Planning', 'In Production', 'Post-Processing', 'Completed')
 */
@Database(
    entities = [
        GenerationHistoryEntity::class,
        StudioInfoEntity::class,
        ProjectDraftEntity::class,
        SessionHistoryEntity::class,
        ArtistBioEntity::class,
        PortfolioProjectEntity::class,
        ServiceCatalogReferenceEntity::class,
        StudioSessionProjectEntity::class,
        MoodBoardConceptEntity::class,
        MoodBoardReferenceEntity::class
    ],
    version = 13,
    exportSchema = false
)
@TypeConverters(StudioProjectStatusConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun generationHistoryDao(): GenerationHistoryDao
    abstract fun studioInfoDao(): StudioInfoDao
    abstract fun projectDraftDao(): ProjectDraftDao
    abstract fun sessionHistoryDao(): SessionHistoryDao
    abstract fun artistBioDao(): ArtistBioDao
    abstract fun portfolioProjectDao(): PortfolioProjectDao
    abstract fun serviceCatalogReferenceDao(): ServiceCatalogReferenceDao
    abstract fun studioSessionProjectDao(): StudioSessionProjectDao
    abstract fun moodBoardConceptDao(): MoodBoardConceptDao
    abstract fun moodBoardReferenceDao(): MoodBoardReferenceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `service_catalog_references` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `packageId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `imageDataUrl` TEXT NOT NULL,
                        `aspectGuide` TEXT NOT NULL,
                        `resolutionTag` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_service_catalog_references_packageId` ON `service_catalog_references` (`packageId`)"
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `studio_session_projects` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionCode` TEXT NOT NULL,
                        `projectTitle` TEXT NOT NULL,
                        `clientName` TEXT NOT NULL,
                        `serviceCategory` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `scheduledDate` TEXT NOT NULL,
                        `studioStage` TEXT NOT NULL,
                        `leadDirector` TEXT NOT NULL,
                        `progressPercent` INTEGER NOT NULL,
                        `deliverableCount` INTEGER NOT NULL,
                        `budgetInr` INTEGER NOT NULL,
                        `productionNotes` TEXT NOT NULL,
                        `startedAt` INTEGER,
                        `completedAt` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_studio_session_projects_status` ON `studio_session_projects` (`status`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_studio_session_projects_serviceCategory` ON `studio_session_projects` (`serviceCategory`)"
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mood_board_concepts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `boardTitle` TEXT NOT NULL,
                        `projectType` TEXT NOT NULL,
                        `prompt` TEXT NOT NULL,
                        `visualStyle` TEXT NOT NULL,
                        `lightingSetup` TEXT NOT NULL,
                        `aspectRatioLabel` TEXT NOT NULL,
                        `modelUsed` TEXT NOT NULL,
                        `localImagePath` TEXT NOT NULL,
                        `colorPaletteTags` TEXT NOT NULL,
                        `linkedDraftId` INTEGER,
                        `isFavorite` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_mood_board_concepts_projectType` ON `mood_board_concepts` (`projectType`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_mood_board_concepts_isFavorite` ON `mood_board_concepts` (`isFavorite`)"
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `mood_board_concepts` ADD COLUMN `sessionProjectId` INTEGER")
                db.execSQL("ALTER TABLE `mood_board_concepts` ADD COLUMN `boardSectionTag` TEXT NOT NULL DEFAULT 'Concept Art'")
                db.execSQL("ALTER TABLE `mood_board_concepts` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_mood_board_concepts_sessionProjectId` ON `mood_board_concepts` (`sessionProjectId`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mood_board_references` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionProjectId` INTEGER,
                        `title` TEXT NOT NULL,
                        `sectionTag` TEXT NOT NULL,
                        `sourceType` TEXT NOT NULL,
                        `imagePathOrUrl` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `colorSwatches` TEXT NOT NULL,
                        `isPinned` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_mood_board_references_sessionProjectId` ON `mood_board_references` (`sessionProjectId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_mood_board_references_sectionTag` ON `mood_board_references` (`sectionTag`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_mood_board_references_isPinned` ON `mood_board_references` (`isPinned`)"
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `studio_session_projects` ADD COLUMN `trackedProductionMillis` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `studio_session_projects` ADD COLUMN `activeTimerStartedAt` INTEGER"
                )
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `studio_session_projects` ADD COLUMN `isArchived` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `studio_session_projects` ADD COLUMN `archivedAt` INTEGER"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_studio_session_projects_isArchived` ON `studio_session_projects` (`isArchived`)"
                )
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `studio_session_projects` ADD COLUMN `inProductionTaskNotes` TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `studio_session_projects` ADD COLUMN `deadlineDate` TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "star_artist_studio_database"
                )
                    .addMigrations(
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13
                    )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
