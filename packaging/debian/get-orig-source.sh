#!/bin/bash

set -e


export VERSION="1.1"

export INDEXVERSION="0.2"
export SEARCHVERSION="0.1"

export PKG="seqcrawler-"$VERSION
mkdir $PKG

export SOLRVERSION="1.4.1"

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


mkdir -p $PKG/etc/apache2/conf.d/
echo "ProxyPass           /solr  http://localhost:8080/solr" > $PKG/etc/apache2/conf.d/seqcrawler.conf
echo "ProxyPassReverse /solr  http://localhost:8080/solr" >> $PKG/etc/apache2/conf.d/seqcrawler.conf

echo "ProxyPass      /CrawlerSearchWebApp  http://localhost:8080/CrawlerSearchWebApp" >> $PKG/etc/apache2/conf.d/seqcrawler.conf
echo "ProxyPassReverse /CrawlerSearchWebApp  http://localhost:8080/CrawlerSearchWebApp" >> $PKG/etc/apache2/conf.d/seqcrawler.conf

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
wget http://maven.irisa.fr/artifactory/genouest-public-snapshot/apache-solr/apache-solr/$SOLRVERSION/apache-solr-$SOLRVERSION.zip

mv apache-solr-$SOLRVERSION.zip $PKG/usr/share/seqcrawler/solr/
unzip $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION.zip -d $PKG/usr/share/seqcrawler/solr/
rm $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION.zip
mv $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION/dist/apache-solr-$SOLRVERSION.war $PKG/usr/share/java/webapps/solr.war
unzip $PKG/usr/share/java/webapps/solr.war -d $PKG/usr/share/java/webapps/solr/
rm $PKG/usr/share/java/webapps/solr.war

mkdir -p $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION/seqcrawler/

svn export https://seqcrawler.svn.sourceforge.net/svnroot/seqcrawler/trunk/index/CrawlerIndex/solr  solr
mv solr  $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION/seqcrawler/
rm -rf $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION/seqcrawler/solr/dataset
rm -rf $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION/seqcrawler/solr/data
rm -rf $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION/seqcrawler/solr/web
rm -f  $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION/seqcrawler/solr/apache-solr*.war


sed -i 's/<web-app>/<web-app>\n<env-entry><env-entry-name>solr\/home<\/env-entry-name><env-entry-value>\/usr\/share\/seqcrawler\/solr\/apache-solr-1.4.1\/seqcrawler\/solr<\/env-entry-value><env-entry-type>java.lang.String<\/env-entry-type><\/env-entry>/' $PKG/usr/share/java/webapps/solr/WEB-INF/web.xml


#Perl module
echo "Installing Solr-GBrowse2 perl module"
svn export https://seqcrawler.svn.sourceforge.net/svnroot/seqcrawler/trunk/GBrowse2

mv GBrowse2 $PKG/usr/share/seqcrawler/

# Solr indexer
wget http://maven.irisa.fr/artifactory/genouest-public-snapshot/org/irisa/genouest/seqcrawler/CrawlerIndex/$INDEXVERSION/CrawlerIndex-$INDEXVERSION-jar-with-dependencies.jar
mv CrawlerIndex-$INDEXVERSION-jar-with-dependencies.jar $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION/seqcrawler/solr/bin/

#sed -i 's/\/opt\/solr\/apache-solr-1.4.1\/seqcrawler\/solr\/bin/\/usr\/share\/seqcrawler\/solr\/apache-solr-1.4.1\/seqcrawler\/solr\/bin/g' $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION/seqcrawler/solr/bin/*.sh

# scripts
echo "Installing scripts"
#svn export https://seqcrawler.svn.sourceforge.net/svnroot/seqcrawler/trunk/index/CrawlerIndex/solr/bin solrbin --force
rm $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION/seqcrawler/solr/bin/index.jsp
rm $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION/seqcrawler/solr/bin/seqcrawler.js
rm $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION/seqcrawler/solr/bin/env.sh
echo "export INDEXHOME=/usr/share/seqcrawler/solr/apache-solr-1.4.1/seqcrawler/solr/bin" > $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION/seqcrawler/solr/bin/env.sh
echo "export INDEXVERSION="$INDEXVERSION >> $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION/seqcrawler/solr/bin/env.sh

#cp solrbin/* $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION/seqcrawler/solr/bin/
chmod 755 $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION/seqcrawler/solr/bin/*.sh
#rm -rf solrbin

#svn export https://seqcrawler.svn.sourceforge.net/svnroot/seqcrawler/trunk/index/CrawlerIndex/solr/conf solrconf --force
#cp solrconf/schema.xml $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION/seqcrawler/solr/conf/
sed -i 's/<dataDir>${solr.data.dir:.\/solr\/data}<\/dataDir>/<dataDir>${solr.data.dir:\/var\/lib\/seqcrawler\/index\/solr}<\/dataDir>/g' $PKG/usr/share/seqcrawler/solr/apache-solr-$SOLRVERSION/seqcrawler/solr/conf/solrconfig.xml
rm -f $PKG/usr/share/seqcrawler/solr/apache-solr-1.4.1/seqcrawler/solr/bin/seqcrawler.properties
#rm -rf solrconf

# Export webapp
echo "Installing export web app"
wget http://maven.irisa.fr/artifactory/genouest-public-snapshot/org/irisa/genouest/seqcrawler/CrawlerIndex/$SEARCHVERSION/CrawlerSearchWebApp-$SEARCHVERSION.war

mv CrawlerSearchWebApp-0.1.war $PKG/usr/share/java/webapps/CrawlerSearchWebApp.war
unzip $PKG/usr/share/java/webapps/CrawlerSearchWebApp.war -d $PKG/usr/share/java/webapps/CrawlerSearchWebApp
rm $PKG/usr/share/java/webapps/CrawlerSearchWebApp.war
rm -f $PKG/usr/share/java/webapps/CrawlerSearchWebApp/WEB-INF/web.xml
rm -f $PKG/usr/share/java/webapps/CrawlerSearchWebApp/WEB-INF/lib/servlet-api-2.5.jar
rm -f $PKG/usr/share/java/webapps/solr/seqcrawler-conf.js

# Solr Web data
echo "Installing Solr web data"
svn export https://seqcrawler.svn.sourceforge.net/svnroot/seqcrawler/trunk/index/CrawlerIndex/solr/web/ solrweb  --force
cp -R solrweb/* $PKG/usr/share/java/webapps/solr
rm -rf solrweb

#MongoDB web
echo "Installing MongoDb web pages"
svn export https://seqcrawler.svn.sourceforge.net/svnroot/seqcrawler/trunk/index/CrawlerIndex/mongodb/www/ mongoweb --force
cp -R mongoweb/* $PKG/usr/share/seqcrawler/mongo/
rm -rf mongoweb
svn export https://seqcrawler.svn.sourceforge.net/svnroot/seqcrawler/trunk/index/CrawlerIndex/mongodb/cgi-bin/ mongocgi --force
cp -R mongocgi/mongo/* $PKG/usr/lib/cgi-bin/mongo/
chmod 755 $PKG/usr/lib/cgi-bin/mongo/*.pl
rm -rf mongocgi

tar cvfz seqcrawler_$VERSION.orig.tar.gz $PKG
rm -rf $PKG
