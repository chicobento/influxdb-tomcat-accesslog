/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cbnt.influxdb;


import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;

import org.apache.catalina.AccessLog;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Serie;

/**
 * <p>
 * This Tomcat extension logs server access directly to a InfluxDB database, and can
 * be used instead of the regular file-based access log implemented in
 * AccessLogValve.
 * To use, copy this jar and its dependencies into the lib directory of the Tomcat installation
 * and configure in server.xml as:
 * </p>
 * <pre>
 *         &lt;Valve className="com.cbnt.influxdb.InfluxDBAccessLogValve" 
 *               connectionURL="http://localhost:8086/" 
 *              connectionName="root"
 *          connectionPassword="root" 
 *                databaseName="tomcatLogs"
 *                  seriesName="accessLogs" 
 *                     pattern="combined" 
 *         /&gt;
 * </pre>
 * <p>
 * It does also supports UDP mode through the parameters 
 * <code>transport</code>(TCP|UDP) and <code>udpPort</code>),
 * The same options as AccessLogValve are supported, such as
 * <code>resolveHosts</code> and <code>pattern</code> ("common" or "combined"
 * only).
 * </p>
 * <p>
 * When Tomcat is started, a database connection is created and used for all the
 * log activity. When Tomcat is shutdown, the database connection is closed.
 * This logger can be used at the level of the Engine context (being shared
 * by all the defined hosts) or the Host context (one instance of the logger
 * per host, possibly using different databases).
 * </p>
 * <p>
 * If the request method is "common", only these fields are used:
 * <code>remoteHost, user, timeStamp, query, status, bytes</code>
 * </p>
 * <p>
 * <i>TO DO: provide option for excluding logging of certain MIME types.</i>
 * </p>
 *
 * This class is based on the <a href="https://tomcat.apache.org/tomcat-8.0-doc/api/org/apache/catalina/valves/JDBCAccessLogValve.html">JDBCAccessLogValve</a> class 
 * by Andre de Jesus and Peter Rossbach.
 * 
 * @author <a href="mailto:chicobento@gmail.com">Francisco Bento da Silva Neto</a>
 * @see https://tomcat.apache.org/tomcat-8.0-doc/api/org/apache/catalina/valves/JDBCAccessLogValve.html
 */

public final class InfluxDBAccessLogValve extends ValveBase implements AccessLog {

	// ----------------------------------------------------------- Constants
	private static final String TRANSPORT_TCP = "TCP";
	private static final String TRANSPORT_UDP = "UDP";
	private static final String PATTERN_COMBINED = "combined";
	private static final String[] commonColumns = new String[] {"remoteHost","userName","query","status","bytes"};
	private static final String[] combinedColumns = new String[] {"remoteHost","userName","query","status","bytes","virtualHost","method","referer","userAgent"};

	// ----------------------------------------------------------- Constructors


    /**
     * Class constructor. Initializes the fields with the default values.
     * The defaults are:
     * <pre>
     *   connectionName = null;
     *   connectionPassword = null;
     *   connectionURL = null;
     *   seriesName = "accessLogs";
     *   databaseName = null;
     *   transport = TRANSPORT_TCP;
     *   udpPort = null;
     *   pattern = "common";
     *   resolveHosts = false;
     * </pre>
     */
    public InfluxDBAccessLogValve() {
        super(true);
        connectionName = null;
        connectionPassword = null;
        connectionURL = null;
        seriesName = "accessLogs";
        databaseName = null;
        transport = TRANSPORT_TCP;
        udpPort = null;
        pattern = "common";
        resolveHosts = false;
        influxDB = null;
    }


    // ----------------------------------------------------- Instance Variables

   /**
     * The connection user name to use when trying to connect to the database.
     */
    protected String connectionName = null;

    /**
     * The connection password to use when trying to connect to the database.
     */
    protected String connectionPassword = null;

    /**
     * The connection URL to use when trying to connect to the database.
     */
    private String connectionURL = null;
    
    /**
     * The series name where the logs will be stored
     */
    private String seriesName = null;
    
    /**
     * The database name where the logs will be stored
     */
    private String databaseName = null;
    
    /**
     * The transport protocol.
     */
    private String transport = null;
    
    /**
     * The UDP port in case TRANSPORT = UDP
     */
    private Integer udpPort = null;
    
    /**
     * The log pattern. COMMON or COMBINED
     */
    private String pattern = null;
    
    
    private boolean resolveHosts = false;

    /**
     * The influxDB object
     */
	private InfluxDB influxDB = null;

    /**
     * @see #setRequestAttributesEnabled(boolean)
     */
    protected boolean requestAttributesEnabled = true;

    // ------------------------------------------------------------- Properties

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRequestAttributesEnabled(boolean requestAttributesEnabled) {
        this.requestAttributesEnabled = requestAttributesEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getRequestAttributesEnabled() {
        return requestAttributesEnabled;
    }

    /**
     * Set the username to use to connect to the database.
     *
     * @param connectionName Username
     */
    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    /**
     * Set the password to use to connect to the database.
     *
     * @param connectionPassword User password
     */
    public void setConnectionPassword(String connectionPassword) {
        this.connectionPassword = connectionPassword;
    }

    /**
     * Sets the JDBC URL for the database where the log is stored.
     *
     * @param connectionURL The JDBC URL of the database.
     */
    public void setConnectionURL(String connectionURL) {
        this.connectionURL = connectionURL;
    }

    /**
     * Sets the logging pattern. The patterns supported correspond to the
     * file-based "common" and "combined". 
     * <P><I>TO DO: more flexible field choices.</I></P>
     *
     * @param pattern The name of the logging pattern.
     */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }


    /**
     * Determines whether IP host name resolution is done.
     *
     * @param resolveHosts "true" or "false", if host IP resolution
     * is desired or not.
     */
    public void setResolveHosts(String resolveHosts) {
        this.resolveHosts = Boolean.valueOf(resolveHosts).booleanValue();
    }

    /**
     * The database series name
     * @param seriesName
     */
    public void setSeriesName(String seriesName) {
		this.seriesName = seriesName;
	}

    /**
     * The database name
     * @param databaseName
     */
	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	/**
	 * The transport protocol. UDP or TCP
	 * @param transport
	 */
	public void setTransport(String transport) {
		this.transport = transport;
	}

	/**
	 * The UDP port
	 * @param udpPort
	 */
	public void setUdpPort(String udpPort) {
		this.udpPort = Integer.valueOf(udpPort);
	}

    // --------------------------------------------------------- Public Methods

	/**
     * This method is invoked by Tomcat on each query.
     *
     * @param request The Request object.
     * @param response The Response object.
     *
     * @exception IOException Should not be thrown.
     * @exception ServletException Database SQLException is wrapped
     * in a ServletException.
     */
    @Override
    public void invoke(Request request, Response response) throws IOException,
            ServletException {
        getNext().invoke(request, response);
    }


    @Override
    public void log(Request request, Response response, long time) {
        if (!getState().isAvailable()) {
            return;
        }

        final String EMPTY = "" ;

        String remoteHost;
        if(resolveHosts) {
            if (requestAttributesEnabled) {
                Object host = request.getAttribute(REMOTE_HOST_ATTRIBUTE);
                if (host == null) {
                    remoteHost = request.getRemoteHost();
                } else {
                    remoteHost = (String) host;
                }
            } else {
                remoteHost = request.getRemoteHost();
            }
        } else {
            if (requestAttributesEnabled) {
                Object addr = request.getAttribute(REMOTE_ADDR_ATTRIBUTE);
                if (addr == null) {
                    remoteHost = request.getRemoteAddr();
                } else {
                    remoteHost = (String) addr;
                }
            } else {
                remoteHost = request.getRemoteAddr();
            }
        }
        String user = request.getRemoteUser();
        String query=request.getRequestURI();

        long bytes = response.getBytesWritten(true);
        if(bytes < 0) {
            bytes = 0;
        }
        int status = response.getStatus();
        String virtualHost = EMPTY;
        String method = EMPTY;
        String referer = EMPTY;
        String userAgent = EMPTY;
        String logPattern = pattern;
        if (logPattern.equals(PATTERN_COMBINED)) {
            virtualHost = request.getServerName();
            method = request.getMethod();
            referer = request.getHeader("referer");
            userAgent = request.getHeader("user-agent");
        }
        synchronized (this) {
            try {
            	
                Serie serie = null;
                if (logPattern.equals(PATTERN_COMBINED)) {
                	serie = new Serie.Builder(seriesName)
                					  .columns(combinedColumns)
                					  .values(remoteHost, 
                							  user, 
                							  query,
                							  status,
                							  bytes,
                							  virtualHost,
                							  method,
                							  referer,
                							  userAgent).build();
                } else {
                	serie = new Serie.Builder(seriesName)
					  .columns(commonColumns)
					  .values(remoteHost, 
							  user, 
							  query,
							  status,
							  bytes).build();
                }
                if (TRANSPORT_UDP.equals(transport)) {
                	influxDB.writeUdp(udpPort, serie);
                } else {
                	influxDB.write(databaseName, TimeUnit.MILLISECONDS, serie);
                }
                
                return;
              } catch (RuntimeException e) {
                // Log the problem for posterity
                  container.getLogger().error(sm.getString("influxDBAccessLogValve.exception"), e);

                // Close the connection so that it gets reopened next time
                if (influxDB != null) {
                    close();
                }
              }
        }

    }


    /**
     * Open (if necessary) and return a database connection for use by
     * this AccessLogValve.
     *
     * @exception RuntimeException if a connection error occurs
     */
    protected void open() throws RuntimeException {

        // Do nothing if there is a database connection already open
        if (influxDB != null) {
            return ;
        }
        this.influxDB = InfluxDBFactory.connect(connectionURL, connectionName, connectionPassword);
    }

    /**
     * Close the database connection.
     */
    protected void close() {
    	influxDB = null;
    }


    /**
     * Start this component and implement the requirements
     * of {@link org.apache.catalina.util.LifecycleBase#startInternal()}.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    @Override
    protected synchronized void startInternal() throws LifecycleException {

        try {
            open() ;
        } catch (RuntimeException e) {
            throw new LifecycleException(e);
        }

        setState(LifecycleState.STARTING);
    }


    /**
     * Stop this component and implement the requirements
     * of {@link org.apache.catalina.util.LifecycleBase#stopInternal()}.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    @Override
    protected synchronized void stopInternal() throws LifecycleException {

        setState(LifecycleState.STOPPING);

        close() ;
    }

}
