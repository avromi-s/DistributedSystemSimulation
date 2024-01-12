// Avromi Schneierson - 1/10/2024
package main.classes;

import PacketCommunication.PacketEncoder;
import PacketCommunication.enums.PacketArgKey;
import javafx.concurrent.Task;

import java.io.PrintWriter;
import java.util.HashMap;

/**
 * This abstract class is used for sending a job as a packet
 */
public abstract class JobSender extends Task<Void> {
    /**
     * Pack the given job into a packet and output it to the given PrintWriter
     * @param jobToSend the job to send
     * @param useInternalId boolean indicating if the job ID sent should use the internal or original id (the internal
     *                      id is used for the slave or master, the original is used for the client)
     * @param out the PrintWriter to output the packet to
     * */
    public boolean sendJob(MasterJob jobToSend, boolean useInternalId, PrintWriter out) {
        HashMap<PacketArgKey, String> args = new HashMap<>();
        args.put(PacketArgKey.JOB_ID, String.valueOf(useInternalId ? jobToSend.getInternalId() : jobToSend.getOriginalId()));
        args.put(PacketArgKey.JOB_TYPE, String.valueOf(jobToSend.getJobType()));
        args.put(PacketArgKey.JOB_SUCCEEDED, String.valueOf(jobToSend.getSucceeded()));
        PacketEncoder packetEncoder = new PacketEncoder(args);
        packetEncoder.setMessage(jobToSend.getResult());
        out.print(packetEncoder);
        return !out.checkError();
    }
}
