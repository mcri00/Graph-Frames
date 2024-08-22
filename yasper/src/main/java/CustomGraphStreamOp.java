import graph.jena.sds.TimeVaryingObject;
import org.apache.commons.rdf.api.IRI;
import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.streamreasoning.rsp4j.api.enums.ReportGrain;
import org.streamreasoning.rsp4j.api.enums.Tick;
import org.streamreasoning.rsp4j.api.exceptions.OutOfOrderElementException;
import org.streamreasoning.rsp4j.api.operators.s2r.execution.assigner.StreamToRelationOperator;
import org.streamreasoning.rsp4j.api.operators.s2r.execution.instance.Window;
import org.streamreasoning.rsp4j.api.operators.s2r.execution.instance.WindowImpl;
import org.streamreasoning.rsp4j.api.sds.timevarying.TimeVarying;
import org.streamreasoning.rsp4j.api.secret.content.Content;
import org.streamreasoning.rsp4j.api.secret.content.ContentFactory;
import org.streamreasoning.rsp4j.api.secret.report.Report;
import org.streamreasoning.rsp4j.api.secret.tick.Ticker;
import org.streamreasoning.rsp4j.api.secret.tick.secret.TickerFactory;
import org.streamreasoning.rsp4j.api.secret.time.Time;
import org.streamreasoning.rsp4j.api.stream.data.DataStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class CustomGraphStreamOp<T> implements StreamToRelationOperator<Graph<T, DefaultEdge>, Graph<T, DefaultEdge>, IterableGraph<T>> {

    private static final Logger log = Logger.getLogger(CustomGraphStreamOp.class);
    private final long windowSize;
    private final GraphProcessingAlgorithm<T> algorithm;
    private final IRI iri;
    private final Time time;
    private final Tick tick;
    private final Ticker ticker;
    private final Report report;
    private final ReportGrain grain;
    private final ContentFactory cf;
    private Graph<T, DefaultEdge> graph;
    private List<TimestampedElement<T>> buffer;
    private long currentTime;

    public CustomGraphStreamOp(IRI iri, long windowSize, GraphProcessingAlgorithm<T> algorithm, Time instance, Tick tick, Report report, ReportGrain grain, ContentFactory<Graph<T, DefaultEdge>, Graph<T, DefaultEdge>, IterableGraph<T>> cf) {
        this.iri = iri;
        this.time = instance;
        this.tick = tick;
        this.ticker = TickerFactory.tick(tick, this);
        this.report = report;
        this.grain = grain;
        this.cf = cf;

        this.windowSize = windowSize;
        this.algorithm = algorithm;
        this.graph = new SimpleGraph<>(DefaultEdge.class);
        this.buffer = new ArrayList<>();
        this.currentTime = 0;
    }

    @Override
    public Report report() {
        return null;
    }

    @Override
    public Tick tick() {
        return null;
    }

    @Override
    public Time time() {
        return time;
    }

    @Override
    public ReportGrain grain() {
        return null;
    }

    @Override
    public Content<Graph<T, DefaultEdge>, Graph<T, DefaultEdge>, IterableGraph<T>> content(long t_e) {
        Graph<T, DefaultEdge> combinedGraph = new SimpleGraph<>(DefaultEdge.class);
        buffer.stream().filter(te -> te.timestamp >= t_e - windowSize).forEach(te -> {
            combinedGraph.addVertex(te.element);
            // Assuming edges are stored elsewhere, add edge logic here
        });
        ContentGeneralGraph<T> res = new ContentGeneralGraph<>(time);
        res.add(combinedGraph);
        return res;
    }

    @Override
    public List<Content<Graph<T, DefaultEdge>, Graph<T, DefaultEdge>, IterableGraph<T>>> getContents(long t_e) {
        Graph<T, DefaultEdge> windowGraph = new SimpleGraph<>(DefaultEdge.class);
        buffer.stream().filter(te -> te.timestamp >= t_e - windowSize).forEach(te -> {
            windowGraph.addVertex(te.element);
            // Assuming edges are stored elsewhere, add edge logic here
        });
        ContentGeneralGraph<T> res = new ContentGeneralGraph<>(time);
        res.add(windowGraph);
        return Collections.singletonList(res);
    }

    public void windowing(Graph<T, DefaultEdge> e, long ts) {
        log.debug("Received element (" + e + "," + ts + ")");
        long t_e = ts;

        if (time.getAppTime() > t_e) {
            log.error("OUT OF ORDER NOT HANDLED");
            throw new OutOfOrderElementException("(" + e + "," + ts + ")");
        }

        buffer.add(new TimestampedElement<>(e, ts));
        graph.addVertex(e);
        // Add edges if available

        // Process the graph with the provided algorithm
        algorithm.process(graph);

        // Check if the window should be closed based on the algorithm
        if (algorithm.shouldCloseWindow(graph)) {
            // Create a new window
            Window active = scope(t_e);
            if (report.report(active, new ContentGeneralGraph<>(time).add(graph), t_e, System.currentTimeMillis())) {
                ticker.tick(t_e, active);
            }

            // Clear the buffer and the graph for the next window
            buffer.clear();
            graph = new SimpleGraph<>(DefaultEdge.class);
        }

        cleanUpWindows(t_e);
    }

    @Override
    public void evict() {

    }

    @Override
    public void evict(long ts) {

    }

    private Window scope(long t_e) {
        long o_i = t_e - windowSize;
        log.debug("Calculating the Windows to Open. First one opens at [" + o_i + "] and closes at [" + t_e + "]");
        log.debug("Computing Window [" + o_i + "," + (o_i + windowSize) + ") if absent");

        return new WindowImpl(o_i, t_e);
    }

    private void cleanUpWindows(long t_e) {
        buffer.removeIf(te -> te.timestamp < t_e - windowSize);
    }



    @Override
    public TimeVarying<IterableGraph<T>> get() {
        return new TimeVaryingObject<>(this, iri);
    }

    @Override
    public String getName() {
        return null;
    }




    private static class TimestampedElement<T> {
        Graph<T, DefaultEdge> element;
        long timestamp;

        public TimestampedElement(Graph<T, DefaultEdge> element, long timestamp) {
            this.element = element;
            this.timestamp = timestamp;
        }
    }
}
