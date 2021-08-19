package ocotillo.samples.parsers;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ocotillo.dygraph.DyEdgeAttribute;
import ocotillo.dygraph.DyGraph;
import ocotillo.dygraph.DyNodeAttribute;
import ocotillo.dygraph.Evolution;
import ocotillo.dygraph.FunctionConst;
import ocotillo.geometry.Coordinates;
import ocotillo.geometry.Interval;
import ocotillo.graph.Edge;
import ocotillo.graph.Node;
import ocotillo.graph.StdAttribute;
import ocotillo.samples.parsers.Commons.DyDataSet;
import ocotillo.samples.parsers.Commons.Mode;
import ocotillo.serialization.ParserTools;

import javax.swing.text.html.parser.Parser;
import java.io.File;
import java.time.LocalDate;
import java.util.*;

public class Montagu {
    private static class LettersDataSet {

        public List<Letter> lettersList = new ArrayList<>();
        public Map<Integer, Person> personsMap = new HashMap<>();
        public List<Person> personsList = new ArrayList<>();
        public LocalDateTime firstTime;
        public LocalDateTime lastTime;
    }

    private static class Letter {

        private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        int sourceID;
        int targetID;
        LocalDate date;


        public Letter(int sourceID, int targetID, String dateString) {
            this.sourceID = sourceID;
            this.targetID = targetID;
            this.date = LocalDate.parse(dateString, TIME_FORMATTER);
        }
    }

    private static class Person {
        String name;
        int ID;

        public Person(String name, int ID) {
            this.name = name;
            this.ID = ID;
        }
    }

    /**
     * Produces the dynamic dataset for this data.
     *
     * @param mode the desired mode.
     * @return the dynamic dataset.
     */
    public static DyDataSet parse(Mode mode) {
        File personsFile = new File("data/Montagu/letter_people_as_sequence/");
        File lettersFile = new File("data/Montagu/letters_sent_as_sequence/");
        LettersDataSet dataset = parseLetters(personsFile, lettersFile);
        return new DyDataSet(
                parseGraph(personsFile, lettersFile, 2, mode),
                1,
                Interval.newClosed(dataset.firstTime.toEpochSecond(ZoneOffset.UTC),
                        dataset.lastTime.toEpochSecond(ZoneOffset.UTC)));
    }

    /**
     * Parses the dialog sequence graph.
     *
     * @param personsFile the persons file.
     * @param lettersFile the letters file
     * @param dialogDuration the factor that encode the duration of a letter exchange.
     * One corresponds to the gap between consecutive letters.
     * @param mode the desired mode.
     * @return the dynamic graph.
     */
    public static DyGraph parseGraph(File personsFile, File lettersFile, double dialogDuration, Mode mode) {
        DyGraph graph = new DyGraph();
        DyNodeAttribute<Boolean> presence = graph.nodeAttribute(StdAttribute.dyPresence);
        DyNodeAttribute<String> label = graph.nodeAttribute(StdAttribute.label);
        DyNodeAttribute<Coordinates> position = graph.nodeAttribute(StdAttribute.nodePosition);
        DyNodeAttribute<Color> color = graph.nodeAttribute(StdAttribute.color);
        DyEdgeAttribute<Boolean> edgePresence = graph.edgeAttribute(StdAttribute.dyPresence);
        DyEdgeAttribute<Color> edgeColor = graph.edgeAttribute(StdAttribute.color);

        LettersDataSet dataset = parseLetters(personsFile, lettersFile);
        Map<String, Node> nodeMap = new HashMap<>();
        for (Person person : dataset.personsList) {
            Node node = graph.newNode(person.name);
            presence.set(node, new Evolution<>(false));
            label.set(node, new Evolution<>(person.name));
            position.set(node, new Evolution<>(new Coordinates(0, 0)));
            color.set(node, new Evolution<>(new Color(141, 211, 199)));
            nodeMap.put(person.name, node);
        }

        for (Letter letter : dataset.lettersList) {
            Node source = nodeMap.get(dataset.personsMap.get(letter.sourceID));
            Node target = nodeMap.get(dataset.personsMap.get(letter.targetID));
            Edge edge = graph.betweenEdge(source, target);
            if (edge == null) {
                edge = graph.newEdge(source, target);
                edgePresence.set(edge, new Evolution<>(false));
                edgeColor.set(edge, new Evolution<>(Color.BLACK));
            }

            Interval participantPresence = Interval.newRightClosed(
                    dialog.time - dialogDuration * dialog.nominalDuration * 10.0,
                    dialog.time + dialogDuration * dialog.nominalDuration * 11.0);
            Interval dialogInterval = Interval.newRightClosed(
                    dialog.time,
                    dialog.time + dialogDuration * dialog.nominalDuration);

            presence.get(source).insert(new FunctionConst<>(participantPresence, true));
            presence.get(target).insert(new FunctionConst<>(participantPresence, true));
            edgePresence.get(edge).insert(new FunctionConst<>(dialogInterval, true));
        }

        Commons.scatterNodes(graph, 200);
        Commons.mergeAndColor(graph,
                dataset.firstTime.toEpochSecond(ZoneOffset.UTC),
                dataset.lastTime.toEpochSecond(ZoneOffset.UTC) + 1,
                mode,
                new Color(141, 211, 199),
                Color.BLACK,
                0.001);
        return graph;
    }

    private static LettersDataSet parseLetters(File personsFile, File lettersFile){
        LettersDataSet lettersDataSet = new LettersDataSet();
        parsePersonsFile(personsFile, lettersDataSet);
        parseLettersFile(lettersFile, lettersDataSet);
        return lettersDataSet;

    }

    private static void parsePersonsFile(File personsFile, LettersDataSet letterDataSet){
        List<String> fileLines = ParserTools.readFileLines(personsFile);
        Map<Integer, Person> personMap = new HashMap<>();
        List<Person> personsList = new ArrayList<>();
        letterDataSet.personsMap = personMap;
        letterDataSet.personsList = personsList;
        for(String personString : fileLines){
            Person newPerson = parsePerson(personString);
            personMap.put(newPerson.ID, newPerson);
            personsList.add(newPerson);
        }
    }

    private static Person parsePerson(String personString){
        String[] tokens = personString.split(",");
        int ID = Integer.parseInt(tokens[0]);
        String name = tokens[1].replaceAll("^\"|\"$", "");
        return new Person(name, ID);
    }

    private static void parseLettersFile(File lettersFile, LettersDataSet letterDataSet){
        List<String> fileLines = ParserTools.readFileLines(lettersFile);
        List<Letter> lettersList = new ArrayList<>();
        letterDataSet.lettersList = lettersList;
        LocalDateTime firstTime = LocalDateTime.MAX;
        LocalDateTime lastTime = LocalDateTime.MIN;
        for(String letterString : fileLines){
            Letter newLetter = parseLetter(letterString);
            firstTime = firstTime.isAfter(ChronoLocalDateTime.from(newLetter.date)) ? LocalDateTime.from(newLetter.date) : firstTime;
            lastTime = lastTime.isBefore(ChronoLocalDateTime.from(newLetter.date)) ? LocalDateTime.from(newLetter.date) : lastTime;
            lettersList.add(parseLetter(letterString));
        }
        letterDataSet.firstTime = firstTime;
        letterDataSet.lastTime = lastTime;
    }

    private static Letter parseLetter(String letterString){
        String[] tokens = letterString.split(",");
        return new Letter(
                Integer.parseInt(tokens[0]),
                Integer.parseInt(tokens[1]),
                tokens[2]
        );

    }
}
