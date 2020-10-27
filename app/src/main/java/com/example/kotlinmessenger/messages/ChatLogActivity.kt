package com.example.kotlinmessenger.messages

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.kotlinmessenger.models.ChatMessage
import com.example.kotlinmessenger.R
import com.example.kotlinmessenger.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.chat_to_row.view.*
import kotlinx.android.synthetic.main.chat_from_row.view.*

class ChatLogActivity : AppCompatActivity() {

    companion object{
        val TAG = "ChatLog"
    }

    val adapter = GroupAdapter<ViewHolder>()

    //initialize variable so you don't have to rerun Parcelize for every message
    var toUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        //add new texts to list of text messages
        recycler_view_chatlog.adapter = adapter

        //get username from user info
        toUser = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        supportActionBar?.title = toUser?.username

        listenForMessages()

        send_button_chatlog.setOnClickListener {
            Log.d(TAG, "Attempting to send message")
            performSendMessage()
        }

    }

    private fun listenForMessages(){
        val fromID = FirebaseAuth.getInstance().uid
        val toID = toUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("/user-messages/$fromID/$toID")

        ref.addChildEventListener(object: ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue((ChatMessage::class.java))

                if (chatMessage != null){
                    //if chat message ID is user ID, add user's chat message
                    if (chatMessage.fromId == FirebaseAuth.getInstance().uid){

                        val currentUser = LatestMessagesActivity.currentUser ?: return

                        adapter.add(ChatFromItem(chatMessage.text, currentUser))
                    }
                    //else it must be other user's chat message
                    else{
                        adapter.add(ChatToItem(chatMessage.text, toUser!!))
                    }

                }

                recycler_view_chatlog.scrollToPosition(adapter.itemCount - 1)

            }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun performSendMessage(){
        //send to chat messages to be stored in messages node in database
        val text = edittext_chat_log.text.toString()

        //fromID = sender's ID
        val fromID = FirebaseAuth.getInstance().uid

        //toID = get other user's ID
        val user = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        val toID = user?.uid

        if (fromID == null) return
        if (toID == null) return

        //push a 2 new nodes into messages (in database) for each new message (one for sender and another for receiver)
        //from reference
        val reference = FirebaseDatabase.getInstance().getReference("/user-messages/$fromID/$toID").push()
        //to reference
        val toReference = FirebaseDatabase.getInstance().getReference("/user-messages/$toID/$fromID").push()

        //current time in milliseconds
        val chatMessage = ChatMessage(reference.key!!, text, fromID, toID, System.currentTimeMillis())

        reference.setValue(chatMessage)
            .addOnSuccessListener {
                Log.d(TAG, "Chat message saved")
                edittext_chat_log.text.clear()
                recycler_view_chatlog.scrollToPosition(adapter.itemCount - 1)
            }

        toReference.setValue(chatMessage)
            .addOnSuccessListener {
                Log.d(TAG, "Chat message received!")
            }

        //keeps track of latest messages to display on latest messages homepage
        val latestMessageReference = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromID/$toID")
        latestMessageReference.setValue(chatMessage)

        val latestMessageToReference = FirebaseDatabase.getInstance().getReference("/latest-messages/$toID/$fromID")
        latestMessageToReference.setValue(chatMessage)
    }

}

//class for chat objects from left side (their messages)
class ChatFromItem(val text: String, val user: User): Item<ViewHolder>(){
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.textView_from_row.text = text

        //load profile picture
        val uri = user.profileImageUrl
        val targetImageView = viewHolder.itemView.imageView_chat_from_row
        Picasso.get().load(uri).into(targetImageView)
    }
    override fun getLayout(): Int {
        return R.layout.chat_from_row
    }
}

//class for chat objects from right side (your messages + profile picture)
class ChatToItem(val text: String, val user: User): Item<ViewHolder>(){
    //load messages
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.textView_to_row.text = text

        //load profile picture
        val uri = user.profileImageUrl
        val targetImageView = viewHolder.itemView.imageView_chat_to_row
        Picasso.get().load(uri).into(targetImageView)
    }

    override fun getLayout(): Int {
        return R.layout.chat_to_row
    }
}