package aws_icl

import tools.task

import java.io.File
import sys.process._
import scala.util.Random
import java.time._

trait ExecuteSim {

  /**
   * Executes concurrently several trip simulations.
   *
   * @param agents number of vehicles/agents to be simulated
   * @param seconds the number of seconds that the simulation will last
   * @param number_sims the number of simulations to be performed
   */
  def execute_sims(agents: String, seconds: String, number_sims: Int): Unit = {

    println("Starting executing simulation data generator.")

    val taskArray: Array[java.util.concurrent.ForkJoinTask[Unit]] = new Array(number_sims)

    val dateTime = LocalDateTime.now().minusDays(Random.between(10, 15))
    val zDateTime = dateTime.atZone(ZoneId.of("GMT"))
    val timestampMil = zDateTime.toInstant.toEpochMilli/1000 * 1000

    var i = 0
    while(i < number_sims) {
      val idx = i
      taskArray(idx) = task(execute_single_sim("car", agents, timestampMil.toString, seconds, idx.toString))
      i += 1
    }

    i = 0
    while(i < number_sims) {
      taskArray(i).join()
      i += 1
    }
    println("Simulated location data created.")
  }


  /**
   * Executes a trip simulator via shell command. The simulation is described in
   * https://github.com/sharedstreets/trip-simulator .
   *
   * @param config the type of vehicle (car,bike,scooter)
   * @param agents number of vehicles/agents to be simulated
   * @param start the start timestamp of the simulation
   * @param seconds the number of seconds that the simulation will last
   * @param num_task ID number of the simulation (performed via a Scala task)
   * @return the exit code from the shell command call
   */
  def execute_single_sim(config: String, agents: String, start: String, seconds: String, num_task: String): Int = {

    val folder_path = s"./resources/trip_sim/output"
    val directory = new File(folder_path).listFiles.filter(_.isFile).toList
    if (directory.exists(file => file.getName.endsWith(s"probes$num_task.json"))) {
      os.remove(os.pwd / "resources" / "trip_sim" / "output" / s"probes$num_task.json")
    }

    val exitCode = Seq("trip-simulator",
                      "--config", config,
                      "--pbf", "./resources/trip_sim/nashville/nash.osm.pbf",
                      "--graph", "./resources/trip_sim/nashville/nash.osrm",
                      "--agents", agents,
                      "--start", start,
                      "--seconds", seconds,
                      "--traces", f"./resources/trip_sim/output/traces$num_task.json",
                      "--probes", f"./resources/trip_sim/output/probes$num_task.json",
                      "--changes", f"./resources/trip_sim/output/changes$num_task.json",
                      "--trips", f"./resources/trip_sim/output/trips$num_task.json").!
    exitCode
  }
}
