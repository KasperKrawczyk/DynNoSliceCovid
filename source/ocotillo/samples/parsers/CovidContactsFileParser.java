package ocotillo.samples.parsers;

import java.io.*;
import java.net.*;
import java.util.*;


public class CovidContactsFileParser {

    public static class Contact {
        final int time;
        final int from;
        final int to;
        final int weight;
        final String location;

        public Contact(int time, int from, int to, int weight, String location) {
            this.time = time;
            this.from = from;
            this.to = to;
            this.weight = weight;
            this.location = location;
        }

        @Override
        public String toString() {
            return "Contact{" +
                    "time=" + time +
                    ", from=" + from +
                    ", to=" + to +
                    ", weight=" + weight +
                    ", location='" + location + '\'' +
                    '}';
        }
    }

    public static Contact parseContact(String line){
        String[] tokens = line.split(",");

        int time = Integer.parseInt(tokens[0]);
        int from = Integer.parseInt(tokens[1]);
        int to = Integer.parseInt(tokens[2]);
        int weight = Integer.parseInt(tokens[3]);
        String location = tokens[4];

        return new Contact(time, from, to, weight, location);
    }




    public static ArrayList<Contact> readInAndFilter(HashMap<Integer, CovidTransmission.Person> personsMap,
                                                     List<CovidTransmission.Person> personsList,
                                                     File contactsFile) {

        ArrayList<Contact> contacts = new ArrayList();

        try{
            //File contactsFile = new File("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\contactsModel.txt");
            //URLConnection contactsURL = new URL("https://raw.githubusercontent.com/ScottishCovidResponse/scrc-vis-modelling/master/ContactTracing/data/2020_07_01_newSet/contacts_covid_model.csv").openConnection();
            //PrintWriter printWriter = new PrintWriter("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\testContactsLarge.txt");
            Scanner in = new Scanner(contactsFile);

            while(in.hasNextLine()){

                String line = in.nextLine();

                Contact newContact = parseContact(line);

                if(personsMap.containsKey(newContact.to)){
                    CovidTransmission.Person infectedPerson = personsMap.get(newContact.to);

                    if(newContact.from == infectedPerson.from && newContact.time == infectedPerson.day){
                        infectedPerson.location = newContact.location;

                        contacts.add(newContact);

                        //printWriter.println(line);
                    }
                }
                if(personsMap.containsKey(newContact.from)){
                    CovidTransmission.Person infectedPerson = personsMap.get(newContact.from);

                    if(newContact.to == infectedPerson.from && newContact.time == infectedPerson.day){

                        infectedPerson.location = newContact.location;

                        contacts.add(newContact);

                        //printWriter.println(line);

                    }
                }
            }

            //assign "InitialInfection" to the location field if null
            personsList.stream()
                    .filter(person -> person.location == null)
                    .forEach(person -> person.location = "InitialInfection");


            in.close();
            //printWriter.close();
        } catch (IOException e) {
            System.out.println("File could not be found");
        }

        return contacts;
    }

    public static Set<String> getLocationsSet(List<Contact> contactsList){
        HashSet<String> locationsSet = new HashSet<>();
        for(Contact contact : contactsList){
            locationsSet.add(contact.location);
        }

        return locationsSet;
    }

//    public static boolean isValidComponent(HashSet<CovidTransmission.Person> personsSetInComponent, HashSet<Contact> contactsSet){
//        for(CovidTransmission.Person p : personsSetInComponent){
//
//        }
//    }


}
