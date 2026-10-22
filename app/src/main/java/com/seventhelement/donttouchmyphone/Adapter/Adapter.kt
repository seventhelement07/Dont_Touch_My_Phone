package com.seventhelement.donttouchmyphone.Adapter

import android.content.Context
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.RecyclerView
import com.seventhelement.data1
import com.seventhelement.donttouchmyphone.R
import java.util.Locale

class Adapter(
    private val list: ArrayList<data1>,
    private val context: Context,
    private val packageName: String,
    private val listener: OnItemSelectedListener,
    private var selectedItem: Int = -1,
    private var f: Boolean = false
) : RecyclerView.Adapter<Adapter.ViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingPosition: Int = -1

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ll: LinearLayoutCompat = itemView.findViewById(R.id.ll)
        val title: TextView = itemView.findViewById(R.id.textView)
        val image: ImageView = itemView.findViewById(R.id.imageView)
        val checkBadge: TextView = itemView.findViewById(R.id.checkBadge)
        val playingLabel: TextView = itemView.findViewById(R.id.playingLabel)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    selectedItem = position
                    notifyDataSetChanged()
                }
            }
        }

        fun bind(current: data1, position: Int) {
            if (selectedItem == -1) f = false

            title.text = current.title.replace('_', ' ').uppercase(Locale.getDefault())
            image.setImageResource(current.image)

            if (position == selectedItem) {
                if (f) {
                    f = false
                    // Initial bind with pre-selected item — highlight but don't replay
                    ll.setBackgroundResource(R.drawable.ll_back2)
                    checkBadge.visibility = View.VISIBLE
                    playingLabel.visibility = View.GONE
                } else if (currentPlayingPosition == position && mediaPlayer?.isPlaying == true) {
                    // Tapping the currently-playing item stops it
                    stopMediaPlayer()
                    ll.setBackgroundResource(R.drawable.ll_back2)
                    checkBadge.visibility = View.VISIBLE
                    playingLabel.visibility = View.GONE
                } else {
                    // Start playing preview
                    stopMediaPlayer()
                    currentPlayingPosition = position
                    listener.onItemSelected(position)
                    val resourceId = context.resources.getIdentifier(current.title, "raw", packageName)
                    if (resourceId != 0) {
                        mediaPlayer = MediaPlayer.create(context, resourceId)
                        mediaPlayer?.setOnCompletionListener {
                            currentPlayingPosition = -1
                            notifyDataSetChanged()
                        }
                        mediaPlayer?.start()
                    }
                    ll.setBackgroundResource(R.drawable.ll_back2)
                    checkBadge.visibility = View.VISIBLE
                    playingLabel.visibility = if (mediaPlayer?.isPlaying == true) View.VISIBLE else View.GONE
                }
            } else {
                ll.setBackgroundResource(R.drawable.ll_back)
                checkBadge.visibility = View.GONE
                playingLabel.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position], position)
    }

    override fun getItemCount(): Int = list.size

    private fun stopMediaPlayer() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        currentPlayingPosition = -1
    }

    fun releaseMediaPlayer() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        currentPlayingPosition = -1
    }

    interface OnItemSelectedListener {
        fun onItemSelected(position: Int)
        fun onItemDeselected()
    }
}
