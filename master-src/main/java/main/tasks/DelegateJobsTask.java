// Avromi Schneierson - 1/10/2024
package main.tasks;

import javafx.concurrent.Task;
import javafx.scene.control.TextArea;
import main.Logging;
import main.MasterModel;
import main.classes.MasterJob;
import main.classes.Slave;
import main.enums.JobType;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is responsible for assigning each job to a slave, as they come in. It load balances to
 * efficiently distribute the jobs amongst the available slaves.
 */
public class DelegateJobsTask extends Task<Void> {

    private final MasterModel masterModel;
    private final TextArea logsTextArea;
    public DelegateJobsTask(MasterModel masterModel, TextArea logsTextArea) {
        this.masterModel = masterModel;
        this.logsTextArea = logsTextArea;
    }

    @Override
    protected Void call() throws Exception {
        while (!isCancelled()) {
            // wait on job requests from clients to come in
            MasterJob jobRequested = masterModel.dequeJobRequested();

            // load balance
            int slaveId = getBestSlaveForJob(jobRequested);

            if (slaveId == -1) {  // no slaves in system
                Logging.consoleLogAndAppendToGUILogs("No slaves connected - unable to process job\n", logsTextArea);
                continue;
            }

            // enqueue the job with the chosen slave for execution
            masterModel.getSlave(slaveId).enqueueJobToRun(jobRequested);
        }
        return null;
    }

     /**
     * This method finds the best Slave to assign the given job, considering overall system load and
     * the JobType of the provided job. This method first iterates through all slaves connected to the master to find
     * the slave with the lowest overall load for each (slave-optimized-for) JobType. Then, this method simulates
     * assigning this job to each of those slaves and return the slave that would result in the overall lowest resulting
     * total load.
     * @return the id of the slave that is best suited to complete the given job
     */
    private int getBestSlaveForJob(MasterJob job) {
        Map<Integer, Slave> slaves = masterModel.getSlaves();
        HashMap<JobType, Slave> lowestLoadSlavesByJobType = new HashMap<>();

        // go through all the slaves and find the ones with the lowest load for each job type that there are slaves for
        for (Slave slave : slaves.values()) {
            int slaveLoad = slave.getTotalLoad();
            Slave lowestLoadSlaveOfSameType = lowestLoadSlavesByJobType.get(slave.getSlaveOptimizedForType());
            if (lowestLoadSlaveOfSameType == null || slaveLoad < lowestLoadSlaveOfSameType.getTotalLoad()) {
                lowestLoadSlavesByJobType.put(slave.getSlaveOptimizedForType(), slave);
            }
        }

        // simulate adding this job to each slave and see which one gives the overall lowest time-to-completion for the
        // resulting total load
        int lowestTotalLoadCompletionTime = Integer.MAX_VALUE;
        Slave bestSlave = null;
        for (Slave slave : lowestLoadSlavesByJobType.values()) {
            int thisSlaveTotalLoadCompletionTime = slave.getTotalLoad() + slave.getJobProcessTime(job);
            if (thisSlaveTotalLoadCompletionTime <= lowestTotalLoadCompletionTime) {
                lowestTotalLoadCompletionTime = thisSlaveTotalLoadCompletionTime;
                bestSlave = slave;
            }
        }

        if (bestSlave != null) {
            return bestSlave.getId();
        } else {
            // no bestSlave was found which means there are no slaves in the system. return -1 to indicate such
            return -1;
        }
    }
}
