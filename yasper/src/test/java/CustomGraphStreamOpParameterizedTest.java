import org.apache.commons.rdf.api.IRI;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.streamreasoning.rsp4j.api.enums.ReportGrain;
import org.streamreasoning.rsp4j.api.enums.Tick;
import org.streamreasoning.rsp4j.api.sds.timevarying.TimeVarying;
import org.streamreasoning.rsp4j.api.secret.report.ReportImpl;
import org.streamreasoning.rsp4j.api.secret.time.TimeImpl;
import org.streamreasoning.rsp4j.api.stream.data.DataStream;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomGraphStreamOpParameterizedTest {

    private CustomGraphStreamOp<> customGraphStreamOp;
    private DataStream<String> dataStream;
    private TimeVarying<String> timeVarying;

    @BeforeEach
    public void setUp(GraphProcessingAlgorithm<String> algorithm) {
        // Create an instance of the CustomGraphStreamOp
        customGraphStreamOp = new CustomGraphStreamOp<>(
                IRI.create("http://example.org/customOp"),
                60000, // Window size in milliseconds
                algorithm,
                new TimeImpl(),
                Tick.TIME_DRIVEN,
                new ReportImpl(),
                ReportGrain.ONE,
                new ContentFactoryImpl());

        // Create a data stream and apply the operator
        dataStream = new DataStreamImpl<>();
        timeVarying = customGraphStreamOp.apply(dataStream);

        // Manually create edges to form the initial graph structure
        customGraphStreamOp.graph.addVertex("A");
        customGraphStreamOp.graph.addVertex("B");
        customGraphStreamOp.graph.addVertex("C");
        customGraphStreamOp.graph.addEdge("A", "B");
        customGraphStreamOp.graph.addEdge("B", "C");
    }

    @ParameterizedTest
    @ArgumentsSource(GraphAlgorithmProvider.class)
    public void testWindowingBasedOnAlgorithm(GraphProcessingAlgorithm<String> algorithm) {
        setUp(algorithm);

        // Add new nodes to the data stream
        dataStream.add("D", System.currentTimeMillis());
        dataStream.add("E", System.currentTimeMillis() + 1000);

        // Add edges to form a graph including the new nodes
        customGraphStreamOp.graph.addEdge("C", "D");
        customGraphStreamOp.graph.addEdge("D", "E");

        // Process the graph with the algorithm
        algorithm.process(customGraphStreamOp.graph);

        // Check if the window should be closed based on the algorithm
        assertTrue(algorithm.shouldCloseWindow(customGraphStreamOp.graph));
    }

    @ParameterizedTest
    @ArgumentsSource(GraphAlgorithmProvider.class)
    public void testContentCoalesce(GraphProcessingAlgorithm<String> algorithm) {
        setUp(algorithm);

        // Add new nodes to the data stream
        dataStream.add("D", System.currentTimeMillis());
        dataStream.add("E", System.currentTimeMillis() + 1000);

        // Add edges to form a graph including the new nodes
        customGraphStreamOp.graph.addEdge("C", "D");
        customGraphStreamOp.graph.addEdge("D", "E");

        // Combine the elements into a single graph
        ContentGeneralGraph<String> content = new ContentGeneralGraph<>(new TimeImpl());
        content.add(customGraphStreamOp.graph);

        // Check if the combined graph contains the expected vertices and edges
        Graph<String, DefaultEdge> combinedGraph = content.coalesce();
        assertTrue(combinedGraph.containsVertex("A"));
        assertTrue(combinedGraph.containsVertex("B"));
        assertTrue(combinedGraph.containsVertex("C"));
        assertTrue(combinedGraph.containsVertex("D"));
        assertTrue(combinedGraph.containsVertex("E"));
        assertTrue(combinedGraph.containsEdge("A", "B"));
        assertTrue(combinedGraph.containsEdge("B", "C"));
        assertTrue(combinedGraph.containsEdge("C", "D"));
        assertTrue(combinedGraph.containsEdge("D", "E"));
    }

    static class GraphAlgorithmProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ParameterContext context) {
            return Stream.of(
                    Arguments.of(new BetweennessCentralityAlgorithm<>(0.5)),
                    Arguments.of(new GraphDiameterAlgorithm<>(3.0)),
                    Arguments.of(new ShortestPathAlgorithm<>("A", "E", 3.0))
            );
        }
    }
}
