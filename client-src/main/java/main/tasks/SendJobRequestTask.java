// Avromi Schneierson - 1/10/2024
package main.tasks;

import javafx.scene.control.TextArea;
import main.ClientModel;
import main.Logging;
import main.classes.Job;
import main.classes.JobSender;

import java.io.PrintWriter;

/**
 * This task is responsible for sending the job requests to the master
 */
public class SendJobRequestTask extends JobSender {
    private final ClientModel clientModel;
    private final PrintWriter socketOut;
    private final TextArea logsTextArea;

    public SendJobRequestTask( ClientModel clientModel, PrintWriter socketOut, TextArea logsTextArea) {
        this.clientModel = clientModel;
        this.socketOut = socketOut;
        this.logsTextArea = logsTextArea;
    }


    /**
     * Request a job from the master by sending a packet with the job details to the master.
     * Wait on the ClientModel's jobsToRequest queue and when a new Job is (requested and enqueued) there, dequeue
     * it and request it from the master.
     * This method runs until cancelled or an error occurs.
     */
    @Override
    protected Void call() {
        Job jobToSend = null;
        try (socketOut) {
            while (!isCancelled()) {
                try {
                    jobToSend = clientModel.dequeJobToRequest();  // retrieve a job to request when it becomes available
                } catch (InterruptedException e) {
                    Logging.consoleLogAndAppendToGUILogs("Thread interrupted or cancelled while waiting to dequeue" +
                            "the next job to request:\n" + e.getMessage() + "\n", logsTextArea);
                }

                if (jobToSend != null) {
                    boolean succeeded = sendJob(jobToSend, socketOut);
                    if (succeeded) {
                        Logging.consoleLogAndAppendToGUILogs("Sent job request to master: [" + jobToSend + "]\n", logsTextArea);
                    } else {
                        Logging.consoleLogAndAppendToGUILogs(("Unable to connect to master and send job request -> " + jobToSend + "\n"), logsTextArea);
                    }
                } else {
                    Logging.consoleLogAndAppendToGUILogs("Error while trying to send job request to master - job is null\n", logsTextArea);
                }
            }
            Logging.consoleLogAndAppendToGUILogs("Send job request task cancelled by Client program\n", logsTextArea);
            socketOut.close();
            return null;
        }
    }
}
