// Avromi Schneierson - 1/10/2024
package main;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import main.classes.Job;
import main.enums.JobType;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class holds all the Job data for the Slave's core operations.
 * This includes <code><i>BlockingQueues</i></code> for the jobs requested as well as for the jobs that have
 * already been completed.
 */
public class SlaveModel {
    /**
     * Holds all the jobs requested that have not yet been executed.
     * The queue is <i>enqueued</i> as new job requests are received (handled by the ReceiveJobRequestsTask).
     * The queue is <i>dequeued</i> when a job is ready to be executed (handled by the ExecutedJobsTask).
     */
    private final LinkedBlockingQueue<Job> jobsToRunQueue = new LinkedBlockingQueue<>();
    /**
     * Holds all the jobs that have been executed, but not returned to the master yet.
     * The queue is <i>enqueued</i> as jobs are executed (handled by the ExecutedJobsTask).
     * The queue is <i>dequeued</i> when a job is ready to be sent back to the master (handled by the SendJobResultsTask).
     */
    private final LinkedBlockingQueue<Job> jobsToSendQueue = new LinkedBlockingQueue<>();
    private final ArrayList<Job> allJobsRequested = new ArrayList<>();
    private final ArrayList<Job> allJobsCompleted = new ArrayList<>();
    private final JobType slaveOptimizedForType;

    // GUI items
    private final ListView<String> pendingJobsListView, completedJobsListView;
    private final Label pendingJobsHeaderLabel, completedJobsHeaderLabel;

    public SlaveModel(JobType slaveOptimizedForType, ListView<String> pendingJobsListView,
                      ListView<String> completedJobsListView, Label pendingJobsHeaderLabel,
                      Label completedJobsHeaderLabel) {
        this.slaveOptimizedForType = slaveOptimizedForType;
        this.pendingJobsListView = pendingJobsListView;
        this.completedJobsListView = completedJobsListView;
        this.pendingJobsHeaderLabel = pendingJobsHeaderLabel;
        this.completedJobsHeaderLabel = completedJobsHeaderLabel;
    }

    public JobType getSlaveOptimizedForType() {
        return slaveOptimizedForType;
    }

    /**
     * Enqueue a requested job to be executed.
     */
    public void enqueueJobToRun(Job jobToRun) throws InterruptedException {
        jobsToRunQueue.put(new Job(jobToRun));  // synchronization not needed here because the queue is thread-safe
        synchronized (allJobsRequested) {
            allJobsRequested.add(new Job(jobToRun));
        }
        Platform.runLater(() -> {
            pendingJobsListView.getItems().add(jobToRun.toString());
            pendingJobsHeaderLabel.setText("Pending Jobs (" + pendingJobsListView.getItems().size() + ")");
        });
    }

    /**
     * Deque a requested job for execution.
     */
    public Job dequeJobToRun() throws InterruptedException {
        return jobsToRunQueue.take();
    }

    /**
     * Enqueue a completed job (with its result) to be returned to the master.
     */
    public void enqueueJobToSend(Job jobCompleted) throws InterruptedException {
        jobsToSendQueue.put(new Job(jobCompleted));
        synchronized (allJobsCompleted) {
            allJobsCompleted.add(new Job(jobCompleted));
        }
        Platform.runLater(() -> {
            completedJobsListView.getItems().add(0, jobCompleted.toString());
            pendingJobsListView.getItems().removeIf(jobStr -> jobCompleted.equals(new Job(jobStr)));
            completedJobsHeaderLabel.setText("Completed Jobs (" + completedJobsListView.getItems().size() + ")");
            pendingJobsHeaderLabel.setText("Pending Jobs" + (!pendingJobsListView.getItems().isEmpty() ? " (" +
                    pendingJobsListView.getItems().size() + ")" : ""));
        });
    }

    /**
     * Deque a completed job to be returned to the master.
     */
    public Job dequeJobToSend() throws InterruptedException {
        return jobsToSendQueue.take();
    }
}
