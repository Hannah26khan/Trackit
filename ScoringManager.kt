package com.example.trackit


class ScoringManager {
    private var score: Int = 100 // Starting score


    fun updateScore(acceleration: Float, rotation: Float) {
// acceleration expected in m/s^2 (if using TYPE_LINEAR_ACCELERATION it excludes gravity)
        if (acceleration > 10f) {
            score -= 10
        }
        if (rotation > 5f) {
            score -= 5 // Penalty for sharp turns (rotation is a magnitude)
        }
// Clamp score to 0..100
        if (score < 0) score = 0
        if (score > 100) score = 100
    }


    fun decreaseScoreForHarshTurn() {
        score -= 5
        if (score < 0) score = 0
    }


    fun getScore(): Int {
        return score
    }


    fun resetScore() {
        score = 100 // Reset score to starting value
    }
}