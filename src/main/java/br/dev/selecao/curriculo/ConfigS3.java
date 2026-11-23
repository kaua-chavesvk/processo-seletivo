package br.dev.selecao.curriculo;

import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@ConditionalOnProperty(name = "app.curriculos.modo", havingValue = "s3")
class ConfigS3 {
  @Bean(destroyMethod = "close")
  S3Client clienteS3(
      @Value("${app.curriculos.s3.regiao}") String regiao,
      @Value("${app.curriculos.s3.endpoint:}") String endpoint,
      @Value("${app.curriculos.s3.path-style:false}") boolean pathStyle) {
    var builder =
        S3Client.builder()
            .region(Region.of(regiao))
            .forcePathStyle(pathStyle)
            .httpClientBuilder(
                UrlConnectionHttpClient.builder()
                    .connectionTimeout(Duration.ofSeconds(5))
                    .socketTimeout(Duration.ofSeconds(30)))
            .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofSeconds(60)));
    if (!endpoint.isBlank()) {
      URI destino = URI.create(endpoint);
      if (!"https".equalsIgnoreCase(destino.getScheme())
          || destino.getHost() == null
          || destino.getUserInfo() != null)
        throw new IllegalArgumentException("O endpoint do bucket exige HTTPS e hostname");
      builder.endpointOverride(destino);
    }
    return builder.build();
  }
}
