package videogamedb.scriptfundamental

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class CheckResponseBodyAndExtract extends Simulation{
  val httpProtocol = http.baseUrl(url = "https://videogamedb.uk/api")
    .acceptHeader("application/json")

  val scn = scenario("Check with Json")

    .exec(http("Get specific game")
    .get("/videogame/1")
    .check(jsonPath("$.name").is("Resident Evil 4")))

    .exec(http("Get all videogames")
    .get("/videogame")
    .check(jsonPath("$[0].id").saveAs("gameId")))

    .exec {sessoin => println(sessoin); sessoin}

    .exec(http("Get specific game - 2nd call")
    .get("/videogame/#{gameId}")
    .check(jsonPath("$.name") is ("Resident Evil 4"))
    .check(bodyString.saveAs("responseBody")))

    .exec{session => println(session("responseBody").as[String]); session}


  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(httpProtocol)
}

