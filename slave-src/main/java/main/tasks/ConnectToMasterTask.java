// Avromi Schneierson - 1/10/2024
package main.tasks;

import PacketCommunication.IPConnection;
import PacketCommunication.PacketEncoder;
import PacketCommunication.enums.PacketArgKey;
import javafx.scene.control.TextArea;
import main.SlaveModel;
import main.Logging;
import main.classes.PacketSender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * This task is responsible for establishing a connection to the Master and then launching a thread to handle the input
 * and output with the Master. This task joins both those threads so as not to close the socket before they are done.
 */
public class ConnectToMasterTask extends PacketSender {
    private final IPConnection ipConnection;
    private final TextArea logsTextArea;
    private final SlaveModel slaveModel;
    private ReceiveJobRequestsTask receiveJobRequestsTask;
    private SendJobResultsTask sendJobResultsTask;
    private Thread receiveJobRequestThread;
    private Thread sendJobResultsThread;

    public ConnectToMasterTask(IPConnection ipConnection, SlaveModel slaveModel, TextArea logsTextArea) {
        this.ipConnection = ipConnection;
        this.slaveModel = slaveModel;
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

            notifyMasterOfSlaveType(masterOut);

            // launch threads for input and output to the master:
            receiveJobRequestsTask = new ReceiveJobRequestsTask(slaveModel, masterIn, logsTextArea);
            sendJobResultsTask = new SendJobResultsTask(slaveModel, masterOut, logsTextArea);
            receiveJobRequestThread = new Thread(receiveJobRequestsTask, "Thread-ReceiveJobRequests");
            sendJobResultsThread = new Thread(sendJobResultsTask, "Thread-SendJobResults");

            receiveJobRequestThread.start();
            Logging.consoleLogAndAppendToGUILogs("launched Thread to connect to Master and receive Job Requests\n", logsTextArea);
            sendJobResultsThread.start();
            Logging.consoleLogAndAppendToGUILogs("launched Thread to connect to Master and send Job results\n", logsTextArea);

            // Join the input/output threads so that we don't close the input/output objects before they are done with them.
            // If the connection is closed, only the input thread can detect that, so, we join the input thread and when it
            // terminates, we cancel the output thread (by cancelling its task)
            receiveJobRequestThread.join();
            sendJobResultsTask.cancel();
            sendJobResultsThread.join();
            Logging.consoleLogAndAppendToGUILogs("Disconnected from master'\n", logsTextArea);
            updateMessage("Disconnected from Master");
            return null;
        } catch (IOException e) {
            updateMessage("Unable to connect to Master. Please try again");
            Logging.consoleLogAndAppendToGUILogs("Unable to connect to master and receive job request. " +
                    "Please try again\n" + "EXCEPTION: exception while listening on port " + ipConnection.getPort()
                    + " or listening for a connection\n", logsTextArea);
            e.printStackTrace();
        } catch (InterruptedException e) {
            updateMessage("Disconnecting from master");
            // if this thread/task is interrupted, then cancel all child thread/tasks
            if (!sendJobResultsTask.cancel()) {
                Logging.consoleLogAndAppendToGUILogs("unable to cancel Thread '" + sendJobResultsThread.getName() + "'\n", logsTextArea);
            }
            if (!receiveJobRequestsTask.cancel()) {
                Logging.consoleLogAndAppendToGUILogs("unable to cancel Thread '" + receiveJobRequestThread.getName() + "'\n", logsTextArea);
            }

            if (!isCancelled()) {  // if not cancelled, then an error occurred, log it
                Logging.consoleLogAndAppendToGUILogs("Disconnected from master. Interrupted while waiting for Thread '"
                        + receiveJobRequestThread.getName() + "' or Thread '" + sendJobResultsThread.getName()
                        + "'\nException: " + e.getMessage() + "\n", logsTextArea);
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Send a packet to notify the master of this slave's type
     * @return <code>true</code> if successful, else <code>false</code>
     * */
    private boolean notifyMasterOfSlaveType(PrintWriter masterOut) {
        PacketEncoder packetEncoder = new PacketEncoder();
        packetEncoder.setArg(PacketArgKey.OPTIMIZED_FOR_JOB_TYPE, slaveModel.getSlaveOptimizedForType().toString());
        return sendPacket(masterOut, packetEncoder);
    }
}