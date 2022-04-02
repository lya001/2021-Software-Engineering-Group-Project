import java.nio.file.Path;

public class IoTTaskInfoBuilder {
  private final String deviceID;
  private double latitude;
  private double longitude;
  private double timestamp;
  private Path credentialsFolderPath;

  private IoTTaskInfoBuilder(String deviceID) {
    this.deviceID = deviceID;
  }

  public static IoTTaskInfoBuilder newIoTTaskInfo(String deviceID) {
    return new IoTTaskInfoBuilder(deviceID);
  }

  public IoTTaskInfoBuilder withCoordinate(double latitude, double longitude){
    this.latitude = latitude;
    this.longitude = longitude;
    return this;
  }

  public IoTTaskInfoBuilder withTimestamp(double timestamp){
    this.timestamp = timestamp;
    return this;
  }

  public IoTTaskInfoBuilder withCredentialsFolder(Path credentialsFolderPath){
    this.credentialsFolderPath = credentialsFolderPath;
    return this;
  }

  public IoTTaskInfo build(){
    return new IoTTaskInfo(deviceID, timestamp, latitude, longitude, credentialsFolderPath);
  }
}
