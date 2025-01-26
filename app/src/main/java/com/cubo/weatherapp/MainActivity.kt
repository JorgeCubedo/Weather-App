package com.cubo.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cubo.weatherapp.models.WeatherResponse
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.round

class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"
    lateinit var txtTemp: TextView
    lateinit var txtFeelsLike: TextView
    lateinit var edtCity: EditText
    lateinit var txtTempMax: TextView
    lateinit var toolbar: Toolbar
    lateinit var imgView: ImageView
    lateinit var progressBar: ProgressBar
    lateinit var txtSunrise: TextView
    lateinit var txtSunset: TextView
    lateinit var progressInfo: ProgressBar
    lateinit var linearLayout: LinearLayout
    lateinit var txtWeatherCondition: TextView

    lateinit var weatherRes: WeatherResponse
    var isCelsius: Boolean = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        initUI()
        if (!checkConnection(applicationContext))
            Toast.makeText(applicationContext, getString(R.string.str_no_network_warning), Toast.LENGTH_LONG).show()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //Se busca la ultima ciudad buscada exitosamente en la app antes de cerrarla
        //Look for the last city used successfully in the app before close it
        val savedCity = getCity()
        if (savedCity.isNotEmpty()) {
            edtCity.setText(savedCity)
            loadApiWeather(savedCity)
        }


        setSupportActionBar(toolbar)
        //Se busca la ciudad usando el boton buscar en el teclado
        //search the city using the Search button in the keyboard
        edtCity.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (checkConnection(applicationContext)) {
                    val query = edtCity.text.toString().trim()
                    if (query.isNotEmpty()) {
                        loadApiWeather(query)
                    }
                } else {
                    Toast.makeText(applicationContext, getString(R.string.str_no_network_warning), Toast.LENGTH_LONG).show()
                }

                true
            }
            else
                false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.change_temp -> {
                isCelsius = !isCelsius
                Log.i(TAG, "IsCelsius: $isCelsius")

                if (weatherRes != null) {
                    loadInfo(weatherRes, isCelsius)
                }
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    //Se consume el api para obtener la informacion
    //Consumes the api to get the information
    private fun loadApiWeather(query: String) {
        progressInfo.visibility = View.VISIBLE
        linearLayout.visibility = View.GONE
        val apikey = "32043d4537be983f458bed8ab729426e"
        val api = RetrofitClient.instance.create(ApiService::class.java)
        api.getWeather(query, apikey).enqueue(object : retrofit2.Callback<WeatherResponse>{
            override fun onResponse(
                call: Call<WeatherResponse>,
                response: Response<WeatherResponse>
            ) {
                val res = response.body()

                if (res != null) {
                    //Toast.makeText(applicationContext, "The weather is: " + res.main.temp, Toast.LENGTH_LONG).show()
                    weatherRes = res
                    loadInfo(weatherRes, isCelsius)
                    saveCity(query)
                }

                progressInfo.visibility = View.GONE
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                t.printStackTrace()
                Toast.makeText(applicationContext, getString(R.string.str_network_error), Toast.LENGTH_LONG).show()
            }
        })
    }

    //Inicializa los componentes de la interfaz de usuario
    //Initialize the UI components
    private fun initUI() {
        edtCity = findViewById(R.id.edit_city)
        txtTemp = findViewById(R.id.text_weather)
        txtFeelsLike = findViewById(R.id.text_feels_like)
        txtTempMax = findViewById(R.id.text_temp_max_min)
        toolbar = findViewById(R.id.toolbar)
        imgView = findViewById(R.id.img_icon)
        progressBar = findViewById(R.id.progress_icon)
        txtSunrise = findViewById(R.id.text_sunrise)
        txtSunset = findViewById(R.id.text_sunset)
        progressInfo = findViewById(R.id.progress_info)
        linearLayout = findViewById(R.id.layout_weather_info)
        txtWeatherCondition = findViewById(R.id.text_weather_condition)
        linearLayout.visibility = View.GONE
        progressInfo.visibility = View.GONE
    }

    //Carga la informacion del api y las escribe en los textviews, validando si es celsius o fahrenheit
    //Load the api information and writes int the textviews, checking if ii celsius or fahrenheit.
    private fun loadInfo(weatherResponse: WeatherResponse, isCelsius: Boolean) {
        var textWeather = getString(R.string.str_weather) + ": " + convertKelvin(weatherResponse.main.temp, isCelsius)
        var textFeelsLike = getString(R.string.feels_like) + ": " + convertKelvin(weatherResponse.main.feels_like, isCelsius)
        var textTempMax = "${getString(R.string.str_temp_max)}:  ${convertKelvin(weatherResponse.main.temp_max, isCelsius)}"
        var textTempMin = "${getString(R.string.str_temp_min)}: ${convertKelvin(weatherResponse.main.temp_min, isCelsius)}"

        if (isCelsius) {
            textWeather += "°C"
            textFeelsLike += "°C"
            textTempMax += "°C"
            textTempMin += "°C"
        }
        else {
            textWeather += "°F"
            textFeelsLike += "°F"
            textTempMax += "°F"
            textTempMin += "°F"
        }
        val textTemp = "$textTempMax - $textTempMin"
        txtTemp.text = textWeather
        txtFeelsLike.text = textFeelsLike
        txtTempMax.text = textTemp

        loadSunriseAndSunset(weatherResponse)

        if (weatherResponse.weather.isNotEmpty())
            loadIcon(weatherResponse.weather.first().icon, weatherResponse.weather.first().description)

        txtWeatherCondition.text = getCondition(weatherResponse.weather.first().description)
        linearLayout.visibility = View.VISIBLE
    }

    //Convert the timezone from seconds to UTC and writes the time of the local sunrise and sunset
    //Convierte la zona horaria de segundos a UTC y escribe la hora de amanecer y atardecer local
    private fun loadSunriseAndSunset(weatherResponse: WeatherResponse) {
        val timezone = weatherResponse.timezone

        val zoneId = ZoneId.ofOffset("UTC", java.time.ZoneOffset.ofTotalSeconds(timezone))

        val sunrise = Instant.ofEpochSecond(weatherResponse.sys.sunrise).atZone(zoneId)
        val sunset = Instant.ofEpochSecond(weatherResponse.sys.sunset).atZone(zoneId)

        Log.d(TAG, "Sunrise: ${sunrise.toLocalTime()}")
        Log.d(TAG, "Sunset: ${sunset.toLocalTime()}")
        val sunriseTime = sunrise.toLocalTime()
        val sunsetTime = sunset.toLocalTime()

        val strSunrise = "${getString(R.string.str_sunrise)}: $sunriseTime"
        val strSunset = "${getString(R.string.str_sunset)}: $sunsetTime"
        txtSunrise.text = strSunrise
        txtSunset.text = strSunset
    }
    //Convierte los grados kelvin a celsius o fahrenheit dependiendo del segundo parametro.
    //Convert the kelvin grades to celsius or fahrenheit depending by the second param
    private fun convertKelvin(kelvin: Double, isCelsius: Boolean): Double {
        if (isCelsius) {
            return round(((kelvin - 273.15)*100)/100)
        }
        return round((((kelvin - 273.15) * 9 / 5 + 32)*100)/100)
    }

    //Se cargar el icono mediante la informacion del api y la biblioteca Picasso
    //Loads the icon using the api's information and the Picasso library
    private fun loadIcon(icon: String, condition: String) {
        imgView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        val url = "${RetrofitClient.BASE_URL_ICON}img/wn/${icon}@2x.png"
        Log.i(TAG, url)
        Picasso.get()
            .load(url)
            .into(imgView)

        imgView.contentDescription = getCondition(condition)
        progressBar.visibility = View.GONE
        imgView.visibility = View.VISIBLE
    }

    private fun getCondition(condition:String): String {
        var conditions = resources.getStringArray(R.array.str_weather_conditions)

        val index = when(condition.toLowerCase(Locale.ROOT)) {
            "clear sky" -> 0
            "few clouds" -> 1
            "scattered clouds" -> 2
            "broken clouds" -> 3
            "shower rain" -> 4
            "rain" -> 5
            "thunderstorm" -> 6
            "snow" -> 7
            "mist" -> 8
            else -> 9
        }

        return if (index in conditions.indices) {
            conditions[index]
        }
        else {
            ""
        }
    }
    //Guarda la ultima ciudad consultada exitosamente usando el shared preference
    //Saves the latest city successfully used, using the shared preference
    private fun saveCity(city: String) {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("CITY", city)
        editor.apply()
    }

    //Obtiene la ciudad desde un shared preference
    //Get the city from the shared preference
    private fun getCity(): String {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("CITY", "") ?: ""
    }

    private fun checkConnection(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities((network))
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}