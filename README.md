cassandra-driver-mapping
========================
  
Entity Mapping Add-on for the DataStax Java Driver (Driver) for Cassandra (C*).  
This Add-on allows you to generate schema automatically and persist JPA annotated entities in C*.

Add-on is not replacement for the Driver but lightweight utility for it.  
You still can utilize full power of the Driver API and Datastax documentation.     
Mapping Add-on relies on Driver version 2.XXX and JPA 2.1.    

Read more about [Datastax Java Driver, Cassandra and CQL3](http://www.datastax.com/documentation/gettingstarted/index.html).

[More Usage Samples in Unit Tests]
(https://github.com/valchkou/cassandra-driver-mapping/blob/master/src/test/java/com/datastax/driver/mapping/)

### Table of Contents  
- [Features](#features)  
- [Jump Start](#start)  
	* [Maven Dependency](#jump_maven)
	* [Init Mapping Session](#jump_init)
	* [Save](#jump_save)
	* [Get](#jump_get)
	* [Delete](#jump_delete)
- [Various Mappings](#mapping)  
	* [Basic](#mapping_basic)
	* [Indexes](#mapping_index)
	* [Compound Primary Key](#mapping_composite)
	* [Composite Partition Key](#mapping_partition)
	* [Table Properties](#mapping_properties)
	* [Override Column Data Type](#mapping_datatype)
	* [Mixed Case for Column Names](#mapping_mixed)
- [Collections](#collections)
	* [Mapping](#mapping_collections)
	* [Optimized operations](#collections_opt)	
		- [List](#collections_list)
		- [Set](#collections_set)
		- [Map](#collections_map)
- [Optimistic Lock](#lock)
	* [Lightweight transactions](#lock_transactions)
	* [@Version](#lock_version)
- [Batch](#batch)
- [Nested Entities](#nested)
- [Mapping Custom Queries](#queries_mapping)  
	* [How To Run](#queries_howto)
	* [Any-to-Any and Magic Gnomes](#queries_gnomes)
- [Building Custom Queries](#queries_building)
	* [CQL String](#queries_cql)
	* [QueryBuilder (better)](#queries_builder)	
	* [QueryBuilder with EntityMetadata (even better)](#queries_meta)
- [Under The Hood](#under)
	* [Prepared Statement Cache](#pscache)  
	* [How Entity get synchronized](#sync)  
	* [Entity Metadata and Data Types](#metadata)  
- [Spring Framework Example](#spring)  

<a name="features"/>
### Features

The features provided by the module include:

- OM Layer
  	* Get entity from Cassandra.
  	* Save entity to Cassandra.
	* Delete entity from Cassandra.
	* Run and map custom Queries and ResultSets.

- Schema Sync
  	* Automatically create table and indexes from Entity. 
  	* Automatically Alter table and indexes if entity definition has changed.
  	* Drop table.

No mapping files, no scripts, no configuration files.   
You don't have to worry about creating the Table and Indexes for your Entity manually.  
All is built-in and taken care of. Entity definition will be automatically [synchronized with C*](#sync).  

<a name="start"/>
### Jump Start

<a name="jump_maven"/>
- Maven Dependency.  
Install in your application from Maven Central using the following dependency:
```xml
    <dependency>
      <groupId>com.valchkou.datastax</groupId>
      <artifactId>cassandra-driver-mapping</artifactId>
      <version>2.1.0-rc1</version>
    </dependency>
```
All new changes and bugfixes are released within the latest version as soon as coded.
Module versioning policy matches underlying datastax driver core versioning.


<a name="jump_init"/>
- Init Mapping Session.  
MappingSession is cheap to instantiate and it is not replacement for the Datastax Session.   
You can instantiate as many mapping sessions as you want. It's threadsafe.  
```java
	import com.datastax.driver.core.Session;
	import com.datastax.driver.mapping.MappingSession;
	...
    	
	Session session; // initialize datastax session.
	MappingSession mappingSession = new MappingSession("keyspace_name", session);
```  
If you wish your mapping session do not synchronize your entities with C* you may turn synch off:
```java
	MappingSession mappingSession = new MappingSession("keyspace_name", session, true);
	// OR
	MappingSession mappingSession = new MappingSession("keyspace_name", session);
	mappingSession.setDoNotSync(true);
```  
Underlying Datastax Session does all the heavylifting and is expansive.   
Prior using MappingSession you need to open the Datastax Session and create the Keyspace using the standard Datastax Driver API. If you are not familiar with procedure please refer to [Datastax Dcumentation](http://www.datastax.com/documentation/developer/java-driver/2.0/java-driver/quick_start/qsQuickstart_c.html).  
Or look at the [Spring Framework Example](#spring).

<a name="jump_save"/>
- Save.
```java
	Entity entity = new Entity();
	mappingSession.save(entity);
```
OR
```java
	import com.datastax.driver.mapping.option.WriteOptions;
	import com.datastax.driver.core.policies.DefaultRetryPolicy;
	import com.datastax.driver.core.ConsistencyLevel;
	...
	// using options
	WriteOptions options = new WriteOptions()
		.setTtl(300)
		.setTimestamp(42)
		.setConsistencyLevel(ConsistencyLevel.ANY)
		.setRetryPolicy(DefaultRetryPolicy.INSTANCE);
		
	Entity entity = new Entity();
	mappingSession.save(entity, options);
```

<a name="jump_get"/>
- Get.
```java
	Entity entity = mappingSession.get(Entity.class, id);
```
OR
```java
	import com.datastax.driver.mapping.option.ReadOptions;
	import com.datastax.driver.core.policies.DefaultRetryPolicy;
	import com.datastax.driver.core.ConsistencyLevel;
	...
	// using options
	ReadOptions options = new ReadOptions()
		.setConsistencyLevel(ConsistencyLevel.ANY)
		.setRetryPolicy(DefaultRetryPolicy.INSTANCE);
		
	Entity entity = mappingSession.get(Entity.class, id, options);
```

<a name="jump_delete"/>
- Delete.
```java
	mappingSession.delete(entity);	
```

<a name="mapping"/>
### Various Mappings

	IMPORTANT!!!  
	- If entity or field is not annotated it will provide its name as default.    
	- Id field is required and must be annotated with @Id or @EmbeddedId.
	- Index name must be unique within the keyspace.  
	- C* supports only single-column-index.


<a name="mapping_basic"/>	  	  
#### Basic Mapping
```java
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Column

@Table (name="mytable")
public class Entity {
	
	@Id
	private long Id;
	
	@Column(name = "myname")
	private String name;
	
	// @Column is not required
	private int age;
	
	@Transient
	private BigDecimal calculable;
	
	// public getters/setters ...
}
```
CQL3 Statement
```
   CREATE TABLE IF NOT EXISTS ks.mytable (id bigint, myname text, age int, PRIMARY KEY(id))
```     

<a name="mapping_index"/>	
#### Mapping Indexes
```java
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Column
import javax.persistence.Index
import java.util.UUID

@Table (name="mytable", 
indexes = {
	@Index(name="entity_email_idx", columnList="email" ), 
	@Index(name="entity_name_idx", columnList="myname" ) 
})
public class Entity {
	
	@Id
	private java.util.UUID code;
	
	@Column(name = "myname")
	private String name;
	private String email;
	// public getters/setters ...
}
```
CQL3 Statement
```
   	CREATE TABLE IF NOT EXISTS ks.mytable (code uuid, myname text, email text,  PRIMARY KEY(code)); 
	CREATE INDEX IF NOT EXISTS entity_email_idx ON ks.mytable(email);  
	CREATE INDEX IF NOT EXISTS entity_name_idx ON ks.mytable(myname);
```   

<a name="mapping_composite"/>
#### Compound Primary Key
```java
public class CompositeKey {
	private String name;
	private int rank;
	// public getters/setters ...
}
```
```java

import javax.persistence.Table;
import javax.persistence.EmbeddedId;	

@Table(name="entity")
public class Entity {
	@EmbeddedId
	private CompositeKey key;
	private String email;
	// public getters/setters ...
}
```
CQL3 Statement
```
   CREATE TABLE IF NOT EXISTS ks.entity (name text,  rank int, email text,  PRIMARY KEY(name, rank))
```     

<a name="mapping_partition"/>
#### Composite Partition Key
```java
public class PartitionKey {
	private String firstName;
	private String lastName;
	// public getters/setters ...
}
```
```java
public class CompositeKey {
	@EmbeddedId
	private PartitionKey key;
	private int age;
	// public getters/setters ...
}
```
```java
import javax.persistence.Table;
import javax.persistence.EmbeddedId;	

@Table(name="entity")
public class Entity {
	@EmbeddedId
	private CompositeKey key;
	private String email;
	// public getters/setters ...
}
```
CQL3 Statement
```
   CREATE TABLE IF NOT EXISTS ks.entity (firstname text, lastname text, age int, email text,  PRIMARY KEY((firstname, lastname), age))
```     

<a name="mapping_properties"/>
#### Table Properties  
This feature is not JPA standard! [ Read more about C* Table properties ] (http://www.datastax.com/documentation/cql/3.1/cql/cql_reference/cql_storage_options_c.html)
```java
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Column

import com.datastax.driver.mapping.annotation.TableProperties;
import com.datastax.driver.mapping.annotation.TableProperty;

@Table (name="mytable")
@TableProperties(values = {
	@TableProperty("comment='Important records'"),
	@TableProperty("read_repair_chance = 1.0"),
	@TableProperty("compression ={ 'sstable_compression' : 'DeflateCompressor', 'chunk_length_kb' : 64 }")
})
public class Entity {
	
	@Id
	private long Id;
	private String name;
	// public getters/setters ...
}
```
CQL3 Statement
```
   CREATE TABLE IF NOT EXISTS ks.mytable (id bigint, name text, PRIMARY KEY(id)) WITH comment='Important records' AND read_repair_chance = 1.0 AND compression ={ 'sstable_compression' : 'DeflateCompressor', 'chunk_length_kb' : 64 }
```     




```java
public class CompositeKey {
	private String name;
	private int rank;
	// public getters/setters ...
}
```
```java

import javax.persistence.Table;
import javax.persistence.EmbeddedId;	
import com.datastax.driver.mapping.annotation.TableProperties;
import com.datastax.driver.mapping.annotation.TableProperty;

@Table(name="entity")
@TableProperties(values = {
		@TableProperty("CLUSTERING ORDER BY (rank DESC)")
	})
public class Entity {
	@EmbeddedId
	private CompositeKey key;
	private String email;
	// public getters/setters ...
}
```
CQL3 Statement
```
   CREATE TABLE IF NOT EXISTS ks.entity (name text,  rank int, email text,  PRIMARY KEY(name, rank)) WITH CLUSTERING ORDER BY (rank DESC)
```
<a name="mapping_datatype"/>
#### Override Column Data Type.   
Datastax defines [data type mapping from Java to C*] (http://www.datastax.com/documentation/developer/java-driver/2.0/java-driver/reference/javaClass2Cql3Datatypes_r.html).  
This addon defines opposite way mapping. [You can explore daults here](#metadata).    
But in case you don't like defaults you are able to override the type on the column level.   
For example you want to leverage "time UUID" for timeseries data instead of "random UUID".  
```java
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Column

@Table (name="mytable")
public class Entity {
	
	@Id
	@Column(name="uid", columnDefinition="timeuuid") // case insensitive
	private UUID uid;		
	
	@Column(name="name", columnDefinition="VarChaR") // case insensitive
	private String name;
	// public getters/setters ...
}
```
CQL3 Statement
```
   CREATE TABLE IF NOT EXISTS ks.mytable (uid timeuuid, name varchar, PRIMARY KEY(uid))
```     
	
<a name="mapping_mixed"/>
#### Mixed Case for Column Names  
[C* converts all names to lowercase](http://www.datastax.com/documentation/cql/3.1/cql/cql_reference/ucase-lcase_r.html). This is default and recommended approach.  
But in case you need enforce the case you will need to wrap you names in double quotes. 
```java
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Column

@Table (name="mytable")
public class Entity {
	
	@Id
	@Column(name = "\"KEY\"")
	private int id;
	private String firstName;

	@Column(name = "\"last_NAME\"")
	private String lastName;

	@Column(name = "AGE")
	private int age;
	// public getters/setters ...
}
```
CQL3 Statement
```
   CREATE TABLE IF NOT EXISTS ks.mytable ("KEY" int, firstName text, "last_NAME" text, AGE int, PRIMARY KEY("KEY"))
```     

<a name="collections"/>
### Collections
   
<a name="mapping_collections"/>
#### Mapping

Collections must have generic type defined. Only java.util.List, Map and Set are allowed.  
By default implementation of HashMap, HashSet and ArrayList are used.
If you are unhappy with that fact and would like your data to be baked with specific collection implementation you can apply an annotation as shown below.
```java
	import com.datastax.driver.mapping.annotation.CollectionType;
	...
	@CollectionType(LinkedList.class)
	private List<String> cats;
	
	@CollectionType(TreeSet.class)
	private Set<Date> dogs;

	@CollectionType(TreeMap.class)
	private Map<String, BigInteger> pets;
}
```
NOTE: this is strictly java side feature and does not effect how your data stored in C*.     

CQL3 Statement
```
   CREATE TABLE IF NOT EXISTS ks.entity (id uuid, cats list<text>, dogs set<timestamp>, pets map<text, varint>,  PRIMARY KEY(id))
```     
For more info on collections please refer [Datastax Using Collection] (http://www.datastax.com/documentation/cql/3.1/cql/cql_using/use_collections_c.html)

<a name="collections_opt"/>
#### Optimized operations  
You can work with your collection properties as you would normally work with other entity properties.  
In addition C* provides optimized operations on collections. Those operations do not require to load and save the whole entity. C* allows us directly manipulate collections.   


<a name="collections_list"/>
- List operations
```java
// append item to list
mappingSession.append(id, Entity.class, "cats", "Black Cat");

// append item to be expired in 5 sec
mappingSession.append(id, Entity.class, "cats", "Expired Cat", new WriteOptions().setTtl(5));

// prepend item
mappingSession.prepend(id, Entity.class, "cats", "First Cat");

// replace item at specified index
mappingSession.replaceAt(id, Entity.class, "cats", "Grey Cat", 1);

// append List of items
List<String> addCats = new ArrayList<String>();
addCats.add("Red Cat");
addCats.add("Green Cat");
mappingSession.append(id, Entity.class, "cats", addCats);

// remove item
mappingSession.remove(id, Entity.class, "cats", "Grey Cat");

// remove List of items
List<String> removeCats = new ArrayList<String>();
removeCats.add("Red Cat");
removeCats.add("Green Cat");
mappingSession.remove(id, Entity.class, "cats", removeCats);

// remove all items
mappingSession.deleteValue(id, Entity.class, "cats");
```

<a name="collections_set"/>
- Set operations
```java
// append item
mappingSession.append(id, Entity.class, "dogs", "Black Dog");

// append item to be expired in 5 sec
mappingSession.append(id, Entity.class, "dogs", "Expired Dog", new WriteOptions().setTtl(5));

// append Set of items
Set<String> addDogs = new HashSet<String>();
addDogs.add("Red Dog");
addDogs.add("Green Dog");
mappingSession.append(id, Entity.class, "dogs", addDogs);

// remove item
mappingSession.remove(id, Entity.class, "dogs", "Black Dog");

// remove Set of items
Set<String> removeDogs = new HashSet<String>();
removeDogs.add("Red Dog");
removeDogs.add("Green Dog");
mappingSession.remove(id, Entity.class, "dogs", removeDogs);

// remove all items
mappingSession.deleteValue(id, Entity.class, "dogs");
```

<a name="collections_map"/>
- Map operations
```java
/** append item */
Map<String, BigInteger> pets = new HashMap<String, BigInteger>();
pets.put("Red Dogs", 25);
pets.put("Black Cats", 50);
mappingSession.append(id, Entity.class, "pets", pets);

/** append items to be expired in 5 sec */
Map<String, BigInteger> pets = new HashMap<String, BigInteger>();
pets.put("Green Dogs", 25);
pets.put("Brown Cats", 50);
mappingSession.append(id, Entity.class, "pets", pets, new WriteOptions().setTtl(5));

/** remove all items */
mappingSession.deleteValue(id, Entity.class, "pets");
```

<a name="lock"/>
### Optimistic Lock
C* does not support locking. But it provides ability for [Optimistic Concurrency Control] (http://en.wikipedia.org/wiki/Optimistic_concurrency_control).  
While running, transactions use data resources without acquiring locks on those resources. Before committing, each transaction verifies that no other transaction has modified the data it has read. If the check reveals conflicting modifications, the committing transaction rolls back and can be restarted.  
This section explains how you can achieve this with C* and Mapping Add-on


<a name="lock_transactions"/>
- Lightweight Transactions  
I don't know why they call it Lightweight Transactions. Those transactions are much heavier than normal C* transactions. Read more about [Datastax Lightweight Transactions.] (http://www.datastax.com/documentation/cassandra/2.0/cassandra/dml/dml_ltwt_transaction_c.html)  
C* supports conditional UPDATE/INSERT using IF/IF NOT EXISTS keywords. When "IF" condition is not met write doesn't happen. The boolean flag "[applied]" is returned.

<a name="lock_version"/>
- @Version  
Mapping Add-on enables optimistic locking using annotation @Version.  
The property must be of "long" data type. Whenever you save entity the version get incremented and as result of operation updated entity is retirned. If you try to save not-the-latest one then "null" will be returned instead and no error will be thrown.
```java
	
	import javax.persistence.Id;
	import javax.persistence.Table;
	import javax.persistence.Version;	
	
	@Table(name="entity")
	public class EntityWithVersion {
		@Id
		private java.util.UUID id;
	
		@Version
		private long version;	
		// public getters/setters ...
	}
	
	@Test
	public void entityWithVersionTest() throws Exception {
		UUID id = UUID.randomUUID();
		EntityWithVersion obj = new EntityWithVersion();
		obj.setId(id);
		obj.setName("ver1"); 
		
		EntityWithVersion loaded = mappingSession.get(EntityWithVersion.class, id);
		assertNull(loaded);
		
		// save object ver1 
		EntityWithVersion saved = mappingSession.save(obj);
		
		// get object ver1
		EntityWithVersion obj1 = mappingSession.get(EntityWithVersion.class, id);
		assertEquals(obj1, saved);
		assertEquals(1, saved.getVersion());
		
		// save object ver2
		saved = mappingSession.save(saved);
		EntityWithVersion obj2 = mappingSession.get(EntityWithVersion.class, id);
		assertEquals(obj2, saved);
		assertEquals(2, saved.getVersion());		
		
		saved = mappingSession.save(obj1);
		assertNull(saved);
	}		
```

<a name="batch"/>
### Batch
```java
		
	mappingSession.withBatch()
		.save(entityA)
		.save(entityB)
		.delete(entityC)
		.delete(entityD)
		.execute();
```


<a name="nested"/>
### Nested Entities
This section shows how you can support nested entities with C* and Mapping Add-on.
```java
	
	@Table(name="entity_a")
	public class EntityA {
		@Id
		private UUID id;
	
		// public getters/setters ...
	}
	
	@Table(name="entity_b")
	public class EntityB {
		@Id
		private UUID id;
		
		// reference on EntityA 
		private UUID refA;
		// public getters/setters ...
	}	
	
	public class TestNested() {
		
		@Test
		public void saveNested() throws Exception {
			EntityA a = new EntityA();
			mappingSession.save(a);
			
			EntityB b = new EntityB();
			b.setRefA(a.getId());
			mappingSession.save(b);
		}

		@Test
		public void loadNested() throws Exception {
			UUID bId = some_id;
			EntityB b = mappingSession.load(bId);
			EntityA a = mappingSession.load(b.getRefA());
		}
		
	}
```


<a name="queries_mapping"/>
### Mapping Custom Queries

<a name="queries_howto"/>
- How To Run  
There are two ways to run and map the query:  
	1) run using mapping session
	```java
	import com.datastax.driver.mapping.MappingSession;
	...
	List<Entity> result = mappingSession.getByQuery(Entity.class, query);
	```

	2) run using DataStax session and map the ResultSet
	```java
	import com.datastax.driver.core.Session;
	import com.datastax.driver.core.ResultSet;
	import com.datastax.driver.mapping.MappingSession;
	...
	ResultSet rs = session.execute(query);	
	List<Entity> result = mappingSession.getFromResultSet(Entity.class, rs);
	```

<a name="queries_gnomes"/>
- Any-to-Any and Magic Gnomes  
This is the coolest feature of the module. Your Entity doesn't have to match the table.  
You can populate any entity from any query (Any-to-Any).  
Consider example: 
```java
	public class AnyObject {
		private String name;
		private int age;
		// public getters/setters ...
	}
```
You can populate this object from any ResultSet which contains 'name' and 'age' columns.  
```java
	ResultSet rs = session.execute("SELECT name, age, birth_date, salary FROM person");	
	List<AnyObject> result = mappingSession.getFromResultSet(AnyObject.class, rs);
```
In this particular case 'name' and 'age' will be populated on 'AnyObject'. 'birth_date' and 'salary' will be ignored and no errors will be thrown.  
The biggest advantage that we can reuse the same entity to query different results from even different tables.
Entity doesn't have to map, match or relate to the table at all. 
Many thank to magic gnomes under the hood making all these work.

<a name="queries_building"/>
### Building Custom Queries
	
<a name="queries_cql"/>
- CQL String
```java
	import com.datastax.driver.mapping.MappingSession;
	... 
	
	// build query
	String query = "SELECT name, age, birth_date, salary FROM person");	
	
	// run query						
	List<Entity> result = mappingSession.getByQuery(Entity.class, query);	
```

<a name="queries_builder"/>
- QueryBuilder (Better)  
Datastax Driver shipped with a tool to build CQL statement.  
You can build your query with Datastax QueryBuilder and map ResultSet on Entity.  
QueryBuilder ensures you build correct CQL.
```java
				
	import com.datastax.driver.core.Statement;
	import com.datastax.driver.core.querybuilder.QueryBuilder;
	import com.datastax.driver.mapping.MappingSession;
	...

	// build query
	Statement query = QueryBuilder.select().all()
		.from("your_keyspace", "your_table").where(eq("column", value));
	
	// run query						
	List<Entity> result = mappingSession.getByQuery(Entity.class, query);
```

<a name="queries_meta"/>
- QueryBuilder with EntityMetadata (Even Better)  
In early stages you may often change table and column names.  
To avoid changing queries each time you rename something you can employ entity metadata.
```java
	import com.datastax.driver.core.Statement;
	import com.datastax.driver.core.querybuilder.QueryBuilder;
	import com.datastax.driver.mapping.MappingSession;
	import com.datastax.driver.mapping.EntityFieldMetaData;
	import com.datastax.driver.mapping.EntityTypeMetadata;	
	...			
	
	// get Entity Metadata
	EntityTypeMetadata emeta = EntityTypeParser.getEntityMetadata(Entity.class);
	
	// get field metadata by property/field name
	EntityFieldMetaData fmeta = emeta.getFieldMetadata(field_name); 
	
	// build query.
	Statement query = QueryBuilder.select().all()
		.from("your_keyspace", emeta.getTableName()).where(eq(fmeta.getColumnName(), value));
							
	// run query
	List<Entity> result = mappingSession.getByQuery(Entity.class, query);
```

<a name="under"/>
### Under The Hood

<a name="pscache"/>	   
#### Prepared Statement Cache
For the performance gain most update/select/delete statements are built as Prepared Statements.
Prepared Statements are reusable and placed in the static cache.
Cache is Guava Cache implementation initialized as:
```java
.expireAfterAccess(5, TimeUnit.MINUTES)
.maximumSize(1000)
.concurrencyLevel(4)
```

If you want to tune the cache for better performance you can do it as:
```java
Cache<String, PreparedStatement> cache = CacheBuilder
	.newBuilder()
	.expireAfterAccess(60, TimeUnit.MINUTES)
	.maximumSize(10000)
	.concurrencyLevel(8)
	.build();

MappingSession.setStatementCache(cache);
```
[More about Guava Cache](https://code.google.com/p/guava-libraries/wiki/CachesExplained)  

	   
<a name="sync"/>	   
#### How Entity get synchronized
The table structure is automatically synchronized with the entity definition on the first use of the entity.  
Any SessionMapping call internally will check if the entity has already been synchronized and if not   
it will run SchemaSync.sync. You can use sync API directly as:  

```java
	// create or alter
	import com.datastax.driver.mapping.schemasync.SchemaSync;
	...
	SchemaSync.sync(keyspace, session, Entity.class);
```
```java
	// drop table
	import com.datastax.driver.mapping.schemasync.SchemaSync;
	...
	SchemaSync.drop(keyspace, session, Entity.class);
```

You don't need to use this API unless you have reasons.   
Such as unittests or if you want to gain few milliseconds on the first use  
you may want to invoke the synchronization on the application start up instead. 

As the project is evolving sometimes there is need to refactor entity, add or delete properties and indexes. 
Again this all taken care automatically but with certain restrictions.     
Please read to understand what will and will not be altered and synchronized.
   
Not Alterable
   - add/delete/rename primary key columns. (C* restriction)  
   - change column data type to incompatible one, such as string to number. (C* restriction)  
   - change property name which is not annotated as @Column. This will be understood as a new property. 
   	
Alterable
   - add new property.
   - delete property.
   - add index on column.
   - change datatype to compatible one. Compatibility is enforced by C*.	
   		
<a name="metadata"/>
#### Entity Metadata and Data Types
   
You may want to access Entity metadata if you are building custom Statements.    
Entity Metadata contains corresponding table and column names.  
Entity Metadata can be easily accessed anywhere in your code as:
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
Datastax driver has mapping of datastax types to java. But not all types are mapped as 1-to-1.  
[CQL3 data types to Java types](http://www.datastax.com/documentation/developer/java-driver/1.0/webhelp/index.html#java-driver/reference/javaClass2Cql3Datatypes_r.html)  
In order the mapping to work the module defines backward mapping for the types.  

Java type | CQL3 data type
--- | ---
int|int
long|bigint
float|float
double|double
boolean|boolean
java.lang.Double|double
java.nio.ByteBuffer|blob
java.math.BigDecimal|decimal
java.lang.String|text
java.util.Date|timestamp
java.lang.Boolean|boolean
java.lang.Integer|int
java.lang.Long|bigint
java.util.Map|map
java.lang.Float|float
java.util.Set|set
java.math.BigInteger|varint
java.util.UUID|uuid
java.util.List|list

You can override defaults as:
```java
	import com.datastax.driver.core.DataType;
	...
	Map<Class<?>, DataType.Name> mapping = new HashMap<Class<?>, DataType.Name>();
	.... populate the map
	EntityTypeParser.setDataTypeMapping(mapping);
```
Or override individual type:
```java
	import com.datastax.driver.core.DataType;
	...
	EntityTypeParser.overrideDataTypeMapping(javaClass, DataType.Name)
```

<a name="spring"/>
### Spring Framework Example 

- Configure properties.  
Let's imagine we have a property file /META-INF/cassandra.properties:
  ```
   	cassandra.keyspace=your_keyspace
	cassandra.node=127.0.0.1
   ```
   
- Include properties in spring config:
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

- Define Entity
	```java
		import java.util.UUID;
		
		import javax.persistence.Entity;
		import javax.persistence.Column;
		import javax.persistence.Id;
		import javax.persistence.Table;
		
		@Entity
		@Table(name="account", indexes = {@Index(name="account_email_idx", columnList="email" )})
		public class Account {
			@Id
			private String id = UUID.randomUUID().toString();
			
			@Column(name="email") 
			private String email;			
			
			public String getId() {
				return id;
			}
			public void setId(String id) {
				this.id = id;
			}			
			public String getEmail() {
				return email;
			}
			public void setEmail(String email) {
				this.email = email;
			}
		}
	```
		   
- Create session factory for C*:
	
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
			
			/** only 1 thread is permitted to open connection */
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
	
- Create DAO and inject factory into it:
		
	```java		
		import java.util.List;
		
		import org.springframework.beans.factory.annotation.Autowired;
		import org.springframework.stereotype.Repository;
		
		import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
		import com.datastax.driver.core.Statement;
		import com.datastax.driver.core.querybuilder.QueryBuilder;
		import com.datastax.driver.mapping.EntityFieldMetaData;
		import com.datastax.driver.mapping.EntityTypeMetadata;
		import com.datastax.driver.mapping.EntityTypeParser;
		import com.datastax.driver.mapping.MappingSession;	
		
		@Repository
		public class AccountDAO {
			
			@Autowired
			CassandraSessionFactory sf;
			
			public void save(Account account) {
				sf.getMappingSession().save(account);	
			}
			
			public void delete(Account account) {
				sf.getMappingSession().delete(account);	
			}
			
			public Account getById(Object id) {
				return sf.getMappingSession().get(ChatAccount.class, id);	
			}
			
			public Account getByEmail(String email) throws Exception {
				
				Statement stmt = buildQueryForColumn("email", email);
				if (stmt==null) return null;
				
				List<Account> result = sf.getMappingSession().getByQuery(Account.class, stmt);
				if (result == null || result.size()==0) return null;
		
				return result.get(0);
			}
		
			
			/** Sample Building Select Statement for a single column with Datastax QueryBuilder */
			protected Statement buildQueryForColumn(String propName, Object propVal) {
				EntityTypeMetadata emeta = EntityTypeParser.getEntityMetadata(Account.class);
				EntityFieldMetaData fmeta = emeta.getFieldMetadata(propName);
				if (fmeta != null) {
					return QueryBuilder
							.select().all()
							.from(sf.getKeyspace(), emeta.getTableName())
							.where(eq(fmeta.getColumnName(), propVal));
				}
				return null;
			}
		}
	```
