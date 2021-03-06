/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2011-2012 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2012 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.config;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.opennms.core.utils.LogUtils;
import org.opennms.netmgt.config.opennmsDataSources.JdbcDataSource;

import snaq.db.DBPoolDataSource;

/**
 * <p>C3P0ConnectionFactory class.</p>
 */
public class DBPoolConnectionFactory extends BaseConnectionFactory {

	private DBPoolDataSource m_dataSource;

    public DBPoolConnectionFactory(final InputStream stream, final String dsName) throws MarshalException, ValidationException, PropertyVetoException, SQLException {
    	super(stream, dsName);
    }

    public DBPoolConnectionFactory(final String configFile, final String dsName) throws IOException, MarshalException, ValidationException, PropertyVetoException, SQLException {
    	super(configFile, dsName);
    }

    protected void initializePool(final JdbcDataSource dataSource) throws SQLException {
    	m_dataSource = new DBPoolDataSource();
    	m_dataSource.setName(dataSource.getName());
    	m_dataSource.setDriverClassName(dataSource.getClassName());
    	m_dataSource.setUrl(dataSource.getUrl());
    	m_dataSource.setUser(dataSource.getUserName());
    	m_dataSource.setPassword(dataSource.getPassword());
    }

    public Connection getConnection() throws SQLException {
    	return m_dataSource.getConnection();
    }

    public String getUrl() {
    	return m_dataSource.getUrl();
    }

    public void setUrl(final String url) {
    	m_dataSource.setUrl(url);
    }

    public String getUser() {
    	return m_dataSource.getUser();
    }

    public void setUser(final String user) {
    	m_dataSource.setUser(user);
    }

    public DataSource getDataSource() {
    	return m_dataSource;
    }

    public Connection getConnection(final String username, final String password) throws SQLException {
    	return m_dataSource.getConnection(username, password);
    }

    public PrintWriter getLogWriter() throws SQLException {
    	return m_dataSource.getLogWriter();
    }

    public void setLogWriter(final PrintWriter out) throws SQLException {
        m_dataSource.setLogWriter(out);
    }

    public void setLoginTimeout(final int seconds) throws SQLException {
        m_dataSource.setLoginTimeout(seconds);
    }

    public int getLoginTimeout() throws SQLException {
        return m_dataSource.getLoginTimeout();
    }

    public void close() throws SQLException {
    	super.close();
    	LogUtils.infof(this, "Closing DBPool pool.");
    	m_dataSource.release();
    }

	public void setIdleTimeout(final int idleTimeout) {
		m_dataSource.setIdleTimeout(idleTimeout);
	}

	public void setMinPool(final int minPool) {
		m_dataSource.setMinPool(minPool);
	}

	public void setMaxPool(final int maxPool) {
		m_dataSource.setMaxPool(maxPool);
	}

	public void setMaxSize(final int maxSize) {
		m_dataSource.setMaxSize(maxSize);
	}
}
