import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
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


    /* Check for file inconsistency
    1. Event’s minimum value greater than its maximum value.
    2. Event’s weight not an integer that is greater than 0.
    3. Discrete event’s minimum/maximum value not an integer.
    4. Event’s mean value greater than its maximum value.
    5. Duplicate event names in events/stats file.
    6. The number of events monitored which is specified at the first line of the events/stats file, does not tally with the actual number of records in the file.
     */
    private static void checkFileInconsistency(){
        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("Checking for inconsistencies between the files.");
        System.out.println("----------------------------------------------------------------------");

        boolean ok = true;

        Set<String> eventSet = new HashSet<>();
        Set<String> statSet = new HashSet<>();

        //Check if there are duplicate events from both file
        //Split them into individual hashsets
        for (Event e : eventList){
            if(!eventSet.contains(e.getEventName())){
                eventSet.add(e.getEventName());
            }else{
                System.out.println("Duplicate event, " + e.getEventName() + " found in " + eventFileName);
                ok = false;
            }
        }
        for (Stats s : statList){
            if(!statSet.contains(s.getEventName())){
                statSet.add(s.getEventName());
            }else{
                System.out.println("Duplicate event, " + s.getEventName() + " found in " + statsFileName);
                ok = false;
            }
        }

        //Check if event name from both set match
        if(eventSet.removeAll(statSet)){
            if(eventSet.size() > 0){
                System.out.println("Inconsistent events detected: " + eventSet);
                ok = false;
            }
        }

        //Check if number of events from both files are the same
        if(eventList.size() != statList.size()){
            System.out.println("Error. Number of events differ by " + (eventList.size() > statList.size() ?
                    eventList.size() - statList.size() : statList.size() - eventList.size()));
            ok = false;
        }else{
            //check first line match with size
            try
            {
                FileReader fr = new FileReader(eventFileName);
                FileReader fr_2 = new FileReader(statsFileName);
                Scanner in = new Scanner(fr);
                Scanner in_2 = new Scanner(fr_2);
                int size = Integer.parseInt(in.nextLine());
                int size_2 = Integer.parseInt(in_2.nextLine());
                if(size != eventList.size() || size_2 != statList.size())
                {
                    System.out.println("Number of events specified in the file does not tally with the actual number of events in the file");
                    ok = false;
                }
                fr.close();
                fr_2.close();
                in.close();
                in_2.close();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
            

            //Check if events matches the one pass in
            for(int i = 0; i < eventList.size(); i++){

                //Check if mean is greater than maximum
                if(eventList.get(i).getMaximum() < statList.get(i).getMean()){
                    System.out.println("Error. Event " +statList.get(i).getEventName() + " mean value is greater than maximum value");
                    ok = false;
                }

                //Check if Event Minimum not greater than maximum
                //Continuous events
                if(eventList.get(i).getMinimum() > eventList.get(i).getMaximum()){
                    System.out.println("Error. Continuous Event " + eventList.get(i).getEventName() + " minimum value is greater than maximum value");
                    ok = false;
                }

                //Check if Event Minimum not greater than maximum
                //Discrete events
                if(eventList.get(i).getMin() > eventList.get(i).getMax()){
                    System.out.println("Error. Discrete Event " + eventList.get(i).getEventName() + " minimum value is greater than maximum value");
                    ok = false;
                }

                //If event weight is not greater than 0
                if(eventList.get(i).getWeight() <= 0){
                    System.out.println("Error. Event " + eventList.get(i).getEventName() + " weight is less than or equal to 0");
                    ok = false;
                }
            }
        }


        // If no errors are detected, validation is successful
        // Else exit the system
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
            //Generate anomaly only if rand is 1.
            //33% chance of being a anomaly.
            //66% chance of being normal
            if(isAlertPhase){
                if(rand.nextInt(3) == 1){
                    tempMin = (int) Math.round(rand.nextGaussian() * standardDeviation + mean) * serverSecret * 3;
                    //System.out.println("d anomaly created");
                }else{
                    tempMin = (int) Math.round(rand.nextGaussian() * standardDeviation + mean);
                }
            }else{
                tempMin = (int) Math.round(rand.nextGaussian() * standardDeviation + mean);
            }

            tempMin = Math.abs(tempMin);
            fw.write(name + ": " + tempMin + "\n");

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
                    //System.out.println("C anomaly created");
                }else{
                    tempMin = Math.round((rand.nextGaussian() * standardDeviation + mean) * 100.0)/ 100.0;
                }
            }else{
                tempMin = Math.round((rand.nextGaussian() * standardDeviation + mean) * 100.0)/ 100.0;
            }

            tempMin = Math.abs(tempMin);
            fw.write(name + ": " + tempMin + "\n");

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


        System.out.println("\nPlease enter new statistics filename (or q to quit): ");
        newStatsFileName = input.nextLine();

        if(newStatsFileName.equals("Q") || newStatsFileName.equals("q")){
            System.out.println("System exiting now.");
            System.exit(0);
        }

        if(readStatsFile(newStatsFileName, newStatsList)){
            System.out.println("Error. Detected errors and inconsistencies in the file.");
            System.exit(0);
        }
        checkFileInconsistency();

        for(Stats s : newStatsList){
            System.out.println(s);
        }


        do{
            System.out.println("\nPlease enter number of days: ");
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
        prompt();
    }

    public static void main(String[] args) {
        //args[0] = "Events.txt";
        //args[1] = "Stats.txt";
        //args[2] = "7";

        //eventFileName = "Events.txt";//args[0];
        //statsFileName = "Stats.txt";//args[1];
        //days = 7;
        run();
    }

}
