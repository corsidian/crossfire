/**
 * $RCSfile$
 * $Revision: 11608 $
 * $Date: 2010-02-07 16:03:12 -0500 (Sun, 07 Feb 2010) $
 *
 * Copyright (C) 2005-2008 B5Chat Community. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.b5chat.crossfire.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;


import org.b5chat.crossfire.core.property.Globals;
import org.logicalcobwebs.proxool.ConnectionPoolDefinitionIF;
import org.logicalcobwebs.proxool.ProxoolException;
import org.logicalcobwebs.proxool.ProxoolFacade;
import org.logicalcobwebs.proxool.admin.SnapshotIF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default b5chat connection provider, which uses an internal connection pool.<p>
 *
 * @author B5Chat Community
 */
public class DefaultConnectionProvider implements IConnectionProvider {

	private static final Logger Log = LoggerFactory.getLogger(DefaultConnectionProvider.class);

    private Properties settings;
    private String driver;
    private String serverURL;
    private String proxoolURL;
    private String username;
    private String password;
    private int minConnections = 3;
    private int maxConnections = 10;
    private int activeTimeout = 900000; // 15 minutes in milliseconds
    private String testSQL = "";
    private Boolean testBeforeUse = true;
    private Boolean testAfterUse = true;

    /**
     * Maximum time a connection can be open before it's reopened (in days)
     */
    private double connectionTimeout = 0.5;

    /**
     * MySQL doesn't currently support Unicode. However, a workaround is
     * implemented in the mm.mysql JDBC driver. Setting the b5chat property
     * database.mysql.useUnicode to true will turn this feature on.
     */
    private boolean mysqlUseUnicode;

    /**
     * Creates a new DefaultConnectionProvider.
     */
    public DefaultConnectionProvider() {
        loadProperties();

        System.setProperty("org.apache.commons.logging.LogFactory", "org.b5chat.crossfire.core.util.log.util.CommonsLogFactory");
    }

    public boolean isPooled() {
        return true;
    }

    public Connection getConnection() throws SQLException {
        try {
            Class.forName("org.logicalcobwebs.proxool.ProxoolDriver");
                return DriverManager.getConnection(proxoolURL, settings);
        }
        catch (ClassNotFoundException e) {
            throw new SQLException("DbConnectionProvider: Unable to find driver: "+e);
        }
    }

    public void start() {
        proxoolURL = "proxool.crossfire:"+getDriver()+":"+getServerURL();
        settings = new Properties();
        settings.setProperty("proxool.maximum-activetime", Integer.toString(activeTimeout));
        settings.setProperty("proxool.maximum-connection-count", Integer.toString(getMaxConnections()));
        settings.setProperty("proxool.minimum-connection-count", Integer.toString(getMinConnections()));
        settings.setProperty("proxool.maximum-connection-lifetime", Integer.toString((int)(86400000 * getConnectionTimeout())));
        settings.setProperty("proxool.test-before-use", testBeforeUse.toString());
        settings.setProperty("proxool.test-after-use", testAfterUse.toString());
        settings.setProperty("proxool.house-keeping-test-sql", testSQL);
        settings.setProperty("user", getUsername());
        settings.setProperty("password", (getPassword() != null ? getPassword() : ""));
    }

    public void restart() {
    }

    public void destroy() {
        settings = null;
    }

    /**
     * Returns the JDBC driver classname used to make database connections.
     * For example: com.mysql.jdbc.Driver
     *
     * @return the JDBC driver classname.
     */
    public String getDriver() {
        return driver;
    }

    /**
     * Sets the JDBC driver classname used to make database connections.
     * For example: com.mysql.jdbc.Driver
     *
     * @param driver the fully qualified JDBC driver name.
     */
    public void setDriver(String driver) {
        this.driver = driver;
        saveProperties();
    }

    /**
     * Returns the JDBC connection URL used to make database connections.
     *
     * @return the JDBC connection URL.
     */
    public String getServerURL() {
        return serverURL;
    }

    /**
     * Sets the JDBC connection URL used to make database connections.
     *
     * @param serverURL the JDBC connection URL.
     */
    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
        saveProperties();
    }

    /**
     * Returns the username used to connect to the database. In some cases,
     * a username is not needed so this method will return null.
     *
     * @return the username used to connect to the datbase.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username used to connect to the database. In some cases, a
     * username is not needed so null should be passed in.
     *
     * @param username the username used to connect to the database.
     */
    public void setUsername(String username) {
        this.username = username;
        saveProperties();
    }

    /**
     * Returns the password used to connect to the database. In some cases,
     * a password is not needed so this method will return null.
     *
     * @return the password used to connect to the database.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password used to connect to the database. In some cases, a
     * password is not needed so null should be passed in.
     *
     * @param password the password used to connect to the database.
     */
    public void setPassword(String password) {
        this.password = password;
        saveProperties();
    }

    /**
     * Returns the minimum number of connections that the pool will use. This
     * should probably be at least three.
     *
     * @return the minimum number of connections in the pool.
     */
    public int getMinConnections() {
        return minConnections;
    }

    /**
     * Sets the minimum number of connections that the pool will use. This
     * should probably be at least three.
     *
     * @param minConnections the minimum number of connections in the pool.
     */
    public void setMinConnections(int minConnections) {
        this.minConnections = minConnections;
        saveProperties();
    }

    /**
     * Returns the maximum number of connections that the pool will use. The
     * actual number of connections in the pool will vary between this value
     * and the minimum based on the current load.
     *
     * @return the max possible number of connections in the pool.
     */
    public int getMaxConnections() {
        return maxConnections;
    }

    /**
     * Sets the maximum number of connections that the pool will use. The
     * actual number of connections in the pool will vary between this value
     * and the minimum based on the current load.
     *
     * @param maxConnections the max possible number of connections in the pool.
     */
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
        saveProperties();
    }

    /**
     * Returns the amount of time between connection recycles in days. For
     * example, a value of .5 would correspond to recycling the connections
     * in the pool once every half day.
     *
     * @return the amount of time in days between connection recycles.
     */
    public double getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Sets the amount of time between connection recycles in days. For
     * example, a value of .5 would correspond to recycling the connections
     * in the pool once every half day.
     *
     * @param connectionTimeout the amount of time in days between connection
     *                          recycles.
     */
    public void setConnectionTimeout(double connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        saveProperties();
    }

    /**
     * Returns the SQL statement used to test if a connection is valid.
     *
     * @return the SQL statement that will be run to test a connection.
     */
    public String getTestSQL() {
        return testSQL;
    }

    /**
     * Sets the SQL statement used to test if a connection is valid.  House keeping
     * and before/after connection tests make use of this.  This
     * should be something that causes the minimal amount of work by the database
     * server and is as quick as possible.
     *
     * @param testSQL the SQL statement that will be run to test a connection.
     */
    public void setTestSQL(String testSQL) {
        this.testSQL = testSQL;
    }

    /**
     * Returns whether returned connections will be tested before being handed over
     * to be used.
     *
     * @return True if connections are tested before use.
     */
    public Boolean getTestBeforeUse() {
        return testBeforeUse;
    }

    /**
     * Sets whether connections will be tested before being handed over to be used.
     *
     * @param testBeforeUse True or false if connections are to be tested before use.
     */
    public void setTestBeforeUse(Boolean testBeforeUse) {
        this.testBeforeUse = testBeforeUse;
    }

    /**
     * Returns whether returned connections will be tested after being returned to
     * the pool.
     *
     * @return True if connections are tested after use.
     */
    public Boolean getTestAfterUse() {
        return testAfterUse;
    }

    /**
     * Sets whether connections will be tested after being returned to the pool.
     *
     * @param testAfterUse True or false if connections are to be tested after use.
     */
    public void setTestAfterUse(Boolean testAfterUse) {
        this.testAfterUse = testAfterUse;
    }

    public boolean isMysqlUseUnicode() {
        return mysqlUseUnicode;
    }

    /**
     * Load properties that already exist from b5chat properties.
     */
    private void loadProperties() {

        driver = Globals.getXMLProperty("database.defaultProvider.driver");
        serverURL = Globals.getXMLProperty("database.defaultProvider.serverURL");
        username = Globals.getXMLProperty("database.defaultProvider.username");
        password = Globals.getXMLProperty("database.defaultProvider.password");
        String minCons = Globals.getXMLProperty("database.defaultProvider.minConnections");
        String maxCons = Globals.getXMLProperty("database.defaultProvider.maxConnections");
        String conTimeout = Globals.getXMLProperty("database.defaultProvider.connectionTimeout");
        testSQL = Globals.getXMLProperty("database.defaultProvider.testSQL", DbConnectionManager.getTestSQL(driver));
        testBeforeUse = Globals.getXMLProperty("database.defaultProvider.testBeforeUse", true);
        testAfterUse = Globals.getXMLProperty("database.defaultProvider.testAfterUse", true);

        // See if we should use Unicode under MySQL
        mysqlUseUnicode = Boolean.valueOf(Globals.getXMLProperty("database.mysql.useUnicode"));
        try {
            if (minCons != null) {
                minConnections = Integer.parseInt(minCons);
            }
            if (maxCons != null) {
                maxConnections = Integer.parseInt(maxCons);
            }
            if (conTimeout != null) {
                connectionTimeout = Double.parseDouble(conTimeout);
            }
        }
        catch (Exception e) {
            Log.error("Error: could not parse default pool properties. " +
                    "Make sure the values exist and are correct.", e);
        }
    }

    /**
     * Save properties as b5chat properties.
     */
    private void saveProperties() {

        Globals.setXMLProperty("database.defaultProvider.driver", driver);
        Globals.setXMLProperty("database.defaultProvider.serverURL", serverURL);
        Globals.setXMLProperty("database.defaultProvider.username", username);
        Globals.setXMLProperty("database.defaultProvider.password", password);
        Globals.setXMLProperty("database.defaultProvider.testSQL", testSQL);
        Globals.setXMLProperty("database.defaultProvider.testBeforeUse", testBeforeUse.toString());
        Globals.setXMLProperty("database.defaultProvider.testAfterUse", testAfterUse.toString());

        Globals.setXMLProperty("database.defaultProvider.minConnections",
                Integer.toString(minConnections));
        Globals.setXMLProperty("database.defaultProvider.maxConnections",
                Integer.toString(maxConnections));
        Globals.setXMLProperty("database.defaultProvider.connectionTimeout",
                Double.toString(connectionTimeout));
    }

    @Override
	public String toString() {
        try {
            ConnectionPoolDefinitionIF poolDef = ProxoolFacade.getConnectionPoolDefinition("crossfire");
            SnapshotIF poolStats = ProxoolFacade.getSnapshot("crossfire", true);
            return poolDef.getMinimumConnectionCount()+","+poolDef.getMaximumConnectionCount()+","
                    +poolStats.getAvailableConnectionCount()+","+poolStats.getActiveConnectionCount();
        }
        catch (ProxoolException e) {
            return "Default IConnection Provider";
        }
    }
}
