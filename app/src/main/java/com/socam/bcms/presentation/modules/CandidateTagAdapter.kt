package com.socam.bcms.presentation.modules

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.socam.bcms.databinding.ItemCandidateTagBinding

/**
 * RecyclerView adapter for displaying candidate tags in Single Scan manual selection
 * Shows ACTIVATED tags for processing selection
 * 
 * Badge Types:
 * - ACTIVE (Green): Normal activated tag ready for processing
 * - DISPOSED (Gray): Tag marked as disposed in database (shows warning when selected)
 * - NO RECORD (Purple): Tag not found in database
 */
class CandidateTagAdapter(
    private val onTagSelected: (CandidateTag) -> Unit
) : RecyclerView.Adapter<CandidateTagAdapter.CandidateTagViewHolder>() {

    private var candidateTags = emptyList<CandidateTag>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateTagViewHolder {
        val binding = ItemCandidateTagBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CandidateTagViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CandidateTagViewHolder, position: Int) {
        holder.bind(candidateTags[position], position + 1) // 1-based ranking
    }

    override fun getItemCount(): Int = candidateTags.size

    /**
     * Update candidate tags list
     */
    fun updateCandidateTags(newTags: List<CandidateTag>): Unit {
        candidateTags = newTags
        notifyDataSetChanged()
        android.util.Log.d("CandidateTagAdapter", "Updated adapter with ${candidateTags.size} tags")
    }

    /**
     * ViewHolder for candidate tag items
     */
    inner class CandidateTagViewHolder(
        private val binding: ItemCandidateTagBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(candidateTag: CandidateTag, rank: Int): Unit {
            // Rank badge (1st, 2nd, 3rd, etc.)
            binding.rankBadge.text = when (rank) {
                1 -> "1st"
                2 -> "2nd" 
                3 -> "3rd"
                else -> "${rank}th"
            }
            
            // Enhanced status badge with colors
            binding.statusBadge.text = candidateTag.badgeType.displayName
            binding.statusBadge.setBackgroundColor(android.graphics.Color.parseColor(candidateTag.badgeType.colorHex))
            
            // RSSI value with color coding (green for strong, red for weak)
            binding.rssiValue.text = "${candidateTag.rssiDbm} dBm"
            binding.rssiValue.setTextColor(
                if (candidateTag.rssiDbm > -50) android.graphics.Color.parseColor("#4CAF50") // Green
                else android.graphics.Color.parseColor("#F44336") // Red
            )
            
            // BC Type and Tag Number (only show if database record exists)
            if (candidateTag.bcType.isNotEmpty() && candidateTag.tagNo.isNotEmpty()) {
                binding.bcType.text = candidateTag.bcType
                binding.tagNumber.text = candidateTag.tagNo
                binding.bcType.visibility = android.view.View.VISIBLE
                binding.tagNumber.visibility = android.view.View.VISIBLE
            } else {
                // Hide BC Type and Tag Number if no database record
                binding.bcType.text = "No Record"
                binding.tagNumber.text = ""
                binding.bcType.visibility = android.view.View.VISIBLE
                binding.tagNumber.visibility = android.view.View.GONE
            }
            
            // EPC value (formatted for readability)
            binding.epcValue.text = candidateTag.epc
            
            // Visual treatment for clickable vs non-clickable
            if (candidateTag.isClickable) {
                binding.root.alpha = 1.0f
                binding.root.isEnabled = true
                binding.root.setOnClickListener {
                    onTagSelected(candidateTag)
                }
            } else {
                binding.root.alpha = 0.5f // Grayed out
                binding.root.isEnabled = false
                binding.root.setOnClickListener(null)
            }
        }
    }
}
