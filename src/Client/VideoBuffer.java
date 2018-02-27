package Client;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class VideoBuffer {
    private List<VideoFrame> frames;
    private Lock lock = new ReentrantLock();

    public VideoBuffer() {
        frames = new ArrayList<>();

    }

    public void addFrame(Image image, int seqNum) {
        addFrame(new VideoFrame(image, seqNum));
    }

    public void addFrame(VideoFrame frame) {
        lock.lock();
        int i = frames.size() - 1;
        while (i >= 0 && frames.get(i).getFrameNumber() > frame.getFrameNumber()) {
            --i;
        }
        frames.add(i + 1, frame);
        lock.unlock();
    }

    public Image getFrame(int frameNo) {
      lock.lock();
      if(frames.isEmpty())
        return null;
      int i,step;
      for(i = 0, step = (1<<20); step > 0; step/=2) {
        if(i+step < frames.size() && frames.get(i+step).getFrameNumber() <= frameNo) {
          i+=step;
        }
      }
      Image frame = frames.get(i).getImage();
      lock.unlock();
      return  frame;
    }



}
