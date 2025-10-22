package com.socam.bcms.presentation.modules

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.socam.bcms.R
import com.socam.bcms.model.TagModificationData
import com.socam.bcms.model.TagStatus

/**
 * RecyclerView Adapter for Multiple Scan mode tag list
 * Displays tags with real-time updates, highlighting, and sorting
 */
class MultipleScanTagAdapter : ListAdapter<TagModificationData, MultipleScanTagAdapter.TagViewHolder>(TagDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_multiple_scan_tag, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tag = getItem(position)
        holder.bind(tag)
    }

    /**
     * Update the list with new data and smooth scrolling to show changes
     */
    fun updateTags(newTags: List<TagModificationData>) {
        submitList(newTags.toList()) // Create new list to trigger DiffUtil
    }

    class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val statusBadge: TextView = itemView.findViewById(R.id.status_badge)
        private val rssiValue: TextView = itemView.findViewById(R.id.rssi_value)
        private val epcValue: TextView = itemView.findViewById(R.id.epc_value)
        private val tidLayout: View = itemView.findViewById(R.id.tid_layout)
        private val tidValue: TextView = itemView.findViewById(R.id.tid_value)

        fun bind(tag: TagModificationData) {
            // Status Badge
            val statusInfo = tag.getStatusDisplayInfo()
            statusBadge.text = statusInfo.displayName
            
            // Set badge color based on status
            val badgeColor = when (tag.getTagStatus()) {
                TagStatus.ACTIVE -> itemView.context.getColor(android.R.color.holo_green_dark)
                TagStatus.INACTIVE -> itemView.context.getColor(android.R.color.holo_red_dark)
            }
            statusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(badgeColor)
            
            // RSSI Value
            rssiValue.text = "${tag.rssiDbm} dBm"
            
            // EPC Value
            epcValue.text = tag.epc
            
            // TID Value (show if available)
            if (!tag.tid.isNullOrBlank()) {
                tidLayout.visibility = View.VISIBLE
                tidValue.text = tag.tid
            } else {
                tidLayout.visibility = View.GONE
            }
            
            // Optional: Highlight recently updated items
            // You can add animation or highlighting here for real-time updates
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    private class TagDiffCallback : DiffUtil.ItemCallback<TagModificationData>() {
        override fun areItemsTheSame(oldItem: TagModificationData, newItem: TagModificationData): Boolean {
            return oldItem.epc == newItem.epc
        }

        override fun areContentsTheSame(oldItem: TagModificationData, newItem: TagModificationData): Boolean {
            return oldItem == newItem
        }
    }
}
