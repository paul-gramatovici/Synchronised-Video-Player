package Client;

public class Stats {
    //Statistics variables:
    //------------------
    private double dataRate = 0;        //Rate of video data received in bytes/s
    private int totalBytes = 0;         //Total number of bytes received in a session
    private double startTime = 0;       //Time in milliseconds when start is pressed
    private double totalPlayTime = 0;   //Time in milliseconds of video playing since beginning
    private float fractionLost = 0;     //Fraction of RTP data packets from sender lost since the prev packet was sent
    private int cumLost = 0;            //Number of packets lost
    private int expRtpNb = 0;           //Expected Sequence number of RTP messages within the session
    private int highSeqNb = 0;          //Highest sequence number received in session

    public double getDataRate() {
        return dataRate;
    }

    public int getTotalBytes() {
        return totalBytes;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public float getFractionLost() {
        return fractionLost;
    }

    public int getCumLost() {
        return cumLost;
    }

    public int getHighSeqNb() {
        return highSeqNb;
    }

    public void update(int seqNb, int payloadLength) {
        double curTime = System.currentTimeMillis();
        totalPlayTime += curTime - startTime;
        startTime = curTime;

        //compute stats and update the label in GUI
        expRtpNb++;
        if (seqNb > highSeqNb) {
            highSeqNb = seqNb;
        }
        if (expRtpNb != seqNb) {
            cumLost++;
        }
        dataRate = totalPlayTime == 0 ? 0 : (totalBytes / (totalPlayTime / 1000.0));
        fractionLost = (float)cumLost / highSeqNb;
        totalBytes += payloadLength;
    }
}
