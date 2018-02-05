JC = javac
J = java

default: Client.Client.class Server.Server.class Server.VideoStream.class Packets.RTPpacket.class Packets.RTCPpacket.class

Client.Client.class: Client.Client.java
	$(JC) $(JFLAGS) Client.Client.java
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
