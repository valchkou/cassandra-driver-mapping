package com.datastax.driver.mapping.entity;

import java.util.UUID;

import javax.persistence.Column;

public class SimpleKey {

	private String name;
	private int rank;
	@Column(columnDefinition="timeuuid")
	private UUID t1;
	@Column(columnDefinition="timeuuid")
	private UUID t2;

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

    public UUID getT1() {
        return t1;
    }

    public void setT1(UUID t1) {
        this.t1 = t1;
    }

    public UUID getT2() {
        return t2;
    }

    public void setT2(UUID t2) {
        this.t2 = t2;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + rank;
        result = prime * result + ((t1 == null) ? 0 : t1.hashCode());
        result = prime * result + ((t2 == null) ? 0 : t2.hashCode());
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
        SimpleKey other = (SimpleKey) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (rank != other.rank)
            return false;
        if (t1 == null) {
            if (other.t1 != null)
                return false;
        } else if (!t1.equals(other.t1))
            return false;
        if (t2 == null) {
            if (other.t2 != null)
                return false;
        } else if (!t2.equals(other.t2))
            return false;
        return true;
    }



}
