package videogamedb.feeders

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Random

class ComplexCustomFeeder extends Simulation{
  val httpProtocol = http.baseUrl(url = "https://videogamedb.uk/api")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  var idNumbers = (1 to 10).iterator
  val rnd = new Random()
  val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  val now = LocalDate.now();

  def randomString(length : Int) ={
    rnd.alphanumeric.filter(_.isLetter).take(length).mkString
  }

  def getRandomDate(startDate : LocalDate, random: Random) : String ={
    startDate.minusDays(random.nextInt(30)).format(pattern)
  }

  val customFeeder = Iterator.continually(Map(
    "gameId" -> idNumbers.next(),
    "name" -> ("Game - " + randomString(5)),
    "releaseDate" -> getRandomDate(now, rnd),
    "reviewScore" -> rnd.nextInt(100),
    "category" -> ("Category - " + randomString(5)),
    "rating" -> ("Rating - " + randomString(3))
  ))

  def authenticate() ={
    exec(http("Authenticate")
    .post("/authenticate")
    .body(StringBody(
      "{\n  \"password\": \"admin\",\n  \"username\": \"admin\"\n}"
    ))
    .check(jsonPath("$.token").saveAs("jwtToken")))
  }

  def createNewGame()={
    repeat(10){
      feed(customFeeder)
        .exec(http("Create a new game - #{name}")
          .post("/videogame")
          .header("Authorization", "Bearer #{jwtToken}")
          .body(ElFileBody("bodies/newGameTemplate.json")).asJson
          .check(status not 404)
        .check(bodyString.saveAs("responseBody")))
        .exec {session => println(session("responseBody").as[String]); session}
        .pause(1)
    }
  }

  def getSpecificVideoGame() = {
    repeat(10) {
      feed(customFeeder)
        .exec(http("Get specific game with id - #{gameId}")
          .get("/videogame/#{gameId}")
          .check(status is 200))
        .pause(1)
    }
  }

//  val scn = scenario("Get specific video game by name")
//    .exec(getSpecificVideoGame())
    val scn = scenario("Create a game by a custom feeder")
        .exec(authenticate())
        .exec(createNewGame())

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(httpProtocol)
}