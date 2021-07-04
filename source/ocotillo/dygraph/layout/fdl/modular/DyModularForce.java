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
package ocotillo.dygraph.layout.fdl.modular;

import java.text.*;
import java.util.*;

import ocotillo.dygraph.extra.SpaceTimeCubeSynchroniser;
import ocotillo.dygraph.extra.SpaceTimeCubeSynchroniser.MirrorConnection;
import ocotillo.dygraph.extra.SpaceTimeCubeSynchroniser.MirrorLine;
import ocotillo.geometry.Box;
import ocotillo.geometry.Coordinates;
import ocotillo.geometry.Geom;
import ocotillo.geometry.GeomE.PointRelation;
import ocotillo.geometry.Interval;
import ocotillo.graph.*;
import ocotillo.graph.extra.BendExplicitGraphSynchroniser.MirrorEdge;
import ocotillo.graph.layout.*;
import ocotillo.graph.layout.fdl.modular.ModularForce;
import ocotillo.structures.IntervalTree;

/**
 * Forces for DyModularFdl.
 */
public abstract class DyModularForce extends ModularForce {

    protected DyModularFdl dyModularFdl;

    /**
     * Attaches the modular element to a DyModularFdl instance. Should be only
     * called by DyModularFdlBuilder.
     *
     * @param dyModularFdl the DyModularFdl instance.
     */
    protected void attachTo(DyModularFdl dyModularFdl) {
        assert (this.dyModularFdl == null) : "The ModularFdl element was already attached to a ModularFdl instance.";
        assert (dyModularFdl != null) : "Attaching the ModularFdl element to a null ModularFdl instance.";
        this.dyModularFdl = dyModularFdl;
    }

    /**
     * Returns the space-time cube synchroniser that acts on the mirror graph.
     *
     * @return the synchroniser.
     */
    protected SpaceTimeCubeSynchroniser stcSynchronizer() {
        assert (dyModularFdl != null) : "The ModularFdl element has not been attached yet.";
        return dyModularFdl.synchronizer;
    }

    protected ArrayList<Node> getDirectNodes(){
        return dyModularFdl.synchronizer.getDirectNodes();
    }

    protected Node getDirectNode(Node node){
        return dyModularFdl.synchronizer.getDirectNode(node);
    }

    protected Node getDirectNode(String nodeID){
        return dyModularFdl.synchronizer.getDirectNode(nodeID);
    }

    protected double computeRadius(Coordinates poleCoordinates){
        double radiusXSquared = Math.pow(poleCoordinates.x() + 12.5, 2);
        double radiusYSquared = Math.pow(poleCoordinates.y() + 12.5, 2);
        double radiusLength = Math.sqrt(radiusXSquared + radiusYSquared);
        return radiusLength;
    }

    protected Collection<Node> pruneSynchroTargetNodes(Collection<Node> unprunedNodes){
        ArrayList<Node> prunedNodes = new ArrayList<>();
        for(Node node : unprunedNodes){
            //System.out.println("before pruning = " + node);
            String nodeID = node.id();
            char lastChar = nodeID.charAt(nodeID.length() - 1);
            if(lastChar != 'n'){
                prunedNodes.add(node);
                //System.out.println("added? = " + node);
            }

        }
        return prunedNodes;
    }



    /**
     * Returns true if all three input point are collinear in 2D space (with coordinate values
     * rounded up to two decimal places), false otherwise.
     *
     * @param firstPoint coordinates of the first point in 2D space
     * @param secondPoint coordinates of the second point in 2D space
     * @param thirdPoint coordinates of the third point in 2D space
     * @return
     */
    private boolean areCollinear(Coordinates firstPoint, Coordinates secondPoint, Coordinates thirdPoint){
        DecimalFormat df = new DecimalFormat("#.00");
        double firstX = Double.parseDouble(df.format(firstPoint.x()));
        double firstY = Double.parseDouble(df.format(firstPoint.x()));

        double secondX = Double.parseDouble(df.format(secondPoint.x()));
        double secondY = Double.parseDouble(df.format(secondPoint.x()));

        double thirdX = Double.parseDouble(df.format(thirdPoint.x()));
        double thirdY = Double.parseDouble(df.format(thirdPoint.x()));

//            System.out.println("mirrorPoleCoordinates: x = " + firstX + " y = " + firstY);
//            System.out.println("mirrorMemberCoordinates: x = " + secondX + " y = " + secondY);
//            System.out.println("repulsionPoint: x = " + thirdX + " y = " + thirdY);

        return (secondY - firstY) * (thirdX - secondX) == (thirdY - secondY) * (secondX - firstX);
    }


    /**
     * Container for the current and desired distances between two elements.
     */
    protected static class DyDistances {

        /**
         * The minimal distance between two elements.
         */
        protected static final double minimal = 0.01;

        /**
         * The computed distance between two elements for the given temperature.
         * The glyph sizes are linearly interpolated from zero size at
         * temperature 1, to normal size at temperature 0. This value is limited
         * not to be lower of the minimal distance.
         */
        protected double currentDistance;
        /**
         * The desired distance between the center of two elements. The distance
         * is computed considering the elements at full size.
         */
        protected double desiredDistance;

        /**
         * Construct a distances object.
         *
         * @param currentDistance the current distance.
         * @param desiredDistance the desired distance.
         */
        public DyDistances(double currentDistance, double desiredDistance) {
            this.currentDistance = Math.max(minimal, currentDistance);
            this.desiredDistance = desiredDistance;
        }
    }

    /**
     * Force that straightens segments in the space-time cube to reduce node
     * movement over time in the dynamic graph.
     */
    public static class TimeStraightning extends DyModularForce {

        protected final double desiredDistance;

        public TimeStraightning(double desiredDistance) {
            this.desiredDistance = desiredDistance / 5.0;
        }

        @Override
        protected NodeAttribute<Coordinates> computeForces() {
            NodeAttribute<Coordinates> forces = new NodeAttribute<>(new Coordinates(0, 0));

            for (Node node : stcSynchronizer().originalGraph().nodes()) {
                IntervalTree<MirrorLine> trajectories = stcSynchronizer().mirrorLines(node);

                List<Node> allBends = new ArrayList<>();
                for (MirrorLine trajectory : trajectories) {
                    MirrorEdge mirrorEdge = synchronizer().getMirrorEdge(trajectory.mirrorEdge());
                    allBends.add(mirrorEdge.source());
                    allBends.addAll(mirrorEdge.bends());
                    allBends.add(mirrorEdge.target());
                }

                computeSmoothingComponent(forces, allBends);
                computeStraightningComponent(forces, allBends);
            }

            return forces;
        }

        /**
         * Force that smoothes the movement trajectories.
         *
         * @param forces the forces.
         * @param allBends the list of bends in the trajectories of this node.
         */
        private void computeSmoothingComponent(NodeAttribute<Coordinates> forces, List<Node> allBends) {
            for (int i = 0; i < allBends.size(); i++) {
                Node node = allBends.get(i);
                Coordinates currentPos = mirrorPositions().get(node);

                Coordinates vector;

                if (i == 0 || i == allBends.size() - 1) {
                    Node other = i != 0 ? allBends.get(i - 1) : allBends.get(1);
                    Coordinates posOther = mirrorPositions().get(other);
                    Coordinates desiredPosition = posOther.plus(currentPos).divide(2);
                    desiredPosition.setZ(currentPos.z());
                    vector = desiredPosition.minus(currentPos);
                } else {
                    Node nodeBefore = allBends.get(i - 1);
                    Node nodeAfter = allBends.get(i + 1);
                    Coordinates posBefore = mirrorPositions().get(nodeBefore);
                    Coordinates posAfter = mirrorPositions().get(nodeAfter);
                    if (mirrorGraph().degree(node) == 2) {
                        Coordinates midPoint = posBefore.plus(posAfter).divide(2);
                        Coordinates centroid = midPoint.plus(currentPos.minus(midPoint).divide(3));
                        vector = centroid.minus(currentPos);
                    } else {
                        double factor = (currentPos.z() - posBefore.z()) / (posAfter.z() - posBefore.z());
                        Coordinates midPointAtZ = posBefore.plus(posAfter.minus(posBefore).times(factor));
                        Coordinates centroidAtZ = midPointAtZ.plus(currentPos.minus(midPointAtZ).divide(3));
                        vector = centroidAtZ.minus(currentPos);
                    }
                }

                double vectMagnitude = Geom.e3D.magnitude(vector);
                if (vectMagnitude > 0) {
                    Coordinates unit = Geom.e3D.unitVector(vector);
                    Coordinates force = unit.times(Math.pow(vectMagnitude / desiredDistance, 2.0));
                    forces.set(node, forces.get(node).plus(force));
                }
            }
        }

        /**
         * Force that prevents trajectories to have an angle too acute with
         * respect to the plane.
         *
         * @param forces the forces.
         * @param allBends the list of bends in the trajectories of this node.
         */
        private void computeStraightningComponent(NodeAttribute<Coordinates> forces, List<Node> allBends) {
            for (int i = 0; i < allBends.size() - 1; i++) {
                for (int j = i + 1; j < allBends.size(); j++) {
                    Node source = allBends.get(i);
                    Node target = allBends.get(j);
                    Coordinates sourcePos = mirrorPositions().get(source);
                    Coordinates targetPos = mirrorPositions().get(target);
                    Coordinates vector3D = targetPos.minus(sourcePos);
                    Coordinates vector2D = vector3D.restr(2);
                    if (dyModularFdl.geometry.almostZero(vector3D.z())
                            || dyModularFdl.geometry.almostZero(vector2D)) {
                        continue;
                    }
                    double angle = Math.max(dyModularFdl.geometry.betweenAngle(vector3D, vector2D), 0.01);
                    Coordinates force = vector2D.times((Math.PI / 2.0 - angle) / angle);
                    forces.set(source, forces.get(source).plus(force));
                    forces.set(target, forces.get(target).plus(force.minus()));
                }
            }
        }
    }

    /**
     * Force that keeps the nodes around the same region of the plane.
     */
    public static class MentalMapPreservation extends DyModularForce {

        protected final double desiredDistance;

        public MentalMapPreservation(double desiredDistance) {
            this.desiredDistance = desiredDistance;
        }

        @Override
        protected NodeAttribute<Coordinates> computeForces() {
            NodeAttribute<Coordinates> forces = new NodeAttribute<>(new Coordinates(0, 0));
            for (Node node : stcSynchronizer().originalGraph().nodes()) {
                IntervalTree<MirrorLine> trajectories = stcSynchronizer().mirrorLines(node);

                List<Node> allBends = new ArrayList<>();
                for (MirrorLine trajectory : trajectories) {
                    MirrorEdge mirrorEdge = synchronizer().getMirrorEdge(trajectory.mirrorEdge());
                    allBends.add(mirrorEdge.source());
                    allBends.addAll(mirrorEdge.bends());
                    allBends.add(mirrorEdge.target());
                }

                for (int i = 0; i < allBends.size(); i++) {
                    Node currentBend = allBends.get(i);
                    List<Node> window = new ArrayList<>();
                    for (int j = Math.max(i - 5, 0); j < i; j++) {
                        window.add(allBends.get(j));
                    }
                    for (int j = Math.min(i + 5, allBends.size() - 1); j > i; j--) {
                        window.add(allBends.get(j));
                    }

                    double weightSum = 0;
                    Coordinates centre = new Coordinates(0, 0);
                    Coordinates currentPos = mirrorPositions().get(currentBend);
                    for (Node nearNode : window) {
                        Coordinates nearNodePos = mirrorPositions().get(nearNode);
                        if (nearNodePos.z() != currentPos.z()) {
                            double weight = 1 / Math.abs(nearNodePos.z() - currentPos.z());
                            centre.plusIP(nearNodePos.times(weight));
                            weightSum += weight;
                        }
                    }
                    if (weightSum != 0) {
                        centre.divideIP(weightSum);
                        Coordinates vector = centre.minus(mirrorPositions().get(node)).restr(2);
                        Coordinates unit = Geom.e2D.unitVector(vector);
                        Coordinates force = unit.timesIP(weightSum * Geom.e2D.magnitude(vector) / desiredDistance);
                        forces.set(node, force);
                    }
                }
            }
            return forces;
        }
    }

    /**
     * Force that keeps the elements close to the graph centre.
     */
    public static class Gravity extends DyModularForce {

        private Coordinates centre = null;

        @Override
        protected NodeAttribute<Coordinates> computeForces() {
//            for(Node node : getDirectNodes()){
//                System.out.println("node in getDirectNodes = " + node);
//                Node mirrorNode = mirrorGraph().getNode(node.id());
//                Box newBox = locator().getBox(mirrorNode);
//                System.out.println("node box = " + newBox);
//                Collection<Node> closeNodes = locator().getCloseNodes(mirrorNode, 12.5);
//                for(Node closeNode : closeNodes){
//                    System.out.println("close node = " + closeNode);
//                }
//            }
            if (centre == null) {
                centre = new Coordinates(0, 0);
                for (Node node : mirrorGraph().nodes()) {
                    centre.plusIP(mirrorPositions().get(node).restr(2));
                }
                centre.divideIP(mirrorGraph().nodeCount());
            }

            NodeAttribute<Coordinates> forces = new NodeAttribute<>(new Coordinates(0, 0));
            for (Node node : mirrorGraph().nodes()) {
                Coordinates force = Geom.e2D.unitVector(centre.minus(mirrorPositions().get(node)));
                forces.set(node, force);
            }
            return forces;
        }
    }

    /**
     * Force that attract connected mirror lines.
     */
    public static class ConnectionAttraction extends DyModularForce {

        public double initialExponent = 4;
        public double finalExponent = 2;

        protected final double desiredDistance;
        private NodeAttribute<Coordinates> forces;

        /**
         * Builds a force that attract connected mirror lines.
         *
         * @param edgeEdgeDistance the ideal distance between the lines.
         */
        public ConnectionAttraction(double edgeEdgeDistance) {
            this.desiredDistance = edgeEdgeDistance;
        }

        @Override
        protected NodeAttribute<Coordinates> computeForces() {
            forces = new NodeAttribute<>(new Coordinates(0, 0));
            for (MirrorConnection connection : stcSynchronizer().mirrorConnections()) {
                Edge sourceEdge = connection.sourceMirrorLine().mirrorEdge();
                MirrorEdge source = synchronizer().getMirrorEdge(sourceEdge);
                Edge targetEdge = connection.targetMirrorLine().mirrorEdge();
                MirrorEdge target = synchronizer().getMirrorEdge(targetEdge);
                computeForce(connection, target, source);
            }
            return forces;
        }

        /**
         * Computes the space interval occupied by the given segment.
         *
         * @param segment the segment.
         * @return the segment space interval.
         */
        private Interval segmentInterval(Edge segment) {
            Coordinates sourcePos = mirrorPositions().get(segment.source());
            Coordinates targetPos = mirrorPositions().get(segment.target());
            return Interval.newClosed(sourcePos.z(), targetPos.z());
        }

        /**
         * Computes the forces relative to a given connection.
         *
         * @param connection the connection.
         * @param source the mirror edge that correspond to the source line.
         * @param target the mirror edge that correspond to the target line.
         */
        private void computeForce(MirrorConnection connection, MirrorEdge source, MirrorEdge target) {
            for (Edge a : source.segments()) {
                Interval aInt = segmentInterval(a);
                Interval aAndConn = aInt.intersection(connection.mirrorInterval());
                if (aAndConn != null) {
                    for (Edge b : target.segments()) {
                        Interval bInt = segmentInterval(b);
                        Interval bAndConn = bInt.intersection(connection.mirrorInterval());
                        if (bAndConn != null) {
                            Interval allInt = aAndConn.intersection(bAndConn);
                            if (allInt != null) {
                                double aRatio = aInt.width() == 0 ? 1 : allInt.width() / aInt.width();
                                double bRatio = bInt.width() == 0 ? 1 : allInt.width() / bInt.width();
                                Coordinates beginningVector = computeConnectingVector(a, b, allInt.leftBound());
                                Coordinates endingVector = computeConnectingVector(a, b, allInt.rightBound());
                                applyVector(beginningVector, allInt.leftBound(), a, b, aInt, bInt, aRatio, bRatio);
                                applyVector(endingVector, allInt.rightBound(), a, b, aInt, bInt, aRatio, bRatio);
                            }
                        }
                    }
                }
            }
        }

        /**
         * Calculates the vector that connects the two segments at the given z
         * value.
         *
         * @param a the first edge.
         * @param b the second edge.
         * @param z the value of z.
         * @return the vector connecting the point of a to the point of b in z.
         */
        private Coordinates computeConnectingVector(Edge a, Edge b, double z) {
            Coordinates aPoint = valueAtZ(z, a);
            Coordinates bPoint = valueAtZ(z, b);
            return bPoint.minus(aPoint);
        }

        /**
         * Gets the 2D coordinates occupied by an edge for the given z value.
         *
         * @param z the z value.
         * @param edge the edge.
         * @return the position of the edge in z.
         */
        private Coordinates valueAtZ(double z, Edge edge) {
            Coordinates sourcePos = mirrorPositions().get(edge.source());
            Coordinates targetPos = mirrorPositions().get(edge.target());
            double interpolationFactor = (z - sourcePos.z()) / (targetPos.z() - sourcePos.z());
            return (targetPos.minus(sourcePos)).timesIP(interpolationFactor)
                    .plusIP(sourcePos);
        }

        /**
         * Applies the forces caused by this vector.
         *
         * @param vector the vector.
         * @param zPos the z position of the vector.
         * @param a the first edge.
         * @param b the second edge.
         * @param aInt the interval of the first edge.
         * @param bInt the interval of the second edge.
         * @param aRatio the portion of first edge interval covered by the
         * connection.
         * @param bRatio the portion of the second edge interval covered by the
         * connection.
         */
        private void applyVector(Coordinates vector, double zPos, Edge a, Edge b,
                Interval aInt, Interval bInt, double aRatio, double bRatio) {
            double currentDistance = Geom.e2D.magnitude(vector);
            if (Geom.e2D.almostZero(currentDistance)) {
                return;
            }
            Coordinates unit = Geom.e2D.unitVector(vector);
            Coordinates baseForce = unit.timesIP(Math.pow(currentDistance / desiredDistance, computeExponent()));
            double aBalance = (zPos - aInt.leftBound()) / aInt.width();
            double bBalance = (zPos - bInt.leftBound()) / bInt.width();
            Coordinates aSourceForce = baseForce.times(aRatio * (1 - aBalance));
            Coordinates aTargetForce = baseForce.times(aRatio * aBalance);
            Coordinates bSourceForce = baseForce.times(-bRatio * (1 - bBalance));
            Coordinates bTargetForce = baseForce.times(-bRatio * bBalance);
            forces.set(a.source(), aSourceForce.plusIP(forces.get(a.source())));
            forces.set(a.target(), aTargetForce.plusIP(forces.get(a.target())));
            forces.set(b.source(), bSourceForce.plusIP(forces.get(b.source())));
            forces.set(b.target(), bTargetForce.plusIP(forces.get(b.target())));
        }

        /**
         * Computes the exponent for the given force.
         *
         * @return the exponent that corresponds to that temperature.
         */
        protected double computeExponent() {
            return finalExponent + (initialExponent - finalExponent) * temperature();
        }
    }

    /**
     * Force that repels lines too close to each other.
     */
    public static class EdgeRepulsion extends DyModularForce {

        public double initialExponent = 1;
        public double finalExponent = 3;

        protected final double desiredDistance;
        private NodeAttribute<Coordinates> forces;

        /**
         * Builds an edge repulsion force.
         *
         * @param desiredDistance the ideal edge distance.
         */
        public EdgeRepulsion(double desiredDistance) {
            this.desiredDistance = desiredDistance;
        }

        @Override
        protected NodeAttribute<Coordinates> computeForces() {
            forces = new NodeAttribute<>(new Coordinates(0, 0, 0));
            for (Node node : mirrorGraph().nodes()) {
                forces.set(node, new Coordinates(0, 0, 0));
            }

            Set<Node> nodesDone = new HashSet<>();
            for (Edge seed : mirrorGraph().edges()) {
                if (!nodesDone.contains(seed.source()) || !nodesDone.contains(seed.target())) {
                    Box seedBox = locator().getBox(seed);
                    Collection<Edge> inner = locator().getEdgesFullyInBox(seedBox.expand(4 * desiredDistance));
                    Collection<Edge> outer = locator().getEdgesPartiallyInBox(seedBox.expand(9 * desiredDistance));
                    inner.add(seed);

                    for (Edge firstSegm : inner) {
                        Edge firstLine = synchronizer().getOriginalEdge(firstSegm);
                        Node firstDyNode = stcSynchronizer().getOriginalNode(firstLine);

                        Node a = firstSegm.source();
                        Node b = firstSegm.target();
                        if (!nodesDone.contains(a) || !nodesDone.contains(b)) {
                            Coordinates aPos = mirrorPositions().get(a);
                            Coordinates bPos = mirrorPositions().get(b);

                            for (Edge secondSegm : outer) {
                                Edge secondLine = synchronizer().getOriginalEdge(secondSegm);
                                Node secondDyNode = stcSynchronizer().getOriginalNode(secondLine);

                                if (firstDyNode != secondDyNode) {
                                    Node c = secondSegm.source();
                                    Node d = secondSegm.target();
                                    Coordinates cPos = mirrorPositions().get(c);
                                    Coordinates dPos = mirrorPositions().get(d);

                                    if (!nodesDone.contains(a)) {
                                        applyNodeEdgeRepulsion(a, aPos, c, cPos, d, dPos);
                                    }
                                    if (!nodesDone.contains(b)) {
                                        applyNodeEdgeRepulsion(b, bPos, c, cPos, d, dPos);
                                    }
                                }
                            }
                            nodesDone.add(a);
                            nodesDone.add(b);
                        }
                    }
                }
            }
            return forces;
        }

        /**
         * Applies the repulsive force between a point an a segment.
         *
         * @param a the point.
         * @param aPos the position of a.
         * @param c the first segment extremity.
         * @param cPos the position of b.
         * @param d the second segment extremity.
         * @param dPos the position of c.
         */
        private void applyNodeEdgeRepulsion(Node a, Coordinates aPos, Node c, Coordinates cPos, Node d, Coordinates dPos) {
            if (Geom.e3D.almostEqual(aPos, cPos) || Geom.e3D.almostEqual(aPos, dPos)) {
                return;
            }
            PointRelation relation = Geom.e3D.pointSegmentRelation(aPos, cPos, dPos);
            Coordinates unit = Geom.e3D.unitVector(relation.closestPoint().minus(aPos));
            Coordinates baseForce = unit.timesIP(Math.pow(desiredDistance / relation.distance(), computeExponent()));
            if (relation.isProjectionIncluded()) {
                Coordinates projection = relation.projection() != null ? relation.projection() : aPos;
                double balance = Geom.e3D.magnitude(projection.minus(cPos))
                        / Geom.e3D.magnitude(dPos.minus(cPos));
                forces.set(a, baseForce.minus().plusIP(forces.get(a)));
                forces.set(c, baseForce.times(1 - balance).plusIP(forces.get(c)));
                forces.set(d, baseForce.times(balance).plusIP(forces.get(d)));
            } else {
                forces.set(a, baseForce.minus().plusIP(forces.get(a)));
                forces.set(c, baseForce.plus(forces.get(c)));
                forces.set(d, baseForce.plus(forces.get(d)));
            }
        }

        /**
         * Computes the exponent for the given force.
         *
         * @return the exponent that corresponds to that temperature.
         */
        protected double computeExponent() {
            return finalExponent + (initialExponent - finalExponent) * temperature();
        }
    }

    public static class PoleAttraction extends DyModularForce {

        /**
         * Used to determine the distance of the repulsion boundary from the pole node
         * Prevents the cluster members clustering too densely
         */
        public double attractionFactor = 50;

        protected final double desiredDistance;
        private NodeAttribute<Coordinates> forces;

        /**
         * Builds an edge repulsion force.
         *
         * @param desiredDistance the ideal edge distance.
         */
        public PoleAttraction(double desiredDistance) {
            this.desiredDistance = desiredDistance;
        }

        @Override
        protected NodeAttribute<Coordinates> computeForces() {
            forces = new NodeAttribute<>(new Coordinates(0, 0));

            for(Cluster cluster : stcSynchronizer().originalGraph().clusters()){

                Node mirrorPole = mirrorGraph().getNode(cluster.pole().id());
                Coordinates poleCoordinates = mirrorPositions().get(mirrorPole);

                for (Node memberNode : cluster.members()) {

                    Node mirrorMemberNode = mirrorGraph().getNode(memberNode.id());
                    Coordinates force = Geom.e2D.unitVector(poleCoordinates.minus(mirrorPositions().get(mirrorMemberNode))).timesIP(attractionFactor).minusIP();
                    forces.set(mirrorMemberNode, force);
                }
            }

            return forces;
        }
    }

    public static class CircumferenceRepulsion extends DyModularForce {

        /**
         * Used to determine the distance of the repulsion boundary from the pole node
         * Prevents the cluster members clustering too densely
         */
        public double repulsionFactor = 60;


        private NodeAttribute<Coordinates> forces;

        /**
         * Builds a circumference repulsion force.
         * Member nodes of this cluster will be repulsed from the edge of the circle towards the pole.
         */

        @Override
        protected NodeAttribute<Coordinates> computeForces() {
            forces = new NodeAttribute<>(new Coordinates(0, 0));

            for(Cluster cluster : stcSynchronizer().originalGraph().clusters()){

                Node mirrorPole = mirrorGraph().getNode(cluster.pole().id());
                Coordinates mirrorPoleCoordinates = mirrorPositions().get(mirrorPole);

                List<Coordinates> faceBoundaryPointsList = computeFaceBoundary(mirrorPoleCoordinates, 12.5);

                for (Node memberNode : cluster.members()) {

                    Node mirrorMemberNode = mirrorGraph().getNode(memberNode.id());

                    Coordinates mirrorMemberCoordinates = mirrorPositions().get(mirrorMemberNode);

                    for(int i = 0; i < faceBoundaryPointsList.size(); i++){
                        Coordinates aPos = faceBoundaryPointsList.get(i);
                        Coordinates bPos;
                        if(i < faceBoundaryPointsList.size() - 1){
                            bPos = faceBoundaryPointsList.get(i + 1);
                        } else {
                            bPos = faceBoundaryPointsList.get(0);
                        }
                        PointRelation pointSegmentRelation = Geom.e3D.pointSegmentRelation(aPos, bPos, mirrorMemberCoordinates);
                        Coordinates projection = pointSegmentRelation.projection() != null ? pointSegmentRelation.projection() : aPos;

                        Coordinates force = computeForce(mirrorMemberCoordinates, projection);
                        forces.set(memberNode, force);
                    }
                }
            }

            return forces;
        }

        /**
         * Computes coordinates of a projection point of a member node on the cluster's circumference
         * which will repulse the member node towards the centre
         * @param poleCoordinates cluster centre coordinates
         * @param memberCoordinates member coordinates
         * @return
         */
        protected Coordinates computeProjectionPoint(Coordinates poleCoordinates, Coordinates memberCoordinates){
            double radiusXSquared = Math.pow(poleCoordinates.x() + 25, 2);
            double radiusYSquared = Math.pow(poleCoordinates.y() + 25, 2);
            double radiusLength = Math.sqrt(radiusXSquared + radiusYSquared);
            return new Coordinates(poleCoordinates.x() + radiusLength, poleCoordinates.y() + radiusLength);
        }

        protected Coordinates computeDistancePoint(Coordinates poleCoordinates){
            double radiusXSquared = Math.pow(poleCoordinates.x() + 24, 2);
            double radiusYSquared = Math.pow(poleCoordinates.y() + 24, 2);
            double radiusLength = Math.sqrt(radiusXSquared + radiusYSquared);
            return new Coordinates(poleCoordinates.x() + radiusLength, poleCoordinates.y() + radiusLength);
        }


        protected Coordinates computeForce(Coordinates memberCoordinates,
                                           Coordinates projectionCoordinates){

            double memberToProjectionMagnitude = Geom.e2D.magnitude(memberCoordinates.minus(projectionCoordinates));

            double desiredDistanceMinusMagnitude = 1 - memberToProjectionMagnitude;
            double dividend = Math.pow(desiredDistanceMinusMagnitude, 4);
            double divisor = memberToProjectionMagnitude;

            double quotient = dividend / divisor;

            Coordinates multipliedCoordinates = memberCoordinates.minus(projectionCoordinates);

            Coordinates force = multipliedCoordinates.timesIP(quotient);

            return force;

        }

        /**
         * Computes the coordinates of a set of nodes that describe an artificial graph face overlaid
         * on the circumference of a graph cluster circle
         * @param centrePoint Coordinates of the pole node
         * @param radius radius of the cluster circle
         * @return List of Coordinates describing an artificial graph face superimposed on the circle
         */
        protected List<Coordinates> computeFaceBoundary(Coordinates centrePoint, double radius){
            int faceDegree = 32;

            List<Coordinates> faceNodesCoordinatesList = new ArrayList<Coordinates>();

            for(int i = 0; i < faceDegree; i++){
                double xCoordinates = centrePoint.x() + radius * Math.cos(2 * Math.PI * i / faceDegree);
                double yCoordinates = centrePoint.y() + radius * Math.sin(2 * Math.PI * i / faceDegree);

                Coordinates newFaceNodeCoordinates = new Coordinates(xCoordinates, yCoordinates);
                faceNodesCoordinatesList.add(newFaceNodeCoordinates);

            }

            return faceNodesCoordinatesList;
        }



    }

    public static class NonClusterNodesRepulsion extends DyModularForce {

        private NodeAttribute<Coordinates> forces;
        private List<Node> poleNodes;

        /**
         * The desired distance between two nodes.
         */
        protected double nodeNodeDistance;
        protected double distanceMargin = 10;
        /**
         * The factor of the desired distance at witch two nodes are close
         * enough to be significantly effecting each other.
         */
        public double distanceActivityFactor = 3;
        /**
         * The initial force exponent.
         */
        public double initialExponent = 4;
        /**
         * The final force exponent.
         */
        public double finalExponent = 2;

        public NonClusterNodesRepulsion(List<Node> poleNodes, double radius){
            this.nodeNodeDistance = radius + this.distanceMargin;
            this.poleNodes = poleNodes;
        }


        public NonClusterNodesRepulsion(double radius){
            this.nodeNodeDistance = radius + this.distanceMargin;
        }

        @Override
        protected NodeAttribute<Coordinates> computeForces() {
            forces = new NodeAttribute<>(new Coordinates(0, 0));

            Set<Node> clusteredNodesSet = new HashSet<>();
            Set<Node> clusterPoles = new HashSet<>();
            Set<Node> nonClusteredNodes = new HashSet<>();

            for(Cluster cluster : stcSynchronizer().originalGraph().clusters()){
                Node mirrorPole = mirrorGraph().getNode(cluster.pole().id());
                clusteredNodesSet.add(mirrorPole);
                clusterPoles.add(mirrorPole);

                for(Node memberNode : cluster.members()){
                    Node mirrorMemberNode = mirrorGraph().getNode(memberNode.id());
                    clusteredNodesSet.add(mirrorMemberNode);
                }
            }

            for(Node node : stcSynchronizer().originalGraph().nodes()){
                if(!clusteredNodesSet.contains(node)){
                    //System.out.println("adding a nonClusteredNode = " + node);
                    nonClusteredNodes.add(node);
                }
            }

            Set<Node> mirrorNonClusteredNodes = new HashSet<>();
            for(Node nonClusteredNode : nonClusteredNodes){
                Node mirrorNode = mirrorGraph().getNode(nonClusteredNode.id());
                mirrorNonClusteredNodes.add(mirrorNode);
            }

            for(Node pole : clusterPoles){
                Collection<Node> withinCluster = locator().getCloseNodes(pole, 25);
                withinCluster.removeAll(clusteredNodesSet);
                withinCluster = pruneSynchroTargetNodes(withinCluster);
                //System.out.println("pole node = " + pole);
                for(Node nodeToRepel : withinCluster){
                    //System.out.println("nodeToRepel = " + nodeToRepel);
                    Coordinates[] computedForces = computeForces(pole, nodeToRepel);
                    forces.set(pole, computedForces[0].plusIP(forces.get(pole)));
                    forces.set(nodeToRepel, computedForces[1].plusIP(forces.get(nodeToRepel)));
                }
            }



            return forces;
        }

        protected double desiredDistance() {
            return this.nodeNodeDistance;
        }

        /**
         * Computes the forces for the given pair of nodes and temperature. The
         * force to apply to the first node will be contained in the position 0
         * of the returned array, and the force to apply to the second node in
         * position 1.
         *
         * @param nodeA the first node.
         * @param nodeB the second node.
         * @return the pair of forces to apply to the given nodes.
         */
        protected Coordinates[] computeForces(Node nodeA, Node nodeB) {
            Coordinates a = mirrorPositions().get(nodeA);
            Coordinates b = mirrorPositions().get(nodeB);
            Coordinates ab = Geom.e2D.unitVector(a.restrMinus(b, 2));
            DyDistances distances = computeDyDistances(nodeA, nodeB, temperature());
            Coordinates force = (ab).timesIP(Math.pow(distances.desiredDistance, 8));
            return new Coordinates[]{force, force.minus()};
        }

        /**
         * Computes the exponent for the given force.
         *
         * @return the exponent that corresponds to that temperature.
         */
        protected double computeExponent() {
            return finalExponent + (initialExponent - finalExponent);
        }



        /**
         * Computes current and desired distances between two nodes.
         *
         * @param a the first node.
         * @param b the second node.
         * @param temperature the temperature.
         * @return the distances.
         */
        protected DyDistances computeDyDistances(Node a, Node b, double temperature) {
            NodeAttribute<Coordinates> mirrorSizes = mirrorGraph().nodeAttribute(StdAttribute.nodeSize);
            NodeAttribute<StdAttribute.NodeShape> mirrorShapes = mirrorGraph().nodeAttribute(StdAttribute.nodeShape);
            Coordinates aPos = mirrorPositions().get(a);
            Coordinates bPos = mirrorPositions().get(b);
            double distanceAtZeroSize = Geom.e2D.magnitude(bPos.restrMinus(aPos, 2));
            double distanceAtFullSize = Layout2D.nodeNodeGlyphDistance(mirrorGraph(), a, b, mirrorPositions(), mirrorSizes, mirrorShapes);
            double elemRadiusAtFullSize = distanceAtZeroSize - distanceAtFullSize;
            double currentElemRadius = elemRadiusAtFullSize * (1 - temperature);
            double currentDistance = distanceAtZeroSize - currentElemRadius;
            double desiredDistance = this.nodeNodeDistance + elemRadiusAtFullSize - currentElemRadius;
            //System.out.println("node a  = " + a + " | node b = " + b);
            //System.out.println("current distance = " + currentDistance + " desired = " + desiredDistance);
            return new DyDistances(currentDistance, desiredDistance);
        }
    }
}
