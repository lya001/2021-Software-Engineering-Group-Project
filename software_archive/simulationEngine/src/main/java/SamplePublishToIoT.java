import java.util.List;
import java.util.concurrent.*;

public class SamplePublishToIoT {
  public SamplePublishToIoT() {}

  public void RunSample() {
    int batchSize = 10;
    int numBatches = 10;
    ExecutorService executorService = Executors.newFixedThreadPool(4);
    ProbesParser probesParser = new ProbesParser();

    List<BatchOfIoTTasks> ioTTaskBatches = probesParser.generateBatchTasks(batchSize, numBatches);
    int currentBatchNumber = 0;
    for (BatchOfIoTTasks taskBatch : ioTTaskBatches) {
      for (IoTTask task : taskBatch.getTasks()) {
        executorService.submit(task);
      }
      try {
        System.out.println("Waiting for task completion: Batch " + currentBatchNumber);
        taskBatch.getAssociatedLatch().await();
        System.out.println("Done: Batch " + currentBatchNumber);
        currentBatchNumber++;
      } catch (InterruptedException e) {
        e.printStackTrace();
        break;
      }
    }
    executorService.shutdown();
  }
}
