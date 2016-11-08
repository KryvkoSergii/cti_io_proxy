package ua.com.smiddle.cti.io.proxy.core.model;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author srg on 08.11.16.
 * @project cti_io_proxy
 */
public class Attachment {
    private Queue<ByteBuffer> messages;
    private SocketChannel channel;
    private ConnectionType connectionType;


    //Constructors
    public Attachment() {
    }

    public Attachment(SocketChannel channel) {
        this.messages = new ConcurrentLinkedQueue<>();
        this.channel = channel;
    }

    public Attachment(SocketChannel channel, ConnectionType connectionType) {
        this.messages = new ConcurrentLinkedQueue<>();
        this.channel = channel;
        this.connectionType = connectionType;
    }

    public Attachment(Queue<ByteBuffer> messages, SocketChannel channel) {
        this.messages = messages;
        this.channel = channel;
    }


    //Getters and setters
    public Queue<ByteBuffer> getMessages() {
        return messages;
    }

    public void setMessages(Queue<ByteBuffer> messages) {
        this.messages = messages;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public void setChannel(SocketChannel channel) {
        this.channel = channel;
    }

    public ConnectionType getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(ConnectionType connectionType) {
        this.connectionType = connectionType;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Attachment{");
        sb.append("messages=").append(messages);
        sb.append(", channel=").append(channel);
        sb.append('}');
        return sb.toString();
    }
}
