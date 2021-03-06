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

package org.opennms.netmgt.snmp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.opennms.core.utils.ThreadCategory;

public class TableTracker extends CollectionTracker implements RowCallback, RowResultFactory {

    private final SnmpTableResult m_tableResult;

    private final List<ColumnTracker> m_columnTrackers;

    public TableTracker(SnmpObjId... ids) {
        this(null, ids);
    }
    
    public TableTracker(RowCallback rc, SnmpObjId... ids) {
        this(rc, 2, ids);
    }

    public TableTracker(RowCallback rc, int maxRepetitions, SnmpObjId... columns) {
        m_tableResult = new SnmpTableResult(rc == null ? this : rc, this, columns);

        m_columnTrackers = new ArrayList<ColumnTracker>(columns.length);
        for (SnmpObjId id : columns) {
            m_columnTrackers.add(new ColumnTracker(this, id, maxRepetitions));
        }
    }

    @Override
    public void setMaxRepetitions(int maxRepetitions) {
        for(ColumnTracker child : m_columnTrackers) {
            child.setMaxRepetitions(maxRepetitions);
        }
    }

    @Override
    public boolean isFinished() {
        if (super.isFinished()) {
            return true;
        }
        for (ColumnTracker ct : m_columnTrackers) {
            if (!ct.isFinished()) {
                return false;
            }
        }
        m_tableResult.tableFinished();
        setFinished(true);
        return true;
    }

    @Override
    public ResponseProcessor buildNextPdu(PduBuilder pduBuilder) {
        if (pduBuilder.getMaxVarsPerPdu() < 1) {
            throw new IllegalArgumentException("maxVarsPerPdu < 1");
        }

        List<ResponseProcessor> processors = new ArrayList<ResponseProcessor>(pduBuilder.getMaxVarsPerPdu());

        for (ColumnTracker ct : getNextColumnTrackers(pduBuilder.getMaxVarsPerPdu())) {
            processors.add(ct.buildNextPdu(pduBuilder));
        }

        return new CombinedColumnResponseProcessor(processors);
    }

    public void storeResult(SnmpResult res) {
        //System.err.println(String.format("storeResult: %s", res));
    	ThreadCategory.getInstance(SnmpResult.class).debug(String.format("storeResult: %s", res));
        m_tableResult.storeResult(res);
    }
    
    public void rowCompleted(SnmpRowResult row) {
        // the default implementation just forwards this to the super class
        // like the defaults for other CollectionTrackers except this does it
        // from the rowCompleted method rather than from storeResult
        for(SnmpResult result : row.getResults()) {
            super.storeResult(result);
        }
    }

    public SnmpRowResult createRowResult(int columnCount, SnmpInstId instance) {
        return m_tableResult.createRowResult(columnCount, instance);
    }

    private List<ColumnTracker> getNextColumnTrackers(int maxVarsPerPdu) {
        List<ColumnTracker> trackers = new ArrayList<ColumnTracker>(maxVarsPerPdu);
        List<ColumnTracker> sortedTrackerList = new ArrayList<ColumnTracker>(m_columnTrackers);

        Collections.sort(sortedTrackerList, new Comparator<ColumnTracker>() {
            public int compare(ColumnTracker o1, ColumnTracker o2) {
                return new CompareToBuilder()
                    .append(o1.getLastInstance(), o2.getLastInstance())
                    .toComparison();
            }
        });
        
        for(Iterator<ColumnTracker> it = sortedTrackerList.iterator(); it.hasNext() && trackers.size() < maxVarsPerPdu; ) {
        
            ColumnTracker tracker = it.next();
            
            if (!tracker.isFinished()) {
                trackers.add(tracker);
            }

        }

        return trackers;
    }

    static private class CombinedColumnResponseProcessor implements ResponseProcessor {
        private final List<ResponseProcessor> m_processors;
        private int m_currentIndex = 0;

        public CombinedColumnResponseProcessor(List<ResponseProcessor> processors) {
            m_processors = processors;
        }

        public void processResponse(SnmpObjId responseObjId, SnmpValue val) {
            try {
            ResponseProcessor rp = m_processors.get(m_currentIndex);
            
            if (++m_currentIndex == m_processors.size()) {
                m_currentIndex = 0;
            }

            rp.processResponse(responseObjId, val);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        public boolean processErrors(int errorStatus, int errorIndex) {
            
            /*
             * errorIndex is varBind index (1 based array of vars)
             * 
             * 
             */
            
            int columnIndex = (errorIndex - 1) % m_processors.size();
            
            ResponseProcessor rp = m_processors.get(columnIndex);

            return rp.processErrors(errorStatus, 1);
        }

    }

    
    

}
