/*
 *      Copyright (C) 2014 Eugene Valchkou.
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

import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Id;
import javax.persistence.Table;

@Table(name="entity")
public class Entity {
	@Id
	private java.util.UUID id;
	private List<String> cats;
	private Set<Date> dogs;
	private Map<String, BigInteger> pets;
		
	// public getters/setters ...

	public java.util.UUID getId() {
		return id;
	}
	public void setId(java.util.UUID id) {
		this.id = id;
	}
	public List<String> getCats() {
		return cats;
	}
	public Set<Date> getDogs() {
		return dogs;
	}
	public Map<String, BigInteger> getPets() {
		return pets;
	}
	public void setCats(List<String> cats) {
		this.cats = cats;
	}
	public void setDogs(Set<Date> dogs) {
		this.dogs = dogs;
	}
	public void setPets(Map<String, BigInteger> pets) {
		this.pets = pets;
	}


}
