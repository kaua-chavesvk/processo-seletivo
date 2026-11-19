package br.dev.selecao;

import br.dev.selecao.usuario.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.*;
import org.testcontainers.containers.PostgreSQLContainer;
import java.nio.file.Path;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc
class FluxoTest {
    @TempDir static Path uploads;
    static PostgreSQLContainer<?> pg;
    @DynamicPropertySource static void banco(DynamicPropertyRegistry r) {
        String externa=System.getenv("TEST_DATABASE_URL");
        if (externa==null) {
            pg=new PostgreSQLContainer<>("postgres:16"); pg.start();
            r.add("spring.datasource.url",pg::getJdbcUrl); r.add("spring.datasource.username",pg::getUsername);
            r.add("spring.datasource.password",pg::getPassword);
        } else r.add("spring.datasource.url",()->externa);
        r.add("app.uploads",()->uploads.toString());
    }
    @AfterAll static void parar() { if(pg!=null) pg.stop(); }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired Contas contas;
    @Test void protegeAdminOrigemESessao() throws Exception {
        var publico = mvc.perform(get("/vagas").secure(true)).andExpect(status().isOk()).andReturn().getResponse();
        assertThat(publico.getHeader("Content-Security-Policy")).contains("frame-ancestors 'none'");
        assertThat(publico.getHeader("Strict-Transport-Security")).isNotBlank();
        assertThat(publico.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(publico.getHeader("Set-Cookie")).isNull();
        chamar(get("/usuarios"),null,null,401);
        chamar(get("/usuarios"),conta(Papel.CANDIDATO,null),null,403);
        chamar(post("/auth/login").header("Origin","https://outro.example"),null,null,403);
        mvc.perform(get("/.env")).andExpect(status().isUnauthorized());
        mvc.perform(get("/.git/config")).andExpect(status().isUnauthorized());
    }
    @Test void parametrosInvalidosRetornamProblemaComCampo() throws Exception {
        for (String[] caso : List.of(new String[]{"/vagas/uuid-invalido","id"},
                new String[]{"/vagas?page=abc","page"}, new String[]{"/vagas?nivel=INVALIDO","nivel"})) {
            var resposta=mvc.perform(get(caso[0])).andExpect(status().isBadRequest()).andReturn().getResponse();
            var problema=json.readTree(resposta.getContentAsByteArray());
            assertThat(problema.at("/erros/0/campo").asText()).isEqualTo(caso[1]);
            assertThat(problema.path("correlationId").asText()).isEqualTo(resposta.getHeader("X-Correlation-ID"));
        }
    }

    @Test void swaggerDescreveAutenticacaoPorOperacao() throws Exception {
        var doc=chamar(get("/v3/api-docs"),null,null,200);
        assertThat(doc.at("/components/securitySchemes/accessToken/scheme").asText()).isEqualTo("bearer");
        assertThat(doc.at("/paths/~1empresas/post/security/0").has("accessToken")).isTrue();
        assertThat(doc.at("/paths/~1auth~1login/post").has("security")).isFalse();
        assertThat(doc.at("/paths/~1vagas/get/security/0").isEmpty()).isTrue();
        assertThat(doc.at("/paths/~1vagas/get/security/1").has("accessToken")).isTrue();
    }
    String senha="segredo-teste";
    JsonNode chamar(MockHttpServletRequestBuilder b,String token,Object corpo,int esperado) throws Exception {
        if(token!=null) b.header("Authorization","Bearer "+token);
        if(corpo!=null) b.contentType("application/json").content(json.writeValueAsBytes(corpo));
        byte[] resposta=mvc.perform(b).andExpect(status().is(esperado)).andReturn().getResponse().getContentAsByteArray();
        return resposta.length==0?json.nullNode():json.readTree(resposta);
    }
    JsonNode login(String email) throws Exception {
        return chamar(post("/auth/login"),null,Map.of("email",email,"senha",senha),200);
    }
    String conta(Papel papel,UUID empresa) throws Exception {
        String email=UUID.randomUUID()+"@example.org";
        contas.criar("Teste",email,senha,papel,empresa);
        return login(email).get("accessToken").asText();
    }
    MockMultipartHttpServletRequestBuilder upload(String vaga,String dados) {
        return multipart("/vagas/"+vaga+"/candidaturas").file(new MockMultipartFile("curriculo","../../cv.pdf","application/pdf",dados.getBytes()));
    }
    @Test void selecaoCompleta() throws Exception {
        String admin=conta(Papel.ADMIN,null);
        String emp=chamar(post("/empresas"),admin,Map.of("nome","Primeira","cnpj","12.ABC.345/01DE-35"),201).get("id").asText();
        String outra=chamar(post("/empresas"),admin,Map.of("nome","Outra","cnpj","04.252.011/0001-10"),201).get("id").asText();
        String recrutador=conta(Papel.RECRUTADOR,UUID.fromString(emp)), externo=conta(Papel.RECRUTADOR,UUID.fromString(outra));
        String candidato=conta(Papel.CANDIDATO,null), outro=conta(Papel.CANDIDATO,null);
        var dados=Map.of("titulo","Java","descricao","Backend","modalidade","REMOTO","nivel","JUNIOR","localizacao","Brasil","tecnologias",List.of("Java"));
        String vaga=chamar(post("/empresas/"+emp+"/vagas"),recrutador,dados,201).get("id").asText();
        String v="/vagas/"+vaga;
        chamar(get(v),null,null,404); chamar(get(v),externo,null,403);
        chamar(upload(vaga,"%PDF-1.7"),candidato,null,422);
        chamar(patch(v+"/status"),recrutador,Map.of("status","PUBLICADA"),200);
        assertThat(chamar(get("/vagas?tecnologia=JAVA&nivel=JUNIOR"),null,null,200).get("totalElements").asInt()).isEqualTo(1);
        chamar(get("/vagas?size=101"),null,null,400); chamar(get("/vagas?sort=empresaId"),null,null,400);
        chamar(upload(vaga,"falso"),candidato,null,422);
        String id=chamar(upload(vaga,"%PDF-1.7"),candidato,null,201).get("id").asText();
        chamar(upload(vaga,"%PDF-1.7"),candidato,null,409);
        String c="/candidaturas/"+id;
        chamar(get(c+"/historico"),outro,null,404); chamar(get(c+"/curriculo"),outro,null,404);
        chamar(get(c+"/historico"),externo,null,403);
        mvc.perform(get(c+"/curriculo").header("Authorization","Bearer "+recrutador)).andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().bytes("%PDF-1.7".getBytes()));
        chamar(patch(c+"/etapa"),recrutador,Map.of("novaEtapa","PROPOSTA"),422);
        chamar(patch(c+"/etapa"),recrutador,Map.of("novaEtapa","ENTREVISTA","observacao","Agendada"),200);
        chamar(patch(c+"/desistencia"),outro,null,404); chamar(patch(c+"/desistencia"),candidato,null,200);
        chamar(patch(c+"/etapa"),recrutador,Map.of("novaEtapa","REPROVADO"),422);
        assertThat(chamar(get(c+"/historico"),candidato,null,200)).hasSize(3);
        chamar(patch(v+"/status"),recrutador,Map.of("status","ENCERRADA"),200);
        chamar(upload(vaga,"%PDF-1.7"),outro,null,422);
    }
    @Test void cicloDeCredenciais() throws Exception {
        String email=UUID.randomUUID()+"@example.org";
        var d=Map.of("nome","Ana","email",email,"senha",senha);
        String id=chamar(post("/auth/cadastro"),null,d,201).get("id").asText();
        chamar(post("/auth/cadastro"),null,Map.of("nome","Ana","email",email.toUpperCase(Locale.ROOT),"senha",senha),409);
        var primeiro=login(email);
        var antigo=Map.of("refreshToken",primeiro.get("refreshToken").asText());
        var novo=chamar(post("/auth/refresh"),null,antigo,200);
        chamar(post("/auth/refresh"),null,antigo,401);
        var rotacionado=Map.of("refreshToken",novo.get("refreshToken").asText());
        chamar(post("/auth/logout"),null,rotacionado,204);
        chamar(post("/auth/refresh"),null,rotacionado,401);
        var sessao=login(email);
        chamar(patch("/usuarios/"+id+"/atividade"),conta(Papel.ADMIN,null),Map.of("ativo",false),204);
        chamar(get("/candidaturas/minhas"),sessao.get("accessToken").asText(),null,401);
        chamar(post("/auth/refresh"),null,Map.of("refreshToken",sessao.get("refreshToken").asText()),401);
    }
}
