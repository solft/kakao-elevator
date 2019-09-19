package response

import dto.Elevator

data class Start(
    val token: String,
    val timestamp: Int,
    val elevators: List<Elevator>,
    val is_end: Boolean
)