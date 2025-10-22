package com.socam.bcms.presentation.modules

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.socam.bcms.databinding.ItemTagActivationCandidateTagBinding
import com.socam.bcms.model.TagStatus

/**
 * RecyclerView adapter for displaying candidate tags in Tag Activation manual selection
 * Shows INACTIVE tags for activation selection
 */
class TagActivationCandidateTagAdapter(
    private val onTagSelected: (TagActivationCandidateTag) -> Unit
) : RecyclerView.Adapter<TagActivationCandidateTagAdapter.TagActivationCandidateTagViewHolder>() {

    private var candidateTags = emptyList<TagActivationCandidateTag>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagActivationCandidateTagViewHolder {
        val binding = ItemTagActivationCandidateTagBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TagActivationCandidateTagViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TagActivationCandidateTagViewHolder, position: Int) {
        holder.bind(candidateTags[position], position + 1) // 1-based ranking
    }

    override fun getItemCount(): Int = candidateTags.size

    /**
     * Update candidate tags list
     */
    fun updateCandidateTags(newTags: List<TagActivationCandidateTag>): Unit {
        candidateTags = newTags
        notifyDataSetChanged()
    }

    /**
     * ViewHolder for candidate tag items
     */
    inner class TagActivationCandidateTagViewHolder(
        private val binding: ItemTagActivationCandidateTagBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(candidateTag: TagActivationCandidateTag, rank: Int): Unit {
            
            binding.rankBadge.text = when (rank) {
                1 -> "1st"
                2 -> "2nd" 
                3 -> "3rd"
                else -> "${rank}th"
            }
            
            // Status badge (all should be INACTIVE since pre-filtered for activation)
            binding.statusBadge.text = when (candidateTag.tagStatus) {
                TagStatus.ACTIVE -> "ACTIVE"
                TagStatus.INACTIVE -> "INACTIVE"
            }
            
            // Set status badge color for INACTIVE tags (ready for activation)
            val context = binding.root.context
            when (candidateTag.tagStatus) {
                TagStatus.ACTIVE -> {
                    binding.statusBadge.setBackgroundTintList(
                        context.getColorStateList(com.socam.bcms.R.color.success)
                    )
                }
                TagStatus.INACTIVE -> {
                    binding.statusBadge.setBackgroundTintList(
                        context.getColorStateList(com.socam.bcms.R.color.warning)
                    )
                }
            }
            
            // RSSI value with color coding
            binding.rssiValue.text = "${candidateTag.rssiDbm} dBm"
            
            // Set RSSI color based on signal strength
            val rssiColor = when {
                candidateTag.rssiDbm >= -40 -> context.getColor(com.socam.bcms.R.color.success)
                candidateTag.rssiDbm >= -60 -> context.getColor(com.socam.bcms.R.color.warning) 
                else -> context.getColor(com.socam.bcms.R.color.error)
            }
            binding.rssiValue.setTextColor(rssiColor)
            
            // For Tag Activation, we show inactive status
            binding.bcType.text = "INACTIVE"
            binding.tagNumber.text = "Ready for Activation"
            
            // EPC value (formatted for readability)
            binding.epcValue.text = candidateTag.epc
            
            // Click handler
            binding.root.setOnClickListener {
                onTagSelected(candidateTag)
            }
        }
    }
}
