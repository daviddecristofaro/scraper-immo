package it.ibee.scraperimmo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ScraperImmoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScraperImmoApplication.class, args);
    }

}
