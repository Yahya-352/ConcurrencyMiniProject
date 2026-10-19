package com.ga.CSVProcessor.controller;


import com.ga.CSVProcessor.model.Employee;
import com.ga.CSVProcessor.service.CsvReaderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    private final CsvReaderService csvReaderService;
    public EmployeeController(CsvReaderService csvReaderService) {
        this.csvReaderService = csvReaderService;
    }

    @GetMapping
    public List<Employee> getEmployees() throws IOException {
        return csvReaderService.readEmployees();
    }
}
