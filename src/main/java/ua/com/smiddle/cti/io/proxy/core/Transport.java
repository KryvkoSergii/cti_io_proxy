package ua.com.smiddle.cti.io.proxy.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import ua.com.smiddle.cti.io.proxy.core.util.LoggerUtil;

import javax.annotation.PostConstruct;
import java.net.Socket;

/**
 * @author srg on 31.10.16.
 * @project cti_io_proxy
 */
public class Transport implements Runnable {
    private boolean interrupted = false;
    private Socket socket;
    @Autowired
    @Qualifier("LoggerUtil")
    private LoggerUtil logger;

    @PostConstruct
    private void init() {
        logger.logAnyway("Transport", "Initialized");
    }

    @Override
    public void run() {
        while (!interrupted) {

        }
    }
}
