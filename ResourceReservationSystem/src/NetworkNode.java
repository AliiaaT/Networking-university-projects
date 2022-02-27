import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class NetworkNode {

    static List<NodeSocket> connectedSockets = new ArrayList<>();
    static GatewaySocket gatewaySocket;
    static String gateway = null;
    static int port = 0;
    static int tcpport = 0;
    static String nodeIdentifier = null;
    static HashMap<String, Integer> nodeResourcesMap = new HashMap<>();
    // identifier, {A:1, B:1}
    static HashMap<String, HashMap<String, Integer>> subnetNodesResourcesMap = new HashMap<>();

    public static void main(String[] args) throws IOException {

        // Parameter scan loop
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-ident":
                    // skip
                    ++i;
                    break;
                case "-tcpport":
                    tcpport = Integer.parseInt(args[++i]);
                    break;
                case "-gateway":
                    String[] gatewayArray = args[++i].split(":");
                    gateway = gatewayArray[0];
                    port = Integer.parseInt(gatewayArray[1]);
                    break;
                default:
                    String[] singleResource = args[i].split(":");
                    nodeResourcesMap.put(singleResource[0], Integer.parseInt(singleResource[1]));
            }
        }
        nodeIdentifier = "localhost:" + tcpport;
        // init subnetNodesResourcesMap with the current node resources
        subnetNodesResourcesMap.put(nodeIdentifier, nodeResourcesMap);

        if (gateway != null) {
            gatewaySocket = new GatewaySocket(gateway, port, nodeIdentifier);
        }

//        InetAddress addr = InetAddress.getByName(gateway);
        ServerSocket serverSocket = new ServerSocket(tcpport);
        while (true) {
            System.out.println("Waiting for Connection ...");
            NodeSocket nodeSocket = new NodeSocket(serverSocket.accept());
            connectedSockets.add(nodeSocket);
        }
    }


    static void askTerminateNetwork() {
        System.out.println("SEND: NODE ASK_TERMINATE");
        // already parent node, start broadcast
        if (isRootNode()) {
            for (NodeSocket connectedSocket: connectedSockets) {
                if (!connectedSocket.isNetworkClientSocket) {
                    connectedSocket.out.println("NODE TERMINATE_NETWORK");
                }
                connectedSocket.disconnect();
            }
            System.exit(0);
        } else {
            gatewaySocket.out.println("NODE ASK_TERMINATE");
        }
    }

    static void askNetworkResources(NodeSocket client, HashMap<String, Integer> resources) {
        System.out.println("Asked resources:");
        System.out.println(resources);
        String allocationParticipants = getAllocationParticipants(resources);
        if (allocationParticipants == null && isRootNode()) {
            System.out.println("askNetworkResources failed");
            if (client.isNetworkClientSocket) {
                // special format for NetworkClient
                client.out.println("FAILED");
                client.disconnect();
            } else {
                client.out.println("NODE ALLOCATION_FAILED");
            }
        } else if (allocationParticipants != null) {
            System.out.println(allocationParticipants);
//            client.responseAllocated(allocationParticipants);

            // check if allocation of itself is needed
            String[] args = allocationParticipants.split(" ");
            for (int i = 0; i < args.length; i++) {
                // resource_name:quantity:ip:port
                String[] singleEntry = args[i].split(":");
                if (nodeIdentifier.equals(singleEntry[2] + ":" + singleEntry[3])) {
                    // update current node resources
                    nodeResourcesMap.put(
                            singleEntry[0],
                            nodeResourcesMap.get(singleEntry[0]) - Integer.parseInt(singleEntry[1])
                    );
                }
            }
            System.out.println("Start allocate!");

            // send to all child nodes allocate resources command
            for (NodeSocket node : connectedSockets) {
                System.out.println("Send to: " + node.identifier);
                // is client destination
                if (node.isNetworkClientSocket) {
                    // special format for NetworkClient
                    node.out.println("ALLOCATE");
                    for (int i = 0; i < args.length; i++) {
                        node.out.println(args[i]);
                    }
                    node.disconnect();
                } else {
                    node.out.println("NODE ALLOCATE_RESOURCES " + allocationParticipants);
                }
            }
        } else {
            System.out.println("Send ask to parent gateway port: " + port);
            gatewaySocket.sendAskNetworkResources(client.identifier, resources);
        }
    }

    private static boolean isRootNode() {
        return gatewaySocket == null;
    }

    /**
     * @param requiredResources resource to be allocated in the subnetwork
     * @return string which contains allocate participants or null if there are not enough resources in the subnetwork
     */
    private static String getAllocationParticipants(HashMap<String, Integer> requiredResources) {
        // <resource>:<quantity>:<node IP>:<node port>
        StringBuilder participants = new StringBuilder();

        for (Map.Entry<String, Integer> entry : requiredResources.entrySet()) {
            String searchKey = entry.getKey();
            int searchQuantity = entry.getValue();
            int searchQuantityLeft = searchQuantity;
            for (Map.Entry<String, HashMap<String, Integer>> subNetworkNodeResourcesEntry : subnetNodesResourcesMap.entrySet()) {
                String nodeIdent = subNetworkNodeResourcesEntry.getKey();
                HashMap<String, Integer> nodeResources = subNetworkNodeResourcesEntry.getValue();
                Integer nodeResource = nodeResources.get(searchKey);
                // if node has this resource than reserve it
                if (nodeResource != null && nodeResource > 0) {
                    // it's not enough to satisfy all left amount
                    if (searchQuantityLeft > nodeResource) {
                        searchQuantityLeft -= nodeResource;
                        // nodeResource = 0;
                        participants
                                .append(searchKey)
                                .append(":")
                                .append(nodeResource)
                                .append(":")
                                .append(nodeIdent)
                                .append(" ");
                    } else {
                        searchQuantityLeft = 0;
                        // nodeResource - searchQuantityLeft
                        participants
                                .append(searchKey)
                                .append(":")
                                .append(nodeResource)
                                .append(":")
                                .append(nodeIdent)
                                .append(" ");
                    }
                }
            }
            // wasn't manage to find appropriate amount of resources
            if (searchQuantityLeft > 0) {
                return null;
            }
        }
        return participants.toString();

//            if (cumulativeChildResourcesMap.containsKey(entry.getKey())) {
//                cumulativeChildResourcesMap.put(
//                        entry.getKey(),
//                        entry.getValue() + cumulativeChildResourcesMap.get(entry.getKey())
//                );
//            }
//        }
    }

    static void addNewNodeToNetwork(String newNodeIdentifier, HashMap<String, Integer> resources) {
        System.out.println("Add new node to: " + nodeIdentifier);
        System.out.println("Subnet with identifier " + nodeIdentifier + " resource are:");
        subnetNodesResourcesMap.put(newNodeIdentifier, resources);
        System.out.println(subnetNodesResourcesMap);
        if (!isRootNode()) {
            gatewaySocket.sendNewNodeConnectedToParentNode(newNodeIdentifier, resources);
        }
    }

    public static String resourcesToString(HashMap<String, Integer> resources) {

        StringBuilder resourcesAsString = new StringBuilder();
        for (Map.Entry<String, Integer> entry : resources.entrySet()) {
            resourcesAsString.append(entry.getKey()).append(":").append(entry.getValue()).append(" ");
        }
        return resourcesAsString.toString();
    }

    public static class NodeSocket extends Thread {
        Socket nodeClient;
        String identifier;
        boolean isNetworkClientSocket = false;

        PrintWriter out;
        BufferedReader in;

        public NodeSocket(Socket nodeClient) {
            this.nodeClient = nodeClient;
            try {
                in = new BufferedReader(new InputStreamReader(nodeClient.getInputStream()));
                out = new PrintWriter(nodeClient.getOutputStream(), true);
                System.out.println("Client Connected ...");
                start();
            } catch (Exception error) {
                System.out.println("Error create NodeSocket.");
                interrupt();
            }
        }

        public void run() {
            try {
                while (true) {
                    String command = in.readLine();
                    System.out.println("NodeSocket GOT: " + command);
                    String[] args = command.split(" ");
                    if (args[0].equals("NODE")) {
                        isNetworkClientSocket = false;
                        readAnotherNetworkNodeCommand(args);
                    } else {
                        isNetworkClientSocket = true;
                        readClientCommand(args);
                    }
                }
            } catch (IOException error) {
                System.out.println("Client disconnected.");
                interrupt();
            }
        }

        public void disconnect() {
            try {
                this.nodeClient.close();
                interrupt();
            } catch (IOException e) {
                System.out.println("Already disconnected");
            }
        }

        public void readAnotherNetworkNodeCommand(String[] args) {
            if ("CONNECT_TO_NETWORK".equals(args[1])) {
                String newNodeIdentifier = args[2];
                identifier = newNodeIdentifier;
                HashMap<String, Integer> resources = new HashMap<>();
                for (int i = 3; i < args.length; i++) {
                    String[] singleResource = args[i].split(":");
                    resources.put(singleResource[0], Integer.parseInt(singleResource[1]));
                }
                addNewNodeToNetwork(newNodeIdentifier, resources);
            } else if ("ASK_RESOURCES".equals(args[1])) {
                String whoAsk = args[2];
                HashMap<String, Integer> resources = new HashMap<>();
                for (int i = 3; i < args.length; i++) {
                    String[] singleResource = args[i].split(":");
                    resources.put(singleResource[0], Integer.parseInt(singleResource[1]));
                }
                askNetworkResources(this, resources);
            } else if ("ASK_TERMINATE".equals(args[1])) {
                if (isRootNode()) {
                    // start broadcast terminate from the parent to all nodes
                    for (NodeSocket connectedSocket: connectedSockets) {
                        if (!connectedSocket.isNetworkClientSocket) {
                            connectedSocket.out.println("NODE TERMINATE_NETWORK");
                        }
                        connectedSocket.disconnect();
                    }
                    System.exit(0);
                } else {
                    // send further to parent
                    gatewaySocket.out.println("NODE ASK_TERMINATE");
                }
            }
        }

        public void readClientCommand(String[] args) {
            if ("terminate".equalsIgnoreCase(args[0])) {
                System.out.println("readClientCommand TERMINATE");
                askTerminateNetwork();
                return;
            }

            String clientIdent = args[0];
            identifier = clientIdent;

            String[] resourcesRow = Arrays.copyOfRange(args, 1, args.length);

            HashMap<String, Integer> askedResources = new HashMap<>();
            for (String resource : resourcesRow) {
                // divide A:1 into {A: 1} and append to hashmap
                String[] singleResource = resource.split(":");
                askedResources.put(singleResource[0], Integer.parseInt(singleResource[1]));
            }

            askNetworkResources(this, askedResources);
        }

    }

    public static class GatewaySocket extends Thread {
        private Socket gatewaySocket;
        String identifier;
        PrintWriter out;
        BufferedReader in;

        public GatewaySocket(String gateway, int port, String identifier) {
            try {
                gatewaySocket = new Socket(gateway, port);
                this.identifier = identifier;
                in = new BufferedReader(new InputStreamReader(gatewaySocket.getInputStream()));
                out = new PrintWriter(gatewaySocket.getOutputStream(), true);
                start();
            } catch (IOException e) {
                System.out.println("Error create NodeSocket.");
                interrupt();
            }
        }

        public void sendAskNetworkResources(String whoAsk, HashMap<String, Integer> resources) {
            out.println("NODE ASK_RESOURCES " + whoAsk + " " + resourcesToString(resources));
        }

        public void sendNewNodeConnectedToParentNode(String newIdentifier, HashMap<String, Integer> resources) {
            out.println("NODE CONNECT_TO_NETWORK " + newIdentifier + " " + resourcesToString(resources));
        }


        public void run() {
//            try {
            StringBuilder resourcesAsString = new StringBuilder();
            for (Map.Entry<String, Integer> entry : nodeResourcesMap.entrySet()) {
                resourcesAsString.append(entry.getKey()).append(":").append(entry.getValue()).append(" ");
            }
            System.out.println("Connected to: " + gatewaySocket.getPort());
            out.println("NODE CONNECT_TO_NETWORK " + nodeIdentifier + " " + resourcesAsString);

            ThreadReader threadReader = new ThreadReader(in);
            threadReader.start();

        }


        public void disconnect() {
            try {
                this.gatewaySocket.close();
                interrupt();
            } catch (IOException e) {
                System.out.println("Already disconnected Gateway socket");
            }
        }


        public void readAnotherNetworkNodeCommand(String[] args) {
            if ("ALLOCATE_RESOURCES".equals(args[1])) {
                boolean resourcesWasModified = false;
                for (int i = 2; i < args.length; i++) {
                    // resource_name:quantity:ip:port
                    String[] singleEntry = args[i].split(":");
                    if (identifier.equals(singleEntry[2] + ":" + singleEntry[3])) {
                        // update current node resources
                        nodeResourcesMap.put(
                                singleEntry[0],
                                nodeResourcesMap.get(singleEntry[0]) - Integer.parseInt(singleEntry[1])
                        );
                        resourcesWasModified = true;
                    }
                }
                if (resourcesWasModified) {
                    System.out.println("resources are updated: " + nodeResourcesMap);
                    sendNewNodeConnectedToParentNode(identifier, nodeResourcesMap);
                }
                // send signal further
                for (NodeSocket connectedSocket : connectedSockets) {
                    System.out.println("GatewaySocket send to: " + connectedSocket.identifier);
                    // special format for NetworkClient socket
                    if (connectedSocket.isNetworkClientSocket) {
                        connectedSocket.out.println("ALLOCATE");
                        for (int i = 2; i < args.length; i++) {
                            connectedSocket.out.println(args[i]);
                        }
                        connectedSocket.disconnect();
                    } else {
                        connectedSocket.out.println(String.join(" ", args));
                    }
                }
            } else if ("ALLOCATION_FAILED".equals(args[1])) {
                // NODE ALLOCATION_FAILED
                for (NodeSocket connectedSocket : connectedSockets) {
                    System.out.println("GatewaySocket send to: " + connectedSocket.identifier);
                    // special format for NetworkClient socket
                    if (connectedSocket.isNetworkClientSocket) {
                        connectedSocket.out.println("FAILED");
                        connectedSocket.disconnect();
                    } else {
                        connectedSocket.out.println(String.join(" ", args));
                    }
                }
            }
            else if ("TERMINATE_NETWORK".equals(args[1])) {
                // node asked to terminate network
                for (NodeSocket connectedSocket: connectedSockets) {
                    if (!connectedSocket.isNetworkClientSocket) {
                        connectedSocket.out.println("NODE TERMINATE_NETWORK");
                    }
                    connectedSocket.disconnect();
                }
                System.exit(0);
            }
        }

        private class ThreadReader extends Thread {
            BufferedReader in;

            public ThreadReader(BufferedReader in) {
                this.in = in;
            }

            @Override
            public void run() {
                try {
                    while (true) {
                        String command = in.readLine();
                        if (command == null) {
                            this.interrupt();
                            return;
                        }
                        System.out.println("GatewaySocket GOT: " + command);
                        String[] args = command.split(" ");
                        if (args[0].equals("NODE")) {
                            readAnotherNetworkNodeCommand(args);
                        } else {
                            // ignore
                        }
                    }
                } catch (IOException error) {
                    System.out.println("Gateway socket disconnected.");
                    interrupt();
                }
            }
        }
    }

}
