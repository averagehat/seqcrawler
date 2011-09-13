<%@ page language="java" pageEncoding="ISO-8859-1" %>
<%@ page import="java.io.*" %>

<%

// Fix 3175150 : accept multiple AUTHDIR

if(request.getParameter("content-type")!=null)  { 
	response.setContentType(request.getParameter("content-type")); 
	String[] type = request.getParameter("content-type").split("/");
	if(type==null || type.length!=2) {
		type = new String[] { "text", "txt"};
	}
    response.setHeader("Content-Disposition", "attachment;filename=result."+type[1]);
} 
else { 
	response.setContentType("text/plain"); 
} 

String[] AUTHDIR = { "/" };
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

if(fileName==null  || (data.length!=2 && !position.equals("undefined"))) {
	 response.sendError(500);
	 return; 
}
//Set filename
File fseq = new File(fileName);
if(!fseq.exists()) {
	 response.sendError(404);
	 return; 
}

String startParam = "0";
String sizeParam = "0";
if(position.equals("undefined")) {
	// Take whole file
	sizeParam = Long.toString(fseq.length());
}
else {
	startParam = data[0];
	sizeParam = data[1];
}


response.setHeader("Content-Disposition","attachment; filename=" + fseq.getName() );


long start =-1;
if(startParam!=null) { start = Long.parseLong(startParam); }
long size =-1;
if(sizeParam!=null) { size = Long.parseLong(sizeParam); }


File f = new File(fileName);
RandomAccessFile raf = new RandomAccessFile(f,"r");
boolean match=false;

for(int i=0;i<AUTHDIR.length;i++) {
if(fileName.startsWith(AUTHDIR[i])) {
if(start>-1) {
        raf.seek(start);
}
match=true;
break;
}

}


if(match) {
byte[] buffer = new byte[(int)size];
int nbbytes = raf.read(buffer);
OutputStream outs = response.getOutputStream();
outs.write(buffer);
outs.flush();
raf.close();
}
else {
	response.sendError(500);
	return;
}

%>

