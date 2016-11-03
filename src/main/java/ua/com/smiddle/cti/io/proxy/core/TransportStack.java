package ua.com.smiddle.cti.io.proxy.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import ua.com.smiddle.cti.io.proxy.core.util.LoggerUtil;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author srg on 31.10.16.
 * @project cti_io_proxy
 */
public class TransportStack implements Runnable {
    @Autowired
    @Qualifier("LoggerUtil")
    private LoggerUtil logger;
    private Queue<byte[]> inputMessages;
    private Queue<byte[]> outputMessages;
    private boolean isInterrupted;
    private Socket socket;

    //Constructors
    public TransportStack() {
    }

    //Getters and setters
    public Queue<byte[]> getInputMessages() {
        return inputMessages;
    }

    public Queue<byte[]> getOutputMessages() {
        return outputMessages;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public boolean isInterrupted() {
        return isInterrupted;
    }

    public void setInterrupted(boolean interrupted) {
        isInterrupted = interrupted;
    }


    //Methods
    @PostConstruct
    private void init() {
        inputMessages = new ConcurrentLinkedQueue<>();
        outputMessages = new ConcurrentLinkedQueue<>();
        logger.logAnyway("Stack", "Initialized");
    }

    @Override
    public void run() {
        byte[] inputMessage;
        byte[] outputMessage;
        SocketChannel channel = socket.getChannel();
        try {
            channel.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (!isInterrupted) {
            try {
                inputMessage = readMessage(channel);
                if (inputMessage != null)
                    inputMessages.offer(inputMessage);
                outputMessage = outputMessages.poll();
                if (outputMessage != null)
                    writeMessage(channel, outputMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        destroy();
    }

    private byte[] readMessage(SocketChannel channel) throws IOException {
        if (channel.validOps() != SelectionKey.OP_READ) return null;
        ByteBuffer length = ByteBuffer.allocate(4);
        ByteBuffer message;
        channel.read(length);
        message = ByteBuffer.allocate(length.capacity() + 4 + length.getInt());
        message.put(length.array(), 0, length.capacity());
        channel.read(message);
        return message.array();
    }

    private void writeMessage(SocketChannel channel, byte[] array) throws IOException {
        channel.write(ByteBuffer.wrap(array));
    }

    @PreDestroy
    public void destroy() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
