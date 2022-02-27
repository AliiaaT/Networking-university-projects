import java.io.*;
import java.net.Socket;

public class SocketStatisticClient {
    public static void main(String args[]) throws Exception {
        Socket soc = new Socket("127.0.0.1", 5217);
        Client t = new Client(soc);
        t.getMemoryUsage();
    }

    static class Client {
        Socket ClientSoc;

        DataInputStream din;
        DataOutputStream dout;

        Client(Socket soc) {
            try {
                ClientSoc = soc;
                din = new DataInputStream(ClientSoc.getInputStream());
                dout = new DataOutputStream(ClientSoc.getOutputStream());
            } catch (Exception ex) {
            }
        }

        public void getMemoryUsage() throws Exception {
            String msgFromServer = din.readUTF();
            System.out.println(msgFromServer);
        }
    }

}


