package com.example.weather2.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.weather2.data.RetrofitInstance
import com.example.weather2.data.WeatherRepository
import com.example.weather2.data.room.AppDatabase
import com.example.weather2.data.room.WeatherDao
import com.example.weather2.databinding.ActivityMainBinding
import com.example.weather2.domain.GetWeatherUseCase
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: WeatherViewModel
    private val adapter = WeatherAdapter()
    private lateinit var weatherDao: WeatherDao

    private val locations = listOf("London", "New York", "Paris", "Moscow", "Bishkek")
    private var currentLocationIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        supportActionBar?.hide()
        setContentView(binding.root)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "weather-database"
        ).build()

        weatherDao = db.weatherDao()

        val api = RetrofitInstance.api
        val repository = WeatherRepository(api, weatherDao)
        val getWeatherUseCase = GetWeatherUseCase(repository)
        viewModel = ViewModelProvider(this, WeatherViewModelFactory(getWeatherUseCase))[WeatherViewModel::class.java]

        binding.recyclerView.adapter = adapter

        val days = 7
        val aqi = "no"
        val alerts = "no"

        getWeatherForLocation(locations[currentLocationIndex], days, aqi, alerts)

        binding.arrow.setOnClickListener {
            currentLocationIndex = (currentLocationIndex + 1) % locations.size
            getWeatherForLocation(locations[currentLocationIndex], days, aqi, alerts)
        }
    }

    private fun getWeatherForLocation(location: String, days: Int, aqi: String, alerts: String) {
        viewModel.getWeather(location, days, aqi, alerts)

        viewModel.weatherResponse.observe(this) { response ->
            val hoursWithDays = response.forecast.forecastday.flatMap { day ->
                day.hour.map { hour -> Pair(hour, day) }
            }
            val sortedHoursWithDays = hoursWithDays.sortedBy { it.first.time_epoch }
            adapter.submitList(sortedHoursWithDays)

            val currentWeather = response.current
            binding.tvToday.text = "Today"
            binding.tvGradus.text = "${currentWeather.temp_c}°C"
            binding.tvWeather.text = currentWeather.condition.text
            binding.tvCity.text = response.location.name

            val iconUrl = "https:${currentWeather.condition.icon}"

            Picasso.get()
                .load(iconUrl)
                .into(binding.ivIcon)
        }
    }
}
