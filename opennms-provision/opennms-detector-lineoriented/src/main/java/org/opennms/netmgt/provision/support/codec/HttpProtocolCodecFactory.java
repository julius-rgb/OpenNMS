/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2008-2012 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.provision.support.codec;

import java.nio.charset.Charset;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

/**
 * <p>HttpProtocolCodecFactory class.</p>
 *
 * @author ranger
 * @version $Id: $
 */
public class HttpProtocolCodecFactory implements ProtocolCodecFactory {
    
    private LineOrientedEncoder m_encoder;
    private HttpStatusResponseDecoder m_decoder;
    
    /**
     * <p>Constructor for HttpProtocolCodecFactory.</p>
     */
    public HttpProtocolCodecFactory() {
        this(Charset.defaultCharset());
    }
    
    /**
     * <p>Constructor for HttpProtocolCodecFactory.</p>
     *
     * @param charset a {@link java.nio.charset.Charset} object.
     */
    public HttpProtocolCodecFactory(final Charset charset) {
        m_encoder = new LineOrientedEncoder(charset);
        m_decoder = new HttpStatusResponseDecoder(charset);
    }
    
    /** {@inheritDoc} */
    public ProtocolDecoder getDecoder(final IoSession session) throws Exception {
        return m_decoder;
    }

    /** {@inheritDoc} */
    public ProtocolEncoder getEncoder(final IoSession session) throws Exception {
        return m_encoder;
    }

}
