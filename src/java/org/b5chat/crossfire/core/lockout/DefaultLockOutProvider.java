/**
 * $Revision$
 * $Date$
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
package org.b5chat.crossfire.core.lockout;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;


import org.b5chat.crossfire.core.util.StringUtils;
import org.b5chat.crossfire.database.DbConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The DefaultLockOutProvider works with the ofUserFlag table to maintain a list of disabled/locked out
 * accounts., and as the name implies, is the default ILockOutProvider implementation.
 *
 * @author Daniel Henninger
 */
public class DefaultLockOutProvider implements ILockOutProvider {

	private static final Logger Log = LoggerFactory.getLogger(DefaultLockOutProvider.class);

    private static final String FLAG_ID = "lockout";
    private static final String DELETE_FLAG =
            "DELETE FROM ofUserFlag WHERE username=? AND name='"+FLAG_ID+"'";
    private static final String ADD_FLAG =
            "INSERT INTO ofUserFlag VALUES(?,'"+FLAG_ID+"',?,?)";
    private static final String RETRIEVE_FLAG =
            "SELECT name,startTime,endTime FROM ofUserFlag WHERE username=? AND name='"+FLAG_ID+"'";

    /**
     * Constructs a new DefaultLockOutProvider
     */
    public DefaultLockOutProvider() {

    }

    /**
     * Default provider retrieves disabled status from ofUserFlag table.
     * @see org.b5chat.crossfire.core.lockout.ILockOutProvider#getDisabledStatus(String)
     */
    public LockOutFlag getDisabledStatus(String username) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        LockOutFlag ret = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(RETRIEVE_FLAG);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();
            if (!rs.next()) {
                return null;
            }
            Date startTime = null;
            if (rs.getString(2) != null) {
                startTime = new Date(Long.parseLong(rs.getString(2).trim()));
            }
            Date endTime = null;
            if (rs.getString(3) != null) {
                endTime = new Date(Long.parseLong(rs.getString(3).trim()));
            }

            ret = new LockOutFlag(username, startTime, endTime);
        }
        catch (Exception e) {
            Log.error("Error loading lockout information from DB", e);
            return null;
        }
        finally {
            DbConnectionManager.closeConnection(rs, pstmt, con);
        }

        return ret;
    }

    /**
     * Default provider deletes existing flag, if it exists, and adds new described flag in ofUserFlag table.
     * @see org.b5chat.crossfire.core.lockout.ILockOutProvider#setDisabledStatus(LockOutFlag)
     */
    public void setDisabledStatus(LockOutFlag flag) {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(DELETE_FLAG);
            pstmt.setString(1, flag.getUsername());
            pstmt.executeUpdate();
        }
        catch (SQLException sqle) {
            // Nothing to do.
        }
        finally {
            DbConnectionManager.closeConnection(pstmt, con);
        }
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(ADD_FLAG);
            pstmt.setString(1, flag.getUsername());
            if (flag.getStartTime() != null) {
                pstmt.setString(2, StringUtils.dateToMillis(flag.getStartTime()));
            }
            else {
                pstmt.setNull(2, Types.VARCHAR);
            }
            if (flag.getEndTime() != null) {
                pstmt.setString(3, StringUtils.dateToMillis(flag.getEndTime()));
            }
            else {
                pstmt.setNull(3, Types.VARCHAR);
            }
            pstmt.executeUpdate();
        }
        catch (SQLException sqle) {
            // Nothing to do.
        }
        finally {
            DbConnectionManager.closeConnection(pstmt, con);
        }
    }

    /**
     * Default provider deletes existing flag from ofUserFlag table.
     * @see org.b5chat.crossfire.core.lockout.ILockOutProvider#unsetDisabledStatus(String)
     */
    public void unsetDisabledStatus(String username) {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(DELETE_FLAG);
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        }
        catch (SQLException sqle) {
            // Nothing to do.
        }
        finally {
            DbConnectionManager.closeConnection(pstmt, con);
        }
    }

    /**
     * Default provider allows editing of disabled status.
     * @see org.b5chat.crossfire.core.lockout.ILockOutProvider#isReadOnly()
     */
    public boolean isReadOnly() {
        return false;
    }

    /**
     * Default provider allows delayed start to disabled status.
     * @see org.b5chat.crossfire.core.lockout.ILockOutProvider#isDelayedStartSupported()
     */
    public boolean isDelayedStartSupported() {
        return true;
    }

    /**
     * Default provider allows timeout of disabled status.
     * @see org.b5chat.crossfire.core.lockout.ILockOutProvider#isTimeoutSupported()
     */
    public boolean isTimeoutSupported() {
        return true;
    }

    /**
     * Default provider should be cached.
     * @see org.b5chat.crossfire.core.lockout.ILockOutProvider#shouldNotBeCached()
     */
    public boolean shouldNotBeCached() {
        return false;
    }

}
