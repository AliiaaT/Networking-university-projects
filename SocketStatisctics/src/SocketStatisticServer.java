import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class SocketStatisticServer {
    static AtomicReference<String> memoryUsage = new AtomicReference<>(getServerResponse());

    public static String getStringGB(long bytes) {
        return String.format("%.2f",bytes/1024.0/1024/1024) + "GB";
    }

    static class FreeMemoryTimerTask extends TimerTask {
        Consumer<String> callback;
        FreeMemoryTimerTask(Consumer<String> callback) {
            this.callback = callback;
        }

        public void run() {
            callback.accept(getServerResponse());
        }
    }

    public static String getServerResponse() {
        long totalMemory = getTotalRam();
        String usedMemory = getStringGB(getTotalRam() - getFreeRAM());
        return getStringGB(totalMemory) + "/" + usedMemory + " is used";
    }

    public static long getFreeRAM()
    {
//        String osName = System.getProperty("os.name");
//        if (osName.equals("Linux"))
//        {
//            try {
//                BufferedReader memInfo = new BufferedReader(new FileReader("/proc/meminfo"));
//                String line;
//                while ((line = memInfo.readLine()) != null)
//                {
//                    if (line.startsWith("MemAvailable: "))
//                    {
//                        // Output is in KB which is close enough.
//                        return java.lang.Long.parseLong(line.split("[^0-9]+")[1]) * 1024;
//                    }
//                }
//            } catch (IOException e)
//            {
//                e.printStackTrace();
//            }
//            // We can also add checks for freebsd and sunos which have different ways of getting available memory
//        } else
//        {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean)osBean;
        return sunOsBean.getFreeMemorySize();
//        }
//        return -1;
    }

    public static long getTotalRam() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean)osBean;
        return sunOsBean.getTotalMemorySize();
    }

    public static void main(String[] args) throws IOException {
        TimerTask task = new FreeMemoryTimerTask((callbackValue) -> {
            memoryUsage.set(callbackValue);
//            System.out.println(memoryUsage.get());
        });
        Timer timer = new Timer();
        timer.schedule(task, 0, 500);

        ServerSocket soc = new ServerSocket(5217);
        System.out.println("Server Started on Port Number 5217");
        while (true) {
            System.out.println("Waiting for Connection ...");
            new Server(soc.accept());
        }
    }

    static class Server extends Thread {
        Socket ClientSoc;

        DataInputStream din;
        DataOutputStream dout;

        Server(Socket soc) {
            try {
                ClientSoc = soc;
                din = new DataInputStream(ClientSoc.getInputStream());
                dout = new DataOutputStream(ClientSoc.getOutputStream());
                start();

            } catch (Exception ex) {
            }
        }

        public void run() {
            try {
                dout.writeUTF(memoryUsage.get());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

