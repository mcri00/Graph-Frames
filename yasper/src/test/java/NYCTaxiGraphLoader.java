import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.streamreasoning.rsp4j.api.enums.ReportGrain;
import org.streamreasoning.rsp4j.api.enums.Tick;
import org.streamreasoning.rsp4j.api.secret.report.ReportImpl;
import org.streamreasoning.rsp4j.api.secret.time.TimeImpl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class NYCTaxiGraphLoader {

    public static Graph<String, DefaultEdge> loadGraph(String filePath) {
        Graph<String, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            // Salta l'intestazione
            br.readLine();

            while ((line = br.readLine()) != null) {
                String[] fields = line.split(",");

                // Colonne relative alle coordinate di pickup e dropoff
                String pickupLocation = fields[5] + "," + fields[6];  // Pickup_longitude, Pickup_latitude
                String dropoffLocation = fields[9] + "," + fields[10]; // Dropoff_longitude, Dropoff_latitude

                // Aggiungi nodi e archi
                graph.addVertex(pickupLocation);
                graph.addVertex(dropoffLocation);
                graph.addEdge(pickupLocation, dropoffLocation);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return graph;
    }

    public static void main(String[] args) {
        String filePath = "path/to/your/nyc_taxi_data.csv"; // Assicurati di specificare il percorso corretto
        Graph<String, DefaultEdge> taxiGraph = loadGraph(filePath);

        // Passa il grafo al tuo CustomGraphStreamOp per il processamento
        GraphProcessingAlgorithm<String> algorithm = new BetweennessCentralityAlgorithm<>(0.5);
        CustomGraphStreamOp<String> customGraphStreamOp = new CustomGraphStreamOp<>(
                IRI.create("http://example.org/customOp"),
                60000, // Window size in milliseconds
                algorithm,
                new TimeImpl(0L),
                Tick.TIME_DRIVEN,
                new ReportImpl(),
                ReportGrain.SINGLE,
                new CustomGraphStreamOpTest.ContentGeneralGraphFactoryImpl()
        );

        long currentTime = System.currentTimeMillis();
        customGraphStreamOp.windowing(taxiGraph, currentTime);

        // A questo punto, puoi eseguire i tuoi test e verificare i risultati
    }
}
