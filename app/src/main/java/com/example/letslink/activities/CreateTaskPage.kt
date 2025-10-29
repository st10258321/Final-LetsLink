package com.example.letslink.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.letslink.R
import com.example.letslink.model.Task
import com.example.letslink.online_database.fb_TaskRepo

class CreateTaskFragment : Fragment() {
    private lateinit var fb_TaskRepo : fb_TaskRepo
    companion object {
        private const val ARG_EVENT_ID = "eventId"

        fun newInstance(eventId: String): CreateTaskFragment {
            val fragment = CreateTaskFragment()
            val bundle = Bundle().apply {
                putString(ARG_EVENT_ID, eventId)
            }
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // We use the layout file originally intended for the Activity
        return inflater.inflate(R.layout.activity_create_task_page, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val eventId = arguments?.getString(ARG_EVENT_ID)
        fb_TaskRepo = fb_TaskRepo(requireContext())

        // Find the back arrow button assuming the ID is 'backArrow' in the layout
        val backArrow: ImageView = view.findViewById(R.id.backArrow)

        backArrow.setOnClickListener {
            // Correct way for a Fragment to go back to the previous Fragment
            parentFragmentManager.popBackStack()
        }
        val btnSave : Button = view.findViewById(R.id.btnSave)
        btnSave.setOnClickListener {
            val etItemName : EditText = view.findViewById(R.id.etItemName)
            val etDescription : EditText = view.findViewById(R.id.etDescription)
            val etDuration : EditText = view.findViewById(R.id.etDuration)
            val datePicker : DatePicker = view.findViewById(R.id.datePicker)

            val day = datePicker.dayOfMonth
            val month = datePicker.month +1
            val year = datePicker.year
            val date = "$day/$month/$year"
            val task : Task = Task()
            task.eventId = eventId!!
            task.taskName = etItemName.text.toString()
            task.taskDescription = etDescription.text.toString()
            task.taskDuration = etDuration.text.toString()
            task.dueDate = date
            fb_TaskRepo.createTask(task) { success, message ->
                if (success) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }

        }



    }
}