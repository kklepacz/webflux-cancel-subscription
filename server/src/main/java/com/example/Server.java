package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.UnicastProcessor;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
public class Server {

    private static final Logger log = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) {
        SpringApplication.run(Server.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(SampleEmitter sampleEmitter) {
        return args -> sampleEmitter.run();
    }

    @Bean
    public ReactiveRepo reactiveRepo() {
        return new ReactiveRepoImpl();
    }

    @Bean
    public SampleEmitter sampleEmitter(ReactiveRepo reactiveRepo) {
        return new SampleEmitter(reactiveRepo);
    }

    @Bean
    public RouterFunction routerFunction(ReactiveRepo repo) {
        return RouterFunctions.route(GET("/objects"), serverRequest -> {
            log.info("Subscribing for GET /objects");
            return ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(repo.findAll(), MyObject.class);
        });
    }
}

class MyObject {
    private final String id;

    MyObject(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MyObject myObject = (MyObject) o;
        return Objects.equals(id, myObject.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

//Emitter - just example. The point is that he is emitting a new values and I save them to the repo to translate it to flux.
class SampleEmitter implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(SampleEmitter.class);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ReactiveRepo repo;

    SampleEmitter(ReactiveRepo repo) {
        this.repo = repo;
    }

    @Override
    public void run() {
        executorService.execute(() -> {
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(1000);
                    String id = UUID.randomUUID().toString();
                    log.info("Added {}", id);
                    repo.save(new MyObject(id));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }
}

//Translation between blocking code and my reactive web stack
interface ReactiveRepo {
    void save(MyObject elem);

    Flux<MyObject> findAll();
}

class ReactiveRepoImpl implements ReactiveRepo {
    private final UnicastProcessor<MyObject> hotProcessor = UnicastProcessor.create();
    private final FluxSink<MyObject> fluxSink = hotProcessor.sink(FluxSink.OverflowStrategy.LATEST);
    private final Flux<MyObject> hotFlux = hotProcessor.publish().autoConnect();

    @Override
    public void save(MyObject elem) {
        fluxSink.next(elem);
    }

    @Override
    public Flux<MyObject> findAll() {
        return hotFlux;
    }
}


