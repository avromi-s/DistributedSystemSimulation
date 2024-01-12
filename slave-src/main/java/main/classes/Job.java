// Avromi Schneierson - 1/10/2024
package main.classes;

import main.enums.JobType;

/**
 * This class represents a job
 * */
public class Job {
    private int jobId;
    private JobType jobType;
    private boolean succeeded;
    private String result;
    private final String TO_STRING_FIELD_DELIMITER = " - ";  // the delimiter used when this job is represented as a String

    public Job() {
    }

    public Job(int jobId, JobType jobType) {
        this.jobId = jobId;
        this.jobType = jobType;
    }

    /**
     * Copy constructor
     * */
    public Job(Job job) {
        this.jobId = job.jobId;
        this.jobType = job.jobType;
        this.succeeded = job.succeeded;
        this.result = job.result;
    }

    public Job(String jobStr) {
        String[] fields = jobStr.split(TO_STRING_FIELD_DELIMITER);
        this.jobId = Integer.parseInt(fields[0]);
        this.jobType = JobType.valueOf(fields[1]);
        this.succeeded = Boolean.parseBoolean(fields[2]);
        this.result = fields[3];
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public boolean getSucceeded() {
        return succeeded;
    }

    public void setSucceeded(boolean succeeded) {
        this.succeeded = succeeded;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    @Override
    public String toString() {
        return String.join(TO_STRING_FIELD_DELIMITER, String.valueOf(jobId), String.valueOf(jobType), String.valueOf(succeeded), result);
    }

    /**
     * We consider two jobs equal if they have the same internal job ID, as the job ID is meant to be the unique identifier
     * */
    @Override
    public boolean equals(Object a) {
        if (a instanceof Job) {
            return ((Job) a).jobId == this.jobId;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return String.valueOf(jobId).hashCode();
    }
}
