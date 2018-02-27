package Client;

import Utils.RtspState;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

public class VspReceiver {
  private final static String CRLF = "\r\n";
  private Socket vspSocket;
  private BufferedReader vspBufferedReader;
  private BufferedWriter vspBufferedWriter;

  private boolean killConnection;
  private VideoPlayer videoPlayer;
  private int rtspId;
  private int groupId;
  private Thread thread;
  RtspClient rtspClient;

  public VspReceiver(InetAddress serverIPAddr, int vspPort, VideoPlayer videoPlayer, int rtspId, int groupId, RtspClient rtspClient) throws IOException {
    this.videoPlayer = videoPlayer;
    this.rtspId = rtspId;
    this.groupId = groupId;
    this.rtspClient = rtspClient;
    this.vspSocket = new Socket(serverIPAddr, vspPort);
    this.vspBufferedReader = new BufferedReader(new InputStreamReader(vspSocket.getInputStream()));
    this.vspBufferedWriter = new BufferedWriter(new OutputStreamWriter(vspSocket.getOutputStream()));
    this.killConnection = false;
  }

  public void start() {
    try {
      vspBufferedWriter.write("Session: " + rtspId + " Group: " + groupId + CRLF);
      vspBufferedWriter.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
    thread = new Thread(this::listen);
    thread.start();
  }

  public void listen() {
    while (!killConnection) {
      System.out.println("LISTENING");
      try {
        String request = vspBufferedReader.readLine();
        //System.out.println(request);
        StringTokenizer tokenizer = new StringTokenizer(request);
        String requestType = tokenizer.nextToken();
        int videoTime = Integer.parseInt(tokenizer.nextToken());
        switch (requestType) {
          case "PLAY":
            videoPlayer.setVideoTime(videoTime);
            if (!videoPlayer.isPlaying()) {
              rtspClient.setState(RtspState.PLAYING);
              videoPlayer.start();
            }
            break;
          default:
            System.out.println("\nGOT PAUSE\n");
            rtspClient.setState(RtspState.READY);
            videoPlayer.stopAt(videoTime);
            break;
        }

      } catch (IOException e) {
        //System.out.println("closing Vsp");
      }
    }
    System.out.println("Closing");
  }

  public void stop() {

    killConnection = true;
    try {
      vspSocket.close();
      vspBufferedReader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      thread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    System.out.println("stop");
  }
}

