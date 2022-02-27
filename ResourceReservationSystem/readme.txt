ResourceReservationSystem

Source file are located in /src/ package.
Compiled classes are located in /out/production/ResourceReservationSystem/ package.

Detailed description of all implemented moments can be found in ResourceReservation (s21366 - group 17)final).pdf file.


How to start first node:
java NetworkNode -ident 1 -tcpport 9001 A:2

Connect new node to the network:
java NetworkNode -ident 2 -tcpport 9002 -gateway localhost:9001 B:3


where:
-ident identificator of NetworkNode.
-tcpport port where NetworkNode will accept tcp connections
A:2 resources assigned to the NetworkNode. Name and quantity are divided by ':'
-gateway ip and port divided by ':' where NetworkNode should connect to the Network (first NetworkNode can't have it)

Client ask to allocate resources in the Network:
java NetworkClient -ident 1 -gateway localhost:9001 A:3

Client ask to terminate whole network:
java NetworkClient -gateway localhost:9001 terminate

Example scripts can be found in /scripts/ package

All required feature were implemented:
- You can construct logical network.
- NetworkClient can connect to the Network and ask to allocate resources and get appropriate success or failure response.
- NetworkClient can connect again and ask to allocate resources one more time on updated resources state of the Network.
- NetworkClient can request to terminate Network.