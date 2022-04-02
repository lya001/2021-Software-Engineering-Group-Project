package aws_icl

import software.amazon.awssdk.crt.CRT
import software.amazon.awssdk.crt.io.{ClientBootstrap, EventLoopGroup, HostResolver}
import software.amazon.awssdk.crt.mqtt.{MqttClientConnection, MqttClientConnectionEvents, MqttMessage, QualityOfService}
import software.amazon.awssdk.iot.AwsIotMqttConnectionBuilder

import java.nio.charset.StandardCharsets
import scala.collection.Map
import scala.collection.parallel.CollectionConverters._
import scala.sys.process.stringSeqToProcess
import java.io.{BufferedWriter, File, FileWriter, PrintWriter}
import java.time.{LocalDateTime, ZoneId}
import akka.actor._
import aws_icl.App.close_write_file

import scala.collection.mutable.ListBuffer



object IoTManager {

  trait Operation

  case class StartSendingReq(num_things: Int) extends Operation
  case class InitializeAck() extends Operation

  case class InitializeReq(manager: ActorRef, sleep_time: Int) extends Operation
  case class SendReq() extends Operation
  case class SendAck() extends Operation
  case class SendFinished() extends Operation

  def props(fw: FileWriter, bw: BufferedWriter, out: PrintWriter): Props =
    Props(new IoTManager(fw, bw, out))
}


class IoTManager(fw: FileWriter, bw: BufferedWriter, out: PrintWriter) extends Actor {
  import IoTManager._

  var listThings: scala.collection.mutable.ListBuffer[ActorRef] = ListBuffer()

  def receive: Receive = {
    case start_sending_req: StartSendingReq =>
      if(listThings.length == start_sending_req.num_things) {
        listThings.foreach(thing => thing ! SendReq())
      } else {
        self ! StartSendingReq(start_sending_req.num_things)
      }

    case initialize_ack: InitializeAck =>
      listThings += sender
      println(s"${listThings.length} devices initialized")

    case send_ack: SendAck =>
      sender ! SendReq()

    case send_finished: SendFinished =>
      listThings -= sender
      sender ! PoisonPill
      if(listThings.isEmpty) {
        println("IoTManager finished")
        close_write_file(fw, bw, out)
        context.parent ! PoisonPill
        self ! PoisonPill
      }
  }
}


object IoTThing {
  final val TOPIC = "simUploadLocation"

  def props(list_messages: List[(String, Double, String, (Double, Double))], certificate: String, key: String, clientId: String, out: PrintWriter): Props =
    Props(new IoTThing(list_messages, certificate, key, clientId, out))
}


class IoTThing(list_messages: List[(String, Double, String, (Double, Double))], certificate: String, key: String, clientId: String, out: PrintWriter) extends Actor with IoTThings {
  import IoTThing._
  import IoTManager._

  var remainingList: List[(String, Double, String, (Double, Double))] = list_messages

  def receive: Receive = initializing()

  def initializing(): Receive = {
    case initialize_req: InitializeReq =>
      println(s"Initializing device $clientId")
      val connection = initialize_iot_connection(certificate, key, clientId, TOPIC)
      initialize_req.manager ! InitializeAck()
      context.become(sendingMessages(connection, initialize_req.sleep_time))
  }

  def sendingMessages(connection: MqttClientConnection, sleep_time: Int): Receive = {
    case send_req: SendReq =>

      if(remainingList.isEmpty) {
        println(s"Device $clientId finished")
        sender ! SendFinished()

      } else {
        val message = remainingList.head
        format_and_publish_msg(message, connection, out, sleep_time)
        remainingList = remainingList.tail
        sender ! SendAck()
      }
  }
}



trait IoTThings {

  final val example_device1: (String, String, String) = ("STT-1901",
    "./resources/certs/STT-1901/dc5d94944fb5ae285acaf30059735cd9db9cd135257f3d818fef207ec0e5216b-certificate.pem.crt",
    "./resources/certs/STT-1901/dc5d94944fb5ae285acaf30059735cd9db9cd135257f3d818fef207ec0e5216b-private.pem.key")

  final val example_device2: (String, String, String) = ("YWU-3589",
    "./resources/certs/YWU-3589/9d6d20a103fd1ba5398708bf7559c68c4c8628fcbcab7ef933fda580d82f117f-certificate.pem.crt",
    "./resources/certs/YWU-3589/9d6d20a103fd1ba5398708bf7559c68c4c8628fcbcab7ef933fda580d82f117f-private.pem.key")

  final val example_device3: (String, String, String) = ("DBK-0132",
    "./resources/certs/DBK-0132/847b61ff82d757aa9d2a60ab7cf906fa3da016675a73fbee29c33d119c0db33f-certificate.pem.crt",
    "./resources/certs/DBK-0132/847b61ff82d757aa9d2a60ab7cf906fa3da016675a73fbee29c33d119c0db33f-private.pem.key")

  final val example_device4: (String, String, String) = ("BRO-9698",
    "./resources/certs/BRO-9698/45edaf9686ec2aa46e784629e7f8a9e2089749736ebb94d43b1d404e824b4751-certificate.pem.crt",
    "./resources/certs/BRO-9698/45edaf9686ec2aa46e784629e7f8a9e2089749736ebb94d43b1d404e824b4751-private.pem.key")

  final val example_device5: (String, String, String) = ("EYS-0719",
    "./resources/certs/EYS-0719/cfb4ade8c929cf0a800402a4200a5f65d581e468928b5607e4b277bd6922ee14-certificate.pem.crt",
    "./resources/certs/EYS-0719/cfb4ade8c929cf0a800402a4200a5f65d581e468928b5607e4b277bd6922ee14-private.pem.key")

  final val example_device6: (String, String, String) = ("WBL-3982",
    "./resources/certs/WBL-3982/e9947a6b539944c4ffb59e9eb26315435bc67111ea9478b0b0ff1125e2bd5f31-certificate.pem.crt",
    "./resources/certs/WBL-3982/e9947a6b539944c4ffb59e9eb26315435bc67111ea9478b0b0ff1125e2bd5f31-private.pem.key")

  final val example_device7: (String, String, String) = ("JJJ-7632",
    "./resources/certs/JJJ-7632/b38bb237ca2ebff6bc5c08d86314336a54cab9469f21efc40c9cb5c950395074-certificate.pem.crt",
    "./resources/certs/JJJ-7632/b38bb237ca2ebff6bc5c08d86314336a54cab9469f21efc40c9cb5c950395074-private.pem.key")

  final val example_device8: (String, String, String) = ("CGB-2143",
    "./resources/certs/CGB-2143/eacd564aaa8371dbc7fd491741dd5e03f2129b2a8fb0d88bb978c205cfac79a5-certificate.pem.crt",
    "./resources/certs/CGB-2143/eacd564aaa8371dbc7fd491741dd5e03f2129b2a8fb0d88bb978c205cfac79a5-private.pem.key")

  final val example_device9: (String, String, String) = ("XHJ-1403",
    "./resources/certs/XHJ-1403/02b2aee8b628d981206874cb15de9d9364e8e4bb35a9098c7b86b7c7383cfe4d-certificate.pem.crt",
    "./resources/certs/XHJ-1403/02b2aee8b628d981206874cb15de9d9364e8e4bb35a9098c7b86b7c7383cfe4d-private.pem.key")

  final val example_device10: (String, String, String) = ("DJY-4169",
    "./resources/certs/DJY-4169/436a3526a85ab149085f3859134a3a77e7f6d20cf806bdb667436328880034fa-certificate.pem.crt",
    "./resources/certs/DJY-4169/436a3526a85ab149085f3859134a3a77e7f6d20cf806bdb667436328880034fa-private.pem.key")

  final val list_example_devices: List[(String, String, String)] =
    List(example_device1, example_device2, example_device3, example_device4, example_device5,
         example_device6, example_device7, example_device8, example_device9, example_device10)


  /**
   * Publishes and logs a message in the correct format.
   *
   * @param message the message to be published and logged
   * @param connection the AWS location connection client
   * @param out the PrintWriter where the log will be printed
   * @param sleep_time time forced between messages in the same thread
   */
  def format_and_publish_msg(message: (String, Double, String, (Double, Double)),
                             connection: MqttClientConnection, out: PrintWriter, sleep_time: Int): Unit = {
    val device_id = message._1
    val formatted_message = "{\"payload\":{\"deviceid\":\"%s\",\"timestamp\":%f,\"location\":{\"lat\":%f,\"long\":%f}}}"
                           .format(device_id, message._2, message._4._1, message._4._2)

    println(s"Device $device_id sending message $formatted_message")

    val dateTime = LocalDateTime.now()
    val zDateTime = dateTime.atZone(ZoneId.of("GMT"))
    val timestampSec = zDateTime.toInstant.toEpochMilli.toDouble

    val log_message = "{\"id\":\"%s\",\"timestamp\":%f,\"sample_time\":%f,\"coordinates\":[%f,%f]}"
      .format(device_id, message._2, timestampSec, message._4._1, message._4._2)

    val published = connection.publish(new MqttMessage(IoTThing.TOPIC, formatted_message.getBytes, QualityOfService.AT_LEAST_ONCE, false))
    out.println(log_message)

    try {
      published.get
    } catch {
      case e: Exception =>
        println(e)
    }

    Thread.sleep(sleep_time)
  }


  /**
   * Initializes the IoT connection for a given device.
   *
   * @param certificate the certificate of the device
   * @param key the key of the device
   * @param clientId the Client ID for the device
   * @param topic the topic for which the connection is established
   * @return the connection client
   */
  def initialize_iot_connection(certificate: String, key: String, clientId: String, topic: String): MqttClientConnection = {

    // Part of the code taken from the official repository of AWS IoT device SDK for Java V2:
    // https://github.com/aws/aws-iot-device-sdk-java-v2/blob/main/samples/BasicPubSub/src/main/java/pubsub/PubSub.java

    val endpoint = "aj4wz5bc28t50-ats.iot.eu-west-1.amazonaws.com"
    val port = 8883

    val callbacks = new MqttClientConnectionEvents() {
      override def onConnectionInterrupted(errorCode: Int): Unit = {
        if (errorCode != 0) System.out.println("Connection interrupted: " + errorCode + ": " + CRT.awsErrorString(errorCode))
      }
      override def onConnectionResumed(sessionPresent: Boolean): Unit = {
         System.out.println("Connection resumed: " + (if (sessionPresent) "existing session" else "clean session"))
      }
    }
    val eventLoopGroup = new EventLoopGroup(1)
    val resolver = new HostResolver(eventLoopGroup)
    val clientBootstrap = new ClientBootstrap(eventLoopGroup, resolver)

    val builder = AwsIotMqttConnectionBuilder
      .newMtlsBuilderFromPath(certificate, key)
      .withBootstrap(clientBootstrap)
      .withConnectionEventCallbacks(callbacks)
      .withClientId(clientId)
      .withEndpoint(endpoint)
      .withPort(port.toShort)
      .withCleanSession(true)
      .withProtocolOperationTimeoutMs(60000)

    val connection = builder.build
    val connected = connection.connect
    connected.get

    val subscribed = connection.subscribe(topic, QualityOfService.AT_LEAST_ONCE, (msg: MqttMessage) => {
      def foo(msg: MqttMessage): Unit = {
        val payload = new String(msg.getPayload, StandardCharsets.UTF_8)
        println("MESSAGE: " + payload)
      }
      foo(msg)
    })
    subscribed.get

    connection
  }


  /**
   * Creates an IoT client and establish a connection for a specific IoT thing. Sends a list of messages.
   *
   * @param list_messages the list of messages to be sent
   * @param certificate the certificate of the IoT device
   * @param key the private key of the IoT device
   * @param clientId the client ID of the IoT device
   * @param current_time specifies if the simulated data has to be changed to data with live timestamps
   * @param out the PrintWriter where the log will be printed
   */
  def iot_connection(list_messages: List[(String, Double, String, (Double, Double))],
                     certificate: String, key: String, clientId: String, current_time: Boolean, out: PrintWriter): Unit = {

    val connection = initialize_iot_connection(certificate, key, clientId, IoTThing.TOPIC)

    if(!current_time) {
      list_messages.foreach(message => format_and_publish_msg(message, connection, out, 500))

    } else {
      list_messages.foreach(message => {
        val timestampSec = get_current_timestamp_sec()
        val new_message = (message._1, timestampSec, message._3, message._4)
        format_and_publish_msg(new_message, connection, out, 0)
        Thread.sleep(30000)
      })
    }

    val disconnected = connection.disconnect
    disconnected.get
  }


  /**
   * Carries out a connection and publishing from a single (arbitrary) IoT device.
   *
   * @param list_messages the list of messages to be sent
   * @param current_time specifies if the simulated data has to be changed to data with live timestamps
   * @param out the PrintWriter where the log will be printed
   */
  def single_iot_connection(list_messages: List[(String, Double, String, (Double, Double))], current_time: Boolean, out: PrintWriter): Unit = {

    val certificate = "./resources/script_certs/keysAndCerts/AWL-1859/ea20186827f8812a7b8fa671d94f2f1058b2b2b3f4f0932bf6a3a190c2c2b0e0-certificate.pem.crt"
    val key = "./resources/script_certs/keysAndCerts/AWL-1859/ea20186827f8812a7b8fa671d94f2f1058b2b2b3f4f0932bf6a3a190c2c2b0e0-private.pem.key"
    val clientId = "AWL-1859"

    iot_connection(list_messages, certificate, key, clientId, current_time, out)
  }


  /**
   * Carries out a connection and publishing from several IoT devices,
   * searching the certificate and key of each device in its corresponding folder.
   *
   * @param map_messages map from IoT devices to their corresponding messages to be sent.
   * @param current_time specifies if the simulated data has to be changed to data with live timestamps
   * @param out the PrintWriter where the log will be printed
   */
  def multi_iot_connection(map_messages: Map[String, List[(String, Double, String, (Double, Double))]], current_time: Boolean, out: PrintWriter): Unit = {

    val list_devices =
      map_messages.keys.toList.par.map(device => {
        val cert_files = get_file_names_in_folder(device)
        (device, cert_files.head, cert_files(1))
      }).toList


    val number_tasks = list_devices.length

    var i = 0
    while(i < number_tasks) {
      val device = list_devices(i)
      println(s"First connecting device ${device._1}")
      iot_connection(map_messages(device._1), device._2, device._3, device._1, current_time, out)
      println(s"Finally disconnecting device ${list_devices(i)._1}")
      i += 1
    }
  }


  /**
   * Returns the current timestamp in seconds.
   *
   * @return the current timestamp in seconds
   */
  def get_current_timestamp_sec(): Double = {
    val dateTime = LocalDateTime.now()
    val zDateTime = dateTime.atZone(ZoneId.of("GMT"))
    val timestampSec = (zDateTime.toInstant.toEpochMilli/1000).toDouble

    timestampSec
  }


  /**
   * Opens a file and creates its corresponding buffers to do writing.
   *
   * @param fileName the name of the file to write
   * @param appendMode Boolean indicating the append mode for the FileWriter
   * @return the FileWriter, BufferedWriter and PrintWriter
   */
  def open_write_file(fileName: String, appendMode: Boolean): (FileWriter, BufferedWriter, PrintWriter) = {

    try {
      val fw = new FileWriter(fileName, appendMode)
      val bw = new BufferedWriter(fw)
      val out = new PrintWriter(bw)
      (fw, bw, out)

    } catch {
      case e: Exception =>
        println(e)
        (null, null, null)
    }
  }


  /**
   * Closes a file and closes its corresponding buffers to do writing.
   *
   * @param fw the FileWriter
   * @param bw the BufferedWriter
   * @param out the PrintWriter
   */
  def close_write_file(fw: FileWriter, bw: BufferedWriter, out: PrintWriter): Unit = {
    try {
      if (out != null) out.close()
      if (bw != null) bw.close()
      if (fw != null) fw.close()

    } catch {
      case e: Exception =>
        println(e)
    }
  }


  /**
   * Opens the file containing the logs.
   *
   * @return the FileWriter, BufferedWriter and PrintWriter corresponding to the log file
   */
  def open_log_file(): (FileWriter, BufferedWriter, PrintWriter, Int) = {

    val fileName = s"./resources/logs/logs_file.txt"
    val (fw, bw, out): (FileWriter, BufferedWriter, PrintWriter) = open_write_file(fileName, appendMode = false)

    if(fw == null || bw == null || out == null) {
      println("Creation of the log file encountered an error. Stopping simulation")
      close_write_file(fw, bw, out)
      (fw, bw, out, -1)
    } else {
      (fw, bw, out, 1)
    }
  }


  /**
   * Executes a Python script that creates all the necessary data for a given list of IoT things (list_devices.txt)
   *
   * @return the exit code of the execution
   */
  def execute_create_devices_script(): Int = {
    println("Creating certificates and keys for new devices.")
    val exitCode = Seq("python3", "./src/main/python/createDevice.py").!
    println("Certificates and keys generated for new devices.")
    exitCode
  }


  /**
   * Handles a file containing the name of the IoT devices present at a given "probes" simulation file.
   *
   * @param file_name name of the probes.json file where we obtain the device ids
   * @param clear_file boolean that determines whether the file containing the devices ids has to be cleared or not
   * @param number_devices_per_sim number of created devices in every simulation file
   */
  def read_write_device_names(file_name: String, clear_file: Boolean, number_devices_per_sim: Int): Int = {

    val fileName = "./resources/trip_sim/device_ids/list_devices.txt"

    val (fw, bw, out): (FileWriter, BufferedWriter, PrintWriter) = open_write_file(fileName, !clear_file)

    if(fw == null || bw == null || out == null) {
      println("Error opening the file with device names.")
      close_write_file(fw, bw, out)
    }

    if(clear_file) {
      out.print("")
      close_write_file(fw, bw, out)
      return 0
    }

    val lines_stream = os.read.lines.stream(os.pwd / "resources" / "trip_sim" / "output" / file_name)
    val lines_json = lines_stream.toList.take(number_devices_per_sim)
    val parsed_lines = lines_json.map(line => ujson.read(line)).map(line => line("properties")("id").str)

    val list_devices = scala.collection.mutable.ListBuffer.empty[String]
    var i = 0
    while(i < number_devices_per_sim) {
      list_devices += parsed_lines(i)
      i+=1
    }
    list_devices.foreach(line => out.println(line))
    close_write_file(fw, bw, out)
    i
  }


  /**
   * Handles a file containing the name of the IoT devices present at several "probes" simulation files.
   *
   * @param number_sims number of simulation files to go through
   * @param number_devices_per_sim number of devices present in every simulation file
   * @return the total number of inserted device ids
   */
  def call_read_write_device_names(number_sims: Int, number_devices_per_sim: Int): Int = {
    read_write_device_names("", clear_file = true, number_devices_per_sim)
    var total_devices = 0
    var i = 0
    while(i < number_sims) {
      total_devices += read_write_device_names(s"probes$i.json", clear_file = false, number_devices_per_sim)
      i += 1
    }

    if(total_devices != number_sims * number_devices_per_sim) println("Error: the file with the device names is incomplete.")
    println("File with device names created.")
    total_devices
  }


  /**
   * Searches for the certificate and private key names files in the specified folder.
   *
   * @param folder name where the files are located
   * @return list of strings with the names of the files
   */
  def get_file_names_in_folder(folder: String): List[String] = {

    val folder_path = s"./resources/script_certs/keysAndCerts/$folder"
    val directory = new File(folder_path)

    val end_files = List("certificate.pem.crt", "private.pem.key")

    val name_certs1_temp = directory.listFiles.filter(_.isFile).toList
    val name_certs1 = name_certs1_temp.filter(file => end_files.exists(file.getName.endsWith(_))).map(file => file.toString)
    var name_certs2 = name_certs1.sorted

    // Be careful when too many certificates and keys are present in the folder
    while(name_certs2.head.equals(name_certs2(1))) {
      name_certs2 = name_certs2.tail
    }

    name_certs2
  }

}
