package com.Saathi.offliniai.ui

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.Saathi.offliniai.R

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    private var messages = listOf<ChatMessage>()

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val row: LinearLayout = itemView.findViewById(R.id.messageRow)
        private val roleText: TextView = itemView.findViewById(R.id.roleText)
        private val messageText: TextView = itemView.findViewById(R.id.messageText)

        fun bind(message: ChatMessage) {
            val context = itemView.context
            val isUser = message.role == "user"

            row.gravity = if (isUser) Gravity.END else Gravity.START
            roleText.text = if (isUser) "You" else "Assistant"
            roleText.gravity = if (isUser) Gravity.END else Gravity.START

            messageText.text = message.content
            messageText.background = ContextCompat.getDrawable(
                context,
                if (isUser) R.drawable.bg_message_user else R.drawable.bg_message_assistant
            )
            messageText.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isUser) R.color.message_user_text else R.color.message_text
                )
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    fun submitList(newMessages: List<ChatMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }
}
