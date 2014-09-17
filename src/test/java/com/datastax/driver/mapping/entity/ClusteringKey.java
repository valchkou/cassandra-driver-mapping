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
}
