package Server;

import Utils.RtspRequest;
import Utils.RtspState;

import java.io.*;
import java.net.*;
import java.util.*;

public class ServerInstance {
    private Thread serverThread = null;

    private RtpSender rtpSender;

    private InetAddress ClientIPAddress;   //Client IP address
    private int RTP_dest_port = 0;      //destination port for RTP packets  (given by the RTSP Client)
    private int RTSP_dest_port;
    private static final int MJPEG_TYPE = 26; //RTP payload type for MJPEG video

    //RTSP variables
    //----------------
    private RtspState state; //RTSP ServerInstance.ServerInstance state == INIT or READY or PLAY
    private Socket RtspSocket; //socket used to send/receive RTSP messages
    //input and output stream filters
    private BufferedReader RTSPBufferedReader;
    private BufferedWriter RTSPBufferedWriter;
    private String VideoFileName; //video file requested from the client
    private int RTSPid = IdGenerator.newId(); //ID of the RTSP session
    private int RTSPSeqNb = 0; //Sequence number of RTSP messages within the session

    private int groupId;
    private int lastVideoTime;

    //RTCP variables
    //----------------
    private RtcpReceiver rtcpReceiver;

    //Performance optimization and Congestion control
    private CongestionController cc;
    
    private final static String CRLF = "\r\n";
    private boolean killConnection;

    //--------------------------------
    //Constructor
    //--------------------------------
    public ServerInstance(int RTP_dest_port) {

        //init RTP sending Timer
        cc = new CongestionController(600);

        //Initiate RTSPstate
        state = RtspState.INIT;

        RTSP_dest_port = RTP_dest_port;

        killConnection = false;
    }

    private void stop() {
        //send back response
        sendResponse();
        //stop timer
        rtpSender.stop();
        rtcpReceiver.stop();
        //update state
        state = RtspState.READY;
        System.out.println("New RTSP state: READY");
    }

    private void play() {
        //send back response
        sendResponse();
        //start timer
        rtpSender.start();
        rtcpReceiver.start();
        //update state
        state = RtspState.PLAYING;
        System.out.println("New RTSP state: PLAYING");
    }

    private void setup() throws Exception {
        //update RTSP state
        state = RtspState.READY;
        System.out.println("New RTSP state: READY");

        //Send response
        sendResponse();

        //init the ServerInstance.VideoStream object:
        VideoStream video = new VideoStream(VideoFileName);

        //init RTP and RTCP sockets
        rtpSender = new RtpSender(video, cc, RTP_dest_port, ClientIPAddress);

        rtcpReceiver = RtcpReceiver.getInstance(RTSPid, cc);

    }

    //------------------------------------
    //Parse RTSP Request
    //------------------------------------
    private RtspRequest parseRequest() {
        RtspRequest request_type = null;
        System.out.println("^&%$%Q£");
        try {
            //parse request line and extract the request_type:
            String RequestLine = RTSPBufferedReader.readLine();
            System.out.println("RTSP ServerInstance.ServerInstance - Received from RtspClient.RtspClient:");
            System.out.println(RequestLine);

            StringTokenizer tokens = new StringTokenizer(RequestLine);
            String request_type_string = tokens.nextToken();

            //convert to request_type structure:
            if ((request_type_string).compareTo("SETUP") == 0)
                request_type = RtspRequest.SETUP;
            else if ((request_type_string).compareTo("PLAY") == 0)
                request_type = RtspRequest.PLAY;
            else if ((request_type_string).compareTo("PAUSE") == 0)
                request_type = RtspRequest.PAUSE;
            else if ((request_type_string).compareTo("TEARDOWN") == 0)
                request_type = RtspRequest.TEARDOWN;
            else if ((request_type_string).compareTo("DESCRIBE") == 0)
                request_type = RtspRequest.DESCRIBE;
            if (request_type == RtspRequest.SETUP) {
                //extract VideoFileName from RequestLine
                VideoFileName = tokens.nextToken();
            }

            //parse the SeqNumLine and extract CSeq field
            String SeqNumLine = RTSPBufferedReader.readLine();
            System.out.println(SeqNumLine);
            tokens = new StringTokenizer(SeqNumLine);
            tokens.nextToken();
            RTSPSeqNb = Integer.parseInt(tokens.nextToken());
        
            //get LastLine
            String LastLine = RTSPBufferedReader.readLine();
            System.out.println(LastLine);

            tokens = new StringTokenizer(LastLine);
            if (request_type == RtspRequest.SETUP) {
                //extract RTP_dest_port from LastLine
                for (int i=0; i<3; i++)
                    tokens.nextToken(); //skip unused stuff
                RTP_dest_port = Integer.parseInt(tokens.nextToken());
                tokens.nextToken(); //skip Group:
                groupId = Integer.parseInt(tokens.nextToken());
            }
            else if (request_type == RtspRequest.DESCRIBE) {
                tokens.nextToken();
                String describeDataType = tokens.nextToken();
            }
            else {
                //otherwise LastLine will be the SessionId line
                tokens.nextToken(); //skip Session:
                RTSPid = Integer.parseInt(tokens.nextToken());
                tokens.nextToken(); //skip VideoTime:
                lastVideoTime = Integer.parseInt(tokens.nextToken());

            }
        } catch(Exception ex) {
            System.out.println("Exception caught: "+ex);
            System.exit(0);
        }
        System.out.println("EndParseREq");
        return request_type;
    }

    // Creates a DESCRIBE response string in SDP format for current media
    private String describe() {
        StringWriter writer1 = new StringWriter();
        StringWriter writer2 = new StringWriter();
        
        // Write the body first so we can get the size later
        writer2.write("v=0" + CRLF);
        writer2.write("m=video " + RTSP_dest_port + " RTP/AVP " + MJPEG_TYPE + CRLF);
        writer2.write("a=control:streamid=" + RTSPid + CRLF);
        writer2.write("a=mimetype:string;\"video/MJPEG\"" + CRLF);
        String body = writer2.toString();

        writer1.write("Content-Base: " + VideoFileName + CRLF);
        writer1.write("Content-Type: " + "application/sdp" + CRLF);
        writer1.write("Content-Length: " + body.length() + CRLF);
        writer1.write(body);
        
        return writer1.toString();
    }

    //------------------------------------
    //Send RTSP Response
    //------------------------------------
    private void sendResponse() {
        try {
            RTSPBufferedWriter.write("RTSP/1.0 200 OK"+CRLF);
            RTSPBufferedWriter.write("CSeq: "+RTSPSeqNb+CRLF);
            RTSPBufferedWriter.write("Session: "+RTSPid+CRLF);
            RTSPBufferedWriter.flush();
            System.out.println("RTSP Server - Sent response to Client");
        } catch(Exception ex) {
            System.out.println("Exception caught: "+ex);
            System.exit(0);
        }
    }

    private void sendDescribe() {
        String des = describe();
        try {
            RTSPBufferedWriter.write("RTSP/1.0 200 OK"+CRLF);
            RTSPBufferedWriter.write("CSeq: "+RTSPSeqNb+CRLF);
            RTSPBufferedWriter.write(des);
            RTSPBufferedWriter.flush();
            System.out.println("RTSP Server - Sent response to Client.");
        } catch(Exception ex) {
            System.out.println("Exception caught: "+ex);
            System.exit(0);
        }
    }

    public void serverShutDown() {
        System.out.println("%0%");
        killConnection = true;
        try {
            RTSPBufferedReader.close();
            RTSPBufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        rtpSender.stop();
        rtcpReceiver.stop();
        System.out.println("%2%");
       // System.exit(0);

    }

    public void serverStart(Socket socket) throws IOException {
        //Initiate TCP connection with the client for the RTSP session
        RtspSocket = socket;

        //Get RtspClient.RtspClient IP address
        ClientIPAddress = RtspSocket.getInetAddress();

        //Set input and output stream filters:
        RTSPBufferedReader = new BufferedReader(new InputStreamReader(RtspSocket.getInputStream()) );
        RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(RtspSocket.getOutputStream()) );

        serverThread = new Thread(this::serverRun);
        serverThread.start();
        System.out.println("end server start");
    }

    private void serverRun() {
        //Wait for the SETUP message from the client
        RtspRequest request_type;
        boolean done = false;
        while(!done && !killConnection) {
            request_type = parseRequest(); //blocking
            if (request_type == RtspRequest.SETUP) {
                done = true;
                try {
                    setup();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("£££ Group: " + groupId);
        //loop to handle RTSP requests
        while(!killConnection) {
            //parse the request
            request_type = parseRequest(); //blocking

            if ((request_type == RtspRequest.PLAY) && (state == RtspState.READY)) {
                play();
            }
            else if ((request_type == RtspRequest.PAUSE) && (state == RtspState.PLAYING)) {
                stop();
            }
            else if (request_type == RtspRequest.TEARDOWN) {
                //send back response
                sendResponse();
                serverShutDown();
            }
            else if (request_type == RtspRequest.DESCRIBE) {
                System.out.println("Received DESCRIBE request");
                sendDescribe();
            }
            System.out.println("£££ VideoTime: " + lastVideoTime);
        }

    }
    //------------------------------------
    //main
    //------------------------------------
    public static void main(String argv[]) throws Exception {
        int RTSPport = Integer.parseInt(argv[0]);
        ServerSocket listenSocket = new ServerSocket(RTSPport);
        while(true) {
            ServerInstance serverInstance = new ServerInstance(RTSPport);
            Socket socket = listenSocket.accept();
            System.out.println("accept");
            serverInstance.serverStart(socket);
        }
        //listenSocket.close();


    }


}


