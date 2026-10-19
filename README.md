# CSV Processor

A Spring Boot application that reads employee data from a CSV file and calculates each employee's new salary concurrently, using a thread pool, a `ReentrantLock` and atomic variables.

The [Project Report](#project-report) at the end covers the implementation details, the problems faced during development, and the outcomes.

---

## 1. Project Setup

### Requirements
- **Java 21**
- No Maven install needed: the project includes the Maven wrapper (`mvnw` / `mvnw.cmd`)

### Tech stack
| | |
|---|---|
| Framework | Spring Boot 4.1.1 (`spring-boot-starter-webmvc`) |
| Build | Maven (wrapper) |
| Language | Java 21 |

### Run the application
```bash
# macOS / Linux / Git Bash
./mvnw spring-boot:run

# Windows (cmd / PowerShell)
mvnw.cmd spring-boot:run
```
The app starts on **http://localhost:8080**.

### Run the tests
```bash
./mvnw test
```

### Input data
The data is read from `src/main/resources/employees.csv` (30 employees). The first line is a header and is skipped.

```csv
id,name,salary,joinedDate,role,projectCompletion
1,Alice,52000,5/12/2019,Employee,0.8
```

| Column | Format | Example |
|---|---|---|
| `id` | integer | `1` |
| `name` | text | `Alice` |
| `salary` | decimal number | `52000` |
| `joinedDate` | `M/d/yyyy` | `5/12/2019` |
| `role` | `Employee`, `Manager` or `Director` (any case) | `Employee` |
| `projectCompletion` | fraction from 0 to 1 | `0.8` (= 80%) |

---

## 2. API

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/employees` | Returns all employees as read from the CSV (`newSalary` is `null`) |
| `POST` | `/api/employees` | Processes all employees concurrently and returns the employees who received a raise, with their `newSalary` |

### Example
```bash
curl -X POST http://localhost:8080/api/employees
```
```json
[
  {
    "id": 1,
    "name": "Alice",
    "salary": 52000,
    "dateJoined": "2019-05-12",
    "role": "EMPLOYEE",
    "projectCompletedPercentage": 0.8,
    "newSalary": 59800.00
  }
]
```
Console output:
```
Raised: 21
Not raised: 9
Raised(ARRAYLIST SIZE): 21
```
> The order of employees in the response can change between calls, because the threads finish in a different order each time.

---

## 3. Salary Rules

| Rule | Increase |
|---|---|
| Project completion **below 60%** | **No increase at all** (the other rules are skipped) |
| Worked **at least 1 full year** | **+2% for each full year** worked |
| Role **Director** | +5% |
| Role **Manager** | +2% |
| Role **Employee** | +1% |

```
newSalary = salary + salary × increaseRate      (rounded to 2 decimals, HALF_UP)
```

**Example: Alice** (Employee, 80% completion, joined 12 May 2019, 7 full years by Oct 2026)
`increaseRate = 7 × 2% + 1% = 15%`, so `52000 × 1.15 = 59800.00`

---

## 4. Project Structure

```
src/main/java/com/ga/CSVProcessor/
├── CsvProcessorApplication.java      Spring Boot entry point
├── controller/
│   └── EmployeeController.java       REST endpoints
├── enums/
│   └── Role.java                     MANAGER, EMPLOYEE, DIRECTOR
├── model/
│   └── Employee.java                 Employee data + new salary
└── service/
    ├── CsvReaderService.java         Reads and parses the CSV file
    ├── SalaryCalculator.java         Salary increase rules
    └── EmployeeService.java          Concurrent processing (thread pool + lock + atomics)
```

---

## 5. Class & Method Documentation

### `CsvProcessorApplication`
Standard Spring Boot entry point (`@SpringBootApplication`).

### `controller.EmployeeController`
`@RestController` mapped to `/api/employees`.

| Method | Mapping | Description |
|---|---|---|
| `List<Employee> processEmployee()` | `POST` | Calls `EmployeeService.processEmployees()` and returns the employees who got a raise |
| `List<Employee> getEmployees()` | `GET` | Calls `CsvReaderService.readEmployees()` and returns the raw CSV data. Throws `IOException` if the file can't be read |

### `enums.Role`
The employee roles: `MANAGER`, `EMPLOYEE`, `DIRECTOR`. The CSV value is converted with `Role.valueOf(value.toUpperCase())`.

### `model.Employee`
A plain Java object holding one employee.

| Field | Type | Description |
|---|---|---|
| `id` | `int` | Employee id |
| `name` | `String` | Employee name |
| `salary` | `BigDecimal` | Current salary (`BigDecimal` to avoid floating-point money errors) |
| `dateJoined` | `LocalDate` | Date the employee joined |
| `role` | `Role` | Employee role |
| `projectCompletedPercentage` | `double` | Project completion, from 0 to 1 |
| `newSalary` | `BigDecimal` | Calculated salary; `null` until processed |

- The constructor takes every field except `newSalary`.
- There are getters for all fields; the only setter is `setNewSalary(BigDecimal)`.

### `service.CsvReaderService`
`@Service` that loads the CSV from the classpath.

| Method | Description |
|---|---|
| `List<Employee> readEmployees() throws IOException` | Opens `employees.csv` via `ClassPathResource`, skips the header, splits each line on `,`, parses each column (date with pattern `M/d/yyyy`) and returns a new list of `Employee` objects. Each call reads the file again and returns new objects |

### `service.SalaryCalculator`
`@Service` with the salary rules. It has **no fields**, so it is stateless and safe to call from many threads at the same time.

| Method | Description |
|---|---|
| `BigDecimal calculateNewSalary(Employee employee)` | Applies the rules from section 3 and returns the new salary, rounded to 2 decimals. Returns the old salary when the employee doesn't qualify |
| `boolean isProjectPercentageBiggerThan60(double percentage)` | `true` if `percentage >= 0.6` |
| `boolean checkJoinedDateIsGreaterThan1Year(LocalDate joinedDate)` | `true` if at least one full year has passed since `joinedDate` |

### `service.EmployeeService`
`@Service` that processes employees concurrently.

| Member | Description |
|---|---|
| `ReentrantLock lock` | Protects the shared `raisedEmployeesList` so only one thread adds to it at a time |
| `List<Employee> processEmployees()` | See below |

**`processEmployees()` step by step**
1. Reads all employees with `CsvReaderService`.
2. Creates a **fixed thread pool of 5 threads** (`Executors.newFixedThreadPool(5)`).
3. Creates, for this call only:
   - `raisedEmployeesList`: an `ArrayList` shared by all tasks, protected by the lock
   - `raisedCount` / `notRaisedCount`: `AtomicInteger` counters
4. Submits **one task per employee**. Each task:
   - calculates and stores the new salary (no lock needed: each task only touches its own `Employee`)
   - if the salary increased: increments `raisedCount`, then adds the employee to the list inside `lock.lock()` / `try` / `finally { lock.unlock(); }`
   - otherwise: increments `notRaisedCount`
5. Calls `shutdown()` (accept no new tasks), then `awaitTermination(1, MINUTES)` to **wait until every task has finished**.
6. Prints the counts and the list size, then returns the list.

Exceptions: `IOException` (CSV) and `InterruptedException` (while waiting) are wrapped in a `RuntimeException`.

---

## 6. Concurrency Summary

| Tool | Used for | Why |
|---|---|---|
| `ExecutorService` (fixed pool of 5) | Running the salary calculations in parallel | Reuses 5 threads instead of creating one thread per employee |
| `ReentrantLock` | Adding to the shared `ArrayList` | `ArrayList` is not thread-safe; several steps (`add` + resize) must happen as one unit |
| `AtomicInteger` | Raised / not-raised counters | A single counter only needs an atomic increment, which is faster than a lock |
| `shutdown()` + `awaitTermination()` | Waiting for the results | Guarantees that every task is finished and its changes are visible before the results are used |

---

# Project Report

## 7. Goal

Build a Spring Boot application that reads employee data from a CSV file, calculates each employee's new salary based on project completion, years worked and role, and does the processing **concurrently** while keeping the shared data **thread-safe**.

## 8. Implementation

### 8.1 Reading the CSV
`CsvReaderService` reads `employees.csv` from the classpath line by line, skips the header, and turns each line into an `Employee` object. Salaries are stored as `BigDecimal` to avoid rounding errors with money, and dates are parsed into `LocalDate`.

### 8.2 Salary rules
`SalaryCalculator` builds an `increaseRate`:
- If project completion is **below 60%**, the rate stays at 0 and nothing else is applied.
- Otherwise:
  - **+2% per full year** worked, only if the employee has worked at least one full year
  - **+5%** Director, **+2%** Manager, **+1%** Employee

The new salary is `salary + salary × increaseRate`, rounded to 2 decimals.

`SalaryCalculator` has no fields (it's stateless), so all threads can use the same instance safely.

### 8.3 Concurrent processing
`EmployeeService.processEmployees()` uses:

| Technique | Where | Purpose |
|---|---|---|
| **Thread pool** | `Executors.newFixedThreadPool(5)` | One task per employee, run on 5 reused threads |
| **ReentrantLock** | Around `raisedEmployeesList.add(...)` | Only one thread at a time can change the shared list |
| **Atomic variables** | `AtomicInteger raisedCount / notRaisedCount` | Thread-safe counting without a lock |
| **Waiting for tasks** | `shutdown()` + `awaitTermination()` | Results are only used after every task has finished |

What needed protection, and what didn't:

| Data | Shared between threads? | Protection |
|---|---|---|
| Each `Employee` object | No: each task only changes its own employee | None needed |
| `SalaryCalculator` | Yes, but it has no state | None needed |
| `raisedCount`, `notRaisedCount` | Yes | `AtomicInteger` |
| `raisedEmployeesList` | Yes | `ReentrantLock` |

## 9. Problems Faced and How They Were Solved

### Problem 1: The list was never created
**Code:** `ArrayList<Employee> raisedEmployeesList;` was declared as a field but never initialized.

**What happened:** the first `add()` threw a `NullPointerException`. It didn't show up anywhere, because `ExecutorService.submit()` catches exceptions thrown inside tasks and keeps them in the `Future`, which was never checked. The endpoint returned `null` without any visible error.

**Fix:** create the list with `new ArrayList<>()`.

**Lesson:** exceptions inside `submit()` are silent. A `try`/`catch` with `printStackTrace()` was later added inside the task so errors are at least logged.

### Problem 2: The list was a field on a Spring singleton
**What happened:** Spring creates **one** `EmployeeService` shared by every request. As a field, the list would collect employees from every POST call (growing each time), and concurrent requests would write into the same list.

**Fix:** made `raisedEmployeesList` a **local variable** inside `processEmployees()`, so each call gets its own list.

### Problem 3: `ArrayList` is not thread-safe
**What happened:** 5 pool threads call `add()` on the same list. Internally `add()` is several steps (check the size, grow the array, write the element, increase `size`). Two threads doing this at the same time can lose an employee, leave a `null` in the list, or throw `ArrayIndexOutOfBoundsException`.

**Fix:** protect the `add()` with a `ReentrantLock`. This took a few attempts (problems 4 to 6).

### Problem 4: The lock was on the wrong thread
**Code:** `lock.lock()` at the start of `processEmployees()` and `lock.unlock()` in its `finally`.

**What happened:** both calls ran on the **request thread**, while `add()` ran on the **pool threads**, which never called `lock()`. So the list was still unprotected. The only effect was that two POST requests couldn't run at the same time.

**Lesson:** a lock only protects code where **every thread that touches the shared data** calls `lock()` first.

### Problem 5: Lock and unlock on different threads
**Code:** `lock.lock()` moved inside the task (pool thread), but `lock.unlock()` stayed in the request thread's `finally`.

**What happened (analysis):**
1. The first pool thread locked and never unlocked.
2. The other 4 pool threads blocked on `lock()` forever.
3. Because the lock is **reentrant**, the thread that owned it could keep locking again (the hold count goes up), so it kept working alone.
4. `awaitTermination` waited the full minute, because 4 threads never finished.
5. The request thread then called `unlock()` on a lock it didn't own, which throws `IllegalMonitorStateException`.

**Lesson:** a `ReentrantLock` belongs to the thread that locked it. `lock()` and `unlock()` must run **on the same thread**.

### Problem 6: Unlock without `finally`
**Code:** `lock.lock(); list.add(employee); lock.unlock();` inside the task.

**What happened:** it worked, but if `add()` ever threw, `unlock()` would be skipped and the lock would stay held forever, causing the same hang as problem 5.

**Fix:** the standard pattern, with `lock()` placed **before** the `try` so that a failed `lock()` never leads to an `unlock()` on a lock that isn't held:
```java
lock.lock();
try {
    raisedEmployeesList.add(employee);
} catch (Exception e) {
    e.printStackTrace();
} finally {
    lock.unlock();
}
```

### Problem 7: The race condition was invisible
**Experiment:** removed the lock to see the race.

**What happened:** everything still looked correct. With only 30 employees and very fast tasks, two threads are almost never inside `add()` at the same moment.

**Lesson:** race conditions are dangerous because wrong code **usually works**. Seeing the race needs much more data (for example, processing the list thousands of times) and comparing `raisedEmployeesList.size()` with `raisedCount`.

### Problem 8: Removing `awaitTermination`
**Experiment:** removed `executorService.awaitTermination(...)`, keeping only `shutdown()`.

**Actual console output (3 POST calls):**
```
Raised: 0
Not raised: 2
Raised(ARRAYLIST SIZE): 0
Raised: 0
Not raised: 0
Raised(ARRAYLIST SIZE): 1
Raised: 1
Not raised: 0
Raised(ARRAYLIST SIZE): 1
```

**What happened:**
- `shutdown()` only stops new tasks from being accepted. It **does not wait**.
- The request thread printed while the pool threads had barely started, so each block is a snapshot taken in the middle of the work (`Raised + Not raised` should be 30).
- In the second call the count was 0 but the list size was 1: a task finished **between** the two `println` lines.
- The POST response still looked correct. The method returns a **reference** to the same list, and Spring converted it to JSON a moment later, by which time the tasks had usually finished. That was luck from timing. With slower tasks the response would be partial, or could fail with `ConcurrentModificationException`.

**Fix:** keep `awaitTermination()`. Always wait for the threads before using their results.

## 10. Outcomes

With the final code, the POST endpoint was called 4 times on the provided CSV (30 employees). Every call gave the same result:

```
Raised: 21
Not raised: 9
Raised(ARRAYLIST SIZE): 21
```

- Total `21 + 9 = 30`: every employee was processed.
- The list size always matches `raisedCount`: no employee was lost.
- Hand check: Alice (Employee, 80%, joined 12 May 2019, 7 full years) gets `7 × 2% + 1% = 15%`, so `52000 → 59800.00`, which matches the API response.
- The **order** of employees in the response changes between calls, because the threads finish in a different order each time. The **content** is always the same.

## 11. Known Limitations and Possible Improvements

- The result of `awaitTermination()` (a `boolean`) isn't checked. On a timeout, a partial list would be returned silently. It could call `shutdownNow()` and report an error.
- The `ReentrantLock` is a field, so separate requests share it and wait on each other even though each has its own list. A local lock (next to the list) would avoid that.
- A new thread pool is created on every request. A single shared pool (for example, a Spring-managed `ExecutorService` bean) would be more efficient.
- The POST endpoint returns only the employees who received a raise.
- The CSV parser uses `split(",")`, so it doesn't support quoted values that contain commas, and the reader isn't closed if parsing fails (try-with-resources would fix that).

## 12. Conclusion

The project reads the CSV, applies the salary rules, and processes employees concurrently with a thread pool. Shared data is protected with the right tool for each case: an `AtomicInteger` for single counters, and a `ReentrantLock` for the non-thread-safe `ArrayList`.

The main things learned:
- A lock only works if every thread uses it.
- `lock()` and `unlock()` must run on the same thread, with `unlock()` in `finally`.
- Exceptions inside `submit()` are silent.
- `shutdown()` doesn't wait; you need `awaitTermination()`.
- Race conditions often stay hidden on small inputs.
