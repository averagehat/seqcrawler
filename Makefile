TOMCAT_VERSION=8.0.26
#NOTE: extra-files in packaging has solr.xml, needed to start solr
install: WebApp
run: server
all: run 

readseq-2.1.11.jar: 
	wget http://maven.irisa.fr/artifactory/genouest-public-release/org/irisa/genouest/tools/readseq/2.1.11/readseq-2.1.11.jar

readseq: readseq-2.1.11.jar 
	mvn install:install-file -Dfile=readseq-2.1.11.jar -DgroupId=org.irisa.genouest.tools -Dversion=2.1.11 -DartifactId=readseq -Dpackaging=jar

CrawlerIndex: readseq
	cd index/CrawlerIndex
	mvn install

CrawlerSearch:
	cd index/CrawlerSearch
	mvn install

CrawlerSearchWebApp:
	cd index/CrawlerSearchWebApp
	mvn install

tomcat:  apache-tomcat-8.0.26

apache-tomcat-8.0.26:
	wget http://mirror.olnevhost.net/pub/apache/tomcat/tomcat-8/v8.0.26/bin/apache-tomcat-8.0.26.tar.gz
	tar -xzf apache-tomcat-8.0.26.tar.gz 
#    cp tomcat /etc/init.d/tomcat
#    chmod 755 /etc/init.d/tomcat

WebApp: tomcat CrawlerSearchWebApp
	ln -s index/CrawlerSearchWebApp/target/CrawlerSearchWebApp apache-tomcat-8.0.26/webapps/

server: WebApp tomcat
	apache-tomcat-8.0.26/webapps/bin/startup.sh
  

