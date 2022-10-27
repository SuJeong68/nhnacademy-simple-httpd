import thread.ServerThread;

import java.io.*;
import java.net.ServerSocket;

public class Server {

    public static boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Usage) shttpd 8080\n$ ");

        int portNumber;
        while (true) {
            String inputStr = br.readLine();
            if (inputStr.contains("shttpd") && isNumeric(inputStr.split(" ")[1])) {
                portNumber = Integer.parseInt(inputStr.split(" ")[1]);
                break;
            }
        }

        ServerSocket serverSocket = new ServerSocket(portNumber);

        Thread thread = new Thread(new ServerThread(serverSocket));
        thread.start();

//        serverSocket.close();
        br.close();
    }
}
