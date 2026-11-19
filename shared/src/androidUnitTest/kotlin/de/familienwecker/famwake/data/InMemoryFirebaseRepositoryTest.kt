package de.familienwecker.famwake.data

import de.familienwecker.famwake.model.FamilyMember
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryFirebaseRepositoryTest {

    @Test
    fun createAndJoinFamily_returnsMatchingFamilyId() = runBlocking {
        val repo = InMemoryFirebaseRepository()
        val (familyId, joinCode) = repo.createFamily("Testfamilie", "u1").getOrThrow()

        val joined = repo.joinFamilyByCode(joinCode).getOrThrow()
        assertEquals(familyId, joined.first)

        val missing = repo.joinFamilyByCode("NOPE99")
        val ex = missing.exceptionOrNull() as AppErrorException
        assertEquals(AppError.FamilyNotFound, ex.appError)
    }

    @Test
    fun memberLifecycle_claimAndPauseUpdateFlow() = runBlocking {
        val repo = InMemoryFirebaseRepository()
        val (familyId, _) = repo.createFamily("Test", "u1").getOrThrow()
        val member = FamilyMember(
            id = "m1",
            name = "Papa",
            earliestWakeUp = LocalTime(6, 0),
            latestWakeUp = LocalTime(7, 0),
            bathroomDurationMinutes = 15,
            wantsBreakfast = false,
            leaveHomeTime = LocalTime(7, 30)
        )
        repo.addOrUpdateMember(familyId, member)

        assertTrue(repo.claimMember(familyId, "m1", "u1", "Daniel", "dev1"))
        repo.updateMemberPauseState(familyId, "m1", true)

        val members = repo.getFamilyMembersFlow(familyId).first()
        assertEquals(1, members.size)
        assertEquals("u1", members.first().claimedByUserId)
        assertTrue(members.first().isPaused)
    }
}
