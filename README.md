cassandra-driver-mapping
========================

Enjoy this cool addon for the DataStax Java Driver which will bring you piece and serenity while working with Apache Cassandra (C*).
The main goal is to enable improved JPA-like behaviour for entities to be persisted in C*.
The module is not replacement for the DataStax Java Driver but time-saver addon to it.
The module relies on DataStax Java Driver version 2.0 and JPA 2.1.

You can read more about Datastax Java Driver at (http://www.datastax.com/drivers/java/apidocs/).

Jump Start
----------

Install it in your application from Maven Central using the following dependency::

    <dependency>
      <groupId>com.valchkou.datastax</groupId>
      <artifactId>cassandra-driver-mapping</artifactId>
      <version>2.0.0-rc2</version>
    </dependency>

Create MappingSession instance::

	import com.datastax.driver.mapping.MappingSession;
    ...
    MappingSession mappingSession = new MappingSession(keyspace, session);
    
    Note: you need to open the session and create the keyspace in prior to use MappingSession.
    If you are not familiar with procedure please refer to http://www.datastax.com/docs for Developers.
 
Play with your entity::   

	Entity entity = new Entity();
    mappingSession.save(entity);
    
    entity = mappingSession.get(Entity.class, id);
    
    mappingSession.delete(entity);	

Very simple, isn't it. Right, no mapping files, no scripts. You don't have to generate your scripts to create the Table and Indexes for the Entity.
All is built-in and taken care. If tables and indexes do not yet exist they will be automatically created when you fist use entity.
If you add or remove property on you entity it will be automatically synchronized with C*. 

Features
--------

The features provided by the plugin module includes:
  - Manipulate Entity
  	* Get entity from Cassandra.
  	* Save entity to Cassandra.
	* Delete entity from Cassandra.
	* Run custom Queries built with datastax.QueryBuilder
	* Convert ResultSet into List of Entities

  - Generate Schema
  	* Create table from any Java Bean even without annotations. 
  	* Create tables and indexes from the JPA 2.1 annotated entities.
  	* Alter tables and indexes if entities definition has changed.
  	* Drop tables and indexes.

Upcoming Features
-----------
   - Support composite Primary Keys
   - Support TTL and Timestamp
   - Enable optimistic lock for Entities (TBD)
   - Support options for Create Table 

Mapping Examples
----------------

   - Simple Bean
   - JPA Entity
   - JPA Entity with indexes 
   - Transient property
   - Collections

Custom Queries
--------------
	- building and running queries with custom where conditions

Alter Behaviour
----------------
   - Explanation of internals how the create and alter work

Entity Metadata
---------------
   - Why and How to access Metadata
   - How to override Data Type mapping

Using with Spring Framework 
---------------------------
   - Simple Sample with Spring Framework
	
