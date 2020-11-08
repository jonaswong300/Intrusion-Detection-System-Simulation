package com.company;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

class IDS {
    private static int days;
    private static String eventFileName;
    private static String statsFileName;

    //Store event and stats data
    static ArrayList<Event> eventList = new ArrayList<Event>();
    static ArrayList<Stats> statList = new ArrayList<Stats>();
    static ArrayList<Stats> newStatsList = new ArrayList<Stats>();

    //Contains the individual data
    //individual event value, event total, mean, sd, daily total
    //{Eventname, {1 of the above}}
    private static HashMap<String, ArrayList<Double>> individualEventValue;
    private static HashMap<String, Double> individualEventTotal;
    private static HashMap<String, Double> individualEventMean;
    private static HashMap<String, Double> individualEventStandardDeviation;
    private static HashMap<Integer, Double> dailyTotal;

    //Contains the statistical data for base and live results
    //{EventName, ArrayList<Double> {Total, Mean, SD}}
    private static HashMap<String, List<Double>> baseLineResultsMap;
    private static HashMap<String, List<Double>> liveResultsMap;

    //Holds threshold before calling anomaly
    //2 * sum of weight
    private static int threshold;

    private static boolean readEventFile(String fileName){

        String [] temp;
        String eventName, eventType;
        double minimum, maximum;
        int weight;
        boolean isDiscrete;

        int lineCounter = 0, eventsMonitored = 0;

        try{
            File file = new File(fileName);
            Scanner scanner = new Scanner(file);

            System.out.println("----------------------------------------------------------------------");
            System.out.println("Processing " + fileName);
            System.out.println("----------------------------------------------------------------------");

            while(scanner.hasNextLine()){
                if(lineCounter == 0){
                    eventsMonitored = scanner.nextInt();
                    scanner.nextLine();
                    lineCounter++;
                }else{
                    //Separate Event name:[CD]:minimum:maximum:weight:
                    temp = scanner.nextLine().split(":");
                    eventName = temp[0];
                    eventType = temp[1];

                    isDiscrete = eventType.charAt(0) == 'D';

                    if(isDiscrete){
                        minimum = Integer.parseInt(temp[2]);

                        if(temp[3].equals("")){
                            maximum = 999999d;
                        }else{
                            maximum = Integer.parseInt(temp[3]);
                        }
                    }
                    else{
                        minimum = Double.parseDouble(temp[2]);

                        //Check if maximum value is empty
                        if(temp[3].equals("")){
                            maximum = 999999f;
                        }else{
                            maximum = Double.parseDouble(temp[3]);
                        }
                    }
                    //Check if minimum is more than maximum
                    if(minimum > maximum){
                        System.err.println("Error. Event " + eventName + ", minimum value is larger than maximum value.");
                        return false;
                    }

                    //Check if weight is integer and if it is positive value
                    weight = Integer.parseInt(temp[4]);
                    threshold += weight;
                    if(weight < 0){
                        System.err.println("Error. Event " + eventName + ", weight is not a positive value.");
                        return false;
                    }

                    //add check for duplicate event


                    eventList.add(new Event(eventName, isDiscrete, minimum, maximum, weight));
                    lineCounter++;
                }

            }
        }catch (IOException e){
            e.printStackTrace();
        }

        //Decrement to remove first line read in
        lineCounter--;

        //Check if number of event record match number of records it should have
        if(eventsMonitored != lineCounter && lineCounter > 0){
            System.out.println("Error. Number of events specified does not tally with actual number of event records.");
        }

        System.out.println(lineCounter + " lines successfully read in!");

        return true;

    }

    private static boolean readStatsFile(String fileName, List<Stats> statList){
        String [] temp;
        String eventName;
        double mean, standardDeviation;

        int lineCounter = 0, eventsMonitored = 0;

        try{
            File file = new File(fileName);
            Scanner scanner = new Scanner(file);

            System.out.println("\n----------------------------------------------------------------------");
            System.out.println("Processing " + fileName);
            System.out.println("----------------------------------------------------------------------");

            while(scanner.hasNextLine()){
                if(lineCounter == 0){
                    eventsMonitored = scanner.nextInt();
                    scanner.nextLine();
                    lineCounter++;
                }else{
                    //Separate Event name:mean:standard deviation
                    temp = scanner.nextLine().split(":");
                    eventName = temp[0];

                    if(temp[1].equals("")){
                        mean = 999999f;
                    }else{
                        mean = Double.parseDouble(temp[1]);
                    }

                    if(temp[2].equals("")){
                        standardDeviation = 999999f;
                    }else{
                        standardDeviation = Double.parseDouble(temp[2]);
                    }

                    //add in check for duplicates
                    statList.add(new Stats(eventName, mean, standardDeviation));
                    lineCounter++;
                }

            }
        }catch (IOException e){
            e.printStackTrace();
        }

        //Decrement to remove 1st line of stats record read in
        lineCounter--;

        //Check if number of event record match number of records it should have
        if(eventsMonitored != lineCounter && lineCounter > 0){
            System.out.println("Error. Number of stats specified does not tally with actual number of stats records.");
        }

        System.out.println(lineCounter + " lines successfully read in!");
        return false;
    }

    private static void checkFileInconsistency(){
        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("Checking for inconsistencies between the files.");
        System.out.println("----------------------------------------------------------------------");

        String mismatchEvent = "";
        boolean ok = true, mismatch = false;

        //Check if size of events and stats are same
        if(eventList.size() != statList.size()){
            System.err.println("Error. Number of events differ by " + (eventList.size() > statList.size() ?
                               eventList.size() - statList.size() : statList.size() - eventList.size()));
            ok = false;
        }

        for(int i = 0; i < eventList.size(); i++){

            if(eventList.get(i).getEventName().equals(statList.get(i).getEventName())){
                mismatch = true;
            }else{
                mismatchEvent = eventList.get(i).getEventName();
                ok = false;
            }

            //Check if mean is greater than maximum
            if(eventList.get(i).getMaximum() < statList.get(i).getMean()){
                System.out.println("Error. Event " +statList.get(i).getEventName() + " mean value is greater than maximum value");
                ok = false;
            }
        }

        if(!mismatch){
            System.out.println("Error. Inconsistent event found " + mismatchEvent);
        }

        if(ok){
            System.out.println("Data successfully validated!");
        }else{
            System.out.println("Error. Inconsistencies detected between files.");
            System.exit(0);
        }
    }

    private static void displayData(){
        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("Displaying data");
        System.out.println("----------------------------------------------------------------------");

        for(Event e : eventList){
            System.out.println(e);
        }

        System.out.println();
        for(Stats s : statList){
            System.out.println(s);
        }
    }

    public static void readCheckDisplayFiles(){
        if(!readEventFile(eventFileName) || readStatsFile(statsFileName, statList)){
            System.out.println("Error. Detected errors and inconsistencies in the file.");
            System.exit(0);
        }
        checkFileInconsistency();
        displayData();
    }

    private static void discreteEventSimulation(String name, int minimum, int maximum, double mean, double standardDeviation, boolean isAlertPhase, int dayCounter, int serverSecret, FileWriter fw){
        Random rand = new Random();
        int tempMin = 0;
        try{
            if(isAlertPhase){
                if(rand.nextInt(3) == 1){
                    tempMin = (int) Math.round(rand.nextGaussian() * standardDeviation + mean) * serverSecret * 3;
                    System.out.println("d anomaly created");
                }else{
                    tempMin = (int) Math.round(rand.nextGaussian() * standardDeviation + mean);
                }
            }else{
                tempMin = (int) Math.round(rand.nextGaussian() * standardDeviation + mean);
            }

            fw.write(name + ": " + Math.abs(tempMin) + "\n");

            if(individualEventValue.containsKey(name)){
                individualEventValue.get(name).add((double)tempMin);
            }else{
                ArrayList<Double> tempList = new ArrayList<>();
                tempList.add((double) tempMin);
                individualEventValue.put(name, tempList);
            }

            if(dailyTotal.containsKey(dayCounter)){
                dailyTotal.put(dayCounter, dailyTotal.get(dayCounter) + tempMin);
            }else{
                dailyTotal.put(dayCounter, (double) tempMin);
            }

        }catch (IOException e){
            e.printStackTrace();
        }

    }

    private static void continuousEventSimulation(String name, double minimum, double maximum, double mean, double standardDeviation, boolean isAlertPhase, int dayCounter, int serverSecret, FileWriter fw){
        Random rand = new Random();
        double tempMin = 0;
        try{
            if(isAlertPhase){
                if(rand.nextInt(3) == 1){
                    tempMin = (Math.round((rand.nextGaussian() * standardDeviation + mean) * 100.0)/ 100.0) * serverSecret*3;
                    System.out.println("C anomaly created");
                }else{
                    tempMin = Math.round((rand.nextGaussian() * standardDeviation + mean) * 100.0)/ 100.0;
                }
            }else{
                tempMin = Math.round((rand.nextGaussian() * standardDeviation + mean) * 100.0)/ 100.0;
            }

            fw.write(name + ": " + Math.abs(tempMin) + "\n");

            if(individualEventValue.containsKey(name)){
                individualEventValue.get(name).add(tempMin);
            }else{
                ArrayList<Double> tempList = new ArrayList<>();
                tempList.add(tempMin);
                individualEventValue.put(name, tempList);
            }

            if(dailyTotal.containsKey(dayCounter)){
                dailyTotal.put(dayCounter, dailyTotal.get(dayCounter) + tempMin);
            }else{
                dailyTotal.put(dayCounter, tempMin);
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private static void formulate(){
        for(String s : individualEventValue.keySet()){

            double sum = 0;
            for(double d : individualEventValue.get(s))
                sum += d;

            individualEventTotal.put(s, sum);
        }

        for(String s : individualEventTotal.keySet()){

            double totalTemp = individualEventTotal.get(s);
            double tempMean = Math.round((totalTemp)/days * 100) / 100.0;
            individualEventMean.put(s, tempMean);

            double tempSD = 0;
            for(double d : individualEventValue.get(s))
                tempSD += Math.pow(d - tempMean, 2);

            tempSD =  Math.round(Math.sqrt(tempSD/days) * 100) / 100.0;
            individualEventStandardDeviation.put(s, tempSD);
        }
    }

    private static void displayHashMapData(){
        for(String s : individualEventValue.keySet())
            System.out.println(s + "\t" + individualEventValue.get(s));
        System.out.println();

        for(String s : individualEventTotal.keySet())
            System.out.println(s + "\t" + individualEventTotal.get(s));
        System.out.println();

        for(String s : individualEventMean.keySet())
            System.out.println(s + "\t" + individualEventMean.get(s));
        System.out.println();

        for(String s : individualEventStandardDeviation.keySet())
            System.out.println(s + "\t" + individualEventStandardDeviation.get(s));
        System.out.println();

        for(int s : dailyTotal.keySet())
            System.out.println(s + "\t" + dailyTotal.get(s));
        System.out.println();
    }

    private static void activitySimulationEngine(boolean isAlertPhase, List<Stats> statList){

        individualEventValue = new HashMap<>();
        individualEventTotal = new HashMap<>();
        individualEventMean = new HashMap<>();
        individualEventStandardDeviation = new HashMap<>();
        dailyTotal = new HashMap<>();

        Random rand = new Random();

        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("Activity Engine running");
        System.out.println("----------------------------------------------------------------------");
        System.out.println("Generating events...");

        String name;
        int serverSecret;
        double mean, standardDeviation;

        try{
            LocalTime lt = LocalTime.now();
            LocalDate ld = LocalDate.now();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HHmmss");
            DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("ddMMyyyy");
            String fileName = dtf2.format(ld) + "-" + dtf.format(lt) + "-activity.txt";

            FileWriter fw = new FileWriter(fileName);

            for(int i = 1; i <= days; i++){
                fw.write("Day " + i + " :" + "\n");
                for(Event e : eventList){
                    for(Stats s : statList){
                        if(e.getEventName().equals(s.getEventName())){
                            serverSecret = rand.nextInt() % days;
                            name = e.getEventName();
                            mean = s.getMean();
                            standardDeviation = s.getStandardDeviation();

                            if(e.isDiscrete()){
                                discreteEventSimulation(name, e.getMin(), e.getMax(), mean, standardDeviation, isAlertPhase, i, serverSecret, fw);
                            }else{
                                continuousEventSimulation(name, e.getMinimum(), e.getMaximum(), mean, standardDeviation, isAlertPhase, i, serverSecret, fw);
                            }
                        }
                    }
                }
                fw.write("\n");
            }
            fw.close();
        }catch (IOException e){
            e.printStackTrace();
        }

        formulate();
        //displayHashMapData();
    }

    private static void analysisEngine(HashMap<String, List<Double>> resultsMap){
        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("Analysis Engine running");
        System.out.println("----------------------------------------------------------------------");
        System.out.println("Analysing generated events data...");

        try{
            LocalTime lt = LocalTime.now();
            LocalDate ld = LocalDate.now();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HHmmss");
            DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("ddMMyyyy");
            String fileName = dtf2.format(ld) + "-" + dtf.format(lt) + "-analysis.txt";

            FileWriter fw = new FileWriter(fileName);

            fw.write("--------------Statistical Data--------------\n");
            for(String s : individualEventTotal.keySet()){
                fw.write(s + "\n");
                fw.write("Total: " + individualEventTotal.get(s) + "\n");
                fw.write("Mean: " + individualEventMean.get(s) + "\n");
                fw.write("SD: " + individualEventStandardDeviation.get(s) + "\n");
                fw.write("\n");

                ArrayList<Double> tempList = new ArrayList<>();
                tempList.add(individualEventTotal.get(s));
                tempList.add(individualEventMean.get(s));
                tempList.add(individualEventStandardDeviation.get(s));
                resultsMap.put(s, tempList);

            }

            fw.write("--------------Daily Totals--------------\n");
            for(int i : dailyTotal.keySet()){
                fw.write("Day " + i + ":\t" + dailyTotal.get(i) + "\n");
            }

            fw.close();

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private static void prompt(){
        String newStatsFileName;
        Scanner input = new Scanner(System.in);

        do{
            System.out.println("\nPlease enter new statistics filename (or q to quit): ");

            newStatsFileName = input.nextLine();


            if(readStatsFile(newStatsFileName, newStatsList)){
                System.out.println("Error. Detected errors and inconsistencies in the file.");
                System.exit(0);
            }
            checkFileInconsistency();

            for(Stats s : newStatsList){
                System.out.println(s);
            }

        }while(newStatsFileName.equals("Q") || newStatsFileName.equals("q"));

        do{
            System.out.println("\nPlease enter number of days");
            days = input.nextInt();
        }while (days <= 0);

    }

    private static void alertEngine(){
        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("Alert Engine running");
        System.out.println("----------------------------------------------------------------------");
        System.out.println("Discovering Anomalies...");


        double mean, sd, actualValue;
        int weight = 0;

        double baseMean, baseSD;

        //Anomaly
        //(Mean - value)/SD * weight

        //AnomalyCounter holds the sum of all for the day
        //anomalyCounter += anomaly

        for(int i = 0; i < days; i++){
            double anomalyCounter = 0, anomaly = 0;
            System.out.println("\n----------------------Day " + (i+1) + "----------------------------");
            for(String s : individualEventValue.keySet()){

                actualValue = Math.abs(individualEventValue.get(s).get(i));

                if(liveResultsMap.containsKey(s)){
                     mean = liveResultsMap.get(s).get(1);
                     sd = liveResultsMap.get(s).get(2);

                     for(Event e : eventList){
                         if(e.getEventName().equals(s))
                             weight = e.getWeight();
                     }

                     if(actualValue < mean){
                         anomaly = ((mean - actualValue) / sd) * weight;
                     }else{
                         anomaly = ((actualValue - mean) / sd) * weight;
                     }

                     if(baseLineResultsMap.containsKey(s)){
                         baseMean = baseLineResultsMap.get(s).get(1);
                         baseSD = baseLineResultsMap.get(s).get(2);

                         //Check against baseline stats
                         if(actualValue > (baseMean + baseSD) || actualValue < (baseMean - baseSD)){
                             anomaly *= 3;
                         }
                     }

                     anomalyCounter += anomaly;
                }

                System.out.printf("Event: %-20s %s: %-15.2f %s: %.2f\n", s, "Actual Num", actualValue, "Anomaly", anomaly);

            }
            System.out.printf("\nDaily Counter: %.6f\n", anomalyCounter);
            System.out.printf("Threshold: %d\n",threshold);
            System.out.println(anomalyCounter > threshold ? "<<<ALERT>>>" : "No Alert");
        }
    }

    public static void run(){
        baseLineResultsMap = new HashMap<>();
        liveResultsMap = new HashMap<>();
        days = 7;
        readCheckDisplayFiles();
        activitySimulationEngine(false, statList);
        analysisEngine(baseLineResultsMap);

        //for(String s : baseLineResultsMap.keySet())
        //System.out.println(s + "\t" + baseLineResultsMap.get(s));

        //threshold for detecting an intrusion is 2*(sum of weights)
        threshold *= 2;

        prompt();
        activitySimulationEngine(true, newStatsList);
        analysisEngine(liveResultsMap);

        //for(String s : liveResultsMap.keySet())
        //System.out.println(s + "\t" + liveResultsMap.get(s));

        alertEngine();
    }

    public static void main(String[] args) {
        eventFileName = "Events.txt"; //args[0];
        statsFileName = "Stats.txt"; //args[1];
        days = 7; //args[2];
        run();
    }

}
