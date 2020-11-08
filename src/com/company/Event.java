package com.company;

class Event {

    private String eventName;
    private boolean isDiscrete;
    private double minimum, maximum;
    private int min, max;
    private int weight;

    public Event(){}

    public Event(String eventName, boolean isDiscrete, double minimum, double maximum, int weight) {
        this.eventName = eventName;
        this.isDiscrete = isDiscrete;
        this.minimum = minimum;
        this.maximum = maximum;
        this.weight = weight;
    }

    public Event(String eventName, boolean isDiscrete, int minimum, int maximum, int weight) {
        this.eventName = eventName;
        this.isDiscrete = isDiscrete;
        this.min = minimum;
        this.max = maximum;
        this.weight = weight;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public boolean isDiscrete() {
        return isDiscrete;
    }

    public void setDiscrete(boolean discrete) {
        isDiscrete = discrete;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public double getMinimum() {
        return minimum;
    }

    public void setMinimum(double minimum) {
        this.minimum = minimum;
    }

    public double getMaximum() {
        return maximum;
    }

    public void setMaximum(double maximum) {
        this.maximum = maximum;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public String toString(){
        if(isDiscrete){
            return String.format("Name: %-15s %s: %-15s %s: %-10d %s: %-15d %s: %d", eventName, "Type", "Discrete" , "Minimum", min, "Maximum", max, "Weight" ,weight);
        }else{
            return String.format("Name: %-15s %s: %-15s %s: %-10.2f %s: %-15.2f %s: %d", eventName, "Type", "Continuous", "Minimum", minimum, "Maximum",maximum, "Weight" ,weight);
        }

    }
}
