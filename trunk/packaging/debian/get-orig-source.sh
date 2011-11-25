#!/bin/bash

set -e


#export VERSION="0.4"
VERSION=`dpkg-parsechangelog | awk '/^Version/ { print $2 }' | cut -f1 -d'-'`

export INDEXVERSION="0.5"
export SEARCHVERSION="0.4"

export PKG="seqcrawler-"$VERSION
mkdir $PKG

#export SOLRVERSION="1.4.1"
export SOLRVERSION="3.3.0"

mkdir -p $PKG/usr/share/seqcrawler/mongo
mkdir -p $PKG/usr/lib/cgi-bin/mongo
mkdir -p $PKG/var/lib/seqcrawler/index/solr
mkdir -p $PKG/var/lib/seqcrawler/downloads
mkdir -p $PKG/var/lib/seqcrawler/example
mkdir -p $PKG/var/lib/seqcrawler/index/mongo
mkdir -p $PKG/usr/share/java/webapps/solr
mkdir -p $PKG/usr/share/java/webapps/CrawlerSearchWebApp
mkdir -p $PKG/var/log/seqcrawler
mkdir -p $PKG/usr/share/seqcrawler/solr
mkdir -p $PKG/etc/seqcrawler/solr


mkdir -p $PKG/etc/apache2/conf.d/
echo "ProxyPass           /solr  http://localhost:8080/solr" > $PKG/etc/apache2/conf.d/seqcrawler.conf
echo "ProxyPassReverse /solr  http://localhost:8080/solr" >> $PKG/etc/apache2/conf.d/seqcrawler.conf

echo "ProxyPass      /seqcrawler  http://localhost:8080/seqcrawler" >> $PKG/etc/apache2/conf.d/seqcrawler.conf
echo "ProxyPassReverse /seqcrawler  http://localhost:8080/seqcrawler" >> $PKG/etc/apache2/conf.d/seqcrawler.conf

echo "Alias        \"/mongo\"    \"/usr/share/seqcrawler/mongo\"" >> $PKG/etc/apache2/conf.d/seqcrawler.conf

echo "Alias        \"/seqcrawler-downloads\"    \"/var/lib/seqcrawler/downloads\"" >> $PKG/etc/apache2/conf.d/seqcrawler.conf

echo "<Directory \"/var/lib/seqcrawler/downloads\">" >> $PKG/etc/apache2/conf.d/seqcrawler.conf
echo "AllowOverride Options" >> $PKG/etc/apache2/conf.d/seqcrawler.conf
echo "Options -Indexes -MultiViews +FollowSymLinks" >> $PKG/etc/apache2/conf.d/seqcrawler.conf
echo "Order allow,deny" >> $PKG/etc/apache2/conf.d/seqcrawler.conf
echo "Allow from all" >> $PKG/etc/apache2/conf.d/seqcrawler.conf
echo "</Directory>" >> $PKG/etc/apache2/conf.d/seqcrawler.conf

echo 

#Get Solr
echo "Deploying solr"
wget http://maven.irisa.fr/artifactory/genouest-public-release/apache-solr/apache-solr/$SOLRVERSION/apache-solr-$SOLRVERSION.tgz

mv apache-solr-$SOLRVERSION.tgz $PKG/usr/share/seqcrawler/solr/
#unzip $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION.zip -d $PKG/usr/share/seqcrawler/solr/
tar xvfz $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION.tgz -C $PKG/usr/share/seqcrawler/solr/
rm -rf $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION/example
rm -rf $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION/client
rm -rf $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION/docs
rm $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION.tgz
mv $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION/dist/apache-solr-$SOLRVERSION.war $PKG/usr/share/java/webapps/solr.war
mv $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION $PKG/usr/share/seqcrawler/solr/apache-solr
unzip $PKG/usr/share/java/webapps/solr.war -d $PKG/usr/share/java/webapps/solr/
rm $PKG/usr/share/java/webapps/solr.war

mkdir -p $PKG/usr/share/seqcrawler/solr/apache-solr/seqcrawler/

svn export https://seqcrawler.svn.sourceforge.net/svnroot/seqcrawler/trunk/index/CrawlerIndex/solr  solr
mv solr  $PKG/usr/share/seqcrawler/solr/apache-solr/seqcrawler/
rm -rf $PKG/usr/share/seqcrawler/solr/apache-solr/seqcrawler/solr/dataset
rm -rf $PKG/usr/share/seqcrawler/solr/apache-solr/seqcrawler/solr/data
rm -rf $PKG/usr/share/seqcrawler/solr/apache-solr/seqcrawler/solr/web
rm -f  $PKG/usr/share/seqcrawler/solr/apache-solr/seqcrawler/solr/apache-solr*.war


sed -i 's/<web-app>/<web-app>\n<env-entry><env-entry-name>solr\/home<\/env-entry-name><env-entry-value>\/usr\/share\/seqcrawler\/solr\/apache-solr\/seqcrawler\/solr<\/env-entry-value><env-entry-type>java.lang.String<\/env-entry-type><\/env-entry>/' $PKG/usr/share/java/webapps/solr/WEB-INF/web.xml


#Perl module
echo "Installing Solr-GBrowse2 perl module"
svn export https://seqcrawler.svn.sourceforge.net/svnroot/seqcrawler/trunk/GBrowse2

mv GBrowse2 $PKG/usr/share/seqcrawler/

# Solr indexer
wget http://maven.irisa.fr/artifactory/genouest-public-release/org/irisa/genouest/seqcrawler/CrawlerIndex/$INDEXVERSION/CrawlerIndex-$INDEXVERSION-jar-with-dependencies.jar
mv CrawlerIndex-$INDEXVERSION-jar-with-dependencies.jar $PKG/usr/share/seqcrawler/solr/apache-solr/seqcrawler/solr/bin/


# scripts
echo "Installing scripts"
#svn export https://seqcrawler.svn.sourceforge.net/svnroot/seqcrawler/trunk/index/CrawlerIndex/solr/bin solrbin --force
rm $PKG/usr/share/seqcrawler/solr/apache-solr/seqcrawler/solr/bin/index.jsp
rm $PKG/usr/share/seqcrawler/solr/apache-solr/seqcrawler/solr/bin/seqcrawler.js
rm $PKG/usr/share/seqcrawler/solr/apache-solr/seqcrawler/solr/bin/env.sh
rm $PKG/usr/share/seqcrawler/solr/apache-solr/seqcrawler/solr/bin/seqcrawler.properties
echo "#!/bin/bash" > $PKG/usr/share/seqcrawler/solr/apache-solr/seqcrawler/solr/bin/env.sh
echo "export INDEXHOME=/usr/share/seqcrawler/solr/apache-solr/seqcrawler/solr/bin" >> $PKG/usr/share/seqcrawler/solr/apache-solr/seqcrawler/solr/bin/env.sh
echo "export INDEXVERSION="$INDEXVERSION >> $PKG/usr/share/seqcrawler/solr/apache-solr/seqcrawler/solr/bin/env.sh

chmod 755 $PKG/usr/share/seqcrawler/solr/apache-solr/seqcrawler/solr/bin/*.sh
chmod 755 $PKG/usr/share/seqcrawler/solr/apache-solr/seqcrawler/solr/bin/*.pl
#rm -rf solrbin

#svn export https://seqcrawler.svn.sourceforge.net/svnroot/seqcrawler/trunk/index/CrawlerIndex/solr/conf solrconf --force
#cp solrconf/schema.xml $PKG/usr/share/seqcrawler/solr/apache-solr/seqcrawler/solr/conf/
sed -i 's/<dataDir>${solr.data.dir:.\/solr\/data}<\/dataDir>/<dataDir>${solr.data.dir:\/var\/lib\/seqcrawler\/index\/solr}<\/dataDir>/g' $PKG/usr/share/seqcrawler/solr/apache-solr/seqcrawler/solr/conf/solrconfig.xml
mv $PKG/usr/share/seqcrawler/solr/apache-solr/seqcrawler/solr/conf/solrconfig.xml $PKG/etc/seqcrawler/solr/
mv $PKG/usr/share/seqcrawler/solr/apache-solr/seqcrawler/solr/conf/schema.xml $PKG/etc/seqcrawler/solr/
rm -f $PKG/usr/share/seqcrawler/solr/apache-solr/seqcrawler/solr/bin/seqcrawler.properties
#rm -rf solrconf

# Export webapp
echo "Installing search/export web app"
wget http://maven.irisa.fr/artifactory/genouest-public-release/org/irisa/genouest/seqcrawler/CrawlerSearchWebApp/$SEARCHVERSION/CrawlerSearchWebApp-$SEARCHVERSION.war

mv CrawlerSearchWebApp-$SEARCHVERSION.war $PKG/usr/share/java/webapps/CrawlerSearchWebApp.war
unzip $PKG/usr/share/java/webapps/CrawlerSearchWebApp.war -d $PKG/usr/share/java/webapps/CrawlerSearchWebApp
rm $PKG/usr/share/java/webapps/CrawlerSearchWebApp.war
#rm -f $PKG/usr/share/java/webapps/CrawlerSearchWebApp/WEB-INF/web.xml
rm -f $PKG/usr/share/java/webapps/CrawlerSearchWebApp/META-INF/context.xml
rm -f $PKG/usr/share/java/webapps/CrawlerSearchWebApp/WEB-INF/lib/servlet-api-2.5.jar
rm -f $PKG/usr/share/java/webapps/CrawlerSearchWebApp/seqcrawler-conf.js

# Solr Web data
#echo "Installing Solr web data"
#svn export https://seqcrawler.svn.sourceforge.net/svnroot/seqcrawler/trunk/index/CrawlerIndex/solr/web/ solrweb  --force
#cp -R solrweb/* $PKG/usr/share/java/webapps/solr
#rm -rf solrweb

#MongoDB web
echo "Installing MongoDb web pages"
svn export https://seqcrawler.svn.sourceforge.net/svnroot/seqcrawler/trunk/index/CrawlerIndex/mongodb/www/ mongoweb --force
cp -R mongoweb/* $PKG/usr/share/seqcrawler/mongo/
rm -rf mongoweb
svn export https://seqcrawler.svn.sourceforge.net/svnroot/seqcrawler/trunk/index/CrawlerIndex/mongodb/cgi-bin/ mongocgi --force
cp -R mongocgi/mongo/* $PKG/usr/lib/cgi-bin/mongo/
chmod 755 $PKG/usr/lib/cgi-bin/mongo/*.pl
rm -rf mongocgi

#Extract files for packaging
echo "Getting extra files for packaging"
svn export https://seqcrawler.svn.sourceforge.net/svnroot/seqcrawler/trunk/packaging/extra-files  extra-files --force
mv extra-files $PKG/

tar cvfz seqcrawler_$VERSION.orig.tar.gz $PKG
rm -rf $PKG
