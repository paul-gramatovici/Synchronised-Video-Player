package Server;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class VspMasterSender {
  private static final int VSP_PORT = 12345;
  private Set<VspSender> senders;
  private boolean killConnection;
  private ServerSocket serverSocket;

  public VspMasterSender() {
    this.senders = new HashSet<>();
    this.killConnection = false;
  }

  private void acceptConnections()  {
    try {
      serverSocket = new ServerSocket(VSP_PORT);
      while(!killConnection) {
        Socket socket  = serverSocket.accept();
        VspSender vspSender = new VspSender(socket);
        senders.add(vspSender);
      }
    } catch (IOException e) {
      System.out.println("VSP Master Stopping");
    }
  }

  public void start() {
    Thread thread = new Thread(this::acceptConnections);
    thread.start();
  }

  public void stop() {
    killConnection = true;
    if(serverSocket != null) {
      try {
        serverSocket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public VspSender getVspSender(int sessionId) {
    while(true) {
      for (VspSender vspSender : senders) {
        if (vspSender.getSessionId() == sessionId) {
          return vspSender;
        }
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public static void main(String[] args) {
    VspMasterSender masterSender = new VspMasterSender();
    masterSender.start();
  }
}
