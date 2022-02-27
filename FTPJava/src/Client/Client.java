package Client;
import java.net.*;

class Client {
    public static void main(String args[]) throws Exception {
        Socket soc = new Socket("127.0.0.1", 6000);
        TransferFileClient t = new TransferFileClient(soc);
        t.displayMenu();
    }
}