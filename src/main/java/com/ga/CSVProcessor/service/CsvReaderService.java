package com.ga.CSVProcessor.service;

import com.ga.CSVProcessor.enums.Role;
import com.ga.CSVProcessor.model.Employee;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvReaderService {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");

    public List<Employee> readEmployees() throws IOException {
        ArrayList<Employee> employees = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader
                (new ClassPathResource("employees.csv").getInputStream()));
        reader.readLine();
        String line;
        while((line = reader.readLine())!= null) {
            String employeeCsvData[] = line.split(",");
            int id = Integer.parseInt(employeeCsvData[0]);
            String name = employeeCsvData[1];
            BigDecimal salary = new BigDecimal(employeeCsvData[2]);
            LocalDate dateJoined = LocalDate.parse(employeeCsvData[3],formatter);
            Role role = Role.valueOf(employeeCsvData[4].toUpperCase());
            double projectCompletedPercentage = Double.parseDouble(employeeCsvData[5]);
            Employee employee = new Employee(id ,name ,salary
                    ,dateJoined , role , projectCompletedPercentage);
            employees.add(employee);
    }
        reader.close();
        return employees;
    }
}
