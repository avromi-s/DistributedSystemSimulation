// Avromi Schneierson - 1/10/2024
package main.tasks;

import javafx.scene.control.TextArea;
import main.ClientModel;
import main.Logging;
import main.classes.Job;
import main.classes.JobReceiver;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * This Task is responsible for receiving job results from the Master and relaying them to the ClientApplication.
 */
public class ReceiveJobResultTask extends JobReceiver {
    private final ClientModel clientModel;
    private final BufferedReader socketIn;
    private final TextArea logsTextArea;

    public ReceiveJobResultTask(ClientModel clientModel, BufferedReader socketIn, TextArea logsTextArea) {
        this.clientModel = clientModel;
        this.socketIn = socketIn;
        this.logsTextArea = logsTextArea;
    }

    /**
     * Receive Job results from the Master and relay the result to the Client Application by updating its Job Result list.
     */
    @Override
    protected Void call() {
        try (socketIn) {
            while (!isCancelled()) {
                Job job = receiveJob(socketIn, logsTextArea);
                if (job == null) {
                    Logging.consoleLogAndAppendToGUILogs("Error while trying to receive job result from Master\n", logsTextArea);
                    continue;
                }
                clientModel.addJobCompleted(job);
                clientModel.removeJobRequested(job);
                Logging.consoleLogAndAppendToGUILogs("Received job result '" + job.getResult() + "' for job #" + job.getJobId() + "\n", logsTextArea);
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
