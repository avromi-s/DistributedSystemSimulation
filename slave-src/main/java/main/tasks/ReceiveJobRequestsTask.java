// Avromi Schneierson - 1/10/2024
package main.tasks;

import javafx.scene.control.TextArea;
import main.SlaveModel;
import main.Logging;
import main.classes.Job;
import main.classes.JobReceiver;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * This class is responsible for receiving a job to complete from the master and enqueueing it in the SlaveModel for eventual
 * execution.
 */
public class ReceiveJobRequestsTask extends JobReceiver {
    private final SlaveModel slaveModel;
    private final BufferedReader socketIn;
    private final TextArea logsTextArea;

    public ReceiveJobRequestsTask(SlaveModel slaveModel, BufferedReader socketIn, TextArea logsTextArea) {
        this.slaveModel = slaveModel;
        this.socketIn = socketIn;
        this.logsTextArea = logsTextArea;
    }

    /**
     * Receive job requests from the master, and enqueue them to be run by adding them to the provided SlaveModel's queue
     */
    @Override
    protected Void call() {
        try (socketIn) {
            while (!isCancelled()) {
                Job job = receiveJob(socketIn, logsTextArea);
                if (job == null) {
                    Logging.consoleLogAndAppendToGUILogs("Error while trying to receive job request from master\n", logsTextArea);
                    continue;
                }

                // Enqueue the job to be run
                try {
                    slaveModel.enqueueJobToRun(job);
                    Logging.consoleLogAndAppendToGUILogs("Enqueued job: '" + job + "' to run\n", logsTextArea);
                } catch (InterruptedException e) {
                    Logging.consoleLogAndAppendToGUILogs("Thread interrupted or cancelled while waiting to" +
                            " enqueue job '" + job + "' the next job to run:\n" + e.getMessage() + "\n", logsTextArea);
                }
            }

            // If the input stream is closed that means we lost connection from the master
            updateMessage("Lost connection to master");
            Logging.consoleLogAndAppendToGUILogs("Lost connection to Master\n", logsTextArea);
            return null;
        } catch (IOException e) {
            Logging.consoleLogAndAppendToGUILogs("Unable to connect to master and receive job request. " +
                    "Please try again\n" + "EXCEPTION: " + e.getMessage() + "\n", logsTextArea);
            e.printStackTrace();
            return null;
        }
    }
}
