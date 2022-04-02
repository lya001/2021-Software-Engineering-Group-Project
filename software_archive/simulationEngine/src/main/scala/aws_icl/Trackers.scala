package aws_icl

import com.amazonaws.services.directory.model.ServiceException
import com.amazonaws.services.location.AmazonLocation
import com.amazonaws.services.location.model.{BatchUpdateDevicePositionRequest, BatchUpdateDevicePositionResult, DevicePositionUpdate, GetDevicePositionRequest}

import java.util
import java.util.Date

import scala.collection.parallel.CollectionConverters._

trait Trackers {

  /**
   * Send requests to tracker.
   *
   * @param batched_lines the batched lines containing the data
   * @param aws_loc the AWS location client
   * @param par boolean indicating to parallelize or not the method
   * @param tracker_name the name of the tracker that will receive the requests
   */
  def send_to_trackers(batched_lines: List[List[(String, Double, String, (Double, Double))]], aws_loc: AmazonLocation, par: Boolean, tracker_name: String): Unit = {

    try {
      println(s"TRACKER $tracker_name: Sending requests")

      val batched_lines_new = if (par) batched_lines.par else batched_lines

      // REQUEST + UPDATE
      batched_lines_new.foreach(line => {

        val request: BatchUpdateDevicePositionRequest = new BatchUpdateDevicePositionRequest
        request.setTrackerName(tracker_name)
        val update_list = new util.ArrayList[DevicePositionUpdate]

        line.foreach(upd => {
          val update: DevicePositionUpdate = new DevicePositionUpdate
          update.setDeviceId(upd._1)

          val array = new java.util.ArrayList[java.lang.Double]
          array.add(upd._4._1)
          array.add(upd._4._2)
          update.setPosition(array)

          val time: Date = new Date(upd._2.toLong)
          update.setSampleTime(time)

          update_list.add(update)
        })

        request.setUpdates(update_list)
        println(s"Request: ${request.toString}")
        val update_result: BatchUpdateDevicePositionResult = aws_loc.batchUpdateDevicePosition(request)
        println(s"Request result: ${update_result.toString}")

      })
      println(s"TRACKER $tracker_name: Sending requests finished")

    } catch {
      case e: ServiceException =>
        println(e)
    }
  }


  /**
   * Retrieves data from given tracker.
   *
   * @param aws_loc the AWS location client
   * @param device_name the device name to get data from
   * @param tracker_name the tracker name to get data from
   */
  def retrieve_from_trackers(aws_loc: AmazonLocation, device_name: String, tracker_name: String): Unit = {
    println(s"Retrieved data from tracker $tracker_name and device $device_name")
    val pos_request: GetDevicePositionRequest = new GetDevicePositionRequest
    pos_request.setDeviceId(device_name)
    pos_request.setTrackerName(tracker_name)

    println(s"Request for $tracker_name and device $device_name: ${pos_request.toString}")
    val pos_result = aws_loc.getDevicePosition(pos_request)
    println(s"Result for $tracker_name and device $device_name: ${pos_result.toString}")
  }
}
