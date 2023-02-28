package videogamedb.simulations

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class FixedDurationLoadSimulation extends Simulation{
  val httpProtocol = http.baseUrl(url = "https://videogamedb.uk/api")
    .acceptHeader("application/json")


  def getAllVideoGames() = {
    exec(http("Get all video games")
      .get("/videogame")
      .check(status.is(200)))
  }

  def getSpecificVideoGame() = {
    exec(http("Get specific video game")
      .get("/videogame/2")
      .check(status.is(200)))
  }

  val scn = scenario("Fixed duration load simulation")
    .forever{
      exec(getAllVideoGames())
        .pause(1)
        .exec(getSpecificVideoGame())
        .pause(1)
        .exec(getAllVideoGames())
    }

  setUp(
    scn.inject(
      nothingFor(5),
      atOnceUsers(10),
      rampUsers(20).during(20)
    )
  ).protocols(httpProtocol)
    .maxDuration(60)
}
