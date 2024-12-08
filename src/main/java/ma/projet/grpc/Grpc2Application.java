package ma.projet.grpc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "ma.projet.entities")
@EnableJpaRepositories(basePackages = "ma.projet.repositories")
public class Grpc2Application {

    public static void main(String[] args) {
        SpringApplication.run(Grpc2Application.class, args);
    }

}
