package com.socam.bcms.presentation.modules

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.socam.bcms.R

/**
 * Adapter for displaying workflow steps in a grid format
 * Each step shows: Icon (🔧), Step Code (e.g., ALW10), and Description
 * Aligned with Batch Process Module display format
 */
class WorkflowStepsAdapter(
    private val onStepClick: (WorkflowStepDisplay) -> Unit
) : RecyclerView.Adapter<WorkflowStepsAdapter.WorkflowStepViewHolder>() {

    private var workflowSteps: List<WorkflowStepDisplay> = emptyList()

    fun updateSteps(steps: List<WorkflowStepDisplay>) {
        println("WorkflowStepsAdapter: Updating with ${steps.size} steps")
        steps.forEach { step ->
            println("WorkflowStepsAdapter: Step - ${step.stepCode}: ${step.stepDescription}")
        }
        workflowSteps = steps
        notifyDataSetChanged()
        println("WorkflowStepsAdapter: Adapter notified of data change")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkflowStepViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workflow_step, parent, false)
        return WorkflowStepViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkflowStepViewHolder, position: Int) {
        holder.bind(workflowSteps[position], onStepClick)
    }

    override fun getItemCount(): Int = workflowSteps.size

    class WorkflowStepViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val stepIcon: TextView = itemView.findViewById(R.id.step_icon)
        private val stepCode: TextView = itemView.findViewById(R.id.step_code)
        private val stepDescription: TextView = itemView.findViewById(R.id.step_description)

        fun bind(step: WorkflowStepDisplay, onStepClick: (WorkflowStepDisplay) -> Unit) {
            println("WorkflowStepsAdapter: Binding step: ${step.stepDescription}")
            
            // Parse stepDescription format: "🔧 ALW10: 安裝標籤"
            val description = step.stepDescription
            val parts = description.split(" ", limit = 2) // Split icon from rest
            
            if (parts.size >= 2) {
                // Extract icon (first part)
                stepIcon.text = parts[0]
                
                // Split step code and description by ": "
                val restParts = parts[1].split(": ", limit = 2)
                stepCode.text = restParts[0] // Step code (e.g., "ALW10")
                
                if (restParts.size >= 2) {
                    stepDescription.text = restParts[1] // Description
                } else {
                    stepDescription.text = "" // No description available
                }
            } else {
                // Fallback if format is different
                stepIcon.text = "🔧"
                stepCode.text = step.stepCode
                stepDescription.text = description
            }
            
            println("WorkflowStepsAdapter: Icon=${stepIcon.text}, Code=${stepCode.text}, Desc=${stepDescription.text}")
            
            itemView.setOnClickListener {
                println("WorkflowStepsAdapter: Step clicked: ${step.stepCode}")
                onStepClick(step)
            }
        }
    }
}
