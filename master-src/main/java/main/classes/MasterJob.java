// Avromi Schneierson - 1/10/2024
package main.classes;

import main.enums.JobType;

/**
 * This class represents a job within the Master system.
 */
public class MasterJob extends Job {
    private static int lastUsedInternalId = -1;
    private static final Object lastUsedInternalIdLock = new Object();

    /**
     * <p>
     *     To track a job, we make use of an internal and original id.
     *     The internal id is used within the master to uniquely identify a job across all clients. It is
     *     tracked only by the Master and is transparent to the client.
     *     The original id is the id used when a job is transmitted to/from a client.
     * </p>
     * <p>
     *     The master will use the internal when communicating with a slave in order to keep the job fully unique in case
     *     there are duplicate ids from two separate clients. When the job is returned to the master, the master will send
     *     the result back to the client with the original id.
     * </p>
     */
    private int internalId;
    private int originalId;
    private int clientId;
    private int slaveId;
    private final String TO_STRING_FIELD_DELIMITER = " - ";

    public MasterJob() {
        this.internalId = getNextAvailableInternalJobId();
    }

    public MasterJob(int originalId) {
        this();
        this.originalId = originalId;
    }

    public MasterJob(int originalId, JobType jobType) {
        this();
        this.originalId = originalId;
        this.setJobType(jobType);
    }

    /**
     * Copy constructor
     */
    public MasterJob(MasterJob job) {
        this.internalId = job.internalId;
        this.originalId = job.originalId;
        this.setJobType(job.getJobType());
        this.setSucceeded(job.getSucceeded());
        this.setResult(job.getResult());
        this.clientId = job.clientId;
        this.slaveId = job.slaveId;
    }

    /**
     * Takes a jobStr (as returned by a job.toString() call) and constructs this job based on that
     */
    public MasterJob(String jobStr) {
        String[] fields = jobStr.split(TO_STRING_FIELD_DELIMITER);
        this.internalId = Integer.parseInt(fields[0]);
        this.originalId = Integer.parseInt(fields[1]);
        this.setJobType(JobType.valueOf(fields[2]));
        this.setSucceeded(Boolean.parseBoolean(fields[3]));
        this.setResult(fields[4]);
        this.clientId = Integer.parseInt(fields[5]);
        this.slaveId = Integer.parseInt(fields[6]);
    }

    /**
     * @return the next available internal job id
     */
    public static int getNextAvailableInternalJobId() {
        synchronized (lastUsedInternalIdLock) {
            return ++lastUsedInternalId;
        }
    }

    public int getInternalId() {
        return internalId;
    }

    public void setInternalId(int internalId) {
        this.internalId = internalId;
    }

    public int getOriginalId() {
        return originalId;
    }

    public void setOriginalId(int originalId) {
        this.originalId = originalId;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public int getSlaveId() {
        return slaveId;
    }

    public void setSlaveId(int assignedSlave) {
        this.slaveId = assignedSlave;
    }

    @Override
    public String toString() {
        return String.join(TO_STRING_FIELD_DELIMITER, String.valueOf(internalId), String.valueOf(originalId), String.valueOf(getJobType()),
                String.valueOf(getSucceeded()), getResult(), String.valueOf(clientId), String.valueOf(slaveId));
    }

    /**
     * We consider two jobs equal if they have the same internal job ID, as the job ID is meant to be the unique identifier
     * */
    @Override
    public boolean equals(Object a) {
        if (a instanceof MasterJob) {
            return ((MasterJob) a).internalId == this.internalId;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return String.valueOf(internalId).hashCode();
    }
}
