package io.synkra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

/** {@code verificador} es una biblioteca de otro proyecto Gradle: módulo compartido (C-36). */
@Modulithic(sharedModules = "verificador")
@SpringBootApplication
public class SynkraApp {

    public static void main(String[] args) {
        SpringApplication.run(SynkraApp.class, args);
    }
}
