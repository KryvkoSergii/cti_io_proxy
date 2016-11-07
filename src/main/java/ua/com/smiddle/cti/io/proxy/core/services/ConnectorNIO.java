package ua.com.smiddle.cti.io.proxy.core.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ua.com.smiddle.cti.io.proxy.core.model.ConnectionType;
import ua.com.smiddle.cti.io.proxy.core.util.LoggerUtil;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    private Selector selector;
    private Map<ConnectionType, Queue<ByteBuffer>> messages = new ConcurrentHashMap<>();
    private Map<ConnectionType, SocketChannel> channels = new HashMap<>();

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
        while (true) {
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
                    } else if (key.isConnectable()) {
                        connect(key);
                    }
                }
            }
        }
    }

    private void connect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        channel.finishConnect();
        messages.put(ConnectionType.SERVER_CONNECTION_TYPE, new ConcurrentLinkedQueue<ByteBuffer>());
        channels.put(ConnectionType.CLIENT_CONNECTION_TYPE, channel);
        logger.logMore_2(CLASS_NAME, FIELD_CONNECTED_NEW_SERVER);
    }

    private void write(SelectionKey key) throws IOException {
        ByteBuffer buf;
        switch ((ConnectionType) key.attachment()) {
            case CLIENT_CONNECTION_TYPE:
                while ((buf = messages.get(key.attachment()).peek()) != null)
                    channels.get(ConnectionType.SERVER_CONNECTION_TYPE).write(buf);
                if (!buf.hasRemaining())
                    messages.get(key.attachment()).poll();
                else {
                    return;
                }
                break;
            case SERVER_CONNECTION_TYPE:
                while ((buf = messages.get(key.attachment()).peek()) != null)
                    channels.get(ConnectionType.CLIENT_CONNECTION_TYPE).write(buf);
                if (!buf.hasRemaining())
                    messages.get(key.attachment()).poll();
                else {
                    return;
                }
                break;
            default:
                logger.logMore_2(CLASS_NAME, EXCEPTION_UNKNOWN_CONNECTION_TYPE);
                break;
        }
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buf = ByteBuffer.allocateDirect(4096);
        int read = channel.read(buf);
        if (read == -1) {
            channels.remove(key.attachment());
            messages.remove(key.attachment());
            return;
        }
        buf.flip();
        SocketChannel oppositeChannel;
        switch ((ConnectionType) key.attachment()) {
            case CLIENT_CONNECTION_TYPE:
                messages.get(ConnectionType.CLIENT_CONNECTION_TYPE).add(buf);
                oppositeChannel = channels.get(ConnectionType.SERVER_CONNECTION_TYPE);
                oppositeChannel.register(selector,SelectionKey.OP_WRITE, ConnectionType.SERVER_CONNECTION_TYPE);
                break;
            case SERVER_CONNECTION_TYPE:
                messages.get(ConnectionType.SERVER_CONNECTION_TYPE).add(buf);
                oppositeChannel = channels.get(ConnectionType.SERVER_CONNECTION_TYPE);
                oppositeChannel.register(selector,SelectionKey.OP_WRITE, ConnectionType.SERVER_CONNECTION_TYPE);
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
        channel.register(key.selector(), SelectionKey.OP_READ, ConnectionType.CLIENT_CONNECTION_TYPE);
        messages.put(ConnectionType.CLIENT_CONNECTION_TYPE, new ConcurrentLinkedQueue<ByteBuffer>());
        channels.put(ConnectionType.CLIENT_CONNECTION_TYPE, channel);
        logger.logMore_2(CLASS_NAME, FIELD_ACCEPTED_NEW_CLIENT_CONNECTION + channel.getRemoteAddress());
        //подключение к серверу
        checkConnectionToServer();
    }

    private void checkConnectionToServer() throws IOException {
        SocketChannel channel = SocketChannel.open(
                new InetSocketAddress(environment.getProperty("connection.server.ip"),
                        Integer.valueOf(environment.getProperty("connection.server.port"))));
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_CONNECT, ConnectionType.SERVER_CONNECTION_TYPE);
        channel.finishConnect();
        messages.put(ConnectionType.SERVER_CONNECTION_TYPE, new ConcurrentLinkedQueue<ByteBuffer>());
        channels.put(ConnectionType.SERVER_CONNECTION_TYPE, channel);
        logger.logMore_2(CLASS_NAME, FIELD_CONNECTED_NEW_SERVER);
    }
}
