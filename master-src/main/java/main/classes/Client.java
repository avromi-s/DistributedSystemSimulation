// Avromi Schneierson - 1/10/2024
package main.classes;

import PacketCommunication.IPConnection;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Each Client's info is stored in an instance of their Client class. The instance is shared with any threads that
 * deal with the Client.
 * This class keeps a live queue of the completed jobs waiting to be sent back to the Client.
 * The Jobs are enqueued by the ReturnJobsTask after they have been returned by the executing slave.
 * The jobs are dequeued by the SendJobResultsToClientTask to be sent back to the Client.
 */
public class Client {
    private static int lastUsedId = -1;
    private static final Object lastUsedIdLock = new Object();
    private final int id;
    private final IPConnection ipConnection;  // the details of the connection to this client

    /**
     * This Client's jobs that are completed and waiting to be sent back to the Client.
     */
    private final LinkedBlockingQueue<MasterJob> jobsCompleted = new LinkedBlockingQueue<>();

    /**
     * All jobs ever requested by this client
     * */
    private final ArrayList<MasterJob> allJobs = new ArrayList<>();

    /**
     * All jobs completed for this client
     * */
    private final ArrayList<MasterJob> allJobsCompleted = new ArrayList<>();

    public Client(int id, IPConnection connection) {
        this.id = id;
        this.ipConnection = connection;
    }

    /**
     * @return the next available client id
     */
    public static int getNextAvailableClientId() {
        synchronized (lastUsedIdLock) {
            return ++lastUsedId;
        }
    }

    public int getId() {
        return id;
    }

    /**
     * Enqueue a slave-completed job to be returned to a client.
     * @param jobCompleted the job completed
     */
    public void enqueueCompletedJob(MasterJob jobCompleted) throws InterruptedException {
        jobsCompleted.put(new MasterJob(jobCompleted));  // synchronization not needed here because the queue is thread-safe
        synchronized (allJobsCompleted) {
            allJobsCompleted.add(new MasterJob(jobCompleted));
        }
    }

    /**
     * Deque a completed job to be sent back to the Client.
     * The SendJobResultsToClientTask for this Client will call this method to sent back the job to the Client.
     */
    public MasterJob dequeCompletedJob() throws InterruptedException {
        return jobsCompleted.take();  // synchronization not needed here because the queue is thread-safe
    }

    /**
     * Add a job that was requested by this client
     */
    public void addJobRequested(MasterJob job) {
        MasterJob jobCopy = new MasterJob(job);
        synchronized (allJobs) {
            allJobs.add(jobCopy);
        }
    }

    /**
     * @return the number of jobs completed by this Slave
     */
    public int getNumJobsCompleted() {
        synchronized (allJobsCompleted) {
            return allJobsCompleted.size();
        }
    }

    /**
     * @return the number of jobs requested by this client
     */
    public int getNumJobsRequested() {
        synchronized (allJobs) {
            return allJobs.size();
        }
    }

    @Override
    public String toString() {
        return "Client #" + getId() + " - (" + getNumJobsCompleted() + " / " + getNumJobsRequested() + ")";
    }

    // Client objects are equal if they have the same id as the id is their unique identifier
    @Override
    public boolean equals(Object a) {
        if (a instanceof Client) {
            return ((Client) a).id == this.id;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return String.valueOf(id).hashCode();
    }
}
