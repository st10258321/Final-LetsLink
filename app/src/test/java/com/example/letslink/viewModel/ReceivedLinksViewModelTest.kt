package com.example.letslink.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.letslink.API_related.GroupRepo
import com.example.letslink.SessionManager
import com.example.letslink.model.GroupResponse
import com.example.letslink.model.Invites
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import org.junit.Assert.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ReceivedLinksViewModelTest {

//testing adapters (SkyFish,2023)

    // Rule to make LiveData work in tests
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // 1. Dependencies to Mock
    private val mockGroupRepo = mockk<GroupRepo>(relaxed = true)
    private val mockSessionManager = mockk<SessionManager>(relaxed = true)

    // 2. Class Under Test
    private lateinit var viewModel: ReceivedLinksViewModel

    // 3. Test Constants
    private val testDispatcher = StandardTestDispatcher()
    private val TEST_USER_UUID = UUID.randomUUID()
    private val TEST_USER_ID_STRING = TEST_USER_UUID.toString()
    private val TEST_GROUP_ID = "group_123"
    private val TEST_INVITE = Invites(
        groupId = TEST_GROUP_ID,
        groupName = "Test Group",
        description = "A description",
        inviteLink = "link_a"
    )

    @Before
    fun setup() {
        // Set the main dispatcher for coroutines testing
        Dispatchers.setMain(testDispatcher)

        // Default: Mock the session manager to return a valid ID
        every { mockSessionManager.getUserId() } returns TEST_USER_UUID

        // Default: Mock the invites flow to return an empty list initially
        coEvery { mockGroupRepo.getReceivedInvites(any()) } returns flowOf(emptyList())

        // Initialize the ViewModel (this triggers the init block)
        viewModel = ReceivedLinksViewModel(mockGroupRepo, mockSessionManager)
    }

    @After
    fun tearDown() {
        // Reset the main dispatcher
        Dispatchers.resetMain()
    }


    @Test
    fun `init block should fetch received invites immediately`() = runTest {
        // Assert: The repository must have been called exactly once with the correct user ID
        // after the ViewModel was instantiated in setup()
        verify(exactly = 1) { mockSessionManager.getUserId() }
        coVerify(exactly = 1) { mockGroupRepo.getReceivedInvites(TEST_USER_ID_STRING) }
    }

    @Test
    fun `fetchReceivedInvites should update LiveData when repo emits data`() = runTest {
        // Arrange
        val expectedInvites = listOf(TEST_INVITE)

         coEvery { mockGroupRepo.getReceivedInvites(TEST_USER_ID_STRING) } returns flowOf(expectedInvites)

        // Act
        viewModel.fetchReceivedInvites(TEST_USER_ID_STRING)
        advanceUntilIdle() // Wait for the coroutine to finish collecting the flow

        // Check if the LiveData holds the  list
        assertEquals(expectedInvites, viewModel.receivedInvites.value)
    }

    @Test
    fun `fetchReceivedInvites should be called with null ID if session manager returns null`() = runTest {
        // Arrange
        every { mockSessionManager.getUserId() } returns null
        // Re-initialize to ensure init() uses the null ID
        viewModel = ReceivedLinksViewModel(mockGroupRepo, mockSessionManager)

        // call fetch with a null string (since getUserId.toString will be "null")
        viewModel.fetchReceivedInvites("null")
        advanceUntilIdle()

        // Assert: Verify the repo was called with the string "null"
        coVerify(atLeast = 1) { mockGroupRepo.getReceivedInvites("null") }
    }



    @Test
    fun `joinGroup should call repo join and then refresh invites`() = runTest {
        // Setup a basic return for the join call
        val successResponse = mockk<GroupResponse>()
        coEvery { mockGroupRepo.joinGroup(any(), any()) } returns successResponse

        // Mock the refresh call to show a new list
        val refreshedInvites = emptyList<Invites>()

         coEvery { mockGroupRepo.getReceivedInvites(TEST_USER_ID_STRING) } returns flowOf(refreshedInvites)

        // Act
        viewModel.joinGroup(TEST_GROUP_ID, TEST_USER_UUID)
        advanceUntilIdle() //

        coVerify(exactly = 1) { mockGroupRepo.joinGroup(TEST_GROUP_ID, TEST_USER_UUID) }


        coVerify(ordering = io.mockk.Ordering.SEQUENCE) {
            mockGroupRepo.getReceivedInvites(TEST_USER_ID_STRING)
            mockGroupRepo.joinGroup(TEST_GROUP_ID, TEST_USER_UUID)
            mockGroupRepo.getReceivedInvites(TEST_USER_ID_STRING)
        }

        // Assert 3: Verify the LiveData was updated with the new (refreshed) value
        assertEquals(refreshedInvites, viewModel.receivedInvites.value)
    }

    @Test
    fun `joinGroup should handle null UUID without error`() = runTest {
        // Arrange
        val nullId: UUID? = null
        val nullIdString = "null"

        // Mock the refresh call with the "null" string
        coEvery { mockGroupRepo.getReceivedInvites(nullIdString) } returns flowOf(emptyList())

        // Act
        viewModel.joinGroup(TEST_GROUP_ID, nullId)
        advanceUntilIdle()

        // Assert 1: The repository should still attempt the join call
        coVerify(exactly = 1) { mockGroupRepo.joinGroup(TEST_GROUP_ID, nullId) }

        // Assert 2: The refresh should still be called with the resulting string "null"
        coVerify(atLeast = 1) { mockGroupRepo.getReceivedInvites(nullIdString) }
    }
}