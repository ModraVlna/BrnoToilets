package com.example.toiletsbrno

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DatabaseServer(context: Context) {

    private lateinit var databaseReference: DatabaseReference    // lTEINIT NESKOR DOSADIM AK NIE CHRASH
    init {
        FirebaseApp.initializeApp(context)
        databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://toiletsbrno.firebaseio.com")
        databaseReference.keepSynced(true)  // automaticki synchroniyovana
    }

    fun getDatabaseReference(): DatabaseReference {
        return databaseReference


    }

    fun getPlacesReference(): DatabaseReference {
        return databaseReference.child("toilets")
    }

    fun sendDataToFirebase(reference: DatabaseReference, key: String?, objectToSend: Any, listener: ValueEventListener){

        var firebaseKey: String? = null
        if (key == null){
            firebaseKey = reference.push().key
        } else {
            firebaseKey = key
        }

        reference.child(firebaseKey!!).setValue(objectToSend)
        reference.addValueEventListener(listener)

    }

}