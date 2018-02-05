package Client;/* ------------------
   Client.Client
   usage: java Client.Client [Server.Server hostname] [Server.Server RTSP listening port] [Video file requested]
   ---------------------- */

import Packets.RTPpacket;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.Timer;

public class Client {
    //GUI
    //----
    private GUI gui;

    private DatagramSocket RTPsocket;        //socket to be used to send and receive UDP packets

    private Timer timer; //timer used to receive data from the UDP socket
    private byte[] buf;  //buffer used to store data received from the server
   
    //RTSP variables
    //----------------
    //rtsp states 
    private final static int INIT = 0;
    private final static int READY = 1;
    private final static int PLAYING = 2;
    private int state;            //RTSP state == INIT or READY or PLAYING
    private Socket RTSPsocket;           //socket used to send/receive RTSP messages
    private InetAddress serverIPAddr;

    //input and output stream filters
    private BufferedReader RTSPBufferedReader;
    private BufferedWriter RTSPBufferedWriter;
    private String VideoFileName; //video file to request to the server
    private int RTSPSeqNb = 0;           //Sequence number of RTSP messages within the session
    private String RTSPid;              // ID of the RTSP session (given by the RTSP Server.Server)

    private final static String CRLF = "\r\n";

    //RTCP variables
    //----------------
    private DatagramSocket RTCPsocket;          //UDP socket for sending RTCP packets
    private RtcpSender rtcpSender;

    //Statistics variables:
    //------------------
    private double statDataRate;        //Rate of video data received in bytes/s
    private int statTotalBytes;         //Total number of bytes received in a session
    private double statStartTime;       //Time in milliseconds when start is pressed
    private double statTotalPlayTime;   //Time in milliseconds of video playing since beginning
    private float statFractionLost;     //Fraction of RTP data packets from sender lost since the prev packet was sent
    private int statCumLost;            //Number of packets lost
    private int statExpRtpNb;           //Expected Sequence number of RTP messages within the session
    private int statHighSeqNb;          //Highest sequence number received in session

    private FrameSynchronizer fsynch;
   
    //--------------------------
    //Constructor
    //--------------------------
    public Client() {

        gui = new GUI(this);

        //init timer
        //--------------------------
        timer = new Timer(20, new timerListener());
        timer.setInitialDelay(0);
        timer.setCoalesce(true);

        //init RTCP packet sender
        rtcpSender = new RtcpSender(this,400);

        //allocate enough memory for the buffer used to receive data from the server
        buf = new byte[15000];    

        //create the frame synchronizer
        fsynch = new FrameSynchronizer(100);
    }

    //------------------------------------
    //main
    //------------------------------------
    public static void main(String argv[]) throws Exception {
        //Create a Client.Client object
        Client client = new Client();
        client.startClient(argv[0], Integer.parseInt(argv[1]), argv[2]);
    }

    public void startClient(String serverHost, int RTSP_server_port, String filename) throws Exception {

        serverIPAddr = InetAddress.getByName(serverHost);

        //get video filename to request:
        VideoFileName = filename;

        //Establish a TCP connection with the server to exchange RTSP messages
        //------------------
        RTSPsocket = new Socket(serverIPAddr, RTSP_server_port);

        //Set input and output stream filters:
        RTSPBufferedReader = new BufferedReader(new InputStreamReader(RTSPsocket.getInputStream()));
        RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(RTSPsocket.getOutputStream()));

        //init RTSP state:
        state = INIT;
    }

    public int getStatTotalBytes() {
        return statTotalBytes;
    }

    public float getStatFractionLost() {
        return statFractionLost;
    }

    public double getStatDataRate() {
        return statDataRate;
    }


    public void setup(){

        System.out.println("Setup Button pressed !");
        if (state == INIT) {
            //Init non-blocking RTPsocket that will be used to receive data
            try {
                //construct a new DatagramSocket to receive RTP packets from the server, on port RTP_RCV_PORT
                RTPsocket = new DatagramSocket();
                //UDP socket for sending QoS RTCP packets
                RTCPsocket = new DatagramSocket();
                //set TimeOut value of the socket to 5msec.
                RTPsocket.setSoTimeout(5);
            }
            catch (SocketException se)
            {
                System.out.println("Socket exception: "+se);
                System.exit(0);
            }

            //init RTSP sequence number
            RTSPSeqNb = 1;

            //Send SETUP message to the server
            sendRequest("SETUP");

            //Wait for the response
            if (parseServerResponse() != 200)
                System.out.println("Invalid Server.Server Response");
            else
            {
                //change RTSP state and print new state
                state = READY;
                System.out.println("New RTSP state: READY");
            }
        }
        //else if state != INIT then do nothing
    }


    public void play() {

        System.out.println("Play Button pressed!");

        //Start to save the time in stats
        statStartTime = System.currentTimeMillis();

        if (state == READY) {
            //increase RTSP sequence number
            RTSPSeqNb++;

            //Send PLAY message to the server
            sendRequest("PLAY");

            //Wait for the response
            if (parseServerResponse() != 200) {
                System.out.println("Invalid Server.Server Response");
            }
            else {
                //change RTSP state and print out new state
                state = PLAYING;
                System.out.println("New RTSP state: PLAYING");

                //start the timer
                timer.start();
                rtcpSender.startSend();
            }
        }
        //else if state != READY then do nothing
    }


    public void pause(){

        System.out.println("Pause Button pressed!");

        if (state == PLAYING)
        {
            //increase RTSP sequence number
            RTSPSeqNb++;

            //Send PAUSE message to the server
            sendRequest("PAUSE");

            //Wait for the response
            if (parseServerResponse() != 200)
                System.out.println("Invalid Server.Server Response");
            else
            {
                //change RTSP state and print out new state
                state = READY;
                System.out.println("New RTSP state: READY");

                //stop the timer
                timer.stop();
                rtcpSender.stopSend();
            }
        }
        //else if state != PLAYING then do nothing
    }



    public void teardown(){

        System.out.println("Teardown Button pressed !");

        //increase RTSP sequence number
        RTSPSeqNb++;

        //Send TEARDOWN message to the server
        sendRequest("TEARDOWN");

        //Wait for the response
        if (parseServerResponse() != 200)
            System.out.println("Invalid Server.Server Response");
        else {
            //change RTSP state and print out new state
            state = INIT;
            System.out.println("New RTSP state: INIT");

            //stop the timer
            timer.stop();
            rtcpSender.stopSend();

            //exit
            System.exit(0);
        }
    }

    public void describe() {
        System.out.println("Sending DESCRIBE request");

        //increase RTSP sequence number
        RTSPSeqNb++;

        //Send DESCRIBE message to the server
        sendRequest("DESCRIBE");

        //Wait for the response
        if (parseServerResponse() != 200) {
            System.out.println("Invalid Server.Server Response");
        }
        else {
            System.out.println("Received response for DESCRIBE");
        }
    }

    public int getStatCumLost() {
        return statCumLost;
    }

    public int getStatHighSeqNb() {
        return statHighSeqNb;
    }

    public InetAddress getServerIPAddr() {
        return serverIPAddr;
    }

    public DatagramSocket getRTCPsocket() {
        return RTCPsocket;
    }


    //------------------------------------
    //Handler for timer
    //------------------------------------
    class timerListener implements ActionListener {


        public void actionPerformed(ActionEvent e) {
          
            //Construct a DatagramPacket to receive data from the UDP socket
            DatagramPacket rcvdp = new DatagramPacket(buf, buf.length);

            try {
                //receive the DP from the socket, save time for stats
                RTPsocket.receive(rcvdp);

                double curTime = System.currentTimeMillis();
                statTotalPlayTime += curTime - statStartTime; 
                statStartTime = curTime;

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
                int payload_length = rtp_packet.getpayload_length();
                byte [] payload = new byte[payload_length];
                rtp_packet.getpayload(payload);

                //compute stats and update the label in GUI
                statExpRtpNb++;
                if (seqNb > statHighSeqNb) {
                    statHighSeqNb = seqNb;
                }
                if (statExpRtpNb != seqNb) {
                    statCumLost++;
                }
                statDataRate = statTotalPlayTime == 0 ? 0 : (statTotalBytes / (statTotalPlayTime / 1000.0));
                statFractionLost = (float)statCumLost / statHighSeqNb;
                statTotalBytes += payload_length;
                gui.updateStatsLabel();

                //get an Image object from the payload bitstream
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                fsynch.addFrame(toolkit.createImage(payload, 0, payload_length), seqNb);

                //update video frame
                gui.setNextFrame(fsynch.nextFrame());
            }
            catch (InterruptedIOException iioe) {
                System.out.println("Nothing to read");
            }
            catch (IOException ioe) {
                System.out.println("Exception caught: "+ioe);
            }
        }
    }


    //------------------------------------
    //Parse Server.Server Response
    //------------------------------------
    private int parseServerResponse() {
        int reply_code = 0;

        try {
            //parse status line and extract the reply_code:
            String StatusLine = RTSPBufferedReader.readLine();
            System.out.println("RTSP Client.Client - Received from Server.Server:");
            System.out.println(StatusLine);

            StringTokenizer tokens = new StringTokenizer(StatusLine);
            tokens.nextToken(); //skip over the RTSP version
            reply_code = Integer.parseInt(tokens.nextToken());

            //if reply code is OK get and print the 2 other lines
            if (reply_code == 200) {
                String SeqNumLine = RTSPBufferedReader.readLine();
                System.out.println(SeqNumLine);

                String SessionLine = RTSPBufferedReader.readLine();
                System.out.println(SessionLine);

                tokens = new StringTokenizer(SessionLine);
                String temp = tokens.nextToken();
                //if state == INIT gets the Session Id from the SessionLine
                if (state == INIT && temp.compareTo("Session:") == 0) {
                    RTSPid = tokens.nextToken();
                }
                else if (temp.compareTo("Content-Base:") == 0) {
                    // Get the DESCRIBE lines
                    String newLine;
                    for (int i = 0; i < 6; i++) {
                        newLine = RTSPBufferedReader.readLine();
                        System.out.println(newLine);
                    }
                }
            }
        } catch(Exception ex) {
            System.out.println("Exception caught: "+ex);
            System.exit(0);
        }

        return(reply_code);
    }



    //------------------------------------
    //Send RTSP Request
    //------------------------------------

    private void sendRequest(String request_type) {
        try {
            //Use the RTSPBufferedWriter to write to the RTSP socket

            //write the request line:
            RTSPBufferedWriter.write(request_type + " " + VideoFileName + " RTSP/1.0" + CRLF);

            //write the CSeq line: 
            RTSPBufferedWriter.write("CSeq: " + RTSPSeqNb + CRLF);

            //check if request_type is equal to "SETUP" and in this case write the 
            //Transport: line advertising to the server the port used to receive 
            //the RTP packets RTP_RCV_PORT
            switch (request_type) {
                case "SETUP":
                    RTSPBufferedWriter.write("Transport: RTP/UDP; client_port= " + RTPsocket.getLocalPort() + CRLF);
                    break;
                case "DESCRIBE":
                    RTSPBufferedWriter.write("Accept: application/sdp" + CRLF);
                    break;
                default:
                    //otherwise, write the Session line from the RTSPid field
                    RTSPBufferedWriter.write("Session: " + RTSPid + CRLF);
                    break;
            }

            RTSPBufferedWriter.flush();
        } catch(Exception ex) {
            System.out.println("Exception caught: "+ex);
            System.exit(0);
        }
    }    
}
