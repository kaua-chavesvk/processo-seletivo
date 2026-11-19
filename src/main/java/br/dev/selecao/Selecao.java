package br.dev.selecao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.scheduling.annotation.EnableScheduling
@org.springframework.boot.context.properties.ConfigurationPropertiesScan
@SpringBootApplication
public class Selecao {
  public static void main(String[] args) {
    SpringApplication.run(Selecao.class, args);
  }
}
