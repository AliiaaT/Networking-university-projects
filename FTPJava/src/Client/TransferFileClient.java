package Client;

import java.io.*;
import java.net.Socket;

class TransferFileClient {
    Socket ClientSocket;

    DataInputStream dataInputStream;
    DataOutputStream dataOutputStream;
    BufferedReader bufferedReader;

    TransferFileClient(Socket soc) {
        try {
            ClientSocket = soc;
            dataInputStream = new DataInputStream(ClientSocket.getInputStream());
            dataOutputStream = new DataOutputStream(ClientSocket.getOutputStream());
            bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        } catch (Exception ex) {
        }
    }

    void sendFile() throws Exception {

        String fileName;
        System.out.print("Enter File Name :");
        fileName = bufferedReader.readLine();
        File f = new File("src/Client/" + fileName);
        System.out.println("TRY TO OPEN: " + f.getAbsolutePath());
        if (!f.exists()) {
            System.out.println("File not Exists...");
            dataOutputStream.writeUTF("File not found");
            return;
        }

        dataOutputStream.writeUTF(fileName);

        String msgFromServer = dataInputStream.readUTF();
        if (msgFromServer.equals("File Already Exists")) {
            String option;
            System.out.println("File Already Exists. Want to OverWrite (Y/N) ?");
            option = bufferedReader.readLine();
            if (option.equals("Y")) {
                dataOutputStream.writeUTF("Y");
            } else {
                dataOutputStream.writeUTF("N");
                return;
            }
        }

        System.out.println("Sending File ...");
        FileInputStream fileInputStream = new FileInputStream(f);
        int ch;
        do {
            ch = fileInputStream.read();
            dataOutputStream.writeUTF(String.valueOf(ch));
        } while (ch != -1);
        fileInputStream.close();
        System.out.println(dataInputStream.readUTF());
    }

    void receiveFile() throws Exception {
        String fileName;
        System.out.print("Enter File Name :");
        fileName = bufferedReader.readLine();
        dataOutputStream.writeUTF(fileName);
        String msgFromServer = dataInputStream.readUTF();

        if (msgFromServer.equals("File Not Found")) {
            System.out.println("File not found on Server ...");
            return;
        } else if (msgFromServer.equals("READY")) {
            System.out.println("Receiving File ...");
            File f = new File("src/Client/" + fileName);
            if (f.exists()) {
                String Option;
                System.out.println("File Already Exists. Want to OverWrite (Y/N) ?");
                Option = bufferedReader.readLine();
                if (Option.equals("N")) {
                    dataOutputStream.flush();
                    return;
                }
            }
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
            System.out.println(dataInputStream.readUTF());
        }
    }

    public void displayMenu() throws Exception {
        while (true) {
            System.out.println("< MENU >");
            System.out.println("1. Send File");
            System.out.println("2. Receive File");
            System.out.println("3. Exit");
            System.out.print("\nEnter Choice :");
            int choice;
            choice = Integer.parseInt(bufferedReader.readLine());
            if (choice == 1) {
                dataOutputStream.writeUTF("SEND");
                sendFile();
            } else if (choice == 2) {
                dataOutputStream.writeUTF("GET");
                receiveFile();
            } else {
                dataOutputStream.writeUTF("DISCONNECT");
                System.exit(1);
            }
        }
    }
}