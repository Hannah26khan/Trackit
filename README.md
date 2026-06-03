# TrackIt

> A sensor-driven Android application for real-time driving behaviour monitoring and quantitative safety scoring — built entirely on native smartphone hardware, requiring no external devices or proprietary infrastructure.

---

## Overview

TrackIt is an Android application developed in Kotlin that leverages the onboard sensor suite of a smartphone — specifically the GPS receiver, accelerometer, and gyroscope — to continuously monitor and evaluate driving behaviour during active trips. The application processes raw inertial and positional data in real time, applies a deterministic scoring model to quantify driver behaviour, and presents the results through a responsive Jetpack Compose interface.

The fundamental premise of TrackIt is that the smartphone already in a driver's pocket contains sufficient sensing capability to produce meaningful assessments of driving quality, without requiring OBD-II port adapters, vehicle integration, or cloud inference pipelines. All computation occurs entirely on-device.

Every session begins with a perfect score of **100**. Adverse driving events — detected through physical thresholds applied to sensor magnitudes — reduce the score progressively. The score at the end of a trip is a direct reflection of the quality of driving across that session.

---

## Core Capabilities

- **Continuous real-time scoring** — driving score updated on a 1-second cycle, derived from live GPS and inertial sensor data, displayed prominently throughout the session
- **Vehicle speed telemetry** — instantaneous speed in km/h extracted from the FusedLocationProvider and rendered in real time
- **Harsh acceleration and braking detection** — total linear acceleration magnitude evaluated per GPS cycle; events exceeding the configured threshold are penalised immediately
- **Directional harsh turn detection** — dual-layer evaluation combining Z-axis gyroscope rotation rate against a tuned angular velocity threshold, with a secondary check on total rotation magnitude
- **Intelligent sensor selection** — the application preferentially registers `TYPE_LINEAR_ACCELERATION` to obtain gravity-compensated inertial data; it falls back gracefully to `TYPE_ACCELEROMETER` on devices where the linear sensor is unavailable, ensuring broad hardware compatibility
- **Compose-native lifecycle management** — all sensor listeners and location update callbacks are bound to the tracking state via `DisposableEffect`, guaranteeing correct registration and teardown without resource leaks
- **Score-gated benefits display** — a contextual benefits panel is conditionally rendered when the driver maintains a score at or above 80, establishing a tangible incentive for safe driving
- **Independent score reset** — session score can be reset explicitly without interrupting an active tracking session

---

## Technical Architecture

| Component | Implementation |
|---|---|
| Language | Kotlin |
| UI Framework | Jetpack Compose with Material Design 3 |
| Location Provider | Google Play Services — `FusedLocationProviderClient` |
| Inertial Sensing | Android `SensorManager` — Accelerometer, Gyroscope |
| Architectural Pattern | Single-activity, reactive state-driven Compose UI |
| Build System | Gradle with Kotlin DSL |
| Minimum SDK | Android 7.0 (API 24) |

---

## Scoring Model

The scoring engine is implemented in `ScoringManager.kt` and operates on a strictly subtractive basis. The session score initialises at **100** and is decremented upon detection of the following events, evaluated once per GPS location callback:

| Event Class | Detection Criterion | Score Penalty |
|---|---|---|
| Harsh acceleration or braking | Total acceleration vector magnitude > 10 m/s² | −10 points |
| Excessive rotational force | Total gyroscope vector magnitude > 3 rad/s | −5 points |
| Directional harsh turn | Z-axis angular velocity magnitude > 1.8 rad/s | −5 points |

The score is hard-clamped to the range **[0, 100]** at all times. Sensor data is sampled continuously at `SENSOR_DELAY_NORMAL`; the most recent snapshot is consumed during each GPS callback for scoring evaluation.

---

## Setup and Installation

### Requirements

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK API level 24 or higher
- A physical Android device — sensor emulation is not available on the Android Emulator
- `ACCESS_FINE_LOCATION` permission granted by the user at runtime

### Building from Source

```bash
git clone https://github.com/yourusername/trackit.git
cd trackit
```

Open the project directory in Android Studio. Allow the Gradle sync to complete, then deploy to a connected physical device via Run or `Shift + F10`.

### Runtime Permissions

The application declares and requests the following permission:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

No network access is required at any point. The application does not transmit data externally, contact remote servers, or require an active internet connection. All sensor processing and scoring logic executes entirely on-device.

---

## Project Structure

```
app/
└── src/
    └── main/
        └── java/com/example/trackit/
            ├── MainActivity.kt       # Compose UI, sensor registration, GPS callbacks, lifecycle management
            └── ScoringManager.kt     # Scoring state, threshold evaluation, penalty application
```

---

## Roadmap

The following capabilities are identified as the primary directions for future development:

- [ ] **Orientation normalisation** — compensate for variable phone placement within the vehicle so that scoring remains consistent regardless of whether the device is mounted, pocketed, or resting on a surface
- [ ] **Per-event audit log** — maintain a timestamped record of all detected events within a session, providing the driver with granular post-trip insight into score changes
- [ ] **Decaying penalty model** — replace the current permanent penalty system with an asymmetric decay function, wherein early infractions retain a lasting negative weight that diminishes over sustained periods of safe driving, without full erasure
- [ ] **Automatic trip detection** — eliminate the requirement for manual session initiation by inferring trip boundaries from GPS velocity and dwell-time heuristics
- [ ] **Multi-trip session history** — persist trip records across sessions and expose aggregate statistics over time
- [ ] **Road surface noise attenuation** — introduce signal filtering to suppress false positive events attributable to road surface irregularities rather than driver behaviour

---

## Known Limitations

The following constraints are acknowledged in the current implementation:

- The scoring model is entirely threshold-based; it does not employ statistical learning, adaptive calibration, or contextual road condition awareness
- Sensor axis readings are orientation-dependent — consistent phone mounting within the vehicle is recommended for reliable and reproducible results
- GPS positional accuracy is subject to degradation in dense urban environments due to signal multipath, and is unavailable in tunnels and underground areas
- Active tracking requires the application to remain in the foreground; a persistent background service has not been implemented in this version

---

## Contributing

Contributions are welcome. For significant changes, please open an issue prior to submitting a pull request to allow for discussion of the proposed modification.

---

<p align="center">Developed with Kotlin and Jetpack Compose &nbsp;·&nbsp; Entirely on-device &nbsp;·&nbsp; No proprietary hardware required</p>