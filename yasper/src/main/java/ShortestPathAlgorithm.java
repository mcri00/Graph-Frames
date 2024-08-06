import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;

import java.util.HashMap;
import java.util.Map;

public class ShortestPathAlgorithm<T> implements GraphProcessingAlgorithm<T> {

    private final T startVertex;
    private final T endVertex;
    private final double threshold;
    private final Map<T, Double> distanceMap;

    public ShortestPathAlgorithm(T startVertex, T endVertex, double threshold) {
        this.startVertex = startVertex;
        this.endVertex = endVertex;
        this.threshold = threshold;
        this.distanceMap = new HashMap<>();
    }

    @Override
    public void process(Graph<T, DefaultEdge> graph) {
        // Update distance map incrementally
        updateDistanceMap(graph);
    }

    @Override
    public boolean shouldCloseWindow(Graph<T, DefaultEdge> graph) {
        // Use the distance map to get the shortest path length
        Double shortestPathLength = distanceMap.getOrDefault(endVertex, Double.POSITIVE_INFINITY);

        // Close the window if the shortest path length is greater than the threshold
        return shortestPathLength > threshold;
    }

    private void updateDistanceMap(Graph<T, DefaultEdge> graph) {
        // Use Dijkstra algorithm to update the shortest paths from the start vertex
        DijkstraShortestPath<T, DefaultEdge> dijkstraAlg = new DijkstraShortestPath<>(graph);

        // Update distances for all vertices from the start vertex
        for (T vertex : graph.vertexSet()) {
            double distance = dijkstraAlg.getPathWeight(startVertex, vertex);
            distanceMap.put(vertex, distance);
        }
    }
}
