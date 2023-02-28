package videogamedb.feeders

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class CsvFeeder extends Simulation{
  val httpProtocol = http.baseUrl(url = "https://videogamedb.uk/api")
    .acceptHeader("application/json")



  val csvFeeder = csv("data/gameCsvFile.csv").circular

  def getSpecificVideoGame() ={
    repeat(10) {
      feed(csvFeeder)
        .exec(http("Get specific game with the name - #{gameName}")
          .get("/videogame/#{gameId}")
          .check(jsonPath("$.name").is("#{gameName}"))
          .check(status is 200))
        .pause(1)
    }
  }

  val scn = scenario("Get specific video game by name")
    .exec(getSpecificVideoGame())

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(httpProtocol)
}