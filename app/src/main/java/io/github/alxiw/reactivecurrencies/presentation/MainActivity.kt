package io.github.alxiw.reactivecurrencies.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.alxiw.reactivecurrencies.R
import io.github.alxiw.reactivecurrencies.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        savedInstanceState ?: supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_container, CurrenciesFragment.newInstance())
            .commit()
    }
}
