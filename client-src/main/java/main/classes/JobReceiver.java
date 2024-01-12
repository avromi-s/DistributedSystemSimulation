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
     * */
    public Job receiveJob(BufferedReader in, TextArea logsTextArea) throws IOException {
        PacketDecoder packetDecoder = receiveOnePacket(in);
        if (packetDecoder == null) {
            Logging.consoleLogAndAppendToGUILogs("Error while parsing args in packet." +
                    " Received packet with invalid structure, packet likely corrupt.\n", logsTextArea);
            return null;
        }

        if (!packetContainsValidJob(packetDecoder)) {
            Logging.consoleLogAndAppendToGUILogs("Error while receiving Job. Received " +
                    "Packet without expected args\n", logsTextArea);
            return null;
        }

        Job job = getJobFromPacket(packetDecoder, logsTextArea);
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

    private Job getJobFromPacket(PacketDecoder packetDecoder, TextArea logsTextArea) {
        Job job = new Job();
        try {
            job.setJobId(Integer.parseInt(packetDecoder.getArg(PacketArgKey.JOB_ID)));
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
