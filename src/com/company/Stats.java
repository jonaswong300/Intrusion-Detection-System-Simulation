package com.company;

class Stats {
    private String eventName;
    private double mean;
    private double standardDeviation;

    public Stats(String eventName, double mean, double standardDeviation) {
        this.eventName = eventName;
        this.mean = mean;
        this.standardDeviation = standardDeviation;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public double getStandardDeviation() {
        return standardDeviation;
    }

    public void setStandardDeviation(double standardDeviation) {
        this.standardDeviation = standardDeviation;
    }

    public String toString(){
        return String.format("Name: %-15s %s: %-10.1f %s: %.2f", eventName, "Mean", mean, "Standard Deviation", standardDeviation);
    }
}
