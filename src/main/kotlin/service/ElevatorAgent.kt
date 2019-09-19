package service

import request.Commands
import response.Action
import response.OnCalls
import response.Start
import retrofit2.Call
import retrofit2.http.*

interface ElevatorAgent {

    @POST("/start/{user_key}/{problem_id}/{number_of_elevators}")
    fun start(
        @Path("user_key") userKey: String,
        @Path("problem_id") problemId: Int,
        @Path("number_of_elevators") numberOfElevators: Int
    ): Call<Start>

    @GET("/oncalls")
    fun onCalls(@Header("X-Auth-Token") token: String): Call<OnCalls>

    @Headers("Content-Type:application/json; charset=UTF-8")
    @POST("/action")
    fun action(
        @Header("X-Auth-Token") token: String,
        @Body commands: Commands
    ): Call<Action>
}