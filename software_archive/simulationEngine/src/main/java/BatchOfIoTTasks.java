import java.util.List;
import java.util.concurrent.CountDownLatch;

public class BatchOfIoTTasks {
  private final List<IoTTask> tasks;
  private final CountDownLatch associatedLatch;

  public BatchOfIoTTasks(List<IoTTask> tasks, CountDownLatch associatedLatch) {
    this.tasks = tasks;
    this.associatedLatch = associatedLatch;
  }

  public List<IoTTask> getTasks() {
    return tasks;
  }

  public CountDownLatch getAssociatedLatch() {
    return associatedLatch;
  }
}
