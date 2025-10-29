package com.example.letslink.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.letslink.API_related.AppContainer
import com.example.letslink.API_related.MyApp
import com.example.letslink.R
import com.example.letslink.SessionManager
import com.example.letslink.activities.GroupChatActivity
import com.example.letslink.activities.GroupDetailsF
import com.example.letslink.activities.ReceivedLinks
import com.example.letslink.adapter.GroupAdapter
import com.example.letslink.local_database.GroupEvent
import com.example.letslink.viewmodels.GroupViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class GroupsFragment : Fragment() {
    private lateinit var viewModel: GroupViewModel
    private lateinit var appContainer: AppContainer
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: GroupAdapter
    private lateinit var groupsRecyclerView: RecyclerView

    private lateinit var floatingButton: FloatingActionButton
    private lateinit var inviteLinkEditText: EditText
    private lateinit var userName: EditText
    private lateinit var joinGroupButton: Button
    private lateinit var sendToUser: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_groups, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        groupsRecyclerView = view.findViewById(R.id.groups_recycler_view)
        inviteLinkEditText = view.findViewById(R.id.link)
        userName = view.findViewById(R.id.username)
        groupsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        val application = requireActivity().application

        appContainer = (application as MyApp).container
        joinGroupButton = view.findViewById(R.id.joinGroupButton)
        sendToUser = view.findViewById(R.id.button)
        sessionManager = appContainer.sessionManager

        viewModel = ViewModelProvider(
            this,
            GroupViewModel.provideFactory(appContainer.groupRepository, sessionManager)
        )[GroupViewModel::class.java]

        setupRecyclerView()
        setupClickListeners()
        observeGroups()
        observeViewModelState()
    }

    private fun observeViewModelState() {
        lifecycleScope.launch {
            viewModel.noteState.collect { state ->
                state.errorMessage?.let { msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                }

                // Show success message
                if (state.isSuccess) {

                    Toast.makeText(
                        requireContext(),
                        "Group operation successful!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun observeGroups() {
        lifecycleScope.launch {
            viewModel.groups.collect { groups ->
                adapter.submitList(groups)
                println("Loaded ${groups.size} groups (via ViewModel and Repository)")
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = GroupAdapter(
            onGroupClick = { note ->
                val groupDetailsFragment = GroupDetailsF()
                val bundle = Bundle().apply {
                    putString("groupId", note.groupId.toString())
                }
                groupDetailsFragment.arguments = bundle

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, groupDetailsFragment)
                    .addToBackStack(null)
                    .commit()
            },
            onGroupDelete = { note ->
                viewModel.onEvent(GroupEvent.deleteNotes(note))
            }
        )

        groupsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@GroupsFragment.adapter
        }
    }

    private fun setupClickListeners() {

        joinGroupButton.setOnClickListener {
            // goes ReceivedLinks page
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ReceivedLinks())
                .addToBackStack(null)
                .commit()
        }

        // 3. send link
        sendToUser.setOnClickListener {
            val rawInviteLink = inviteLinkEditText.text.toString().trim()
            val recipientUsername = userName.text.toString().trim()
//require context is needed to view toast for fragmenets (Ariel ,202
            if (recipientUsername.isBlank() || rawInviteLink.isBlank()) {
                Toast.makeText(requireContext(), "User name or link is empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //  takes the actual uuid from the end of the url
            val groupIdToSearch = try {
                // Split the URL
                rawInviteLink.substringAfterLast("/")
            } catch (e: Exception) {
                //require context is needed to view toast for fragmenets (Ariel ,202
                Toast.makeText(requireContext(), "Invalid Group Link/ID format.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Check if the extracted ID is valid
            if (groupIdToSearch.length != 36) {
                //require context is needed to view toast for fragmenets (Ariel ,202
                Toast.makeText(requireContext(), "Invalid Group Link/ID format (ID not found).", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            //Find the Group to Invite
            val groupToInviteFrom = viewModel.groups.value.find {
                // compares the ids
                it.groupId.toString() == groupIdToSearch
            }

            if (groupToInviteFrom == null) {
                Toast.makeText(requireContext(), "Group not found for this link/ID in your list.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            //Call ViewModel to find the user ID and send the invite
            lifecycleScope.launch {
                viewModel.sendPersonalizedInvite(recipientUsername, groupToInviteFrom)
                //require context is needed to view toast for fragmenets (Ariel ,202
                Toast.makeText(requireContext(), "Processing invite to $recipientUsername...", Toast.LENGTH_SHORT).show()
            }
        }
    }
}