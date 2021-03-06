/**
 * $RCSfile$
 * $Revision: 11691 $
 * $Date: 2010-05-01 12:42:07 -0400 (Sat, 01 May 2010) $
 *
 * Copyright (C) 2004-2008 B5Chat Community. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;


import org.b5chat.crossfire.core.property.Globals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A connection provider for the embedded hsqlDB database. The database file is stored at
 * <tt>home/database</tt>. The log file for this connection provider is stored at
 * <tt>[home]/logs/EmbeddedConnectionProvider.log</tt>, so you should ensure
 * that the <tt>[home]/logs</tt> directory exists.
 *
 * @author Matt Tucker
 */
public class EmbeddedConnectionProvider implements IConnectionProvider {
	private final static Logger logger = LoggerFactory.getLogger(EmbeddedConnectionProvider.class);
	
    private Properties settings;
    private String serverURL;
    private String driver = "org.hsqldb.jdbcDriver";
    private String proxoolURL;

    public EmbeddedConnectionProvider() {
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
            throw new SQLException("EmbeddedConnectionProvider: Unable to find driver: "+e);
        }
    }

    public void start() {
        File databaseDir = new File(Globals.getHomeDirectory(), File.separator + "embedded-db");
        // If the database doesn't exist, create it.
        if (!databaseDir.exists()) {
            databaseDir.mkdirs();
        }

        try {
            serverURL = "jdbc:hsqldb:" + databaseDir.getCanonicalPath() + File.separator + "crossfire";
        }
        catch (IOException ioe) {
            logger.error("EmbeddedConnectionProvider: Error starting connection pool: ", ioe);
        }
        proxoolURL = "proxool.crossfire:"+driver+":"+serverURL;
        settings = new Properties();
        settings.setProperty("proxool.maximum-connection-count", "25");
        settings.setProperty("proxool.minimum-connection-count", "3");
        settings.setProperty("proxool.maximum-connection-lifetime", Integer.toString((int)(86400000 * 0.5)));
        settings.setProperty("user", "sa");
        settings.setProperty("password", "");
    }

    public void restart() {
        // Kill off pool.
        destroy();
        // Start a new pool.
        start();
    }

    public void destroy() {
        // Shutdown the database.
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = getConnection();
            pstmt = con.prepareStatement("SHUTDOWN");
            pstmt.execute();
        }
        catch (SQLException sqle) {
            logger.error(sqle.getMessage(), sqle);
        }
        finally {
            DbConnectionManager.closeConnection(pstmt, con);
        }
        // Blank out the settings
        settings = null;
    }

    @Override
	public void finalize() throws Throwable {
        super.finalize();
        destroy();
    }
}