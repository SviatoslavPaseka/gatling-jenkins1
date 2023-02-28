package videogamedb.scriptfundamental

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

import scala.concurrent.duration.DurationInt

class AddPauseTime extends Simulation{
  val httpProtocol = http.baseUrl(url = "https://videogamedb.uk/api")
    .acceptHeader("application/json")

  val scn = scenario("Video game db 3 calls")
    .exec(http("Get all video games 1st call")
    .get("/videogame"))
    .pause(5)
    .exec(http("Get specific game")
    .get("/videogame/1"))
    .pause(1, 6)

    .exec(http("Get all video games 2nd call")
    .get("/videogame"))
    .pause(3000.milliseconds)

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(httpProtocol)
}
