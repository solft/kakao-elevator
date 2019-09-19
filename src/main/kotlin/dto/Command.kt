package dto

data class Command(
    val elevator_id: Int,
    val command: String,
    val call_ids: List<Int>
)