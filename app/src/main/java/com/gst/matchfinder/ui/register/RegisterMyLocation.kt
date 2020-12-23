package com.gst.matchfinder.ui.register

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnSuccessListener
import com.gst.matchfinder.R
import com.gst.matchfinder.data.Constants
import java.util.*

class RegisterMyLocation : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener,
    OnMapReadyCallback,
    /*LocationListener,*/
    GoogleMap.OnMapClickListener,
    GoogleMap.OnMapLongClickListener{

    private lateinit  var mMap: GoogleMap
    protected lateinit var mLastLocation: Location
    private var mCurrLocationMarker: Marker? = null
    private lateinit var mGoogleApiClient: GoogleApiClient // why need this?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_my_location)

        this.getSupportActionBar()?.hide()

        var mapFragment: SupportMapFragment = getSupportFragmentManager().findFragmentById(R.id.register_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API).build()

    }

    override fun onResume() {
        super.onResume()

        var mapFragment: SupportMapFragment = getSupportFragmentManager().findFragmentById(R.id.register_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mGoogleApiClient.connect()
    }

    override fun onPause() {
        super.onPause()

        if(mGoogleApiClient.isConnected){
            mGoogleApiClient.disconnect()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.setOnMapClickListener(this)
        mMap.setOnMapLongClickListener(this)

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(37.63, 126.98)
        //mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 10F))

        val handler = Handler(Looper.getMainLooper())
        handler.post(Runnable {
            Toast.makeText(this@RegisterMyLocation, "선택할 위치를 길게 누르세요", Toast.LENGTH_LONG).show()
        })
    }


    override fun onConnectionFailed(p0: ConnectionResult) {
        Toast.makeText(applicationContext, "GPS를 켜십시요", Toast.LENGTH_LONG).show()
    }

    override fun onConnected(p0: Bundle?) {

        var mFusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        if (/*ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && */
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            }
        }

        mFusedLocationClient.lastLocation.addOnSuccessListener {
            object : OnSuccessListener<Location>{
                override fun onSuccess(p0: Location?) {
                    if(p0 != null){
                        val latlng = LatLng(p0.latitude, p0.longitude)

                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 15F))
                        mLastLocation = p0

                        Toast.makeText(this@RegisterMyLocation, "자신의 위치를 길게 누르세요", Toast.LENGTH_SHORT).show()
                    } else{
                        Toast.makeText(this@RegisterMyLocation, "GPS를 켜십시요", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onMapClick(point: LatLng){
        //Toast.makeText(this@RegisterMyLocation, "자신의 위치를 길게 누르세요", Toast.LENGTH_SHORT).show()
    }

    override fun onMapLongClick(p0: LatLng) {
        mCurrLocationMarker?.remove()
        mCurrLocationMarker = mMap.addMarker(MarkerOptions().position(p0).title("내 위치"))

        val temp = Location(LocationManager.GPS_PROVIDER)
        temp.latitude = p0.latitude
        temp.longitude = p0.longitude

        mLastLocation = temp
        val string_address: String = resolveAddress(mLastLocation)
        //Toast.makeText(this@RegisterMyLocation, string_address, Toast.LENGTH_SHORT).show()
        deliverResultToReceiver(Activity.RESULT_OK, string_address)
    }

    /*override fun onLocationChanged(location: Location) {

        mLastLocation = location
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker!!.remove()
        }
        //Place current location marker
        val latLng = LatLng(location.latitude, location.longitude)
        val markerOptions = MarkerOptions()
        markerOptions.position(latLng)
        markerOptions.title("현재 위치")
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        mCurrLocationMarker = mMap!!.addMarker(markerOptions)

        //move map camera
        mMap!!.moveCamera(CameraUpdateFactory.newLatLng(latLng))
        mMap!!.animateCamera(CameraUpdateFactory.zoomTo(11f))
    }*/

    override fun onConnectionSuspended(p0: Int) {
        Toast.makeText(applicationContext, "GPS 연결이 지연되고 있습니다", Toast.LENGTH_LONG).show()
    }

    private fun resolveAddress(location: Location) : String{

        var return_address: String
        var geocoder = Geocoder(this, Locale.getDefault())

        val list = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        return_address = list[0].getAddressLine(0)

        return return_address
    }

    private fun deliverResultToReceiver(resultCode: Int, message: String){
        val resultIntent = Intent()

        resultIntent.putExtra(Constants.MAP_ADDRESS_RESULT_KEY, message)
        setResult(resultCode, resultIntent)
        finish()
    }

}


/**********************************************
val handler = Handler(Looper.getMainLooper())
handler.post(Runnable {
Toast.makeText(this@RegisterMyLocation, "Map Connected", Toast.LENGTH_SHORT).show()
})
 **********************************************/












