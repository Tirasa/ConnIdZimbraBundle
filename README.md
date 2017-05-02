ConnIdZimbraBundle
==============

The Zimbra bundle is part of the [ConnId](http://connid.tirasa.net) project.

The source code for this bundle was initially donated by [Intesys](http://www.intesys.it/).

<a href="https://maven-badges.herokuapp.com/maven-central/net.tirasa.connid.bundles/net.tirasa.connid.bundles.zimbra">
  <img src="https://maven-badges.herokuapp.com/maven-central/net.tirasa.connid.bundles/net.tirasa.connid.bundles.zimbra/badge.svg"/>
</a>

## How to get it

### Maven

```XML
<dependency>
  <groupId>net.tirasa.connid.bundles</groupId>
  <artifactId>net.tirasa.connid.bundles.zimbra</artifactId>
  <version>${connid.zimbra.version}</version>
</dependency>
```

where `connid.zimbra.version` is one of [available](http://repo1.maven.org/maven2/net/tirasa/connid/bundles/net.tirasa.connid.bundles.zimbra/).

### Downloads

Available from [releases](https://github.com/Tirasa/ConnIdZimbraBundle/releases).

### Runtime dependencies

This bundle requires the following JAR files to be available in the runtime classpath:

* zimbraclient.jar
* zimbracommon.jar
* zimbrasoap.jar
* zimbrastore.jar

which can be normally found under `/opt/zimbra/lib/jars/` of your Zimbra deployment.

Moreover, depending on your actual Zimbra version, the following additional runtime dependencies might be needed:

* commons-codec-1.7.jar
* commons-httpclient-3.1.jar
* dom4j-1.5.2.jar
* guava-13.0.1.jar
* javamail-1.4.5.jar
* json.jar
* log4j-1.2.16.jar

which can be also found under `/opt/zimbra/lib/jars/` of your Zimbra deployment.

## Project information

 * [wiki](https://connid.atlassian.net/wiki/display/BASE/Zimbra)
 * [issues](https://connid.atlassian.net/browse/ZIMBRA)
