/*
 *      Copyright (C) 2015 Eugene Valchkou.
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
package com.datastax.driver.mapping.schemasync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fine tuning to adjust entity synchronization rules
 * 
 * Rules can be defined globally for all entities and per specific entity Usage:
 * SyncOptions.withGlobalOptions().add()
 * 
 * If Entity options are defined the global will be ignored for this Entity.
 *
 */
public class SyncOptions {

	private Map<Class<?>, List<SyncOptionTypes>> entityOptions = new HashMap<Class<?>, List<SyncOptionTypes>>();
	private List<SyncOptionTypes> globalOptions = new ArrayList<SyncOptionTypes>();

	public SyncOptions() {}

	public static SyncOptions withOptions() {
		return new SyncOptions();
	}

	/**
	 * Add Global option. Will affect all entities
	 * 
	 * @param type SyncOptionTypes
	 * @return this
	 */
	public SyncOptions add(SyncOptionTypes type) {
		globalOptions.add(type);
		return this;
	}

	/**
	 * Add Entity specific option
	 * 
	 * @param clazz Entity class
	 * @param type SyncOptionTypes
	 * @return this
	 */
	public SyncOptions add(Class<?> clazz, SyncOptionTypes type) {
		List<SyncOptionTypes> opts = entityOptions.get(clazz);
		if (opts == null) {
			opts = new ArrayList<SyncOptionTypes>();
			entityOptions.put(clazz, opts);
		}
		opts.add(type);
		return this;
	}

	/**
	 * Short hand to tell that this Entity should be synchronized with Cassandra
	 * 
	 * @param clazz Entity class
	 * @return this
	 */
	public SyncOptions doSync(Class<?> clazz) {
		entityOptions.put(clazz, new ArrayList<SyncOptionTypes>());
		return this;
	}

	/**
	 * Short hand to mark this Entity to not synchronize with Cassandra
	 * 
	 * @param clazz Entity class
	 * @return this
	 */	
	public SyncOptions doNotSync(Class<?> clazz) {
		return add(clazz, SyncOptionTypes.DoNotSync);
	}
	
	public List<SyncOptionTypes> getGlobalOptions() {
		return globalOptions;
	}
	
	/**
	 * Short hand to not synchronize with Cassandra
	 *
	 * @return this
	 */	
	public SyncOptions doNotSync() {
		return add(SyncOptionTypes.DoNotSync);
	}

	public List<SyncOptionTypes> getOptions(Class<?> clazz) {
		List<SyncOptionTypes> opts = entityOptions.get(clazz);
		if (opts == null) {
			return globalOptions;
		}
		return opts;
	}
	
	public Map<Class<?>, List<SyncOptionTypes>> getEntityOptions() {		
		return entityOptions;
	}
	
	public boolean isDoNotSync(Class<?> clazz) {
		return getOptions(clazz).contains(SyncOptionTypes.DoNotSync);
	}

}
