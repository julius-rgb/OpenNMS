<%--
/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2006-2012 The OpenNMS Group, Inc.
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

--%>

<%@page language="java"
	contentType="text/html"
	session="true"
	isErrorPage="true"
	import="org.opennms.web.notification.*"
%>

<%
    NoticeIdNotFoundException ninfe = null;
    
    if( exception instanceof NoticeIdNotFoundException ) {
        ninfe = (NoticeIdNotFoundException)exception;
    }
    else if( exception instanceof ServletException ) {
        ninfe = (NoticeIdNotFoundException)((ServletException)exception).getRootCause();
    }
    else {
        throw new ServletException( "This error page does not handle this exception type.", exception );
    }
%>

<jsp:include page="/includes/header.jsp" flush="false" >
  <jsp:param name="title" value="Error" />
  <jsp:param name="headTitle" value="Notice ID Not Found" />
  <jsp:param name="headTitle" value="Error" />
  <jsp:param name="breadcrumb" value="Error" />
</jsp:include>

<h1>Notice Id Not Found</h1>

<p>
  The notice id <%=ninfe.getBadID()%> is invalid. <%=ninfe.getMessage()%>
  <br/>
  You can re-enter it here or
  <a href="notification/browse?acktype=unack">browse all of the notices</a>
  to find the notice you are looking for.
</p>

<form method="get" action="notice/detail.jsp">
  <p>
    Get&nbsp;details&nbsp;for&nbsp;Notice&nbsp;ID:
    <br/>
    <input type="text" name="id"/>
  </p>
</form>

<jsp:include page="/includes/footer.jsp" flush="false" />
