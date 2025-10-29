package com.example.letslink.model

sealed interface LoginEvent {
    data class checkEmail(val email: String) : LoginEvent
    data class checkPassword(val password: String) : LoginEvent
    object Login : LoginEvent
    data class GoogleLogin(val idToken: String) : LoginEvent
    data class LoginFailed(val message: String) : LoginEvent

}