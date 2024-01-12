// Avromi Schneierson - 1/10/2024
package main.tasks;

import PacketCommunication.IPConnection;
import javafx.concurrent.Task;
import javafx.scene.control.TextArea;
import main.MasterModel;
import main.Logging;
import main.classes.Client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * This task is responsible for launching and handling the connection to a single Client.
 * This task starts threads for both the input and output to the Client.
 * The input is handled by the ReceiveJobRequestsFromClientTask and the output is handled by the SendJobResultsToClientTask.
 * This task joins both those input/output threads so that the socket is not closed prematurely, and once those threads
 * are no longer executing, closes the socket.
 * */
public class ClientConnectionHandlerTask extends Task<Void> {
    private final Socket clientSocket;
    private final MasterModel masterModel;
    private final TextArea logsTextArea;
    private final int clientId;

    public ClientConnectionHandlerTask(Socket clientSocket, TextArea logsTextArea, MasterModel masterModel, int clientId) {
        this.clientSocket = clientSocket;
        this.logsTextArea = logsTextArea;
        this.masterModel = masterModel;
        this.clientId = clientId;
    }


    @Override
    protected Void call() throws Exception {
        Logging.consoleLogAndAppendToGUILogs("creating input / output threads for client connection...\n", logsTextArea);
        Client client = null;
        try (BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
        ) {
            // create input and output threads for communication with the client
            client = new Client(clientId, new IPConnection(clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort()));
            SendJobResultsToClientTask sendJobResultsTask = new SendJobResultsToClientTask(client, clientOut, logsTextArea);
            ReceiveJobRequestsFromClientTask receiveJobRequestsTask = new ReceiveJobRequestsFromClientTask(masterModel, client, clientIn, logsTextArea);
            Thread outputHandlerThread = new Thread(sendJobResultsTask, "Client" + client.getId() + "-Output");
            Thread inputHandlerThread = new Thread(receiveJobRequestsTask, "Client" + client.getId() + "-Input");

            // start both input and output threads
            inputHandlerThread.start();
            outputHandlerThread.start();
            masterModel.addActiveClient(client);
            Logging.consoleLogAndAppendToGUILogs("Connected to new Client at IP '" + clientSocket.getInetAddress().getHostAddress() + "' - ID: '" + client.getId() + "'\n", logsTextArea);

            // Join the input/output threads so that we don't close the input/output objects before they are done with them.
            // If the connection is closed, only the input thread can detect that, so, we join the input thread and when it
            // terminates, we cancel the output thread (by cancelling its task)
            inputHandlerThread.join();
            sendJobResultsTask.cancel();
            outputHandlerThread.join();
            masterModel.removeActiveClient(client);
            Logging.consoleLogAndAppendToGUILogs("Disconnected from Client ID: '" + client.getId() + " - removed client from system'\n", logsTextArea);
            return null;
        } catch (Exception e) {
            if (client != null) masterModel.removeActiveClient(client);
            Logging.consoleLogAndAppendToGUILogs("Error starting client input / output threads - client not added to system\n", logsTextArea);
            Logging.consoleLogAndAppendToGUILogs(e.getMessage() + "\n", logsTextArea);
            e.printStackTrace();
            clientSocket.close();  // (we have to close this manually as it was declared in a separate thread that doesn't maintain a reference to it)
            return null;
        }
    }
}
