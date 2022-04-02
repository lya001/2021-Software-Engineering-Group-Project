package aws_icl

import akka.actor.{ActorRef, ActorSystem}
import aws_icl.IoTManager._
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.location.{AmazonLocation, AmazonLocationClientBuilder}
import tools.task

import java.io.{BufferedReader, BufferedWriter, FileReader, FileWriter, PrintWriter}


object App extends ExecuteSim with IoTThings with Parser with Trackers {

  final val BATCH_MAX_SIZE = 10
  final val SECONDS_RATE = 30
  final val EXAMPLE_PROBES_NAME = "probes.json"

  // NUMBER OF SIMULATIONS (PROBES.JSON CREATED) AND NUMBER OF AGENTS (DEVICES) FOR EACH SIMULATION
  final val NUMBER_SIMS = 10
  final val SIM_NUMBER_AGENTS = 20
  final val SECONDS_SIMULATION = 7200
  final val OFFSET_MULTIPLIER = SECONDS_SIMULATION + SECONDS_RATE


  def main(args : Array[String]): Unit = {

    val offset = get_and_update_offset(false) * OFFSET_MULTIPLIER

    // ---------------- CALLS TO RUN THE SIMULATION ENGINE ----------------
    // run initialize_sim_and_devices_data once (for a set of simulation parameters), then just comment it and
    // run multiple_x_test as many times as necessary (but do not forget the offset, it should not exceed 10 days)

    //initialize_sim_and_devices_data()
    multiple_actor_iot_test(NUMBER_SIMS, SIM_NUMBER_AGENTS, offset)
    //multiple_concurrent_test(iot_test, NUMBER_SIMS, offset, current_time = false) // deprecated


    // Methods to test simple sending and retrieving data.
    //val (fw, bw, out, error_code): (FileWriter, BufferedWriter, PrintWriter, Int) = open_log_file()
    //if(error_code == 1) {
    //  iot_test("probes.json", multiple_devices = false, offset, current_time = false, out)
    //  trackers_test("probes.json", multiple_devices = false, offset, current_time = false, out)
    //}
  }


  /**
   * Using the actor model, performs a publishing simulation from multiple "probes" files concurrently.
   *
   * @param number_sims number of simulation data files (probes) to be concurrently fetched
   * @param number_devices number of devices per simulation file
   * @param offset number of seconds to add to the timestamps in the simulation files
   */
  def multiple_actor_iot_test(number_sims: Int, number_devices: Int, offset: Int): Unit = {

    val (fw, bw, out, error_code): (FileWriter, BufferedWriter, PrintWriter, Int) = open_log_file()
    if(error_code != 1) return

    implicit val system: ActorSystem = ActorSystem("IoTSystem")
    val manager = system.actorOf(IoTManager.props(fw, bw, out))
    println("IoT Manager actor started")

    var actorList: List[ActorRef] = Nil
    val sleep_time: Int = 100 + 6000/number_devices

    var i: Int = 0
    while(i < number_sims) {

      val probes_name = s"probes$i.json"
      val map_devices = parse_list_device_filtered(SECONDS_RATE, probes_name, offset)
      val list_devices_data = map_devices.keys.toList.map(device => {
          val cert_files = get_file_names_in_folder(device)
          (device, cert_files.head, cert_files(1))
        })

      var j: Int = 0
      while(j < number_devices) {
        val device = list_devices_data(j)
        val device_actor = system.actorOf(IoTThing.props(map_devices(device._1), device._2, device._3, device._1, out))
        actorList = device_actor :: actorList
        device_actor ! InitializeReq(manager, sleep_time)
        j += 1
      }

      i += 1
    }

    manager ! StartSendingReq(number_sims * number_devices)
  }


  /**
   * Performs a publishing simulation from multiple "probes" files concurrently.
   *
   * @param function the test function to decide to publish to IoT core or to trackers
   * @param number_sims number of simulation data files (probes) to be concurrently fetched
   * @param offset number of seconds to add to the timestamps in the simulation files
   * @param current_time specifies if the simulated data has to be changed to data with live timestamps
   */
  def multiple_concurrent_test(function: (String, Boolean, Int, Boolean, PrintWriter) => Unit, number_sims: Int, offset: Int, current_time: Boolean): Unit = {

    val (fw, bw, out, error_code): (FileWriter, BufferedWriter, PrintWriter, Int) = open_log_file()
    if(error_code != 1) return

    val taskArray: Array[java.util.concurrent.ForkJoinTask[Unit]] = new Array(number_sims)

    var i = 0
    while(i < number_sims) {
      val idx = i
      taskArray(idx) = task(function(s"probes$idx.json", true, offset, current_time, out))
      i += 1
    }

    i = 0
    while(i < number_sims) {
      taskArray(i).join()
      i += 1
    }

    close_write_file(fw, bw, out)
  }


  /**
   * Performs a simulation of publishing to trackers.
   *
   * @param probes_name the name of the probes file, containing the simulation data
   * @param offset number of seconds to add to the timestamps in the simulation files
   */
  def trackers_test(probes_name: String, multiple_devices: Boolean, offset: Int, current_time: Boolean, out: PrintWriter): Unit = {
    // We just set an arbitrary device and tracker.
    val device_name = "STT-1901"
    val tracker_name = "sim-tracker-java1"
    val batched_lines = parse_list_batched(BATCH_MAX_SIZE, SECONDS_RATE, probes_name, offset)

    val client_builder: AmazonLocationClientBuilder =
      AmazonLocationClientBuilder.standard.withRegion(Regions.EU_WEST_1).withCredentials(new ProfileCredentialsProvider)
    val client: AmazonLocation = client_builder.build

    send_to_trackers(batched_lines, client, par = false, tracker_name)
    retrieve_from_trackers(client, device_name, tracker_name)
  }


  /**
   * Performs a simulation of publishing to IoT core: both from multiple IoT things and just from a single device.
   * 
   * @param probes_name the name of the probes file, containing the simulation data
   * @param multiple_devices boolean determining if the publishing is performed by multiple IoT devices or not
   * @param offset number of seconds to add to the timestamps in the simulation files
   * @param current_time specifies if the simulated data has to be changed to data with live timestamps
   * @param out the PrintWriter where the log will be printed
   */
  def iot_test(probes_name: String, multiple_devices: Boolean, offset: Int, current_time: Boolean, out: PrintWriter): Unit = {

    println(s"Starting IoT test for $probes_name")

    if(multiple_devices) {
      val map_devices = parse_list_device_filtered(SECONDS_RATE, probes_name, offset)
      multi_iot_connection(map_devices, current_time, out)

    } else {
      val lines = parse_list(SECONDS_RATE, probes_name, offset)
      // We just reduce the number of messages to send. We also set an arbitrary device.
      val lines2 = lines.map(message => ("AWL-1859", message._2, message._3, message._4)).take(5)
      single_iot_connection(lines2, current_time, out)
    }
    println(s"Finished IoT test for $probes_name")
  }


  /**
   * Runs the generator of simulation data, retrieves the IoT devices' names and creates the corresponding certs/keys for them.
   */
  def initialize_sim_and_devices_data(): Unit = {
    execute_sims(SIM_NUMBER_AGENTS.toString, SECONDS_SIMULATION.toString, NUMBER_SIMS)
    get_and_update_offset(true)
    call_read_write_device_names(NUMBER_SIMS, SIM_NUMBER_AGENTS)
    execute_create_devices_script()
  }


  /**
   * Gets the last registered offset and updates its value by 1, then returns the actual new offset
   * @param set_offset_to_zero boolean indicating if the offset should be directly be set to zero in the file
   *
   * @return the actual new offset, if null returns -1
   */
  def get_and_update_offset(set_offset_to_zero: Boolean): Int = {

    var last_offset = -1

    try {
      val fr = new FileReader("./resources/trip_sim/offset/last_offset.txt")
      val br = new BufferedReader(fr)

      try {
        val line = br.readLine()
        if (line != null) last_offset = line.toInt

      } catch {
        case e: Exception => println(e)
      } finally {
        br.close()
        fr.close()
      }
    } catch {
      case e: Exception =>
        println(e)
        return -1
    }

    val fileName = "./resources/trip_sim/offset/last_offset.txt"
    val (fw, bw, out): (FileWriter, BufferedWriter, PrintWriter) = open_write_file(fileName, appendMode = false)
    if(fw == null || bw == null || out == null) {
      println("Error opening the file containing the offset.")
      close_write_file(fw, bw, out)
      return -1
    }

    var new_offset = -1
    if(set_offset_to_zero) {
      new_offset = 0
    } else if(last_offset != -1) {
      new_offset = last_offset + 1
    }

    out.println(new_offset)
    val new_offset_in_days = (new_offset * OFFSET_MULTIPLIER).toDouble / (3600 * 24)
    println(s"The new offset is $new_offset_in_days days. New data has to be generated if this value is greater or equal than 10 days.")

    close_write_file(fw, bw, out)
    new_offset
  }
}
