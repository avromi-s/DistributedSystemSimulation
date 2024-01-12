// Avromi Schneierson - 1/10/2024
package main.tasks;

import javafx.concurrent.Task;
import javafx.scene.control.TextArea;
import main.MasterModel;
import main.Logging;
import main.classes.Slave;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This task listens on the designated port for Slaves to connect. For each Slave that connects, this task starts a new
 * Thread responsible for setting up the connection to that Slave (SlaveConnectionHandlerTask).
 */
public class AcceptSlaveConnectionsTask extends Task<Void> {
    private final int SLAVE_PORT_NUM = 30000;
    private final TextArea logsTextArea;
    private final MasterModel masterModel;

    public AcceptSlaveConnectionsTask(TextArea logsTextArea, MasterModel masterModel) {
        this.logsTextArea = logsTextArea;
        this.masterModel = masterModel;
    }


    @Override
    protected Void call() throws IOException {
        Logging.consoleLogAndAppendToGUILogs("Creating ServerSocket to accept incoming Slave Connections\n", logsTextArea);
        try (ServerSocket serverSocket = new ServerSocket(SLAVE_PORT_NUM)) {
            while (!isCancelled()) {
                Socket slaveSocket = null;
                try {
                    int slaveId = Slave.getNextAvailableSlaveId();
                    slaveSocket = serverSocket.accept();
                    new Thread(new SlaveConnectionHandlerTask(slaveSocket, logsTextArea, masterModel, slaveId), "Slave" + slaveId + "-ConnectionHandler").start();
                } catch (Exception e) {
                    Logging.consoleLogAndAppendToGUILogs("Unable to connect to slave\nEXCEPTION: see console for details", logsTextArea);
                    e.printStackTrace();
                    if (slaveSocket != null && !slaveSocket.isClosed()) {  // manually close socket if it's still open
                        slaveSocket.close();
                    }
                }
            }
        }
        return null;
    }
}
