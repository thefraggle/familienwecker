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
    @Query("SELECT * FROM members ORDER BY sequenceOrder ASC")
    fun getAllMembers(): Flow<List<FamilyMemberEntity>>

    @Upsert
    suspend fun upsertMembers(members: List<FamilyMemberEntity>)

    @Query("DELETE FROM members WHERE id = :id")
    suspend fun deleteMember(id: String)

    @Query("DELETE FROM members")
    suspend fun clearAll()
}

@Database(entities = [FamilyMemberEntity::class], version = 1, exportSchema = false)
@ConstructedBy(FamWakeDatabaseConstructor::class)
abstract class FamWakeDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
}
