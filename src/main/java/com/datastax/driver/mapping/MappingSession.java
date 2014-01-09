package com.datastax.driver.mapping;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;

public class MappingSession {
	
	private Session session;
	private String keyspace;
//	private static Map<String, PreparedStatement> insertCache = new HashMap<>();
	private static Map<String, PreparedStatement> deleteCache = new HashMap<>();
	private static Map<String, PreparedStatement> selectCache = new HashMap<>();
	
	public MappingSession(String keyspace, Session session) {
		this.session = session;
		this.keyspace = keyspace;
	}
	
	/**
	 * Return the persistent instance of the given entity class with the given identifier, 
	 * or null if there is no such persistent instance
	 * 
	 * @param clazz - a persistent class
	 * @param id - an identifier
	 * @return a persistent instance or null
	 * @throws Exception
	 */
	public <T> T get(Class<T> clazz, Object id) throws Exception {
		BoundStatement bs = prepareSelect(clazz, id);
		List<T> all = populateFromResultSet(clazz, session.execute(bs));
		if (all.size() > 0) {
			return all.get(0);
		}	
		return null;
	}

	public <E> void delete(E entity) throws Exception {
		BoundStatement bs = prepareDelete(entity);
		session.execute(bs);
	}
	
	/**
	 * Persist the given instance 
	 * Entity must have a property id or a property annotated with @Id
	 * @param entity - an instance of a persistent class
	 * @return saved instance
	 * @throws Exception
	 */
	public <E> E save(E entity) throws Exception {
		Statement insert = prepareInsert(entity);
		session.execute(insert);
		return entity;
	}	

	/**
	 * create Statement to persist the instance in Cassandra
	 * @param entity to be inserted
	 * @return com.datastax.driver.core.BoundStatement
	 * @throws Exception
	 */
	private <E> Statement prepareInsert(E entity) throws Exception {
		Class<?> clazz = entity.getClass();
		EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
		String table = entityMetadata.getTableName();
		List<EntityTypeMetadata.FieldData> fields = entityMetadata.getFields(); 

		String[] columns = new String[fields.size()];
		Object[] values = new Object[fields.size()];
		for (int i=0; i<fields.size(); i++) {
			columns[i] = fields.get(i).getColumnName();
			values[i] = fields.get(i).getValue(entity);
		}		
		// TODO : create bound statement
		return insertInto(keyspace, table).values(columns, values);
	}	
	
	/**
	 * Prepare BoundStatement to select row by id
	 */
	private <T> BoundStatement prepareSelect(Class<T> clazz, Object id) throws Exception {
		EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(clazz);
		String table = entityMetadata.getTableName();
		
		// get prepared statement
		PreparedStatement ps = selectCache.get(table);
		if (ps == null) {
			String idColumn = entityMetadata.getIdField().getColumnName();
			Statement select = select().all().from(keyspace, table).where(eq(idColumn, QueryBuilder.bindMarker()));
	        ps = session.prepare(select.toString());
			selectCache.put(table, ps);
		}
		
		// bind parameters
		BoundStatement bs = ps.bind(id);
		return bs;
	}
	
	private <E> BoundStatement prepareDelete(E entity) throws Exception {
		EntityTypeMetadata entityMetadata = EntityTypeParser.getEntityMetadata(entity.getClass());
		Object id = entityMetadata.getIdField().getValue(entity);
		String table = entityMetadata.getTableName();
		
		PreparedStatement ps = deleteCache.get(table);
		if (ps == null) {
			String idColumn = entityMetadata.getIdField().getColumnName();
			Statement delete = QueryBuilder.delete().from(keyspace, table).where(eq(idColumn, QueryBuilder.bindMarker()));
			ps = session.prepare(delete.toString());
			deleteCache.put(table, ps);
		}
		
		// bind parameters
		BoundStatement bs = ps.bind(id);
		return bs;
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
				try {
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
				} catch (Exception ex) {
					// field is not properly mapped, log debug and skip the error
				}
			}
			result.add(entity);
		}
		
		return result;
	}


}
