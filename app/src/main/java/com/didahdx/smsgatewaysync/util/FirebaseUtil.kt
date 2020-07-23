package com.didahdx.smsgatewaysync.utilities


import android.app.ListActivity
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.database.*
import kotlin.collections.ArrayList
import java.util.*


object FirebaseUtil {
    var mFirebaseDatabase: FirebaseDatabase? = null
    var mDatabaseReference: DatabaseReference? = null
    private var firebaseUtil: FirebaseUtil? = null
    var mFirebaseAuth: FirebaseAuth? = null

    var mAuthListener: AuthStateListener? = null
//    var mDeals: ArrayList<TravelDeal>? = null
    private const val RC_SIGN_IN = 123
//    private var caller: ListActivity? = null
    var isAdmin = false
    fun openFbReference(ref: String?, callerActivity: ListActivity) {
        if (firebaseUtil == null) {
            firebaseUtil = FirebaseUtil
            mFirebaseDatabase = FirebaseDatabase.getInstance()
            mFirebaseAuth = FirebaseAuth.getInstance()
//            caller = callerActivity
            mAuthListener = AuthStateListener { firebaseAuth ->
                if (firebaseAuth.currentUser == null) {
//                    signIn()
                } else {
                    val userId = firebaseAuth.uid
                    checkAdmin(userId)
                }
                Toast.makeText(
                    callerActivity.baseContext,
                    "Welcome back!",
                    Toast.LENGTH_LONG
                ).show()
            }
//            connectStorage()
        }
//        mDeals = ArrayList<TravelDeal>()
        mDatabaseReference = mFirebaseDatabase!!.reference.child(ref!!)
    }

//    private fun signIn() {
//        // Choose authentication providers
//        val providers: List<AuthUI.IdpConfig> = Arrays.asList(
//            Builder(AuthUI.EMAIL_PROVIDER).build(),
//            Builder(AuthUI.GOOGLE_PROVIDER).build()
//        )
//
//// Create and launch sign-in intent
//        caller!!.startActivityForResult(
//            AuthUI.getInstance()
//                .createSignInIntentBuilder()
//                .setAvailableProviders(providers)
//                .setIsSmartLockEnabled(false)
//                .build(),
//            RC_SIGN_IN
//        )
//    }

    private fun checkAdmin(uid: String?) {
        isAdmin = false
        val ref =
            mFirebaseDatabase!!.reference.child("administrators")
                .child(uid!!)
        val listener: ChildEventListener = object : ChildEventListener {
            override fun onChildAdded(
                dataSnapshot: DataSnapshot,
                s: String?
            ) {
                isAdmin = true
//                caller.showMenu()
            }

            override fun onChildChanged(
                dataSnapshot: DataSnapshot,
                s: String?
            ) {
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
            override fun onChildMoved(
                dataSnapshot: DataSnapshot,
                s: String?
            ) {
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        }
        ref.addChildEventListener(listener)
    }

    fun attachListener() {
        mFirebaseAuth!!.addAuthStateListener(mAuthListener!!)
    }

    fun detachListener() {
        mFirebaseAuth!!.removeAuthStateListener(mAuthListener!!)
    }

//    fun connectStorage() {
//        mStorage = FirebaseStorage.getInstance()
//        mStorageRef = mStorage.getReference().child("deals_pictures")
//    }
}
