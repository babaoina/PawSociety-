package com.example.pawsociety

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FindAdapter(
    private val pets: List<Pet>,
    private val onItemClick: (Pet) -> Unit
) : RecyclerView.Adapter<FindAdapter.FindViewHolder>() {

    private val colors = listOf(
        "#FF6B35", "#4CAF50", "#2196F3", "#9C27B0",
        "#F44336", "#009688", "#FF9800", "#3F51B5",
        "#E91E63", "#7A4F2B", "#00BCD4", "#8BC34A"
    )

    class FindViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: FrameLayout = itemView.findViewById(R.id.post_container)
        val imagePlaceholder: TextView = itemView.findViewById(R.id.image_placeholder)
        val petName: TextView = itemView.findViewById(R.id.pet_name)
        val statusBadge: TextView = itemView.findViewById(R.id.status_badge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FindViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_find_grid, parent, false)
        return FindViewHolder(view)
    }

    override fun onBindViewHolder(holder: FindViewHolder, position: Int) {
        try {
            val pet = pets[position]

            // Set placeholder text
            val firstLetter = if (pet.name.isNotEmpty()) {
                pet.name.first().toString()
            } else {
                "?"
            }

            val emoji = "🐾"
            holder.imagePlaceholder.text = "$emoji\n$firstLetter"

            // Set color based on pet ID
            val colorIndex = Math.abs(pet.petId.hashCode()) % colors.size
            holder.imagePlaceholder.setBackgroundColor(Color.parseColor(colors[colorIndex]))

            // Set pet name
            holder.petName.text = pet.name

            // Set status badge (Breed instead)
            holder.statusBadge.text = pet.breed
            holder.statusBadge.setBackgroundColor(Color.parseColor("#7A4F2B"))

            // Make container square
            holder.container.post {
                val width = holder.container.width
                if (width > 0) {
                    holder.container.layoutParams.height = width
                    holder.container.requestLayout()
                }
            }

            // Set click listener
            holder.itemView.setOnClickListener {
                onItemClick(pet)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getItemCount() = pets.size
}