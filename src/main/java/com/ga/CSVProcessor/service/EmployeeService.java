package com.ga.CSVProcessor.service;

import com.ga.CSVProcessor.model.Employee;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class EmployeeService {

    private final SalaryCalculator salaryCalculator;
    private final CsvReaderService csvReaderService;
    ReentrantLock lock = new ReentrantLock();
    public EmployeeService(SalaryCalculator salaryCalculator, CsvReaderService csvReaderService) {
        this.salaryCalculator = salaryCalculator;
        this.csvReaderService = csvReaderService;
    }

    public List<Employee> processEmployees() {
        List<Employee> employees = null;
        ArrayList<Employee> raisedEmployeesList = new ArrayList<>();
        try {
            employees = csvReaderService.readEmployees();

            ExecutorService executorService = Executors.newFixedThreadPool(5);
            AtomicInteger raisedCount = new AtomicInteger(0);
            AtomicInteger notRaisedCount = new AtomicInteger(0);

            for (Employee employee : employees) {
                executorService.submit(() -> {
                    BigDecimal newSalary = salaryCalculator.calculateNewSalary(employee);
                    employee.setNewSalary(newSalary);

                    if(newSalary.compareTo(employee.getSalary()) > 0){
                        raisedCount.incrementAndGet(); // checking how many got raise
                        lock.lock();
                        try{
                            raisedEmployeesList.add(employee);
                        }catch (Exception e){
                            e.printStackTrace();
                        }finally {
                            lock.unlock();
                        }

                    }else{
                        notRaisedCount.incrementAndGet(); //checking how many didnt get raise
                    }

                });
            }
            executorService.shutdown();
            executorService.awaitTermination(1 , TimeUnit.MINUTES);
            System.out.println("Raised: " + raisedCount.get() + "\nNot raised: " + notRaisedCount.get());
            System.out.println("Raised(ARRAYLIST SIZE): " + raisedEmployeesList.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return raisedEmployeesList;
    }
}
