package com.vedicapps.mantrajap

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HeaderAdapter(private val fullTitle: String) : RecyclerView.Adapter<HeaderAdapter.HeaderViewHolder>() {

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconTxt: TextView = view.findViewById(R.id.headerIcon)
        val titleTxt: TextView = view.findViewById(R.id.headerTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_header, parent, false)
        return HeaderViewHolder(view)
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        // This logic splits your string (e.g., "✨ Suggested Mantras")
        // into the Emoji part and the Text part.
        val parts = fullTitle.split(" ", limit = 2)
        if (parts.size == 2) {
            holder.iconTxt.text = parts[0]   // Shows the Emoji
            holder.titleTxt.text = parts[1]  // Shows the Text
        } else {
            holder.titleTxt.text = fullTitle
        }
    }

    override fun getItemCount() = 1
}