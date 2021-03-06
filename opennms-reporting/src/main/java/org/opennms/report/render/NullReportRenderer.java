/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2009-2012 The OpenNMS Group, Inc.
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

package org.opennms.report.render;

import java.io.InputStream;
import java.io.OutputStream;

import org.opennms.core.utils.ThreadCategory;
import org.opennms.reporting.availability.render.ReportRenderException;
import org.opennms.reporting.availability.render.ReportRenderer;
import org.springframework.core.io.Resource;

/**
 * NullReportRenderer will do nothing.
 *
 * @author <a href="mailto:antonio@opennms.it">Antonio Russo</a>
 * @version $Id: $
 */
public class NullReportRenderer implements ReportRenderer {

    private static final String LOG4J_CATEGORY = "OpenNMS.Report";

    private String m_outputFileName;
    
    private String m_inputFileName;
    
    private String m_baseDir;

    @SuppressWarnings("unused")
    private Resource m_xsltResource;

    /**
     * <p>render</p>
     *
     * @throws org.opennms.reporting.availability.render.ReportRenderException if any.
     */
    public void render() throws ReportRenderException {
        String oldPrefix = ThreadCategory.getPrefix();
        ThreadCategory.setPrefix(LOG4J_CATEGORY);
        ThreadCategory log = ThreadCategory.getInstance(NullReportRenderer.class);
        log.debug("Do nothing");
        m_outputFileName = m_inputFileName;
        ThreadCategory.setPrefix(oldPrefix);
    }

    /** {@inheritDoc} */
    public void setXsltResource(Resource xsltResource) {
        this.m_xsltResource = xsltResource;
    }

    /** {@inheritDoc} */
    public void setOutputFileName(String outputFileName) {
        this.m_outputFileName = outputFileName;
    }
    
    /**
     * <p>getOutputFileName</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getOutputFileName() {
        return m_outputFileName;
    }

    /** {@inheritDoc} */
    public void setInputFileName(String intputFileName) {
        this.m_inputFileName = intputFileName;
    }
    
    /** {@inheritDoc} */
    public void setBaseDir(String baseDir){
        this.m_baseDir = baseDir;
    }
    
    /**
     * <p>getBaseDir</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getBaseDir(){
       return m_baseDir;
    }

    /** {@inheritDoc} */
    public void render(String inputFileName, String outputFileName,
            Resource xlstResource)
            throws org.opennms.reporting.availability.render.ReportRenderException {
    }

    /** {@inheritDoc} */
    public void render(String inputFileName, OutputStream outputStream,
            Resource xsltResource)
            throws org.opennms.reporting.availability.render.ReportRenderException {
    }

    /** {@inheritDoc} */
    public void render(InputStream inputStream, OutputStream outputStream,
            Resource xsltResource)
            throws org.opennms.reporting.availability.render.ReportRenderException {
    }

    /** {@inheritDoc} */
    public byte[] render(String inputFileName, Resource xsltResource)
            throws org.opennms.reporting.availability.render.ReportRenderException {
        return new byte[0];
    }

}
