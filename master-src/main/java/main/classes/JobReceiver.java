// Avromi Schneierson - 1/10/2024
package main.classes;

import PacketCommunication.PacketDecoder;
import PacketCommunication.enums.PacketArgKey;
import javafx.scene.control.TextArea;
import main.Logging;
import main.enums.JobType;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * This abstract class is used for receiving and extracting Jobs from packets
 */
public abstract class JobReceiver extends PacketReceiver {
    /**
     * Receive a job from a packet read from the given BufferedReader
     * @param in the BufferedReader to read the job from
     * @param useInternalId boolean indicating if the job id received should be set as the internal or original id in the
     *                      returned job object (the internal id is used for the slave or master, the original is used
     *                      for the client)
     * @param logsTextArea the GUI TextArea to output any logs while receiving the job
     * */
    public MasterJob receiveJob(BufferedReader in, boolean useInternalId, TextArea logsTextArea) throws IOException {
        PacketDecoder packetDecoder = receiveOnePacket(in);
        if (packetDecoder == null) {
            Logging.consoleLogAndAppendToGUILogs("Error while parsing args in packet." +
                    " Received packet with invalid structure, packet likely corrupt.\n", logsTextArea);
            return null;
        }

        if (!packetContainsValidJob(packetDecoder)) {
            Logging.consoleLogAndAppendToGUILogs("Error while receiving MasterJob. Received " +
                    "Packet without expected args\n", logsTextArea);
            return null;
        }

        MasterJob job = getJobFromPacket(packetDecoder, useInternalId, logsTextArea);
        if (job == null) {
            Logging.consoleLogAndAppendToGUILogs("Error while parsing args in packet." +
                    " Received packet with invalid structure, packet likely corrupt.\n\tPacket: '" +
                    packetDecoder.getPacketString() + "'\n", logsTextArea);
        }
        return job;
    }

    private boolean packetContainsValidJob(PacketDecoder packetDecoder) {
        return packetDecoder.containsArg(PacketArgKey.JOB_ID) && packetDecoder.containsArg(PacketArgKey.JOB_SUCCEEDED);
    }

    private MasterJob getJobFromPacket(PacketDecoder packetDecoder, boolean useInternalId, TextArea logsTextArea) {
        MasterJob job = new MasterJob();
        try {
            if (useInternalId) {
                job.setInternalId(Integer.parseInt(packetDecoder.getArg(PacketArgKey.JOB_ID)));
            } else {
                job.setOriginalId(Integer.parseInt(packetDecoder.getArg(PacketArgKey.JOB_ID)));
            }
            job.setJobType(JobType.valueOf(packetDecoder.getArg(PacketArgKey.JOB_TYPE)));
            job.setSucceeded(Boolean.parseBoolean(packetDecoder.getArg(PacketArgKey.JOB_SUCCEEDED)));
            job.setResult(packetDecoder.getMessage());
            return job;
        } catch (IllegalArgumentException e) {
            // unknown jobId or jobType, we can't process this packet, restart from the beginning of the while loop to read another packet
            return null;
        }
    }
}
