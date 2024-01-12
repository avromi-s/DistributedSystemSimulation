// Avromi Schneierson - 1/10/2024
package main;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import main.classes.Client;
import main.classes.MasterJob;
import main.classes.Slave;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * <p>
 * This class holds the following info:
 *     <ul>
 *         <li>
 *             Clients - clients are added to or removed from the system by adding them to this class
 *         </li>
 *         <li>
 *             Slaves - slaves are added to or removed from the system by adding them to this class
 *         </li>
 *         <li>
 *             Jobs - jobs are added to or removed from the system by adding them to this class
 *             <p>
 *                 The general flow of a job through the system is as follows:
 *                 <ol>
 *                     <li>
 *                          A job request is received from a client via its ReceiveJobRequestsFromClientTask thread and
 *                          enqueued here, in the MasterModel.
 *                     </li>
 *                     <li>
 *                          The job is dequeued from the MasterModel by the DelegateJobsTask thread so it can be assigned the most
 *                          optimal slave. Once the most optimal slave is found for that job, it is enqueued directly
 *                          with the Slave in its Slave object.
 *                     </li>
 *                     <li>
 *                         The job is dequeued from the Slave object by the Slave's SendJobRequestsToSlaveTask thread and sent
 *                         to the slave for execution.
 *                     </li>
 *                     <li>
 *                         The job is received back from the slave with its ReceiveJobResultsFromSlaveTask thread once it has
 *                         been completed. Once the job is received back from the slave, it is enqueued in the MasterModel.
 *                     </li>
 *                     <li>
 *                          The job is dequeued from the MasterModel by the ReturnJobsTask thread so it can be returned
 *                          to the client that requested it. The client is looked up and the job (now with its result) is
 *                          enqueued directly with the correct Client in its Client object.
 *                     </li>
 *                     <li>
 *                         The job is dequeued from the Client object by the Client's SendJobResultsToClientTask thread and sent back
 *                         to the client.
 *                     </li>
 *                 </ol>
 *             </p>
 *         </li>
 *     </ul>
 * </p>
 * Each Slave and Client's info is stored in an instance of their respective Slave and Client classes. The
 * instance is shared with any threads that deal with the Slave or Client.
 */
public class MasterModel {
    private final ConcurrentHashMap<Integer, Client> activeClientsMap = new ConcurrentHashMap<>();  // maps client ID ---> client instance. used so that we can look up a specific client
    private final ConcurrentHashMap<Integer, Slave> activeSlavesMap = new ConcurrentHashMap<>();  // maps slave ID ---> slave instance. used so that we can look up a specific slave
    private final ObservableList<Client> activeClients = FXCollections.observableArrayList();  // the underlying list used for the Client GUI listview
    private final ObservableList<Slave> activeSlaves = FXCollections.observableArrayList();  // the underlying list used for the Client GUI listview

    /**
     * Holds all the jobs requested by all clients that have not yet been delegated and enqueued
     * with a slave. The queue is <i>enqueued</i> by the various clients as new jobs are requested (handled by the ReceiveJobRequestsFromClientTask).
     * The queue is <i>dequeued</i> by the thread responsible for assigning the jobs to slaves (load balancing) (handled by the DelegateJobsTask).
     */
    private final LinkedBlockingQueue<MasterJob> allJobsRequested = new LinkedBlockingQueue<>();

    /**
     * Holds all the jobs completed by all slaves that have not yet been returned to the client that originally requested them.
     * The queue is <i>enqueued</i> by the various slaves as new jobs are completed (handled by the ReceiveJobResultsFromSlaveTask).
     * The queue is <i>dequeued</i> by the thread responsible for sending back the job result to the clients that requested them (handled by the ReturnJobsTask).
     */
    private final LinkedBlockingQueue<MasterJob> allJobsCompleted = new LinkedBlockingQueue<>();

    /**
     * Holds all jobs that ever were requested. Used for retrieving the original details (such as the id) of a job when
     * received back from a Slave.
     * */
    private final HashMap<Integer, MasterJob> allJobs = new HashMap<>();  // internal job ID ---> job instance
    private int numJobsRequested = 0, numJobsCompleted = 0;
    private final ListView<Client> clientsListView;
    private final ListView<Slave> slavesListView;
    private final Label statusLabel, clientsHeaderLabel, slavesHeaderLabel;

    public MasterModel(ListView<Client> clientsListView, ListView<Slave> slavesListView, Label statusLabel, Label clientsHeaderLabel, Label slavesHeaderLabel) {
        this.clientsListView = clientsListView;
        this.clientsListView.setItems(activeClients);

        this.slavesListView = slavesListView;
        this.slavesListView.setItems(activeSlaves);

        this.statusLabel = statusLabel;
        this.clientsHeaderLabel = clientsHeaderLabel;
        this.slavesHeaderLabel = slavesHeaderLabel;
    }

    ////////////////
    // Clients
    ////////////////

    /**
     * Add a client to the system
     */
    public void addActiveClient(Client client) {
        activeClientsMap.put(client.getId(), client);
        Platform.runLater(() -> {
            activeClients.add(client);
            clientsHeaderLabel.setText("Clients (" + activeClients.size() + ")");
        });
    }

    /**
     * Remove a client from the system
     */
    public void removeActiveClient(Client client) {
        activeClientsMap.remove(client.getId());
        Platform.runLater(() -> {
            activeClients.remove(client);
            clientsHeaderLabel.setText("Clients" + (!activeClients.isEmpty() ? " (" + activeClients.size() + ")" : ""));
        });
    }

    public Client getClient(int clientId) {
        return activeClientsMap.get(clientId);
    }

    /**
     * @return an unmodifiableMap of all clients
     */
    public Map<Integer, Client> getClients() {
        return Collections.unmodifiableMap(activeClientsMap);
    }

    ////////////////
    // Slaves
    ////////////////

    /**
     * Add a slave to the system. Once added, this slave may be delegated jobs to execute
     */
    public void addActiveSlave(Slave slave) {
        activeSlavesMap.put(slave.getId(), slave);  // manual synchronization is not needed here as the map is thread-safe
        Platform.runLater(() -> {
            activeSlaves.add(slave);
            slavesHeaderLabel.setText("Slaves (" + activeSlaves.size() + ")");
        });
    }

    /**
     * Remove the given slave from the system. Once removed, this slave will no longer be delegated jobs to execute
     */
    public void removeActiveSlave(Slave slave) {
        activeSlavesMap.remove(slave.getId());
        Platform.runLater(() -> {
            activeSlaves.remove(slave);
            slavesHeaderLabel.setText("Slaves" + (!activeSlaves.isEmpty() ? " (" + activeSlaves.size() + ")" : ""));
        });
    }

    /**
     * Retrieve the Slave object for the given id
     */
    public Slave getSlave(int slaveId) {
        return activeSlavesMap.get(slaveId);
    }

    /**
     * @return an unmodifiableMap of all slaves
     */
    public Map<Integer, Slave> getSlaves() {
        return Collections.unmodifiableMap(activeSlavesMap);
    }

    ////////////////
    // Jobs
    ////////////////

    /**
     * Enqueue a client-requested job to be delegated by the Master for execution.
     * We store the job by its internal id as opposed to the external id so that jobs from separate clients which may
     * have the same external id don't collide. The external id will still be used when we return the job to the client
     *
     * @param job the job requested
     */
    public void enqueueJobRequested(MasterJob job) throws InterruptedException {
        MasterJob jobCopy = new MasterJob(job);  // we copy the job just in case the caller maintains a reference to the job object
        int jobId = jobCopy.getInternalId();
        allJobsRequested.put(jobCopy);
        synchronized (allJobs) {
            allJobs.put(jobId, jobCopy);
        }

        numJobsRequested++;
        Platform.runLater(() -> {
            statusLabel.setText("Jobs Requested: " + numJobsRequested + "\nJobs Completed: " + numJobsCompleted);
        });
    }


    /**
     * Deque a requested job for delegation to a slave so it can be executed.
     */
    public MasterJob dequeJobRequested() throws InterruptedException {
        return allJobsRequested.take();
    }

    /**
     * Enqueue a slave-completed job to be returned to a client.
     * @param job the job completed
     */
    public void enqueueJobCompleted(MasterJob job) throws InterruptedException {
        MasterJob jobCopy = new MasterJob(job);
        jobCopy.setOriginalId(allJobs.get(jobCopy.getInternalId()).getOriginalId());  // recover the job's original id as the slave returns the job using the internal id
        allJobsCompleted.put(jobCopy);

        numJobsRequested--;
        numJobsCompleted++;
        Platform.runLater(() -> {
            statusLabel.setText("Jobs Requested: " + numJobsRequested + "\nJobs Completed: " + numJobsCompleted);
        });
    }

    /**
     * Deque a completed job to be returned to the Client.
     * The ReturnJobsTask will call this method, look up the client, and give the job results to the appropriate Task/Thread
     * to be sent back to the Client.
     */
    public MasterJob dequeJobCompleted() throws InterruptedException {
        return allJobsCompleted.take();
    }

    /**
     * Returns a copy of the job for the given internalId
     */
    public MasterJob getJob(int internalId) {
        synchronized (allJobs) {
            if (allJobs.get(internalId) != null) {
                return new MasterJob(allJobs.get(internalId));
            } else {
                return null;
            }
        }
    }
}