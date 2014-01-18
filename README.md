cassandra-driver-mapping
========================

This is addon for the DataStax Java Driver for Apache Cassandra (C*), 
The main goal is to enable JPA-like behavour for entities to be persisted in C*.
The module is not replacement for the DataStax Java Driver but compact enhancement to it.
The module uses DataStax Java Driver version 2.0 and JPA 2.1.

For Datastax Java Driver info please refer to the (http://www.datastax.com/drivers/java/apidocs/).


Features
--------

The features provided by the plugin module includes:
  - Generate Schema
  	* Create table from any Java Bean even without annotations. 
  	* Create tables and indexes from the JPA 2.1 annotated entities.
  	* Alter tables and indexes if entities definition has changed.
  	* Drop tables and indexes.

  - Manipulate Entity
  	* Get entity from Cassandra.
  	* Save entity to Cassandra.
	* Delete entity from Cassandra.
	* Transform Queries built with datastax.QueryBuilder into entities
	* Transform datastax ResultSet into entities

Getting Started
---------------

Install it in your application from Maven Central using the following dependency::

    <dependency>
      <groupId>com.valchkou.datastax</groupId>
      <artifactId>cassandra-driver-mapping</artifactId>
      <version>2.0.0-rc2</version>
    </dependency>

Create or alter Tables and Indexes::
    
    SchemaSync.sync(keyspace, session, Entity1.class, Entity2.class, Entity3.class ...);
	
Drop Tables and Indexes::
    
    SchemaSync.drop(keyspace, session, Entity1.class, Entity2.class, Entity3.class ...);

Manipulate Entity::
    
    MappingSession msession = new MappingSession(keyspace, session);
    
    Entity entity = msession.get(Entity.class, id);
    
    msession.save(entity);
    
    msession.delete(entity);	

Query Entities::
    
    MappingSession msession = new MappingSession(keyspace, session);
    
    Statement query = QueryBuilder
    	.select()
    	.all()
    	.from(keyspace, table)
    	.where(eq(column, value));
    	
    List<Entity> items = msession.getByQuery(Entity.class, query);
			

Upcoming Features
-----------
   - Support composite Primary Keys
   - Support TTL and Timestamp
   - Enable optimistic lock for Entities (TBD)
   - Support options for Create Table 

Mapping Examples
----------------
   - comins soon
   - Simple Bean
   - JPA Entity
   - JPA Entity with indexes 
   - Transient property
   - Collections

Alter Behaviour
----------------
   - comins soon

Entity Metadata
---------------
   - comins soon

Using with Spring Framework 
---------------------------
   - comins soon
	
