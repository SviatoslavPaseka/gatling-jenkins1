package videogamedb

import com.influxdb.client.write.Point
import com.influxdb.client.{InfluxDBClient, InfluxDBClientFactory}

import scala.jdk.CollectionConverters._

class InfluxDBConnection( url: String, username: String, password: String) {
  val client: InfluxDBClient = InfluxDBClientFactory.create(url, username, password.toCharArray)

  def writePoint(measurement: String, tags: Map[String, String], fields: Map[String, AnyRef]): Unit = {
    val point = Point.measurement(measurement)
      .addTags(tags.asJava)
      .addFields(fields.asJava)
      //.build()

  val writeApi = client.getWriteApi()
  writeApi.writePoint(point)
  //writeApi.writeRecord(bucket, org, WritePrecision.NS, point.toLineProtocol())
  writeApi.close()
  }

  def close(): Unit = {
    client.close()
  }
}
