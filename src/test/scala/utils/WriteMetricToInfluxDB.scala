package utils

import io.gatling.core.Predef._
import org.influxdb.InfluxDB
import org.influxdb.dto.Point
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable.HashMap
import scala.concurrent.duration.DurationLong

class WriteMetricToInfluxDB(){
  private var counterByRequest = new HashMap[String, AtomicInteger]()

  def writeResponseTime(influxdb: InfluxDB, testName: String, requestName:String) ={
    exec(session => {
      val measurementName = "response_times"
      val fieldName = "response_time"
      val responseTime = session("responseTime").as[Int]
      writeToInfluxDB(testName, requestName, responseTime, measurementName, fieldName, influxdb)
      session
    })
  }

  def writeThroughput(influxdb: InfluxDB, testName: String, requestName: String, secondsPauseBeforeRequest: Long) = {
    counterByRequest.put(testName, new AtomicInteger(0))
    exec(session => {
      counterByRequest.get(testName).get.incrementAndGet()
      val testDuration = System.currentTimeMillis().millisecond.toMillis - Duration.ofMillis(session("startTime").as[Long]).toMillis- secondsPauseBeforeRequest*1000
      val throughput = ((counterByRequest.get(testName).get.toString.toDouble / testDuration) * 1000).toInt
      val measurementName = "throughput"
      writeToInfluxDB(testName, requestName, throughput, measurementName, "throughput", influxdb)
      session
    })
  }

  private def writeToInfluxDB(testName: String, requestName: String, fieldValue: Int, measurementName: String, fieldName: String, influxdb: InfluxDB): Unit = {
    val point = Point.measurement(measurementName)
      .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
      .tag("test_name", testName)
      .tag("request_name", requestName)
      .addField(fieldName, fieldValue)
      .build()
    influxdb.write(point)
  }
}