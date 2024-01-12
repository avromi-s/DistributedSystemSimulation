// Avromi Schneierson - 1/10/2024
package main.tasks;

import PacketCommunication.IPConnection;
import PacketCommunication.PacketDecoder;
import PacketCommunication.enums.PacketArgKey;
import javafx.scene.control.TextArea;
import main.MasterModel;
import main.Logging;
import main.classes.PacketReceiver;
import main.classes.Slave;
import main.enums.JobType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * This task is responsible for launching and handling the connection to a single Slave.
 * This task starts threads for both the input and output to the Slave.
 * The input is handled by the ReceiveJobResultsFromSlaveTask and the output is handled by the SendJobRequestsToSlaveTask.
 * This task joins both those input/output threads so that the socket is not closed prematurely, and once those threads
 * are no longer executing, closes the socket.
 */
public class SlaveConnectionHandlerTask extends PacketReceiver {
    private final Socket slaveSocket;
    private final MasterModel masterModel;
    private final TextArea logsTextArea;
    private final int slaveId;

    public SlaveConnectionHandlerTask(Socket slaveSocket, TextArea logsTextArea, MasterModel masterModel, int slaveId) {
        this.slaveSocket = slaveSocket;
        this.logsTextArea = logsTextArea;
        this.masterModel = masterModel;
        this.slaveId = slaveId;
    }

    @Override
    protected Void call() throws Exception {
        Logging.consoleLogAndAppendToGUILogs("creating input / output threads for slave connection...\n", logsTextArea);
        Slave slave = null;
        try (BufferedReader slaveIn = new BufferedReader(new InputStreamReader(slaveSocket.getInputStream()));
             PrintWriter slaveOut = new PrintWriter(slaveSocket.getOutputStream(), true);
        ) {
            // receive one packet to determine the slave type (what JobType it is optimized for)
            JobType slaveOptimizedForType = getSlaveOptimizedForType(slaveIn);
            if (slaveOptimizedForType == null) return null;

            // launch threads for input and output to the slave:
            slave = new Slave(slaveId, new IPConnection(slaveSocket.getInetAddress().getHostAddress(), slaveSocket.getPort()), slaveOptimizedForType);
            SendJobRequestsToSlaveTask sendJobRequestsTask = new SendJobRequestsToSlaveTask(slave, slaveOut, logsTextArea);
            ReceiveJobResultsFromSlaveTask receiveJobsTask = new ReceiveJobResultsFromSlaveTask(masterModel, slave, slaveIn, logsTextArea);
            Thread outputHandlerThread = new Thread(sendJobRequestsTask, "Slave #" + slave.getId() + " - " + sendJobRequestsTask.getClass().getName() + " (Output)");
            Thread inputHandlerThread = new Thread(receiveJobsTask, "Slave #" + slave.getId() + " - " + receiveJobsTask.getClass().getName() + " (Input)");

            outputHandlerThread.start();
            inputHandlerThread.start();
            masterModel.addActiveSlave(slave);
            Logging.consoleLogAndAppendToGUILogs("Connected to new Slave at IP '" + slaveSocket.getInetAddress().getHostAddress() + "' - ID: '" + slave.getId() + "'\n", logsTextArea);


            // Join the input/output threads so that we don't close the input/output objects before they are done with them.
            // If the connection is closed, only the input thread can detect that, so, we join the input thread and when it
            // terminates, we cancel the output thread (by cancelling its task)
            inputHandlerThread.join();
            sendJobRequestsTask.cancel();
            outputHandlerThread.join();
            masterModel.removeActiveSlave(slave);
            Logging.consoleLogAndAppendToGUILogs("Disconnected from Slave ID: '" + slave.getId() + " - slave removed from system'\n", logsTextArea);
            return null;
        } catch (Exception e) {
            if (slave != null) masterModel.removeActiveSlave(slave);
            Logging.consoleLogAndAppendToGUILogs("Error starting slave input / output threads - slave not added to system\n", logsTextArea);
            Logging.consoleLogAndAppendToGUILogs(e.getMessage() + "\n", logsTextArea);
            e.printStackTrace();
            slaveSocket.close();  // (we have to close this manually as it was declared in a separate thread that doesn't maintain a reference to it)
            return null;
        }
    }

    /**
     * @return the job type that the slave is optimized for, or <code>null</code> if an error occurred
     */
    private JobType getSlaveOptimizedForType(BufferedReader in) {
        try {
            PacketDecoder packet = receiveOnePacket(in);
            if (packet != null && packet.containsArg(PacketArgKey.OPTIMIZED_FOR_JOB_TYPE)) {
                return JobType.valueOf(packet.getArg(PacketArgKey.OPTIMIZED_FOR_JOB_TYPE));
            } else {
                Logging.consoleLogAndAppendToGUILogs("Error starting slave input / output threads - unable to determine JobType that slave is optimized for - slave not added to system\n", logsTextArea);
                return null;
            }
        } catch (Exception e) {
            Logging.consoleLogAndAppendToGUILogs("Error starting slave input / output threads - unable to determine JobType that slave is optimized for - slave not added to system\n", logsTextArea);
            Logging.consoleLogAndAppendToGUILogs(e.getMessage() + "\n", logsTextArea);
            e.printStackTrace();
            return null;
        }
    }
}
