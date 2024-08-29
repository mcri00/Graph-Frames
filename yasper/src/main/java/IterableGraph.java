import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.util.Iterator;

public class IterableGraph<T> implements Iterable<T> {
    private Graph<T, DefaultEdge> graph;

    public IterableGraph(Graph<T, DefaultEdge> graph){
        this.graph = graph;
    }
    @Override
    public Iterator<T> iterator() {
        return graph.vertexSet().iterator();
    }
    public Graph<T, DefaultEdge> getGraph() {
        return this.graph;
    }
}
