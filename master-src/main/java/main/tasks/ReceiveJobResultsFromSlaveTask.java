// Avromi Schneierson - 1/10/2024
package main.tasks;

import javafx.scene.control.TextArea;
import main.MasterModel;
import main.Logging;
import main.classes.MasterJob;
import main.classes.JobReceiver;
import main.classes.Slave;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * This class is responsible for listening for MasterJob results from a specific Slave sent over the Slave's socket.
 * This task is started by the SlaveConnectionHandlerTask when a new Slave connects and serves as the sole receiver of input
 * from the Slave.
 * When a job is received by this Task, it enqueues it with the master so it can be returned to the Client.
 */
public class ReceiveJobResultsFromSlaveTask extends JobReceiver {
    private final MasterModel masterModel;
    private final Slave slave;
    private final BufferedReader socketIn;
    private final TextArea logsTextArea;

    public ReceiveJobResultsFromSlaveTask(MasterModel masterModel, Slave slave, BufferedReader socketIn, TextArea logsTextArea) {
        this.masterModel = masterModel;
        this.slave = slave;
        this.socketIn = socketIn;
        this.logsTextArea = logsTextArea;
    }


    @Override
    protected Void call() throws Exception {
        try (socketIn) {
            while (!isCancelled()) {
                MasterJob job = receiveJob(socketIn, true, logsTextArea);
                if (job == null) {
                    Logging.consoleLogAndAppendToGUILogs("Error while trying to receive job from received packet\n", logsTextArea);
                    continue;
                }
                job.setSlaveId(slave.getId());

                try {
                    masterModel.enqueueJobCompleted(job);
                    slave.addJobCompleted(job);
                } catch (InterruptedException e) {
                    Logging.consoleLogAndAppendToGUILogs("Thread interrupted or cancelled while waiting to enqueue" +
                            "a completed job:\n" + e.getMessage() + "\n", logsTextArea);
                }
            }
        } catch (IOException e) {
            Logging.consoleLog("Error while trying to receive job result from slave #"
                    + slave.getId() + "\n");
            //e.printStackTrace();
            return null;
        }
        return null;
    }
}
