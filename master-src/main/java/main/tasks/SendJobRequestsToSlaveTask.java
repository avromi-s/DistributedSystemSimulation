// Avromi Schneierson - 1/10/2024
package main.tasks;

import javafx.scene.control.TextArea;
import main.Logging;
import main.classes.MasterJob;
import main.classes.JobSender;
import main.classes.Slave;

import java.io.PrintWriter;

/**
 * This class is responsible for sending Jobs to a specific slave to be completed.
 * This task is started by the SlaveConnectionHandlerTask when a new Slave connects and serves as the sole sender of output
 * to that Slave.
 * The class waits on its Slave's jobsToRun queue and once a job is enqueued there, this class dequeues it and sends
 * it to the Slave.
 */
public class SendJobRequestsToSlaveTask extends JobSender {
    private final Slave slave;
    private final PrintWriter socketOut;
    private final TextArea logsTextArea;


    public SendJobRequestsToSlaveTask(Slave slave, PrintWriter socketOut, TextArea logsTextArea) {
        this.slave = slave;
        this.socketOut = socketOut;
        this.logsTextArea = logsTextArea;
    }

    @Override
    protected Void call() {
        MasterJob jobToSend = null;
        try (socketOut) {
            while (!isCancelled()) {
                try {
                    jobToSend = slave.dequeJobToRun();
                } catch (InterruptedException e) {
                    Logging.consoleLog("Thread interrupted or cancelled while waiting to dequeue" +
                            "the next job to send:\n" + e.getMessage() + "\n");
                    if (isCancelled()) break;
                }

                if (jobToSend != null) {
                    boolean succeeded = sendJob(jobToSend, true, socketOut);
                    if (succeeded) {
                        Logging.consoleLogAndAppendToGUILogs("Sent job request to slave #" + slave.getId() + ": [" + jobToSend + "]\n", logsTextArea);
                    } else {
                        Logging.consoleLogAndAppendToGUILogs("Unable to send job request to slave #" + slave.getId() + ": [" + jobToSend + "]\n", logsTextArea);
                    }
                } else {
                    Logging.consoleLogAndAppendToGUILogs("Error while trying to send MasterJob request to slave - job is null\n", logsTextArea);
                }
            }
            Logging.consoleLog("Send job request task for slave #" + slave.getId() + " cancelled\n");
            socketOut.close();
            return null;
        }
    }
}
