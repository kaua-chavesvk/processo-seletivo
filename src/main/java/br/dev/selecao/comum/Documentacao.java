package br.dev.selecao.comum;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Documentacao {
    private static final String JWT = "accessToken";

    @Bean
    OpenAPI especificacao() {
        return new OpenAPI()
            .info(new Info().title("Plataforma de Processo Seletivo").version("1.0"))
            .components(new Components().addSecuritySchemes(JWT, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                .description("Cole o accessToken recebido em POST /auth/login.")));
    }

    @Bean
    OpenApiCustomizer acessoDasRotas() {
        return api -> api.getPaths().forEach((rota, item) ->
            item.readOperationsMap().forEach((metodo, operacao) -> {
                if (rota.startsWith("/auth/") || rota.equals("/interface/config")) return;
                boolean vagaPublica = metodo == PathItem.HttpMethod.GET
                    && (rota.equals("/vagas") || rota.equals("/vagas/{id}"));
                if (vagaPublica) operacao.addSecurityItem(new SecurityRequirement());
                operacao.addSecurityItem(new SecurityRequirement().addList(JWT));
            }));
    }
}
