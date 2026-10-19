package com.ga.CSVProcessor.service;

import com.ga.CSVProcessor.model.Employee;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class SalaryCalculator {

    public BigDecimal calculateNewSalary(Employee employee){

    }

    public boolean isProjectPercentageBiggerThan60(double percentage){
        return (percentage >= 0.6);

    }
    public boolean checkJoinedDate(LocalDate joinedDate){
        return (!LocalDate.now().isBefore(joinedDate.plusYears(1)));
    }
}
