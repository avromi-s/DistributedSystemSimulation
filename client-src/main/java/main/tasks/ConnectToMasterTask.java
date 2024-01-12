// Avromi Schneierson 1/10/2024
package main.tasks;

import PacketCommunication.IPConnection;
import javafx.concurrent.Task;
import javafx.scene.control.TextArea;
import main.ClientModel;
import main.Logging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * This task is responsible for establishing a connection to the Master and then launching a thread to handle the input
 * and output with the Master. This task joins both those threads so as not to close the socket before they are done.
 */
public class ConnectToMasterTask extends Task<Void> {
    private final IPConnection ipConnection;
    private final TextArea logsTextArea;
    private final ClientModel clientModel;
    private ReceiveJobResultTask receiveJobResultTask;
    private SendJobRequestTask sendJobRequestTask;
    private Thread receiveJobResultThread;
    private Thread sendJobRequestsThread;

    public ConnectToMasterTask(IPConnection ipConnection, ClientModel clientModel, TextArea logsTextArea) {
        this.ipConnection = ipConnection;
        this.clientModel = clientModel;
        this.logsTextArea = logsTextArea;
    }

    /**
     * Connect to the Master and launch the input and output threads. Join those threads so that the socket is open until
     * they are done. If this task is cancelled, cancel the child tasks as well.
     */
    @Override
    protected Void call() {
        updateMessage("Connecting to Master...");
        Logging.consoleLogAndAppendToGUILogs("Connecting to Master...\n", logsTextArea);
        try (Socket socket = new Socket(ipConnection.getIp(), ipConnection.getPort());
             PrintWriter masterOut = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader masterIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ) {
            updateMessage("Connected to Master");
            Logging.consoleLogAndAppendToGUILogs("Connected to Master\nReady to receive packet\n", logsTextArea);

            // launch threads for input and output to the master:
            sendJobRequestTask = new SendJobRequestTask(clientModel, masterOut, logsTextArea);
            receiveJobResultTask = new ReceiveJobResultTask(clientModel, masterIn, logsTextArea);
            sendJobRequestsThread = new Thread(sendJobRequestTask, "Thread-SendJobRequests");
            receiveJobResultThread = new Thread(receiveJobResultTask, "Thread-ReceiveJobResults");

            sendJobRequestsThread.start();
            Logging.consoleLogAndAppendToGUILogs("launched Thread to connect to Master and send Job requests\n", logsTextArea);
            receiveJobResultThread.start();
            Logging.consoleLogAndAppendToGUILogs("launched Thread to connect to Master and receive Job results\n", logsTextArea);

            // Join the input/output threads so that we don't close the input/output objects before they are done with them.
            // If the connection is closed, only the input thread can detect that, so, we join the input thread and when it
            // terminates, we cancel the output thread (by cancelling its task)
            receiveJobResultThread.join();
            sendJobRequestTask.cancel();
            sendJobRequestsThread.join();
            Logging.consoleLogAndAppendToGUILogs("Disconnected from master'\n", logsTextArea);
            updateMessage("Disconnected from Master");
        } catch (IOException e) {
            updateMessage("Unable to connect to Master. Please try again");
            Logging.consoleLogAndAppendToGUILogs("Unable to connect to master and receive job request. " +
                    "Please try again\n" + "EXCEPTION: exception while listening on port " + ipConnection.getPort()
                    + " or listening for a connection\n", logsTextArea);
            e.printStackTrace();
        } catch (InterruptedException e) {
            updateMessage("Disconnected from master");
            // if this thread/task is interrupted, then cancel all child thread/tasks
            if (!sendJobRequestTask.cancel()) {
                Logging.consoleLogAndAppendToGUILogs("unable to cancel Thread '" + sendJobRequestsThread.getName() + "'\n", logsTextArea);
            }
            if (!receiveJobResultTask.cancel()) {
                Logging.consoleLogAndAppendToGUILogs("unable to cancel Thread '" + receiveJobResultThread.getName() + "'\n", logsTextArea);
            }

            if (!isCancelled()) {  // if not cancelled, then an error occurred, log it
                Logging.consoleLogAndAppendToGUILogs("Disconnected from master. Interrupted while waiting for Thread '"
                        + sendJobRequestsThread.getName() + "' or Thread '" + receiveJobResultThread.getName()
                        + "'\nException: " + e.getMessage() + "\n", logsTextArea);
                e.printStackTrace();
            }
        }
        return null;
    }
}