package com.example.toiletsbrno

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.maps.android.PolyUtil
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.PermissionListener
import com.utsman.samplegooglemapsdirection.kotlin.model.DirectionResponses
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet_dialog.*
import kotlinx.android.synthetic.main.bottom_sheet_dialog.view.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.content_review.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Files.size
import java.util.*
import kotlinx.android.synthetic.main.bottom_sheet_dialog.view.rBar as rBar1


class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
    PermissionListener {


    companion object {
        const val REQUEST_CHECK_SETTINGS = 43
    }
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var googleMap: GoogleMap? = null // musim priradit null
    private  lateinit var fireBaseServer: DatabaseServer
    var toilets = mutableListOf<Toilet>()
    private lateinit var chooserToilet: LatLng
    private  var currentLocation: LatLng = LatLng(0.0, 0.0)
    private  var routeLocation: LatLng = LatLng(0.0, 0.0)
    private val TAG = "Monitor Location"
    private var mLm: LocationManager? = null
    private var idToilet: String = "0"

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val mapFragment: SupportMapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment // nejde zmenit final pridavat do listu ano var premenna

        mapFragment.getMapAsync(this)


        fusedLocationProviderClient = FusedLocationProviderClient(this)
        fireBaseServer = DatabaseServer(this)


        val button = findViewById<ImageButton>(R.id.buttonRoute)
        button.setOnClickListener {
            googleMap?.clear()
            updateMarks()
            linear.visibility = View.INVISIBLE
        }


        getLastLocation()

    }

    override fun onMapReady(p0: GoogleMap?) {

        googleMap = p0
        googleMap?.setOnMarkerClickListener(this)

      getDataToMap()

        if (isPermissionGiven()){
            googleMap?.isMyLocationEnabled = true
            googleMap?.uiSettings?.isMyLocationButtonEnabled = true
            googleMap?.uiSettings?.isZoomControlsEnabled = true
            getCurrentLocation()
        } else {
            givePermission()
        }

    }

    fun getDataToMap(){

        fireBaseServer.getPlacesReference().addValueEventListener(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                for (placeDS in p0.children){
                    var place: Toilet? = placeDS.getValue(Toilet::class.java)

                    place?.let {

                        val toi = Toilet(placeDS.key)

                        toi.placeId = it.placeId
                        toi.address = it.address
                        toi.day_time = it.day_time
                        toi.latitude = it.latitude
                        toi.longitude = it.longitude
                        toi.payment = it.payment
                        toi.note = it.note
                        toilets.add(toi)
                        it.placeId = placeDS.key
                        val markerOptions: MarkerOptions = MarkerOptions()
                        markerOptions.position(LatLng(it.latitude!!, it.longitude!!))
                        markerOptions.icon(bitmapDescriptorFromVector(getApplicationContext(),R.drawable.toilet))
                        //  googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(brno, zoomLevel))
                        var marker = googleMap?.addMarker(markerOptions)

                    }

                }

            }
        })

    }

    private fun bitmapDescriptorFromVector(
        context: Context,
        vectorResId: Int
    ): BitmapDescriptor? {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        vectorDrawable!!.setBounds(
            0,
            0,
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight
        )
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }


    override fun onMarkerClick(p0: Marker?): Boolean {
        onStartListening()
        if (currentLocation.latitude == 0.0) {
            getCurrentLocation()

        }
        val view = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(view)


        for(i in toilets) {
            if(i.latitude?.toDouble() == p0?.position?.latitude ){

                var reviewList = mutableListOf<String>()
                val dbVal = FirebaseDatabase.getInstance().getReference("review")
                dbVal.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {

                        val post = dataSnapshot.child(i.name.toString()).getValue()
                        Log.v("k", "${post.toString()}")
                        if(post.toString() != "null") {
                            reviewList.clear()
                            for (element in post.toString()) {
                                Log.v("k4", element.toString())
                                if (element.toString() == "1" || element.toString() == "2" ||  element.toString() == "3"|| element.toString() == "4" || element.toString() == "5"  ) {
                                    Log.v("celkove1", element.toString())
                                    reviewList.add(element.toString())

                                }
                            }

                            view.numbers.setText("(" + reviewList.size.toString()+ ")")
                            var sum = 0.0
                            for (value in reviewList){
                                sum += value.toDouble()

                            }

                            var average = sum/reviewList.size
                            view.rBar.rating = average.toFloat()

                        } else{
                            reviewList.clear()
                            view.numbers.setText("(" + reviewList.size.toString()+ ")")
                            view.rBar.rating = 0.0.toFloat()
                        }


                    }

                    override fun onCancelled(databaseError: DatabaseError) {

                    }
                })


                view.note.text = i.note
                view.address.text = i.address
                view.open.text = i.day_time
                view.paymant.text = i.payment

                idToilet = i.name.toString()
                chooserToilet = LatLng(i.latitude!!.toDouble(), i.longitude!!.toDouble())
                view.distance.text = i.latitude?.toDouble()?.let {

                    disttance(i.latitude?.toDouble(),i.longitude?.toDouble(),currentLocation.latitude, currentLocation.longitude)
                }
            }

        }




        view.buttonNavigation.setOnClickListener(object: View.OnClickListener {
            override fun onClick(view: View): Unit {

                val gmmIntentUri = Uri.parse("google.navigation:q="+chooserToilet.latitude+","+chooserToilet.longitude + "&mode=w")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                startActivity(mapIntent)


            }
        })

        view.buttonStreetView.setOnClickListener(object: View.OnClickListener {
            override fun onClick(view: View): Unit {

                val intent = Intent(this@MainActivity,StreetViewActivity::class.java);
                intent.putExtra("latToilet", chooserToilet.latitude.toString() )
                intent.putExtra("lngToilet", chooserToilet.longitude.toString() )
                startActivity(intent);

            }
        })

        view.buttonRoute.setOnClickListener(object: View.OnClickListener {
            override fun onClick(view: View): Unit {

                if (currentLocation.latitude != 0.0) {

                    val fromFKIP =
                        currentLocation.latitude.toString() + "," + currentLocation.longitude.toString()
                    routeLocation = LatLng(chooserToilet.latitude!!.toDouble(), chooserToilet.longitude!!.toDouble())
                    val toMonas =
                        routeLocation.latitude.toString() + "," + routeLocation.longitude.toString()

                    val apiServices = RetrofitClient.apiServices(this@MainActivity)
                    apiServices.getDirection(
                        "walking",
                        fromFKIP,
                        toMonas,
                        "AIzaSyC3RwBupXyFdul5XtIAWjDsF9f8ogyLam4"
                    )
                        .enqueue(object : Callback<DirectionResponses> {
                            override fun onResponse(
                                call: Call<DirectionResponses>,
                                response: Response<DirectionResponses>
                            ) {
                                drawPolyline(response)
                                Log.d("", response.message())
                            }

                            override fun onFailure(call: Call<DirectionResponses>, t: Throwable) {
                                Log.e("", t.localizedMessage)
                            }
                        })

                    val cameraPosition = CameraPosition.Builder()
                        .target(LatLng(currentLocation!!.latitude, currentLocation!!.longitude))
                        .zoom(16f)
                        .build()

                    googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                    linear.visibility = View.VISIBLE
                    textDistance.setText(
                        disttance(
                            chooserToilet.latitude?.toDouble(),
                            chooserToilet.longitude?.toDouble(),
                            currentLocation.latitude,
                            currentLocation.longitude
                        )
                    )
                    dialog.dismiss()

                } else {
                    getToast()
                }

            }
        })

    view.buttonReview.setOnClickListener(object: View.OnClickListener {
        override fun onClick(view: View): Unit {


            val intent = Intent(this@MainActivity,RatingActivity::class.java);
            intent.putExtra("review", idToilet)

            startActivity(intent);
        }
    })



    dialog.show()
        return true
    }


    fun updateMarks(){
        for(i in toilets) {
            val markerOptions: MarkerOptions = MarkerOptions()
            markerOptions.position(LatLng(i.latitude!!, i.longitude!!))
            markerOptions.icon(
                bitmapDescriptorFromVector(
                    getApplicationContext(),
                    R.drawable.toilet
                )
            )
            googleMap?.addMarker(markerOptions)

        } }


    fun getToast(){

        Toast.makeText(this, "No current location found.", Toast.LENGTH_LONG).show()
    }

fun updateRoute(){

    val fromFKIP = currentLocation.latitude.toString() + "," + currentLocation.longitude.toString()
    val toMonas = routeLocation.latitude.toString() + "," + routeLocation.longitude.toString()

    val apiServices = RetrofitClient.apiServices(this@MainActivity)
    apiServices.getDirection("walking",fromFKIP, toMonas, "AIzaSyC3RwBupXyFdul5XtIAWjDsF9f8ogyLam4")
        .enqueue(object : Callback<DirectionResponses> {
            override fun onResponse(call: Call<DirectionResponses>, response: Response<DirectionResponses>) {
                drawPolyline(response)
                Log.d("bisa dong oke", response.message())
            }

            override fun onFailure(call: Call<DirectionResponses>, t: Throwable) {
                Log.e("anjir error", t.localizedMessage)
            }
        })

}


   fun disttance(lat1: Double? = 0.0, lng1: Double? = 0.0, lat2: Double? = 0.0, lng2: Double? = 0.0): String {

   var loc1 = Location("")
            loc1.setLatitude(lat1!!)
            loc1.setLongitude(lng1!!)
       var loc2 = Location("")
            loc2.setLatitude(lat2!!)
            loc2.setLongitude(lng2!!);
            var distanceInMeters = loc1.distanceTo(loc2)

       if(distanceInMeters < 1000){
           val decimal = BigDecimal(distanceInMeters.toDouble()).setScale(0, RoundingMode.HALF_EVEN)

           var meters = decimal.toString()
           meters = meters.removePrefix("00")
           meters = meters.removePrefix("0")
           return meters + " m"
       } else{
           distanceInMeters = distanceInMeters/1000
           val decimal = BigDecimal(distanceInMeters.toDouble()).setScale(0, RoundingMode.HALF_EVEN)
           val decimal2 = BigDecimal(distanceInMeters.toDouble()).setScale(3, RoundingMode.HALF_EVEN)
           var meters: String = decimal2.toString()

           meters =  meters.substring(meters.length-3,meters.length)

           meters = meters.removePrefix("00")
           meters = meters.removePrefix("0")

           return decimal.toString() + " km " + meters + " m"

       }
   }


    //DRAW ROUTE
    private fun drawPolyline(response: Response<DirectionResponses>) {
        googleMap?.clear()
        updateMarks()
        val shape = response.body()?.routes?.get(0)?.overviewPolyline?.points
        val polyline = PolylineOptions()
            .addAll(PolyUtil.decode(shape))
            .width(14f)
            .color(Color.BLUE)


        googleMap?.addPolyline(polyline)
        

    }

    private interface ApiServices {
        @GET("maps/api/directions/json")
        fun getDirection( @Query("mode") mode: String,
                            @Query("origin") origin: String,
                         @Query("destination") destination: String,

                         @Query("key") apiKey: String): Call<DirectionResponses>
    }

    private object RetrofitClient {
        fun apiServices(context: Context): ApiServices {
            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(context.resources.getString(R.string.base_url))
                .build()

            return retrofit.create<ApiServices>(ApiServices::class.java)
        }
    }

// ZISTOVANIE AKTUALNEJ POLOHY
    private fun isPermissionGiven(): Boolean{
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun givePermission() {
        Dexter.withActivity(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(this)
            .check()
    }

    override fun onPermissionGranted(response: PermissionGrantedResponse?) {
        googleMap?.isMyLocationEnabled = true
        getCurrentLocation()

    }

    override fun onPermissionRationaleShouldBeShown(
        permission: com.karumi.dexter.listener.PermissionRequest?,
        token: PermissionToken?
    ) {
        token!!.continuePermissionRequest()
    }


    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
        Toast.makeText(this, "Permission required for showing location", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun getCurrentLocation() {

        val locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = (2*1000).toLong()
        locationRequest.fastestInterval = 2000

        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        val locationSettingsRequest = builder.build()

        val result = LocationServices.getSettingsClient(this).checkLocationSettings(locationSettingsRequest)
        result.addOnCompleteListener { task ->
            try {
                val response = task.getResult(ApiException::class.java)
                if (response!!.locationSettingsStates.isLocationPresent){
                    getLastLocation()
                }
            } catch (exception: ApiException) {
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        val resolvable = exception as ResolvableApiException
                        resolvable.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)

                    } catch (e: IntentSender.SendIntentException) {
                    } catch (e: ClassCastException) {
                    }

                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> { }
                }
            }
        }
    }

    private fun getLastLocation() {
        fusedLocationProviderClient.lastLocation
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful && task.result != null) {
                    val mLastLocation = task.result

                    var address = "No known address"

                    val gcd = Geocoder(this, Locale.getDefault())
                    val addresses: List<Address>
                    try {
                        addresses = gcd.getFromLocation(mLastLocation!!.latitude, mLastLocation.longitude, 1)
                        if (addresses.isNotEmpty()) {
                            address = addresses[0].getAddressLine(0)

                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }


                    currentLocation = LatLng(mLastLocation!!.latitude, mLastLocation!!.longitude)
                    val cameraPosition = CameraPosition.Builder()
                        .target(LatLng(mLastLocation!!.latitude, mLastLocation!!.longitude))
                        .zoom(16f)
                        .build()
                    googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                    googleMap?.isMyLocationEnabled = true

                } else {

                    Toast.makeText(this, "No current location found. This application requires an internet connection and access to the current location", Toast.LENGTH_LONG).show()


                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> {
                if (resultCode == Activity.RESULT_OK) {
                    getCurrentLocation()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    // LOCATION LISTENER
    fun setLocationInfo(location: Location) { //Extract information from location that we pass from location listener

        currentLocation = LatLng(location.latitude, location.longitude)
        if(linear.isVisible){
            textDistance.setText(disttance(chooserToilet.latitude?.toDouble(),chooserToilet.longitude?.toDouble(),currentLocation.latitude, currentLocation.longitude))
            updateRoute()
        }
    }


    private val mNetworkListener: LocationListener? = object : LocationListener {
        override fun onLocationChanged(location: Location) { //TODO 5 : Call text binding method and passing location information into its
            setLocationInfo(location)
            Log.d(TAG, "Monitor Location - Location Changed")
        }

        override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {
            Log.d(TAG, "Monitor Location - Status Change$s")
        }

        override fun onProviderEnabled(s: String) {
            Log.d(TAG, "Monitor Location - Provider Enabled$s")
        }

        override fun onProviderDisabled(s: String) {
            Log.d(TAG, "Monitor Location - Provider Disable$s")
        }
    }

    @SuppressLint("MissingPermission")
    private fun onStartListening() { //init location manager.
        mLm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        /*Log.d(TAG, "onStartListening: started")
        //binding listener to manager with network provider.
        mLm!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10, 15f, mNetworkListener)*/

        val isGPSEnabled: Boolean = mLm!!.isProviderEnabled(LocationManager.GPS_PROVIDER)


        val isNetworkEnabled: Boolean = mLm!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if(isGPSEnabled){

            mLm!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10, 15f, mNetworkListener)
           // googleMap?.isMyLocationEnabled = true
        } else{

            mLm!!.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10, 15f, mNetworkListener)
            //googleMap?.isMyLocationEnabled = true
        }
    }



    private fun doStopListening() {
        if (mNetworkListener != null) {
            mLm!!.removeUpdates(mNetworkListener)
        }
    }



}





