package Client;

import Packets.RTPpacket;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class RtpReceiver implements ActionListener {

    private Client client;

    private DatagramSocket RTPsocket;        //socket to be used to send and receive UDP packets
    private Timer timer; //timer used to receive data from the UDP socket
    private byte[] buf;  //buffer used to store data received from the server
    private FrameSynchronizer fsynch;

    RtpReceiver(Client client) {
        this.client = client;
        timer = new Timer(20, this);
        timer.setInitialDelay(0);
        timer.setCoalesce(true);

        buf = new byte[15000];

        fsynch = new FrameSynchronizer(100);
    }

    public void openSocket() throws SocketException {
        RTPsocket = new DatagramSocket();
        RTPsocket.setSoTimeout(5);
    }

    public void start() {
        timer.start();
    }

    public void pause() {
        timer.stop();
    }

    public void stop() {
        timer.stop();
        RTPsocket.close();
    }

    public void actionPerformed(ActionEvent e) {
        //Construct a DatagramPacket to receive data from the UDP socket
        DatagramPacket rcvdp = new DatagramPacket(buf, buf.length);

        try {
            //receive the DP from the socket, save time for stats
            RTPsocket.receive(rcvdp);

            //create an Packets.RTPpacket object from the DP
            RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());
            int seqNb = rtp_packet.getsequencenumber();

            //this is the highest seq num received

            //print important header fields of the RTP packet received:
            System.out.println("Got RTP packet with SeqNum # " + seqNb
                    + " TimeStamp " + rtp_packet.gettimestamp() + " ms, of type "
                    + rtp_packet.getpayloadtype());

            //print header bitstream:
            rtp_packet.printheader();

            //get the payload bitstream from the Packets.RTPpacket object
            int payloadLength = rtp_packet.getpayload_length();
            byte [] payload = new byte[payloadLength];
            rtp_packet.getpayload(payload);

            client.getStats().update(seqNb, payloadLength);
            client.getGUI().updateStatsLabel();

            //get an Image object from the payload bitstream
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            fsynch.addFrame(toolkit.createImage(payload, 0, payloadLength), seqNb);

            //update video frame
            client.getGUI().setNextFrame(fsynch.nextFrame());
        }
        catch (InterruptedIOException iioe) {
            System.out.println("Nothing to read");
        }
        catch (IOException ioe) {
            System.out.println("Exception caught: "+ioe);
        }
    }

    public int getLocalPort() {
        return RTPsocket.getLocalPort();
    }
}