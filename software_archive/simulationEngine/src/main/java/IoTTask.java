import software.amazon.awssdk.crt.CRT;
import software.amazon.awssdk.crt.io.ClientBootstrap;
import software.amazon.awssdk.crt.io.EventLoopGroup;
import software.amazon.awssdk.crt.io.HostResolver;
import software.amazon.awssdk.crt.mqtt.MqttClientConnection;
import software.amazon.awssdk.crt.mqtt.MqttClientConnectionEvents;
import software.amazon.awssdk.crt.mqtt.MqttMessage;
import software.amazon.awssdk.crt.mqtt.QualityOfService;
import software.amazon.awssdk.iot.AwsIotMqttConnectionBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

public class IoTTask implements Runnable {

  private final CountDownLatch latch;
  private final IoTTaskInfo taskInfo;

  public IoTTask(IoTTaskInfo taskInfo, CountDownLatch latch) {
    this.taskInfo = taskInfo;
    this.latch = latch;
  }

  @Override
  public void run() {
    System.out.println("Running task" + taskInfo.getMessageToBePublished());
    try {
      publishToIoT();
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    System.out.println("Finished task" + taskInfo.getMessageToBePublished());
    latch.countDown();
  }

  private void publishToIoT() {
    MqttClientConnectionEvents callbacks =
        new MqttClientConnectionEvents() {
          @Override
          public void onConnectionInterrupted(int errorCode) {
            if (errorCode != 0) {
              System.out.println(
                  "Connection interrupted: " + errorCode + ": " + CRT.awsErrorString(errorCode));
            }
          }

          @Override
          public void onConnectionResumed(boolean sessionPresent) {
            System.out.println(
                "Connection resumed: " + (sessionPresent ? "existing session" : "clean session"));
          }
        };
    try (EventLoopGroup eventLoopGroup = new EventLoopGroup(1);
        HostResolver resolver = new HostResolver(eventLoopGroup);
        ClientBootstrap clientBootstrap = new ClientBootstrap(eventLoopGroup, resolver);
        AwsIotMqttConnectionBuilder builder =
            AwsIotMqttConnectionBuilder.newMtlsBuilderFromPath(
                taskInfo.getCertificate().toString(), taskInfo.getPrivateKey().toString())) {

      builder
          .withBootstrap(clientBootstrap)
          .withConnectionEventCallbacks(callbacks)
          .withClientId(taskInfo.getDeviceID())
          .withEndpoint(IoTTaskInfo.ENDPOINT)
          .withPort((short) IoTTaskInfo.PORT)
          .withCleanSession(true)
          .withProtocolOperationTimeoutMs(60000);

      try (MqttClientConnection connection = builder.build()) {

        CompletableFuture<Boolean> connected = connection.connect();
        try {
          boolean sessionPresent = connected.get();
          System.out.println(
              "Connected to " + (!sessionPresent ? "new" : "existing") + " session!");
        } catch (Exception ex) {
          throw new RuntimeException("Exception occurred during connect", ex);
        }
        CompletableFuture<Integer> published =
            connection.publish(
                new MqttMessage(
                    IoTTaskInfo.TOPIC,
                    taskInfo.getMessageToBePublished().getBytes(),
                    QualityOfService.AT_LEAST_ONCE,
                    false));
        published.get();
        Thread.sleep(1000);
        CompletableFuture<Void> disconnected = connection.disconnect();
        disconnected.get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
        throw new RuntimeException("Exception occurred");
      }
    }
  }
}
