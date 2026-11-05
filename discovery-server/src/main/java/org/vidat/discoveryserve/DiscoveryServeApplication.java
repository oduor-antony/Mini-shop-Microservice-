package org.vidat.discoveryserve;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServeApplication.class, args);
    }

}
