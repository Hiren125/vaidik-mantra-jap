package com.vedicapps.mantrajap

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EmptyStateAdapter(private var isVisible: Boolean = false) : RecyclerView.Adapter<EmptyStateAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Use the Activity context from the parent view
        val context = parent.context
        val textView = TextView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            // Padding values in pixels (48px horizontal, 40px vertical)
            setPadding(48, 60, 48, 60)
            gravity = Gravity.CENTER

            // FIX: Use 16f (Float). Android's .textSize property defaults to SP.
            textSize = 16f

            // Use our Saffron color from the theme
            setTextColor(context.getColorFromAttr(com.google.android.material.R.attr.colorPrimary))

            text = context.getString(R.string.no_mantra_found)
            visibility = View.VISIBLE
        }
        return ViewHolder(textView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // No dynamic binding needed for a static message
    }

    override fun getItemCount(): Int = if (isVisible) 1 else 0

    fun updateVisibility(visible: Boolean) {
        if (isVisible != visible) {
            isVisible = visible
            // Standard refresh to show/hide the row
            notifyDataSetChanged()
        }
    }
}