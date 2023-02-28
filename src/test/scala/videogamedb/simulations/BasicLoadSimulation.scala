package videogamedb.simulations

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._
class BasicLoadSimulation extends Simulation{
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

  val scn = scenario("Load simulation")
    .exec(getAllVideoGames())
    .pause(3)
    .exec(getSpecificVideoGame())
    .pause(3)
    .exec(getAllVideoGames())



  setUp(
    scn.inject(
      nothingFor(4),
      atOnceUsers(5),
      rampUsers(10).during(10)
    )
  ).protocols(httpProtocol)
}