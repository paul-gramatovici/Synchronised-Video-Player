package Client;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class VideoBuffer {
  private List<VideoFrame> frames;
  private Lock lock = new ReentrantLock();
  private int currentFrame;
  private int lastFrameShownIndex;

  public VideoBuffer() {
    frames = new ArrayList<>();
    currentFrame = 0;
    lastFrameShownIndex = 0;
  }

  public void addFrame(Image image, int seqNum) {
    addFrame(new VideoFrame(image, seqNum));
  }

  public void addFrame(VideoFrame frame) {
    lock.lock();
    int i = frames.size()-1;
    while (i >= 0 && frames.get(i).getFrameNumber() > frame.getFrameNumber()) {
      --i;
    }
    frames.add(i+1, frame);
    lock.unlock();
  }

  public Image nextFrame() {
    currentFrame ++;
    lock.lock();
    while(lastFrameShownIndex < frames.size() - 1 &&  frames.get(lastFrameShownIndex).getFrameNumber() < currentFrame) {
      lastFrameShownIndex++;
    }
    while(lastFrameShownIndex > 0 && frames.get(lastFrameShownIndex).getFrameNumber() > currentFrame) {
      lastFrameShownIndex--;
    }
    Image image = frames.get(lastFrameShownIndex).getImage();
    lock.unlock();
    return image;
  }

}
