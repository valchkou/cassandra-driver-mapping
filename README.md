cassandra-driver-mapping
========================

This is the Plugin for the core module of the DataStax Java Driver for Apache Cassandra (C*), 
The main goal of the module is to enable JPA-like behavour for your java entities to be persisted in cassandra.
You can generate cassandra schema out of your POJOs or JPA annotated entities.
The module is not replacement for the DataStax Java Driver but handy extention built on top of DataStax Java Driver.
The module does not do any entities caching internally neither it supports hibernate-like caches.

Features
--------

The features provided by the plugin module includes:
  - Genereate tables and indexes from the javax.persistence annotated entities.
  - Genereate tables from the POJO. No annotations are required.
  - Alter tables and indexes if entity declaration has changed.  
  - Drop tables and indexes.

  - Load entity from Cassandra.  
  - Save entity to Cassandra.  
  - Delete entity from Cassandra.  


Prerequisite
------------
The module doesn't have its own versioning system insted it piggybacks DataStax Java Driver version.
The module uses DataStax Java Driver version 2.0 and JPA 2.1

If you want to run the unit tests provided with this driver, you will also need
to have ccm installed (http://github.com/pcmanus/ccm) as the tests use it. Also
note that the first time you run the tests, ccm will download/compile the
source of C* under the hood, which may require some time (that depends on your
Internet connection or machine).

Installing
----------

The last release of the plugin is available on Maven Central. You can install
it in your application using the following Maven dependency::

    <dependency>
      <groupId>com.datastax.cassandra</groupId>
      <artifactId>cassandra-driver-mapping</artifactId>
      <version>2.0.0-rc2</version>
    </dependency>


Getting Started
---------------

Suppose you have a Cassandra cluster running and connection session to the cluster initialized. 
Please refer to the API reference (http://www.datastax.com/drivers/java/apidocs/).

A simple example using this core driver could be::

    Cluster cluster = Cluster.builder()
                        .addContactPoints("cass1", "cass2")
                        .build();
    Session session = cluster.connect("db1");

    for (Row row : session.execute("SELECT * FROM table1"))
        // do something ...

Please note that when we build the Cluster object, we only provide the address
to 2 Cassandra hosts. We could have provided only one host or the 3 of them,
this doesn't matter as long as the driver is able to contact one of the host
provided as "contact points". Even if only one host was provided, the driver
would use this host to discover the other ones and use the whole cluster
automatically. This is also true for new nodes joining the cluster.

For now, please refer to the API reference (http://www.datastax.com/drivers/java/apidocs/).
More informations and documentation will come later.
