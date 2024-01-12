// Avromi Schneierson - 1/10/2024
package main;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import main.classes.Client;
import main.classes.Slave;
import main.tasks.AcceptClientConnectionsTask;
import main.tasks.AcceptSlaveConnectionsTask;
import main.tasks.DelegateJobsTask;
import main.tasks.ReturnJobsTask;

/**
 * The controller for the Master application. This class is responsible for handling all GUI events and launching the
 * initial threads.
 * <ul>
 *     <li>
 *         On Master startup, 4 threads are started:
 *         <ol>
 *             <li>
 *                 AcceptClientConnectionsTask - responsible for accepting and setting up new client connections.
 *             </li>
 *             <li>
 *                 AcceptSlaveConnectionsTask - responsible for accepting and setting up new slave connections.
 *             </li>
 *             <li>
 *                 DelegateJobsTask - responsible for delegating the job requests amongst the available slaves (load balances)
 *             </li>
 *             <li>
 *                 ReturnJobsTask - responsible for returning completed jobs to the clients that originally requested them
 *             </li>
 *         </ol>
 *     </li>
 *
 *     <li>
 *         For each connected client, there is an input thread responsible for receiving job requests
 *         (ReceiveJobRequestsFromClientTask), and an output thread responsible for sending the job results
 *         (SendJobResultsToClientTask).
 *     </li>
 *     <li>
 *         For each connected slave, there is an output thread responsible for sending the job requests
 *         (SendJobRequestsToSlaveTask), and an input thread responsible for receiving job results
 *         (ReceiveJobResultsFromSlaveTask).
 *     </li>
 * </ul>
 * */
public class MasterController {
    private MasterModel masterModel;  // stores all data for the Master's operations

    // JavaFx controls:
    @FXML
    public Label statusLabel;
    @FXML
    public Label clientsHeaderLabel;
    @FXML
    public Label slavesHeaderLabel;
    @FXML
    public Button startupButton;
    @FXML
    public TextArea statusLogsTextArea;
    @FXML
    public ListView<Client> clientsListView;
    @FXML
    public ListView<Slave> slavesListView;

    @FXML
    public void initialize() {
        masterModel = new MasterModel(clientsListView, slavesListView, statusLabel, clientsHeaderLabel, slavesHeaderLabel);
    }

    @FXML
    public void startup() {
        Logging.consoleLogAndAppendToGUILogs("Starting up Master...\n", statusLogsTextArea);
        Thread connectToClients = new Thread(new AcceptClientConnectionsTask(statusLogsTextArea, masterModel), "Thread-ConnectToClients");
        Thread connectToSlaves = new Thread(new AcceptSlaveConnectionsTask(statusLogsTextArea, masterModel), "Thread-ConnectToSlaves");
        Thread delegateJobs = new Thread(new DelegateJobsTask(masterModel, statusLogsTextArea), "Thread-DelegateJobs");
        Thread returnJobs = new Thread(new ReturnJobsTask(masterModel), "Thread-ReturnJobs");

        connectToClients.setDaemon(true);
        connectToSlaves.setDaemon(true);
        delegateJobs.setDaemon(true);
        returnJobs.setDaemon(true);

        connectToClients.start();
        connectToSlaves.start();
        delegateJobs.start();
        returnJobs.start();

        startupButton.setDisable(true);
    }
}