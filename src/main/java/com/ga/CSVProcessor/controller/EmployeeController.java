package com.ga.CSVProcessor.controller;

import com.ga.CSVProcessor.model.Employee;
import com.ga.CSVProcessor.service.CsvReaderService;
import com.ga.CSVProcessor.service.EmployeeService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final CsvReaderService csvReaderService;
    private final EmployeeService employeeService;

    public EmployeeController(CsvReaderService csvReaderService ,EmployeeService employeeService) {
        this.csvReaderService = csvReaderService;
        this.employeeService = employeeService;
    }

    @PostMapping
    public List<Employee> processEmployee() {
        return employeeService.processEmployees();
    }

    @GetMapping
    public List<Employee> getEmployees() throws IOException {
        return csvReaderService.readEmployees();
    }
}
