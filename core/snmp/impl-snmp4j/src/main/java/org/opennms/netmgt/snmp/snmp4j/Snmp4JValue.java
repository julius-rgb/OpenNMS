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

package org.opennms.netmgt.snmp.snmp4j;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpUtils;
import org.opennms.netmgt.snmp.SnmpValue;
import org.snmp4j.smi.Counter32;
import org.snmp4j.smi.Counter64;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Opaque;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.UnsignedInteger32;
import org.snmp4j.smi.Variable;

class Snmp4JValue implements SnmpValue {
    Variable m_value;
    
    Snmp4JValue(final Variable value) {
        if (value == null) {
            throw new NullPointerException("value attribute cannot be null");
        }
        m_value = value;
    }
    
    Snmp4JValue(final int syntax, final byte[] bytes) {
        switch (syntax) {
        case SMIConstants.SYNTAX_INTEGER: {
            m_value = new Integer32(new BigInteger(bytes).intValue());
            break;
        }
        case SMIConstants.SYNTAX_COUNTER32: {
            m_value = new Counter32(new BigInteger(bytes).longValue());
            break;
        }
        case SMIConstants.SYNTAX_COUNTER64: {
            m_value = new Counter64(new BigInteger(bytes).longValue());
            break;
        }
        case SMIConstants.SYNTAX_TIMETICKS: {
            m_value = new TimeTicks(new BigInteger(bytes).longValue());
            break;
        }
        case SMIConstants.SYNTAX_UNSIGNED_INTEGER32: {
            m_value = new UnsignedInteger32(new BigInteger(bytes).longValue());
            break;
        }
        case SMIConstants.SYNTAX_IPADDRESS: {
            try {
                m_value = new IpAddress(InetAddress.getByAddress(bytes));
            } catch (final UnknownHostException e) {
                throw new IllegalArgumentException("unable to create InetAddress from bytes: "+e.getMessage());
            }
            break;
        }
        case SMIConstants.SYNTAX_OBJECT_IDENTIFIER: {
            m_value = new OID(new String(bytes));
            break;
        }
        case SMIConstants.SYNTAX_OCTET_STRING: {
            m_value = new OctetString(bytes);
            break;
        }
        case SMIConstants.SYNTAX_OPAQUE: {
            m_value = new Opaque(bytes);
            break;
        }
        case SMIConstants.SYNTAX_NULL: {
            m_value = new Null();
            break;
        }
        default:
            throw new IllegalArgumentException("invalid syntax "+syntax);
        }
        if (m_value == null) {
            throw new IllegalArgumentException("value object created from syntax " + syntax + " is null");
        }

    }
    
    public byte[] getBytes() {
        switch (m_value.getSyntax()) {
        case SMIConstants.SYNTAX_INTEGER:
        case SMIConstants.SYNTAX_COUNTER32:
        case SMIConstants.SYNTAX_COUNTER64:
        case SMIConstants.SYNTAX_TIMETICKS:
        case SMIConstants.SYNTAX_UNSIGNED_INTEGER32:
            return toBigInteger().toByteArray();
        case SMIConstants.SYNTAX_IPADDRESS:
            return toInetAddress().getAddress();
        case SMIConstants.SYNTAX_OBJECT_IDENTIFIER:
            return toSnmpObjId().toString().getBytes();
        case SMIConstants.SYNTAX_OCTET_STRING:
            return ((OctetString)m_value).getValue();
        case SMIConstants.SYNTAX_OPAQUE:
            return((Opaque)m_value).getValue();
        case SMIConstants.SYNTAX_NULL:
            return new byte[0];
        default:
            throw new IllegalArgumentException("cannot convert "+m_value+" to a byte array");
        }
    }

    public int getType() {
        return m_value.getSyntax();
    }
    
    public boolean isEndOfMib() {
        return m_value.getSyntax() == SMIConstants.EXCEPTION_END_OF_MIB_VIEW;
    }
    
    public boolean isNumeric() {
        switch (m_value.getSyntax()) {
        case SMIConstants.SYNTAX_INTEGER:
        case SMIConstants.SYNTAX_COUNTER32:
        case SMIConstants.SYNTAX_COUNTER64:
        case SMIConstants.SYNTAX_TIMETICKS:
        case SMIConstants.SYNTAX_UNSIGNED_INTEGER32:
            return true;
        default:
            return false;
        }
    }
    
    public int toInt() {
        switch (m_value.getSyntax()) {
        case SMIConstants.SYNTAX_COUNTER64:
            return (int)((Counter64)m_value).getValue();
        case SMIConstants.SYNTAX_INTEGER:
            return ((Integer32)m_value).getValue();
        case SMIConstants.SYNTAX_COUNTER32:
        case SMIConstants.SYNTAX_TIMETICKS:
        case SMIConstants.SYNTAX_UNSIGNED_INTEGER32:
            return (int)((UnsignedInteger32)m_value).getValue();
        default:
            return Integer.parseInt(m_value.toString());
        }
    }
    
    public long toLong() {
        switch (m_value.getSyntax()) {
        case SMIConstants.SYNTAX_COUNTER64:
            return ((Counter64)m_value).getValue();
        case SMIConstants.SYNTAX_INTEGER:
            return ((Integer32)m_value).getValue();
        case SMIConstants.SYNTAX_COUNTER32:
        case SMIConstants.SYNTAX_TIMETICKS:
        case SMIConstants.SYNTAX_UNSIGNED_INTEGER32:
            return ((UnsignedInteger32)m_value).getValue();
        case SMIConstants.SYNTAX_OCTET_STRING:
            return (convertStringToLong());
        default:
            return Long.parseLong(m_value.toString());
        }
    }

    private long convertStringToLong() {
        return Double.valueOf(m_value.toString()).longValue();
    }

    public String toDisplayString() {
        switch (m_value.getSyntax()) {
        case SMIConstants.SYNTAX_OBJECT_IDENTIFIER :
            return SnmpObjId.get(((OID)m_value).getValue()).toString();
        case SMIConstants.SYNTAX_TIMETICKS :
            return Long.toString(toLong());
        case SMIConstants.SYNTAX_OCTET_STRING :
            return toStringDottingCntrlChars(((OctetString)m_value).getValue());
        default :
            return m_value.toString();
        }
    }

    private String toStringDottingCntrlChars(final byte[] value) {
        final byte[] results = new byte[value.length];
        for (int i = 0; i < value.length; i++) {
            results[i] = Character.isISOControl((char)value[i]) ? (byte)'.' : value[i];
        }
        return new String(results);
    }

    public InetAddress toInetAddress() {
        switch (m_value.getSyntax()) {
            case SMIConstants.SYNTAX_IPADDRESS:
                return ((IpAddress)m_value).getInetAddress();
            default:
                throw new IllegalArgumentException("cannot convert "+m_value+" to an InetAddress"); 
        }
    }

    public String toHexString() {
        switch (m_value.getSyntax()) {
        case SMIConstants.SYNTAX_OCTET_STRING:
            return ((OctetString)m_value).toHexString().replaceAll(":", "");
        default:
                throw new IllegalArgumentException("cannot convert "+m_value+" to a HexString");
        }
    }
        
    public String toString() {
        return toDisplayString();
    }

    public BigInteger toBigInteger() {
        switch (m_value.getSyntax()) {
        case SMIConstants.SYNTAX_COUNTER64:
            final Counter64 cnt = (Counter64)m_value;
            if (cnt.getValue() > 0) {
                return BigInteger.valueOf(cnt.getValue());
            } else {
                return new BigInteger(cnt.toString());
            }
        case SMIConstants.SYNTAX_INTEGER:
            return BigInteger.valueOf(((Integer32)m_value).getValue());
        case SMIConstants.SYNTAX_COUNTER32:
        case SMIConstants.SYNTAX_TIMETICKS:
        case SMIConstants.SYNTAX_UNSIGNED_INTEGER32:
            return BigInteger.valueOf(((UnsignedInteger32)m_value).getValue());
        default:
            return new BigInteger(m_value.toString());
        }
    }

    public SnmpObjId toSnmpObjId() {
        switch (m_value.getSyntax()) {
        case SMIConstants.SYNTAX_OBJECT_IDENTIFIER:
            return SnmpObjId.get(((OID)m_value).getValue());
        default:
                throw new IllegalArgumentException("cannot convert "+m_value+" to an SnmpObjId");
        }
    }
    
    public boolean isDisplayable() {
        if (isNumeric()) {
            return true;
        }
        
        if (getType() == SnmpValue.SNMP_OBJECT_IDENTIFIER || getType() == SnmpValue.SNMP_IPADDRESS) {
            return true;
        }
        
        if (getType() == SnmpValue.SNMP_OCTET_STRING) {
            return SnmpUtils.allBytesDisplayable(getBytes());
        }
        
        return false;
    }

    public boolean isNull() {
        return getType() == SnmpValue.SNMP_NULL;
    }

    public Variable getVariable() {
        return m_value;
    }

    public boolean isError() {
        switch (getType()) {
        case SnmpValue.SNMP_NO_SUCH_INSTANCE:
        case SnmpValue.SNMP_NO_SUCH_OBJECT:
            return true;
        default:
            return false;
        }
        
    }

    @Override
    public int hashCode() {
        if (m_value == null) return 5231;
    	return m_value.hashCode();
    }
    
    @Override
    public boolean equals(final Object obj) {
    	   if (obj == null) return false;
    	   if (obj == this) return true;
    	   if (obj.getClass() != getClass()) return false;

    	   final Snmp4JValue that = (Snmp4JValue)obj;
    	   return new EqualsBuilder()
    	   	.append(this.m_value, that.m_value)
    	   	.isEquals();
    }
}