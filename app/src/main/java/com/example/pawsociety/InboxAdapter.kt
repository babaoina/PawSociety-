package com.example.pawsociety

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class InboxAdapter(
    private val users: List<AppUser>,
    private val currentUserId: String,
    private val relationshipRepo: RelationshipRepository,
    private val onUserClick: (AppUser) -> Unit
) : RecyclerView.Adapter<InboxAdapter.InboxViewHolder>() {

    private val colors = listOf(
        "#7A4F2B", "#B88B4A", "#4CAF50", "#2196F3", "#FF9800", "#9C27B0", "#E91E63"
    )

    class InboxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileIcon: TextView = itemView.findViewById(R.id.profile_icon)
        val username: TextView = itemView.findViewById(R.id.username)
        val btnFollow: TextView = itemView.findViewById(R.id.btn_follow)
        
        var followJob: Job? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InboxViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inbox_simple, parent, false)
        return InboxViewHolder(view)
    }

    override fun onBindViewHolder(holder: InboxViewHolder, position: Int) {
        val user = users[position]

        val firstLetter = if (user.username.isNotEmpty()) {
            user.username.first().toString().uppercase()
        } else {
            "?"
        }

        val colorIndex = Math.abs(user.uid.hashCode()) % colors.size
        holder.profileIcon.text = firstLetter
        holder.profileIcon.setBackgroundColor(Color.parseColor(colors[colorIndex]))
        holder.profileIcon.setTextColor(Color.WHITE)

        holder.username.text = user.username

        holder.itemView.setOnClickListener {
            onUserClick(user)
        }
        
        // Follow Logic
        holder.followJob?.cancel()
        holder.btnFollow.setOnClickListener(null)
        
        if (currentUserId.isEmpty() || currentUserId == user.uid) {
            holder.btnFollow.visibility = View.GONE
        } else {
            holder.btnFollow.visibility = View.VISIBLE
            
            holder.followJob = CoroutineScope(Dispatchers.Main).launch {
                relationshipRepo.isFollowing(currentUserId, user.uid).collect { isFollowing ->
                    if (isFollowing) {
                        holder.btnFollow.text = "Following"
                        holder.btnFollow.setBackgroundColor(Color.parseColor("#BDBDBD"))
                    } else {
                        holder.btnFollow.text = "Follow"
                        holder.btnFollow.setBackgroundColor(Color.parseColor("#7A4F2B"))
                    }
                    
                    holder.btnFollow.setOnClickListener {
                        CoroutineScope(Dispatchers.IO).launch {
                            if (isFollowing) {
                                relationshipRepo.unfollowUser(currentUserId, user.uid)
                            } else {
                                relationshipRepo.followUser(currentUserId, user.uid)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount() = users.size
}