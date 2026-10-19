package com.ga.CSVProcessor.service;

import com.ga.CSVProcessor.enums.Role;
import com.ga.CSVProcessor.model.Employee;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

@Service
public class SalaryCalculator {
    public BigDecimal calculateNewSalary(Employee employee){
        double increaseRate = 0;
        if(isProjectPercentageBiggerThan60(employee.getProjectCompletedPercentage())){
            if(checkJoinedDateIsGreaterThan1Year(employee.getDateJoined())){
                int year = Period.between(employee.getDateJoined(), LocalDate.now()).getYears();
                increaseRate += 0.02 * year;
            }
            switch (employee.getRole()) {
                case MANAGER:
                    increaseRate += 0.02;
                    break;
                case DIRECTOR:
                    increaseRate += 0.05;
                    break;
                case EMPLOYEE:
                    increaseRate += 0.01;
                    break;
            }
        }
        return employee.getSalary()
                .multiply(BigDecimal.valueOf(increaseRate))
                .add(employee.getSalary()).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public boolean isProjectPercentageBiggerThan60(double percentage){
        return (percentage >= 0.6);
    }
    public boolean checkJoinedDateIsGreaterThan1Year(LocalDate joinedDate){
        return (!LocalDate.now().isBefore(joinedDate.plusYears(1)));
    }
}
