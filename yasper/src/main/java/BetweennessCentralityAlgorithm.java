import org.jgrapht.Graph;
import org.jgrapht.alg.scoring.BetweennessCentrality;
import org.jgrapht.graph.DefaultEdge;

import java.util.Map;

public class BetweennessCentralityAlgorithm<T> implements GraphProcessingAlgorithm<T> {

    private double threshold;

    public BetweennessCentralityAlgorithm(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public void process(Graph<T, DefaultEdge> graph) {
        BetweennessCentrality<T, DefaultEdge> bc = new BetweennessCentrality<>(graph);
        Map<T, Double> scores = bc.getScores();
        scores.forEach((vertex, score) -> System.out.println("Vertex: " + vertex + " Betweenness Centrality: " + score));
    }

    @Override
    public boolean shouldCloseWindow(Graph<T, DefaultEdge> graph) {
        BetweennessCentrality<T, DefaultEdge> bc = new BetweennessCentrality<>(graph);
        return bc.getScores().values().stream().anyMatch(score -> score >= threshold);
    }
}
