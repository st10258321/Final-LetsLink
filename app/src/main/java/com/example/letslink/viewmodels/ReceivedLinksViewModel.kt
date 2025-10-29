package com.example.letslink.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.letslink.API_related.GroupRepo
import com.example.letslink.SessionManager
import com.example.letslink.model.Invites
import kotlinx.coroutines.launch
import java.util.UUID
//Acts as a bridge to allow whatever is inputted in the ui to be written to room (Lackner, 2025b)
class ReceivedLinksViewModel(
    private val groupRepo: GroupRepo,
    private val sessionManager: SessionManager // To get the current user ID
) : ViewModel() {

    // to show the list of invites
    private val _receivedInvites = MutableLiveData<List<Invites>>()
    val receivedInvites: LiveData<List<Invites>> = _receivedInvites


    init {
        fetchReceivedInvites(sessionManager.getUserId().toString())
    }

    fun fetchReceivedInvites(currentUserIdString: String) {
        viewModelScope.launch {
            // Collect  from the repo
            groupRepo.getReceivedInvites(currentUserIdString).collect { invitesList ->
                _receivedInvites.value = invitesList
            }
        }
    }

    //  Function to handle the join
    fun joinGroup(groupId: String, userId: UUID?) {
        viewModelScope.launch {
            // Call the joinGroup
            groupRepo.joinGroup(groupId, userId)


            fetchReceivedInvites(userId.toString())
        }
    }

    companion object {
        /**
         * Factory for creating  with dependencies.

         */
        fun provideFactory(
            groupRepo: GroupRepo,
            sessionManager: SessionManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {

            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // Check if the requested ViewModel class is the correct one
                if (modelClass.isAssignableFrom(ReceivedLinksViewModel::class.java)) {
                    return ReceivedLinksViewModel(groupRepo, sessionManager) as T
                }
                // Throw an exception if the factory is asked to create the wrong ViewModel
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}