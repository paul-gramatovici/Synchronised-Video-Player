package Server;

import java.io.*;
import java.net.Socket;
import java.util.StringTokenizer;

public class VspSender {
  private final static String CRLF = "\r\n";


  private Socket socket;
  private int groupId;
  private int sessionId;
  private BufferedWriter writer;

  public VspSender(Socket socket) throws IOException {
    this.socket = socket;
    writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    String info = reader.readLine();
    StringTokenizer tokenizer = new StringTokenizer(info);
    tokenizer.nextToken();
    sessionId = Integer.parseInt(tokenizer.nextToken());
    tokenizer.nextToken();
    groupId = Integer.parseInt(tokenizer.nextToken());
    System.out.println(sessionId + " " + groupId);
  }

  public void sendPlay(int time) {
    try {
      writer.write("PLAY " + time + CRLF);
      writer.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void sendPause(int time) {
    try {
      writer.write("PAUSE " + time + CRLF);
      writer.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public int getSessionId() {
    return sessionId;
  }
}
