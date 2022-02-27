package Server;

import java.net.*;

public class Server {
    public static void main(String args[]) throws Exception {
        ServerSocket soc = new ServerSocket(6000);
        System.out.println("FTP Server Started on Port Number 6000");
        while (true) {
            System.out.println("Waiting for Connection ...");
            TransferFileServer t = new TransferFileServer(soc.accept());
        }
    }
}