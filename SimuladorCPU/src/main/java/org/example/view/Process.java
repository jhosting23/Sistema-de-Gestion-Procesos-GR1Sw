package org.example.view;

import java.awt.Color;

public class Process {

    public enum State { NEW, READY, RUNNING, FINISHED }

    public final String pid;
    public final int arrival;
    public final int burst;
    public final int priority;
    public final int memory;
    public final Color color;

    public int remaining;
    public State state = State.NEW;
    public int waitingTime = 0;
    public int turnaroundTime = 0;
    public int startTime = -1;
    public int finishTime = -1;
    public int coreAssigned = -1;

    public Process(String pid, int arrival, int burst, int priority, int memory, Color color) {
        this.pid = pid;
        this.arrival = arrival;
        this.burst = burst;
        this.priority = priority;
        this.memory = memory;
        this.color = color;
        this.remaining = burst;
    }
}
