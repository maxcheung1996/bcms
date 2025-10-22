package com.socam.bcms.presentation.modules

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.socam.bcms.R

/**
 * Adapter for displaying batch scanned tags in RecyclerView
 */
class BatchTagsAdapter(
    private val onRemoveTag: (String) -> Unit
) : ListAdapter<BatchTagData, BatchTagsAdapter.BatchTagViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BatchTagViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_batch_tag, parent, false)
        return BatchTagViewHolder(view, onRemoveTag)
    }

    override fun onBindViewHolder(holder: BatchTagViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BatchTagViewHolder(
        itemView: View,
        private val onRemoveTag: (String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val bcTypeBadge: TextView = itemView.findViewById(R.id.bcTypeBadge)
        private val rssiValue: TextView = itemView.findViewById(R.id.rssiValue)
        private val tagNumber: TextView = itemView.findViewById(R.id.tagNumber)
        private val epcValue: TextView = itemView.findViewById(R.id.epcValue)
        private val removeTagButton: MaterialButton = itemView.findViewById(R.id.removeTagButton)

        fun bind(batchTag: BatchTagData) {
            // BC Type Badge
            bcTypeBadge.text = batchTag.bcType
            setBcTypeColor(batchTag.bcType)
            
            // RSSI Value
            rssiValue.text = batchTag.getFormattedRssi()
            
            // Tag Number
            tagNumber.text = batchTag.tagNumber
            
            // EPC Value (formatted for readability)
            epcValue.text = formatEpcForDisplay(batchTag.epc)
            
            // Remove Button
            removeTagButton.setOnClickListener {
                onRemoveTag(batchTag.epc)
            }
        }

        private fun setBcTypeColor(bcType: String) {
            val color = when (bcType.uppercase()) {
                "MIC" -> Color.parseColor("#FF6B35") // Orange
                "ALW" -> Color.parseColor("#4ECDC4") // Teal
                "TID" -> Color.parseColor("#45B7D1") // Blue
                else -> Color.parseColor("#6C757D")   // Gray
            }
            bcTypeBadge.setBackgroundColor(color)
        }

        private fun formatEpcForDisplay(epc: String): String {
            // Add spaces every 4 characters for better readability
            return epc.chunked(4).joinToString(" ")
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<BatchTagData>() {
        override fun areItemsTheSame(oldItem: BatchTagData, newItem: BatchTagData): Boolean {
            return oldItem.epc == newItem.epc
        }

        override fun areContentsTheSame(oldItem: BatchTagData, newItem: BatchTagData): Boolean {
            return oldItem == newItem
        }
    }
}
