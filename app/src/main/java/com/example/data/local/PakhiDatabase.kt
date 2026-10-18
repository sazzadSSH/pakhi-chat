package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AdminLogDao
import com.example.data.local.dao.AnnouncementDao
import com.example.data.local.dao.AppConfigDao
import com.example.data.local.dao.BannerDao
import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.AdminLogEntity
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.AppConfigEntity
import com.example.data.local.entity.BannerEntity
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        AnnouncementEntity::class,
        BannerEntity::class,
        AdminLogEntity::class,
        AppConfigEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PakhiDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun announcementDao(): AnnouncementDao
    abstract fun bannerDao(): BannerDao
    abstract fun adminLogDao(): AdminLogDao
    abstract fun appConfigDao(): AppConfigDao

    companion object {
        @Volatile
        private var INSTANCE: PakhiDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): PakhiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PakhiDatabase::class.java,
                    "pakhi_database.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        private suspend fun populateInitialData(db: PakhiDatabase) {
            val now = System.currentTimeMillis()

            // 3. Announcements
            db.announcementDao().insertAnnouncement(
                AnnouncementEntity(
                    id = "ann_1",
                    title = "পাখিতে স্বাগতম!",
                    body = "আধুনিক ও নিরাপদ রিয়েল-টাইম মেসেজিংয়ে আপনাকে স্বাগতম। আপনার অনন্য Pakhi ID বন্ধুদের সাথে শেয়ার করে চ্যাট শুরু করুন।",
                    pinned = true,
                    published = true,
                    createdAt = now
                )
            )
            db.announcementDao().insertAnnouncement(
                AnnouncementEntity(
                    id = "ann_2",
                    title = "নিরাপত্তা ও গোপনীয়তা নির্দেশিকা",
                    body = "পাখিতে আপনার ব্যক্তিগত মেসেজ সুরক্ষিত। আপনার Pakhi ID ছাড়া কেউ আপনার সাথে যোগাযোগ করতে পারবে না।",
                    pinned = false,
                    published = true,
                    createdAt = now - 3600000L
                )
            )

            // 4. Banners
            db.bannerDao().insertBanner(
                BannerEntity(
                    id = "ban_1",
                    title = "দ্রুত ও নিরাপদ যোগাযোগ",
                    description = "কথা উড়ে যায়, মন ছুঁয়ে যায়",
                    orderIndex = 1,
                    active = true,
                    createdAt = now
                )
            )
            db.bannerDao().insertBanner(
                BannerEntity(
                    id = "ban_2",
                    title = "অনন্য Pakhi ID",
                    description = "নাম্বার বা গোপন তথ্য শেয়ার ছাড়াই সহজে যোগাযোগ",
                    orderIndex = 2,
                    active = true,
                    createdAt = now - 3600000L
                )
            )

            // 5. App Config
            db.appConfigDao().insertOrUpdate(
                AppConfigEntity(
                    id = "main",
                    appName = "পাখি",
                    tagline = "কথা উড়ে যায়, মন ছুঁয়ে যায়",
                    maintenanceEnabled = false,
                    maintenanceMessage = "পাখির সার্ভার রক্ষণাবেক্ষণ চলছে। শীঘ্রই ফিরে আসছি।"
                )
            )

            // 6. Admin Log initial
            db.adminLogDao().insertLog(
                AdminLogEntity(
                    id = "log_init",
                    adminUid = "pakhi",
                    action = "SYSTEM_INITIALIZE",
                    targetType = "SYSTEM",
                    targetId = "main",
                    meta = "System bootstrap completed successfully",
                    timestamp = now
                )
            )
        }
    }
}
