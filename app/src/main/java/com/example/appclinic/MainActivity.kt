package com.example.appclinic

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.appclinic.databinding.ActivityMainBinding
import com.example.appclinic.ui.ViewPagerAdapter
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "🔒 Expedientes (AES)"
                1 -> "✍️ Recetas (Ed25519)"
                2 -> "🛡️ KeyStore Nativo"
                else -> "Tab ${position + 1}"
            }
        }.attach()
    }
}
