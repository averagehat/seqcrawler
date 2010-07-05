<%@ page language="java" pageEncoding="ISO-8859-1" %>
<%@ page import="java.io.*" %>

<%
if(request.getParameter("content-type")!=null)  { 
	response.setContentType(request.getParameter("content-type")); 
} 
else { 
	response.setContentType("text/plain"); 
} 

String AUTHDIR ="/db";
int buffSize = 100;

String fileName = request.getParameter("file");

if(fileName==null) {
	 response.sendError(404);
	 return; 
}

String position = request.getParameter("position");
if(position==null) {
	 response.sendError(500);
	 return; 
}
String[] data = position.split("-");

if(fileName==null || data.length!=2) {
	 response.sendError(500);
	 return; 
}


String startParam = data[0];
String sizeParam = data[1];


long start =-1;
if(startParam!=null) { start = Long.parseLong(startParam); }
long size =-1;
if(sizeParam!=null) { size = Long.parseLong(sizeParam); }


if(fileName.startsWith(AUTHDIR)) {
File f = new File(fileName);
RandomAccessFile raf = new RandomAccessFile(f,"r");
if(start>-1) {
	raf.seek(start);
}

byte[] buffer = new byte[(int)size];
int nbbytes = raf.read(buffer);
response.getOutputStream().write(buffer);

response.getOutputStream().flush();
raf.close();
}
else {
	response.sendError(500);
	return;
}

%>

