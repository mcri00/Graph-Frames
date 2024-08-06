import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.graph.DefaultEdge;

public class GraphDiameterAlgorithm<T> implements GraphProcessingAlgorithm<T> {

    private final double threshold;
    private double currentDiameter;

    public GraphDiameterAlgorithm(double threshold) {
        this.threshold = threshold;
        this.currentDiameter = 0;
    }

    @Override
    public void process(Graph<T, DefaultEdge> graph) {
        // Calculate the diameter of the graph
        updateDiameter(graph);
    }

    @Override
    public boolean shouldCloseWindow(Graph<T, DefaultEdge> graph) {
        // Check if the current diameter exceeds the threshold
        return currentDiameter > threshold;
    }

    private void updateDiameter(Graph<T, DefaultEdge> graph) {
        // Use Floyd-Warshall algorithm to calculate all pairs shortest paths
        FloydWarshallShortestPaths<T, DefaultEdge> floydWarshall = new FloydWarshallShortestPaths<>(graph);

        // Update the current diameter
        currentDiameter = 0;
        for (T source : graph.vertexSet()) {
            for (T target : graph.vertexSet()) {
                double distance = floydWarshall.getPathWeight(source, target);
                if (distance > currentDiameter) {
                    currentDiameter = distance;
                }
            }
        }

        // Print the current diameter for debugging purposes
        System.out.println("Current graph diameter: " + currentDiameter);
    }
}
