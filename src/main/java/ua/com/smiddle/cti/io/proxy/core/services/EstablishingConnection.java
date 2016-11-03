package ua.com.smiddle.cti.io.proxy.core.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import ua.com.smiddle.cti.io.proxy.core.TransportStack;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author srg on 03.11.16.
 * @project cti_io_proxy
 */
@Component("EstablishingConnection")
@Scope("singleton")
public class EstablishingConnection implements Runnable {
    @Autowired
    Environment env;
    @Autowired
    private ApplicationContext context;
    private Thread thread;

    @PostConstruct
    private void init() {
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(Integer.valueOf(env.getProperty("connection.listener.port")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Socket withClient;
        Socket withServer;
        TransportStack client;
        TransportStack server;
        while (true) {
            try {
                withClient = serverSocket.accept();
                withServer = new Socket(env.getProperty("connection.server.ip"), Integer.valueOf(env.getProperty("connection.server.port")));
                client = context.getBean(TransportStack.class, withClient);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }
}
