package videogamedb

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._
import org.influxdb.InfluxDBFactory
import org.influxdb.dto.Point

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._

class MyFirstTest extends Simulation{
  val influxDBConnection = new InfluxDBConnection("http://localhost:8086", "user", "password")

  val influxdb = InfluxDBFactory.connect("http://localhost:8086", "user", "password")
  //influxdb.createDatabase("gatling-MyFirstTest")
  influxdb.setDatabase("gatlingdb")
  def writeToInfluxDB(testName: String, requestName: String, responseTime: Int): Unit = {
    val point = Point.measurement("response_times")
      .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
      .tag("test_name", testName)
      .tag("request_name", requestName)
      .addField("response_time", responseTime)
      .build()
    influxdb.write(point)
  }
  // 1 Http configuration
  val httpProtocol = http.baseUrl(url = "https://videogamedb.uk/api")
        .acceptHeader(value = "application/json")

  def getVideogames ={
    exec(http(requestName = "Get all games")
      .get("/videogame")
    .check(status is 200)
      .check(responseTimeInMillis.saveAs("responseTime"))
    )
//    .exec(session => {
//      val testName = "getVideogames"
//      val requestName = "Get all games"
//      val responseTime = session("responseTime").as[Int]
//      writeToInfluxDB(testName, requestName, responseTime)
//      session
//    })
    .exec(session => {
      println(session.toString)
//      val throughput = session("request").as[Long] / session("duration").as[FiniteDuration].toSeconds
//      val tags = Map("scenario" -> "My Scenario")
//      val fields = Map("throughput" -> throughput.asInstanceOf[AnyRef])
//      influxDBConnection.writePoint("my-measurement", tags, fields)
      session
    })
  }
  def getSpecificVideoGame ={
    exec(http("Get specific game")
      .get("/videogame/1")
      .check(status is 200)
      .check(jsonPath("$.name").is("Resident Evil 4"))
      .check(responseTimeInMillis.saveAs("responseTime")))
//      .exec(session => {
//        println(session)
//        val testName = "getSpecificVideoGame"
//        val requestName = "GetSpecificVideoGames"
//        val responseTime = session("responseTime").as[Int]
//        writeToInfluxDB(testName, requestName, responseTime)
//        session
//      })
  }

  // 2 Scenario Definition
  val scn = scenario(name = "My first test")
    .repeat(1) {
//      exec(getVideogames)
//        .pause(2)
        exec(getSpecificVideoGame)
        .pause(2)
    }

  // 3 Load Scenario
  setUp(
    scn.inject(
      nothingFor(1),
      atOnceUsers(10),
      rampUsers(10).during(5),
      constantUsersPerSec(10).during(10)
    )
  ).protocols(httpProtocol)
  after {
    influxDBConnection.close()
  }
}