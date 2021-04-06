package com.codesgood.justifiedtextview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.codesgood.justifiedtextview.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvJustifiedParagraph.setText(R.string.lorem_ipsum_extended)
    }
}