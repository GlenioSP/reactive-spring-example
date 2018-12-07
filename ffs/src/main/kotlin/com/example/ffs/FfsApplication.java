package com.example.ffs;

import java.time.Duration;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class FfsApplication {

    @Bean
    ApplicationRunner demoData(MovieRepository movieRepository) {
        return args -> {
            movieRepository.deleteAll().thenMany(
            Flux.just("The Silence of the Lambdas", "Back to the Future",
                      "AEon Flux", "Meet the Fluxers", "The Fluxxinator",
                      "Flux Gordon", "Y Tu Mono Tambien")
                .map(Movie::new)
                .flatMap(movieRepository::save))
            .thenMany(movieRepository.findAll())
            .subscribe(System.out::println);
        };
    }

//    @Bean
//    RouterFunction<?> routerFunction(MovieService ms) {
//        return RouterFunctions.route(RequestPredicates.GET("/movies"),
//                       req -> ServerResponse.ok().body(ms.getAllMovies(), Movie.class))
//             .andRoute(RequestPredicates.GET("/movies/{id}"),
//                       req -> ServerResponse.ok().body(ms.getMovieById(req.pathVariable("id")), Movie.class))
//             .andRoute(RequestPredicates.GET("/movies/{id}/events"),
//                       req -> ServerResponse.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(ms.getEvents(req.pathVariable("id")), MovieEvent.class));
//    }

    public static void main(String[] args) {
        SpringApplication.run(FfsApplication.class, args);
    }
}

// this rest controller functionality is equivalent to the routerFunction bean inside FfsApplication. Therefore, this bean
// is commented out as an alternative.
@RestController
class MovieRestController {

    private final MovieService movieService;

    MovieRestController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping("/movies")
    Flux<Movie> all() {
        return this.movieService.getAllMovies();
    }

    @GetMapping("/movies/{id}")
    Mono<Movie> byId(@PathVariable String id) {
        return this.movieService.getMovieById(id);
    }

    @GetMapping(value = "/movies/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<MovieEvent> events(@PathVariable String id) {
        return this.movieService.getEvents(id);
    }
}

@Service
class MovieService {
    private final MovieRepository movieRepository;

    MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public Flux<Movie> getAllMovies() {
        return this.movieRepository.findAll();
    }

    public Mono<Movie> getMovieById(String id) {
        return this.movieRepository.findById(id);
    }

    public Flux<MovieEvent> getEvents(String movieId) {
        return Flux.<MovieEvent>generate(sink -> sink.next(new MovieEvent(movieId, new Date())))
            .delayElements(Duration.ofSeconds(1));
    }
}

interface MovieRepository extends ReactiveCrudRepository<Movie, String> {

    Flux<Movie> findByTitle(String title);
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class MovieEvent {
    private String movieId;
    private Date dateViewed;
}

@Document
@Data
@NoArgsConstructor
@RequiredArgsConstructor
class Movie {

    @Id
    private String id;
    @NonNull
    private String title;
}
