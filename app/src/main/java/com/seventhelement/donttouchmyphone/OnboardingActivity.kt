package com.seventhelement.donttouchmyphone

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.seventhelement.donttouchmyphone.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    data class Page(
        val iconRes: Int,
        val title: String,
        val body: String,
        val headerColor: String
    )

    private val pages = listOf(
        Page(
            R.drawable.ic_ob_welcome,
            "Welcome to Don't Touch",
            "Your phone's personal alarm guard.\nSimple to set up, impossible to bypass without your PIN.",
            "#4CAF50"
        ),
        Page(
            R.drawable.ic_ob_arm,
            "Arm the Alarm",
            "Tap START, place your phone on a flat surface, and step away.\n\nThe alarm activates after a 10-second countdown — plenty of time to get comfortable.",
            "#1976D2"
        ),
        Page(
            R.drawable.ic_ob_grace,
            "Pick It Up Safely",
            "If you pick up your phone, a silent 5-second grace period starts.\n\nEnter your PIN before the countdown ends and the alarm never sounds.\n\nTip: press Volume Down 3× quickly to disarm silently — no screen needed.",
            "#00796B"
        ),
        Page(
            R.drawable.ic_ob_alarm,
            "Intruders Have No Chance",
            "Anyone without your PIN triggers a loud alarm and flashing torch.\n\nEvery alarm event is logged with a timestamp. Open Alarm History to see when and how your phone was touched.",
            "#C62828"
        )
    )

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = PagesAdapter()

        TabLayoutMediator(binding.tabDots, binding.viewPager) { _, _ -> }.attach()

        binding.btnSkip.setOnClickListener { finish() }

        binding.btnNext.setOnClickListener {
            if (binding.viewPager.currentItem < pages.lastIndex) {
                binding.viewPager.currentItem++
            } else {
                finish()
            }
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val isLast = position == pages.lastIndex
                binding.btnNext.text = if (isLast) "Get Started" else "Next"
                binding.btnNext.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(Color.parseColor(pages[position].headerColor))
                binding.btnSkip.visibility = if (isLast) View.INVISIBLE else View.VISIBLE
            }
        })

        // Set initial button color
        binding.btnNext.backgroundTintList =
            android.content.res.ColorStateList.valueOf(Color.parseColor(pages[0].headerColor))
    }

    inner class PagesAdapter : RecyclerView.Adapter<PagesAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val headerBg: FrameLayout = view.findViewById(R.id.headerBg)
            val icon: ImageView = view.findViewById(R.id.onboardingIcon)
            val title: TextView = view.findViewById(R.id.onboardingTitle)
            val body: TextView = view.findViewById(R.id.onboardingBody)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_onboarding_page, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val page = pages[position]
            holder.headerBg.setBackgroundColor(Color.parseColor(page.headerColor))
            holder.icon.setImageResource(page.iconRes)
            holder.title.text = page.title
            holder.body.text = page.body
        }

        override fun getItemCount() = pages.size
    }
}
