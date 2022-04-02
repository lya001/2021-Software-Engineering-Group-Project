import java.io.BufferedReader;
import com.google.gson.*;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProbesParser {
  private final String pathToProbesJson;

  public ProbesParser() {
    this("resources/trip_sim/output/probes0.json");
  }

  public ProbesParser(String pathToProbesJson) {
    this.pathToProbesJson = pathToProbesJson;
  }

  /**
   * Read the next few lines and generate corresponding tasks
   *
   * @return a list of tasks for current iteration
   */
  public List<BatchOfIoTTasks> generateBatchTasks(int batchSize, int numBatches) {
    List<BatchOfIoTTasks> ioTTaskBatches = new ArrayList<>();
    try {
      FileReader fr = new FileReader(pathToProbesJson);
      BufferedReader br = new BufferedReader(fr);
      Random rand = new Random(System.currentTimeMillis());
      Double offset = rand.nextDouble();
      for (int i = 0; i < numBatches; i++) {
        BatchOfIoTTasks currentBatch = parseOneBatch(br, batchSize, offset);
        ioTTaskBatches.add(currentBatch);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
    return ioTTaskBatches;
  }

  public IoTTaskInfo parseOneLineToTaskInfo(String line, Double offset) {
    // Sample line
    // {"type":"Feature","properties":{"id":"ALH-9251","time":1634131646000,"status":"idling"},"geometry":{"type":"Point","coordinates":[-86.79301074115749,36.185062345208706]}}

    // We can use the existing solution, which is more comprehensive and realistic, to do the
    // parsing.
    // For now, a simplistic way is used.

    // Parse with Gson.
    // Tutorial: https://attacomsian.com/blog/gson-read-write-json
    Map<?, ?> content = new Gson().fromJson(line, Map.class);
    String deviceID = (String) ((Map<?, ?>) content.get("properties")).get("id");
    Double timestamp = (Double) ((Map<?, ?>) content.get("properties")).get("time") + offset;
    List<Double> coordinate =
        (List<Double>) ((Map<?, ?>) content.get("geometry")).get("coordinates");
    Double latitude = coordinate.get(0);
    Double longitude = coordinate.get(1);

    // Alternative way of parsing using class
    ProbesJsonLine probesJsonLine = new Gson().fromJson(line, ProbesJsonLine.class);

    Path credentialsFolder = findDeviceCredentialFolder(deviceID);
    return IoTTaskInfoBuilder
        .newIoTTaskInfo(deviceID)
        .withTimestamp(timestamp)
        .withCoordinate(latitude, longitude)
        .withCredentialsFolder(credentialsFolder)
        .build();
  }

  public Path findDeviceCredentialFolder(String deviceID) {
    Path keysAndCertsPath = Paths.get("resources", "script_certs", "keysAndCerts");
    try {
      Stream<Path> matches =
          Files.find(keysAndCertsPath, 1, (path, basicFileAttributes) -> path.endsWith(deviceID));
      return matches.collect(Collectors.toList()).get(0);
    } catch (IOException e) {
      throw new RuntimeException("IOException occurred in finding the folder");
    } catch (IndexOutOfBoundsException e) {
      throw new RuntimeException("Credentials Not found for this device");
    }
  }

  private BatchOfIoTTasks parseOneBatch(BufferedReader br, int batchSize, Double offset)
      throws IOException {
    List<IoTTask> tasks = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(batchSize);

    for (int i = 0; i < batchSize; i++) {
      String line = br.readLine();
      IoTTaskInfo taskInfo = parseOneLineToTaskInfo(line, offset);
      IoTTask task = new IoTTask(taskInfo, latch);
      tasks.add(task);
    }
    return new BatchOfIoTTasks(tasks, latch);
  }

  static class ProbesJsonLine {
    // Sample line
    // {"type":"Feature",
    // "properties":{"id":"ALH-9251",
    //               "time":1634131646000,
    //               "status":"idling"},
    // "geometry":{"type":"Point",
    //             "coordinates":[-86.79301074115749,36.185062345208706]}
    // }

    String type;
    ProbesJsonLineProperty properties;

    static class ProbesJsonLineProperty {
      String id;
      Double time;
      String status;
    }

    ProbesJsonLineGeometry geometry;

    static class ProbesJsonLineGeometry {
      String type;
      List<Double> coordinates;
    }
  }
}
