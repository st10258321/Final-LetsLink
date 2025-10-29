package com.example.letslink.online_database
import android.util.Log
import com.example.letslink.model.User
import com.google.firebase.analytics.analytics
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging


class fb_userRepo(
    private val auth:FirebaseAuth = FirebaseAuth.getInstance(),
    private val database : DatabaseReference = com.google.firebase.database.FirebaseDatabase.getInstance().reference
) {
    fun register(user : User, callback :(Boolean, String?, User?) -> Unit){
        auth.createUserWithEmailAndPassword(user.email,user.password)
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    FirebaseMessaging.getInstance().token.addOnCompleteListener{tokenTask ->
                        if(tokenTask.isSuccessful){
                            user.fcmToken = tokenTask.result ?: ""
                            Log.d("FCM Token", "FCM Token: ${user.fcmToken}")
                        }else{
                            Log.e("FCM Token", "Failed to get FCM token", tokenTask.exception)
                        }

                        database.child("users")
                            .child(user.userId.toString())
                            .setValue(user)
                            .addOnCompleteListener{ task ->
                                if(task.isSuccessful){
                                    callback(true,null,user)
                                }else{
                                    callback(false,task.exception?.message,null)
                                }
                            }
                    }

                }else{
                    callback(false,task.exception?.message,null)
                }



            }

    }

}