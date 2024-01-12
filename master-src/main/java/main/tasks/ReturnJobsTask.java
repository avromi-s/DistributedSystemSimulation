// Avromi Schneierson - 1/10/2024
package main.tasks;

import javafx.concurrent.Task;
import main.MasterModel;
import main.classes.MasterJob;

/**
 * This class is responsible for returning completed jobs to the client that requested them.
 * */
public class ReturnJobsTask extends Task<Void> {
    private final MasterModel masterModel;
    public ReturnJobsTask(MasterModel masterModel) {
        this.masterModel = masterModel;
    }

    @Override
    protected Void call() throws Exception {
        while (!isCancelled()) {
            // wait on job completions from slaves to come in
            MasterJob jobCompleted = masterModel.dequeJobCompleted();

            // find the client that requested this job
            int clientId = findJobClient(jobCompleted);

            // enqueue the job with the client for return
            masterModel.getClient(clientId).enqueueCompletedJob(jobCompleted);
        }
        return null;
    }

    /**
     * @return the id of the client that originally requested the provided job
     * */
    private int findJobClient(MasterJob job) {
        return masterModel.getJob(job.getInternalId()).getClientId();
    }
}
