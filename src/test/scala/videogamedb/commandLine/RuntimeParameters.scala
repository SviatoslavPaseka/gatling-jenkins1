package videogamedb.commandLine

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class RuntimeParameters extends Simulation {
  val httpProtocol = http.baseUrl(url = "https://videogamedb.uk/api")
    .acceptHeader("application/json")

  def USERCOUNT = System.getProperty("USERCOUNT", "5").toInt
  def RAMPDURATION = System.getProperty("RAMP_DURATION", "10").toInt
  def TESTDURATION = System.getProperty("TEST_DURATION", "30").toInt

  before{
    println(s"Test start with ${USERCOUNT} users")
    println(s"Test start with ${RAMPDURATION} ramp duration")
    println(s"Test start with ${TESTDURATION} test duration")
  }

  def getAllVideoGames() = {
    exec(http("Get all video games")
      .get("/videogame")
      .check(status.is(200)))
      .pause(1)
  }

  def getSpecificVideoGame() = {
    exec(http("Get specific video game")
      .get("/videogame/2")
      .check(status.is(200)))
  }

  val scn = scenario("Load simulation")
    .forever {
      exec(getAllVideoGames())
    }


  setUp(
    scn.inject(
      nothingFor(5),
      rampUsers(USERCOUNT).during(RAMPDURATION)
    )
  ).protocols(httpProtocol)
    .maxDuration(TESTDURATION)
}