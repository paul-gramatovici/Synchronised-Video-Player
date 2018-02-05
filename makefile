JC = javac
J = java

default: Client.RtspClient.class Server.Server.class Server.VideoStream.class Packets.RTPpacket.class Packets.RTCPpacket.class

Client.RtspClient.class: Client.RtspClient.java
	$(JC) $(JFLAGS) Client.RtspClient.java
Server.Server.class: Server.Server.java
	$(JC) $(JFLAGS) Server.Server.java
Server.VideoStream.class: Server.VideoStream.java
	$(JC) $(JFLAGS) Server.VideoStream.java
Packets.RTPpacket.class: Packets.RTPpacket.java
	$(JC) $(JFLAGS) Packets.RTPpacket.java
Packets.RTCPpacket.class: Packets.RTCPpacket.java
	$(JC) $(JFLAGS) Packets.RTCPpacket.java
clean:
	rm -f *.class
