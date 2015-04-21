package com.datastax.driver.mapping.entity;

public class ClusteringKey {
    private String user;
    private int expense_id;
    public int getExpense_id() {
        return expense_id;
    }
    public void setExpense_id(int expense_id) {
        this.expense_id = expense_id;
    }
    public String getUser() {
        return user;
    }
    public void setUser(String user) {
        this.user = user;
    }
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + expense_id;
		result = prime * result + ((user == null) ? 0 : user.hashCode());
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
		ClusteringKey other = (ClusteringKey) obj;
		if (expense_id != other.expense_id)
			return false;
		if (user == null) {
			if (other.user != null)
				return false;
		} else if (!user.equals(other.user))
			return false;
		return true;
	}
}
