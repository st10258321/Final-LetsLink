package com.example.letslink.viewmodels

import com.example.api_test.GroupState
import com.example.letslink.API_related.GroupRepo
import com.example.letslink.SessionManager
import com.example.letslink.model.Group
import com.example.letslink.model.GroupResponse
import com.example.letslink.local_database.GroupEvent
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class GroupViewModelTest {

    //testing adapters (SkyFish,2023)
    // 1. Dependencies to Mock
    private val mockRepository = mockk<GroupRepo>()
    private val mockSessionManager = mockk<SessionManager>()

    // 2. Class Under Test
    private lateinit var viewModel: GroupViewModel

    // 3. Test Constants
    private val testDispatcher = StandardTestDispatcher()
    private val TEST_USER_UUID = UUID.randomUUID()
    private val TEST_GROUP_ID = "group_xyz123"
    private val TEST_GROUP_NAME = "Weekly Meetup"
    private val TEST_DESCRIPTION = "Planning sessions"
    private val TEST_INVITE_LINK = "http://invite.link/456"
    private val TEST_RECIPIENT_USERNAME = "Bob"
    private val TEST_RECIPIENT_ID = "user_id_A"
    private val TEST_GROUP = Group(
        groupId = UUID.randomUUID(),
        userId = TEST_USER_UUID,
        groupName = TEST_GROUP_NAME,
        description = TEST_DESCRIPTION
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // assume user is logged in
        every { mockSessionManager.getUserId() } returns TEST_USER_UUID
        coEvery { mockRepository.getGroupsByUserId(any()) } returns flowOf(emptyList())
        coEvery { mockRepository.getGroupsByUserId(TEST_USER_UUID) } returns flowOf(emptyList()) // <--- FIX 1 +

        // Initialize the ViewModel
        viewModel = GroupViewModel(mockRepository, mockSessionManager)
    }

    @After
    fun tearDown() {
        // Reset the main dispatcher
        Dispatchers.resetMain()
    }

    @Test
    fun `joinGroup should succeed and update state on valid response`() = runTest {
        // Arrange
        val successResponse = mockk<GroupResponse>()
        coEvery { mockRepository.joinGroup(any(), any()) } returns successResponse

        // Act
        viewModel.joinGroupFromInvite(TEST_GROUP_ID)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { mockRepository.joinGroup(TEST_GROUP_ID, TEST_USER_UUID) }
        val finalState = viewModel.noteState.value
        assertEquals(false, finalState.isLoading)
        assertEquals(true, finalState.isSuccess)
        assertEquals(null, finalState.errorMessage)
    }

    @Test
    fun `joinGroup should set error state if user session is invalid`() = runTest {
        every { mockSessionManager.getUserId() } returns null
        viewModel = GroupViewModel(mockk(), mockSessionManager)

        // Act
        viewModel.joinGroupFromButton(TEST_GROUP_ID)
        advanceUntilIdle()

        // Assert
        val finalState = viewModel.noteState.value
        assertEquals("Error: Cannot join group. User session is invalid.", finalState.errorMessage)
        coVerify(exactly = 0) { mockRepository.joinGroup(any(), any()) }
    }

    @Test
    fun `joinGroup should set error state if repository returns null`() = runTest {
        // Arrange
        coEvery { mockRepository.joinGroup(any(), any()) } returns null

        // Act
        viewModel.joinGroupFromInvite(TEST_GROUP_ID)
        advanceUntilIdle()

        // Assert
        val finalState = viewModel.noteState.value
        assertEquals("Failed to join group. Server response was null.", finalState.errorMessage)
        assertEquals(false, finalState.isSuccess)
    }

    @Test
    fun `createNote should call repo and reset state on successful sync`() = runTest {
        // Arrange
        val successResponse = mockk<GroupResponse>()
        every { successResponse.inviteLink } returns TEST_INVITE_LINK

        // Set inputs
        viewModel.onEvent(GroupEvent.setTitle(TEST_GROUP_NAME))
        viewModel.onEvent(GroupEvent.setDecription(TEST_DESCRIPTION))
        coEvery { mockRepository.createAndSyncGroup(any()) } returns successResponse

        // Act
        viewModel.onEvent(GroupEvent.createNote)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { mockRepository.createAndSyncGroup(any()) }
        val finalState = viewModel.noteState.value
        assertEquals(true, finalState.isSuccess)
        assertEquals(false, finalState.isLoading)
        assertEquals(TEST_INVITE_LINK, finalState.inviteLink)

        // Verify state was reset
        assertEquals("", finalState.groupName)
    }

    @Test
    fun `createNote should set error on blank input and skip repository call`() = runTest {
        // Arrange
        viewModel.onEvent(GroupEvent.setDecription(TEST_DESCRIPTION))

        // Act
        viewModel.onEvent(GroupEvent.createNote)
        advanceUntilIdle()

        // Assert
        assertEquals("Error regarding entering notes", viewModel.noteState.value.errorMessage)
        coVerify(exactly = 0) { mockRepository.createAndSyncGroup(any()) }
    }

    @Test
    fun `createNote should set error if API sync fails but group is saved locally`() = runTest {
        // Arrange
        viewModel.onEvent(GroupEvent.setTitle(TEST_GROUP_NAME))
        viewModel.onEvent(GroupEvent.setDecription(TEST_DESCRIPTION))
        coEvery { mockRepository.createAndSyncGroup(any()) } returns null

        // Act
        viewModel.onEvent(GroupEvent.createNote)
        advanceUntilIdle()

        // Assert
        val finalState = viewModel.noteState.value
        assertEquals("Group saved locally but failed to generate invite link.", finalState.errorMessage)
        assertEquals(false, finalState.isSuccess)
    }

    @Test
    fun `sendPersonalizedInvite should succeed if recipient ID is found locally`() = runTest {
        // Arrange
        coEvery { mockRepository.getRecipientIdFromRoom(TEST_RECIPIENT_USERNAME) } returns TEST_RECIPIENT_ID
        coEvery { mockRepository.assignInvite(any(), any(), any(), any()) } returns Unit

        // Act
        viewModel.sendPersonalizedInvite(TEST_RECIPIENT_USERNAME, TEST_GROUP)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { mockRepository.getRecipientIdFromRoom(TEST_RECIPIENT_USERNAME) }
        coVerify(exactly = 1) {
            mockRepository.assignInvite(
                recipientId = TEST_RECIPIENT_ID,
                groupId = TEST_GROUP.groupId.toString(),
                groupName = TEST_GROUP_NAME,
                description = TEST_DESCRIPTION
            )
        }
        assertEquals(null, viewModel.noteState.value.errorMessage)
    }

    @Test
    fun `sendPersonalizedInvite should show error that ID is not found`() = runTest {
        // Arrange
        coEvery { mockRepository.getRecipientIdFromRoom(any()) } returns null

        // Act
        viewModel.sendPersonalizedInvite(TEST_RECIPIENT_USERNAME, TEST_GROUP)
        advanceUntilIdle()

        // Assert
        assertEquals("User $TEST_RECIPIENT_USERNAME not found.", viewModel.noteState.value.errorMessage)
        coVerify(exactly = 0) { mockRepository.assignInvite(any(), any(), any(), any()) }
    }


    @Test
    fun `getRecipientId must return local ID and not check firebase`() = runTest {
        // Arrange
        coEvery { mockRepository.getRecipientIdFromRoom(any()) } returns TEST_RECIPIENT_ID
        coEvery { mockRepository.getRecipientIdFromFirebase(any()) } returns "Should not be called"

        // Act
        val result = viewModel.getRecipientId(TEST_RECIPIENT_USERNAME)

        // Assert
        assertEquals(TEST_RECIPIENT_ID, result)
        coVerify(exactly = 1) { mockRepository.getRecipientIdFromRoom(TEST_RECIPIENT_USERNAME) }
        coVerify(exactly = 0) { mockRepository.getRecipientIdFromFirebase(any()) }
    }

    @Test
    fun `check firebase if local ID is null`() = runTest {
        // Arrange
        coEvery { mockRepository.getRecipientIdFromRoom(any()) } returns null
        coEvery { mockRepository.getRecipientIdFromFirebase(any()) } returns TEST_RECIPIENT_ID

        // Act
        val result = viewModel.getRecipientId(TEST_RECIPIENT_USERNAME)

        // Assert
        assertEquals(TEST_RECIPIENT_ID, result)
        coVerify(exactly = 1) { mockRepository.getRecipientIdFromRoom(TEST_RECIPIENT_USERNAME) }
        coVerify(exactly = 1) { mockRepository.getRecipientIdFromFirebase(TEST_RECIPIENT_USERNAME) }
    }
}