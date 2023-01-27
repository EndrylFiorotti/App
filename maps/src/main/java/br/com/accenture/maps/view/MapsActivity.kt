package br.com.accenture.maps.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.StrictMode
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import br.com.accenture.maps.R
import br.com.accenture.maps.databinding.ActivityMapsBinding
import br.com.accenture.maps.modules.mapsModules
import br.com.accenture.maps.viewModel.MapsViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.context.startKoin
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule
import kotlin.random.Random


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var currentLocation: Location

    private val viewModel: MapsViewModel by viewModel()
    private val randomGenerator = Random(System.currentTimeMillis())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST) {
            println(it.name)
        }
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setKoinUp()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }

    private fun setKoinUp() {
        startKoin {
            modules(mapsModules)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = false
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        mMap.setMaxZoomPreference(20f)
        mMap.setMinZoomPreference(17f)
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.custom_map))
        setUpMap()
    }

    @SuppressLint("MissingPermission")
    private fun setUpMap() {

        val task = fusedLocationProviderClient.lastLocation

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
            return
        }
        mMap.isMyLocationEnabled = true

        task.addOnSuccessListener(this) {
            if (it != null) {
                currentLocation = it
                val currentLatLong = LatLng(it.latitude, it.longitude)
                mMap.addMarker(
                    MarkerOptions()
                        .position(currentLatLong)
                        .title("My Position")
                )!!
                    .setIcon(
                        BitmapDescriptorFactory.fromResource(R.drawable.red)
                    )
                mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLong))


                mMap.setOnMapLoadedCallback {
                    val cameraPosition = CameraPosition.builder()
                        .target(currentLatLong)
                        .zoom(18.5f)
                        .bearing(90f)
                        .tilt(70f)
                        .build()

                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                }
                 val handler = Handler()
                handler.postDelayed(object:Runnable{
                    override fun run(){
                            setUpPoke(currentLatLong)
                            handler.postDelayed(this,randomGenerator.nextLong(3000,20000))
                    }
                },0)
            }
        }

    }

    fun setUpPoke(currentLatLong: LatLng) {
        runOnUiThread {
            if (viewModel.pokemonPopulation > 3) {
                viewModel.pokemonPopulation = 0

                mMap.clear()
                mMap.addMarker(
                    MarkerOptions()
                        .position(currentLatLong)
                        .title("My Position")
                )!!
                    .setIcon(
                        BitmapDescriptorFactory.fromResource(R.drawable.red)
                    )
            } else {
                viewModel.pokemonPopulation ++

                mMap.addMarker(
                    MarkerOptions()
                        .position(
                            viewModel.randomNearLocation(
                                currentLatLong,
                                randomGenerator
                            )
                        )
                )!!
                    .setIcon(
                        BitmapDescriptorFactory.fromBitmap(
                            viewModel.bpmConvertor(
                                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/${
                                    viewModel.randomPokemon(
                                        randomGenerator
                                    )
                                }.png"
                            )
                        )
                    )
            }
        }
    }
}