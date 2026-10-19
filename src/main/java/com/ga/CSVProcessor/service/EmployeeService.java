package com.ga.CSVProcessor.service;

import com.ga.CSVProcessor.model.Employee;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
public class EmployeeService {
    private final SalaryCalculator salaryCalculator;
    private final CsvReaderService csvReaderService;

    public EmployeeService(SalaryCalculator salaryCalculator, CsvReaderService csvReaderService) {
        this.salaryCalculator = salaryCalculator;
        this.csvReaderService = csvReaderService;
    }

    public List<Employee> processEmployees() {
        List<Employee> employees = null;
        try {
            employees = csvReaderService.readEmployees();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (Employee employee : employees) {
            BigDecimal newSalary = salaryCalculator.calculateNewSalary(employee);
            employee.setNewSalary(newSalary);
        }
        return employees;
    }
}
