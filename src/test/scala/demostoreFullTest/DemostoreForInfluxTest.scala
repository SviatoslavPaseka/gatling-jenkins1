package demostoreFullTest

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.scenario.Simulation

import org.influxdb.InfluxDBFactory
import utils.WriteMetricToInfluxDB

import scala.concurrent.duration.DurationLong

class DemostoreForInfluxTest extends Simulation{

  val domain = "demostore.gatling.io"
  val httpProtocol = http
    .baseUrl("https://" + domain)

  val influxdb = InfluxDBFactory.connect("http://localhost:8086", "user", "password")
  influxdb.setDatabase("gatlingdb")

  val metricWriter = new WriteMetricToInfluxDB()

  val initSession = exec(flushCookieJar)
    .exec(session => session.set("startTime", System.currentTimeMillis()))

  def homePage = {
    exec(
      http("Load home page")
        .get("/")
        .check(status is 200)
        .check(css("#_csrf", "content").saveAs("csrfValue"))
        .check(regex("<title>Gatling Demo-Store</title>"))
        .check(responseTimeInMillis.saveAs("responseTime")))

      .exec(metricWriter.writeResponseTime(influxdb,"homePage","Load home page"))
      .exec(metricWriter.writeThroughput(influxdb, "homePage","Load home page", 0))
  }

  def aboutPage = {
    exec(
      http("Load about us page")
        .get("/about-us")
        .check(status is 200)
        .check(substring("About Us"))
        .check(responseTimeInMillis.saveAs("responseTime")))
      .exec(metricWriter.writeResponseTime(influxdb,"aboutPage","Load about us page"))
      .exec(metricWriter.writeThroughput(influxdb, "aboutPage","Load about us page", 2))
  }

  def browseStore = {
    exec(initSession)
      .exec(homePage)
      .pause(2)
      .exec(aboutPage)
      .pause(2)
  }

  def default = scenario("Default Load test")
    .during(30.seconds) {
      randomSwitch(
        100d -> exec(browseStore)
      )
    }

  setUp(
    default.inject(
      nothingFor(2),
      atOnceUsers(5),
      rampUsers(30).during(30.seconds)
    )
  ).protocols(httpProtocol)
}