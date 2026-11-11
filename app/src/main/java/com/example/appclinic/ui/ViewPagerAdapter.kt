package com.example.appclinic.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ExpedientesFragment()
            1 -> ComprobantesFragment()
            2 -> KeyStoreFragment()
            else -> ExpedientesFragment()
        }
    }
}
