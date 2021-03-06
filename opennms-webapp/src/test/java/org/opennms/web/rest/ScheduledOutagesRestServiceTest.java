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

package org.opennms.web.rest;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;

import javax.xml.bind.JAXBContext;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.config.CollectdConfigFactory;
import org.opennms.netmgt.config.NotifdConfigFactory;
import org.opennms.netmgt.config.PollOutagesConfigFactory;
import org.opennms.netmgt.config.PollerConfigFactory;
import org.opennms.netmgt.config.ThreshdConfigFactory;
import org.opennms.netmgt.config.poller.Outage;
import org.opennms.netmgt.dao.FilterDao;
import org.opennms.netmgt.filter.FilterDaoFactory;
import org.opennms.test.ConfigurationTestUtils;
import org.opennms.test.mock.MockLogAppender;
import org.springframework.core.io.FileSystemResource;

public class ScheduledOutagesRestServiceTest extends AbstractSpringJerseyRestTestCase {

    private JAXBContext m_jaxbContext;
    private FilterDao m_filterDao;
    private String m_onmsHome;

    @Override
    public void beforeServletStart() throws Exception {
        MockLogAppender.setupLogging();
        File etc = new File("target/test-work-dir/etc");
        etc.mkdirs();
        m_onmsHome = etc.getParent();
        System.setProperty("opennms.home", m_onmsHome);
        ConfigurationTestUtils.setRelativeHomeDirectory(m_onmsHome);

        // Setup Scheduled Outages Configuration
        File outagesConfig = new File(etc, "poll-outages.xml");
        FileUtils.writeStringToFile(outagesConfig, "<?xml version=\"1.0\"?>"
                + "<outages>"
                + "<outage name='my-junit-test' type='weekly'>"
                + "<time day='monday' begins='13:30:00' ends='14:45:00'/>"
                + "<interface address='match-any'/>"
                + "</outage>"
                + "</outages>");
        PollOutagesConfigFactory factory = new PollOutagesConfigFactory(new FileSystemResource(outagesConfig));
        PollOutagesConfigFactory.setInstance(factory);

        // Setup Filter DAO
        m_filterDao = EasyMock.createMock(FilterDao.class);
        EasyMock.expect(m_filterDao.getActiveIPAddressList("IPADDR != '0.0.0.0'")).andReturn(Collections.singletonList(InetAddressUtils.getLocalHostAddress())).anyTimes();
        EasyMock.replay(m_filterDao);
        FilterDaoFactory.setInstance(m_filterDao);

        // Setup Collectd Configuration
        File collectdConfig = new File(etc, "collectd-configuration.xml");
        FileUtils.writeStringToFile(collectdConfig, "<?xml version=\"1.0\"?>"
                + "<collectd-configuration threads=\"50\">"
                + "<package name=\"example1\">"
                + "<filter>IPADDR != '0.0.0.0'</filter>"
                + "<include-range begin=\"1.1.1.1\" end=\"254.254.254.254\"/>"
                + "<service name=\"SNMP\" interval=\"300000\" user-defined=\"false\" status=\"on\">"
                + "<parameter key=\"collection\" value=\"default\"/>"
                + "</service>"
                + "</package>"
                + "<collector service=\"SNMP\" class-name=\"org.opennms.netmgt.collectd.SnmpCollector\"/>"
                + "</collectd-configuration>");
        CollectdConfigFactory.setInstance(new CollectdConfigFactory(new FileInputStream(collectdConfig), "localhost", false));

        // Setup Pollerd Configuration
        File pollerdConfig = new File(etc, "poller-configuration.xml");
        FileUtils.writeStringToFile(pollerdConfig, "<?xml version=\"1.0\"?>"
                + "<poller-configuration threads=\"10\" nextOutageId=\"SELECT nextval(\'outageNxtId\')\" serviceUnresponsiveEnabled=\"false\">"
                + "<node-outage status=\"on\" pollAllIfNoCriticalServiceDefined=\"true\"></node-outage>"
                + "<package name=\"example1\">"
                + "<filter>IPADDR != '0.0.0.0'</filter>"
                + "<rrd step = \"300\">"
                + "<rra>RRA:AVERAGE:0.5:1:2016</rra>"
                + "<rra>RRA:AVERAGE:0.5:12:4464</rra>"
                + "<rra>RRA:MIN:0.5:12:4464</rra>"
                + "<rra>RRA:MAX:0.5:12:4464</rra>"
                + "</rrd>"
                + "<service name=\"ICMP\" interval=\"300000\"/>"
                + "<downtime begin=\"0\" end=\"30000\"/>"
                + "</package>"
                + "<monitor service=\"ICMP\" class-name=\"org.opennms.netmgt.poller.monitors.IcmpMonitor\"/>"
                + "</poller-configuration>");
        PollerConfigFactory.setInstance(new PollerConfigFactory(1, new FileInputStream(pollerdConfig), "localserver", false));

        // Setup Threshd Configuration
        File threshdConfig = new File(etc, "threshd-configuration.xml");
        FileUtils.writeStringToFile(threshdConfig, "<?xml version=\"1.0\"?>"
                + "<threshd-configuration threads=\"5\">"
                + "<package name=\"example1\">"
                + "<filter>IPADDR != '0.0.0.0'</filter>"
                + "<include-range begin=\"1.1.1.1\" end=\"254.254.254.254\"/>"
                + "<service name=\"SNMP\" interval=\"300000\" user-defined=\"false\" status=\"on\">"
                + "<parameter key=\"thresholding-group\" value=\"mib2\"/>"
                + "</service>"
                + "</package>"
                + "</threshd-configuration>");
        ThreshdConfigFactory.setInstance(new ThreshdConfigFactory(new FileInputStream(threshdConfig), "localserver", false));

        // Setup Notifid Configuration
        FileUtils.writeStringToFile(new File(etc, "notifd-configuration.xml"), "<?xml version=\"1.0\"?>"
                + "<notifd-configuration status=\"off\" match-all=\"true\">"
                + "<queue><queue-id>default</queue-id><interval>20s</interval>"
                + "<handler-class><name>org.opennms.netmgt.notifd.DefaultQueueHandler</name></handler-class>"
                + "</queue>"
                + "</notifd-configuration>");
        NotifdConfigFactory.init();

        m_jaxbContext = JAXBContext.newInstance(Outage.class);
    }

    // This is required in order to avoid override configuration files in opennms-base-assembly
    @Override
    public void afterServletStart() {
        System.setProperty("opennms.home", m_onmsHome);
        ConfigurationTestUtils.setRelativeHomeDirectory(m_onmsHome);
    }

    @Override
    public void afterServletDestroy() {
        EasyMock.verify(m_filterDao);
        MockLogAppender.assertNoWarningsOrGreater();
    }

    @Test
    public void testGetOutage() throws Exception {
        String url = "/sched-outages/my-junit-test";
        Outage outage = getXmlObject(m_jaxbContext, url, 200, Outage.class);
        Assert.assertNotNull(outage);
        Assert.assertEquals("match-any", outage.getInterface(0).getAddress());
    }

    @Test
    public void testSetOutage() throws Exception {
        String url = "/sched-outages";
        String outage = "<?xml version=\"1.0\"?>" +
                "<outage name='test-outage' type='specific'>" +
                "<time day='friday' begins='13:20:00' ends='15:30:00' />" +
                "<time begins='17-Feb-2012 19:20:00' ends='18-Feb-2012 22:30:00' />" +
                "<node id='11' />" +
                "</outage>";
        sendPost(url, outage);
    }

    @Test
    public void testDeleteOutage() throws Exception {
        sendRequest(DELETE, "/sched-outages/my-junit-test", 200);
    }

    @Test
    public void testUpdateCollectdConfig() throws Exception {
        sendRequest(PUT, "/sched-outages/my-junit-test/collectd/example1", 200);
        sendRequest(DELETE, "/sched-outages/my-junit-test/collectd/example1", 200);
    }

    @Test
    public void testUpdatePollerdConfig() throws Exception {
        sendRequest(PUT, "/sched-outages/my-junit-test/pollerd/example1", 200);
        sendRequest(DELETE, "/sched-outages/my-junit-test/pollerd/example1", 200);
    }

    @Test
    public void testUpdateThreshdConfig() throws Exception {
        sendRequest(PUT, "/sched-outages/my-junit-test/threshd/example1", 200);
        sendRequest(DELETE, "/sched-outages/my-junit-test/threshd/example1", 200);
    }

    @Test
    public void testUpdateNotifdConfig() throws Exception {
        sendRequest(PUT, "/sched-outages/my-junit-test/notifd", 200);
        sendRequest(DELETE, "/sched-outages/my-junit-test/notifd", 200);
    }

}
