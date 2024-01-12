// Avromi Schneierson - 1/10/2024
package main.tasks;

import javafx.concurrent.Task;
import javafx.scene.control.TextArea;
import main.SlaveModel;
import main.Logging;
import main.classes.Job;

import java.util.concurrent.ThreadLocalRandom;

/**
 * This class is responsible for 'executing' a job of any type. The execution runs faster if the JobType matches the type
 * of this slave.
 * */
public class ExecuteJobsTask extends Task<Void> {
    private final long PROCESSING_TIME_OPTIMIZED_MS = 2000;
    private final long PROCESSING_TIME_NOT_OPTIMIZED_MS = 10000;
    private SlaveModel slaveModel;
    private TextArea logsTextArea;
    public ExecuteJobsTask(SlaveModel slaveModel, TextArea logsTextArea) {
        this.slaveModel = slaveModel;
        this.logsTextArea = logsTextArea;
    }

    /**
     * 'Execute' the job by sleeping a different amount of time based on if this slave is optimized for the job or not
     * @return the 'result' (a randomized, positive int) of the job execution, or -1 if an error occurred
     * */
    @Override
    protected Void call() {
        Job jobToExecute = null;
        try {
            while (!isCancelled()) {
                jobToExecute = slaveModel.dequeJobToRun();  // retrieve a job to execute when it becomes available
                Logging.consoleLogAndAppendToGUILogs("Executing job #" + jobToExecute.getJobId() + " [Type " + jobToExecute.getJobType() + "]\n", logsTextArea);
                updateMessage("Executing job #" + jobToExecute.getJobId() + " [Type " + jobToExecute.getJobType() + "]...\n");

                if (jobToExecute.getJobType() == slaveModel.getSlaveOptimizedForType()) {
                    Thread.sleep(PROCESSING_TIME_OPTIMIZED_MS);
                } else {
                    Thread.sleep(PROCESSING_TIME_NOT_OPTIMIZED_MS);
                }
                updateMessage("");

                int result = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
                jobToExecute.setResult(String.valueOf(result));
                jobToExecute.setSucceeded(true);
                slaveModel.enqueueJobToSend(jobToExecute);  // put the result in the queue, wait if no space available
            }
            Logging.consoleLogAndAppendToGUILogs("Execute job task cancelled by Slave program\n", logsTextArea);
            return null;
        } catch (InterruptedException e) {
            Logging.consoleLogAndAppendToGUILogs("Error while executing job: '" + jobToExecute + "'\n", logsTextArea);
            return null;
        }
    }
}
