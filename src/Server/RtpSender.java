package Server;

import Packets.RTPpacket;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;


public class RtpSender implements ActionListener {
    private DatagramSocket RTPsocket; //socket to be used to send and receive UDP packets
    private int destinationPort;
    private InetAddress clientIP;
    private Timer timer;
    private CongestionController cc;

    private ImageTranslator imgTranslator;


    //Video variables:
    //----------------
    private int imagenb = 0; //image nb of the image currently transmitted
    private VideoStream video; //ServerInstance.VideoStream object used to access video frames
    private static final int MJPEG_TYPE = 26; //RTP payload type for MJPEG video
    private static final int FRAME_PERIOD = 33; //Frame period of the video to stream, in ms
    private static final int VIDEO_LENGTH = 500; //length of the video in frames

    //private Timer timer;    //timer used to send the images at the video frame rate
    private byte[] buf;     //buffer used to store the images to send to the client

    RtpSender(VideoStream video, CongestionController cc, int destinationPort, InetAddress clientIP) {
        this.cc = cc;
        this.destinationPort = destinationPort;
        this.clientIP = clientIP;
        try {
            RTPsocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        timer = new Timer(FRAME_PERIOD, this);
        timer.setInitialDelay(0);
        timer.setCoalesce(true);

        //init the ServerInstance.VideoStream object:
        this.video = video;

        buf = new byte[20000];
        imgTranslator = new ImageTranslator(0.8f);
    }

    public void start() {
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        byte[] frame;

        //if the current image nb is less than the length of the video
        if (imagenb < VIDEO_LENGTH) {
            //update current imagenb
            imagenb++;

            try {
                //get next frame to send from the video, as well as its size
                int image_length = video.getnextframe(buf);

                //adjust quality of the image if there is congestion detected
                float quality = cc.getCompressionQuality();
                if (quality < 1.0f) {
                    imgTranslator.setCompressionQuality(quality);
                    frame = imgTranslator.compress(Arrays.copyOfRange(buf, 0, image_length));
                    image_length = frame.length;
                    System.arraycopy(frame, 0, buf, 0, image_length);
                }

                //Builds an Packets.RTPpacket object containing the frame
                RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, imagenb*FRAME_PERIOD, buf, image_length);

                //get to total length of the full rtp packet to send
                int packet_length = rtp_packet.getlength();

                //retrieve the packet bitstream and store it in an array of bytes
                byte[] packet_bits = new byte[packet_length];
                rtp_packet.getpacket(packet_bits);

                //send the packet as a DatagramPacket over the UDP socket
                DatagramPacket senddp = new DatagramPacket(packet_bits, packet_length, clientIP, destinationPort);
                RTPsocket.send(senddp);

                System.out.println("Send frame #" + imagenb + ", Frame size: " + image_length + " (" + buf.length + ")");
                //print the header bitstream
                rtp_packet.printheader();

                //gui.update(imagenb);

            }
            catch(Exception ex) {
                System.out.println("Exception caught: "+ex);
                System.exit(0);
            }
        }
        else {
            //if we have reached the end of the video file, stop the timer
            timer.stop();
            //rtcpReceiver.stopRcv();
        }
        timer.setDelay(cc.getDelay());
    }

  public boolean isRunning() {
        return timer.isRunning();
  }
}
