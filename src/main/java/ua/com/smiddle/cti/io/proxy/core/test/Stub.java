package ua.com.smiddle.cti.io.proxy.core.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author srg on 07.11.16.
 * @project cti_io_proxy
 */
public class Stub {

    public static void main(String[] args) {
        try {
            System.out.println("Init....");
            ServerSocket ss = new ServerSocket(42026);
            Socket s = ss.accept();
            System.out.println("Connected: " + s.getRemoteSocketAddress());
            InputStream is = s.getInputStream();
            OutputStream os = s.getOutputStream();
            int data;
            while ((data = is.read()) != -1) {
                os.write(convert(data));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static int convert(int i) {
        if (Character.isLetter(i)) {
            return i ^ ' ';
        } else return i;
    }


}
