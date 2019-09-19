package response

import dto.Call
import dto.Elevator

data class OnCalls(
    val token: String,
    val timestamp: Int,
    val elevators: List<Elevator>,
    val calls: List<Call>,
    val is_end: Boolean
)