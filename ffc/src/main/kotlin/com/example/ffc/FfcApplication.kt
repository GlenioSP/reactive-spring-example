package com.example.ffc

import org.reactivestreams.Publisher
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.cloud.gateway.route.builder.filters
import org.springframework.cloud.gateway.route.builder.routes
import org.springframework.context.support.beans
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono

@SpringBootApplication
class FfcApplication

fun main(args: Array<String>) {

    SpringApplicationBuilder()
            .sources(FfcApplication::class.java)
            .initializers(beans {
                bean {
                    WebClient.builder()
                            .baseUrl("http://localhost:8080/movies")
                            .build()
                }
                bean {
                    router {
                        val client = ref<WebClient>()
                        GET("/titles") {
                            val names: Publisher<String> =
                                    client
                                    .get()
                                    .retrieve().bodyToFlux<Movie>()
                                    .map { it.title }
                            ServerResponse.ok()
                                    .body(names)
                        }
                    }
                }
                bean {
                    MapReactiveUserDetailsService(
                        User.withDefaultPasswordEncoder()
                            .username("user")
                            .password("password")
                            .roles("USER")
                            .build())
                }
                bean {
                    ref<ServerHttpSecurity>()
                        .httpBasic().and()
                        .authorizeExchange()
                            .pathMatchers("/rl").authenticated()
                            .anyExchange().permitAll()
                        .and()
                        .build()
                }
                bean {
                    val builder = ref<RouteLocatorBuilder>()
                    builder.routes {
                        route {
                            // call to localhost:8080/proxy
                            path("/proxy")
                            uri("http://localhost:8080/movies")
                            // in case of using Eureka, one could call something like this...
                            // uri("lb://movie-service/movies")
                        }
                        route {
                            val redisRl = ref<RequestRateLimiterGatewayFilterFactory>()
                                    .apply(
                                        ref<RequestRateLimiterGatewayFilterFactory.Config>()
                                            .setKeyResolver { exchange ->  Mono.empty() }
                                            .setRateLimiter(RedisRateLimiter(5, 10))
                                    )
                            path("/rl")
                            filters {
                                filter(redisRl)
                            }
                            uri("http://localhost:8080/movies")
                        }
                    }
                }
            })
            .run(*args)
}

class Movie(val id: String? = null, val title: String? = null)
