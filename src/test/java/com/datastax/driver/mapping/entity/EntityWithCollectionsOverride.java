/*
 *   Copyright (C) 2014 Eugene Valchkou.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.datastax.driver.mapping.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.datastax.driver.mapping.annotation.CollectionType;

@Entity
@Table(name="test_collections_override") 
public class EntityWithCollectionsOverride {
	@Id
	private UUID id = UUID.randomUUID();
	
	@Column(name="pips")	
	@CollectionType(TreeMap.class)
	private Map<String, BigDecimal> rates;
	
	@CollectionType(TreeSet.class)
	private Set<String> refs;
	
	@CollectionType(LinkedList.class)
	private List<Integer> trades;
	
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
		EntityWithCollectionsOverride other = (EntityWithCollectionsOverride) obj;
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
	public Set<String> getRefs() {
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
	public void setRefs(Set<String> refs) {
		this.refs = refs;
	}
	public void setTrades(List<Integer> trades) {
		this.trades = trades;
	}

   public void addRef(String ref) {
       if (refs == null) {
           refs = new HashSet<String>();
       }
       refs.add(ref);
   }
	
   public void addTrade(Integer trade) {
       if (trades == null) {
    	   trades = new ArrayList<Integer>();
       }
       trades.add(trade);
   } 
   
	public void addRate(String key, BigDecimal val) {
		if (rates == null) {
			rates = new HashMap<String, BigDecimal>();
		}
		rates.put(key, val);
	}   
}
