import android.content.Context
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.seventhelement.data1
import com.seventhelement.donttouchmyphone.R

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
        val title: TextView = itemView.findViewById(R.id.textView)
        val ll: androidx.appcompat.widget.LinearLayoutCompat = itemView.findViewById(R.id.ll)
        val image: ImageView = itemView.findViewById(R.id.imageView)

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
            if (selectedItem == -1) {
                f = false
            }
            title.text = current.title.toUpperCase()
            image.setImageResource(current.image)

            if (position == selectedItem) {
                if (f) {
                    f = false
                } else if (currentPlayingPosition == position && mediaPlayer?.isPlaying == true && currentPlayingPosition != -1) {
                    if(mediaPlayer!=null)
                        stopMediaPlayer()
                } else {
                    if(mediaPlayer!=null)
                    stopMediaPlayer()
                    currentPlayingPosition = position
                    listener.onItemSelected(position)
                    val resourceId = context.resources.getIdentifier(current.title, "raw", packageName)
                    mediaPlayer = MediaPlayer.create(context, resourceId)
                    mediaPlayer?.start()
                }
                itemView.setBackgroundResource(R.drawable.ll_back2)
            } else {
                itemView.setBackgroundResource(R.drawable.ll_back)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val current = list[position]
        holder.bind(current, position)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    private fun stopMediaPlayer() {
        mediaPlayer?.apply {
            if(isPlaying)
            stop()
            release()
        }
        mediaPlayer = null
    }

    fun releaseMediaPlayer() {
        mediaPlayer?.apply {
            release()
        }
        mediaPlayer = null
    }

    interface OnItemSelectedListener {
        fun onItemSelected(position: Int)
        fun onItemDeselected()
    }
}
