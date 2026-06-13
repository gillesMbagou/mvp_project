package be.caresync.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableCaching
@EnableJpaRepositories(basePackages = "be.caresync.demo.repository.jpa")
public class GlucometerDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(GlucometerDemoApplication.class, args);
    }
}
