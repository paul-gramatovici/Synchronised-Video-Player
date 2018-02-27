package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class VideoPlayer implements ActionListener {
  private final static int FRAME_PERIOD = 30;
  private int videoTime;
  private VideoBuffer videoBuffer;
  private Timer timer; //TODO: coalesce?
  private int stopTime;
  private boolean scheduledStop;

  public VideoPlayer(VideoBuffer videoBuffer) {
    this.videoTime = 0;
    this.videoBuffer = videoBuffer;
    timer = new Timer(FRAME_PERIOD, this);
    timer.setInitialDelay(0);
    stopTime = 0;
    scheduledStop = false;
  }

  public int getFramePeriod() {
    return FRAME_PERIOD;
  }

  public int getVideoTime() {
    return videoTime;
  }

  public void setVideoTime(int videoTime) {
    this.videoTime = videoTime;
  }

  public Image getFrame() {
    return videoBuffer.getFrame(videoTime/FRAME_PERIOD);
  }

  public void stop() {
    timer.stop();
  }

  public void stopAt(int stopTime) {
    this.stopTime = stopTime;
    this.scheduledStop = true;
  }

  public void start() {
    timer.start();
  }

  public boolean isPlaying() {
    return timer.isRunning();
  }

  @Override
  public void actionPerformed(ActionEvent actionEvent) {
    videoTime += FRAME_PERIOD;
    if(scheduledStop && videoTime >= stopTime) {
      timer.stop();
      videoTime = stopTime;
      scheduledStop = false;
      stopTime = 0;
    }
  }
}
