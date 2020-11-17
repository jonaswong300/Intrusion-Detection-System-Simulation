This assignment was done by:
1. Jonas Wong Zhi Shuen  6276702
2. Joel Ong Zhen         6276696
3. Yap Junjie Brandon    6278899
4. Winnifred Wang        6278863


This program is in Java, Intellij platform.
Must be tested on Windows Command Prompt.

Classes required to compile:
1. Event.java
2. Stats.java
3. IDS.java
4. Main.java

Change command directory so that the command line will run from the folder where all the files are located.
Please ensure all the .txt files are in the same folder as the .java files.

Command to compile program:
javac Events.java Stats.java IDS.java Main.java


Command to run program:
java IDS Events.txt Stats.txt [number of days]


Inconsistencies track:
1. Event’s minimum value greater than its maximum value.
2. Event’s weight not an integer that is greater than 0.
3. Check if discrete event’s minimum/maximum value is a double.
4. Event’s mean value greater than its maximum value.
5. Duplicate event names in events/stats file.
6. The number of events monitored which is specified at the first line of the events/stats file, does not tally with the actual number of records in the file.


Note:
1. replace [number of days] with a positive integer
2. Events.txt can be replaced by the name of the file that contains event data
3. Stats.txt can be replaced by the name of the file that contains stats data