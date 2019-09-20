import dto.Call
import dto.Command
import dto.Elevator
import request.Commands
import response.OnCalls
import response.Start
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import service.ElevatorAgent
import kotlin.math.abs

const val ELEVATOR_HIGH = 25
const val ELEVATOR_CAPACITY = 8

fun simpleStrategy(elevators: List<Elevator>, calls: List<Call>): Commands {

    val nowCalls = calls.toMutableList()
    val commandList = mutableListOf<Command>()

    for(elevator in elevators) {

        // 현재 층에 내릴 사람 있는지 확인
        val exitIds = elevator.endPassengers()

        // 현재 층에 탈 사람 있는지 확인
        val enterIds = if (elevator.passengers.size == ELEVATOR_CAPACITY) emptyList() else nowCalls.filter { it.start == elevator.floor }.map { it.id }

        when {
            // 사람 내리기
            exitIds.isNotEmpty() ->
                when(elevator.status) {
                    "STOPPED" -> {
                        commandList.add(Command(elevator.id, "OPEN", emptyList()))
                    }
                    "OPENED" -> {
                        commandList.add(Command(elevator.id, "EXIT", exitIds))
                    }
                    "UPWARD" -> {
                        commandList.add(Command(elevator.id, "STOP", emptyList()))
                    }
                    "DOWNWARD" -> {
                        commandList.add(Command(elevator.id, "STOP", emptyList()))
                    }
                }
            // 사람 태우기
            enterIds.isNotEmpty() -> {
                when(elevator.status) {
                    "STOPPED" -> {
                        commandList.add(Command(elevator.id, "OPEN", emptyList()))
                    }
                    "OPENED" -> {
                        val incomingCalls = enterIds.take(ELEVATOR_CAPACITY - elevator.passengers.size)
                        nowCalls.removeAll { it.id in incomingCalls }
                        commandList.add(Command(elevator.id, "ENTER", incomingCalls))
                    }
                    "UPWARD" -> {
                        commandList.add(Command(elevator.id, "STOP", emptyList()))
                    }
                    "DOWNWARD" -> {
                        commandList.add(Command(elevator.id, "STOP", emptyList()))
                    }
                }
            }
            else -> {
                // 그대로 진행
                when(elevator.status) {
                    "STOPPED" -> {
                        when {
                            elevator.floor == ELEVATOR_HIGH -> commandList.add(Command(elevator.id, "DOWN", emptyList()))
                            elevator.floor == 1 -> commandList.add(Command(elevator.id, "UP", emptyList()))
                            else -> {
                                commandList.add(Command(elevator.id, "DOWN", emptyList()))
                            }
                        }
                    }
                    "OPENED" -> {
                        commandList.add(Command(elevator.id, "CLOSE", emptyList()))
                    }
                    "UPWARD" -> {
                        if (elevator.floor >= ELEVATOR_HIGH) {
                            commandList.add(Command(elevator.id, "STOP", emptyList()))
                        } else {
                            commandList.add(Command(elevator.id, "UP", emptyList()))
                        }
                    }
                    "DOWNWARD" -> {
                        if (elevator.floor <= 1) {
                            commandList.add(Command(elevator.id, "STOP", emptyList()))
                        } else {
                            commandList.add(Command(elevator.id, "DOWN", emptyList()))
                        }
                    }
                }
            }
        }
    }

    return Commands(commandList)
}

fun makeCommand(elevators: List<Elevator>, calls: List<Call>): Commands {
    var nowCalls = calls.toMutableList()
    val commandList = mutableListOf<Command>()

    for(elevator in elevators) {

        val enterIds = nowCalls.filter { it.start == elevator.floor }.map { it.id }
        val exitIds = elevator.endPassengers()

        when(elevator.status) {
            "STOPPED" -> {
                // STOP, UP, DOWN, OPEN
                when {
                    exitIds.isNotEmpty() || enterIds.isNotEmpty() -> {
                        // OPEN
                        commandList.add(Command(elevator.id, "OPEN", emptyList()))
                    }
                    else -> {
                        // UP, DOWN
                        val next = if (elevator.passengers.isEmpty()) nowCalls.map { it.start }.distinct().minBy {
                            abs(
                                it - elevator.floor
                            )
                        }!! else elevator.nextDestination()

                        if(next > elevator.floor)
                            commandList.add(Command(elevator.id, "UP", emptyList()))
                        else
                            commandList.add(Command(elevator.id, "DOWN", emptyList()))
                    }
                }
            }
            "UPWARD" -> {
                // STOP, UP
                when {
                    exitIds.isNotEmpty() || enterIds.isNotEmpty() -> {
                        // STOP
                        commandList.add(Command(elevator.id, "STOP", emptyList()))
                    }
                    else -> {
                        // UP
                        commandList.add(Command(elevator.id, "UP", emptyList()))
                    }
                }
            }
            "DOWNWARD" -> {
                // STOP, DOWN
                when {
                    exitIds.isNotEmpty() || enterIds.isNotEmpty() -> {
                        // STOP
                        commandList.add(Command(elevator.id, "STOP", emptyList()))
                    }
                    else -> {
                        // DOWN
                        commandList.add(Command(elevator.id, "DOWN", emptyList()))
                    }
                }
            }
            "OPENED" -> {
                // OPEN, CLOSE, ENTER, EXIT
                // 순서 Exit -> Enter -> Close

                val command = if (exitIds.isNotEmpty()) "EXIT" else if(enterIds.isNotEmpty()) "ENTER" else "CLOSE"
                val callIds = when(command) {
                    "EXIT" -> exitIds
                    "ENTER" -> {
                        nowCalls = nowCalls.filter { it.start != elevator.floor }.toMutableList()
                        enterIds
                    }
                    else -> emptyList()
                }

                commandList.add(Command(elevator.id, command, callIds))
            }
        }
    }

    return Commands(commandList)
}

fun main() {

    // 초기화
    val retrofit = Retrofit
        .Builder()
        .baseUrl("http://localhost:8000")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val elevatorAgent = retrofit.create(ElevatorAgent::class.java)


    // 0번

    val startResponse: Start = elevatorAgent.start("test", 1, 4).execute().body()!!
    println(startResponse)
    val token = startResponse.token

    while (true) {
        val onCallsResponse: OnCalls = elevatorAgent.onCalls(token).execute().body()!!

        if (onCallsResponse.is_end)
            break

        println("엘리베이터 상태: ${onCallsResponse.elevators}")
        println("요청 상태: ${onCallsResponse.calls}")

        val nextCommands = simpleStrategy(onCallsResponse.elevators, onCallsResponse.calls)
        println(nextCommands)
        val actionResponse = elevatorAgent.action(token, nextCommands).execute().body()!!
        println(actionResponse)
        println("**************")

//        Thread.sleep(1000L)
    }

}