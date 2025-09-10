# Trackit
TrackIt is an android app built in Kotlin using Jetpack Compose that monitors your driving behaviour. It uses smartphone sensors like accelerometer, gyroscope and GPS to calcualte your speed and intensity(sharpness) of your turns. Each sensor has the following use:

Accelerometer: Detects sudden positive and negative spikes in speed, basically harsh acceleration or sudden brakes.
Gyroscope: Detects sharp turns.
GPS: For calculating speed.

At the end you will be provided with a score out of 100 based on the aboe parameters using our scoring logic.

MainActivity.kt -> Handles the imports, UI, Sensors integration.        
ScoringManager.kt -> Handles Score output with a scoring logic.
