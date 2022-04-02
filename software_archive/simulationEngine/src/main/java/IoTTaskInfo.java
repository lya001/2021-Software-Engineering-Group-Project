import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IoTTaskInfo {
  public static final String ROOTCaPATH = "";
  public static final String TOPIC = "testUploadLocation";
  public static final int PORT = 8883;
  public static final String ENDPOINT = "aj4wz5bc28t50-ats.iot.eu-west-1.amazonaws.com";

  private final String deviceID;
  private final Double timestamp;
  private final Double latitude;
  private final Double longitude;
  private final Path credentialsFolder;

  public IoTTaskInfo(
      String deviceID,
      Double timestamp,
      Double latitude,
      Double longitude,
      Path credentialsFolder) {
    this.deviceID = deviceID;
    this.timestamp = timestamp;
    this.latitude = latitude;
    this.longitude = longitude;
    this.credentialsFolder = credentialsFolder;
  }

  public String getDeviceID() {
    return deviceID;
  }

  public Double getTimestamp() {
    return timestamp;
  }

  public Double getLatitude() {
    return latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public String getMessageToBePublished() {
    return generatePayloadMessage(getDeviceID(), getTimestamp(), getLatitude(), getLongitude());
  }

  public Path getCredentialsFolder() {
    return credentialsFolder;
  }

  public Path getCertificate() {
    try {
      Stream<Path> matches =
          Files.find(
              credentialsFolder,
              1,
              (path, basicFileAttributes) ->
                  path.getFileName().toString().endsWith("certificate.pem.crt"));
      return matches.collect(Collectors.toList()).get(0);
    } catch (IOException e) {
      throw new RuntimeException("IOException occurred in finding the certificate");
    } catch (IndexOutOfBoundsException e) {
      throw new RuntimeException("Certificate Not found for this device");
    }
  }

  public Path getPrivateKey() {
    try {
      Stream<Path> matches =
          Files.find(
              credentialsFolder,
              1,
              (path, basicFileAttributes) ->
                  path.getFileName().toString().endsWith("private.pem.key"));
      return matches.collect(Collectors.toList()).get(0);
    } catch (IOException e) {
      throw new RuntimeException("IOException occurred in finding the certificate");
    } catch (IndexOutOfBoundsException e) {
      throw new RuntimeException("Certificate Not found for this device");
    }
  }

  public String generatePayloadMessage(
      String deviceID, Double timestamp, Double latitude, Double longitude) {

    return String.format(
        "{\"payload\":{\"deviceid\":\"%s\",\"timestamp\":%f,\"location\":{\"lat\":%f,\"long\":%f}}}",
        deviceID, timestamp, latitude, longitude);
  }
}
