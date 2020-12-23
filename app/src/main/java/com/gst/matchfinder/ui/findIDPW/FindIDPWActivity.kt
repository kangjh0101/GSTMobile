package com.gst.matchfinder.ui.findIDPW

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.tabs.TabLayoutMediator
import com.gst.matchfinder.R
import kotlinx.android.synthetic.main.activity_find_i_d_p_w.*

class FindIDPWActivity : AppCompatActivity(){

    private lateinit var tab_names: Array<String>

    private var findIDPWPageChangeCallback = object : OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
//Toast.makeText(this@FindIDPWActivity, "Selected position 2222: $position", Toast.LENGTH_SHORT).show()
            //super.onPageSelected(position)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Switch to AppTheme for displaying the activity
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_i_d_p_w)

        this.getSupportActionBar()?.hide()

        tab_names = resources.getStringArray(R.array.find_idpw_tab_names)

        //TODO:3 Wire DoppelgangerAdapter with ViewPager2 here
        val findIDPWPagerAdapter = FindIDPWPagerAdapter(this, tab_names.size)
        find_idpw_view_pager.adapter = findIDPWPagerAdapter

        //TODO:5 Register page change callback here
        find_idpw_view_pager.registerOnPageChangeCallback(findIDPWPageChangeCallback)

        // A mediator to link a TabLayout with a ViewPager2. The mediator will synchronize
        // the ViewPager2's position with the selected tab when a tab is selected, and the
        // TabLayout's scroll position when the user drags the ViewPager2.
        TabLayoutMediator(find_idpw_tabs, find_idpw_view_pager) { tab, position ->
            //To get the first name of doppelganger celebrities
            tab.text = tab_names[position]
        }.attach()

    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        //TODO:6 Unregister page change callback here
        find_idpw_view_pager.unregisterOnPageChangeCallback(findIDPWPageChangeCallback)
    }
}

//Toast.makeText(this@FindIDPWActivity, "Selected position: $position", Toast.LENGTH_SHORT).show()