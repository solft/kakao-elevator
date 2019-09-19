package dto

data class Elevator(
    val id: Int,
    val floor: Int,
    val passengers: List<Call>,
    val status: String
)