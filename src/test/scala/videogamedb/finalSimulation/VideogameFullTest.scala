package videogamedb.finalSimulation

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Random

class VideogameFullTest extends Simulation{
  val httpProtocol = http.baseUrl(url = "https://videogamedb.uk/api")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  def USERCOUNT = System.getProperty("USERCOUNT", "5").toInt
  def RAMPDURATION = System.getProperty("RAMP_DURATION", "10").toInt
    def TESTDURATION = System.getProperty("TEST_DURATION", "30").toInt

  var idNumbers = (1 to 10000).iterator
  val rnd = new Random()
  val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  val now = LocalDate.now();

  before {
    println(s"Test start with ${USERCOUNT} users")
    println(s"Test start with ${RAMPDURATION} ramp duration")
    println(s"Test start with ${TESTDURATION} test duration")
  }

  after{
    println("test end")
  }

  def randomString(length: Int) = {
    rnd.alphanumeric.filter(_.isLetter).take(length).mkString
  }

  def getRandomDate(startDate: LocalDate, random: Random): String = {
    startDate.minusDays(random.nextInt(30)).format(pattern)
  }

  def getRandomNumber(random: Random) ={
    random.nextInt(9) + 1
  }

  val customFeeder = Iterator.continually(Map(
    "gameId" -> idNumbers.next(),
    "name" -> ("Game - " + randomString(5)),
    "releaseDate" -> getRandomDate(now, rnd),
    "reviewScore" -> rnd.nextInt(100),
    "category" -> ("Category - " + randomString(5)),
    "rating" -> ("Rating - " + randomString(3))
  ))

  def getAllVideoGames() = {
    exec(http("Get all video games")
      .get("/videogame")
      .check(status.is(200)))
      .pause(2)
  }

  def authenticate() = {
    exec(http("Authenticate")
      .post("/authenticate")
      .body(StringBody(
        "{\n  \"password\": \"admin\",\n  \"username\": \"admin\"\n}"
      ))
      .check(jsonPath("$.token").saveAs("jwtToken")))
      .pause(2)
  }

  def getSpecificVideoGame() = {

      exec(http(s"Get specific video game with id: ${getRandomNumber(rnd)}")
        .get(s"/videogame/${getRandomNumber(rnd)}")
        .check(jsonPath("$.name").saveAs("specificGameName"))
        .check(status.is(200)))
      .exec{session => println(session("specificGameName").as[String]);session}
      .pause(2)
  }

  def createNewGame()={
    feed(customFeeder)
    .exec(http("Create a new game")
      .post("/videogame")
        .header("Authorization", "Bearer #{jwtToken}")
        .body(ElFileBody("bodies/newGameTemplate.json")).asJson
        .check(status not 403)
      .check(status is 200)
      .check(bodyString.saveAs("responseBody")))
      .exec{session => println(session("responseBody").as[String]); session}
      .pause(1)
  }

  def deleteGame() ={

      exec(http("Delete a game")
        .delete("/videogame/2")
        .header("Authorization", "Bearer #{jwtToken}")

        .check(status.is(200).saveAs("okStatus")))
      .exec{session => println(session("okStatus").as[String]);session}
        .pause(1)
  }

  val scn = scenario("Load simulation")
    .forever{
      exec(authenticate())
        .exec(getAllVideoGames())
        .exec(createNewGame())
        .exec(getSpecificVideoGame())
        .exec(deleteGame())
    }


  setUp(
    scn.inject(
      nothingFor(5),
      atOnceUsers(10),
      rampUsers(USERCOUNT).during(RAMPDURATION)
    )
  ).protocols(httpProtocol)
    .maxDuration(TESTDURATION)
    .assertions(
      global.responseTime.max.lt(220),
      global.successfulRequests.percent.gt(99)
    )
}