package aws_icl

import java.time.{LocalDateTime, ZoneId}
import scala.collection.Map
import scala.util.Random

trait Parser {

  /**
   * Obtains the data from the corresponding json file and filters it to only keep what is necessary for the publishing simulations.
   *
   * @param seconds_rate the second interval between the location updates from the same device
   * @param file_name the name of the json file to be parsed
   * @param offset number of seconds to add to the timestamps in the file
   * @return the parsed data
   */
  def parse_list(seconds_rate: Int, file_name: String, offset: Int): List[(String, Double, String, (Double, Double))] = {

    val lines_stream: os.Generator[String] = os.read.lines.stream(os.pwd / "resources" / "trip_sim" / "output" / file_name)
    val lines_json: List[String] = lines_stream.toList

    val parsed_lines_1: List[ujson.Value.Value] = lines_json.map(line => ujson.read(line))
    val parsed_lines_2 = parsed_lines_1.map(
      line => (line("properties")("id").str, line("properties")("time").num / 1000 + offset, line("properties")("status").str,
              (line("geometry")("coordinates")(0).num, line("geometry")("coordinates")(1).num)))


    //Randomly chooses the offset for the seconds rate filtering (do not confuse to the other offset added to avoid overlapping timestamps).
    val random_remainder = Random.between(0, seconds_rate)
    val parsed_lines_time_filtered = parsed_lines_2.filter(quad => (quad._2 % seconds_rate.toDouble).toInt == random_remainder)
    parsed_lines_time_filtered
  }


  /**
   * Obtains the data from the corresponding json file and filters it to only keep what is necessary for the publishing simulations.
   * Batches the result.
   *
   * @param batch_size the number of elements of each batch
   * @param seconds_rate the second interval between the location updates from the same device
   * @param file_name the name of the json file to be parsed
   * @param offset number of seconds to add to the timestamps in the file
   * @return the batched parsed data
   */
  def parse_list_batched(batch_size: Int, seconds_rate: Int,
                         file_name: String, offset: Int): List[List[(String, Double, String, (Double, Double))]] = {
    parse_list(seconds_rate, file_name, offset).grouped(batch_size).toList
  }


  /**
   * Obtains the data from the corresponding json file and filters it to only keep what is necessary for the publishing simulations.
   * Organises the result with respect to the device names.
   *
   * @param seconds_rate the second interval between the location updates from the same device
   * @param file_name the name of the json file to be parsed
   * @param offset number of seconds to add to the timestamps in the file
   * @return the device-name-distributed parsed data
   */
  def parse_list_device_filtered(seconds_rate: Int, file_name: String,
                                 offset: Int): Map[String, List[(String, Double, String, (Double, Double))]] = {
    val temp = parse_list(seconds_rate, file_name, offset).groupBy(_._1)
    temp
  }
}
