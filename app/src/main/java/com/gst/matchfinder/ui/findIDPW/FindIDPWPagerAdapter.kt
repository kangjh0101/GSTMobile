package com.gst.matchfinder.ui.findIDPW

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class FindIDPWPagerAdapter(activity: AppCompatActivity, private val itemsCount: Int): FragmentStateAdapter(activity) {

    override fun createFragment(position: Int): Fragment {
        return FindIDPWFragment.getInstance(position)
//Log.v("tab_action", "when is this called?")
    }

    override fun getItemCount(): Int{
        return itemsCount
    }
}