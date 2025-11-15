package com.example.letslink.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.example.letslink.R
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.letslink.SessionManager
import com.example.letslink.local_database.LetsLinkDB
import com.example.letslink.local_database.UserDao
import com.example.letslink.online_database.SyncDataManager
import com.example.letslink.online_database.fb_userRepo
import kotlinx.coroutines.launch


class AccountFragment : Fragment() {
    private lateinit var userDao: UserDao
    private lateinit var userRepo : fb_userRepo
    private lateinit var syncManager : SyncDataManager
    private lateinit var sessionManager: SessionManager
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val email = view.findViewById<TextView>(R.id.emailTxt)
        val name = view.findViewById<TextView>(R.id.nameTxt)
        val dateOfBirth = view.findViewById<TextView>(R.id.dateOfBirthTxt)
        var emergencyContact = view.findViewById<EditText>(R.id.emergencyContact)
        val saveBtn = view.findViewById<TextView>(R.id.save_button)
        syncManager = SyncDataManager(requireContext())
        userDao = LetsLinkDB.getDatabase(requireContext()).userDao()
        userRepo = fb_userRepo()
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val emailUser = sharedPref.getString(SessionManager.KEY_USER_EMAIL, "")
        val nameUser = sharedPref.getString(SessionManager.KEY_USER_NAME, "")
        email.text = emailUser
        name.text = nameUser
        val userId = sharedPref.getString(SessionManager.KEY_USER_ID, "")
        lifecycleScope.launch {
          val user = userDao.getUserById(userId!!)
            if(user != null){
                dateOfBirth.text = user.dateOfBirth
            }
            if(user?.emergencyContact != ""){
                emergencyContact.setText(user?.emergencyContact)
            }
        }
        saveBtn.setOnClickListener {
            //save the emergency contact to online and local database
            lifecycleScope.launch {
                val user =   userDao.getUserById(userId!!)
                if(user != null){
                    user.emergencyContact = emergencyContact.text.toString()
                    try {
                        userDao.upsertUser(user)
                    }catch(e : Exception){
                        Log.d("check-error",e.toString())
                        Toast.makeText(requireContext(),"Error updating emergency contact - locally",Toast.LENGTH_SHORT).show()
                    }
                }
            }
            if(syncManager.isInternetAvailable(requireContext())) {
                //update user online
                userRepo.updateUserEmergencyContact(userId!!, emergencyContact.text.toString()) { success ->
                    if(success){
                        Toast.makeText(requireContext(),"Emergency contact updated successfully",Toast.LENGTH_SHORT).show()
                    }else{
                        Toast.makeText(requireContext(),"Error updating emergency contact",Toast.LENGTH_SHORT).show()
                    }

                }
            }

        }




    }
}