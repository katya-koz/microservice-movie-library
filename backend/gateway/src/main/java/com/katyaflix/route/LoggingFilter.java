package com.katyaflix.gateway.route;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;


@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() { return -1; }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        System.out.println("Incoming request: " + exchange.getRequest().getURI());
        return chain.filter(exchange);
    }
}