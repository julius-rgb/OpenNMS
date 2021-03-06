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

package org.opennms.web.rest;

import java.text.ParseException;
import java.util.Date;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.netmgt.dao.EventDao;
import org.opennms.netmgt.model.OnmsEvent;
import org.opennms.netmgt.model.OnmsEventCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sun.jersey.spi.resource.PerRequest;

@Component
/**
 * <p>EventRestService class.</p>
 *
 * @author ranger
 * @version $Id: $
 * @since 1.8.1
 */
@PerRequest
@Scope("prototype")
@Path("events")
public class EventRestService extends OnmsRestService {

	@Autowired
	private EventDao m_eventDao;

	@Context
	UriInfo m_uriInfo;

	@Context
	HttpHeaders m_headers;

	@Context
	SecurityContext m_securityContext;

	/**
	 * <p>getEvent</p>
	 *
	 * @param eventId a {@link java.lang.String} object.
	 * @return a {@link org.opennms.netmgt.model.OnmsEvent} object.
	 */
	@GET
	@Produces( { MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	@Path("{eventId}")
	@Transactional
	public OnmsEvent getEvent(@PathParam("eventId") final String eventId) {
	    readLock();
	    try {
	        return m_eventDao.get(new Integer(eventId));
	    } finally {
	        readUnlock();
	    }
	}

	/**
	 * returns a plaintext string being the number of events
	 *
	 * @return a {@link java.lang.String} object.
	 */
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("count")
	@Transactional
	public String getCount() {
	    readLock();
	    try {
	        return Integer.toString(m_eventDao.countAll());
	    } finally {
	        readUnlock();
	    }
	}

	/**
	 * Returns all the events which match the filter/query in the query parameters
	 *
	 * @return Collection of OnmsEvents (ready to be XML-ified)
	 * @throws java.text.ParseException if any.
	 */
	@GET
	@Produces( { MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	@Transactional
	public OnmsEventCollection getEvents() throws ParseException {
	    readLock();
	    
	    try {
    	    final CriteriaBuilder builder = new CriteriaBuilder(OnmsEvent.class);
    	    applyQueryFilters(m_uriInfo.getQueryParameters(), builder);
    
    	    final OnmsEventCollection coll = new OnmsEventCollection(m_eventDao.findMatching(builder.toCriteria()));
    		coll.setTotalCount(m_eventDao.countMatching(builder.clearOrder().toCriteria()));
    		
    		return coll;
	    } finally {
	        readUnlock();
	    }
	}

	/**
	 * Updates the event with id "eventid"
	 * If the "ack" parameter is "true", then acks the events as the current logged in user, otherwise unacks the events
	 *
	 * @param eventId a {@link java.lang.String} object.
	 * @param ack a {@link java.lang.Boolean} object.
	 */
	@PUT
	@Path("{eventId}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Transactional
	public void updateEvent(@PathParam("eventId") final String eventId, @FormParam("ack") final Boolean ack) {
	    writeLock();
	    
	    try {
    	    final OnmsEvent event = m_eventDao.get(new Integer(eventId));
    		if (ack == null) {
    			throw new IllegalArgumentException("Must supply the 'ack' parameter, set to either 'true' or 'false'");
    		}
    		processEventAck(event, ack);
	    } finally {
	        writeUnlock();
	    }
	}

	/**
	 * Updates all the events that match any filter/query supplied in the form.
	 * If the "ack" parameter is "true", then acks the events as the current logged in user, otherwise unacks the events
	 *
	 * @param formProperties Map of the parameters passed in by form encoding
	 */
	@PUT
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Transactional
	public void updateEvents(final MultivaluedMapImpl formProperties) {
	    writeLock();
	    
	    try {
    		Boolean ack=false;
    		if(formProperties.containsKey("ack")) {
    			ack="true".equals(formProperties.getFirst("ack"));
    			formProperties.remove("ack");
    		}
    
    		final CriteriaBuilder builder = new CriteriaBuilder(OnmsEvent.class);
    		applyQueryFilters(formProperties, builder);
    		builder.orderBy("eventTime").desc();
    
    		for (final OnmsEvent event : m_eventDao.findMatching(builder.toCriteria())) {
    			processEventAck(event, ack);
    		}
	    } finally {
	        writeUnlock();
	    }
	}

	private void processEventAck(final OnmsEvent event, final Boolean ack) {
		if (ack) {
			event.setEventAckTime(new Date());
			event.setEventAckUser(m_securityContext.getUserPrincipal().getName());
		} else {
			event.setEventAckTime(null);
			event.setEventAckUser(null);
		}
		m_eventDao.save(event);
	}
}
