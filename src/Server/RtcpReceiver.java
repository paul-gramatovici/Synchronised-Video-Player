package Server;

import Packets.RTCPpacket;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class RtcpReceiver implements ActionListener {

    private static final int RTCP_RCV_PORT = 19001;
    private static final int RTCP_PERIOD = 400;     //How often to check for control events

    private Timer rtcpTimer;
    private byte[] rtcpBuf;

    private DatagramSocket RTCPsocket;

    private CongestionController congestionController;

    public RtcpReceiver(CongestionController congestionController) {
        //set timer with interval for receiving packets
        rtcpTimer = new Timer(RTCP_PERIOD, this);
        rtcpTimer.setInitialDelay(0);
        rtcpTimer.setCoalesce(true);

        //allocate buffer for receiving RTCP packets
        rtcpBuf = new byte[512];

        try {
            RTCPsocket = new DatagramSocket(RTCP_RCV_PORT);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        this.congestionController = congestionController;
    }

    public void actionPerformed(ActionEvent e) {
        //Construct a DatagramPacket to receive data from the UDP socket
        DatagramPacket dp = new DatagramPacket(rtcpBuf, rtcpBuf.length);
        float fractionLost;

        try {
            RTCPsocket.receive(dp);   // Blocking
            RTCPpacket rtcpPkt = new RTCPpacket(dp.getData(), dp.getLength());
            System.out.println("[RTCP] " + rtcpPkt);
            //set congestion level between 0 to 4
            fractionLost = rtcpPkt.getFractionLost();
            congestionController.updateLevel(fractionLost);

        }
        catch (InterruptedIOException iioe) {
            System.out.println("Nothing to read");
        }
        catch (IOException ioe) {
            System.out.println("Exception caught: "+ioe);
        }
    }

    public void start() {
        rtcpTimer.start();
    }

    public void stop() {
        rtcpTimer.stop();
    }

    //init socket
}
