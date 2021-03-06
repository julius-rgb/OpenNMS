/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2010-2012 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.tools.spectrum;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.List;

import org.opennms.core.utils.LogUtils;
import org.springframework.core.io.Resource;

public class EventDispositionReader {
    private Resource m_resource;
    private Reader m_reader;
    private StreamTokenizer m_tokenizer;
    
    private static final String eventCodeExpr = "^0[Xx][0-9A-Fa-f]{1,8}$";
    private static final String logEventToken = "E";
    private static final String createAlarmToken = "A";
    private static final String uniqueAlarmToken = "U";
    private static final String notUserClearableToken = "N";
    private static final String notPersistentToken = "T";
    private static final String clearAlarmToken = "C";
    
    /**
     *  E  A ,,U,N,T 
     * 0x180000    E 50              A 2,               0x180000,     N
     * 
     * This entry states that when an event of type 0x180000 occurs, it will be logged (E) with a severity of 50.
     * Also it will generate an alarm (A) of severity Major (2) and of alarm type 0x180000. The alarm is not user
     * clearable (N).
     * 
     * The E can be present or absent. If absent the event is not logged to the Archive Manager and will not be
     * observed in any client view unless the client is displaying the event history when the event occurs. The
     * event severity is not currently used but is logged with the event. If the E is absent the severity should
     * also be absent.
     * 
     * The A indicates that when this event occurs an alarm should be generated. Following the A are the alarm
     * severity, alarm cause (type) and three qualifiers that can appear in any combination. The alarm type does
     * not need to have the same value as the type of the event that generates it, however making them equal
     * when possible enhances readability. The possible qualifiers are:
     *
     * U - A unique alarm should be generated for each occurrence of this event (otherwise no new alarm is
     *     generated for subsequent occurrences, instead the existing alarm is noted)
     * N - The alarm is not user clearable
     * T - the alarm is not persistent (it will not be maintained through a SpectroSERVER restart)
     *  
     *  In place of the A there can be a C. The C would be followed by an alarm cause value and indicates that
     *  when the event occurs it should clear an alarm of the type indicated if one exists on the model.
     *  
     *  In place of the U one or more event variables can be listed as event discriminators:
     *  E A ,,,N,T
     *  
     *  When discriminators are specified then they are considered together with the alarm cause in determining
     *  the effective type of the alarm. Consider the following:
     *  
     *  0x180000 E 50 A 2,0x180000, 2,3,N
     *  0x180001 E 50 C   0x180000, 2,3
     *  
     *  This states that event 0x180000 should generate an alarm of type 0x180000 and that the values for event
     *  variables 2 and 3 should be saved with the alarm. If event 0x180000 occurs again and there is already
     *  an alarm present, but the new values of event variables 2 and 3 are not both equal to those stored with
     *  the alarm, then a new alarm will be generated, even though the unique indicator is not specified. The
     *  new alarm is considered to be of a different type since the values for the discriminators are different.
     *  Similarly if event 0x180001 occurs, it will only clear an alarm on this model of type 0x180000 if that
     *  alarm was generated with the same values of event variables 2 and 3 that were stored when the alarm was
     *  created. The specification of the discriminators with the clear event is required.
     */
    
    private enum TokenType {
        none,
        eventCode,
        logEvent,
        eventSeverity,
        createAlarm,
        clearAlarm,
        alarmSeverity,
        alarmSeverityComma,
        alarmCause,
        alarmCauseComma,
        uniqueAlarm,
        notUserClearable,
        notPersistent
    }
    
    public EventDispositionReader(Resource rsrc) throws IOException {
        m_resource = rsrc;
        m_reader = new BufferedReader(new InputStreamReader(m_resource.getInputStream()));
        m_tokenizer = new StreamTokenizer(m_reader);
        m_tokenizer.resetSyntax();
        m_tokenizer.commentChar('#');
        m_tokenizer.eolIsSignificant(true);
        m_tokenizer.whitespaceChars('\n', '\n');
        m_tokenizer.whitespaceChars('\r', '\r');
        m_tokenizer.whitespaceChars(' ', ' ');
        m_tokenizer.whitespaceChars('\t', '\t');
        m_tokenizer.wordChars('0', '9');
        m_tokenizer.wordChars('a', 'z');
        m_tokenizer.wordChars('A', 'Z');
    }
    
    public List<EventDisposition> getEventDispositions() throws IOException {
        List<EventDisposition> eventDispositions = new ArrayList<EventDisposition>();
        EventDisposition thisEventDisposition = null;
        TokenType lastToken = TokenType.none;
        
        boolean pastAlarmCause = false;
        boolean justHitEol = true;
        
        while (m_tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
            //System.err.println(m_tokenizer);
            
            if (justHitEol && m_tokenizer.ttype == StreamTokenizer.TT_WORD && m_tokenizer.sval.matches(eventCodeExpr)) {
                LogUtils.debugf(this, "Found an event code %s on line %d, creating a new event-disposition", m_tokenizer.sval, m_tokenizer.lineno());
                thisEventDisposition = new EventDisposition(m_tokenizer.sval);
                lastToken = TokenType.eventCode;
                pastAlarmCause = false;
                justHitEol = false;
            }

            if (m_tokenizer.ttype == StreamTokenizer.TT_EOL) {
                LogUtils.tracef(this, "Hit EOL on line %d", m_tokenizer.lineno());
                if (thisEventDisposition != null) {
                    LogUtils.tracef(this, "At EOL on line %d, and the working event-disposition is non-null, adding it to the completed pile", m_tokenizer.lineno());
                    LogUtils.tracef(this, "%s", thisEventDisposition.toString());
                    eventDispositions.add(thisEventDisposition);
                } else {
                    LogUtils.tracef(this, "At EOL on line %d, but the working event-disposition is null so not adding it", m_tokenizer.lineno());
                }
                justHitEol = true;
            }
            
            if (m_tokenizer.ttype == StreamTokenizer.TT_WORD && m_tokenizer.sval.equals(logEventToken)) {
                if (lastToken != TokenType.eventCode) {
                    LogUtils.errorf(this, "An event-log token [%s] must follow an event code directly, but the one on line %d of %s follows a %s token", logEventToken, m_tokenizer.lineno(), m_resource, lastToken.name());
                    throw new IllegalArgumentException("Found a log-event token [" + logEventToken + "] in an unexpected place on line " + m_tokenizer.lineno());
                }
                LogUtils.debugf(this, "Found a log-event token [%s] on line %d, setting log-event true", m_tokenizer.sval, m_tokenizer.lineno());
                thisEventDisposition.setLogEvent(true);
                lastToken = TokenType.logEvent;
            }
            
            if (lastToken == TokenType.logEvent) {
                if (m_tokenizer.nextToken() == StreamTokenizer.TT_WORD && m_tokenizer.sval.matches("^\\d+$")) {
                    LogUtils.tracef(this, "Found event severity %s for event-code %s on line %d", m_tokenizer.sval, thisEventDisposition.getEventCode(), m_tokenizer.lineno());
                    thisEventDisposition.setEventSeverity(Integer.valueOf(m_tokenizer.sval));
                    lastToken = TokenType.eventSeverity;
                } else {
                    LogUtils.errorf(this, "Found a token [%s] following an event-severity token [%s] on line %d of %s that does not appear to be an event severity", m_tokenizer.sval, logEventToken, m_tokenizer.lineno(), m_resource);
                    throw new IllegalArgumentException("Found an out-of-place token [" + m_tokenizer.sval + "] on line " + m_tokenizer.lineno());
                }
            }
            
            if (m_tokenizer.ttype == StreamTokenizer.TT_WORD && (m_tokenizer.sval.equals(createAlarmToken) || m_tokenizer.sval.equals(clearAlarmToken))) {
                LogUtils.tracef(this, "Found a create-alarm or clear-alarm token [%s] on line %d, checking that it's not out of order", m_tokenizer.sval, m_tokenizer.lineno());
                if (lastToken != TokenType.eventCode && lastToken != TokenType.eventSeverity) {
                    LogUtils.errorf(this, "Found a token [%s] NOT following an event-code [0xNNN...] or event-severity [e.g. 20] token on line %d of %s that does not appear to be an event severity", m_tokenizer.sval, m_tokenizer.lineno(), m_resource);
                    throw new IllegalArgumentException("Found an out-of-place token [" + m_tokenizer.sval + "] on line "+ m_tokenizer.lineno());
                }
            }
            
            if (m_tokenizer.ttype == StreamTokenizer.TT_WORD && m_tokenizer.sval.equals(createAlarmToken)) {
                LogUtils.debugf(this, "Found a create-alarm token [%s] on line %d", m_tokenizer.sval, m_tokenizer.lineno());
                LogUtils.tracef(this, "Found a create-alarm token [%s] on line %d, peeking ahead for the alarm severity", m_tokenizer.sval, m_tokenizer.lineno());
                if (m_tokenizer.nextToken() != StreamTokenizer.TT_WORD || ! m_tokenizer.sval.matches("^[01234]$")) {
                    LogUtils.errorf(this, "Found a create-alarm token [%s] on line %d of %s that's followed by an unexpected token [%s] instead of an alarm severity in range 0-4", createAlarmToken, m_tokenizer.lineno(), m_resource, m_tokenizer.sval);
                    throw new IllegalArgumentException("Found a create-alarm token [" + createAlarmToken + "] on line [" + m_tokenizer.lineno() + "] followed by unexpected token [" + m_tokenizer.sval + "] instead of an alarm severity in range 0-4");
                } else if (m_tokenizer.ttype == StreamTokenizer.TT_WORD && m_tokenizer.sval.matches("^[01234]$")) {
                    LogUtils.debugf(this, "Found alarm-severity token [%s] on line %d, using it to set alarm severity", m_tokenizer.sval, m_tokenizer.lineno());
                    thisEventDisposition.setCreateAlarm(true);
                    thisEventDisposition.setAlarmSeverity(Integer.valueOf(m_tokenizer.sval));
                    lastToken = TokenType.alarmSeverity;
                }
                
                if (m_tokenizer.nextToken() != ',') {
                    LogUtils.errorf(this, "The alarm-severity value [%d] on line %d of %s is not followed by a comma", thisEventDisposition.getAlarmSeverity(), m_tokenizer.lineno(), m_resource);
                    throw new IllegalArgumentException("Alarm-severity [" + thisEventDisposition.getAlarmSeverity() + "] not followed by a comma on line "  + m_tokenizer.lineno());
                }
                lastToken = TokenType.alarmSeverityComma;
                continue;
            }
            
            if (m_tokenizer.ttype == StreamTokenizer.TT_WORD && m_tokenizer.sval.equals(clearAlarmToken)) {
                LogUtils.debugf(this, "Found a clear-alarm token [%s] on line %d, setting clearAlarm to true", m_tokenizer.sval, m_tokenizer.lineno());
                thisEventDisposition.setClearAlarm(true);
                lastToken = TokenType.clearAlarm;
            }
            
            if (lastToken == TokenType.alarmSeverityComma || lastToken == TokenType.clearAlarm) {
                if (m_tokenizer.ttype != StreamTokenizer.TT_WORD) {
                    LogUtils.errorf(this, "Expecting an alarm-cause token [e.g. 0xNNN...] after a %s on line %d of %s but got a non-word token of type %d instead", lastToken.name(), m_tokenizer.lineno(), m_resource, m_tokenizer.ttype);
                    throw new IllegalArgumentException("Expected an alarm-cause [e.g. 0xNNN...] after the " + lastToken.name() + " on line " + m_tokenizer.lineno() + " but got a non-word token instead");                    
                } else if (m_tokenizer.sval.matches(eventCodeExpr)) {
                    if (lastToken == TokenType.alarmSeverityComma) { 
                        LogUtils.debugf(this, "Found alarm-cause of [%s] on line %d, setting accordingly", m_tokenizer.sval, m_tokenizer.lineno());
                        thisEventDisposition.setAlarmCause(m_tokenizer.sval);
                    } else if (lastToken == TokenType.clearAlarm) {
                        LogUtils.debugf(this, "Found clear-alarm-cause of [%s] on line %d, setting accordingly", m_tokenizer.sval, m_tokenizer.lineno());
                        thisEventDisposition.setClearAlarmCause(m_tokenizer.sval);
                    }
                    lastToken = TokenType.alarmCause;
                }
            }
            
            if (lastToken == TokenType.alarmCause && m_tokenizer.ttype == ',') {
                lastToken = TokenType.alarmCauseComma;
                pastAlarmCause = true;
            }
            
            if (lastToken == TokenType.alarmCauseComma && m_tokenizer.ttype != StreamTokenizer.TT_WORD) {
                LogUtils.errorf(this, "Found an unexpected non-word token after the comma that follows alarm-cause or clear-alarm-cause [%s] on line %d of %s", thisEventDisposition.getAlarmCause(), m_tokenizer.lineno(), m_resource);
                throw new IllegalArgumentException("Unexpected token after the comma following alarm-cause or clear-alarm-cause [" + thisEventDisposition.getAlarmCause() + "] on line " + m_tokenizer.lineno());
            }
            
            if (pastAlarmCause && m_tokenizer.ttype == ',') {
                LogUtils.tracef(this, "Ignoring a comma in post-(clear)-alarm-cause section of disposition for event-code %s on line %d", thisEventDisposition.getEventCode(), m_tokenizer.lineno());
            }
            
            if (pastAlarmCause && m_tokenizer.ttype == StreamTokenizer.TT_WORD && m_tokenizer.sval.matches("^\\d+$")) {
                LogUtils.debugf(this, "Found a numeric token [%s] after the (clear)-alarm-cause on line %d, adding as a discriminator", m_tokenizer.sval, m_tokenizer.lineno());
                thisEventDisposition.addDiscriminator(Integer.valueOf(m_tokenizer.sval));
            }
            
            if (pastAlarmCause && m_tokenizer.ttype == StreamTokenizer.TT_WORD && m_tokenizer.sval.equals(uniqueAlarmToken)) {
                LogUtils.debugf(this, "Found a unique-alarm token [%s] after the (clear)-alarm-cause on line %d, setting unique-alarm to true", m_tokenizer.sval, m_tokenizer.lineno());
                thisEventDisposition.setUniqueAlarm(true);
            }

            if (pastAlarmCause && m_tokenizer.ttype == StreamTokenizer.TT_WORD && m_tokenizer.sval.equals(notUserClearableToken)) {
                LogUtils.debugf(this, "Found a not-user-clearable token [%s] after the (clear)-alarm-cause on line %d, setting user-clearable to false", m_tokenizer.sval, m_tokenizer.lineno());
                thisEventDisposition.setUserClearable(false);
            }

            if (pastAlarmCause && m_tokenizer.ttype == StreamTokenizer.TT_WORD && m_tokenizer.sval.equals(notPersistentToken)) {
                LogUtils.debugf(this, "Found a not-persistent token [%s] after the (clear)-alarm-cause on line %d, setting persistent to false", m_tokenizer.sval, m_tokenizer.lineno());
                thisEventDisposition.setPersistent(false);
            }

        }
        
        LogUtils.infof(this, "Loaded %d event-dispositions from [%s]", eventDispositions.size(), m_resource);
        return eventDispositions;
    }
    
}
