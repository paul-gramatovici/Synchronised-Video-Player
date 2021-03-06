package Packets;//class Packets.RTPpacket

import java.util.*;

public class RTPpacket{

    //size of the RTP header:
    private static int HEADER_SIZE = 12;

    //Fields that compose the RTP header
    private int Version;
    private int Padding;
    private int Extension;
    private int CC;
    private int Marker;
    private int PayloadType;
    private int SequenceNumber;
    private int TimeStamp;
    private int Ssrc;
    
    //Bitstream of the RTP header
    private byte[] header;

    //size of the RTP payload
    private int payload_size;
    //Bitstream of the RTP payload
    private byte[] payload;
    
    //--------------------------
    //Constructor of an Packets.RTPpacket object from header fields and payload bitstream
    //--------------------------
    public RTPpacket(int PType, int Framenb, int Time, byte[] data, int data_length){
        //fill by default header fields:
        Version = 2;
        Padding = 0;
        Extension = 0;
        CC = 0;
        Marker = 0;
        Ssrc = 1337;    // Identifies the server

        //fill changing header fields:
        SequenceNumber = Framenb;
        TimeStamp = Time;
        PayloadType = PType;
        
        //build the header bistream:
        header = new byte[HEADER_SIZE];

        //fill the header array of byte with RTP header fields
        header[0] = (byte)(Version << 6 | Padding << 5 | Extension << 4 | CC);
        header[1] = (byte)(Marker << 7 | PayloadType & 0x000000FF);
        header[2] = (byte)(SequenceNumber >> 8);
        header[3] = (byte)(SequenceNumber & 0xFF); 
        header[4] = (byte)(TimeStamp >> 24);
        header[5] = (byte)(TimeStamp >> 16);
        header[6] = (byte)(TimeStamp >> 8);
        header[7] = (byte)(TimeStamp & 0xFF);
        header[8] = (byte)(Ssrc >> 24);
        header[9] = (byte)(Ssrc >> 16);
        header[10] = (byte)(Ssrc >> 8);
        header[11] = (byte)(Ssrc & 0xFF);

        //fill the payload bitstream:
        payload_size = data_length;
        payload = new byte[data_length];

        //fill payload array of byte from data (given in parameter of the constructor)
        payload = Arrays.copyOf(data, payload_size);
    }
      
    //--------------------------
    //Constructor of an Packets.RTPpacket object from the packet bistream
    //--------------------------
    public RTPpacket(byte[] packet, int packet_size)
    {
        //fill default fields:
        Version = 2;
        Padding = 0;
        Extension = 0;
        CC = 0;
        Marker = 0;
        Ssrc = 0;

        //check if total packet size is lower than the header size
        if (packet_size >= HEADER_SIZE) 
        {
            //get the header bitsream:
            header = new byte[HEADER_SIZE];
            System.arraycopy(packet, 0, header, 0, HEADER_SIZE);

            //get the payload bitstream:
            payload_size = packet_size - HEADER_SIZE;
            payload = new byte[payload_size];
            System.arraycopy(packet, HEADER_SIZE, payload, 0, packet_size - HEADER_SIZE);

            //interpret the changing fields of the header:
            Version = (header[0] & 0xFF) >>> 6;
            PayloadType = header[1] & 0x7F;
            SequenceNumber = (header[3] & 0xFF) + ((header[2] & 0xFF) << 8);
            TimeStamp = (header[7] & 0xFF) + ((header[6] & 0xFF) << 8) + ((header[5] & 0xFF) << 16) + ((header[4] & 0xFF) << 24);
        }
    }

    //--------------------------
    //getpayload: return the payload bistream of the Packets.RTPpacket and its size
    //--------------------------
    public int getpayload(byte[] data) {

        System.arraycopy(payload, 0, data, 0, payload_size);

        return(payload_size);
    }

    //--------------------------
    //getpayload_length: return the length of the payload
    //--------------------------
    public int getpayload_length() {
        return(payload_size);
    }

    //--------------------------
    //getlength: return the total length of the RTP packet
    //--------------------------
    public int getlength() {
        return(payload_size + HEADER_SIZE);
    }

    //--------------------------
    //getPacket: returns the packet bitstream and its length
    //--------------------------
    public int getpacket(byte[] packet)
    {
        //construct the packet = header + payload
        System.arraycopy(header, 0, packet, 0, HEADER_SIZE);
        System.arraycopy(payload, 0, packet, HEADER_SIZE, payload_size);

        //return total size of the packet
        return(payload_size + HEADER_SIZE);
    }

    //--------------------------
    //gettimestamp
    //--------------------------

    public int gettimestamp() {
        return(TimeStamp);
    }

    //--------------------------
    //getsequencenumber
    //--------------------------
    public int getsequencenumber() {
        return(SequenceNumber);
    }

    //--------------------------
    //getpayloadtype
    //--------------------------
    public int getpayloadtype() {
        return(PayloadType);
    }

    //--------------------------
    //print headers without the SSRC
    //--------------------------
    public void printheader()
    {
        System.out.print("[RTP-Header] ");
        System.out.println("Version: " + Version 
                           + ", Padding: " + Padding
                           + ", Extension: " + Extension 
                           + ", CC: " + CC
                           + ", Marker: " + Marker 
                           + ", PayloadType: " + PayloadType
                           + ", SequenceNumber: " + SequenceNumber
                           + ", TimeStamp: " + TimeStamp);

    }
}