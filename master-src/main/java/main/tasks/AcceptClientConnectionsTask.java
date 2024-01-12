// Avromi Schneierson - 1/10/2024
package main.tasks;

import javafx.concurrent.Task;
import javafx.scene.control.TextArea;
import main.MasterModel;
import main.Logging;
import main.classes.Client;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This task listens on the designated port for Clients to connect. For each Client that connects, this task starts a new
 * Thread responsible for setting up the connection to that client (ClientConnectionHandlerTask).
 */
public class AcceptClientConnectionsTask extends Task<Void> {
    private final int CLIENT_PORT_NUM = 30001;
    private final TextArea logsTextArea;
    private final MasterModel masterModel;

    public AcceptClientConnectionsTask(TextArea logsTextArea, MasterModel masterModel) {
        this.logsTextArea = logsTextArea;
        this.masterModel = masterModel;
    }

    @Override
    protected Void call() throws IOException {
        Logging.consoleLogAndAppendToGUILogs("Creating ServerSocket to accept incoming Client Connections\n", logsTextArea);
        try (ServerSocket serverSocket = new ServerSocket(CLIENT_PORT_NUM)) {
            while (!isCancelled()) {
                Socket clientSocket = null;
                try {
                    int clientId = Client.getNextAvailableClientId();
                    clientSocket = serverSocket.accept();
                    new Thread(new ClientConnectionHandlerTask(clientSocket, logsTextArea, masterModel, clientId), "Client" + clientId + "-ConnectionHandler").start();
                } catch (Exception e) {
                    Logging.consoleLogAndAppendToGUILogs("Unable to connect to slave\nEXCEPTION: see console for details", logsTextArea);
                    e.printStackTrace();
                    if (clientSocket != null && !clientSocket.isClosed()) {  // manually close socket if it's still open
                        clientSocket.close();
                    }
                }
            }
        }
        return null;
    }
}
