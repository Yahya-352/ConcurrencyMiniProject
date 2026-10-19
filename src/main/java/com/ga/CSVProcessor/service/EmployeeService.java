package com.ga.CSVProcessor.service;

import com.ga.CSVProcessor.model.Employee;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {

    private final SalaryCalculator salaryCalculator;
    private final CsvReaderService csvReaderService;

    public EmployeeService(SalaryCalculator salaryCalculator, CsvReaderService csvReaderService) {
        this.salaryCalculator = salaryCalculator;
        this.csvReaderService = csvReaderService;
    }

    public List<Employee> processEmployees(List<Employee> employees) {
        for (Employee employee : employees) {
            salaryCalculator.calculateNewSalary(employee);
        }
        return employees;
    }
}
