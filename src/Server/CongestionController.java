package Server;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;



public class CongestionController implements ActionListener {
    private static int FRAME_PERIOD = 33; //Frame period of the video to stream, in ms

    private Timer ccTimer;
    private int prevLevel;  //previously sampled congestion level

    private int congestionLevel;

    private int sendDelay;  //the delay to send images over the wire. Ideally should be
    //equal to the frame rate of the video file, but may be
    //adjusted when congestion is detected.

    public CongestionController(int interval) {
        sendDelay = FRAME_PERIOD;
        ccTimer = new Timer(interval, this);
        ccTimer.start();
    }

    public void actionPerformed(ActionEvent e) {

        //adjust the send rate
        if (prevLevel != congestionLevel) {
            sendDelay = FRAME_PERIOD + congestionLevel * (int)(FRAME_PERIOD * 0.1);
            prevLevel = congestionLevel;
            System.out.println("Send delay changed to: " + sendDelay);
        }
    }

    public void updateLevel(float fractionLost) {
        if (fractionLost >= 0 && fractionLost <= 0.01) {
            congestionLevel = 0;    //less than 0.01 assume negligible
        }
        else if (fractionLost > 0.01 && fractionLost <= 0.25) {
            congestionLevel = 1;
        }
        else if (fractionLost > 0.25 && fractionLost <= 0.5) {
            congestionLevel = 2;
        }
        else if (fractionLost > 0.5 && fractionLost <= 0.75) {
            congestionLevel = 3;
        }
        else {
            congestionLevel = 4;
        }
        System.out.println("CONGESTION: " + congestionLevel + "\n\n");
    }

    public int getDelay() {
        return sendDelay;
    }

    public float getCompressionQuality() {
        return 1.0f - congestionLevel * 0.2f;
    }
}