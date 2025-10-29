package com.example.letslink.activities

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.example.letslink.R
import com.example.letslink.SessionManager

import com.example.letslink.fragments.CreateTaskFragment
import com.example.letslink.local_database.GroupDao
import com.example.letslink.local_database.LetsLinkDB
import com.example.letslink.model.Event
import com.example.letslink.model.Group
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class EventDetails : Fragment() {
    companion object {
        private const val ARG_EVENT_ID = "eventId"
        private const val ARG_TITLE = "title"
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_LOCATION = "location"
        private const val ARG_START_TIME = "startTime"
        private const val ARG_END_TIME = "endTime"
        private const val ARG_DATE = "date"

        fun newInstace(event: Event): EventDetails {
            val fragment = EventDetails()
            val bundke = Bundle().apply {
                putString(ARG_EVENT_ID, event.eventId)
                putString(ARG_TITLE, event.title)
                putString(ARG_DESCRIPTION, event.description)
                putString(ARG_LOCATION, event.location)
                putString(ARG_START_TIME, event.startTime)
                putString(ARG_END_TIME, event.endTime)
                putString(ARG_DATE, event.date)
            }
            fragment.arguments = bundke
            return fragment
        }
    }

    private val selectedGroupIds = mutableListOf<String>()
    private val groupList = mutableListOf<Group>()

    //private val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var groupDao: GroupDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_event_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        groupDao = LetsLinkDB.getDatabase(requireContext()).groupDao()
        val name = auth.currentUser?.displayName
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val userId = sharedPref.getString(SessionManager.KEY_USER_ID, null)
        val eventId = arguments?.getString(ARG_EVENT_ID)
        // Back Button Logic
        val backArrow: ImageButton = view.findViewById(R.id.back_image_button)
        backArrow.setOnClickListener {

            parentFragmentManager.popBackStack()
        }
        val tvSelectedGroups = view.findViewById<TextView>(R.id.tvSelectedGroups)
        tvSelectedGroups.setOnClickListener {
            showGroupSelectionDialog()
        }
        //getting group names and id's from database
        lifecycleScope.launch {
            groupDao.getNotesByUserId(userId!!).collect { groups ->
                groupList.clear()
                groupList.addAll(groups)
                Log.d("GroupList", groupList.count().toString())
            }
        }
        //adding the current event being viewed to different groups
        val btnAddToGroups: Button = view.findViewById(R.id.btnAddToGroups)
        btnAddToGroups.setOnClickListener {
            val dbRef = FirebaseDatabase.getInstance().getReference("events")
            dbRef.child(eventId!!).child("groups").setValue(selectedGroupIds).addOnSuccessListener {
                Toast.makeText(requireContext(), "Event added to groups", Toast.LENGTH_SHORT).show()
                tvSelectedGroups.setText("Select Groups")
            }.addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Failed to add event to groups",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val addTaskButton: LinearLayout = view.findViewById(R.id.btn_add_task)
        //val userIdString = sharedPreferences.getString("KEY_USER_ID", null) ?: return

        val todoButton = view.findViewById<View>(R.id.btn_view_todo)
        todoButton.setOnClickListener {
            val toDoFragment = ToDoFragment.newInstance(eventId!!)
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    toDoFragment
                ) // make sure this matches your container id
                .addToBackStack(null) // so user can go back
                .commit()

        }

        addTaskButton?.setOnClickListener {
            val createTaskFragment = CreateTaskFragment.newInstance(eventId!!)

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, createTaskFragment)
                .addToBackStack(null)
                .commit()
        }

        val title = arguments?.getString(ARG_TITLE)
        val description = arguments?.getString(ARG_DESCRIPTION)
        val location = arguments?.getString(ARG_LOCATION)
        val startTime = arguments?.getString(ARG_START_TIME)
        val endTime = arguments?.getString(ARG_END_TIME)
        val date = arguments?.getString(ARG_DATE)

        val eventTitle: TextView = view.findViewById(R.id.txt_event_title)
        val eventDescription: TextView = view.findViewById(R.id.txt_event_description)
        val eventLocation: TextView = view.findViewById(R.id.txt_event_location)
        val eventDate: TextView = view.findViewById(R.id.txt_event_date)
        val eventTime: TextView = view.findViewById(R.id.txt_event_time)
        val eventHost: TextView = view.findViewById(R.id.txt_event_host)

        eventTime.text = "${eventTime.text} $startTime"
        eventDate.text = "${eventDate.text} $date"
        eventLocation.text = "${eventLocation.text} $location"
        eventDescription.text = description
        eventTitle.text = title
        eventHost.text = name
    }

    private fun showGroupSelectionDialog() {
        val groupName = groupList.map { it.groupName }.toTypedArray()
        val checkedItems = BooleanArray(groupList.size) { i ->
            selectedGroupIds.contains(groupList[i].groupId.toString())
        }

        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Select Groups")
        builder.setMultiChoiceItems(groupName, checkedItems) { _, which, isChecked ->
            val group = groupList[which]
            if (isChecked) {
                if (!selectedGroupIds.contains(group.groupId.toString())) {
                    selectedGroupIds.add(group.groupId.toString())
                }
            } else {
                selectedGroupIds.remove(group.groupId.toString())
            }
        }
        builder.setPositiveButton("OK") { dialog, _ ->
            val selectedName = groupList.filter { selectedGroupIds.contains(it.groupId.toString()) }
                .map { it.groupName }

            val tvSelectedGroups = view?.findViewById<TextView>(R.id.tvSelectedGroups)
            if (selectedName.isEmpty()) {
                tvSelectedGroups?.setText("Select Groups")
            } else {
                tvSelectedGroups?.setText(selectedName.toString())
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }
}
