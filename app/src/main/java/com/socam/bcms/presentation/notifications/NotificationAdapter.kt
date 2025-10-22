package com.socam.bcms.presentation.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.socam.bcms.R

/**
 * Adapter for displaying sync error notifications in RecyclerView
 */
class NotificationAdapter(
    private val onClearError: (String) -> Unit
) : ListAdapter<SyncErrorDisplayItem, NotificationAdapter.NotificationViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification_error, parent, false)
        return NotificationViewHolder(view, onClearError)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationViewHolder(
        itemView: View,
        private val onClearError: (String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardNotificationError)
        private val textBcType: TextView = itemView.findViewById(R.id.textBcType)
        private val textTagId: TextView = itemView.findViewById(R.id.textTagId)
        private val textTagNumber: TextView = itemView.findViewById(R.id.textTagNumber)
        private val textCategory: TextView = itemView.findViewById(R.id.textCategory)
        private val textErrorMessage: TextView = itemView.findViewById(R.id.textErrorMessage)
        private val textTimestamp: TextView = itemView.findViewById(R.id.textTimestamp)
        private val textRetryCount: TextView = itemView.findViewById(R.id.textRetryCount)
        private val textSeverity: TextView = itemView.findViewById(R.id.textSeverity)
        private val buttonClear: MaterialButton = itemView.findViewById(R.id.buttonClearError)

        fun bind(item: SyncErrorDisplayItem) {
            textBcType.text = item.bcType
            textTagId.text = "Tag: ${item.tagId}"
            textTagNumber.text = "Number: ${item.tagNumber}"
            textCategory.text = "Category: ${item.category}"
            textErrorMessage.text = item.errorMessage
            textTimestamp.text = item.formattedTime
            textRetryCount.text = "Retries: ${item.retryCount}"
            textSeverity.text = item.severityLevel.displayName

            // Set severity color
            val context = itemView.context
            val severityColor = ContextCompat.getColor(context, item.severityLevel.colorRes)
            textSeverity.setTextColor(severityColor)
            
            // Set card stroke color based on severity
            cardView.strokeColor = when (item.severityLevel) {
                SeverityLevel.HIGH -> ContextCompat.getColor(context, android.R.color.holo_red_light)
                SeverityLevel.MEDIUM -> ContextCompat.getColor(context, android.R.color.holo_orange_light)
                SeverityLevel.LOW -> ContextCompat.getColor(context, android.R.color.holo_blue_light)
            }
            cardView.strokeWidth = 2

            // Clear button click
            buttonClear.setOnClickListener {
                onClearError(item.recordId)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<SyncErrorDisplayItem>() {
        override fun areItemsTheSame(oldItem: SyncErrorDisplayItem, newItem: SyncErrorDisplayItem): Boolean {
            return oldItem.recordId == newItem.recordId
        }

        override fun areContentsTheSame(oldItem: SyncErrorDisplayItem, newItem: SyncErrorDisplayItem): Boolean {
            return oldItem == newItem
        }
    }
}
