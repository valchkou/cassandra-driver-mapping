cassandra-driver-mapping
========================

Enjoy this cool addon for the DataStax Java Driver which will bring you piece and serenity while working with Apache Cassandra (C*).
The main goal is to enable improved JPA-like behaviour for entities to be persisted in C*.
The module is not replacement for the DataStax Java Driver but smart addon to it.
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
    
    MappingSession msession = new MappingSession(keyspace, session);
 
Play with your entity::   

	Entity entity = new Entity();
    msession.save(entity);
    
    entity = msession.get(Entity.class, id);
    
    msession.delete(entity);	

If tables and indexes do not exist they will be automatically created on the fist entity use.

You can also run custom Queries like this::
    
    MappingSession msession = new MappingSession(keyspace, session);
    
    Statement query = QueryBuilder
    	.select()
    	.all()
    	.from(keyspace, table)
    	.where(eq(column, value));
    	
    List<Entity> items = msession.getByQuery(Entity.class, query);

You can also Create, Alter and Drop Tables and Indexes::
    
    SchemaSync.sync(keyspace, session, Entity1.class, Entity2.class, Entity3.class ...);
	
    SchemaSync.drop(keyspace, session, Entity1.class, Entity2.class, Entity3.class ...);
			


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
	
