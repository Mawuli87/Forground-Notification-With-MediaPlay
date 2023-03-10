package com.yawomessie.forgroungnotificationwithmediaplayer

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.yawomessie.forgroungnotificationwithmediaplayer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val TAG = MainActivity::class.java.simpleName
    private var mService: Messenger? = null
    private lateinit var mBoundServiceIntent: Intent
    private var mServiceBound = false

    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mService = Messenger(service)
            mServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
            mServiceBound = false
        }
    }
    private lateinit var requestLauncher: ActivityResultLauncher<String>


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)



    requestLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                mBoundServiceIntent = Intent(this@MainActivity, MediaService::class.java)
                mBoundServiceIntent.action = MediaService.ACTION_CREATE

                startService(mBoundServiceIntent)
                bindService(mBoundServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)


            } else {
                showErrorMessage()
            }
        }



        binding.btnPlay.setOnClickListener {
           askForNotificationPermission()
           if (checkForPermission(Manifest.permission.FOREGROUND_SERVICE)){
               if (mServiceBound) {
                   try {
                       mService?.send(Message.obtain(null, MediaService.PLAY, 0, 0))
                   } catch (e: RemoteException) {
                       e.printStackTrace()
                   }
               }
           }else {
               askForNotificationPermission()
           }
        }

        binding.btnStop.setOnClickListener {
            if (mServiceBound) {
                try {
                    mService?.send(Message.obtain(null, MediaService.STOP, 0, 0))
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
        unbindService(mServiceConnection)
        mBoundServiceIntent.action = MediaService.ACTION_DESTROY

        startService(mBoundServiceIntent)
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun askForNotificationPermission() {
    requestLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun showErrorMessage() {
        Toast.makeText(
            this,
            "Permission is not granted",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun checkForPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}