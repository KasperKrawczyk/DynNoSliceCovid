package ocotillo.graph;

public class ClusterAttribute<V> extends ElementAttribute<Cluster, V> {
    /**
     * Constructs an edge attribute.
     *
     * @param defaultValue the value of an edge when not directly set.
     */
    public ClusterAttribute(V defaultValue) {
        super(defaultValue);
    }

    @Override
    public Attribute.Type getAttributeType() {
        return Attribute.Type.cluster;
    }
}
