package com.datastax.driver.mapping.entity;

import javax.persistence.EmbeddedId;

public class PrimaryKey {
	@EmbeddedId
	private ClusteringKey pk;

	public ClusteringKey getPk() {
		return pk;
	}

	public void setPk(ClusteringKey pk) {
		this.pk = pk;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((pk == null) ? 0 : pk.hashCode());
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
		PrimaryKey other = (PrimaryKey) obj;
		if (pk == null) {
			if (other.pk != null)
				return false;
		} else if (!pk.equals(other.pk))
			return false;
		return true;
	}

}