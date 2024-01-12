// Avromi Schneierson - 1/10/2024
package main.tasks;

import javafx.scene.control.TextArea;
import main.Logging;
import main.classes.Client;
import main.classes.MasterJob;
import main.classes.JobSender;

import java.io.PrintWriter;

/**
 * This class is responsible for sending MasterJob results to a specific client, once they have been completed.
 * This task is started by the ConnectToClientsTask when a new Client connects and serves as the sole sender of output
 * to that Client.
 * The class waits on its Clients jobsCompleted queue and once a job is enqueued there, this class dequeues it and sends
 * it to the Client.
 */
public class SendJobResultsToClientTask extends JobSender {
    private final Client client;
    private final PrintWriter socketOut;
    private final TextArea logsTextArea;

    public SendJobResultsToClientTask(Client client, PrintWriter socketOut, TextArea logsTextArea) {
        this.client = client;
        this.socketOut = socketOut;
        this.logsTextArea = logsTextArea;
    }

    @Override
    protected Void call() {
        MasterJob jobToSend = null;
        try (socketOut) {
            while (!isCancelled()) {
                try {
                    jobToSend = client.dequeCompletedJob();
                } catch (InterruptedException e) {
                    Logging.consoleLog("Thread interrupted or cancelled while waiting to dequeue" +
                            "the job results to send to client:\n" + e.getMessage() + "\n");
                    if (isCancelled()) break;
                }

                if (jobToSend != null) {
                    boolean succeeded = sendJob(jobToSend, false, socketOut);

                    if (succeeded) {
                        Logging.consoleLogAndAppendToGUILogs("Sent job result to client #" + client.getId() + ": [" + jobToSend + "]\n", logsTextArea);
                    } else {
                        Logging.consoleLogAndAppendToGUILogs("Unable to send job result to client #" + client.getId() + ": [" + jobToSend + "]\n", logsTextArea);
                    }
                } else {
                    Logging.consoleLogAndAppendToGUILogs("Error while trying to send MasterJob result to client - job is null\n", logsTextArea);
                }
            }
            Logging.consoleLog("Send job result task for client #" + client.getId() + " cancelled\n");
            socketOut.close();
            return null;
        }
    }
}
