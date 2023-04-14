package demostoreFullTest

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.scenario.Simulation

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.util.Random

import org.influxdb.InfluxDBFactory
import org.influxdb.dto.Point


class DemostoreTest extends Simulation {

  val domain = "demostore.gatling.io"
  val rnd = new Random()

  val influxdb = InfluxDBFactory.connect("http://localhost:8086", "user", "password")
  influxdb.setDatabase("gatlingdb")


  def writeToInfluxDB(testName: String, requestName: String, responseTime: Int): Unit = {
    val point = Point.measurement("response_times")
      .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
      .tag("test_name", testName)
      .tag("request_name", requestName)
      .addField("response_time", responseTime)
      .build()
    influxdb.write(point)
  }

  def userCount = getProperty("USERS", "30").toInt
  def rampDuration = getProperty("RAMP_DURATION", "10").toInt
  def testDuration = getProperty("DURATION", "20").toInt

  private def getProperty(propertyName : String, defaultValue: String)= {
    Option(System.getenv(propertyName))
      .orElse(Option(System.getProperty(propertyName)))
      .getOrElse(defaultValue)
  }


  def randomString (length:Int) :String = {
    rnd.alphanumeric.filter(_.isLetter).take(length).mkString
  }

  val httpProtocol = http
    .baseUrl("https://" + domain)

  val categoryFeederCsv = csv("data/categoryDetails.csv").random
  val productFeederJson = jsonFile("data/productDetails.json").random
  val loginFeederCsv = csv("data/loginDetails.csv").circular

  val initSession = exec(flushCookieJar)
    .exec(session => session.set("randomNumber", rnd.nextInt))
    .exec(session => session.set("customerLoggedIn", false))
    .exec(session => session.set("cartTotal", 0.0))
    .exec(addCookie(Cookie("sessionId", randomString(10)).withDomain(domain)))
    .exec{session => println(session);session}

  object CMSPages{
    def homePage ={
      exec(
        http("Load home page")
          .get("/")
          .check(status is 200)
          .check(css("#_csrf", "content").saveAs("csrfValue"))
          .check(regex("<title>Gatling Demo-Store</title>"))
          .check(responseTimeInMillis.saveAs("responseTime")))
//          .exec(session => {
//            val testName = "homePage"
//            val requestName = "Load home page"
//            val responseTime = session("responseTime").as[Int]
//            writeToInfluxDB(testName, requestName, responseTime)
//            session
//          }
    }

    def aboutPage ={
      exec(
        http("Load about us page")
          .get("/about-us")
          .check(status is 200)
          .check(substring("About Us"))
          .check(responseTimeInMillis.saveAs("responseTime")))
//          .exec(session => {
//            val testName = "aboutPage"
//            val requestName = "Load about us page"
//            val responseTime = session("responseTime").as[Int]
//            writeToInfluxDB(testName, requestName, responseTime)
//            session
//          })
    }
  }

  object Catalog {
    object Category {
      def view = {
        feed(categoryFeederCsv)
          .exec(
            http("Load Categories Page - ${categoryName}")
              .get("/category/${categorySlug}")
              .check(status is 200)
              .check(css("#CategoryName").is("${categoryName}"))
              .check(responseTimeInMillis.saveAs("responseTime")))
//          .exec(session => {
//            val testName = "Catalog.Category.view"
//            val requestName = "Load Categories Page - ${categoryName}"
//            val responseTime = session("responseTime").as[Int]
//            writeToInfluxDB(testName, requestName, responseTime)
//            session
//          })
      }
    }
    object Product {
      def view = {
        feed(productFeederJson)
          .exec(
            http("Load product page - ${name}")
              .get("/product/${slug}")
              .check(status is 200)
              .check(css("#ProductDescription").is ("${description}"))
              .check(responseTimeInMillis.saveAs("responseTime")))
//          .exec(session => {
//            val testName = "Catalog.Product.view"
//            val requestName = "Load product page - ${name}"
//            val responseTime = session("responseTime").as[Int]
//            writeToInfluxDB(testName, requestName, responseTime)
//            session
//          })
      }
      def add = {
        var total = 0.0
        exec(view)
        .exec(
          http("Add product to cart")
            .get("/cart/add/${id}")
            .check(status is 200)
            .check(substring("items in your cart"))
        )
          .exec(session => {
            val currentCartTotal = session("cartTotal").as[Double]
            val itemPrice = session("price").as[Double]
            total = currentCartTotal + itemPrice
            session
          })
          .exec(session => session.set("cartTotal", total))

      }
    }
  }

  object Customer {
    def login ={
      feed(loginFeederCsv)
        .exec(
          http("Load login page")
            .get("/login")
            .check(status is 200)
            .check(substring("Username:"))
        )
        .exec(
          http("Login action")
            .post("/login")
            .check(status is 200)
            .formParam("_csrf", "${csrfValue}")
            .formParam("username", "${username}")
            .formParam("password", "${password}")
      ).exec(session => session.set("customerLoggedIn", true))
    }
  }

  object Checkout {
    def viewCart ={
      doIf(session => !session("customerLoggedIn").as[Boolean]) {
        exec(Customer.login)
      }
      .exec(
          http("Load cart page")
            .get("/cart/view")
            .check(status is 200)
            .check(substring("Cart Overview"))
            .check(css("#grandTotal").is("$#{cartTotal}"))

        )
    }

    def checkout = {
      exec(
        http("Checkout")
          .get("/cart/checkout")
          .check(status is 200)
          .check(substring("Thanks for your order! See you soon!"))
      )
    }
  }

   val scn = scenario("DemostoreTest")
     .exec(initSession)
    .exec(CMSPages.homePage)
    .pause(2)
    .exec(CMSPages.aboutPage)
    .pause(2)
    .exec(Catalog.Category.view)
    .pause(2)
     .exec(Catalog.Product.add)
    .pause(2)
    .exec(Checkout.viewCart)
    .pause(2)
    .exec(Checkout.checkout)

  object UserJourneys {
    val minPause = 500.milliseconds
    val maxPause = 3.seconds

    def browseStore = {
      exec(initSession)
        .exec(CMSPages.homePage)
        .pause(maxPause)
        .exec(CMSPages.aboutPage)
        .pause(minPause, maxPause)
        .repeat(5){
          exec(Catalog.Category.view)
            .pause(minPause, maxPause)
            .exec(Catalog.Product.view)
        }
    }

    def abandonCart ={
      exec(initSession)
        .exec(CMSPages.homePage)
        .pause(maxPause)
        .exec(Catalog.Category.view)
        .pause(minPause, maxPause)
        .exec(Catalog.Product.view)
        .pause(minPause, maxPause)
        .exec(Catalog.Product.add)
    }

    def completePurchase ={
      exec(initSession)
        .exec(CMSPages.homePage)
        .pause(maxPause)
        .exec(Catalog.Category.view)
        .pause(minPause, maxPause)
        .exec(Catalog.Product.view)
        .pause(minPause, maxPause)
        .exec(Catalog.Product.add)
        .pause(minPause, maxPause)
        .exec(Checkout.viewCart)
        .pause(minPause, maxPause)
        .exec(Checkout.checkout)
    }
  }

  object Scenarios {
    def default = scenario("Default Load test")
      .during(testDuration.seconds){
        randomSwitch(
          75d -> exec(UserJourneys.browseStore),
          15d -> exec(UserJourneys.abandonCart),
          10d -> exec(UserJourneys.completePurchase)
        )
      }
    def highPurchase = scenario("High Purchase Load test")
      .during(testDuration.seconds) {
        randomSwitch(
          25d -> exec(UserJourneys.browseStore),
          25d -> exec(UserJourneys.abandonCart),
          50d -> exec(UserJourneys.completePurchase)
        )
      }
  }

  setUp(
    Scenarios.default.inject(
      rampUsers(userCount).during(rampDuration.seconds)
    )
//    Scenarios.highPurchase.inject(
//      rampUsers(5).during(10.seconds)
//    )
  ).protocols(httpProtocol).assertions(global.responseTime.mean.lt(700))

  //**Closed model simulation**
//	setUp(
//    scn.inject(
//      constantConcurrentUsers(10).during(20.seconds),
//      rampConcurrentUsers(10).to(20).during(20.seconds)
//    )
//   ).protocols(httpProtocol)

  //**Throttle simulation** (11% failed requests)
//  setUp(
//    scn.inject(
//      constantUsersPerSec(1).during(3.minutes)
//    )
//  ).protocols(httpProtocol)
//    .throttle(
//      reachRps(10).in(30.seconds),
//      holdFor(60.seconds),
//      jumpToRps(20),
//      holdFor(60.seconds)
//    ).maxDuration(3.minutes)
}