package education.creatorindex;

import education.creatorindex.config.InfraiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(InfraiProperties.class)
public class CreatorIndexApplication {
    public static void main(String[] args) {
        SpringApplication.run(CreatorIndexApplication.class, args);
    }
}
