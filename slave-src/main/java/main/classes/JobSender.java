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
     * */
    public boolean sendJob(Job jobToSend, PrintWriter out) {
        HashMap<PacketArgKey, String> args = new HashMap<>();
        args.put(PacketArgKey.JOB_ID, String.valueOf(jobToSend.getJobId()));
        args.put(PacketArgKey.JOB_TYPE, String.valueOf(jobToSend.getJobType()));
        args.put(PacketArgKey.JOB_SUCCEEDED, String.valueOf(jobToSend.getSucceeded()));
        PacketEncoder packetEncoder = new PacketEncoder(args);
        packetEncoder.setMessage(jobToSend.getResult());
        out.print(packetEncoder);
        return !out.checkError();
    }
}
