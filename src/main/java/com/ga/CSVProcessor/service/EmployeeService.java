package com.ga.CSVProcessor.service;

import com.ga.CSVProcessor.model.Employee;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
            ExecutorService executorService = Executors.newFixedThreadPool(5);
            for (Employee employee : employees) {
                executorService.submit(() -> {
                    BigDecimal newSalary = salaryCalculator.calculateNewSalary(employee);
                    employee.setNewSalary(newSalary);
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(1 , TimeUnit.MINUTES);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return employees;
    }
}
