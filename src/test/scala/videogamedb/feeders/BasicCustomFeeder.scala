package videogamedb.feeders

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class BasicCustomFeeder extends Simulation{
  val httpProtocol = http.baseUrl(url = "https://videogamedb.uk/api")
    .acceptHeader("application/json")

  var idNumbers = (1 to 10).iterator


  val customFeeder = Iterator.continually(Map("gameId" -> idNumbers.next()))

  def getSpecificVideoGame() = {
    repeat(10) {
      feed(customFeeder)
        .exec(http("Get specific game with id - #{gameId}")
          .get("/videogame/#{gameId}")
          .check(status is 200)
        .check(jsonPath("$.name").saveAs("gameName")))
        .pause(1)
        .exec{ session => println(session("gameName").as[String]); session }
    }
  }

  val scn = scenario("Get specific video game by name")
    .exec(getSpecificVideoGame())

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(httpProtocol)
}
