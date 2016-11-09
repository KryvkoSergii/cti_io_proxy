package ua.com.smiddle.cti.io.proxy.core.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ua.com.smiddle.cti.io.proxy.core.model.Attachment;
import ua.com.smiddle.cti.io.proxy.core.model.ConnectionType;
import ua.com.smiddle.cti.io.proxy.core.util.LoggerUtil;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author srg on 07.11.16.
 * @project cti_io_proxy
 */
@Component("ConnectorNIO")
@Scope("singleton")
public class ConnectorNIO {
    private final static String CLASS_NAME = "ConnectorNIO";
    private final static String EXCEPTION_UNKNOWN_CONNECTION_TYPE = "Unknown connection type";
    private final static String FIELD_ACCEPTED_NEW_CLIENT_CONNECTION = "Accepted new client connection: ";
    private final static String FIELD_CONNECTED_NEW_SERVER = "New server connected: ";

    @Autowired
    Environment environment;
    @Autowired
    @Qualifier("LoggerUtil")
    private LoggerUtil logger;
    private volatile boolean interruped = false;
    private Selector selector;
    private Map<ConnectionType, Attachment> connections = new ConcurrentHashMap<>();

    public ConnectorNIO() {
    }

    @PostConstruct
    private void init() {
        logger.logMore_2(CLASS_NAME, "Initialized");
        execute();
    }

    @Async(value = "threadPoolTransfer")
    private void execute() {
        try {
            process();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void process() throws IOException {
        selector = Selector.open();
        //серверный сокет слушающий подключения
        ServerSocketChannel asServerChannel = ServerSocketChannel.open();
        asServerChannel.bind(new InetSocketAddress("localhost", Integer.valueOf(environment.getProperty("connection.listener.port"))));
        asServerChannel.configureBlocking(false);
        selector = Selector.open();
        asServerChannel.register(selector, SelectionKey.OP_ACCEPT);

        //обработка селектов
        while (!interruped) {
            selector.select();
            for (Iterator<SelectionKey> itKeys = selector.selectedKeys().iterator(); itKeys.hasNext(); ) {
                SelectionKey key = itKeys.next();
                itKeys.remove();
                if (key.isValid()) {
                    if (key.isAcceptable()) {
                        accept(key);
                    } else if (key.isReadable()) {
                        read(key);
                    } else if (key.isWritable()) {
                        write(key);
                    }
                }
            }
        }
    }

    private void connect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        channel.finishConnect();
        connections.put(ConnectionType.SERVER_CONNECTION_TYPE, new Attachment(channel, ConnectionType.SERVER_CONNECTION_TYPE));
        logger.logMore_2(CLASS_NAME, FIELD_CONNECTED_NEW_SERVER);
    }

    private void write(SelectionKey key) throws IOException {
        ByteBuffer buf;
        Attachment attachment = (Attachment) key.attachment();
        while ((buf = attachment.getMessages().poll()) != null)
            attachment.getChannel().write(buf);
//        if (!buf.hasRemaining()) attachment.getMessages().poll();
        attachment.getChannel().register(selector, SelectionKey.OP_READ, attachment);
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buf = ByteBuffer.allocateDirect(4096);
        int read = channel.read(buf);
        if (read == -1) {
            switch (((Attachment) key.attachment()).getConnectionType()) {
                case CLIENT_CONNECTION_TYPE:
                    logger.logMore_2(CLASS_NAME, "Client " + ((Attachment) key.attachment()).getChannel().getRemoteAddress() + " terminated connection");
                    break;
                case SERVER_CONNECTION_TYPE:
                    logger.logMore_2(CLASS_NAME, "Server " + ((Attachment) key.attachment()).getChannel().getRemoteAddress() + " terminated connection");
                    break;
                default:
                    logger.logMore_2(CLASS_NAME, EXCEPTION_UNKNOWN_CONNECTION_TYPE);
                    break;
            }
            closeConnections();
            return;
        }
        buf.flip();
        switch (((Attachment) key.attachment()).getConnectionType()) {
            case CLIENT_CONNECTION_TYPE:
                connections.get(ConnectionType.SERVER_CONNECTION_TYPE).getMessages().add(buf);
                connections.get(ConnectionType.SERVER_CONNECTION_TYPE)
                        .getChannel().register(selector, SelectionKey.OP_WRITE, connections.get(ConnectionType.SERVER_CONNECTION_TYPE));
                break;
            case SERVER_CONNECTION_TYPE:
                connections.get(ConnectionType.CLIENT_CONNECTION_TYPE).getMessages().add(buf);
                connections.get(ConnectionType.CLIENT_CONNECTION_TYPE)
                        .getChannel().register(selector, SelectionKey.OP_WRITE, connections.get(ConnectionType.CLIENT_CONNECTION_TYPE));
                break;
            default:
                logger.logMore_2(CLASS_NAME, EXCEPTION_UNKNOWN_CONNECTION_TYPE);
                break;
        }
    }

    /**
     * принятие клиентского соединения
     *
     * @param key
     * @throws IOException
     */
    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel channel = ssc.accept();
        channel.configureBlocking(false);
        Attachment attachment = new Attachment(channel, ConnectionType.CLIENT_CONNECTION_TYPE);
        channel.register(key.selector(), SelectionKey.OP_READ, attachment);
        connections.put(ConnectionType.CLIENT_CONNECTION_TYPE, attachment);
        logger.logMore_2(CLASS_NAME, FIELD_ACCEPTED_NEW_CLIENT_CONNECTION + channel.getRemoteAddress());
        //подключение к серверу
        try {
            checkConnectionToServer();
        } catch (Exception e) {
            logger.logMore_2(CLASS_NAME, "accept: connecting to server throw Exception=" + e.getMessage());
            closeConnections();
        }
    }

    private void checkConnectionToServer() throws IOException {
        SocketChannel channel = SocketChannel.open(
                new InetSocketAddress(environment.getProperty("connection.server.ip"),
                        Integer.valueOf(environment.getProperty("connection.server.port"))));
        channel.configureBlocking(false);
        Attachment attachment = new Attachment(channel, ConnectionType.SERVER_CONNECTION_TYPE);
        channel.register(selector, SelectionKey.OP_CONNECT, attachment);
        channel.finishConnect();
        connections.put(ConnectionType.SERVER_CONNECTION_TYPE, attachment);
        logger.logMore_2(CLASS_NAME, FIELD_CONNECTED_NEW_SERVER + channel.getRemoteAddress());
    }

    private void closeConnections() throws IOException {
        for (Iterator attachmentIter = connections.entrySet().iterator(); attachmentIter.hasNext(); ) {
            Map.Entry<ConnectionType, Attachment> attachment = (Map.Entry<ConnectionType, Attachment>) attachmentIter.next();
            if (attachment.getValue().getChannel() != null) {
                logger.logMore_2(CLASS_NAME, "Closing " + attachment.getKey() + " for " + attachment.getValue().getChannel().getRemoteAddress());
                attachment.getValue().getMessages().clear();
                attachment.getValue().getChannel().close();
            }
        }
        connections.clear();
    }

    @PreDestroy
    private void destroy() {
        try {
            closeConnections();
            logger.logAnyway(CLASS_NAME, "destroying...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
