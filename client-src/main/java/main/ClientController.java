// Avromi Schneierson - 1/10/2024
package main;

import PacketCommunication.IPConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import main.tasks.ConnectToMasterTask;
import main.classes.Job;
import main.enums.JobType;

/**
 * This class is responsible for handling all the GUI events for the Client application, including launching the
 * background tasks to both request Jobs and retrieve their results with the help of the Client class
 */
public class ClientController {
    private final int PORT_NUM = 30001;   // clients communicate on a different port number than slaves so that they don't interfere
    private IPConnection ipConnection;
    private ClientModel clientModel;
    private ConnectToMasterTask connectToMasterTask;

    // JavaFx controls:
    @FXML
    public Label statusLabel;
    @FXML
    public Label pendingJobsHeaderLabel;
    @FXML
    public Label completedJobsHeaderLabel;
    @FXML
    public TextField masterIpTextField;
    @FXML
    public TextArea statusLogsTextArea;
    @FXML
    public ChoiceBox<String> jobTypeChoiceBox;
    @FXML
    public ListView<String> pendingJobsListView;
    @FXML
    public ListView<String> returnedJobsListView;

    @FXML
    public void initialize() {
        clientModel = new ClientModel(pendingJobsListView, returnedJobsListView, pendingJobsHeaderLabel, completedJobsHeaderLabel);
    }

    @FXML
    public void requestNewJob(ActionEvent actionEvent) {
        if (connectToMasterTask == null || !connectToMasterTask.isRunning()) {
            connectToMaster();
        }
        Job job = new Job(clientModel.getNextJobId(), JobType.valueOf(jobTypeChoiceBox.getValue()));
        try {
            clientModel.enqueueJobToRequest(job);
        } catch (InterruptedException e) {
            statusLabel.setText("Error requesting job");
        }
    }

    /**
     * Launch the ConnectToMasterTask which launches a thread for output to the Master (to request jobs), and a thread
     * for input from the Master (to receive jobs)
     * */
    @FXML
    public void connectToMaster() {
        // check IP
        try {
            this.ipConnection = new IPConnection(masterIpTextField.getText(), PORT_NUM);
        } catch (IllegalArgumentException e) {
            statusLabel.setText("Please provide a valid IPv4 address");
            return;
        }

        // Create and launch task to connect to socket, which will launch the individual threads
        connectToMasterTask = new ConnectToMasterTask(ipConnection, clientModel, statusLogsTextArea);
        statusLabel.textProperty().bind(connectToMasterTask.messageProperty());  // (allow task to update status label)
        Thread connectThread = new Thread(connectToMasterTask, "Thread-ConnectToMaster");
        connectThread.setDaemon(true);
        connectThread.start();
    }
}

