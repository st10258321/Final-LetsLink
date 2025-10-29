package com.example.letslink.viewmodels

import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.letslink.local_database.UserDao
import com.example.letslink.API_related.UUIDConverter
import com.example.letslink.SessionManager
import com.example.letslink.model.LoginEvent
import com.example.letslink.model.LoginState
import com.example.letslink.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.MessageDigest
//Acts as a bridge to allow whatever is inputted in the ui to be written to room (Lackner, 2025b)
class LoginViewModel(private val dao: UserDao) : ViewModel() {
    private val firebaseAuth = FirebaseAuth.getInstance()
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
    private fun siginInWithGoogle(idToken:String){
        _loginState.update{it.copy(isLoading = true, errorMessage = null)}
        uuidConverter = UUIDConverter()
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        viewModelScope.launch{
            try {
                    firebaseAuth.signInWithCredential(credential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                _loginState.update {
                                    it.copy(
                                        email = task.result.user?.email!!,
                                        isLoading = false,
                                        isSuccess = true,
                                        errorMessage = null
                                    )
                                }
                                val id = task.result.user?.uid
                                val name = task.result.user?.displayName
                                val email = task.result.user?.email

                                if (name != null && email != null) {
                                    _loggedInUser?.userId = id!!
                                    _loggedInUser?.firstName = name
                                    _loggedInUser?.email = email


                                }

                            } else {
                                _loginState.update {
                                    it.copy(
                                        isLoading = false,
                                        errorMessage = task.exception?.message
                                    )
                                }
                            }
                        }

                } catch (e:Exception){
                    _loginState.update { it.copy(isLoading = false, isSuccess = false, errorMessage = e.message) }
                Log.d("LoginViewModel", "Google login failed with exception: ${e.message}")
                }

        }
    }
    private fun attemptLogin() {
        val state = loginState.value
        Log.d("LoginViewModel", "Attempting login for user: ${state.email}")

        if (state.email.isBlank()) {
            _loginState.update { it.copy(errorMessage = "Email is required") }
            Log.d("LoginViewModel", "Email is blank.")
            return
        }

        if (state.password.isBlank()) {
            _loginState.update { it.copy(errorMessage = "Password is required") }
            Log.d("LoginViewModel", "Password is blank.")
            return
        }

        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                var user = dao.getUserByEmail(state.email)


                if (user == null && Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
                    user = dao.getUserByEmail(state.email)
                    Log.d("LoginViewModel", "Initial username lookup failed. Attempting lookup by email.")
                }

                if (user == null) {
                    Log.d("LoginViewModel", "User lookup failed. User not found.")
                    _loginState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Invalid email or password"
                        )
                    }
                    return@launch
                }

                Log.d("LoginViewModel", "User lookup successful. User found: ${user.email}")

                if (!verifyPassword(state.password, user.password)) {
                    Log.d("LoginViewModel", "Password mismatch.")
                    _loginState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Invalid email or password"
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
                Log.e("LoginViewModel", "Login attempt failed with exception: ${e.message}")
                _loginState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Login failed: ${e.message}"
                    )
                }
            }
        }
    }

    class LoginViewModelFactory(private val userDao: UserDao) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LoginViewModel(userDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}