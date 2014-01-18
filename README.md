cassandra-driver-mapping
========================

Enjoy this cool addon for the DataStax Java Driver which will bring you piece and serenity while working with Apache Cassandra (C*).
The main goal is to enable improved JPA-like behaviour for entities to be persisted in C*.
The module is not replacement for the DataStax Java Driver but time-saver addon to it.
The module relies on DataStax Java Driver version 2.0 and JPA 2.1.

You can read more about Datastax Java Driver at (http://www.datastax.com/drivers/java/apidocs/).

Jump Start
----------

Install it in your application from Maven Central using the following dependency:
```
    <dependency>
      <groupId>com.valchkou.datastax</groupId>
      <artifactId>cassandra-driver-mapping</artifactId>
      <version>2.0.0-rc2</version>
    </dependency>
```
Create MappingSession instance:
```
	import com.datastax.driver.mapping.MappingSession;
    ...
    MappingSession mappingSession = new MappingSession(keyspace, session);
```    
Note: You need to open the session and create the keyspace in prior to use MappingSession.
If you are not familiar with procedure please refer to http://www.datastax.com/docs for Developers.
Or look  at the Spring Framework section at the bottom.
 
Now you can play with your entity: 
```java
	Entity entity = new Entity();
    mappingSession.save(entity);
    
    entity = mappingSession.get(Entity.class, id);
    
    mappingSession.delete(entity);	
```
Very simple, isn't it? No mapping files, no scripts, no configuration files. 
You don't have to worry about creating the Table and Indexes for your Entity.
All is built-in and taken care of. If table and indexes do not yet exist they will be automatically created when you fist use of the entity.
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
   Please read to understand what will and will not be altered.
   Module creates tables and indexes based on entity definition.
   As your project is evolving you may want to refactor entity, add or delete properties and indexes.
   
   Not alterable
   		- add/delete/rename primary key columns
   		- change column data type to incompatible one, such as string to number.	
   		- change property name which is not annotated as @Column. This will be understood as a new property.
   	
   Alterable
   		- add new property.
   		- delete property.
   		- add index on column
   		- change datatype to compatible one. Compatibility is enforced by C*.	
   		

Entity Metadata
---------------
   
You may want to access Entity metadata if you are building custom Statements.
Entity Metadata contains corresponding table and column names.
Metadata can be accessed any where in you code as:
```java	
	EntityTypeMetadata emeta = EntityTypeParser.getEntityMetadata(Entity.class)
	emeta.getTableName(); // corresponding table name in C*
	
	// get field meta info by property name
	EntityFieldMetaData fdata = emeta.getFieldMetadata("email");
	 
	// corresponding column name in C*
	String columnName = fdata.getColumnName(); 
	
	 // all the persistent fields on entity
	List<EntityFieldMetaData> fields = emeta.getFields();
```	
   
The core part of mapping addon is a mapping between datastax DataTypes and Java types.
Default mapping is:
```java
		DataType.Name.BLOB.asJavaClass(), 		DataType.Name.BLOB
		DataType.Name.BOOLEAN.asJavaClass(),    DataType.Name.BOOLEAN
	    DataType.Name.TEXT.asJavaClass(),     	DataType.Name.TEXT
		DataType.Name.TIMESTAMP.asJavaClass(),  DataType.Name.TIMESTAMP
	    DataType.Name.UUID.asJavaClass(),       DataType.Name.UUID
	    DataType.Name.INT.asJavaClass(),    	DataType.Name.INT
	    DataType.Name.DOUBLE.asJavaClass(),     DataType.Name.DOUBLE
	    DataType.Name.FLOAT.asJavaClass(),     	DataType.Name.FLOAT
	    DataType.Name.BIGINT.asJavaClass(),     DataType.Name.BIGINT
	    DataType.Name.DECIMAL.asJavaClass(), 	DataType.Name.DECIMAL
	    DataType.Name.VARINT.asJavaClass(),  	DataType.Name.VARINT
	    DataType.Name.MAP.asJavaClass(), 	   	DataType.Name.MAP
	    DataType.Name.LIST.asJavaClass(), 	   	DataType.Name.LIST
	    DataType.Name.SET.asJavaClass(), 	   	DataType.Name.SET  
	    boolean.class, 							DataType.Name.BOOLEAN 
	    int.class, 								DataType.Name.INT
	    long.class, 							DataType.Name.BIGINT
	    double.class, 							DataType.Name.DOUBLE
	    float.class, 							DataType.Name.FLOAT
```
You can override defaults as:
```java
	Map<Class<?>, DataType.Name> mapping = new HashMap<Class<?>, DataType.Name>();
	.... populate the map
	EntityTypeParser.setDataTypeMapping(mapping);
```
Or override individual type:
```java
	EntityTypeParser.overrideDataTypeMapping(javaClass, DataType.Name)
```
This is very internal so you have to understand what you are doing.
Java type must match data type defined in core driver com.datastax.driver.core.DataType.


Using with Spring Framework 
---------------------------
- Configure propertyews such as keyspace and nodes.
Let's guess you have a property file /META-INF/cassandra.properties:
  ```
   		cassandra.keyspace=your_keyspace
		cassandra.node=127.0.0.1
   ```
   
- Include properties in your spring config:
   ```xml
    <?xml version="1.0" encoding="UTF-8"?>
	<beans:beans xmlns="http://www.springframework.org/schema/mvc"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:beans="http://www.springframework.org/schema/beans"
		xmlns:context="http://www.springframework.org/schema/context"
		xsi:schemaLocation="http://www.springframework.org/schema/mvc http://www.springframework.org/schema/mvc/spring-mvc.xsd
		http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
		http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">

		
		<!-- Enables the Spring MVC @Controller programming model -->
		<annotation-driven />

		<context:property-placeholder location="classpath*:/META-INF/cassandra.properties"/>  
	
		<context:component-scan base-package="your.package.path" />
	 </beans:beans> 
   ```
   
- Create a class which will initialize connection to C*:
	
	```java
		import org.springframework.beans.factory.annotation.Value;
		import org.springframework.stereotype.Repository;
		import com.datastax.driver.core.Cluster;
		import com.datastax.driver.core.Session;
		import com.datastax.driver.mapping.MappingSession;
		import com.datastax.driver.mapping.schemasync.SchemaSync;
		
		@Repository
		public class CassandraSessionFactory {
			
			@Value("${cassandra.keyspace}")
			private String keyspace;
			
			@Value("${cassandra.node}")
			private String node;
			
			private Cluster cluster;
			private Session session;
			private MappingSession mappingSession;
				
			public Session getSession() {
				if (session == null) {
					connect();
				}
				return session;
			}
		
			public MappingSession getMappingSession() {
				if (session == null) {
					connect();
				}
				return mappingSession;
			}
		
			public String getKeyspace() {
				return keyspace;
			}
			
			protected synchronized void connect() {
				if (session == null) {
					cluster = Cluster.builder().addContactPoint(node).build();
					session = cluster.connect();
					session.execute("CREATE KEYSPACE IF NOT EXISTS "+ getKeyspace() +
						" WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 3 }");
						
					mappingSession = new MappingSession(getKeyspace(), getSession());
				}	
			}
		}
	```
	
- inject your factory in YourEntityDAO::
		
	```java		
		@Repository
		public class AccountDAO {
			
			@Autowired
			CassandraSessionFactory sf;
			
			public void save(Account account) {
				sf.getMappingSession().save(account);	
			}
			
			public void save(Account account) {
				sf.getMappingSession().delete(account);	
			}
			
			public Account getById(Object id) {
				return sf.getMappingSession().get(ChatAccount.class, id);	
			}
			
			public Account getByEmail(String email) {
				EntityTypeMetadata emeta = EntityTypeParser.getEntityMetadata(Account.class);
				EntityFieldMetaData fmeta = emeta.getFieldMetadata("email");
				Statement stmt = QueryBuilder
						.select().all()
						.from(sf.getKeyspace(), emeta.getTableName())
						.where(eq(fmeta.getColumnName(), email));
				List<ChatAccount> result = sf.getMappingSession().getByQuery(Account.class, stmt);
				if (result != null && result.size()>0) {
					return result.get(0);
				}
				return null;
			}
		}
	```
	

