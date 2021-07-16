package ocotillo.samples.parsers;

import java.awt.Color;
import java.io.*;
import java.util.*;

public class CovidEventsFileParser {

    public static class Event{
        public final int time;
        public final String eventType;
        public final int id;
        public final String newStatus;
        public final String additionalInfo;
        public final Color color;

        public Event(int time, String eventType, int id, String newStatus, String additionalInfo, Color color) {
            this.time = time;
            this.eventType = eventType;
            this.id = id;
            this.newStatus = newStatus;
            this.additionalInfo = additionalInfo;
            this.color = color;
        }

        public int getTime() {
            return time;
        }

        @Override
        public String toString() {
            return "Event{" +
                    "time=" + time +
                    ", eventType='" + eventType + '\'' +
                    ", id=" + id +
                    ", newStatus='" + newStatus + '\'' +
                    ", additionalInfo='" + additionalInfo + '\'' +
                    ", color=" + color +
                    '}';
        }
    }

    /**
     * Encodes events in time as an Event[][] matrix. Indexes of rows and columns model actual values
     * @param eventsFile events file
     * @param personsMap map of id : person entries
     * @param matrixColumns the largest id + 1
     * @return Event[][] matrix encoding events in the data
     */
    public static Event[][] parseEventsFile(File eventsFile, HashMap<Integer,
            CovidTransmission.Person> personsMap, int matrixColumns){


        int lastDay = 0;

        try{
            Scanner in = new Scanner(eventsFile);

            lastDay = findLastDay(in, personsMap);
        } catch (FileNotFoundException e) {
            System.out.println(("File " + eventsFile + " could not be found"));
        }


        Event[][] events = new Event[lastDay + 1][matrixColumns];

        try (Scanner in = new Scanner(eventsFile)) {
            //PrintWriter pw = new PrintWriter("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\testEventsLarge.txt");

            while(in.hasNextLine()){
                String line = in.nextLine();

                //|| !personsMap.containsKey(Integer.parseInt(line.split(",")[2]))

                //skip unsupported statuses
                if(isInvalidStatus(line.split(",")[3])
                        || !personsMap.containsKey(Integer.parseInt(line.split(",")[2]))){
                    continue;
                }
                Event newEvent = parseEvent(line);


                //System.out.println("newEvent = " + newEvent);
                //pw.println(line);

                events[newEvent.time][newEvent.id] = newEvent;

            }

            //pw.close();
        } catch (FileNotFoundException e){
            System.out.println("File " + eventsFile + " could not be found");
        }

        return events;
    }

    /**
     * Finds the last day an event in the data occurs
     * @param in Scanner object with the stream of events data
     * @param personsMap map of id : person entries
     * @return int with the last day in the events data
     */
    public static int findLastDay(Scanner in, HashMap<Integer, CovidTransmission.Person> personsMap) {
        int lastDay = 0;

        while (in.hasNextLine()) {
            String line = in.nextLine();
            if(isInvalidStatus(line.split(",")[3])){
                continue;
            }
            Event newEvent = parseEvent(line);
            if (personsMap.containsKey(newEvent.id) && lastDay < newEvent.time) {
                lastDay = newEvent.time;
            }
        }

        return lastDay;
    }

    /**
     * Parses a String with an event's data into an Event object
     * @param line String with an event's data
     * @return an Event object
     */
    public static Event parseEvent(String line){
        String[] tokens = line.split(",");

        int time = Integer.parseInt(tokens[0]);
        String eventType = tokens[1];
        eventType = eventType.replace("\"", "");
        int id = Integer.parseInt(tokens[2]);
        String newStatus = tokens[3];
        newStatus = newStatus.replace("\"", "");
        String additionalInfo = tokens[4];
        additionalInfo = additionalInfo.replace("\"", "");

        Color color = null;

        switch(newStatus) {
            case "EXPOSED":
                color = new Color(175, 222, 158, 255);
                break;
            case "ASYMPTOMATIC":
                color = new Color(238, 255, 0);
                break;
            case "PRESYMPTOMATIC":
                color = new Color(222, 154, 37);
                break;
            case "SYMPTOMATIC":
                color = new Color(239, 98, 72);
                break;
            case "SEVERELY_SYMPTOMATIC":
                color = new Color(255, 0, 0);
                break;
            case "RECOVERED":
                color = new Color(0, 168, 255);
                break;
            case "DEAD":
                color = new Color(80, 80, 80);
                break;
            default:
                System.out.println("Event Type " + newStatus + " unsupported");
                break;
        }

        return new Event(time, eventType, id, newStatus, additionalInfo, color);

    }

    /**
     * Logic to determine if an event's status is supported in the parser
     * @param statusString substring of an event's String with the event's status
     * @return true if the status is supported, false otherwise
     */
    public static boolean isInvalidStatus(String statusString){
        statusString = statusString.replace("\"", "");
        return (statusString.equalsIgnoreCase("ALERTED") ||
                statusString.equalsIgnoreCase("REQUESTED_TEST") ||
                statusString.equalsIgnoreCase("AWAITING_RESULT") ||
                statusString.equalsIgnoreCase("TESTED_NEGATIVE") ||
                statusString.equalsIgnoreCase("TESTED_POSITIVE") ||
                statusString.equalsIgnoreCase("AlertEvent") ||
                statusString.equalsIgnoreCase("NONE"));
    }

//    public static void main(String[] args) {
//        File eventsFile = new File("ResourcesCovid/events.txt");
//
//    }


}
