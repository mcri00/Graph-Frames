import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.streamreasoning.rsp4j.api.secret.content.Content;
import org.streamreasoning.rsp4j.api.secret.time.Time;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ContentGeneralGraph<T> implements Content<Graph<T, DefaultEdge>, Graph<T, DefaultEdge>, IterableGraph<T>> {
    private final Time instance;
    private final Set<Graph<T, DefaultEdge>> elements;
    private long last_timestamp_changed;

    public ContentGeneralGraph(Time instance) {
        this.instance = instance;
        this.elements = new HashSet<>();
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public void add(Graph<T, DefaultEdge> e) {
        elements.add(e);
        this.last_timestamp_changed = instance.getAppTime();
    }



    @Override
    public String toString() {
        return elements.toString();
    }

    @Override
    public IterableGraph<T> coalesce() {
        if (elements.size() == 1) {
            return new IterableGraph<>(elements.stream().findFirst().orElseGet(() -> new SimpleGraph<>(DefaultEdge.class)));
        } else {
            Graph<T, DefaultEdge> combinedGraph = new SimpleGraph<>(DefaultEdge.class);
            for (Graph<T, DefaultEdge> graph : elements) {
                for (T vertex : graph.vertexSet()) {
                    combinedGraph.addVertex(vertex);
                }
                for (DefaultEdge edge : graph.edgeSet()) {
                    combinedGraph.addEdge(graph.getEdgeSource(edge), graph.getEdgeTarget(edge));
                }
            }
            return new IterableGraph<>(combinedGraph);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContentGeneralGraph<?> that = (ContentGeneralGraph<?>) o;
        return last_timestamp_changed == that.last_timestamp_changed &&
                Objects.equals(elements, that.elements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elements, last_timestamp_changed);
    }
}
