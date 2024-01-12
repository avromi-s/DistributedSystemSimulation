// Avromi Schneierson - 1/10/2024
package main;

import PacketCommunication.IPConnection;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import main.enums.JobType;
import main.tasks.ConnectToMasterTask;
import main.tasks.ExecuteJobsTask;

/**
 * The controller for the Slave application. This class is responsible for handling all GUI events and launching the
 * initial threads.
 * */
public class SlaveController {
    private final int PORT_NUM = 30000;  // slaves communicate on a different port number than clients so that they don't interfere
    private IPConnection ipConnection;
    private SlaveModel slaveModel;
    private ConnectToMasterTask connectToMasterTask;
    private ExecuteJobsTask executeJobsTask;

    // JavaFx controls:
    @FXML
    public Label statusLabel;
    @FXML
    public Label pendingJobsHeaderLabel;
    @FXML
    public Label completedJobsHeaderLabel;
    @FXML
    public Button connectButton;
    @FXML
    public TextField masterIpTextField;
    @FXML
    public TextArea statusLogsTextArea;
    @FXML
    public ListView<String> pendingJobsListView;
    @FXML
    public ListView<String> completedJobsListView;

    /**
     * Instantiates the slave model with the job type that it is optimized for.
     * This is called by the SlaveApplication class when the application is started.
     * (we specifically call our own 'init' method as opposed to FXML's initialize so that we can have it run after the
     * application is fully started and so that we can pass in the jobtype)
     * */
    public void init(JobType jobType) {
        slaveModel = new SlaveModel(jobType, pendingJobsListView, completedJobsListView, pendingJobsHeaderLabel, completedJobsHeaderLabel);
    }

    /**
     * Launch threads to connect to the Master and execute Jobs:
     * <ul>
     *     <li>Launch the ConnectToMasterTask which, in turn, launches a thread for output to the Master, and a thread
     *     for input from the Master.</li>
     *     <li>Launch the ExecuteJobsTask which is responsible for executing the Jobs, as they become available</li>
     * </ul>
     *
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

        connectToMasterTask = getConnectToMasterTask();
        Thread connectThread = new Thread(connectToMasterTask, "Thread-ConnectToMaster");
        connectThread.setDaemon(true);
        connectThread.start();

        executeJobsTask = getExecuteJobsTask();
        Thread executeThread = new Thread(executeJobsTask, "Thread-ExecuteJobs");
        executeThread.setDaemon(true);
        executeThread.start();
    }

    /**
     * Disable the GUI controls for connecting, and return a ConnectToMasterTask which will reenable them when it is
     * terminated.
     * */
    private ConnectToMasterTask getConnectToMasterTask() {
        ConnectToMasterTask task = new ConnectToMasterTask(ipConnection, slaveModel, statusLogsTextArea);
        // disable controls prior to running task and re-enable if connection isn't established or is
        // terminated at any point
        masterIpTextField.setDisable(true);
        connectButton.setDisable(true);

        EventHandler<WorkerStateEvent> enableControls = event -> {
            masterIpTextField.setDisable(false);
            connectButton.setDisable(false);
        };
        task.setOnCancelled(enableControls);
        task.setOnFailed(enableControls);
        task.setOnSucceeded(enableControls);  // even if terminated successfully (i.e., master closed the connection), allow reconnecting
        statusLabel.textProperty().bind(task.messageProperty());  // (allow task to update status label)
        return task;
    }

    /**
     * @return the ExecuteJobsTask, responsible for executing the jobs received from the Master
     * */
    private ExecuteJobsTask getExecuteJobsTask() {
        ExecuteJobsTask executeJobsTask = new ExecuteJobsTask(slaveModel, statusLogsTextArea);
        statusLabel.textProperty().bind(executeJobsTask.messageProperty());
        return executeJobsTask;
    }
}