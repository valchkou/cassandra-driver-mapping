package com.datastax.driver.mapping.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="test_collections")
public class EntityWithCollections {
	@Id
	private UUID id = UUID.randomUUID();
	private Map<String, BigDecimal> rates = new HashMap<String, BigDecimal>();
	private Set<Integer> refs = new HashSet<Integer>();
	private List<Integer> trades = new ArrayList<Integer>();
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((rates == null) ? 0 : rates.hashCode());
		result = prime * result + ((refs == null) ? 0 : refs.hashCode());
		result = prime * result + ((trades == null) ? 0 : trades.hashCode());
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
		EntityWithCollections other = (EntityWithCollections) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (rates == null) {
			if (other.rates != null)
				return false;
		} else if (!rates.equals(other.rates))
			return false;
		if (refs == null) {
			if (other.refs != null)
				return false;
		} else if (!refs.equals(other.refs))
			return false;
		if (trades == null) {
			if (other.trades != null)
				return false;
		} else if (!trades.equals(other.trades))
			return false;
		return true;
	}
	public UUID getId() {
		return id;
	}
	public Map<String, BigDecimal> getRates() {
		return rates;
	}
	public Set<Integer> getRefs() {
		return refs;
	}
	public List<Integer> getTrades() {
		return trades;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	public void setRates(Map<String, BigDecimal> rates) {
		this.rates = rates;
	}
	public void setRefs(Set<Integer> refs) {
		this.refs = refs;
	}
	public void setTrades(List<Integer> trades) {
		this.trades = trades;
	}
}
