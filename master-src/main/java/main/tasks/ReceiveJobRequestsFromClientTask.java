// Avromi Schneierson - 1/10/2024
package main.tasks;

import javafx.scene.control.TextArea;
import main.MasterModel;
import main.Logging;
import main.classes.Client;
import main.classes.MasterJob;
import main.classes.JobReceiver;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * This Task is responsible for listening for MasterJob requests from a specific Client sent over the Client's socket.
 * This Task is started by the ConnectToClientsTask when a new Client connects and serves as the sole receiver of input
 * from the Client.
 * When a job is received by this Task, it enqueues it with the master for execution
 */
public class ReceiveJobRequestsFromClientTask extends JobReceiver {
    private final MasterModel masterModel;
    private final BufferedReader socketIn;
    private final TextArea logsTextArea;
    private final Client client;

    public ReceiveJobRequestsFromClientTask(MasterModel masterModel, Client client, BufferedReader socketIn, TextArea logsTextArea) {
        this.masterModel = masterModel;
        this.client = client;
        this.socketIn = socketIn;
        this.logsTextArea = logsTextArea;
    }

    @Override
    protected Void call() throws Exception {
        try (socketIn) {
            while (!isCancelled()) {
                MasterJob job = receiveJob(socketIn, false, logsTextArea);
                if (job == null) {
                    Logging.consoleLogAndAppendToGUILogs("Error while trying to receive job request from client #"
                            + client.getId() + "\n", logsTextArea);
                    continue;
                }
                job.setClientId(client.getId());  // add the client for this job

                try {
                    masterModel.enqueueJobRequested(job);
                    client.addJobRequested(job);
                    Logging.consoleLogAndAppendToGUILogs("Received job request from client for job - " + job + "\n", logsTextArea);
                } catch (InterruptedException e) {
                    Logging.consoleLogAndAppendToGUILogs("Thread interrupted or cancelled while waiting to enqueue" +
                            "a client's requested job:\n" + e.getMessage() + "\n", logsTextArea);
                }
            }
        } catch (IOException e) {
            Logging.consoleLog("Error while trying to receive job request from client #"
                    + client.getId() + "\n");
            //e.printStackTrace();
            return null;
        }
        return null;
    }
}
