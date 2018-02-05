package Client;

import java.net.InetAddress;

public class Client {
  private GUI gui;
  private RtspClient rtspClient;

  public Client() throws Exception {
    Stats stats = new Stats();
    VideoBuffer videoBuffer = new VideoBuffer();
    InetAddress serverIP = InetAddress.getByName("localhost");
    RtcpSender rtcpSender = new RtcpSender(400,stats,serverIP);
    RtpReceiver rtpReceiver = new RtpReceiver(stats,videoBuffer);

    rtspClient = new RtspClient(serverIP, 1051, "res/movie.Mjpeg", stats, rtpReceiver, rtcpSender);
    gui = new GUI(rtspClient, videoBuffer, stats);
  }

  public static void main(String[] args) {
    try {
      new Client();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
