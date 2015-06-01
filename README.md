influxdb-tomcat-accesslog
=============

A Tomcat Access Log Valve that will log to an InfluxDB database.

This implementation aims to add influxdb support to the [Tomcat Access Log Valve Component](https://tomcat.apache.org/tomcat-8.0-doc/config/valve.html#Access_Log_Valve). 
Tomcat's default Access Log Valve logs the logs/localhost_access_log.TIMESTAMP.txt file.

# Configuration

Drop the following jars inside TOMCAT_HOME/lib folder:
- influxdb-tomcat-accesslog-0.1-SNAPSHOT.jar
- guava-18.0.jar
- okhttp-2.0.0.jar
- okhttp-urlconnection-2.0.0.jar
- gson-2.3.jar
- okio-1.0.0.jar
- influxdb-java-1.5.jar


Add the following section to Tomcat's server.xml file. 
PS: You can either keep, or replace the default access log org.apache.catalina.valves.AccessLogValve.  

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

|time|sequence_number|method|status|referer|userAgent|remoteHost|bytes|virtualHost|userName|query|
|1433179304218|155440001|GET|200|http://localhost:8080/docs/security-howto.html|Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:38.0) Gecko/20100101 Firefox/38.0|127.0.0.1|8218|localhost||/docs/maven-jars.html|


### Build Requirements

* Java 1.6+
* Maven 3.0+

This implementation is still on early development days and hasn't been published yet to any public maven repository.
