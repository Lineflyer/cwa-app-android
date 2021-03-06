package de.rki.coronawarnapp.bugreporting.censors

import de.rki.coronawarnapp.bugreporting.debuglog.LogLine
import de.rki.coronawarnapp.contactdiary.model.ContactDiaryPersonEncounter
import de.rki.coronawarnapp.contactdiary.storage.repo.ContactDiaryRepository
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testhelpers.BaseTest
import kotlin.concurrent.thread

class DiaryEncounterCensorTest : BaseTest() {

    @MockK lateinit var diaryRepo: ContactDiaryRepository

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }

    private fun createInstance(scope: CoroutineScope) = DiaryEncounterCensor(
        debugScope = scope,
        diary = diaryRepo
    )

    private fun mockEncounter(
        _id: Long,
        _circumstances: String
    ) = mockk<ContactDiaryPersonEncounter>().apply {
        every { id } returns _id
        every { circumstances } returns _circumstances
    }

    @Test
    fun `censoring replaces the logline message`() = runBlockingTest {
        every { diaryRepo.personEncounters } returns flowOf(
            listOf(
                mockEncounter(1, _circumstances = ""),
                mockEncounter(2, _circumstances = "A rainy day"),
                mockEncounter(3, "Spilled coffee on each others laptops")
            )
        )

        val instance = createInstance(this)
        val censorMe = LogLine(
            timestamp = 1,
            priority = 3,
            message =
                """
                On A rainy day,
                two persons Spilled coffee on each others laptops,
                everyone disliked that.
                """.trimIndent(),
            tag = "I'm a tag",
            throwable = null
        )

        instance.checkLog(censorMe) shouldBe censorMe.copy(
            message =
                """
                    On Encounter#2/Circumstances,
                    two persons Encounter#3/Circumstances,
                    everyone disliked that.
                """.trimIndent()
        )
    }

    @Test
    fun `censoring returns null if all circumstances are blank`() = runBlockingTest {
        every { diaryRepo.personEncounters } returns flowOf(listOf(mockEncounter(1, _circumstances = "")))
        val instance = createInstance(this)
        val notCensored = LogLine(
            timestamp = 1,
            priority = 3,
            message = "That was strange.",
            tag = "I'm a tag",
            throwable = null
        )
        instance.checkLog(notCensored) shouldBe null
    }

    @Test
    fun `censoring returns null if there are no locations no match`() = runBlockingTest {
        every { diaryRepo.personEncounters } returns flowOf(emptyList())

        val instance = createInstance(this)

        val notCensored = LogLine(
            timestamp = 1,
            priority = 3,
            message = "Nothing ever happens.",
            tag = "I'm a tag",
            throwable = null
        )
        instance.checkLog(notCensored) shouldBe null
    }

    @Test
    fun `censoring returns null if the message did not change`() = runBlockingTest {
        every { diaryRepo.personEncounters } returns flowOf(
            listOf(
                mockEncounter(1, _circumstances = "March weather"),
                mockEncounter(2, _circumstances = "Rainy, cold"),
            )
        )

        val instance = createInstance(this)
        val notCensored = LogLine(
            timestamp = 1,
            priority = 3,
            message = "I like turtles",
            tag = "I'm a tag",
            throwable = null
        )
        instance.checkLog(notCensored) shouldBe null
    }

    // EXPOSUREAPP-5670 / EXPOSUREAPP-5691
    @Test
    fun `replacement doesn't cause recursion`() {
        every { diaryRepo.personEncounters } returns flowOf(
            listOf(
                mockEncounter(1, _circumstances = "March weather"),
                mockEncounter(2, _circumstances = "Rainy, cold"),
            )
        )

        val logLine = LogLine(
            timestamp = 1,
            priority = 3,
            message = "Lorem ipsum",
            tag = "I'm a tag",
            throwable = null
        )

        var isFinished = false

        thread {
            Thread.sleep(500)
            if (isFinished) return@thread
            Runtime.getRuntime().exit(1)
        }

        runBlocking {
            val instance = createInstance(this)

            val processedLine = try {
                instance.checkLog(logLine)
            } finally {
                isFinished = true
            }
            processedLine shouldBe null
        }
    }
}
