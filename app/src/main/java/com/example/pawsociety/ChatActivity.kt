package com.example.pawsociety

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var etMessage: EditText
    private lateinit var rvMessages: RecyclerView
    private lateinit var chatAdapter: ChatAdapter

    private val chatRepository = ChatRepository()
    private val userRepository = UserRepository()

    private var currentUserId: String = ""
    private var otherUserId: String = ""
    private var otherUserName: String = ""
    private var conversationId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        otherUserId = intent.getStringExtra("USER_ID") ?: ""
        otherUserName = intent.getStringExtra("USER_NAME") ?: "User"

        findViewById<TextView>(R.id.tv_chat_title).text = otherUserName
        findViewById<TextView>(R.id.btn_back).setOnClickListener { finish() }

        etMessage = findViewById(R.id.et_message)
        rvMessages = findViewById(R.id.rv_messages)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        rvMessages.layoutManager = layoutManager

        chatAdapter = ChatAdapter(currentUserId)
        rvMessages.adapter = chatAdapter

        findViewById<TextView>(R.id.btn_send).setOnClickListener {
            sendMessage()
        }

        lifecycleScope.launch {
            val user = userRepository.getCurrentUser()
            if (user == null || otherUserId.isEmpty()) {
                Toast.makeText(this@ChatActivity, "Error loading chat", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            currentUserId = user.uid
            chatAdapter.updateCurrentUserId(currentUserId)

            val result = chatRepository.getOrCreateConversation(otherUserId)
            if (result is Resource.Success) {
                conversationId = result.data
                listenForMessages()
            } else if (result is Resource.Error) {
                Toast.makeText(this@ChatActivity, result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun listenForMessages() {
        lifecycleScope.launch {
            chatRepository.getMessages(conversationId).collect { messages ->
                chatAdapter.submitList(messages)
                if (messages.isNotEmpty()) {
                    rvMessages.scrollToPosition(messages.size - 1)
                }
            }
        }
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty() || conversationId.isEmpty()) return

        etMessage.text.clear()
        lifecycleScope.launch {
            chatRepository.sendMessage(conversationId, currentUserId, text)
        }
    }

    inner class ChatAdapter(private var currentUserId: String) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

        private var messages: List<Message> = emptyList()

        fun submitList(newList: List<Message>) {
            messages = newList
            notifyDataSetChanged()
        }

        fun updateCurrentUserId(id: String) {
            currentUserId = id
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
            return MessageViewHolder(view)
        }

        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
            val message = messages[position]
            holder.bind(message)
        }

        override fun getItemCount() = messages.size

        inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvMessage: TextView = itemView.findViewById(R.id.tv_message_text)
            private val container: LinearLayout = itemView.findViewById(R.id.message_container)

            fun bind(message: Message) {
                tvMessage.text = message.text
                val isMe = message.senderId == currentUserId

                if (isMe) {
                    container.gravity = Gravity.END
                    tvMessage.setBackgroundColor(Color.parseColor("#E3F2FD")) // Light Blue
                } else {
                    container.gravity = Gravity.START
                    tvMessage.setBackgroundColor(Color.parseColor("#E0E0E0")) // Light Gray
                }
            }
        }
    }
}
