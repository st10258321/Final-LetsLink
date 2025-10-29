package com.example.letslink.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.ViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.security.MessageDigest
import java.util.UUID
import com.example.letslink.local_database.UserDao
import com.example.letslink.model.User
import com.example.letslink.model.UserEvent

@OptIn(ExperimentalCoroutinesApi::class)
class UserViewModelTest {
//testing adapters (SkyFish,2023)
    // Rule to handle LiveData/Coroutines
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // 1. Dependency to Mock
    private val mockDao = mockk<UserDao>(relaxed = true)

    //  model class
    private lateinit var viewModel: UserViewModel

    // 3. Test Constants
    private val testDispatcher = StandardTestDispatcher()
    private val TEST_FIRST_NAME = "Joe"
    private val TEST_PASSWORD = "Password123"
    private val TEST_EMAIL = "alice@test.com"
    private val TEST_DOB = "1990-01-01"
    private val HASHED_TEST_PASSWORD = "010967da0843936611593c6cd87f7422dd81f8f9486c7381f13b690ac77d076d337d12f6b553e18a09a5c8c2ae4b9983f9826bc5989f66085a86475510000000" // SHA-512 hash of "Password123"

    @Before
    fun setup() {
        // Set the main dispatcher for coroutines testing
        Dispatchers.setMain(testDispatcher)

        // Initialize the ViewModel
        viewModel = UserViewModel(mockDao)
    }

    @After
    fun tearDown() {
        // Reset the main dispatcher
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // UTILITY FUNCTION TEST
    // -------------------------------------------------------------------------

    @Test
    fun `hasPass should return hash for given password`() {
        // Assert: Check if the utility function produces the expected SHA-512 hash string
        assertEquals(HASHED_TEST_PASSWORD, viewModel.hasPass(TEST_PASSWORD))
    }

    @Test
    fun `setEmail should update email  and clear error`() {
        // Arrange: Set an initial error state
        viewModel.onEvent(UserEvent.setFirstName("Initial"))
        viewModel.onEvent(UserEvent.createUser) // Should set an error

        // Act
        viewModel.onEvent(UserEvent.setEmail(TEST_EMAIL))

        // Assert: Verify state update
        assertEquals(TEST_EMAIL, viewModel.userState.value.email)
        assertEquals(null, viewModel.userState.value.errorMessage)
    }

    @Test
    fun `setFirstName event should update firstName state and clear error`() {
        // Act
        viewModel.onEvent(UserEvent.setFirstName(TEST_FIRST_NAME))

        // Assert
        assertEquals(TEST_FIRST_NAME, viewModel.userState.value.firstName)
        assertEquals(null, viewModel.userState.value.errorMessage)
    }

    @Test
    fun `deleteUser event should call DAO deleteUser`() = runTest {
        // Arrange
        val userToDelete = User(
            firstName = TEST_FIRST_NAME,
            email = TEST_EMAIL,
            password = TEST_PASSWORD,
            dateOfBirth = TEST_DOB
        )
        coEvery { mockDao.deleteUser(userToDelete) } returns Unit

        // Act
        viewModel.onEvent(UserEvent.deleteUser(userToDelete))
        advanceUntilIdle() // Wait for viewModelScope.launch to finish

        // Assert: Verify the DAO method was called with the correct object
        coVerify(exactly = 1) { mockDao.deleteUser(userToDelete) }
    }

    private fun setupFullValidState() {
        // Helper function to set all fields to a valid state
        viewModel.onEvent(UserEvent.setFirstName(TEST_FIRST_NAME))
        viewModel.onEvent(UserEvent.setPassword(TEST_PASSWORD))
        viewModel.onEvent(UserEvent.setEmail(TEST_EMAIL))
        viewModel.onEvent(UserEvent.setDateOfBirth(TEST_DOB))
    }

    @Test
    fun `createUser should succeed, call DAO, and reset state on success`() = runTest {
        // Arrange
        setupFullValidState()
        // setup mock dao
        coEvery { mockDao.upsertUser(any()) } returns Unit

        // Act
        viewModel.onEvent(UserEvent.createUser)
        advanceUntilIdle() // Wait for DAO upsert to complete

        // Verify DAO was called with the correctly hashed password
        coVerify(exactly = 1) {
            mockDao.upsertUser(match { user ->
                user.firstName == TEST_FIRST_NAME &&
                        user.password == HASHED_TEST_PASSWORD &&
                        user.email == TEST_EMAIL
            })
        }

        // Verify state was reset and success flag is true
        val finalState = viewModel.userState.value
        assertEquals(true, finalState.isSuccess)
        assertEquals("", finalState.firstName) // Check state reset
        assertEquals(null, finalState.errorMessage)
    }

    @Test
    fun `createUser should set error state if a field is blank`() = runTest {
        // Set all fields except password
        viewModel.onEvent(UserEvent.setFirstName(TEST_FIRST_NAME))
        viewModel.onEvent(UserEvent.setEmail(TEST_EMAIL))
        viewModel.onEvent(UserEvent.setDateOfBirth(TEST_DOB))

        // Act
        viewModel.onEvent(UserEvent.createUser)
        advanceUntilIdle()

        // Check error message
        assertEquals("All fields are required.", viewModel.userState.value.errorMessage)

        //  Verify dao was NOT called
        coVerify(exactly = 0) { mockDao.upsertUser(any()) }
    }

    @Test
    fun `createUser should set error state if dateOfBirth is blank`() = runTest {
        // Set all fields except
        viewModel.onEvent(UserEvent.setFirstName(TEST_FIRST_NAME))
        viewModel.onEvent(UserEvent.setPassword(TEST_PASSWORD))
        viewModel.onEvent(UserEvent.setEmail(TEST_EMAIL))

        // Act
        viewModel.onEvent(UserEvent.createUser)
        advanceUntilIdle()

        // Check error message
        assertEquals("Date of birth is required.", viewModel.userState.value.errorMessage)

        // Verify DAO was NOT called
        coVerify(exactly = 0) { mockDao.upsertUser(any()) }
    }


    @Test
    fun `createUser should set error state on DAO exception`() = runTest {
        // Arrange
        setupFullValidState()
        val exceptionMessage = "Database write failed"
        // Mock the DAO to throw an exception
        coEvery { mockDao.upsertUser(any()) } throws Exception(exceptionMessage)

        // Act
        viewModel.onEvent(UserEvent.createUser)
        advanceUntilIdle()

        // Assert
        val finalState = viewModel.userState.value
        assertEquals("Account creation failed: $exceptionMessage", finalState.errorMessage)
        assertEquals(false, finalState.isSuccess)
    }


    @Test
    fun `getUserByEmail should return user from DAO`() = runTest {
        // Arrange
        val expectedUser = User(
            firstName = TEST_FIRST_NAME,
            email = TEST_EMAIL,
            password = TEST_PASSWORD,
            dateOfBirth = TEST_DOB
        )
        // Mock the DAO to return the expected user
        coEvery { mockDao.getUserByEmail(TEST_EMAIL) } returns expectedUser

        // Act
        val result = viewModel.getUserByEmail(TEST_EMAIL)

        // Assert
        assertEquals(expectedUser, result)
        coVerify(exactly = 1) { mockDao.getUserByEmail(TEST_EMAIL) }
    }
}
/***
 *  Android Developers. (n.d.). Application. [online] Available at: https://developer.android.com/reference/android/app/Application.
 *  Ariel (2020). Difference between getContext() and requireContext() when using fragments.
 *  [online] Stack Overflow. Available at: https://stackoverflow.com/questions/60402490/difference-between-getcontext-and-requirecontext-when-using-fragments
 *  Philipp Lackner (2021). The Ultimate Retrofit Crash Course. [online]
 *  YouTube. Available at: https://www.youtube.com/watch?v=t6Sql3WMAnk [Accessed 7 Oct. 2025].
 * Ketul Patel .2015. Session Manager in Android Studio | Android Tutorial’s. [online] Youtu.be. Available at: https://youtu.be/SLkQIlfRWgM?si=-s5dIa3l6nKtnCVD [Accessed 2 May 2025].
 *
 * Lindevs. 2020. Display Date Picker Dialog in Android, 31 July 2020. [Online]. Available at: https://lindevs.com/display-date-picker-dialog-in-android/ [Accessed 30 April 2025].
 *
 * SkyFish. “Functional Unit Testing Kotlin View Models and Use Cases.” YouTube, 25 Sept. 2023, www.youtube.com/watch?v=4aa0cyIgG8s. Accessed 5 Oct. 2025.
SSOJet (2022). SHA-512 in Kotlin | SSOJet. [online] SSOJet | Enterprise-Grade SSO in Minutes,Not Days or Months.
Available at: https://ssojet.com/hashing/sha-512-in-kotlin/ [Accessed 7 Oct. 2025].
 * Ariel (2020). Difference between getContext() and requireContext() when using fragments. [online]
 * Stack Overflow. Available at: https://stackoverflow.com/questions/60402490/difference-between-getcontext-and-requirecontext-when-using-fragments.
 *
 * Lackner, P. (2025a). Page Restricted. [online] Youtu.be. Available at: https://youtu.be/hrJZIF7qSSw?si=RigZW4gEa1uvjlZY [Accessed 7 Oct. 2025]
 * ackner, P. (2025b). The FULL Beginner Guide for Room in Android | Local Database Tutorial for Android. [online] Youtu.be. Available at: https://youtu.be/bOd3wO0uFr8?si=j0PC3HmfUQmrYKjB [Accessed 7 Oct. 2025].
 *
 * */