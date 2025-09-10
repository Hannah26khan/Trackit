// File: MainActivity.kt
package com.example.trackit

import android.os.Bundle
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    private val scoringManager = ScoringManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request location permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TrackItScreen(scoringManager)
                }
            }
        }
    }
}

@Composable
fun TrackItScreen(scoringManager: ScoringManager) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var isTracking by rememberSaveable { mutableStateOf(false) }
    var currentScore by remember { mutableStateOf(scoringManager.getScore()) }
    var speed by remember { mutableStateOf(0f) }

    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val accelValues = remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }
    val gyroValues = remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }

    // Choose linear acceleration if available (excludes gravity). Fallback to accelerometer.
    val linearAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    val accelSensorForRegister = linearAccelSensor ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // We'll register sensors & location inside one DisposableEffect tied to isTracking so they are cleaned up properly
    DisposableEffect(isTracking) {
        var locationCallback: LocationCallback? = null

        if (isTracking) {
            // Sensor listeners
            val accelListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (accelSensorForRegister != null && event.sensor.type == accelSensorForRegister.type) {
                        accelValues.value = event.values.clone()
                    }

                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            val gyroListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                        gyroValues.value = event.values.clone()
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            if (accelSensorForRegister != null) {
                sensorManager.registerListener(
                    accelListener,
                    accelSensorForRegister,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }

            val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            if (gyroSensor != null) {
                sensorManager.registerListener(
                    gyroListener,
                    gyroSensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }

            // Location updates
            val locationRequest = LocationRequest.create().apply {
                interval = 1000
                fastestInterval = 500
                priority = Priority.PRIORITY_HIGH_ACCURACY
            }

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    // Convert m/s to km/h
                    speed = location.speed * 3.6f

                    val acc = accelValues.value
                    val gyro = gyroValues.value

                    val totalAccel = sqrt(acc[0] * acc[0] + acc[1] * acc[1] + acc[2] * acc[2])
                    val totalRotation = sqrt(gyro[0] * gyro[0] + gyro[1] * gyro[1] + gyro[2] * gyro[2])

                    val zRotation = gyro[2]
                    // Tuned threshold for harsh turn detection (rad/s) — adjust after testing
                    val isHarshTurn = abs(zRotation) > 1.8f

                    if (isHarshTurn) {
                        scoringManager.decreaseScoreForHarshTurn()
                    }

                    scoringManager.updateScore(totalAccel, totalRotation)
                    currentScore = scoringManager.getScore()

                    Log.d("TrackIt", "Lat: ${location.latitude}, Lng: ${location.longitude}, Speed: ${location.speed}, acc=$totalAccel, rot=$totalRotation")
                }
            }

            try {
                // Check permission before requesting updates
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                } else {
                    Log.w("TrackIt", "Location permission not granted; cannot request updates")
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }

            // onDispose will run when isTracking becomes false or composable leaves composition
            onDispose {
                sensorManager.unregisterListener(accelListener)
                sensorManager.unregisterListener(gyroListener)
                locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
            }
        } else {
            // If not tracking, ensure sensors are not registered; nothing to dispose for this branch
            onDispose { }
        }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("TrackIt", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 32.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Driving Score", fontSize = 24.sp, modifier = Modifier.padding(bottom = 8.dp))
                Text(currentScore.toString(), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Speed: ${"%.1f".format(speed)} km/h", fontSize = 18.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { isTracking = !isTracking },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(text = if (isTracking) "Stop Tracking" else "Start Tracking", fontSize = 18.sp)
            }

            // Explicit reset button instead of auto-reset on stop
            Button(
                onClick = {
                    scoringManager.resetScore()
                    currentScore = scoringManager.getScore()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(text = "Reset Score", fontSize = 18.sp)
            }
        }

        if (isTracking) {
            Text(
                text = "Tracking your driving behavior...",
                modifier = Modifier.padding(top = 16.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (currentScore >= 80) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Current Benefits", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    Text("✓ Cashback Rewards")
                    Text("✓ Insurance Discounts")
                    Text("✓ Premium Features")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MaterialTheme {
        TrackItScreen(ScoringManager())
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 640)
@Composable
fun PhonePreview() {
    MaterialTheme {
        TrackItScreen(ScoringManager())
    }
}

