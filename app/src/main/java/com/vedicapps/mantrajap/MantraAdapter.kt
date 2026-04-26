package com.vedicapps.mantrajap

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class MantraAdapter(private val onClick: (Mantra) -> Unit) :
    ListAdapter<Mantra, MantraAdapter.MantraViewHolder>(MantraDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MantraViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mantra, parent, false)
        return MantraViewHolder(view)
    }

    override fun onBindViewHolder(holder: MantraViewHolder, position: Int) {
        val mantra = getItem(position)
        val context = holder.itemView.context

        // 1. Calculations - Only focusing on full Malas
        val completedMalas = mantra.count / 108
        val totalTargetMalas = mantra.target
        val totalBeadsTarget = totalTargetMalas * 108

        // 2. Bind Text Labels
        holder.nameText.text = mantra.name
//        val label = if (totalTargetMalas == 1) "Mala" else "Malas"

        val label = if (totalTargetMalas == 1) {
            context.getString(R.string.label_mala_singular)
        } else {
            context.getString(R.string.label_mala_plural)
        }

        holder.progressText.text = "$completedMalas / $totalTargetMalas $label"

        // 3. Progress Bar Setup
        holder.progressBar.max = totalBeadsTarget
        holder.progressBar.progress = mantra.count.coerceAtMost(totalBeadsTarget)

        // 4. THE MALA-ONLY LOGIC
        when {
            // If user has done MORE full malas than the target (e.g., 2/1)
            completedMalas > totalTargetMalas -> {
                val overRed = Color.parseColor("#D32F2F")
                holder.progressBar.progressTintList = ColorStateList.valueOf(overRed)
                holder.progressText.setTextColor(overRed)
                holder.itemView.alpha = 1.0f
            }

            // If user has reached the EXACT number of full malas (e.g., 1/1)
            completedMalas == totalTargetMalas -> {
                val successGreen = Color.parseColor("#4CAF50")
                holder.progressBar.progressTintList = ColorStateList.valueOf(successGreen)
                holder.progressText.setTextColor(successGreen)
                holder.itemView.alpha = 0.8f
            }

            // Still chanting the target malas (e.g., 0/1)
            else -> {
                val primarySaffron = context.getColorFromAttr(com.google.android.material.R.attr.colorPrimary)
                holder.progressBar.progressTintList = ColorStateList.valueOf(primarySaffron)
                holder.progressText.setTextColor(primarySaffron)
                holder.itemView.alpha = 1.0f
            }
        }

        holder.itemView.setOnClickListener { onClick(mantra) }
    }

    class MantraViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.textMantraName)
        val progressText: TextView = itemView.findViewById(R.id.textMantraProgress)
        val progressBar: ProgressBar = itemView.findViewById(R.id.mantraProgressBar)
    }

    class MantraDiffCallback : DiffUtil.ItemCallback<Mantra>() {
        override fun areItemsTheSame(oldItem: Mantra, newItem: Mantra) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Mantra, newItem: Mantra) = oldItem == newItem
    }
}

/**
 * Extension for theme color access
 */
fun Context.getColorFromAttr(attr: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attr, typedValue, true)
    return typedValue.data
}