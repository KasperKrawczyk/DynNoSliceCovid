/**
 * Copyright © 2014-2016 Paolo Simonetto
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package ocotillo.samples.parsers;

import java.awt.Color;
import java.text.*;
import java.util.*;

import ocotillo.dygraph.DyEdgeAttribute;
import ocotillo.dygraph.DyGraph;
import ocotillo.dygraph.DyNodeAttribute;
import ocotillo.dygraph.Evolution;
import ocotillo.dygraph.Function;
import ocotillo.dygraph.FunctionConst;
import ocotillo.dygraph.FunctionRect;
import ocotillo.dygraph.Interpolation;
import ocotillo.dygraph.extra.EvolutionAnalyser;
import ocotillo.geometry.Coordinates;
import ocotillo.geometry.Interval;
import ocotillo.graph.*;

/**
 * Parses the InfoVis collaboration data.
 */
public class Commons {

    /**
     * Returns a dynamic dataset created with the default values.
     */
    public static class DyDataSet {

        public final DyGraph dygraph;
        public final double suggestedTimeFactor;
        public final Interval suggestedInterval;

        /**
         * Builds a dynamic dataset.
         *
         * @param dygraph the dynamic graph.
         * @param suggestedTimeFactor the suggested time factor.
         * @param suggestedInterval the suggested animation interval.
         */
        public DyDataSet(DyGraph dygraph, double suggestedTimeFactor, Interval suggestedInterval) {
            this.dygraph = dygraph;
            this.suggestedTimeFactor = suggestedTimeFactor;
            this.suggestedInterval = suggestedInterval;
        }
    }

    /**
     * Indicate the mode for handling appeared nodes and edges.
     */
    public static enum Mode {
        plain,
        keepAppearedNode,
        keepAppearedEdges;
    }

    public static void scatterNodes(DyGraph graph, double distance) {
        Random randomGen = new Random(73);

        for (Node node : graph.nodes()) {
            graph.nodeAttribute(StdAttribute.nodePosition).set(node,
                    new Evolution<>(new Coordinates(randomGen.nextDouble() * distance,
                            randomGen.nextDouble() * distance)));
        }
    }

    public static void scatterNodesZero(DyGraph graph) {


        for (Node node : graph.nodes()) {
            graph.nodeAttribute(StdAttribute.nodePosition).set(node,
                    new Evolution<>(new Coordinates(0.0, 0.0)));
        }
    }

    public static Coordinates positionPoleOverCircle(DyGraph graph, int count, Node pole) {
        DecimalFormat df = new DecimalFormat("###.##");

        int faceDegree = graph.poles().size();

        double xCoordinates = 50.0 * Math.cos(2 * Math.PI * count / faceDegree);
        double yCoordinates = 50.0 * Math.sin(2 * Math.PI * count / faceDegree);

        System.out.println(pole.originId() + " : x = " + df.format(xCoordinates) + " | y = " + df.format(yCoordinates));

        Coordinates newFaceNodeCoordinates = new Coordinates(xCoordinates, yCoordinates);
        graph.nodeAttribute(StdAttribute.nodePosition).set(pole, new Evolution<>(newFaceNodeCoordinates));


        return newFaceNodeCoordinates;
    }

    public static void positionNodesOverCircle(NodeAttribute<Coordinates> mirrorPositions, List<Node> nodeList) {
        DecimalFormat df = new DecimalFormat("###.##");

        int faceDegree = nodeList.size();
        int count = 0;

        for (Node node : nodeList) {
            count++;

            double xCoordinates = 50.0 * Math.cos(2 * Math.PI * count / faceDegree);
            double yCoordinates = 50.0 * Math.sin(2 * Math.PI * count / faceDegree);


            System.out.println(node.originId() + " : x = " + df.format(xCoordinates) + " | y = " + df.format(yCoordinates));

            Coordinates oldCoordinates = mirrorPositions.get(node);

            Coordinates newFaceNodeCoordinates = new Coordinates(xCoordinates, yCoordinates, oldCoordinates.z());
            mirrorPositions.set(node, newFaceNodeCoordinates);
        }

    }


    public static void scatterNodesWithExclusion(DyGraph graph, double distance, List<Node> nodesToExclude) {
        Random randomGen = new Random(73);

        for (Node node : graph.nodes()) {
            if(nodesToExclude.contains(node)){
                System.out.println("nodeToExclude = " + node);
                continue;
            }
            graph.nodeAttribute(StdAttribute.nodePosition).set(node,
                    new Evolution<>(new Coordinates(randomGen.nextDouble() * distance,
                            randomGen.nextDouble() * distance)));
        }
    }

    public static void scatterNodesAroundClusterPoles(DyGraph graph,
                                                      double clusterDistance,
                                                      double memberDistance,
                                                      double nonMemberDistance) {
        Random randomGen = new Random(73);
        Set<Node> nodesDone = new HashSet<>();
        DecimalFormat df = new DecimalFormat("###.##");
        int count = 0;

        for(Cluster cluster : graph.clusters()){
            count++;
            Node poleNode = cluster.pole();
            Coordinates poleCoordinates = positionPoleOverCircle(graph, count, poleNode);
            graph.nodeAttribute(StdAttribute.nodePosition).set(poleNode, new Evolution<>(poleCoordinates));
            nodesDone.add(poleNode);

            for(Node memberNode : cluster.members()){
                if(!nodesDone.contains(memberNode)){
                    double offset = ((memberDistance - ( - memberDistance) + 1) *  randomGen.nextDouble()) + ( - memberDistance);
                    Coordinates memberCoordinates = new Coordinates(
                            poleCoordinates.x() + offset,
                            poleCoordinates.y() + offset);
//                    System.out.println(poleNode.id() + " : x = " + df.format(poleCoordinates.x()) + " | y = " + df.format(poleCoordinates.y()));
//                    System.out.println(memberNode.id() + " : x = " + df.format(memberCoordinates.x()) + " | y = " + df.format(memberCoordinates.y()));
                    graph.nodeAttribute(StdAttribute.nodePosition).set(memberNode, new Evolution<>(memberCoordinates));
                    nodesDone.add(memberNode);
                }
            }
        }

        for (Node node : graph.nodes()) {
            if(!nodesDone.contains(node)){
                Coordinates nodeCoordinates = (new Coordinates(randomGen.nextDouble() * nonMemberDistance,
                        randomGen.nextDouble() * nonMemberDistance));
                graph.nodeAttribute(StdAttribute.nodePosition).set(node, new Evolution<>(nodeCoordinates));

            }
        }
    }

    /**
     * Computes the coordinates for a pole nodes that are overlaid
     * on the circumference of circle
     */
    public static Coordinates computeCoordinatesOverCircle(int count, int numVertices){

        double xCoordinates = 200.0 * Math.cos(2 * Math.PI * count / numVertices);
        double yCoordinates = 200.0 * Math.sin(2 * Math.PI * count / numVertices);

        return new Coordinates(xCoordinates, yCoordinates);

    }

    /**
     * Merges overlapping or continuous presences, and assign colour and fading
     * to the graph elements.
     *
     * @param graph the dynamic graph.
     * @param dataStartTime the time at which data starts.
     * @param dataEndTime the time at which data stops.
     * @param mode the desired mode.
     * @param nodeColor the node colour.
     * @param edgeColor the edge colour.
     * @param fadingDuration the duration of the fading.
     */
    public static void mergeAndColor(DyGraph graph, double dataStartTime, double dataEndTime, Mode mode,
            Color nodeColor, Color edgeColor, double fadingDuration) {

        mergePresenceFunctions(graph, dataStartTime, dataEndTime, mode);

        for (Node node : graph.nodes()) {
            Commons.setApparance(graph, node, nodeColor, fadingDuration);
        }
        for (Edge edge : graph.edges()) {
            Commons.setApparance(graph, edge, edgeColor, fadingDuration);
        }
    }

    /**
     * Merges presences into a set of non overlapping functions.
     *
     * @param graph the dynamic graph.
     * @param dataStartTime the time at which data starts.
     * @param dataEndTime the time at which data stops.
     * @param mode the desired mode.
     */
    public static void mergePresenceFunctions(DyGraph graph, double dataStartTime, double dataEndTime, Mode mode) {
        DyNodeAttribute<Boolean> nodePresence = graph.nodeAttribute(StdAttribute.dyPresence);
        for (Node node : graph.nodes()) {
            Evolution<Boolean> nodeEvolution = nodePresence.get(node);
            switch (mode) {
                case plain:
                    Evolution<Boolean> combinedEvolution = EvolutionAnalyser.mergeFunctions(nodeEvolution);
                    nodeEvolution.clear();
                    for (Function<Boolean> function : combinedEvolution) {
                        nodeEvolution.insert(function);
                    }
                    break;
                case keepAppearedNode:
                case keepAppearedEdges:
                    double startTime = nodeEvolution.iterator().next().interval().leftBound();
                    nodeEvolution.clear();
                    nodeEvolution.insert(new FunctionConst<>(Interval.newClosed(startTime, dataEndTime), true));
                    break;
                default:
                    throw new UnsupportedOperationException("Mode not yet supported");
            }
        }

        DyEdgeAttribute<Boolean> edgePresence = graph.edgeAttribute(StdAttribute.dyPresence);
        for (Edge edge : graph.edges()) {
            Evolution<Boolean> edgeEvolution = edgePresence.get(edge);
            switch (mode) {
                case plain:
                case keepAppearedNode:
                    Evolution<Boolean> combinedEvolution = EvolutionAnalyser.mergeFunctions(edgeEvolution);
                    edgeEvolution.clear();
                    for (Function<Boolean> function : combinedEvolution) {
                        edgeEvolution.insert(function);
                    }
                    break;
                case keepAppearedEdges:
                    double startTime = edgeEvolution.iterator().next().interval().leftBound();
                    edgeEvolution.clear();
                    edgeEvolution.insert(new FunctionConst<>(Interval.newClosed(startTime, dataEndTime), true));
                    break;
                default:
                    throw new UnsupportedOperationException("Mode not yet supported");
            }
        }
    }

    /**
     * Sets the node appearance and fading.
     *
     * @param graph the graph.
     * @param node the node.
     * @param color the solid colour.
     * @param fadingDuration the fading duration.
     */
    public static void setApparance(DyGraph graph, Node node, Color color, double fadingDuration) {
        setApparance(color, fadingDuration,
                graph.<Boolean>nodeAttribute(StdAttribute.dyPresence).get(node),
                graph.<Color>nodeAttribute(StdAttribute.color).get(node));
    }

    /**
     * Sets the edge appearance and fading.
     *
     * @param graph the graph.
     * @param edge the edge.
     * @param color the solid colour.
     * @param fadingDuration the fading duration.
     */
    public static void setApparance(DyGraph graph, Edge edge, Color color, double fadingDuration) {
        setApparance(color, fadingDuration,
                graph.<Boolean>edgeAttribute(StdAttribute.dyPresence).get(edge),
                graph.<Color>edgeAttribute(StdAttribute.color).get(edge));
    }

    /**
     * Sets the element appearance and fading.
     *
     * @param color the solid colour.
     * @param fadingDuration the fading duration.
     * @param presenceEvo the presence evolution.
     * @param colorEvo the colour evolution.
     */
    private static void setApparance(Color color, double fadingDuration, Evolution<Boolean> presenceEvo, Evolution<Color> colorEvo) {
        for (Function<Boolean> current : presenceEvo) {
            Interval interval = current.interval();
            double endFadeIn = interval.leftBound() + fadingDuration;
            double startFadeOut = interval.rightBound() - fadingDuration;
            if (startFadeOut <= endFadeIn) {
                endFadeIn = (interval.leftBound() + interval.rightBound()) / 2.0;
                startFadeOut = endFadeIn;
            }
            colorEvo.insert(new FunctionRect.Color(
                    Interval.newClosed(interval.leftBound(), endFadeIn),
                    new Color(0f, 0f, 0f, 0f), color, Interpolation.Std.smoothStep));
            if (endFadeIn != startFadeOut) {
                colorEvo.insert(new FunctionConst<>(
                        Interval.newRightClosed(endFadeIn, startFadeOut), color));
            }
            colorEvo.insert(new FunctionRect.Color(
                    Interval.newRightClosed(startFadeOut, interval.rightBound()),
                    color, new Color(0f, 0f, 0f, 0f), Interpolation.Std.smoothStep));
        }
    }
}
