influxdb-tomcat-accesslog
=============

A Tomcat Access Log Valve that will log to an InfluxDB database.

This implementation aims to add influxdb support to the [Tomcat Access Log Valve Component](https://tomcat.apache.org/tomcat-8.0-doc/config/valve.html#Access_Log_Valve).

Tomcat's default Access Log Valve logs the access logs to the *logs/localhost_access_log.TIMESTAMP.txt* file.

# Configuration

Add the following section to Tomcat's server.xml file.

You can either keep, or replace the default access log org.apache.catalina.valves.AccessLogValve.  

### TCP mode:
```XML
        <Valve className="com.cbnt.influxdb.InfluxDBAccessLogValve" 
          connectionName="root"
      connectionPassword="root" 
           connectionURL="http://localhost:8086/" 
            databaseName="mydatabase"
               seriesName="httpAccessLogs" 
                  pattern="combined" />
```

### UDP mode
```XML
        <Valve className="com.cbnt.influxdb.InfluxDBAccessLogValve" 
          connectionName="root"
      connectionPassword="root" 
           connectionURL="http://localhost:8086/"
               transport="UDP"
                 udpPort="4444" 
               seriesName="httpAccessLogs" 
                  pattern="combined" />
```

# Sample Data

An example of a request logged with influxdb-tomcat-accesslog:

|time|sequence_number|remoteHost|userName|query|bytes|virtualHost|status|method|referer|userAgent|
|----|---------------|----------|--------|-----|-----|-----------|------|------|-------|---------|
|1433192553974|155500001|127.0.0.1||/docs/images/asf-feather.png|0|localhost|304|GET|http://localhost:8080/docs/security-howto.html|Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:38.0) Gecko/20100101 Firefox/38.0|
|1433192553963|155490001|127.0.0.1||/docs/images/tomcat.png|0|localhost|304|GET|http://localhost:8080/docs/security-howto.html|Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:38.0) Gecko/20100101 Firefox/38.0|
|1433192553946|155480001|127.0.0.1||/docs/images/fonts/fonts.css|0|localhost|304|GET|http://localhost:8080/docs/images/docs-stylesheet.css|Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:38.0) Gecko/20100101 Firefox/38.0|

### Build

* Java 1.6+
* Maven 3.0+

Buid with maven: 

    $ mvn clean install

After the build is done, drop the following jars inside TOMCAT_HOME/lib folder:
- influxdb-tomcat-accesslog-0.1-SNAPSHOT.jar
- guava-18.0.jar
- okhttp-2.0.0.jar
- okhttp-urlconnection-2.0.0.jar
- gson-2.3.jar
- okio-1.0.0.jar
- influxdb-java-1.5.jar

This implementation is still on early development days and hasn't been published yet to any public maven repository.
