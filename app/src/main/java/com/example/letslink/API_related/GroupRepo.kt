
package com.example.letslink.API_related

import com.example.letslink.SessionManager
import com.example.letslink.local_database.GroupDao
import com.example.letslink.local_database.UserDao
import com.example.letslink.model.Group
import com.example.letslink.model.Invites
import com.example.letslink.model.GroupResponse
import com.example.letslink.model.InviteRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 *
 * This class hanndels coordination between the local Room database via GroupDao and the
 * LetsLinkAPI.
 * //(Philipp Lackner ,2021)
 *
 *
 */
class GroupRepo(
    private val groupDao: GroupDao,
    private val groupApiService: LetsLinkAPI,
    private val sessionManager: SessionManager,
    private val db: DatabaseReference,
    private val userDao: UserDao
) {


    fun getGroupsByUserId(userId: UUID): Flow<List<Group>> {
        //  existing method to convert the UUID to a String
        return groupDao.getNotesByUserId(userId.toString())
    }
    suspend fun getRecipientIdFromRoom(username: String): String? {
        return userDao.getUserByUsername(username)?.userId
    }
    suspend fun getRecipientIdFromFirebase(username: String): String? {
        val usersRef = db.child("users")


        val query = usersRef.orderByChild("firstName").equalTo(username)

        return try {
            // Fetch the data  Kotlin coroutine wrapper
            val snapshot = query.get().await()

            if (snapshot.exists()) {

                snapshot.children.firstOrNull()?.key
            } else {
                null
            }
        } catch (e: Exception) {
            // Handle network errors, permission issues, etc.
            e.printStackTrace()
            null
        }
    }

    /**
     * Handles the creation of a group by saving it locally first,
     * then synchronizing with the remote API, and then updating it
     */
    suspend fun createAndSyncGroup(group: Group): GroupResponse? {
        //Save locally first
        groupDao.insertGroup(group)
        val currentUserId = sessionManager.getUserId()

        // calling group request which handles the sending request to api
        val apiRequest = GroupRequest(
            groupId = group.groupId.toString(),
            userId = currentUserId.toString(),
            description = group.description,
            groupName = group.groupName,
        )

        // Sync with the API
        return try {
            val response = groupApiService.createGroup(apiRequest)

            val updatedGroup = group.copy(inviteLink = response.inviteLink)
            groupDao.insertGroup(updatedGroup)
            println("Group synced successfully. Invite link received: ${response.inviteLink}")
            //  Return the successful response which means the link has been generated
            response
        } catch (e: Exception) {
            e.printStackTrace()
            println("ERROR: Failed to sync group ${group.groupId} with API.")
            //Return null on failure
            null
        }
    }

    /**
     * Attempts to join an existing group via the API and a new group
     *
     *
     */
    suspend fun joinGroup(groupId: String, userID: UUID?): GroupResponse? {

        //Use SessionManager to get the current user ID
        val currentUserId = sessionManager.getUserId()

        // Prepare the network request
        val apiRequest = JoinGroupRequest(
            groupId = groupId,
            userId = currentUserId
        )

        // Call the API
        return try {
            val response = groupApiService.joinGroup(apiRequest)

            println("DEBUG API RESPONSE: $response")

            val newGroup = Group(
                groupId = response.groupId,
                userId = currentUserId,
                groupName = response.groupName,
                description = response.description,
                inviteLink = response.inviteLink
            )

            groupDao.insertGroup(newGroup)

            println("Group joined successfully: ${response.groupName}")
            response
        } catch (e: Exception) {
            e.printStackTrace()
            println("ERROR: Failed to join group $groupId. ${e.message}")
            null
        }
    }
    fun getReceivedInvites(userId: String): Flow<List<Invites>> = callbackFlow {
        // Defines the db path to the user's received invites
        val invitesRef = db.child("users").child(userId).child("receivedInvites")

        // Define the type indicator for the entire collection
        val typeIndicator = object : GenericTypeIndicator<Map<String, Invites>>() {}

        val listener = invitesRef.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                //  Read the entire collection using enericTypeIndicator
                val invitesMap: Map<String, Invites>? = snapshot.getValue(typeIndicator)

                // Convert List<Invites>
                val invitesList: List<Invites> = invitesMap?.values?.toList() ?: emptyList()


                trySend(invitesList)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle the error if the read  fails
                close(error.toException()) // Close
            }
        })
        // pauses the coroutine until the flow is closed
        awaitClose {
            invitesRef.removeEventListener(listener)
        }
    }
    suspend fun assignInvite(
        recipientId: String,
        groupId: String,
        groupName: String,
        description: String
    ) {
        val apiRequest = InviteRequest(
            groupId = groupId,
            userId = recipientId,
            groupName = groupName,
            description = description
        )

        try {

            groupApiService.assignInviteToUser(apiRequest)
            println("Invite for group $groupId successfully assigned to user $recipientId.")
        } catch (e: Exception) {
            e.printStackTrace()
            println("ERROR: Failed to assign invite to user $recipientId.")
        }
    }
}

