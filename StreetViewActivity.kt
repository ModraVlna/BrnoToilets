package com.example.toiletsbrno

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback
import com.google.android.gms.maps.StreetViewPanorama
import com.google.android.gms.maps.StreetViewPanorama.OnStreetViewPanoramaChangeListener
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.StreetViewPanoramaCamera
import com.google.android.gms.maps.model.StreetViewPanoramaOrientation
import com.google.android.gms.maps.model.StreetViewSource
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_street_view.*
import kotlinx.android.synthetic.main.content_street_view.*


class StreetViewActivity : AppCompatActivity(), OnStreetViewPanoramaReadyCallback {


    private var mStreetViewPanorama: StreetViewPanorama? = null
    private var secondLocation = false
    private var latToilet: Double? = 0.0
    private var lngToilet: Double? = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_street_view)
        setSupportActionBar(toolbar)
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true);
        latToilet = intent.getStringExtra("latToilet").toDouble()
        lngToilet= intent.getStringExtra("lngToilet").toDouble()


       var streetView = supportFragmentManager.findFragmentById(R.id.googleMapStreetView) as SupportStreetViewPanoramaFragment?
       streetView!!.getStreetViewPanoramaAsync(this)
        this.secondLocation = !secondLocation
        onStreetViewPanoramaReady(mStreetViewPanorama);

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onStreetViewPanoramaReady(p0: StreetViewPanorama?) {
        mStreetViewPanorama?.setPosition(LatLng(latToilet!!, lngToilet!!))
        p0?.setPosition(LatLng(latToilet!!, lngToilet!!))
        mStreetViewPanorama = p0

        p0?.setStreetNamesEnabled(true)
        p0?.setPanningGesturesEnabled(true)
        p0?.setZoomGesturesEnabled(true)
        p0?.setUserNavigationEnabled(true)
        p0?.animateTo(
            StreetViewPanoramaCamera.Builder().orientation(StreetViewPanoramaOrientation(20F, 20F))
                .zoom(p0?.getPanoramaCamera().zoom)
                .build(), 2000
        )

        p0?.setOnStreetViewPanoramaChangeListener(panoramaChangeListener)

    }

    private val panoramaChangeListener =
        OnStreetViewPanoramaChangeListener { streetViewPanoramaLocation ->

        }

}

