package com.ga.CSVProcessor.model;

import com.ga.CSVProcessor.enums.Role;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Employee {
    private int id;
    private String name;
    private BigDecimal salary;
    private LocalDate dateJoined;
    private Role role;
    private double projectCompletedPercentage;
    private BigDecimal newSalary;

    public Employee(int id, String name, BigDecimal salary, LocalDate dateJoined, Role role ,
                    double projectCompletedPercentage) {
        this.id = id;
        this.name = name;
        this.salary = salary;
        this.role = role;
        this.dateJoined = dateJoined;
        this.projectCompletedPercentage = projectCompletedPercentage;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public BigDecimal getNewSalary() {
        return newSalary;
    }

    public double getProjectCompletedPercentage() {
        return projectCompletedPercentage;
    }

    public LocalDate getDateJoined() {
        return dateJoined;
    }

    public Role getRole() {
        return role;
    }

    public void setNewSalary(BigDecimal newSalary) {
        this.newSalary = newSalary;
    }
}
