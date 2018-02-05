package Client;

import java.io.*;
import java.net.*;
import java.util.*;

public class RtspClient {

    private final static String CRLF = "\r\n";

    //RTSP
    private final static int INIT = 0;
    private final static int READY = 1;
    private final static int PLAYING = 2;
    private int state;
    private Socket RTSPsocket;           //socket used to send/receive RTSP messages
    private BufferedReader RTSPBufferedReader;
    private BufferedWriter RTSPBufferedWriter;
    private String VideoFileName; //video file to request to the server
    private int RTSPSeqNb;           //Sequence number of RTSP messages within the session
    private String RTSPid;              // ID of the RTSP session (given by the RTSP Server.Server)

    //RTCP
    private RtcpSender rtcpSender;

    //RTP
    private RtpReceiver rtpReceiver;

    private final Stats stats; // delete this


    public RtspClient(InetAddress serverIPAddr, int RTSP_server_port, String filename, Stats stats, RtpReceiver rtpReceiver, RtcpSender rtcpSender) throws Exception {

        this.stats = stats;

        this.rtcpSender = rtcpSender;
        this.rtpReceiver = rtpReceiver;

        VideoFileName = filename;

        RTSPsocket = new Socket(serverIPAddr, RTSP_server_port);

        RTSPBufferedReader = new BufferedReader(new InputStreamReader(RTSPsocket.getInputStream()));
        RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(RTSPsocket.getOutputStream()));

        state = INIT;

        RTSPSeqNb = 0;
    }

    public void setup(){
        System.out.println("Setup Button pressed !");
        if (state == INIT) {
            try {
                rtpReceiver.openSocket();
                rtcpSender.openSocket();
            }
            catch (SocketException se)
            {
                System.out.println("Socket exception: "+se);
                System.exit(0);
            }

            RTSPSeqNb = 0;

            sendRequest("SETUP");

            if (parseServerResponse() != 200)
                System.out.println("Invalid Server.Server Response");
            else
            {
                state = READY;
                System.out.println("New RTSP state: READY");
            }
        }
    }

    public void play() {
        System.out.println("Play Button pressed!");
        stats.setStartTime(System.currentTimeMillis());
        if (state == READY) {
            sendRequest("PLAY");
            if (parseServerResponse() != 200) {
                System.out.println("Invalid Server.Server Response");
            }
            else {
                state = PLAYING;
                System.out.println("New RTSP state: PLAYING");
                rtpReceiver.start();
                rtcpSender.start();
            }
        }
    }

    public void pause(){
       System.out.println("Pause Button pressed!");
        if (state == PLAYING) {
            sendRequest("PAUSE");
            if (parseServerResponse() != 200) {
                System.out.println("Invalid Server.Server Response");
            } else {
                state = READY;
                System.out.println("New RTSP state: READY");
                rtpReceiver.pause();
                rtcpSender.pause();
            }
        }
    }

    public void teardown(){
        System.out.println("Teardown Button pressed !");
        sendRequest("TEARDOWN");
        if (parseServerResponse() != 200) {
            System.out.println("Invalid Server.Server Response");
        } else {
            state = INIT;
            System.out.println("New RTSP state: INIT");
            rtpReceiver.stop();
            rtcpSender.stop();

            System.exit(0);
        }
    }

    public void describe() {
        System.out.println("Sending DESCRIBE request");
        sendRequest("DESCRIBE");
        if (parseServerResponse() != 200) {
            System.out.println("Invalid Server.Server Response");
        } else {
            System.out.println("Received response for DESCRIBE");
        }
    }


    private int parseServerResponse() {
        int reply_code = 0;
        try {
            //parse status line and extract the reply_code:
            String StatusLine = RTSPBufferedReader.readLine();
            System.out.println("RTSP RtspClient.RtspClient - Received from Server.Server:");
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

    private void sendRequest(String request_type) {
        RTSPSeqNb ++;
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
                    RTSPBufferedWriter.write("Transport: RTP/UDP; client_port= " + rtpReceiver.getLocalPort() + CRLF);
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
