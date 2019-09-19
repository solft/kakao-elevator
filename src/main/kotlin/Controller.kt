import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import service.ElevatorAgent

class Controller {
    private val retrofit = Retrofit
        .Builder()
        .baseUrl("http://localhost:8000")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val elevatorAgent = retrofit.create(ElevatorAgent::class.java)


}