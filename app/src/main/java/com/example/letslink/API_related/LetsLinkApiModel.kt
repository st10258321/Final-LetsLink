package com.example.letslink.API_related
import android.accessibilityservice.GestureDescription
import com.google.gson.annotations.SerializedName

data class GroupRequest(
    @SerializedName("groupId")
    val groupId: String,
    @SerializedName("userId")
    val userId: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("groupName")
    val groupName: String
)
