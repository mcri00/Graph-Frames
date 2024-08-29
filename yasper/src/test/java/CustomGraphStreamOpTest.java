import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.streamreasoning.rsp4j.api.operators.s2r.execution.instance.Window;
import org.streamreasoning.rsp4j.api.secret.content.Content;
import org.streamreasoning.rsp4j.api.secret.content.ContentFactory;
import org.streamreasoning.rsp4j.api.secret.report.Report;
import org.streamreasoning.rsp4j.api.secret.time.Time;
import org.streamreasoning.rsp4j.api.enums.ReportGrain;
import org.streamreasoning.rsp4j.api.enums.Tick;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomGraphStreamOpTest {

    private CustomGraphStreamOp<String> customGraphStreamOp;

    @BeforeEach
    public void setUp(GraphProcessingAlgorithm<String> algorithm) {
        // Inizializza CustomGraphStreamOp con un algoritmo specifico
        customGraphStreamOp = new CustomGraphStreamOp<>(
                IRI.create("http://example.org/customOp"),
                60000, // Window size in milliseconds
                algorithm,
                new TimeImpl(),
                Tick.TIME_DRIVEN,
                new ReportImpl(),
                ReportGrain.SINGLE,
                new ContentGeneralGraphFactoryImpl());
    }

    @ParameterizedTest
    @ArgumentsSource(GraphAlgorithmProvider.class)
    public void testWindowingBasedOnAlgorithm(GraphProcessingAlgorithm<String> algorithm) {
        setUp(algorithm);

        // Crea un grafo di test
        Graph<String, DefaultEdge> testGraph = new SimpleGraph<>(DefaultEdge.class);
        testGraph.addVertex("A");
        testGraph.addVertex("B");
        testGraph.addVertex("C");
        testGraph.addEdge("A", "B");
        testGraph.addEdge("B", "C");

        // Applica il metodo windowing con il grafo e un timestamp simulato
        customGraphStreamOp.windowing(testGraph, System.currentTimeMillis());

        // Verifica se la finestra dovrebbe chiudersi in base all'algoritmo
        assertTrue(algorithm.shouldCloseWindow(testGraph));
    }

    @ParameterizedTest
    @ArgumentsSource(GraphAlgorithmProvider.class)
    public void testContentCoalesce(GraphProcessingAlgorithm<String> algorithm) {
        setUp(algorithm);

        // Crea un grafo di test
        Graph<String, DefaultEdge> testGraph = new SimpleGraph<>(DefaultEdge.class);
        testGraph.addVertex("A");
        testGraph.addVertex("B");
        testGraph.addVertex("C");
        testGraph.addEdge("A", "B");
        testGraph.addEdge("B", "C");

        // Aggiungi il grafo al CustomGraphStreamOp
        customGraphStreamOp.windowing(testGraph, System.currentTimeMillis());

        // Verifica la corretta unione dei contenuti della finestra
        ContentGeneralGraph<String> content = new ContentGeneralGraph<>(new TimeImpl());
        content.add(testGraph);

        // Verifica se il grafo risultante contiene i vertici e gli archi previsti
        Graph<String, DefaultEdge> combinedGraph = content.coalesce().getGraph();
        assertTrue(combinedGraph.containsVertex("A"));
        assertTrue(combinedGraph.containsVertex("B"));
        assertTrue(combinedGraph.containsVertex("C"));
        assertTrue(combinedGraph.containsEdge("A", "B"));
        assertTrue(combinedGraph.containsEdge("B", "C"));
    }

    public static class GraphAlgorithmProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of(new BetweennessCentralityAlgorithm<>(0.5)),
                    Arguments.of(new GraphDiameterAlgorithm<>(3.0)),
                    Arguments.of(new ShortestPathAlgorithm<>("A", "C", 2.0))
            );
        }
    }

    // Implementazione di TimeImpl e ReportImpl per i test
    static class TimeImpl implements Time {
        private long appTime;

        @Override
        public long getAppTime() {
            return appTime;
        }

        @Override
        public void setAppTime(long appTime) {
            this.appTime = appTime;
        }

        @Override
        public long getSystemTime() {
            return System.currentTimeMillis();
        }
    }

    static class ReportImpl implements Report {
        @Override
        public boolean report(Window window, Content content, long t_e, long sysTime) {
            return true;
        }
    }

    // Implementazione di una ContentFactory per il test
    static class ContentGeneralGraphFactoryImpl implements ContentFactory<Graph<String, DefaultEdge>, Graph<String, DefaultEdge>, IterableGraph<String>> {
        @Override
        public ContentGeneralGraph<String> create() {
            return new ContentGeneralGraph<>(new TimeImpl());
        }
    }
}
