cassandra-driver-mapping
========================

This is the Plugin for the DataStax Java Driver for Apache Cassandra (C*), 
The module is not replacement for the DataStax Java Driver but small handy add-on to it.
The main goal is to enable JPA-like behavour for entities to be persisted in C*.


Features
--------

The features provided by the plugin module includes:
-Generate Schema
  -- Create table from any Java Bean without annotations. Id property and public getters/sestters are required.
  - Create table with indexes from the JPA 2.1 annotated entities.
  - Alter tables and indexes if entity class has changed.
  - Drop tables and indexes.

  - Get entity from Cassandra.
  - Save entity to Cassandra.
  - Delete entity from Cassandra.  

Getting Started
---------------

Create or alter Tables and Indexes::
    
    SchemaSync.sync(keyspace, session, Entity1.class, Entity2.class, Entity3.class ...);
	
Drop Tables and Indexes::
    
    SchemaSync.drop(keyspace, session, Entity1.class, Entity2.class, Entity3.class ...);

Work with Entity::
    
    MappingSession msession = new MappingSession(keyspace, session);
    
    Entity entity = msession.get(Entity.class, id);
    
    msession.save(entity);
    
    msession.delete(entity);	


Installing
----------

The last release of the plugin is available on Maven Central. You can install
it in your application using the following Maven dependency::

    <dependency>
      <groupId>com.datastax.cassandra</groupId>
      <artifactId>cassandra-driver-mapping</artifactId>
      <version>2.0.0-rc2</version>
    </dependency>

Prerequisite
------------
The module doesn't have its own versioning system insted it piggybacks DataStax Java Driver version.
The module uses DataStax Java Driver version 2.0 and JPA 2.1

Coming Features
---------------
   - Support composite Primary Keys with Embedded Annotation
   - Support optimistic lock with timestamp or versioning	

Please see wiki page for complete information about mapping plugin.
For Datastax Java Driver please refer to the (http://www.datastax.com/drivers/java/apidocs/).

