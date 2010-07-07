This solr directory is solr configuration to use for seqcrawler.

**********************************
Updates in configuration to be done:

conf/solrconfig.xml:

If index shards is supported, the following lines must be uncommented and updated:
<str name="shards">shardserver1/solr/,shardserver2/solr/,...</str>

Must be updated to index data location. data dir must contain index and spellchecker subdirectories.
<dataDir>${solr.data.dir:./solr/data}</dataDir>

Solritas config: (request handlers /itas /mobile /mobiledetails)
update storageHost,gbHost hosts and ports where Gbrowse and Storage backend are installed


Apache configuration to restrict write operations from behind network for storage host/virtual host
<LIMIT PUT DELETE>
order deny,allow
deny from all
allow from 192.168.1.*  // ?syntaxe allow from internal network
</LIMIT>



************************************
Files installation

Put solr.war in a Tomcat container. Copy lib directory in WEB-INF/lib directory of deployed web app.
Update WEB-INF/web.xml to point to correct solr home (var entryt solr/home) e.g. current deployed dir.

Put bin/index.jsp in deployed webapp
Copy jquery-1.2.3.min.js and main.css under webapp /itas/

Access to localhost:8080/solr
Admin access: localhost:8080/solr/admin
Admin access should be securized in Tomcat server.

Web interface for query:
localhost:8080/solr/itas?q=aa


Copy solr/bin/lookup.jsp in webapp dir. Edit file to update:
String AUTHDIR ="/";
AUTHDIR is a directory filter to restrict file access to a defined sub directory structure

**************************************
Usages:

crawler.sh is a java program to index files. It can index single file or whole directory.
GFF type is preferred when possible (files should be transformed first).
Call crawler.sh -h for usage

merge.sh is a script to merge multiple indexes.
Call merge.sh -h for usage


***************************************
Apache Frontend:
it is advised to uyse apache as a front end to solr tomcat webapp and storage backend to hdie port and restrict access to PUT and DELETE operations (security constraints)
Apache can load balance on Solr masters.

To limit operations, place the following in a Directory section. See paache conf for user authentification
<Limit POST PUT DELETE>
      Require valid-user
</Limit>

For proxy, specify a virtualhost with proxy to correct hosts/ports:

<VirtualHost *:80>
    ProxyPass           /solr  http://192.168.1.237:8080/solr
    ProxyPassReverse /solr  http://192.168.1.237:8080/solr

    ProxyPass           /riak  http://192.168.1.237:8098/riak
    ProxyPassReverse /riak  http://192.168.1.237:8098/riak


</VirtualHost>

For load balancing:
<Proxy balancer://mysolrcluster>
BalancerMember http://192.168.1.50:8080
BalancerMember http://192.168.1.51:8080
</Proxy>
ProxyPass /solr balancer://mysolrcluster 

<Proxy balancer://myriakcluster>
BalancerMember http://192.168.1.50:8098
BalancerMember http://192.168.1.51:8098
</Proxy>
ProxyPass /riak balancer://myriakcluster 

Optionally to manage the cluster
 <Location /balancer-manager>
               SetHandler balancer-manager
               Order Deny,Allow
               Deny from all
               Allow from aHostNameToAllowManager
 </Location>





