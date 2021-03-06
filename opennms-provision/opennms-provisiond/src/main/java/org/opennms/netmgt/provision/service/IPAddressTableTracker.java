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

package org.opennms.netmgt.provision.service;

import static org.opennms.core.utils.InetAddressUtils.getInetAddress;
import static org.opennms.core.utils.InetAddressUtils.normalize;
import static org.opennms.core.utils.InetAddressUtils.str;

import java.net.InetAddress;

import org.opennms.core.utils.InetAddressUtils;
import org.opennms.core.utils.LogUtils;
import org.opennms.netmgt.model.OnmsIpInterface;
import org.opennms.netmgt.model.OnmsSnmpInterface;
import org.opennms.netmgt.snmp.RowCallback;
import org.opennms.netmgt.snmp.SnmpInstId;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpResult;
import org.opennms.netmgt.snmp.SnmpRowResult;
import org.opennms.netmgt.snmp.SnmpValue;
import org.opennms.netmgt.snmp.TableTracker;

/**
 * PhysInterfaceTableTracker
 *
 * @author brozow
 * @version $Id: $
 */
public class IPAddressTableTracker extends TableTracker {
    
	public static final SnmpObjId IP_ADDRESS_PREFIX_TABLE_ENTRY = SnmpObjId.get(".1.3.6.1.2.1.4.32.1");
    public static final SnmpObjId IP_ADDRESS_TABLE_ENTRY = SnmpObjId.get(".1.3.6.1.2.1.4.34.1");
    
    public static final SnmpObjId IP_ADDRESS_IF_INDEX = SnmpObjId.get(IP_ADDRESS_TABLE_ENTRY, "3");
    public static final SnmpObjId IP_ADDRESS_TYPE_INDEX = SnmpObjId.get(IP_ADDRESS_TABLE_ENTRY, "4");
    public static final SnmpObjId IP_ADDRESS_PREFIX_INDEX = SnmpObjId.get(IP_ADDRESS_TABLE_ENTRY, "5");
    public static final SnmpObjId IP_ADDRESS_PREFIX_ORIGIN_INDEX = SnmpObjId.get(IP_ADDRESS_PREFIX_TABLE_ENTRY, "5");
    public static final SnmpObjId IP_ADDRESS_ORIGIN_INDEX = SnmpObjId.get(IP_ADDRESS_TABLE_ENTRY, "6");
    public static final SnmpObjId IP_ADDRESS_STATUS_INDEX = SnmpObjId.get(IP_ADDRESS_TABLE_ENTRY, "7");
    public static final SnmpObjId IP_ADDRESS_CREATED_INDEX = SnmpObjId.get(IP_ADDRESS_TABLE_ENTRY, "8");
    public static final SnmpObjId IP_ADDRESS_LAST_CHANGED_INDEX = SnmpObjId.get(IP_ADDRESS_TABLE_ENTRY, "9");
    public static final SnmpObjId IP_ADDRESS_ROW_STATUS_INDEX = SnmpObjId.get(IP_ADDRESS_TABLE_ENTRY, "10");
    public static final SnmpObjId IP_ADDRESS_STORAGE_TYPE_INDEX = SnmpObjId.get(IP_ADDRESS_TABLE_ENTRY, "11");
    public static final int TYPE_IPV4  = 1;
    public static final int TYPE_IPV6  = 2;
    public static final int TYPE_IPV4Z = 3;
    public static final int TYPE_IPV6Z = 4;
    public static final int TYPE_DNS   = 16;

    private static final int IP_ADDRESS_TYPE_UNICAST = 1;
    // private static final int IP_ADDRESS_TYPE_ANYCAST = 2;
    // private static final int IP_ADDRESS_TYPE_BROADCAST = 3;

    private static SnmpObjId[] s_tableColumns = new SnmpObjId[] {
        IP_ADDRESS_IF_INDEX,
        IP_ADDRESS_PREFIX_INDEX,
        IP_ADDRESS_TYPE_INDEX
    };
    
    class IPAddressRow extends SnmpRowResult {

        public IPAddressRow(final int columnCount, final SnmpInstId instance) {
            super(columnCount, instance);
            LogUtils.debugf(this, "column count = %d, instance = %s", columnCount, instance);
        }
        
        public Integer getIfIndex() {
        	final SnmpValue value = getValue(IP_ADDRESS_IF_INDEX);
        	return value.toInt();
        }
        
        public String getIpAddress() {
            final SnmpResult result = getResult(IP_ADDRESS_IF_INDEX);
            SnmpInstId instance = result.getInstance();
            final int[] instanceIds = instance.getIds();

            final int addressType = instanceIds[0];
            int addressIndex = 2;
            int addressLength = instanceIds[1];
            // Begin NMS-4906 Lame Force 10 agent!
            if (addressType == TYPE_IPV4 && instanceIds.length != 6) {
                LogUtils.warnf(this, "BAD AGENT: Does not conform to RFC 4001 Section 4.1 Table Indexing!!! Report them immediately.  Making a best guess!");
                addressIndex = instanceIds.length - 4;
                addressLength = 4;
            }
            if (addressType == TYPE_IPV6 && instanceIds.length != 18) {
                LogUtils.warnf(this, "BAD AGENT: Does not conform to RFC 4001 Section 4.1 Table Indexing!!! Report them immediately.  Making a best guess!");
                addressIndex = instanceIds.length - 16;
                addressLength = 16;
            }
            // End NMS-4906 Lame Force 10 agent!

            if (addressIndex < 0 || addressIndex + addressLength > instanceIds.length) {
                LogUtils.warnf(this, "BAD AGENT: Returned instanceId %s does not enough bytes to contain address!. Skipping.", instance);
                return null;
            }

            if (addressType == TYPE_IPV4 || addressType == TYPE_IPV6 || addressType == TYPE_IPV6Z) {
                final InetAddress address = getInetAddress(instanceIds, addressIndex, addressLength);
                return str(address);
            }
            return null;
        }

        public Integer getType() {
            final SnmpValue value = getValue(IP_ADDRESS_TYPE_INDEX);
            return value.toInt();
        }

        private InetAddress getNetMask() {
            final SnmpValue value = getValue(IP_ADDRESS_PREFIX_INDEX);

            final SnmpInstId netmaskRef = value.toSnmpObjId().getInstance(IP_ADDRESS_PREFIX_ORIGIN_INDEX);

            // See bug NMS-5036
            // {@see http://issues.opennms.org/browse/NMS-5036}
            if (netmaskRef == null) {
                LogUtils.warnf(this, "BAD AGENT: Null netmask instanceId");
                return null;
            }

            final int[] rawIds = netmaskRef.getIds();
            final int addressType = rawIds[1];
            final int mask = rawIds[rawIds.length - 1];
            int addressLength = rawIds[2];
            int addressIndex = 3;

            // Begin NMS-4906 Lame Force 10 agent!
            if (addressType == TYPE_IPV4 && rawIds.length != 1+6+1) {
                LogUtils.warnf(this, "BAD AGENT: Does not conform to RFC 4001 Section 4.1 Table Indexing!!! Report them immediately.  Making a best guess!");
                addressIndex = rawIds.length - (4+1);
                addressLength = 4;
            }
            if (addressType == TYPE_IPV6 && rawIds.length != 1+18+1) {
                LogUtils.warnf(this, "BAD AGENT: Does not conform to RFC 4001 Section 4.1 Table Indexing!!! Report them immediately.  Making a best guess!");
                addressIndex = rawIds.length - (16 + 1);
                addressLength = 16;
            }
            // End NMS-4906 Lame Force 10 agent!

            if (addressIndex < 0 || addressIndex + addressLength > rawIds.length) {
                LogUtils.warnf(this, "BAD AGENT: Returned instanceId %s does not enough bytes to contain address!. Skipping.", netmaskRef);
                return null;
            }

            //final InetAddress address = getInetAddress(rawIds, addressIndex, addressLength);

            if (addressType == TYPE_IPV4) {
                return InetAddressUtils.convertCidrToInetAddressV4(mask);
            } else if (addressType == TYPE_IPV6) {
                return InetAddressUtils.convertCidrToInetAddressV6(mask);
            } else {
                LogUtils.warnf(this, "unknown address type, expected 1 (IPv4) or 2 (IPv6), but got %d", addressType);
                return null;
            }
        }

        public OnmsIpInterface createInterfaceFromRow() {

            final Integer ifIndex = getIfIndex();
            final String ipAddr = getIpAddress();
            final Integer type = getType();
            final InetAddress netMask = getNetMask();

            LogUtils.debugf(this, "createInterfaceFromRow: ifIndex = %s, ipAddress = %s, type = %s, netmask = %s", ifIndex, ipAddr, type, netMask);

            if (type != IP_ADDRESS_TYPE_UNICAST || ipAddr == null) {
                return null;
            }

            final OnmsSnmpInterface snmpIface = new OnmsSnmpInterface(null, ifIndex);
            snmpIface.setNetMask(netMask);
            snmpIface.setCollectionEnabled(true);

            final OnmsIpInterface iface = new OnmsIpInterface(ipAddr, null);
            iface.setSnmpInterface(snmpIface);

            iface.setIfIndex(ifIndex);
            final String hostName = normalize(ipAddr);
            LogUtils.debugf(this, "setIpHostName: %s", hostName);
            iface.setIpHostName(hostName == null? ipAddr : hostName);

            return iface;
        }

        private SnmpResult getResult(final SnmpObjId base) {
            for(final SnmpResult result : getResults()) {
                if (base.equals(result.getBase())) {
                    return result;
                }
            }
            
            return null;
        }

    }
    
    /**
     * <p>Constructor for IPInterfaceTableTracker.</p>
     */
    public IPAddressTableTracker() {
        super(s_tableColumns);
    }

    /**
     * <p>Constructor for IPInterfaceTableTracker.</p>
     *
     * @param rowProcessor a {@link org.opennms.netmgt.snmp.RowCallback} object.
     */
    public IPAddressTableTracker(final RowCallback rowProcessor) {
        super(rowProcessor, s_tableColumns);
    }
    
    /** {@inheritDoc} */
    @Override
    public SnmpRowResult createRowResult(final int columnCount, final SnmpInstId instance) {
        return new IPAddressRow(columnCount, instance);
    }

    /** {@inheritDoc} */
    @Override
    public void rowCompleted(final SnmpRowResult row) {
        processIPAddressRow((IPAddressRow)row);
    }

    /**
     * <p>processIPInterfaceRow</p>
     *
     * @param row a {@link org.opennms.netmgt.provision.service.IPAddressTableTracker.IPAddressRow} object.
     */
    public void processIPAddressRow(final IPAddressRow row) {
        
    }

}
