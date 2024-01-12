// Avromi Schneierson - 1/10/2024
package main.tasks;

import javafx.scene.control.TextArea;
import main.SlaveModel;
import main.Logging;
import main.classes.Job;
import main.classes.JobSender;

import java.io.PrintWriter;

/**
 * This task is responsible for sending the jobs results to the master
 */
public class SendJobResultsTask extends JobSender {
    private final SlaveModel slaveModel;
    private final PrintWriter socketOut;
    private final TextArea logsTextArea;

    public SendJobResultsTask(SlaveModel slaveModel, PrintWriter socketOut, TextArea logsTextArea) {
        this.slaveModel = slaveModel;
        this.socketOut = socketOut;
        this.logsTextArea = logsTextArea;
    }

    /**
     * Send job results to the Master. The result of a job can be an error as well.
     * Wait on the SlaveModel's jobsToSend queue and when a new Job is retrieved from there, send its results
     * to the master.
     * This method runs until cancelled or an error occurs.
     */
    @Override
    protected Void call() {
        Job jobToSend = null;
        try (socketOut) {
            while (!isCancelled()) {
                try {
                    jobToSend = slaveModel.dequeJobToSend();  // retrieve a job to send when it becomes available
                } catch (InterruptedException e) {
                    Logging.consoleLogAndAppendToGUILogs("Thread interrupted or cancelled while waiting to dequeue" +
                            "the next job to send:\n" + e.getMessage() + "\n", logsTextArea);
                    if (isCancelled()) break;
                }

                if (jobToSend != null) {
                    boolean succeeded = sendJob(jobToSend, socketOut);
                    if (succeeded) {
                        Logging.consoleLogAndAppendToGUILogs("Sent job result to master: [" + jobToSend + "]\n", logsTextArea);
                    } else {
                        Logging.consoleLogAndAppendToGUILogs(("Unable to connect to master and send job result -> " + jobToSend + "\n"), logsTextArea);
                    }
                } else {
                    Logging.consoleLogAndAppendToGUILogs("Error while trying to send Job result to master - job is null\n", logsTextArea);
                }
            }
            Logging.consoleLog("Send job result task cancelled by Slave program\n");
            socketOut.close();
            return null;
        }
    }
}