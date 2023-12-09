package edu.training.droidbountyhunterkotlin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import edu.training.droidbountyhunterkotlin.models.Fugitivo

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private var fugitivo : Fugitivo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        fugitivo = intent.getParcelableExtra("fugitivo")

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        title = fugitivo!!.name
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val position: LatLng
        if (fugitivo!!.latitude == 0.0 && fugitivo!!.longitude == 0.0)
        {
           position = LatLng(-34.0, 151.0)
        }
        else
        {
            position = LatLng(fugitivo!!.latitude, fugitivo!!.longitude)
        }

        mMap!!.addMarker(MarkerOptions().position(position).title(fugitivo!!.name))
        mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 12f))
    }
}