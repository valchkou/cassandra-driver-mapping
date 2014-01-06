package com.datastax.driver.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.InvalidQueryException;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;

public class DataStaxTemplate {

	private static final Logger log = Logger.getLogger(DataStaxTemplate.class.getName());
	
	protected static DataStaxTemplateOptions classOptions = new DataStaxTemplateOptions();
	protected DataStaxTemplateOptions options = classOptions;
	
	private static Cluster cluster;
	protected static Session session;
	
	private static Map<String, PreparedStatement> insertCache = new HashMap<String, PreparedStatement>();
	private static Map<String, PreparedStatement> deleteCache = new HashMap<>();
	private static Map<String, PreparedStatement> selectCache = new HashMap<>();
	 
	public <T> void createTable(Class<T> clazz) {
		String cql = buildCreateTableCQL(clazz);
		log.info("Create Table:"+cql);
		session.execute(cql);
	}

	public <T> void dropTable(Class<T> clazz) {
		String cql = buildDropTableCQL(clazz);
		log.info("Drop Table:"+cql);
		session.execute(cql);
	}
	
	/** create KEYSPACE if no yet exists*/
	public void createKeyspace(String keyspace) {
		session.execute("CREATE KEYSPACE IF NOT EXISTS "+ keyspace +" WITH replication = {'class':'SimpleStrategy', 'replication_factor':3};");
		log.info("Finished creating " + keyspace + " keyspace.");
	}	

	/** create KEYSPACE if no yet exists*/
	public void createAndSetKeyspace(String keyspace) {
		options.setKeyspaceName(keyspace);
		createKeyspace(keyspace);
	}
	
	/** drop KEYSPACE if exists or ignore*/
	public void dropKeyspace(String keyspace) {
		getSession().execute("DROP KEYSPACE IF EXISTS " + keyspace);
		log.info("Finished dropping " + keyspace + " keyspace.");
	}	


	/**
	 * Update or create an Entity 
	 * Entity must have a property id or a property annotated with @Id
	 * @param entity
	 * @return saved entity
	 * @throws Exception
	 */
	public <E> E save(E entity) throws Exception {
		try {
			BoundStatement stmt = prepareInsert(entity);
			session.execute(stmt);
		} catch (InvalidQueryException e) {
			if (noTableError(e)) {
				createTable(entity.getClass());
				save(entity);
			}
		}
		return entity;
	}	

	/**
	 * Delete Entity by id
	 * Entity must have a property id or a property annotated with @Id
	 * @param entity
	 * @throws Exception
	 */
	public <E> void delete(E entity) throws Exception {
		try {
			EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(entity.getClass());
			Object id = entityMetadata.getIdField().getValue(entity);
			BoundStatement stmt = prepareDelete(id, entityMetadata);
			session.execute(stmt);
		} catch (InvalidQueryException e) {
			if (noTableError(e)) {
				createTable(entity.getClass());
				delete(entity);
			}
		}
			
	}	

	/**
	 * Delete Entity by id
	 * Entity must have a property id or a property annotated with @Id
	 * @param entity
	 * @throws Exception
	 */
	public <T> void delete(Object id, Class<T> clazz) throws Exception {
		try {
			EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
			BoundStatement stmt = prepareDelete(id, entityMetadata);
			session.execute(stmt);
		} catch (InvalidQueryException e) {
			if (noTableError(e)) {
				createTable(clazz);
				delete(id, clazz);
			}
		}
			
	}
	
	/**
	 * load entity by ID
	 * Entity must have a property id or a property annotated with @Id
	 * @param id
	 * @param entity class
	 * @return entity object
	 * @throws Exception
	 */
	public <T> T load(Object id, Class<T> clazz) throws Exception {
		try {
			BoundStatement stmt = prepareSelect(id, clazz);
			ResultSet rs = session.execute(stmt);
			List<T> all = populateFromResultSet(clazz, rs);
			if (all.size() > 0) {
				return all.get(0);
			}			
		} catch (InvalidQueryException e) {
			if (noTableError(e)) {
				createTable(clazz);
				return load(id, clazz);
			}
		}		
		return null;
	}

	/**
	 * load entities
	 * Entity must have a property id or a property annotated with @Id
	 * @param id
	 * @param entity class
	 * @return entity object
	 * @throws Exception
	 */
	public <T> List<T> loadFromCql(String cql, Class<T> clazz) throws Exception {
		ResultSet rs = session.execute(cql);
		List<T> all = populateFromResultSet(clazz, rs);
		return all;
	}
	
	/** 
	 * Convert ResultSet into List<T>. Create an instance of <T> for each row.
	 * To populate instance of <T> iterate through the entity metadata 
	 * and retrieve the value from the ResultSet by the field name
	 * @throws Exception */
	private <T> List<T> populateFromResultSet(Class<T> clazz, ResultSet rs) throws Exception {
		List<T> result = new ArrayList<>();
		EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
		for (Row row: rs.all()) {
			T entity = clazz.newInstance();
			for (EntityTypeMetadata.FieldData field: entityMetadata.getFields()) {
				DataType.Name dataType = field.getDataType();
				Object value = null;
				switch (dataType) {
					case BLOB:
						value = row.getBytes(field.getColumnName());
						break;
					case BOOLEAN:
						value = row.getBool(field.getColumnName());
						break;
					case TEXT:
						value = row.getString(field.getColumnName());
						break;
					case TIMESTAMP:
						value = row.getDate(field.getColumnName());
						break;
					case UUID:
						value = row.getUUID(field.getColumnName());
						break;
					case INT:
						value = row.getInt(field.getColumnName());
						break;
					case DOUBLE:
						value = row.getDouble(field.getColumnName());
						break;
					case BIGINT:
						value = row.getLong(field.getColumnName());
						break;
					case DECIMAL:
						value = row.getDecimal(field.getColumnName());
						break;
					case VARINT:
						value = row.getVarint(field.getColumnName());
						break;
					case FLOAT:
						value = row.getFloat(field.getColumnName());
						break;						
					case MAP:
						value = row.getMap(field.getColumnName(), Object.class, Object.class);
						break;
					case LIST:
						value = row.getList(field.getColumnName(), Object.class);
						break;
					case SET:
						value = row.getSet(field.getColumnName(), Object.class);
						break;
					default:
						break;
				}
				
				if (value != null) {
					field.setValue(entity, value);
				}
			}
			result.add(entity);
		}
		
		return result;
	}
	
	/**
	 * build CQL sentence to create table for the entity
	 * @param type - the entity class
	 * @return CQL string
	 */
	private <T> String buildCreateTableCQL(Class<T> clazz) {
		StringBuilder columns = new StringBuilder();
		EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz); 
		for (EntityTypeMetadata.FieldData fd: entityMetadata.getFields()) {
			if (fd.isGenericType()) {
				columns.append(fd.getColumnName()+" "+fd.getGenericDef()+", ");
			} else {
				columns.append(fd.getColumnName()+" "+fd.getDataType().toString()+", ");
			}	
		}
		String idColumn = entityMetadata.getIdField().getColumnName();
		String tableName = entityMetadata.getTableName();
		return String.format(options.getCqlForCreateTable(), options.getKeyspaceName(), tableName, columns.toString(), idColumn);
	}
	
	/**
	 * get CQL string to drop table for the entitySS
	 * @param type - the entity class
	 * @return CQL string
	 */
	private <T> String buildDropTableCQL(Class<T> clazz) {
		String tableName = EntityTypeParser.getEntityMetadata(clazz).getTableName();
		return String.format(options.getCqlForDropTable(), options.getKeyspaceName(), tableName);
	}	

	/**
	 * create BoundStatement to insert the entity into DB
	 * @param entity to be inserted
	 * @return com.datastax.driver.core.BoundStatement
	 * @throws Exception
	 */
	private <E> BoundStatement prepareInsert(E entity) throws Exception {
		Class<?> clazz = entity.getClass();
		EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
		String tableName = entityMetadata.getTableName();
		List<EntityTypeMetadata.FieldData> fields = entityMetadata.getFields(); 
		 
		
		// get prepared statement
		PreparedStatement preparedStatement = buildPreparedStatementForInsert(tableName,  entityMetadata.getFields());
		
		// create BoundStatement
		BoundStatement boundStatement = new BoundStatement(preparedStatement);
		
		// bind values
		Object[] vals = new Object[fields.size()];
		for (int i=0; i<fields.size(); i++) {
			vals[i] = fields.get(i).getValue(entity);
		}
		boundStatement.bind(vals);
		
		return boundStatement;
	}
	
	/**
	 * prepare delete CQL bound statement
	 * @param entity
	 * @return
	 * @throws Exception
	 */
	private <T> BoundStatement prepareDelete(Object id, EntityTypeMetadata entityMetadata) throws Exception {
		
		String tableName = entityMetadata.getTableName();
		
		// get or create prepared statement
		PreparedStatement preparedStatement = deleteCache.get(tableName);
		if (preparedStatement == null) {
			String idColumn = entityMetadata.getIdField().getColumnName();
			String cql = String.format(options.getCqlForDeleteEntity(), options.getKeyspaceName(), tableName, idColumn);
			preparedStatement = session.prepare(cql);
			deleteCache.put(tableName, preparedStatement);
		}
		
		// build bound statement 
		BoundStatement boundStatement = new BoundStatement(preparedStatement);
		boundStatement.bind(id);
		return boundStatement;
	}	
	
	private <T> BoundStatement prepareSelect(Object id, Class<T> clazz) throws Exception {
		EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
		String tableName = entityMetadata.getTableName();
		
		// get prepared statement
		PreparedStatement preparedStatement = selectCache.get(tableName);
		if (preparedStatement == null) {
			String idColumn = entityMetadata.getIdField().getColumnName();
			String cql = String.format(options.getCqlForLoadEntity(), options.getKeyspaceName(), tableName, idColumn);
			preparedStatement = getSession().prepare(cql);
			selectCache.put(tableName, preparedStatement);
		}
		
		// create BoundStatement
		BoundStatement boundStatement = new BoundStatement(preparedStatement);
		boundStatement.bind(id);
		return boundStatement;
	}	

	private <E> PreparedStatement buildPreparedStatementForInsert(String tableName, List<EntityTypeMetadata.FieldData> fields) {
		PreparedStatement preparedStatement = insertCache.get(tableName);
			if (preparedStatement == null) {
				StringBuilder cols = new StringBuilder();
				StringBuilder vals = new StringBuilder();
				for (EntityTypeMetadata.FieldData fd: fields) {
					cols.append(fd.getColumnName()+",");
					vals.append("?,");
				}
				// strip last comma
				cols.delete(cols.length()-1, cols.length());
				vals.delete(vals.length()-1, vals.length());
				
				String cql = String.format(options.getCqlForInsertEntity(), options.getKeyspaceName(), tableName, cols, vals);
				log.info("cql insert:"+cql);
				preparedStatement = getSession().prepare(cql);
				insertCache.put(tableName, preparedStatement);
			}
		return preparedStatement;
	}
		
	/** open connection to Cassandra cluster */
	public static void connect() {
		String node = classOptions.getNode();
		cluster = Cluster.builder().addContactPoint(node).build();
		setSession(cluster.connect());
		
		// print out connection info
		Metadata metadata = cluster.getMetadata();
		log.info("Connected to cluster: " + metadata.getClusterName());
		for (Host host : metadata.getAllHosts()) {
			log.info(String.format("Datatacenter: %s; Host: %s; Rack: %s \n",
					host.getDatacenter(), host.getAddress(), host.getRack()));
		}
	}
	
	/** shutdown connection to cassandra cluster */
	public static void disconnect() {
		cluster.shutdown();
	}

	public static Session getSession() {
		return session;
	}

	public static void setSession(Session sesion) {
		session = sesion;
	}
	
	public static void setClassOptions(DataStaxTemplateOptions opt) {
		classOptions = opt;
	}

	public void setOptions(DataStaxTemplateOptions opt) {
		options = opt;
	}
	
	private boolean noTableError(InvalidQueryException e) {
		if (e.getMessage().contains("unconfigured columnfamily")) {
			return true;
		}
		return false;
		
	}
	
}