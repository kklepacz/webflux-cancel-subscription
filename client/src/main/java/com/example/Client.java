package com.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;

import java.util.Objects;

@SpringBootApplication
public class Client {

    private static final Logger log = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) {
        SpringApplication.run(Client.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            Disposable disposable = WebClient.create("http://localhost:8080")
                    .get()
                    .uri("/objects")
                    .retrieve()
                    .bodyToFlux(MyObject.class)
                    .subscribe(n -> log.info("Next: {}", n.getId()),
                            e -> log.error("Error: {}", e),
                            () -> log.info("Completed"));
            //I want to gracefully cancel subscription when I'm not interested in it anymore (for sake of this example - after 10 seconds)
            Thread.sleep(10000);
            disposable.dispose();
        };
    }
}

class MyObject {
    private final String id;

    @JsonCreator
    MyObject(@JsonProperty("id") String id) {
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