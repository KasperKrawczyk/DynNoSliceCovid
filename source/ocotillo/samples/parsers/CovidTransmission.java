package ocotillo.samples.parsers;

import com.sun.xml.internal.bind.v2.*;
import ocotillo.dygraph.*;
import ocotillo.geometry.Coordinates;
import ocotillo.geometry.Interval;
import ocotillo.graph.*;
import ocotillo.samples.parsers.Commons.DyDataSet;
import ocotillo.samples.parsers.Commons.Mode;

import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.stream.*;

/**
 * Parses the Covid-19 transmission dataset
 */
public class CovidTransmission {

    /**
     * Models an infected person
     */
    public static class Person {
        public final int id;
        public final int from;
        public final int day;
        public String location;

        public Person(int id, int from, int day) {
            this.id = id;
            this.from = from;
            this.day = day;
            this.location = null;
        }

        public Person(int id, int from, int day, String location) {
            this.id = id;
            this.from = from;
            this.day = day;
            this.location = location;
        }

        @Override
        public String toString() {
            return "Person{" +
                    "id=" + id +
                    ", from=" + from +
                    ", day=" + day +
                    ", location='" + location + '\'' +
                    '}';
        }

        public int getDay() {
            return this.day;
        }

        public int getID() {
            return this.id;
        }
    }

    public static class CovidDataSet {
        public final List<Person> personsList;
        public final HashSet<Person> personsSet;
        public final HashMap<Integer, Person> personsMap;
        public final List<List<Person>> components;
        public final List<CovidContactsFileParser.Contact> contactsList;
        public final Set<String> locationsSet;
        public final Color[][] timeSteps;
        public final int firstDay;
        public final int lastDay;

        public CovidDataSet(List<Person> personsList, HashSet<Person> personsSet,
                            HashMap<Integer, Person> personsMap, List<List<Person>> components,
                            ArrayList<CovidContactsFileParser.Contact> contactsList,
                            Color[][] timeSteps,
                            int firstDay, int lastDay) {
            this.personsList = personsList;
            this.personsSet = personsSet;
            this.personsMap = personsMap;
            this.components = components;
            this.contactsList = contactsList;
            this.locationsSet = CovidContactsFileParser.getLocationsSet(contactsList);
            this.timeSteps = timeSteps;
            this.firstDay = firstDay;
            this.lastDay = lastDay;
        }
    }

    public static class CovidDyDataSet extends DyDataSet{

        public Set<String> locationHighlightOptions;

        /**
         * Builds a dynamic dataset.
         *
         * @param dygraph             the dynamic graph.
         * @param suggestedTimeFactor the suggested time factor.
         * @param suggestedInterval   the suggested animation interval.
         * @param locationHighlightOptions list of locations after filtering components in the model
         */
        public CovidDyDataSet(DyGraph dygraph, double suggestedTimeFactor,
                              Interval suggestedInterval, Set<String> locationHighlightOptions) {
            super(dygraph, suggestedTimeFactor, suggestedInterval);
            this.locationHighlightOptions = locationHighlightOptions;
        }
    }




    /**
     * Parses a single component into a list of personsSet
     *
     * @param component String containing one graph component
     * @return List of personsSet in the component
     */
    public static HashSet<Person> parseComponent(String component) {
        HashSet<Person> personsSetInComponent = new HashSet<>();
        try (Scanner parseString = new Scanner(component)) {
            int counter = 0;
            while (parseString.hasNextLine()) {

                String line = parseString.nextLine();

                if (line.equalsIgnoreCase("")) {
                    continue;
                }

                line = line.replaceAll("\\s+", "");
                if (line.substring(0, 2).equalsIgnoreCase("->")) {
                    line = line.substring(2);
                }

                String[] tokens = line.split("->");

                String vectorString = tokens[0];
                Person vectorPerson = parseVectorPerson(vectorString);

                //personsSetInComponent.add(vectorPerson);
                //only add the person at the head of a line if it is the first line in a component
                if (counter == 0) {
                    personsSetInComponent.add(vectorPerson);
                }

                String infectedsString = tokens[1];
                HashSet<Person> infectedsSet = parseInfecteds(infectedsString, vectorPerson);
                personsSetInComponent.addAll(infectedsSet);

                counter++;
            }
        }
        return personsSetInComponent;
    }

//    /**
//     * Adds a List of events with status updates to each person in a Set
//     * @param eventsFile file with events
//     * @param personsSetInDataset set of Person objects in the dataset
//     * @return Set of Persons updated with their respective events ( = new statuses)
//     */
//    public static HashSet<Person> addEvents(File eventsFile, HashSet<Person> personsSetInDataset){
//        HashMap<Integer, ArrayList<CovidEventsFileParser.Event>> eventsMap =
//                CovidEventsFileParser.parseEventsFile(eventsFile);
//        for(Person person : personsSetInDataset){
//            ArrayList<CovidEventsFileParser.Event> personEvents = eventsMap.get(person.id);
//            person.events = personEvents;
//        }
//
//        return personsSetInDataset;
//    }

    /**
     * Parses components in the model
     *
     * @param personsFile file with the infection map
     * @param eventsFile  file with the events
     * @return the transmission dataset
     */
    public static CovidDataSet parseCovidFiles(File personsFile, File eventsFile, File contactsFile) throws NoSuchElementException{

        HashSet<Person> personsSetInDataset = new HashSet<>();

        List<List<Person>> components = new ArrayList<>();

        try (Scanner in = new Scanner(personsFile)) {
            in.useDelimiter("\\n\\r");
            while (in.hasNext()) {
                String component = in.next();
                HashSet<Person> personsSetInComponent = parseComponent(component);
                ArrayList<Person> personsListInComponent = new ArrayList<>(personsSetInComponent);
                components.add(personsListInComponent);
                personsSetInDataset.addAll(personsSetInComponent);
            }

        } catch (FileNotFoundException e) {
            System.out.println("File " + personsFile + " could not be found");
        }


        List<Person> personsList = new ArrayList<>(personsSetInDataset);

        HashMap<Integer, Person> personsMap = mapPersons(personsList);

        //create matrix of color changes across time
        Color[][] timeSteps = createTimeSteps(eventsFile, personsSetInDataset, personsMap);

        ArrayList<CovidContactsFileParser.Contact> contactsList =
                CovidContactsFileParser.readInAndFilter(personsMap, personsList, contactsFile);

        findLocationTypes(contactsList);

        //writePersonsWithLocations(personsMap, personsList, contactsList);

        return (new CovidDataSet(personsList, personsSetInDataset, personsMap, components,
                contactsList, timeSteps, 0, timeSteps.length));
    }

    /**
     * Parses components in the model
     *
     * @param personsFile file with the infection map
     * @param eventsFile  file with the events
     * @param contactsFile file with the contacts
     * @return the transmission dataset
     */
    public static CovidDataSet parseCovidFilesWithLocation(File personsFile, File eventsFile, File contactsFile) throws NoSuchElementException{

        HashSet<Person> personsSetInDataset = new HashSet<>();

        List<List<Person>> components = new ArrayList<>();

        try (Scanner in = new Scanner(personsFile)) {
            in.useDelimiter("\\n\\r");
            while (in.hasNext()) {
                String component = in.next();
                HashSet<Person> personsSetInComponent = parseComponent(component);
                ArrayList<Person> personsListInComponent = new ArrayList<>(personsSetInComponent);
                components.add(personsListInComponent);
                personsSetInDataset.addAll(personsSetInComponent);
            }

        } catch (FileNotFoundException e) {
            System.out.println("File " + personsFile + " could not be found");
        }

        List<Person> personsList = new ArrayList<>(personsSetInDataset);

        HashMap<Integer, Person> personsMap = mapPersons(personsList);

        ArrayList<CovidContactsFileParser.Contact> contactsList =
                CovidContactsFileParser.readInAndFilter(personsMap, personsList, contactsFile);

        //create matrix of color changes across time
        Color[][] timeSteps = createTimeSteps(eventsFile, personsSetInDataset, personsMap);

        findLocationTypes(contactsList);

        writePersonsWithLocations(personsMap, personsList, contactsList);

        return (new CovidDataSet(personsList, personsSetInDataset, personsMap, components,
                contactsList, timeSteps, 0, timeSteps.length));
    }

    /**
     * Parses components in the model
     * Leaves in components with 50% or more infected in a specified location
     *
     * @param personsFile file with the infection map
     * @param eventsFile  file with the events
     * @param contactsFile file with the contacts
     * @param locationString string with a specified location, used for filtering
     * @return the transmission dataset
     */
    public static CovidDataSet parseCovidFilesWithLocationFilter(File personsFile,
                                                                 File eventsFile,
                                                                 File contactsFile,
                                                                 double selectedFilterFactor,
                                                                 String locationString) throws NoSuchElementException {



        List<List<Person>> componentsUnfiltered = new ArrayList<>();

        try (Scanner in = new Scanner(personsFile)) {
            in.useDelimiter("\\n\\r");
            while (in.hasNext()) {
                String component = in.next();
                HashSet<Person> personsSetInComponent = parseComponent(component);
                ArrayList<Person> personsListInComponent = new ArrayList<>(personsSetInComponent);
                componentsUnfiltered.add(personsListInComponent);
                //personsSetInDataset.addAll(personsSetInComponent);
            }

        } catch (FileNotFoundException e) {
            System.out.println("File " + personsFile + " could not be found");
        }

        HashSet<Person> personsSetInDataset = new HashSet<>();
        for(List<Person> component : componentsUnfiltered){
            personsSetInDataset.addAll(component);
        }


        List<Person> personsList = new ArrayList<>(personsSetInDataset);

        HashMap<Integer, Person> personsMap = mapPersons(personsList);

        ArrayList<CovidContactsFileParser.Contact> contactsList =
                CovidContactsFileParser.readInAndFilter(personsMap, personsList, contactsFile);

//        System.out.println("ARE THEY ALLOCATED?");
        //THEY ARE!
//        personsList.forEach(System.out::println);

        //filtering out needs to take place AFTER locations have been assigned
        List<List<Person>> componentsFiltered = new ArrayList<>();
        HashSet<Person> personsSetInDatasetFiltered = new HashSet<>();

        for(List<Person> component : componentsUnfiltered){
            //System.out.println("DOES ANYTHING HAPPEN HERE?");
            if(filterComponent(component, locationString, selectedFilterFactor)){
                //System.out.println("DOES ANYTHING HAPPEN HERE? YES IT DOES");
              componentsFiltered.add(component);
              personsSetInDatasetFiltered.addAll(component);
              //component.forEach(System.out::println);
            }
        }

        List<Person> personsListFiltered = new ArrayList<>(personsSetInDatasetFiltered);
        HashMap<Integer, Person> personsMapFiltered = mapPersons(personsListFiltered);

        //create matrix of color changes across time
        Color[][] timeSteps = createTimeSteps(eventsFile, personsSetInDatasetFiltered, personsMapFiltered);

        ArrayList<CovidContactsFileParser.Contact> contactsListFiltered =
                CovidContactsFileParser.readInAndFilter(personsMapFiltered, personsListFiltered, contactsFile);

        //findLocationTypes(contactsList);

        //writePersonsWithLocations(personsMap, personsList, contactsList);

        return (new CovidDataSet(personsListFiltered, personsSetInDatasetFiltered, personsMapFiltered, componentsFiltered,
                contactsListFiltered, timeSteps, 0, timeSteps.length));
    }


    public static void findLocationTypes(List<CovidContactsFileParser.Contact> contactsList){
        HashSet<String> contactsSet = new HashSet<>();

        for(CovidContactsFileParser.Contact contact : contactsList){
            contactsSet.add(contact.location);
        }
    }

    /**
     * Finds persons whose location of infection is known, and writes their formatted data to a file
     * @param personsMap map with ID : Person entries
     * @param personsList list of all persons
     * @param contactsList list of all contacts
     */
    public static void writePersonsWithLocations(HashMap<Integer, Person> personsMap,
                                          List<Person> personsList,
                                          ArrayList<CovidContactsFileParser.Contact> contactsList){

        String formatInitial = "%d,%d,%d,null\n";
        String formatNonInitial = "%d,%d,%d,%s\n";

        try {
            PrintWriter printWriter = new PrintWriter("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\personsWithTransmissionLocation.txt");


            for (Person p : personsList) {
                if (p.from == -1) {
                    printWriter.printf(formatInitial, p.id, p.from, p.day);
                    System.out.printf(formatInitial, p.id, p.from, p.day);
                }
            }

            for (CovidContactsFileParser.Contact c : contactsList) {
                Person p = personsMap.get(c.to);
                if (c.from == p.from && c.time == p.day) {
                    printWriter.printf(formatNonInitial, p.id, p.from, p.day, c.location);
                    System.out.printf(formatNonInitial, p.id, p.from, p.day, c.location);
                }
            }
            printWriter.close();
        }catch (FileNotFoundException e){
            System.out.println("FileNotFound");
        }
    }

    public static HashMap<Integer, Person> mapPersonsWithLocations(File file){

        HashMap<Integer, Person> personsMap = new HashMap<>();

        try {
            Scanner in = new Scanner(file);
            while(in.hasNextLine()){
                String line = in.nextLine();
                String[] tokens = line.split(",");
                int ID = Integer.parseInt(tokens[0]);
                int from = Integer.parseInt(tokens[1]);
                int time = Integer.parseInt(tokens[2]);
                String location = tokens[3];

                Person newPerson = new Person(ID, from, time, location);

                personsMap.put(newPerson.id, newPerson);
            }
        } catch (FileNotFoundException e){
            System.out.println("File " + file + " could not be found");
        }

        return personsMap;
    }

    /**
     * Maps persons to their IDs
     * @param personsList a list of persons in the infection model
     * @return A HashMap of <ID : person> entries
     */
    public static HashMap<Integer, Person> mapPersons (List<Person> personsList){
        HashMap<Integer, Person> personsMap = new HashMap<>();
        for(Person person : personsList){
            personsMap.put(person.id, person);
        }
        return personsMap;
    }

    /**
     * Filters components to leave only these with <= 50% of infection in a specified location
     * @param components list of components in the infection model
     * @param location specified location
     * @return list of valid persons
     */
    private static ArrayList<Person> filterComponents(List<List<Person>> components, String location, double selectedFilterFactor){
        ArrayList<Person> validPersons = new ArrayList<>();
        for(List<Person> component : components){
            if(filterComponent(component, location, selectedFilterFactor)){
                validPersons.addAll(component);
            }
        }
        return validPersons;
    }

    /**
     * Checks if <= 50% of component's infection took place in a specified location, if so returns true, false otherwise
     * @param component a list of persons - a components in the infection model
     * @param location specified location
     * @return true if <= 50% infections took place in a specified location, false otherwise
     */
    private static boolean filterComponent(List<Person> component, String location, double selectedFilterFactor) {
        System.out.println("selectedFilterFactor in filterComponent() = " + selectedFilterFactor);
        double numOfPersInLocation = 0;
        for(Person person : component){
            if(person.location != null && person.location.equalsIgnoreCase(location)){
                //System.out.println("AHA2!" + person);
                numOfPersInLocation++;
            }
        }
        // TODO could add a functionality to choose the threshold (a slider, e.g.?)
        return numOfPersInLocation / component.size() >= selectedFilterFactor;
    }

    /**
     * @param eventsFile file with events
     * @param personsSet set of Person objects in the dataset
     * @return Color[][] matrix with timesteps coded as color changes over time (row indexes = days, column indexes = IDs)
     */
    public static Color[][] createTimeSteps(File eventsFile,
                                            HashSet<Person> personsSet,
                                            HashMap<Integer, Person> personsMap) throws NoSuchElementException {
        Color[][] timeSteps;
        ArrayList<Person> persons = new ArrayList<>(personsSet);

        Person personLargestID = persons
                .stream()
                .max(Comparator.comparing(Person::getID))
                .orElseThrow(NoSuchElementException::new);
        int matrixColumns = personLargestID.getID() + 1; //columns

        //Covid.EventsFileParse.findLastDay() is used to determine the number of rows in the matrix
        CovidEventsFileParser.Event[][] events = CovidEventsFileParser.parseEventsFile(eventsFile, personsMap, matrixColumns);
        timeSteps = new Color[events.length][events[0].length];
        //preload day 0 with Color.WHITE, so we can copy colors later
        Arrays.fill(timeSteps[0], Color.WHITE);


        //copy an Event object's color into the timeSteps matrix
        for (int t = 0; t < timeSteps.length; t++) {

            if (t > 0) {
                for (int column = 0; column < timeSteps[t].length; column++) {
                    timeSteps[t][column] = timeSteps[t - 1][column];
                }
            }

            for (int column = 0; column < timeSteps[t].length; column++) {
                if (events[t][column] == null) {
                    continue;
                }

                timeSteps[t][column] = events[t][column].color;
                //System.out.println("Node " + column + " " + (events[t][column].time == t)  +  " at time " + t + "; color = " + events[t][column].color);
            }
        }


        return timeSteps;
    }

    /**
     * Parses a vector person at the head of a line
     *
     * @param personString String containing person data
     * @return Person object - a vector
     */
    public static Person parseVectorPerson(String personString) {
        String[] tokens = personString.split("\\(");
        int id = Integer.parseInt(tokens[0]);
        String day = tokens[1];
        day = day.replaceAll("\\)", "");
        int dayInt = Integer.parseInt(day);

        return (new Person(id, -1, dayInt));
    }

    /**
     * Parses an infected person
     *
     * @param personString String containing person data
     * @param vector infecting Person
     * @return infected person (Person object)
     */
    public static Person parsePerson(String personString, Person vector) {

        String[] tokens = personString.split("\\(");
        int id = Integer.parseInt(tokens[0]);

        String day = tokens[1];
        day = day.replaceAll("\\)", "");
        int dayInt = Integer.parseInt(day);

        return (new Person(id, vector.id, dayInt));
    }

    /**
     * Parses a list o Persons infected by their vector
     *
     * @param infectedsString list of people contained within []
     * @param vector infecting Person
     * @return list of Person object
     */
    public static HashSet<Person> parseInfecteds(String infectedsString, Person vector) {
        HashSet<Person> infectedsList = new HashSet<>();
        infectedsString = infectedsString.replaceAll("[\\[|\\]]", "");
        String[] tokens = infectedsString.split(",");

        for (String token : tokens) {
            Person infectedPerson = parsePerson(token, vector);
            infectedsList.add(infectedPerson);
        }

        return infectedsList;
    }

    /**
     * Creates and draws a graph from the input data
     *
     * @param infectionsFile infection map file
     * @param eventsFile events file
     * @param mode mode the graph is drawn in
     * @return a graph DyGraph object
     */
    public static DyGraph parseGraph(File infectionsFile, File eventsFile, File contactsFile, Mode mode) {
        DyGraph graph = new DyGraph();
        DyNodeAttribute<Boolean> presence = graph.nodeAttribute(StdAttribute.dyPresence);
        DyNodeAttribute<String> label = graph.nodeAttribute(StdAttribute.label);
        DyNodeAttribute<Coordinates> position = graph.nodeAttribute(StdAttribute.nodePosition);
        DyNodeAttribute<Color> color = graph.nodeAttribute(StdAttribute.color);

        DyEdgeAttribute<Boolean> edgePresence = graph.edgeAttribute(StdAttribute.dyPresence);
        DyEdgeAttribute<Color> edgeColor = graph.edgeAttribute(StdAttribute.color);

        CovidDataSet dataset = parseCovidFiles(infectionsFile, eventsFile, contactsFile);

        Map<Integer, Node> nodeMap = new HashMap<>();

        //create origin node
//        Node origin = graph.newNode("" + -1);
//        presence.set(origin, new Evolution<>(false));
//        label.set(origin, new Evolution<>(""));
//        position.set(origin, new Evolution<>(new Coordinates(0, 0)));
//        color.set(origin, new Evolution<>(new Color(255, 255, 255)));
//        nodeMap.put(-1, origin);
//        Interval originInterval = Interval.newRightClosed(-1, dataset.timeSteps.length);
//        presence.get(origin).insert(new FunctionConst<>(originInterval, true));
        //end of origin node definition

        //draw nodes, set initial node color
        for (Person person : dataset.personsList) {
            //System.out.println(person.location);
            int personID = person.id;
            Node newNode = graph.newNode("" + personID);
            presence.set(newNode, new Evolution<>(false));
            label.set(newNode, new Evolution<>("" + personID));
            position.set(newNode, new Evolution<>(new Coordinates(0, 0)));
            color.set(newNode, new Evolution<>(new Color(255, 255, 255)));

            nodeMap.put(personID, newNode);
            Interval presenceInterval = Interval.newRightClosed(person.day, dataset.timeSteps.length);

            presence.get(newNode).insert(new FunctionConst<>(presenceInterval, true));
        }

        //color nodes
        for (int t = 0; t < dataset.timeSteps.length; t++) {
            for (Person person : dataset.personsList) {
                Node node = graph.getNode("" + person.id);
                Interval newStatusInterval = Interval.newRightClosed(t - 0.5, t + 0.5);
                Color newColor = dataset.timeSteps[t][person.id];

                color.get(node).insert(new FunctionConst<>(newStatusInterval, newColor));
            }
        }


        //draw edges
        for (Person person : dataset.personsSet) {

            if (person.from == -1) {
                continue;
            }

            Node source = nodeMap.get(person.from);
            Node target = nodeMap.get(person.id);
            Edge edge = graph.betweenEdge(source, target);


            if (edge == null) {
                edge = graph.newEdge(source, target);
                edgePresence.set(edge, new Evolution<>(false));
                edgeColor.set(edge, new Evolution<>(Color.BLACK));
            }


            Interval transmissionInterval = Interval.newRightClosed(person.day, dataset.timeSteps.length);
            edgePresence.get(edge).insert(new FunctionConst<>(transmissionInterval, true));
        }

        Commons.scatterNodes(graph, 200);

        Commons.mergePresenceFunctions(graph,
                -1.5,
                dataset.timeSteps.length,
                mode);

        return graph;

    }

    /**
     * Creates and draws a graph from the input data
     * Resizes nodes representing persons infected in a specified location
     *
     * @param infectionsFile infection map file
     * @param eventsFile events file
     * @param mode mode the graph is drawn in
     * @return a graph DyGraph object
     */
    public static DyGraph parseGraphWithLocations(File infectionsFile, File eventsFile, File contactsFile,
                                                  String location, Mode mode) {
        DyGraph graph = new DyGraph();
        DyNodeAttribute<Boolean> presence = graph.nodeAttribute(StdAttribute.dyPresence);
        DyNodeAttribute<String> label = graph.nodeAttribute(StdAttribute.label);
        DyNodeAttribute<Coordinates> position = graph.nodeAttribute(StdAttribute.nodePosition);
        DyNodeAttribute<Color> color = graph.nodeAttribute(StdAttribute.color);
        DyNodeAttribute<Coordinates> size = graph.nodeAttribute(StdAttribute.nodeSize);

        DyEdgeAttribute<Boolean> edgePresence = graph.edgeAttribute(StdAttribute.dyPresence);
        DyEdgeAttribute<Color> edgeColor = graph.edgeAttribute(StdAttribute.color);

        CovidDataSet dataset = parseCovidFilesWithLocation(infectionsFile, eventsFile, contactsFile);

        Map<Integer, Node> nodeMap = new HashMap<>();

        //create origin node
//        Node origin = graph.newNode("" + -1);
//        presence.set(origin, new Evolution<>(false));
//        label.set(origin, new Evolution<>(""));
//        position.set(origin, new Evolution<>(new Coordinates(0, 0)));
//        color.set(origin, new Evolution<>(new Color(255, 255, 255)));
//        nodeMap.put(-1, origin);
//        Interval originInterval = Interval.newRightClosed(-1, dataset.timeSteps.length);
//        presence.get(origin).insert(new FunctionConst<>(originInterval, true));
        //end of origin node definition

        //draw nodes, set initial node color
        for (Person person : dataset.personsSet) {
            int personID = person.id;
            Node newNode = graph.newNode("" + personID);
            presence.set(newNode, new Evolution<>(false));
            label.set(newNode, new Evolution<>("" + personID));
            position.set(newNode, new Evolution<>(new Coordinates(0, 0)));
            color.set(newNode, new Evolution<>(new Color(255, 255, 255)));

            nodeMap.put(personID, newNode);
            Interval presenceInterval = Interval.newRightClosed(person.day, dataset.timeSteps.length);

            presence.get(newNode).insert(new FunctionConst<>(presenceInterval, true));
        }

        //color and resize nodes
        for (int t = 0; t < dataset.timeSteps.length; t++) {
            for (Person person : dataset.personsSet) {
                Node node = graph.getNode("" + person.id);
                if(person.location != null && person.location.equalsIgnoreCase(location)){
                    size.set(node, new Evolution<>(new Coordinates(2, 2)));
                }
                Interval newStatusInterval = Interval.newRightClosed(t - 0.5, t + 0.5);
                Color newColor = dataset.timeSteps[t][person.id];

                color.get(node).insert(new FunctionConst<>(newStatusInterval, newColor));
            }
        }


        //draw edges
        for (Person person : dataset.personsSet) {
            if (person.from == -1) {
                continue;
            }

            Node source = nodeMap.get(person.from);
            Node target = nodeMap.get(person.id);
            Edge edge = graph.betweenEdge(source, target);

            if (edge == null) {
                edge = graph.newEdge(source, target);
                edgePresence.set(edge, new Evolution<>(false));
                edgeColor.set(edge, new Evolution<>(Color.BLACK));
            }

            Interval transmissionInterval = Interval.newRightClosed(person.day, dataset.timeSteps.length);
            edgePresence.get(edge).insert(new FunctionConst<>(transmissionInterval, true));
        }

        Commons.scatterNodes(graph, 200);

        Commons.mergePresenceFunctions(graph,
                -1.5,
                dataset.timeSteps.length,
                mode);

        return graph;

    }

    /**
     * Creates and draws a graph from the input data
     * Resizes nodes representing persons infected in a specified location
     *
     * @param infectionsFile infection map file
     * @param eventsFile events file
     * @param mode mode the graph is drawn in
     * @return a graph DyGraph object
     */
    public static DyGraph parseGraphWithLocationAttraction(File infectionsFile, File eventsFile, File contactsFile,
                                                  String selectedLocation, Mode mode) {
        DyGraph graph = new DyGraph();
        DyNodeAttribute<Boolean> presence = graph.nodeAttribute(StdAttribute.dyPresence);
        DyNodeAttribute<String> label = graph.nodeAttribute(StdAttribute.label);
        DyNodeAttribute<Coordinates> position = graph.nodeAttribute(StdAttribute.nodePosition);
        DyNodeAttribute<Color> color = graph.nodeAttribute(StdAttribute.color);
        DyNodeAttribute<Coordinates> size = graph.nodeAttribute(StdAttribute.nodeSize);

        DyEdgeAttribute<Boolean> edgePresence = graph.edgeAttribute(StdAttribute.dyPresence);
        DyEdgeAttribute<Color> edgeColor = graph.edgeAttribute(StdAttribute.color);
        DyEdgeAttribute<Double> edgeStrength = graph.newEdgeAttribute("Strength", 0.0);

        CovidDataSet dataset = parseCovidFilesWithLocation(infectionsFile, eventsFile, contactsFile);
        Color locationAttractionColor = new Color(255, 255, 255, 0);

        Map<Integer, Node> nodeMap = new HashMap<>();

        //create origin node
        Node origin = graph.newNode("" + -1);
        presence.set(origin, new Evolution<>(false));
        label.set(origin, new Evolution<>(""));
        position.set(origin, new Evolution<>(new Coordinates(0, 0)));
        color.set(origin, new Evolution<>(locationAttractionColor));
        nodeMap.put(-1, origin);
        Interval originInterval = Interval.newRightClosed(-1, dataset.timeSteps.length);
        presence.get(origin).insert(new FunctionConst<>(originInterval, true));
        //end of origin node definition

        //draw nodes, set initial node color
        for (Person person : dataset.personsSet) {
            int personID = person.id;
            Node newNode = graph.newNode("" + personID);
            presence.set(newNode, new Evolution<>(false));
            label.set(newNode, new Evolution<>("" + personID));
            position.set(newNode, new Evolution<>(new Coordinates(0, 0)));
            color.set(newNode, new Evolution<>(new Color(255, 255, 255)));

            nodeMap.put(personID, newNode);
            Interval presenceInterval = Interval.newRightClosed(person.day, dataset.timeSteps.length);

            presence.get(newNode).insert(new FunctionConst<>(presenceInterval, true));
        }

        //color and resize nodes
        for (int t = 0; t < dataset.timeSteps.length; t++) {
            for (Person person : dataset.personsSet) {
                Node node = graph.getNode("" + person.id);
                if(person.location != null && person.location.equalsIgnoreCase(selectedLocation)){
                    size.set(node, new Evolution<>(new Coordinates(2, 2)));
                }
                Interval newStatusInterval = Interval.newRightClosed(t - 0.5, t + 0.5);
                Color newColor = dataset.timeSteps[t][person.id];

                color.get(node).insert(new FunctionConst<>(newStatusInterval, newColor));
            }
        }


        //draw edges
        for (Person person : dataset.personsSet) {
            if (person.from == -1) {
                continue;
            }

            Node source = nodeMap.get(person.from);
            Node target = nodeMap.get(person.id);
            Edge edge = graph.betweenEdge(source, target);

            if (edge == null) {
                edge = graph.newEdge(source, target);
                edgePresence.set(edge, new Evolution<>(false));
                edgeColor.set(edge, new Evolution<>(Color.BLACK));
                edgeStrength.set(edge, new Evolution<>(1.0));
            }

            Interval transmissionInterval = Interval.newRightClosed(person.day, dataset.timeSteps.length);
            edgePresence.get(edge).insert(new FunctionConst<>(transmissionInterval, true));
        }

        //filter by the selected location
        List<Person> personsFilteredByLocation = dataset.personsList
                .stream()
                .filter(person -> person.location.equalsIgnoreCase(selectedLocation))
                .collect(Collectors.toList());

        for(Person person : personsFilteredByLocation){
            Node source = nodeMap.get(person.id);
            Edge locationAttractionEdge = graph.betweenEdge(source, origin);
            if(locationAttractionEdge == null){
                locationAttractionEdge = graph.newEdge(source, origin);
                edgePresence.set(locationAttractionEdge, new Evolution<>(false));
                edgeColor.set(locationAttractionEdge, new Evolution<>(locationAttractionColor));
                edgeStrength.set(locationAttractionEdge, new Evolution<>(2.0));
            }


            Interval transmissionInterval = Interval.newRightClosed(person.day, dataset.timeSteps.length);
            edgePresence.get(locationAttractionEdge).insert(new FunctionConst<>(transmissionInterval, true));
        }


        Commons.scatterNodes(graph, 200);

        Commons.mergePresenceFunctions(graph,
                -1.5,
                dataset.timeSteps.length,
                mode);

        return graph;

    }

    /**
     * Creates and draws a graph from the input data
     * Resizes nodes representing persons infected in a specified location
     *
     * @param infectionsFile infection map file
     * @param eventsFile events file
     * @param mode mode the graph is drawn in
     * @return a graph DyGraph object
     */
    public static DyGraph parseGraphWithMultipleLocationsAttraction(File infectionsFile, File eventsFile, File contactsFile,
                                                           List<String> selectedLocationsList, Mode mode) {

        DyGraph graph = new DyGraph();
        DyNodeAttribute<Boolean> presence = graph.nodeAttribute(StdAttribute.dyPresence);
        DyNodeAttribute<String> label = graph.nodeAttribute(StdAttribute.label);
        DyNodeAttribute<Coordinates> position = graph.nodeAttribute(StdAttribute.nodePosition);
        DyNodeAttribute<Color> color = graph.nodeAttribute(StdAttribute.color);

        DyEdgeAttribute<Boolean> edgePresence = graph.edgeAttribute(StdAttribute.dyPresence);
        DyEdgeAttribute<Color> edgeColor = graph.edgeAttribute(StdAttribute.color);
        DyEdgeAttribute<Double> edgeStrength = graph.newEdgeAttribute("Strength", 0.0);

        DyClusterAttribute<Boolean> clusterPresence = graph.clusterAttribute(StdAttribute.dyPresence);
        DyClusterAttribute<Color> clusterColor = graph.clusterAttribute(StdAttribute.color);

        CovidDataSet dataset = parseCovidFilesWithLocation(infectionsFile, eventsFile, contactsFile);
        Color locationAttractionNodeColor = new Color(0, 255, 0, 255);
        Color locationAttractionEdgeColor = new Color(178, 215, 44, 109);
        Color clusterStrokeColor = new Color(216, 239, 6, 255);

        HashMap<String, Node> polesHashMap = new HashMap<>();

        Map<Integer, Node> nodeMap = new HashMap<>();
        Map<Node, Cluster> clusterMap = new HashMap<>();

        int polesIDCounter = 0;

        //create poles
        for(String location : selectedLocationsList){
            polesIDCounter--;
            Node pole = graph.newNode("" + location, "" + location);
            presence.set(pole, new Evolution<>(false));
            label.set(pole, new Evolution<>(location));
            position.set(pole, new Evolution<>(new Coordinates(0, 0)));
            color.set(pole, new Evolution<>(locationAttractionNodeColor));
            nodeMap.put(polesIDCounter, pole);
            polesHashMap.put(location, pole);
            Interval originInterval = Interval.newRightClosed(-1, dataset.timeSteps.length);
            presence.get(pole).insert(new FunctionConst<>(originInterval, true));


//            Cluster cluster = graph.newCluster(pole, new ArrayList<>());
//            clusterPresence.set(cluster, new Evolution<>(true));
//            clusterColor.set(cluster, new Evolution<>(clusterStrokeColor));
//            Interval clusterInterval = Interval.newRightClosed(-1, dataset.timeSteps.length);
//            clusterPresence.get(cluster).insert(new FunctionConst<>(clusterInterval, true));
//            clusterMap.put(pole, cluster);
        }

        //draw nodes, set initial node color
        for (Person person : dataset.personsSet) {
            int personID = person.id;
            Node newNode = graph.newNode("" + personID, "" + personID);
            presence.set(newNode, new Evolution<>(false));
            label.set(newNode, new Evolution<>("" + personID));
            position.set(newNode, new Evolution<>(new Coordinates(0, 0)));
            color.set(newNode, new Evolution<>(new Color(255, 255, 255)));


            Interval presenceInterval = Interval.newRightClosed(person.day, dataset.timeSteps.length);
            nodeMap.put(personID, newNode);
            presence.get(newNode).insert(new FunctionConst<>(presenceInterval, true));

//            Node pole = graph.getNode(person.location);
//            if(pole != null){
//                Cluster cluster = graph.getCluster(pole.id());
//                cluster.addMember(newNode);
//            }
        }

        //color and resize nodes
        for (int t = 0; t < dataset.timeSteps.length; t++) {
            for (Person person : dataset.personsSet) {
                Node node = graph.getNode("" + person.id);
                Interval newStatusInterval = Interval.newRightClosed(t - 0.5, t + 0.5);
                Color newColor = dataset.timeSteps[t][person.id];

                color.get(node).insert(new FunctionConst<>(newStatusInterval, newColor));
            }
        }


        //draw edges
        for (Person person : dataset.personsSet) {
            if (person.from == -1) {
                continue;
            }

            Node source = nodeMap.get(person.from);
            Node target = nodeMap.get(person.id);
            Edge edge = graph.betweenEdge(source, target);

            if (edge == null) {
                edge = graph.newEdge(source, target);
                edgePresence.set(edge, new Evolution<>(false));
                edgeColor.set(edge, new Evolution<>(Color.BLACK));
                edgeStrength.set(edge, new Evolution<>(1.0));
            }

            Interval transmissionInterval = Interval.newRightClosed(person.day, dataset.timeSteps.length);
            edgePresence.get(edge).insert(new FunctionConst<>(transmissionInterval, true));
        }


        //draw clusters
        for (Person person : dataset.personsSet) {
            if (person.from == -1) {
                continue;
            }
            Node member = nodeMap.get(person.id);
            if (polesHashMap.containsKey(person.location)) {

                Node poleNode = polesHashMap.get(person.location);

                Edge locationAttractionEdge = graph.newEdge(member, poleNode);
                edgePresence.set(locationAttractionEdge, new Evolution<>(false));
                edgeColor.set(locationAttractionEdge, new Evolution<>(locationAttractionEdgeColor));
                edgeStrength.set(locationAttractionEdge, new Evolution<>(5.5));

                Interval transmissionInterval = Interval.newRightClosed(person.day, dataset.timeSteps.length);
                edgePresence.get(locationAttractionEdge).insert(new FunctionConst<>(transmissionInterval, true));

                Cluster cluster = graph.getCluster(poleNode.id());
                if (cluster == null) {
                    cluster = graph.newCluster(poleNode, new ArrayList<Node>());
                    cluster.addMember(member);
                    clusterPresence.set(cluster, new Evolution<>(true));
                    clusterColor.set(cluster, new Evolution<>(clusterStrokeColor));
                    //clusterShape.set(cluster, new Evolution<>(StdAttribute.ClusterShape.ellipse));
                    //clusterWidth.set(cluster, new Evolution<>(1.0));
                    Interval clusterInterval = Interval.newRightClosed(-1, dataset.timeSteps.length);
                    clusterPresence.get(cluster).insert(new FunctionConst<>(clusterInterval, true));
                }
                if (!cluster.members().contains(member)) {
                    cluster.addMember(member);
                }
            }

        }

        Commons.scatterNodesAroundClusterPoles(graph, 200, 12.5, 200);

        Commons.mergePresenceFunctions(graph,
                -1.5,
                dataset.timeSteps.length,
                mode);

        return graph;

    }

    /**
     * Creates and draws a graph from the input data
     * Resizes nodes representing persons infected in a specified location
     *
     * @param infectionsFile infection map file
     * @param eventsFile events file
     * @param mode mode the graph is drawn in
     * @return a graph DyGraph object
     */
    public static DyGraph parseGraphWithTransmission(File infectionsFile, File eventsFile, File contactsFile,
                                                  String locationFrom, String locationTo, Mode mode) {
        DyGraph graph = new DyGraph();
        DyNodeAttribute<Boolean> presence = graph.nodeAttribute(StdAttribute.dyPresence);
        DyNodeAttribute<String> label = graph.nodeAttribute(StdAttribute.label);
        DyNodeAttribute<Coordinates> position = graph.nodeAttribute(StdAttribute.nodePosition);
        DyNodeAttribute<Color> color = graph.nodeAttribute(StdAttribute.color);
        DyNodeAttribute<Coordinates> size = graph.nodeAttribute(StdAttribute.nodeSize);

        DyEdgeAttribute<Boolean> edgePresence = graph.edgeAttribute(StdAttribute.dyPresence);
        DyEdgeAttribute<Color> edgeColor = graph.edgeAttribute(StdAttribute.color);
        DyEdgeAttribute<Double> edgeWidth = graph.edgeAttribute(StdAttribute.edgeWidth);

        CovidDataSet dataset = parseCovidFilesWithLocation(infectionsFile, eventsFile, contactsFile);

        Map<Integer, Node> nodeMap = new HashMap<>();

        //create origin node
//        Node origin = graph.newNode("" + -1);
//        presence.set(origin, new Evolution<>(false));
//        label.set(origin, new Evolution<>(""));
//        position.set(origin, new Evolution<>(new Coordinates(0, 0)));
//        color.set(origin, new Evolution<>(new Color(255, 255, 255)));
//        nodeMap.put(-1, origin);
//        Interval originInterval = Interval.newRightClosed(-1, dataset.timeSteps.length);
//        presence.get(origin).insert(new FunctionConst<>(originInterval, true));
        //end of origin node definition

        //draw nodes, set initial node color
        for (Person person : dataset.personsSet) {
            int personID = person.id;
            Node newNode = graph.newNode("" + personID);
            presence.set(newNode, new Evolution<>(false));
            label.set(newNode, new Evolution<>("" + personID));
            position.set(newNode, new Evolution<>(new Coordinates(0, 0)));
            color.set(newNode, new Evolution<>(new Color(255, 255, 255)));

            nodeMap.put(personID, newNode);
            Interval presenceInterval = Interval.newRightClosed(person.day, dataset.timeSteps.length);

            presence.get(newNode).insert(new FunctionConst<>(presenceInterval, true));
        }

        //color and resize nodes
        for (int t = 0; t < dataset.timeSteps.length; t++) {
            for (Person person : dataset.personsSet) {
                Node node = graph.getNode("" + person.id);
                Interval newStatusInterval = Interval.newRightClosed(t - 0.5, t + 0.5);
                Color newColor = dataset.timeSteps[t][person.id];

                color.get(node).insert(new FunctionConst<>(newStatusInterval, newColor));
            }
        }


        //draw edges
        for (Person person : dataset.personsSet) {
            if (person.from == -1) {
                continue;
            }

            Node source = nodeMap.get(person.from);
            Node target = nodeMap.get(person.id);
            Edge edge = graph.betweenEdge(source, target);

            if (edge == null) {
                edge = graph.newEdge(source, target);
                edgePresence.set(edge, new Evolution<>(false));
                edgeColor.set(edge, new Evolution<>(Color.BLACK));
            }
            Interval transmissionInterval = Interval.newRightClosed(person.day, dataset.timeSteps.length);
            edgePresence.get(edge).insert(new FunctionConst<>(transmissionInterval, true));

            if(dataset.personsMap.get(person.from).location.equalsIgnoreCase(locationFrom)
                    && dataset.personsMap.get(person.id).location.equalsIgnoreCase(locationTo)) {
                edgeColor.set(edge, new Evolution<>(Color.ORANGE));
                System.out.println("AHA");
                edgeWidth.set(edge, new Evolution<>(1.2));
                edgeWidth.get(edge).insert(new FunctionConst<>(transmissionInterval, 1.2));
            }

        }

        Commons.scatterNodes(graph, 200);

        Commons.mergePresenceFunctions(graph,
                -1.5,
                dataset.timeSteps.length,
                mode);

        return graph;

    }



    /**
     * Creates and draws a graph from the input data
     * Filters components to leave only these with 50% or more of infections in the specified location
     *
     * @param infectionsFile infection map file
     * @param eventsFile events file
     * @param mode mode the graph is drawn in
     * @return a graph DyGraph object
     */
    public static DyGraph parseGraphWithLocationFilter(File infectionsFile,
                                                       File eventsFile,
                                                       File contactsFile,
                                                       String location,
                                                       double selectedFilterFactor, Mode mode) throws NoSuchElementException{
        DyGraph graph = new DyGraph();
        DyNodeAttribute<Boolean> presence = graph.nodeAttribute(StdAttribute.dyPresence);
        DyNodeAttribute<String> label = graph.nodeAttribute(StdAttribute.label);
        DyNodeAttribute<Coordinates> position = graph.nodeAttribute(StdAttribute.nodePosition);
        DyNodeAttribute<Color> color = graph.nodeAttribute(StdAttribute.color);
        DyNodeAttribute<Coordinates> size = graph.nodeAttribute(StdAttribute.nodeSize);

        DyEdgeAttribute<Boolean> edgePresence = graph.edgeAttribute(StdAttribute.dyPresence);
        DyEdgeAttribute<Color> edgeColor = graph.edgeAttribute(StdAttribute.color);

        CovidDataSet dataset = parseCovidFiles(infectionsFile, eventsFile, contactsFile);

        List<Person> validPersons = filterComponents(dataset.components, location, selectedFilterFactor);


        Map<Integer, Node> nodeMap = new HashMap<>();

        //create origin node
//        Node origin = graph.newNode("" + -1);
//        presence.set(origin, new Evolution<>(false));
//        label.set(origin, new Evolution<>(""));
//        position.set(origin, new Evolution<>(new Coordinates(0, 0)));
//        color.set(origin, new Evolution<>(new Color(255, 255, 255)));
//        nodeMap.put(-1, origin);
//        Interval originInterval = Interval.newRightClosed(-1, dataset.timeSteps.length);
//        presence.get(origin).insert(new FunctionConst<>(originInterval, true));
        //end of origin node definition

        //draw nodes, set initial node color
        for (Person person : validPersons) {
            int personID = person.id;
            Node newNode = graph.newNode("" + personID);
            presence.set(newNode, new Evolution<>(false));
            label.set(newNode, new Evolution<>("" + personID));
            position.set(newNode, new Evolution<>(new Coordinates(0, 0)));
            color.set(newNode, new Evolution<>(new Color(255, 255, 255)));

            nodeMap.put(personID, newNode);
            Interval presenceInterval = Interval.newRightClosed(person.day, dataset.timeSteps.length);

            presence.get(newNode).insert(new FunctionConst<>(presenceInterval, true));
        }

        //color and resize nodes
        for (int t = 0; t < dataset.timeSteps.length; t++) {
            for (Person person : validPersons) {
                Node node = graph.getNode("" + person.id);

                Interval newStatusInterval = Interval.newRightClosed(t - 0.5, t + 0.5);
                Color newColor = dataset.timeSteps[t][person.id];

                if(person.location.equalsIgnoreCase(location)){ // TODO test without person.location != null
                    size.set(node, new Evolution<>(new Coordinates(2, 2)));
                    color.get(node).insert(new FunctionConst<>(newStatusInterval, newColor));
                } else if(!person.location.equalsIgnoreCase(location)){
                    newColor = new Color(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), 63);
                    color.get(node).insert(new FunctionConst<>(newStatusInterval, newColor));
                }
            }
        }

        //draw edges
        for (Person person : validPersons) {
            if (person.from == -1) {
                continue;
            }

            Node source = nodeMap.get(person.from);
            Node target = nodeMap.get(person.id);
            Edge edge = graph.betweenEdge(source, target);

            if (edge == null) {
                edge = graph.newEdge(source, target);
                edgePresence.set(edge, new Evolution<>(false));
                edgeColor.set(edge, new Evolution<>(Color.BLACK));
            }

            Interval transmissionInterval = Interval.newRightClosed(person.day, dataset.timeSteps.length);
            edgePresence.get(edge).insert(new FunctionConst<>(transmissionInterval, true));
        }

        Commons.scatterNodes(graph, 200);

        Commons.mergePresenceFunctions(graph,
                -1.5,
                dataset.timeSteps.length,
                mode);

        return graph;

    }



    /**
     * Produces the dynamic dataset for this data.
     *
     * @param mode the desired mode.
     * @return the dynamic dataset.
     */
    public static CovidDyDataSet parse(Mode mode) {
        File infectionsFile = new File("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\testInfectionMapLarge.txt");
        File eventsFile = new File("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\events.txt");
        File contactsFile = new File("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\contactsModel.txt");
        CovidDataSet dataset = parseCovidFiles(infectionsFile, eventsFile, contactsFile);
        return new CovidDyDataSet(
                parseGraph(infectionsFile, eventsFile, contactsFile, mode),
                5,
                Interval.newClosed(0, dataset.personsList.size() - 1),
                dataset.locationsSet);
    }

    /**
     * Produces the dynamic dataset for this data.
     *
     * @param mode the desired mode.
     * @return the dynamic dataset.
     */
    public static CovidDyDataSet parse(Mode mode, String location) throws NoSuchElementException {
        File infectionsFile = new File("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\testInfectionMapLarge.txt");
        File eventsFile = new File("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\events.txt");
        File contactsFile = new File("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\contactsModel.txt");
        CovidDataSet dataset = parseCovidFiles(infectionsFile, eventsFile, contactsFile);
        return new CovidDyDataSet(
                parseGraphWithLocations(infectionsFile, eventsFile, contactsFile, location, mode),
                5,
                Interval.newClosed(0, dataset.personsList.size() - 1),
                dataset.locationsSet);
    }


    /**
     * Produces the dynamic dataset for this data.
     *
     * @param mode the desired mode.
     * @return the dynamic dataset.
     */
    public static CovidDyDataSet parseWithLocationFilter(Mode mode,
                                                         String location,
                                                         double selectedFilterFactor) throws NoSuchElementException {
        File infectionsFile = new File("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\testInfectionMapLarge.txt");
        File eventsFile = new File("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\events.txt");
        File contactsFile = new File("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\contactsModel.txt");
        CovidDataSet dataset = parseCovidFilesWithLocationFilter(infectionsFile, eventsFile, contactsFile,
                selectedFilterFactor, location);
        return new CovidDyDataSet(
                parseGraphWithLocationFilter(infectionsFile, eventsFile, contactsFile, location, selectedFilterFactor, mode),
                5,
                Interval.newClosed(0, dataset.personsList.size() - 1),
                dataset.locationsSet);
    }

    /**
     * Produces the dynamic dataset for this data.
     *
     * @param mode the desired mode.
     * @return the dynamic dataset.
     */
    public static CovidDyDataSet parseWithTransmission(Mode mode,
                                                       String locationFrom,
                                                       String locationTo) throws NoSuchElementException {
        File infectionsFile = new File("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\testInfectionMapLarge.txt");
        File eventsFile = new File("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\events.txt");
        File contactsFile = new File("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\contactsModel.txt");
        CovidDataSet dataset = parseCovidFiles(infectionsFile, eventsFile, contactsFile);
        return new CovidDyDataSet(
                parseGraphWithTransmission(infectionsFile, eventsFile, contactsFile, locationFrom, locationTo, mode),
                5,
                Interval.newClosed(0, dataset.personsList.size() - 1),
                dataset.locationsSet);
    }

    /**
     * Produces the dynamic dataset for this data.
     *
     * @param mode the desired mode.
     * @return the dynamic dataset.
     */
    public static CovidDyDataSet parseWithLocationAttraction(Mode mode, String selectedLocation) {
        File infectionsFile = new File("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\testInfectionMapLarge.txt");
        File eventsFile = new File("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\events.txt");
        File contactsFile = new File("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\contactsModel.txt");
        CovidDataSet dataset = parseCovidFiles(infectionsFile, eventsFile, contactsFile);
        return new CovidDyDataSet(
                parseGraphWithLocationAttraction(infectionsFile, eventsFile, contactsFile, selectedLocation, mode),
                5,
                Interval.newClosed(0, dataset.personsList.size() - 1),
                dataset.locationsSet);
    }


    /**
     * Produces a list of dynamic datasets for this data.
     *
     * @param mode the desired mode.
     * @return the dynamic dataset.
     */
    public static List<CovidDyDataSet> parseMultipleWithLocationFilter(Mode mode, List<String> selectedLocations) {
        File infectionsFile = new File("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\testInfectionMapLarge.txt");
        File eventsFile = new File("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\events.txt");
        File contactsFile = new File("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\contactsModel.txt");
        CovidDataSet dataset = parseCovidFiles(infectionsFile, eventsFile, contactsFile);
        List<CovidDyDataSet> covidDyDataSetList = new ArrayList<>();

        for(String selectedLocation : selectedLocations){
            CovidDyDataSet covidDyDataSetLocation = new CovidDyDataSet(
                    parseGraphWithLocations(infectionsFile, eventsFile, contactsFile, selectedLocation, mode),
                    5,
                    Interval.newClosed(0, dataset.personsList.size() - 1),
                    dataset.locationsSet);
            covidDyDataSetList.add(covidDyDataSetLocation);
        }


        return covidDyDataSetList;

    }

    /**
     * Produces the dynamic dataset for this data.
     *
     * @param mode the desired mode.
     * @return the dynamic dataset.
     */
    public static CovidDyDataSet parseWithMultipleLocationsAttraction(Mode mode, List<String> selectedLocationsList) {
        File infectionsFile = new File("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\testInfectionMapLarge.txt");
        File eventsFile = new File("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\events.txt");
        File contactsFile = new File("C:\\Users\\kaspe\\IdeaProjects\\DynNoSlice\\data\\Covid\\contactsModel.txt");
        CovidDataSet dataset = parseCovidFiles(infectionsFile, eventsFile, contactsFile);
        return new CovidDyDataSet(
                parseGraphWithMultipleLocationsAttraction(infectionsFile, eventsFile, contactsFile, selectedLocationsList, mode),
                5,
                Interval.newClosed(0, dataset.personsList.size() - 1),
                dataset.locationsSet);
    }
//    public static void main(String[] args) {
//        File file = new File("data/Covid/testInfectionMap.txt");
//        DyGraph graph = parseGraph(file, Mode.keepAppearedEdges);
//        CovidDataSet cDS = parseComponents(file);
//        cDS.personsSet.forEach(Person -> Person.events.stream().forEach(System.out::println));
//    }

}
