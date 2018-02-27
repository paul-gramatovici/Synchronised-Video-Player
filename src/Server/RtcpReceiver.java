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
import java.util.HashMap;
import java.util.Map;

public class RtcpReceiver implements ActionListener {

    private static RtcpReceiver instance = null;

    private static final int RTCP_RCV_PORT = 19001;
    private static final int RTCP_PERIOD = 50;     //How often to check for control events

    private Timer rtcpTimer;
    private byte[] rtcpBuf;

    private DatagramSocket RTCPsocket;

    private Map<Integer,CongestionController> congestionControllers;

    public static RtcpReceiver getInstance(Integer session, CongestionController congestionController) {
        if(instance == null)
            instance = new RtcpReceiver();
        instance.congestionControllers.put(session, congestionController);
        return instance;
    }

    private RtcpReceiver() {
        //set timer with interval for receiving packets
        congestionControllers = new HashMap<>();
        rtcpTimer = new Timer(RTCP_PERIOD, this);
        rtcpTimer.setInitialDelay(0);
        rtcpTimer.setCoalesce(true);

        //allocate buffer for receiving RTCP packets
        rtcpBuf = new byte[512];

        try {
            RTCPsocket = new DatagramSocket(RTCP_RCV_PORT);
            RTCPsocket.setSoTimeout(5);

        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void actionPerformed(ActionEvent e) {
        //Construct a DatagramPacket to receive data from the UDP socket
        DatagramPacket dp = new DatagramPacket(rtcpBuf, rtcpBuf.length);
        float fractionLost;
        int session;
        try {
            RTCPsocket.receive(dp);   // Blocking
            RTCPpacket rtcpPkt = new RTCPpacket(dp.getData(), dp.getLength());
            System.out.println("[RTCP] " + rtcpPkt);
            //set congestion level between 0 to 4
            fractionLost = rtcpPkt.getFractionLost();
            session = rtcpPkt.getSession();
            CongestionController cc = congestionControllers.get(session);
            cc.updateLevel(fractionLost);

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

    public boolean isRunning() {
        return rtcpTimer.isRunning();
    }

}
