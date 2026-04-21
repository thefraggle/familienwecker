package de.familienwecker.famwake.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "members")
data class FamilyMemberEntity(
    @PrimaryKey val id: String,
    val name: String,
    val earliestWakeUp: String, // LocalTime as String
    val latestWakeUp: String,   // LocalTime as String
    val bathroomDurationMinutes: Long,
    val wantsBreakfast: Boolean,
    val leaveHomeTime: String?, // LocalTime as String
    val isPaused: Boolean,
    val isAwakeToday: Boolean,
    val lastResetDate: String,
    val claimedByUserId: String?,
    val claimedByUserName: String?,
    val sequenceOrder: Int,
    val createdAt: Long?,
    val lastUpdatedAt: Long?,
    val deviceAlarmEnabled: Boolean?,
    val dayProfilesJson: String? // JSON serialized Map<Int, DayProfile>
)

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY sequenceOrder ASC, createdAt ASC")
    fun getAllMembers(): Flow<List<FamilyMemberEntity>>

    @Upsert
    suspend fun upsertMembers(members: List<FamilyMemberEntity>)

    @Query("DELETE FROM members WHERE id = :id")
    suspend fun deleteMember(id: String)

    @Query("DELETE FROM members WHERE id NOT IN (:ids)")
    suspend fun deleteNotInIds(ids: List<String>)

    @Query("DELETE FROM members")
    suspend fun clearAll()
}

// Migration strategy: fallbackToDestructiveMigration()
// Rationale: The app has no data worth preserving in Room – all canonical data lives in
// Firestore. Room is used as a pure local cache. On schema changes, a clean wipe and
// re-sync from Firestore is the correct and safe strategy.
// To apply: in FamWakeDatabaseConstructor (androidMain) or the builder call, add
//   .fallbackToDestructiveMigration()
// When bumping this version, do NOT add manual migrations – just increment and rely on
// fallbackToDestructiveMigration.
@Database(entities = [FamilyMemberEntity::class], version = 1, exportSchema = false)
@ConstructedBy(FamWakeDatabaseConstructor::class)
abstract class FamWakeDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
}
