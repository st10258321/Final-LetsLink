package com.example.letslink.viewmodels

import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.letslink.local_database.UserDao
import com.example.letslink.API_related.UUIDConverter
import com.example.letslink.R
import com.example.letslink.SessionManager
import com.example.letslink.model.LoginEvent
import com.example.letslink.model.LoginState
import com.example.letslink.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.MessageDigest
import com.google.firebase.database.FirebaseDatabase

class LoginViewModel(private val dao: UserDao, private val sessionManager: SessionManager, private val context: Context) : ViewModel() {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var uuidConverter : UUIDConverter

    private val _loginState = MutableStateFlow(LoginState())
    val loginState = _loginState.asStateFlow()
    var _loggedInUser: User? = null


    ////hash passwords (SSOJet ,2022)
    fun hasPass(hashPassword : String): String
    {
        val bytes = hashPassword.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-512")
        val digest = md.digest(bytes)
        return digest.fold("") { str, byte -> str + "%02x".format(byte) }
    }
    fun verifyPassword(providedPassword: String, storedHash: String): Boolean {
        val providedPasswordHash = hasPass(providedPassword)
        return providedPasswordHash == storedHash
    }
    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.checkEmail -> {
                _loginState.update { it.copy(email = event.email, errorMessage = null) }
            }
            is LoginEvent.checkPassword -> {
                _loginState.update { it.copy(password = event.password, errorMessage = null) }
            }
            LoginEvent.Login -> {
                attemptLogin()
            }

            is LoginEvent.GoogleLogin -> siginInWithGoogle(event.idToken)
            is LoginEvent.LoginFailed -> _loginState.update{it.copy(errorMessage = event.message)}
        }
    }
    private fun siginInWithGoogle(idToken: String) {
        _loginState.update { it.copy(isLoading = true, errorMessage = null) }
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result.user
                    if (firebaseUser?.email != null && firebaseUser.displayName != null) {
                        val userId = firebaseUser.uid
                        val email = firebaseUser.email!!
                        val name = firebaseUser.displayName!!

                        val newUser = User(
                            userId = userId,
                            firstName = name,
                            email = email,
                            password = "" // No password for Google users
                        )

                        viewModelScope.launch {
                            syncUserToLocalDatabase(newUser)
                            sessionManager.saveUserSession(userId = userId, email = email, name = name)

                            _loginState.update {
                                it.copy(
                                    userId = userId,
                                    name = name,
                                    email = email,
                                    isLoading = false,
                                    isSuccess = true,
                                    errorMessage = null
                                )
                            }
                            Log.d("LoginViewModel", "Google Sign-In successful and user synced.")
                        }
                    } else {
                        _loginState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = context.getString(R.string.lvm9_failed_to_retrieve_google_user_details)
                            )
                        }
                        Log.d("LoginViewModel", "Google sign in successful, but user details are missing.")
                    }
                } else {
                    _loginState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = task.exception?.message ?: context.getString(R.string.lvm9_unknown_google_sign_in_error)
                        )
                    }
                    Log.d("LoginViewModel", "Google sign in failed with exception: ${task.exception?.message}")
                }
            }
    }
    private fun attemptLogin() {
        val state = loginState.value
        Log.d("LoginViewModel", "Attempting login for user: ${state.email}")

        if (state.email.isBlank()) {
            _loginState.update { it.copy(errorMessage = context.getString(R.string.lvm9_email_is_required)) }
            Log.d("LoginViewModel", "Email is blank.")
            return
        }

        if (state.password.isBlank()) {
            _loginState.update { it.copy(errorMessage = context.getString(R.string.lvm9_password_is_required)) }
            Log.d("LoginViewModel", "Password is blank.")
            return
        }

        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // First try Firebase Authentication
                Log.d("LoginViewModel", "Attempting Firebase authentication...")

                firebaseAuth.signInWithEmailAndPassword(state.email, state.password)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            // Firebase authentication successful
                            val firebaseUser = authTask.result.user
                            Log.d("LoginViewModel", "Firebase authentication successful for: ${firebaseUser?.email}")

                            // Now get user profile from Realtime Database
                            val userId = firebaseUser?.uid
                            if (userId != null) {
                                getUserFromFirebaseDatabase(userId, state.email)
                            sessionManager.saveUserSession(userId,state.email,state.name)
                            } else {
                                _loginState.update {
                                    it.copy(
                                        isLoading = false,
                                        errorMessage = context.getString(R.string.lvm9_user_id_not_found)
                                    )
                                }
                            }
                        } else {
                            // Firebase authentication failed
                            Log.d("LoginViewModel", "Firebase auth failed: ${authTask.exception?.message}")

                            // Fallback to local database check
                            checkLocalDatabase(state.email, state.password)
                        }
                    }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Login attempt failed with exception: ${e.message}")
                _loginState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = context.getString(R.string.lvm9_login_failed, e.message)
                    )
                }
            }
        }
    }
    private fun getUserFromFirebaseDatabase(firebaseUid: String, email: String) {
        Log.d("LoginViewModel", "Searching for user by email: $email")

        val query = database.child("users").orderByChild("email").equalTo(email)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userEntry = snapshot.children.firstOrNull()
                    Log.d("LoginViewModel", "Firebase data: ${userEntry?.value}")

                    // MANUAL EXTRACTION - don't use automatic parsing
                    val userData = userEntry?.value as? Map<String, Any>

                    if (userData != null) {
                        // Handle the userId whether it's a string or UUID object
                        val userIdValue = userData["userId"]
                        val actualUserId = when (userIdValue) {
                            is String -> userIdValue
                            is Map<*, *> -> {
                                // It's stored as UUID object - extract the bits
                                val bits = userIdValue as Map<String, Long>
                                val mostSigBits = bits["mostSignificantBits"] ?: 0L
                                val leastSigBits = bits["leastSignificantBits"] ?: 0L
                                java.util.UUID(mostSigBits, leastSigBits).toString()
                            }
                            else -> userEntry.key ?: "" // Fallback to database key
                        }

                        val user = User(
                            userId = actualUserId,
                            firstName = userData["firstName"] as? String ?: "",
                            password = userData["password"] as? String ?: "",
                            dateOfBirth = userData["dateOfBirth"] as? String ?: "",
                            email = userData["email"] as? String ?: email,
                            fcmToken = userData["fcmToken"] as? String ?: "",
                            liveLocation = userData["liveLocation"] as? String ?: ""
                        )

                        Log.d("LoginViewModel", "User found with ID: $actualUserId")

                        _loggedInUser = user
                        _loginState.update {
                            it.copy(
                                userId = actualUserId,
                                name = user.firstName,
                                email = email,
                                isLoading = false,
                                isSuccess = true,
                                errorMessage = null
                            )
                        }

                        viewModelScope.launch {
                            syncUserToLocalDatabase(user)
                        }
                    }
                } else {
                    _loginState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = context.getString(R.string.lvm9_account_not_found)
                        )
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LoginViewModel", "Firebase Database error: ${error.message}")
                _loginState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = context.getString(R.string.lvm9_database_error, error.message)
                    )
                }
            }
        })
    }

    private fun checkLocalDatabase(email: String, password: String) {
        viewModelScope.launch {
            try {
                Log.d("LoginViewModel", "Falling back to local database check...")
                var user = dao.getUserByEmail(email)

                if (user == null && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    user = dao.getUserByEmail(email)
                    Log.d("LoginViewModel", "Initial username lookup failed. Attempting lookup by email.")
                }

                if (user == null) {
                    Log.d("LoginViewModel", "User lookup failed. User not found.")
                    _loginState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = context.getString(R.string.lvm9_invalid_email_or_password)
                        )
                    }
                    return@launch
                }

                Log.d("LoginViewModel", "User lookup successful in local database: ${user.email}")

                if (!verifyPassword(password, user.password)) {
                    Log.d("LoginViewModel", "Password mismatch.")
                    _loginState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = context.getString(R.string.lvm9_invalid_email_or_password)
                        )
                    }
                    return@launch
                }

                Log.d("LoginViewModel", "Password match successful. Login complete.")
                _loggedInUser = user
                _loginState.update {
                    it.copy(
                        userId = user.userId,
                        name = user.firstName,
                        email = user.email,
                        isLoading = false,
                        isSuccess = true,
                        errorMessage = null
                    )
                }

            } catch (e: Exception) {
                Log.e("LoginViewModel", "Local database login failed: ${e.message}")
                _loginState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = context.getString(R.string.lvm9_login_failed, e.message)
                    )
                }
            }
        }
    }

    private suspend fun syncUserToLocalDatabase(user: User) {
        try {
            val existingUser = dao.getUserByEmail(user.email)
            if (existingUser == null) {
                dao.upsertUser(user)
                Log.d("LoginViewModel", "User synced to local database: ${user.email}")
            } else {
                dao.upsertUser(user)
                Log.d("LoginViewModel", "User updated in local database: ${user.email}")
            }
        } catch (e: Exception) {
            Log.e("LoginViewModel", "Failed to sync user to local database: ${e.message}")
        }
    }


    class LoginViewModelFactory(private val userDao: UserDao, private val sessionManager: SessionManager, private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LoginViewModel(userDao,sessionManager,context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
