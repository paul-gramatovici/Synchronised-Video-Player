package Client;

import Packets.RTCPpacket;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class RtcpSender implements ActionListener {

    private static final int RTCP_RCV_PORT = 19001;   //port where the client will receive the RTP packets

    private final Client client;

    private DatagramSocket RTCPsocket;          //UDP socket for sending RTCP packets
    private Timer rtcpTimer;

    private int lastHighSeqNb;      // The last highest Seq number received
    private int lastCumLost;        // The last cumulative packets lost



    RtcpSender(Client client, int interval) {
        this.client = client;
        rtcpTimer = new Timer(interval, this);
        rtcpTimer.setInitialDelay(0);
        rtcpTimer.setCoalesce(true);
    }

    public void openSocket() throws SocketException {
        RTCPsocket = new DatagramSocket();
    }

    public void actionPerformed(ActionEvent e) {

        // Calculate the stats for this period
        int numPktsExpected = client.getStats().getHighSeqNb() - lastHighSeqNb;
        int numPktsLost = client.getStats().getCumLost() - lastCumLost;
        float fractionLost = numPktsExpected == 0 ? 0f : (float) numPktsLost / numPktsExpected;
        lastHighSeqNb = client.getStats().getHighSeqNb();
        lastCumLost = client.getStats().getCumLost();

        RTCPpacket rtcp_packet = new RTCPpacket(fractionLost, client.getStats().getCumLost(), client.getStats().getHighSeqNb());
        int packet_length = rtcp_packet.getlength();
        byte[] packet_bits = new byte[packet_length];
        rtcp_packet.getpacket(packet_bits);

        try {
            DatagramPacket dp = new DatagramPacket(packet_bits, packet_length, client.getServerIPAddr(), RTCP_RCV_PORT);
            RTCPsocket.send(dp);
        } catch (InterruptedIOException iioe) {
            System.out.println("Nothing to read");
        } catch (IOException ioe) {
            System.out.println("Exception caught: "+ioe);
        }
    }

    // Start sending RTCP packets
    public void start() {
        rtcpTimer.start();
    }

    public void pause() {
        rtcpTimer.stop();
    }

    // Stop sending RTCP packets
    public void stop() {
        rtcpTimer.stop();
        RTCPsocket.close();
    }
}