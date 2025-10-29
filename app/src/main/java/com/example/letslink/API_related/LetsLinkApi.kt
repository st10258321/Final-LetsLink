package com.example.letslink.API_related

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import androidx.room.Dao
import com.example.letslink.API_related.GroupRequest
import com.example.letslink.API_related.JoinGroupRequest
import com.example.letslink.model.Group
import com.example.letslink.model.GroupResponse
import com.example.letslink.model.InviteRequest


/**
 * Interface for the LetsLink API
 */
//(Philipp Lackner, 2021)
interface LetsLinkAPI {

    // @GET("groups")
    //suspend fun getGroups(): List<Group>

    @POST("groups")
    suspend fun createGroup(@Body request: GroupRequest): GroupResponse

    @POST("api/group/join")
    suspend fun joinGroup(@Body request: JoinGroupRequest): GroupResponse
    @POST("/invite/specificUser")
    suspend fun assignInviteToUser(@Body request: InviteRequest): GroupResponse
}