// Avromi Schneierson - 1/10/2024
package main.classes;

import PacketCommunication.IPConnection;
import main.enums.JobType;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Each Slave's info is stored in an instance of their Slave class. The instance is shared with any threads that
 * deal with the Slave.
 * This class keeps a live queue of the jobs waiting to be sent to the Slave for execution.
 * The Jobs are enqueued by the DelegateJobsTask after being requested by a client.
 * The jobs are dequeued by the SendJobRequestsToSlaveTask to be sent to the Slave for execution.
 */
public class Slave {
    private static int lastUsedId = -1;
    private static final Object lastUsedIdLock = new Object();
    private final int id;
    private final IPConnection ipConnection;  // the details of the connection to this slave
    private final JobType slaveOptimizedForType;

    /**
     * Stores how long it takes for this slave to process a job of a given type. The process times for each job type will
     * be determined based on the job type this slave is optimized for when this slave is constructed.
     */
    private final ConcurrentHashMap<JobType, Integer> jobProcessTimes = new ConcurrentHashMap<>();
    private final int PROCESS_TIME_OPTIMIZED_JOB = 2;  // in seconds
    private final int PROCESS_TIME_UNOPTIMIZED_JOB = 10;

    /**
     * This Slave's assigned jobs waiting to be sent to the Slave for execution.
     */
    private final LinkedBlockingQueue<MasterJob> jobsToRun = new LinkedBlockingQueue<>();

    /**
     * All jobs currently requested and not completed from this Slave
     * */
    private final ArrayList<MasterJob> allJobsRequested = new ArrayList<>();

    /**
     * All jobs already completed by this Slave
     * */
    private final ArrayList<MasterJob> allJobsCompleted = new ArrayList<>();

    public Slave(int id, IPConnection connection, JobType slaveOptimizedForType) {
        this.id = id;
        this.ipConnection = connection;
        this.slaveOptimizedForType = slaveOptimizedForType;
        for (JobType jobType : JobType.values()) {
            this.jobProcessTimes.put(jobType, slaveOptimizedForType == jobType ? PROCESS_TIME_OPTIMIZED_JOB : PROCESS_TIME_UNOPTIMIZED_JOB);
        }
    }

    /**
     * @return the next available slave id
     */
    public static int getNextAvailableSlaveId() {
        synchronized (lastUsedIdLock) {
            return ++lastUsedId;
        }
    }

    public int getId() {
        return id;
    }

    public JobType getSlaveOptimizedForType() {
        return slaveOptimizedForType;
    }

    /**
     * Enqueue a requested job to be executed by the Slave.
     * @param jobRequested the requested job
     */
    public void enqueueJobToRun(MasterJob jobRequested) throws InterruptedException {
        jobsToRun.put(new MasterJob(jobRequested));  // synchronization not needed here because the queue is thread-safe
        synchronized (allJobsRequested) {
            allJobsRequested.add(new MasterJob(jobRequested));
        }
    }

    /**
     * Deque a requested job to be sent to the Slave for execution.
     * The SendJobRequestsToSlaveTask for this Slave will call this method to send the job request to this Slave.
     */
    public MasterJob dequeJobToRun() throws InterruptedException {
        return jobsToRun.take();
    }

    /**
     * Add a job that was completed by this slave
     * Since the job is now completed, it is no longer being requested, so we remove it from the allJobsRequested list
     * so that the system load can be calculated correctly
     */
    public void addJobCompleted(MasterJob job) {
        MasterJob jobCopy = new MasterJob(job);
        synchronized (allJobsCompleted) {
            allJobsCompleted.add(jobCopy);
        }
        synchronized (allJobsRequested) {
            allJobsRequested.remove(jobCopy);
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
     * @return the number of jobs requested from this Slave
     */
    public int getNumJobsRequested() {
        synchronized (allJobsRequested) {
            return allJobsRequested.size();
        }
    }

    /**
     * @return the time it takes for this slave to complete the given job (based on what JobType this slave is optimized for)
     */
    public int getJobProcessTime(MasterJob job) {
        return jobProcessTimes.get(job.getJobType());
    }

    /**
     * Returns the current total system load on this slave, based on the number of requested (and not returned) jobs,
     * their job types, and what job type this slave is optimized for.
     */
    public int getTotalLoad() {
        int load = 0;
        synchronized (allJobsRequested) {
            for (MasterJob job : allJobsRequested) {
                load += jobProcessTimes.get(job.getJobType());
            }
        }
        return load;
    }

    @Override
    public String toString() {
        return "Slave #" + getId() + " [Type " + slaveOptimizedForType + "] - (" + getNumJobsCompleted() + " / " + getNumJobsRequested() + ")";
    }

    // Slave objects are equal if they have the same id as the id is their unique identifier
    @Override
    public boolean equals(Object a) {
        if (a instanceof Slave) {
            return ((Slave) a).id == this.id;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return String.valueOf(id).hashCode();
    }
}
