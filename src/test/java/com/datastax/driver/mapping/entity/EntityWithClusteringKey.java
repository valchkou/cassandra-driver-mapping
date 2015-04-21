package com.datastax.driver.mapping.entity;

import java.util.Date;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "test_entity_clustering")
public class EntityWithClusteringKey {
	
	@EmbeddedId
	private ClusteringKey key;

	private long timestamp;
	private Date asof;
	
	public ClusteringKey getKey() {
		return key;
	}
	public void setKey(ClusteringKey key) {
		this.key = key;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	public Date getAsof() {
		return asof;
	}
	public void setAsof(Date asof) {
		this.asof = asof;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((asof == null) ? 0 : asof.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EntityWithClusteringKey other = (EntityWithClusteringKey) obj;
		if (asof == null) {
			if (other.asof != null)
				return false;
		} else if (!asof.equals(other.asof))
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (timestamp != other.timestamp)
			return false;
		return true;
	}

}
