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

package org.opennms.netmgt.dao.hibernate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.utils.LogUtils;
import org.opennms.netmgt.dao.DataLinkInterfaceDao;
import org.opennms.netmgt.dao.DatabasePopulator;
import org.opennms.netmgt.dao.NodeDao;
import org.opennms.netmgt.dao.db.JUnitConfigurationEnvironment;
import org.opennms.netmgt.dao.db.JUnitTemporaryDatabase;
import org.opennms.netmgt.model.DataLinkInterface;
import org.opennms.netmgt.model.OnmsCriteria;
import org.opennms.netmgt.model.OnmsNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations={
        "classpath:/META-INF/opennms/applicationContext-dao.xml",
        "classpath:/META-INF/opennms/applicationContext-databasePopulator.xml",
        "classpath:/META-INF/opennms/applicationContext-setupIpLike-enabled.xml",
        "classpath*:/META-INF/opennms/component-dao.xml"
})
@JUnitConfigurationEnvironment
@JUnitTemporaryDatabase
public class DataLinkInterfaceDaoHibernateTest {
    @Autowired
    private NodeDao m_nodeDao;
    
	@Autowired
    private DataLinkInterfaceDao m_dataLinkInterfaceDao;

	@Autowired
	private DatabasePopulator m_databasePopulator;
	
	@Before
	public void setUp() {
	    for (final OnmsNode node : m_nodeDao.findAll()) {
	        m_nodeDao.delete(node);
	    }
	    m_nodeDao.flush();
		m_databasePopulator.populateDatabase();
	}

    @Test
    public void testFindById() throws Exception {
        // Note: This ID is based upon the creation order in DatabasePopulator - if you change
        // the DatabasePopulator by adding additional new objects that use the onmsNxtId sequence
        // before the creation of this object then this ID may change and this test will fail.
        //
        int id = 64;
        DataLinkInterface dli = m_dataLinkInterfaceDao.findById(id);
        if (dli == null) {
            List<DataLinkInterface> dlis = m_dataLinkInterfaceDao.findAll();
            StringBuffer ids = new StringBuffer();
            for (DataLinkInterface current : dlis) {
                if (ids.length() > 0) {
                    ids.append(", ");
                }
                ids.append(current.getId());
            }
            fail("No DataLinkInterface record with ID " + id + " was found, the only IDs are: " + ids.toString());
        }
        assertNotNull(dli);
        assertEquals(m_databasePopulator.getNode1().getId(), dli.getNode().getId());
        assertEquals(Integer.valueOf(1), dli.getIfIndex());
    }

    @Test
    public void testFindByCriteria() throws Exception {
        OnmsCriteria criteria = new OnmsCriteria(DataLinkInterface.class);
        criteria.createAlias("node", "node", OnmsCriteria.LEFT_JOIN);
        criteria.add(Restrictions.or(
            Restrictions.eq("node.id", m_databasePopulator.getNode1().getId()),
            Restrictions.eq("nodeParentId", m_databasePopulator.getNode1().getId())
        ));
        
        final List<DataLinkInterface> dlis = m_dataLinkInterfaceDao.findMatching(criteria);
        for (final DataLinkInterface iface : dlis) {
            LogUtils.debugf(this, "dli = %s", iface);
        }
        assertEquals(3, dlis.size());
    }
    
    @Test
    @Transactional // why is this necessary?
    public void testSaveDataLinkInterface() {
        // Create a new data link interface and save it.
        DataLinkInterface dli = new DataLinkInterface(m_databasePopulator.getNode2(), 2, m_databasePopulator.getNode1().getId(), 1, "?", new Date());
        dli.setLinkTypeId(101);
        m_dataLinkInterfaceDao.save(dli);
        m_dataLinkInterfaceDao.flush();

        assertNotNull(m_dataLinkInterfaceDao.get(dli.getId()));

        DataLinkInterface dli2 = m_dataLinkInterfaceDao.findById(dli.getId());
        assertSame(dli, dli2);
        assertEquals(dli.getId(), dli2.getId());
        assertEquals(dli.getNode().getId(), dli2.getNode().getId());
        assertEquals(dli.getIfIndex(), dli2.getIfIndex());
        assertEquals(dli.getNodeParentId(), dli2.getNodeParentId());
        assertEquals(dli.getParentIfIndex(), dli2.getParentIfIndex());
        assertEquals(dli.getStatus(), dli2.getStatus());
        assertEquals(dli.getLinkTypeId(), dli2.getLinkTypeId());
        assertEquals(dli.getLastPollTime(), dli2.getLastPollTime());
    }
}
