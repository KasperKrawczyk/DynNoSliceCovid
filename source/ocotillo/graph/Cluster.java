package ocotillo.graph;

import lombok.*;
import ocotillo.geometry.*;

import java.util.*;

@EqualsAndHashCode(callSuper = true)
public class Cluster extends Element{

    /**
     * Experimentally established factor used in determining the radius of the cluster's circle
     */
    private static final double RADIUS_FACTOR = 2.25;
    private final Node pole;
    private final List<Node> membersList;
    private Coordinates radius;

    /**
     * Constructs a cluster. Pole's id gets assigned as the cluster's id
     *
     * @param pole centre of the cluster
     * @param membersList list of nodes contained in the cluster
     */
    public Cluster(Node pole, List<Node> membersList) {
        super(pole.id());
        this.pole = pole;
        this.membersList = membersList;
    }

    /**
     * Gets the pole of the cluster
     *
     * @return the cluster pole
     */
    public Node pole() {
        return this.pole;
    }

    /**
     * Gets the list of members of the cluster, without the pole
     *
     * @return list of members of the cluster, without the pole
     */
    public List<Node> members(){
        return this.membersList;
    }

    /**
     * Add a member to the members list of the cluster
     */
    public void addMember(Node node){
        membersList.add(node);
    }

    /**
     * Add a list of members to the members list of the cluster
     */
    public void addMembers(List<Node> nodesList){
        membersList.addAll(nodesList);
    }

    /**
     * Checks if a node is a member of the cluster
     *
     * @param node the node
     * @return true if a node belongs to the cluster as its member, false otherwise
     */
    public boolean isNodeMember(Node node){
        return this.membersList.contains(node);
    }

    /**
     * Checks if a node is the pole of the cluster
     *
     * @param node the node
     * @return true if the node is the pole of the cluster, false otherwise
     */
    public boolean isNodePole(Node node){
        return this.pole.equals(node);
    }

    public void setRadius(){
        double radiusCoordinatesScalar = this.members().size() * Cluster.RADIUS_FACTOR;
        this.radius = new Coordinates(radiusCoordinatesScalar, radiusCoordinatesScalar);
    }

//    public Coordinates getRadius(Graph graph){
//        Node initRadiusNode = this.membersList.get(0);
//        Coordinates radius = graph.nodeAttribute(initRadiusNode.id(), Coordinates)
//        graph.nodeAttribute()
//    }


}
