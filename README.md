# Concurrent Job Queue (Producer–Consumer) — Java

A thread-safe **bounded job queue** in Java that models the classic **producer–consumer** concurrency problem.
The project includes performance metrics, fairness/starvation analysis, automated JUnit tests, and GitHub Actions CI.

---

## Features
- Bounded FIFO queue using `synchronized`, `wait()`, and `notifyAll()`
- Graceful shutdown so consumers exit cleanly after all jobs are processed
- Multi-producer / multi-consumer simulation
- Throughput measurement (jobs per second)
- Fairness metrics: **Jain’s fairness index**, **Gini coefficient**, and **MinShare**
- JUnit unit + integration tests with worst-case fairness validation over multiple trials
- GitHub Actions CI runs tests on every push and pull request

---

## Project Structure
```text
src/
  model/        # Job model
  queue/        # JobQueue (bounded FIFO + shutdown)
  worker/       # Producer / Consumer
test/           # JUnit tests
.github/
  workflows/ci.yml  # CI pipeline

## Requirements
Java 17 recommended (works with Java 11+)
JUnit Platform Console (downloaded automatically in CI)

## Compile and Run
From the repository root:
javac -d out $(find src -name "*.java")
java -cp out Main --capacity 50 --producers 8 --consumers 8 --jobs 200 --quiet

## Run Tests Locally
If you have junit-platform-console-standalone.jar in lib/:
rm -rf out_test
mkdir -p out_test

javac -cp "lib/junit-platform-console-standalone.jar" \
  -d out_test \
  $(find src -name "*.java") \
  $(find test -name "*.java")

java -jar lib/junit-platform-console-standalone.jar \
  -cp out_test \
  --scan-class-path

## Fairness Benchmark Example
A typical 5-trial fairness benchmark reports:
Jain’s fairness index: ≈ 0.9996–1.0000
Gini coefficient: ≈ 0.004–0.012
Minimum share per consumer: ≈ 12%+
The test asserts worst-case thresholds across trials to detect starvation or imbalance.

## Why This Project
This project demonstrates practical concurrency engineering:
correct synchronization, blocking behavior, shutdown semantics, performance measurement,
and validation via automated tests and continuous integration.