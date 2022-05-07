package com.example.hypersine

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hypersine.databinding.ActivityMainBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.Gson
import com.google.maps.android.SphericalUtil
import com.microdrones.technical_test.Main
import com.microdrones.technical_test.data.models.Config
import com.microdrones.technical_test.data.models.Drones
import com.microdrones.technical_test.data.models.Mission
import com.microdrones.technical_test.data.models.Point
import java.io.*
import java.util.*
import java.util.stream.Collectors

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMainBinding

    private val configs = ArrayList<Config>()
    private val drones = ArrayList<Drones>()
    private val missions = ArrayList<Mission>()
    private val markers = ArrayList<Marker>()
    private val endedFlying = ArrayList<Boolean>()

    private var startTime = 0L
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadData()
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        addPaths()
        updateMarkers()
    }

    private fun loadData() {
        val configFiles = assets.list("configurations")
        val droneFiles = assets.list("drones")
        val missionFiles = assets.list("missions")
        val gson = Gson()
        for (mission in missionFiles!!){
            val fileInString: String =
                applicationContext.assets.open("missions/"+mission).bufferedReader().use { it.readText() }
                missions.add(gson.fromJson(fileInString, Mission::class.java))
        }

        for (config in configFiles!!){
            val fileInString: String =
                applicationContext.assets.open("configurations/"+config).bufferedReader().use { it.readText() }
            configs.add(gson.fromJson(fileInString, Config::class.java))
        }

        for (drone in droneFiles!!){
            val fileInString: String =
                applicationContext.assets.open("drones/"+drone).bufferedReader().use { it.readText() }
            drones.add(gson.fromJson(fileInString, Drones::class.java))
        }
    }

    private fun updateMarkers() {
        val elapsed = System.currentTimeMillis() / 1000 - startTime
        markers.forEachIndexed { index, marker ->
            if (endedFlying[index]) {
                return@forEachIndexed
            }

            val energy = configs[index].energy
            val totalEnergyCapacity = energy.capacity * energy.numberOfBatteries

            val mission = missions[index]
            val points = mission.points
            if (points!![0].endTime > elapsed) {
                if ((points[0].energy + mission.energyReq) > totalEnergyCapacity) {
                    updateMarkerState(marker, "landed halfway", index)
                }
                return@forEachIndexed
            }

            if (mission.endTime < elapsed || (mission.endTime >= elapsed && points[points.size - 1].endTime < elapsed)) {
                val lastPt = points[points.size - 1]
                marker.apply {
                    position = LatLng(lastPt.latitude, lastPt.longitude)

                    if (mission.endTime < elapsed) {
                        updateMarkerState(this, "flew properly", index)
                    }
                }
            } else {
                var prevPt = points[0]
                var currentPt: Point
                for (i in 1 until points.size) {
                    currentPt = points[i]
                    if (elapsed >= prevPt.endTime && elapsed < currentPt.endTime) {
                        markers[index].position = calcPosOnPolyline(prevPt, currentPt, elapsed)

                        if ((currentPt.energy + mission.energyReq) > totalEnergyCapacity) {
                            updateMarkerState(marker, "landed halfway", index)
                        }

                        return@forEachIndexed
                    } else {
                        prevPt = currentPt
                    }
                }
            }
        }
    }

    private fun updateMarkerState(marker: Marker, snippet: String, index: Int) {
        marker.snippet = snippet
        // required to refresh marker snippet
        marker.showInfoWindow()
        endedFlying[index] = true
        Toast.makeText(this, missions[index].name + " - " + snippet, Toast.LENGTH_LONG).show()
    }

    private fun calcPosOnPolyline(p1: Point, p2: Point, elapsed: Long): LatLng {
        val origin = LatLng(p1.latitude, p1.longitude)
        val destination = LatLng(p2.latitude, p2.longitude)
        return SphericalUtil.interpolate(
            origin,
            destination,
            (elapsed - p1.endTime) / (p2.endTime - p1.endTime)
        )
    }

    private fun addPaths() {
        for (mission in missions) {
            endedFlying.add(false)

            val p0 = mission.points!![0]
            val p0LatLng = LatLng(p0.latitude, p0.longitude)
            val marker = mMap.addMarker(
                MarkerOptions().position(p0LatLng).snippet("flying").title(mission.name)
            )
            marker?.let {
                markers.add(it)
                it.showInfoWindow()
            }
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(p0LatLng, 16f))

            val polylineOpts = PolylineOptions()
            mission.points.forEach {
                polylineOpts.add(LatLng(it.latitude, it.longitude))
            }
            mMap.addPolyline(polylineOpts)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}