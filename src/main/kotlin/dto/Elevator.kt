package dto

import kotlin.math.abs

data class Elevator(
    val id: Int,
    val floor: Int,
    val passengers: List<Call>,
    val status: String
) {

    fun endPassengers(): List<Int> {
        return passengers.filter { it.end == floor }.map { it.id }
    }

    fun nextDestination(): Int {
        return passengers.map { it.start }.distinct().minBy {
            abs(
                it - floor
            )
        }!!
    }
}
