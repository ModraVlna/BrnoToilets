package com.example.toiletsbrno

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_main.*


class RatingActivity :  AppCompatActivity() {

    val dbVal = FirebaseDatabase.getInstance().getReference("review")
    var toilets = mutableListOf<String>()

    lateinit var newId: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.rating)
        setSupportActionBar(toolbar)
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)


        val rBar = findViewById<RatingBar>(R.id.rBar)

        newId = intent.getStringExtra("review")
        dbVal.child(newId.toString())

        getData()

        if (rBar != null) {

            val button = findViewById<Button>(R.id.button)
            button?.setOnClickListener {


                val msg = rBar.rating.toString()
                var valueRating = rBar.rating // stores the rating
                rBar.setRating(valueRating)

                toilets.add(valueRating.toInt().toString())
                Log.v("vkladam", toilets.toString())
                dbVal.child(newId.toString()).setValue(toilets)
                finish()
            }

        }

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }


fun getData(){

    dbVal.addValueEventListener(object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            val post = dataSnapshot.child(newId.toString()).getValue()
            //Log.v("kokot", "${post.toString()}")
            if(post.toString() != "null") {
                for (element in post.toString()) {
                    if (element.toString() == "1" || element.toString() == "2" ||  element.toString() == "3"|| element.toString() == "4" || element.toString() == "5"  )
                        toilets.add(element.toString())
                }
            }


        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    })


}
}