package Server;

import java.io.*;
import java.net.Socket;

class TransferFileServer extends Thread {
    Socket ClientSocket;
    DataInputStream dataInputStream;
    DataOutputStream dataOutputStream;

    TransferFileServer(Socket soc) {
        try {
            ClientSocket = soc;
            dataInputStream = new DataInputStream(ClientSocket.getInputStream());
            dataOutputStream = new DataOutputStream(ClientSocket.getOutputStream());
            System.out.println("FTP Client Connected ...");
            start();
        } catch (Exception ex) {
        }
    }

    void sendFile() throws Exception {
        String filename = dataInputStream.readUTF();
        File f = new File("src/Server/" + filename);
        System.out.println("TRY TO OPEN: " + f.getAbsolutePath());

        if (!f.exists()) {
            dataOutputStream.writeUTF("File Not Found");
            return;
        } else {
            dataOutputStream.writeUTF("READY");
            FileInputStream fileInputStream = new FileInputStream(f);
            int ch;
            do {
                ch = fileInputStream.read();
                dataOutputStream.writeUTF(String.valueOf(ch));
            }
            while (ch != -1);
            fileInputStream.close();
            dataOutputStream.writeUTF("File Receive Successfully");
        }
    }

    void receiveFile() throws Exception {
        String filename = dataInputStream.readUTF();
        if (filename.equals("File not found")) {
            return;
        }
        File f = new File("src/Server/" + filename);
        System.out.println("TRY TO OPEN: " + f.getAbsolutePath());
        String option;
        // f = "FileClient"
        if (f.exists()) { // should check relative path
            dataOutputStream.writeUTF("File Already Exists");
            option = dataInputStream.readUTF();
        } else {
            dataOutputStream.writeUTF("SendFile");
            option = "Y";
        }

        if (option.equals("Y")) {
            FileOutputStream fileOutputStream = new FileOutputStream(f);
            int ch;
            String temp;
            do {
                temp = dataInputStream.readUTF();
                ch = Integer.parseInt(temp);
                if (ch != -1) {
                    fileOutputStream.write(ch);
                }
            } while (ch != -1);
            fileOutputStream.close();
            dataOutputStream.writeUTF("File Send Successfully");
        } else {
            return;
        }

    }


    public void run() {
        while (true) {
            try {
                System.out.println("Waiting for Command ...");
                String Command = dataInputStream.readUTF();
                if (Command.equals("GET")) {
                    System.out.println("\tGET Command Received ...");
                    sendFile();
                    continue;
                } else if (Command.equals("SEND")) {
                    System.out.println("\tSEND Command Receiced ...");
                    receiveFile();
                    continue;
                } else if (Command.equals("DISCONNECT")) {
                    System.out.println("\tDisconnect Command Received ...");
                    System.exit(1);
                }
            } catch (Exception ex) {
            }
        }
    }
}