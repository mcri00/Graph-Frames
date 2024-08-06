import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

public interface GraphProcessingAlgorithm<T> {
    void process(Graph<T, DefaultEdge> graph);
    boolean shouldCloseWindow(Graph<T, DefaultEdge> graph);
}
