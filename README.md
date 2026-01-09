# Concurrent Job Queue (Java)

A Java implementation of a **thread-safe bounded job queue** using the **producer–consumer** pattern.  
Supports multiple producers and consumers, blocking behavior (full/empty queue), and graceful shutdown.

## Features
- Bounded buffer with capacity limit
- Multiple producers and consumers
- Blocking `put()` when full and `take()` when empty
- `wait()` / `notifyAll()` synchronization
- Graceful shutdown (consumers exit once queue drains)

## Run

Compile:
```bash
javac -d out $(find src -name "*.java")
Run:
java -cp out Main
Why this matters
This project demonstrates core backend/systems concepts:
thread safety and synchronization
race condition prevention
safe shutdown semantics for worker threads