package Client;

import java.awt.*;

public class VideoFrame {
  private Image image;
  private int frameNumber;

  public VideoFrame(Image image, int frameNumber) {
    this.image = image;
    this.frameNumber = frameNumber;
  }

  public Image getImage() {
    return image;
  }

  public int getFrameNumber() {
    return frameNumber;
  }
}
