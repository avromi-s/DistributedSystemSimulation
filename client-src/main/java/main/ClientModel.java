// Avromi Schneierson - 1/10/2024
package main;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import main.classes.Job;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class holds all the Job data for the Client's core operations.
 * This includes <code><i>BlockingQueues</i></code> for the jobs requested as well as for the jobs that have
 * already been completed.
 */
public class ClientModel {
    private final Object lastJobIdLock = new Object();
    private int lastJobId = -1;

    /**
     * Holds all the jobs requested by the user that have not yet been requested from the master.
     * The queue is <i>enqueued</i> as new jobs are requested by the user (handled by the ClientController).
     * The queue is <i>dequeued</i> when a job is ready to be requested from the master (handled by the SendJobRequestTask).
     */
    private final LinkedBlockingQueue<Job> jobsToRequest = new LinkedBlockingQueue<>();
    private final ArrayList<Job> allJobsRequested = new ArrayList<>();
    private final ArrayList<Job> allJobsCompleted = new ArrayList<>();

    // GUI items
    private final ListView<String> pendingJobsListView, completedJobsListView;
    private final Label pendingJobsHeaderLabel, completedJobsHeaderLabel;

    public ClientModel(ListView<String> pendingJobsListView, ListView<String> completedJobsListView,
                       Label pendingJobsHeaderLabel, Label completedJobsHeaderLabel) {
        this.pendingJobsListView = pendingJobsListView;
        this.completedJobsListView = completedJobsListView;
        this.pendingJobsHeaderLabel = pendingJobsHeaderLabel;
        this.completedJobsHeaderLabel = completedJobsHeaderLabel;
    }

    /**
     * Enqueue a job to be requested from the master
     * */
    public void enqueueJobToRequest(Job jobToRun) throws InterruptedException {
        jobsToRequest.put(new Job(jobToRun));  // synchronization not needed here because the queue is thread-safe
        synchronized (allJobsRequested) {
            allJobsRequested.add(new Job(jobToRun));
        }
        Platform.runLater(() -> {
            pendingJobsListView.getItems().add(jobToRun.toString());
            pendingJobsHeaderLabel.setText("Pending Jobs (" + pendingJobsListView.getItems().size() + ")");
        });
    }

    /**
     * Get the next job to be requested from the master
     */
    public Job dequeJobToRequest() throws InterruptedException {
        return jobsToRequest.take();  // synchronization not needed here because the queue is thread-safe
    }

    /**
     * Remove a job requested that is now completed
     * */
    public void removeJobRequested(Job job) {
        synchronized (allJobsRequested) {
            allJobsRequested.remove(job);
        }
        Platform.runLater(() -> {
            pendingJobsListView.getItems().removeIf(jobStr -> job.equals(new Job(jobStr)));
            pendingJobsHeaderLabel.setText("Pending Jobs" + (!pendingJobsListView.getItems().isEmpty() ? " (" +
                    pendingJobsListView.getItems().size() + ")" : ""));
        });
    }

    /**
     * Add a completed job to the collection and update the GUI
     * */
    public void addJobCompleted(Job job) {
        synchronized (allJobsCompleted) {
            allJobsCompleted.add(job);
        }
        Platform.runLater(() -> {
            completedJobsListView.getItems().add(0, job.toString());
            completedJobsHeaderLabel.setText("Completed Jobs (" + completedJobsListView.getItems().size() + ")");
        });
    }

    /**
     * @return the next job Id available
     * */
    public int getNextJobId() {
        synchronized (lastJobIdLock) {
            return ++lastJobId;
        }
    }
}
