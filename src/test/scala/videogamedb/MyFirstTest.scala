package videogamedb

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class MyFirstTest extends Simulation{

  // 1 Http configuration
  val httpProtocol = http.baseUrl(url = "https://videogamedb.uk/api")
        .acceptHeader(value = "application/json")


  // 2 Scenario Definition
  val scn = scenario(name = "My first test")
    .exec(http(requestName = "Get all games")
    .get("/videogame"))
  // 3 Load Scenario
  setUp(
    scn.inject(atOnceUsers(users = 1))
  ).protocols(httpProtocol)

}