import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.streamreasoning.rsp4j.api.secret.content.Content;
import org.streamreasoning.rsp4j.api.secret.content.ContentFactory;
import org.streamreasoning.rsp4j.api.secret.time.Time;
import org.streamreasoning.rsp4j.api.secret.time.TimeImpl;

public class ContentFactoryImpl<T> implements ContentFactory<Graph<T, DefaultEdge>, Graph<T, DefaultEdge>, IterableGraph<T>> {

    @Override
    public Content<Graph<T, DefaultEdge>, Graph<T, DefaultEdge>, IterableGraph<T>> createEmpty() {
        return null;
    }

    @Override
    public Content<Graph<T, DefaultEdge>, Graph<T, DefaultEdge>, IterableGraph<T>> create() {
        // Crea una nuova istanza di Content per gestire il contenuto dei grafi
        return new ContentGeneralGraph<>(new TimeImpl(System.currentTimeMillis()));
    }
}
