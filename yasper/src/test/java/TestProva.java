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
import org.streamreasoning.rsp4j.api.RDFUtils;
import org.streamreasoning.rsp4j.api.exceptions.OutOfOrderElementException;
import org.streamreasoning.rsp4j.api.operators.s2r.execution.instance.Window;
import org.streamreasoning.rsp4j.api.secret.content.Content;
import org.streamreasoning.rsp4j.api.secret.content.ContentFactory;
import org.streamreasoning.rsp4j.api.secret.report.Report;
import org.streamreasoning.rsp4j.api.secret.report.strategies.ReportingStrategy;
import org.streamreasoning.rsp4j.api.secret.time.ET;
import org.streamreasoning.rsp4j.api.secret.time.Time;
import org.streamreasoning.rsp4j.api.enums.ReportGrain;
import org.streamreasoning.rsp4j.api.enums.Tick;
import org.streamreasoning.rsp4j.api.secret.time.TimeInstant;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class TestProva {

    private CustomGraphStreamOp<String> customGraphStreamOp;

    public void setUp(GraphProcessingAlgorithm<String> algorithm) {
        // Inizializza CustomGraphStreamOp con un algoritmo specifico
        customGraphStreamOp = new CustomGraphStreamOp<>(
                RDFUtils.createIRI("http://example.org/customOp"),
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

    // Test di Casi Limite e Consistenza

    @ParameterizedTest
    @ArgumentsSource(GraphAlgorithmProvider.class)
    public void testEmptyGraph(GraphProcessingAlgorithm<String> algorithm) {
        setUp(algorithm);

        // Crea un grafo vuoto
        Graph<String, DefaultEdge> testGraph = new SimpleGraph<>(DefaultEdge.class);

        // Aggiungi il grafo vuoto al CustomGraphStreamOp
        customGraphStreamOp.windowing(testGraph, System.currentTimeMillis());

        // Verifica che il grafo risultante sia vuoto
        IterableGraph<String> resultGraph = customGraphStreamOp.get().get();
        assertNotNull(resultGraph, "Resulting graph should not be null");
        assertTrue(resultGraph.getGraph().vertexSet().isEmpty());
        assertTrue(resultGraph.getGraph().edgeSet().isEmpty());
    }

    @ParameterizedTest
    @ArgumentsSource(GraphAlgorithmProvider.class)
    public void testSingleNodeGraph(GraphProcessingAlgorithm<String> algorithm) {
        setUp(algorithm);

        // Crea un grafo con un singolo nodo
        Graph<String, DefaultEdge> testGraph = new SimpleGraph<>(DefaultEdge.class);
        testGraph.addVertex("A");

        // Aggiungi il grafo al CustomGraphStreamOp
        customGraphStreamOp.windowing(testGraph, System.currentTimeMillis());

        // Verifica che il grafo contenuto abbia solo il singolo nodo
        assertTrue(customGraphStreamOp.get().get().getGraph().containsVertex("A"));
        assertFalse(customGraphStreamOp.get().get().getGraph().containsEdge("A", "B")); // Nessun edge dovrebbe esserci
    }

    @ParameterizedTest
    @ArgumentsSource(GraphAlgorithmProvider.class)
    public void testOutOfOrderUpdates(GraphProcessingAlgorithm<String> algorithm) {
        setUp(algorithm);

        // Crea un grafo di test
        Graph<String, DefaultEdge> testGraph = new SimpleGraph<>(DefaultEdge.class);
        testGraph.addVertex("A");
        testGraph.addVertex("B");
        testGraph.addEdge("A", "B");

        long currentTime = System.currentTimeMillis();

        // Aggiungi il grafo con un timestamp corretto
        customGraphStreamOp.windowing(testGraph, currentTime);

        // Verifica lo stato del grafo dopo la prima aggiunta
        IterableGraph<String> resultGraph = customGraphStreamOp.get().get();
        assertTrue(resultGraph.getGraph().containsVertex("A"));
        assertTrue(resultGraph.getGraph().containsVertex("B"));
        assertTrue(resultGraph.getGraph().containsEdge("A", "B"));

        // Incrementa il tempo per essere sicuri che sia "fuori ordine"
        long outOfOrderTime = currentTime - 100000;

        // Prova ad aggiungere lo stesso grafo con un timestamp fuori ordine
        try {
            customGraphStreamOp.windowing(testGraph, outOfOrderTime);
            fail("Expected OutOfOrderElementException was not thrown.");
        } catch (OutOfOrderElementException e) {
            // Success: l'elemento fuori ordine viene rilevato e gestito correttamente
        }

        // Verifica che il grafo non sia stato modificato dall'elemento fuori ordine
        IterableGraph<String> resultGraphAfterException = customGraphStreamOp.get().get();
        assertTrue(resultGraphAfterException.getGraph().containsVertex("A"));
        assertTrue(resultGraphAfterException.getGraph().containsVertex("B"));
        assertTrue(resultGraphAfterException.getGraph().containsEdge("A", "B"));

        // Verifica che AppTime sia stato aggiornato correttamente e non sia cambiato con l'elemento fuori ordine
        assertEquals(currentTime, customGraphStreamOp.getTime().getAppTime(), "AppTime should remain the same if the element is out of order.");
    }



    @ParameterizedTest
    @ArgumentsSource(GraphAlgorithmProvider.class)
    public void testGraphConsistency(GraphProcessingAlgorithm<String> algorithm) {
        setUp(algorithm);

        // Crea un grafo di test
        Graph<String, DefaultEdge> testGraph = new SimpleGraph<>(DefaultEdge.class);
        testGraph.addVertex("A");
        testGraph.addVertex("B");
        testGraph.addEdge("A", "B");

        // Aggiungi il grafo al CustomGraphStreamOp
        customGraphStreamOp.windowing(testGraph, System.currentTimeMillis());

        // Verifica che il grafo non sia in uno stato inconsistente
        Graph<String, DefaultEdge> resultGraph = customGraphStreamOp.get().get().getGraph();
        assertTrue(resultGraph.containsVertex("A"));
        assertTrue(resultGraph.containsVertex("B"));
        assertTrue(resultGraph.containsEdge("A", "B"));

        // Esegui una serie di operazioni
        customGraphStreamOp.windowing(testGraph, System.currentTimeMillis());
        customGraphStreamOp.windowing(testGraph, System.currentTimeMillis());

        // Verifica di nuovo la consistenza
        resultGraph = customGraphStreamOp.get().get().getGraph();
        assertTrue(resultGraph.containsVertex("A"));
        assertTrue(resultGraph.containsVertex("B"));
        assertTrue(resultGraph.containsEdge("A", "B"));
    }

    // Test per funzioni specifiche

    @ParameterizedTest
    @ArgumentsSource(GraphAlgorithmProvider.class)
    public void testEvict(GraphProcessingAlgorithm<String> algorithm) {
        setUp(algorithm);

        // Crea un grafo di test
        Graph<String, DefaultEdge> testGraph = new SimpleGraph<>(DefaultEdge.class);
        testGraph.addVertex("A");
        testGraph.addVertex("B");
        testGraph.addEdge("A", "B");

        // Applica evict e verifica che il grafo sia vuoto
        customGraphStreamOp.evict();
        assertTrue(customGraphStreamOp.get().get().getGraph().vertexSet().isEmpty());
    }

    @ParameterizedTest
    @ArgumentsSource(GraphAlgorithmProvider.class)
    public void testCleanUpWindows(GraphProcessingAlgorithm<String> algorithm) {
        setUp(algorithm);

        // Crea un grafo di test
        Graph<String, DefaultEdge> testGraph = new SimpleGraph<>(DefaultEdge.class);
        testGraph.addVertex("A");
        testGraph.addVertex("B");
        testGraph.addEdge("A", "B");

        // Simula il passare del tempo e verifica che la finestra venga pulita correttamente
        customGraphStreamOp.windowing(testGraph, System.currentTimeMillis());
        // Per testare cleanUpWindows indirettamente, puoi chiamare windowing di nuovo con un timestamp avanzato
        customGraphStreamOp.windowing(testGraph, System.currentTimeMillis() + 100000); // Passa un tempo superiore alla window size

        // Verifica che gli elementi fuori dalla finestra siano stati rimossi
        assertTrue(customGraphStreamOp.get().get().getGraph().vertexSet().isEmpty());
    }


    @ParameterizedTest
    @ArgumentsSource(GraphAlgorithmProvider.class)
    public void testCyclicGraph(GraphProcessingAlgorithm<String> algorithm) {
        setUp(algorithm);

        // Crea un grafo ciclico di test
        Graph<String, DefaultEdge> testGraph = new SimpleGraph<>(DefaultEdge.class);
        testGraph.addVertex("A");
        testGraph.addVertex("B");
        testGraph.addVertex("C");
        testGraph.addEdge("A", "B");
        testGraph.addEdge("B", "C");
        testGraph.addEdge("C", "A");

        // Applica il metodo windowing
        customGraphStreamOp.windowing(testGraph, System.currentTimeMillis());

        // Verifica la consistenza e che il ciclo sia gestito correttamente
        Graph<String, DefaultEdge> resultGraph = customGraphStreamOp.get().get().getGraph();
        assertTrue(resultGraph.containsEdge("A", "B"));
        assertTrue(resultGraph.containsEdge("B", "C"));
        assertTrue(resultGraph.containsEdge("C", "A"));
    }

    @ParameterizedTest
    @ArgumentsSource(GraphAlgorithmProvider.class)
    public void testDisconnectedGraph(GraphProcessingAlgorithm<String> algorithm) {
        setUp(algorithm);

        // Crea un grafo disconnesso di test
        Graph<String, DefaultEdge> testGraph = new SimpleGraph<>(DefaultEdge.class);
        testGraph.addVertex("A");
        testGraph.addVertex("B");
        testGraph.addVertex("C");
        testGraph.addVertex("D");
        testGraph.addEdge("A", "B");

        // Applica il metodo windowing
        customGraphStreamOp.windowing(testGraph, System.currentTimeMillis());

        // Verifica che la parte disconnessa venga gestita correttamente
        Graph<String, DefaultEdge> resultGraph = customGraphStreamOp.get().get().getGraph();
        assertTrue(resultGraph.containsVertex("C"));
        assertTrue(resultGraph.containsVertex("D"));
        assertTrue(resultGraph.containsEdge("A", "B"));
        assertFalse(resultGraph.containsEdge("C", "D")); // Nessun edge tra C e D
    }

    public static class GraphAlgorithmProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of(new BetweennessCentralityAlgorithm<>(0.5)),
                    Arguments.of(new GraphDiameterAlgorithm<>(1.0)),
                    Arguments.of(new ShortestPathAlgorithm<>("A", "C", 1.5))
            );
        }
    }

    // Implementazione di TimeImpl e ReportImpl per i test
    static class TimeImpl implements Time {
        private long appTime;

        @Override
        public long getScope() {
            return 0;
        }

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

        @Override
        public ET getEvaluationTimeInstants() {
            return null;
        }

        @Override
        public void addEvaluationTimeInstants(TimeInstant i) {

        }

        @Override
        public TimeInstant getEvaluationTime() {
            return null;
        }

        @Override
        public boolean hasEvaluationInstant() {
            return false;
        }
    }

    static class ReportImpl implements Report {
        @Override
        public boolean report(Window window, Content content, long t_e, long sysTime) {
            return true;
        }

        @Override
        public void add(ReportingStrategy r) {

        }

        @Override
        public ReportingStrategy[] strategies() {
            return new ReportingStrategy[0];
        }
    }

    // Implementazione di una ContentFactory per il test
    static class ContentGeneralGraphFactoryImpl implements ContentFactory<Graph<String, DefaultEdge>, Graph<String, DefaultEdge>, IterableGraph<String>> {
        @Override
        public Content<Graph<String, DefaultEdge>, Graph<String, DefaultEdge>, IterableGraph<String>> createEmpty() {
            return new ContentGeneralGraph<>(new TimeImpl());
        }

        @Override
        public ContentGeneralGraph<String> create() {
            return new ContentGeneralGraph<>(new TimeImpl());
        }
    }
}