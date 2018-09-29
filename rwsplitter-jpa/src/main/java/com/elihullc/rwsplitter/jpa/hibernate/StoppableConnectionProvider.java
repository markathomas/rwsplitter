package com.elihullc.rwsplitter.jpa.hibernate;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.Stoppable;

/**
 * A simple interface extending {@link ConnectionProvider} and {@link Stoppable }
 */
public interface StoppableConnectionProvider extends ConnectionProvider, Stoppable {

    /**
     * Whether or not this connection provider is read-only
     * @return true if read-only, false otherwise
     */
    boolean isReadOnly();
}
