package ua.com.smiddle.cti.io.proxy.core.services;


import ua.com.smiddle.cti.io.proxy.core.model.Log;

/**
 * Интерфейс доступа к модулю сохраниения логов.
 *
 * @author Kryvko Sergii (ksa@smiddle.com.ua)
 * @project SmiddleFinesseConnector
 */
public interface LogPersister {
    void persist(Log log);
}
