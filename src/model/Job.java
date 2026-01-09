package model; // declares this file belongs to the "model" package 

public class Job { // defines a class named Job
    private final int id; // unique identifier for the job (immutable because final)
    private final int durationMs; // simulated processing time in milliseconds (immutable)

    public Job(int id, int durationMs) { // constructor used to create a Job
        this.id = id; // store the provided id in the object field
        this.durationMs = durationMs; // store the provided duration in the object field
    }

    public int getId() { // getter method to access id
        return id; // return the job id
    }

    public int getDurationMs() { // getter method to access duration
        return durationMs; // return the job processing time in ms
    }

    @Override
    public String toString() { // controls how the job prints in logs
        return "Job{id=" + id + ", durationMs=" + durationMs + "}"; // formatted string for debug output
    }
}
