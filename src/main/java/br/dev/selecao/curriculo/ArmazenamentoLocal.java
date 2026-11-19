package br.dev.selecao.curriculo;

import br.dev.selecao.comum.Falha;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.core.io.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.util.*;
@Component
public class ArmazenamentoLocal implements ArmazenamentoCurriculo {
    private final Path raiz;
    public ArmazenamentoLocal(@Value("${app.uploads}") String diretorio) throws IOException {
        raiz=Files.createDirectories(Path.of(diretorio).toAbsolutePath().normalize());
    }
    public String salvar(MultipartFile arquivo) {
        if (!"application/pdf".equalsIgnoreCase(arquivo.getContentType()) || arquivo.getSize()>5L*1024*1024)
            throw Falha.invalida("Envie um PDF de até 5 MB");
        String chave=UUID.randomUUID()+".pdf";
        try (var entrada=arquivo.getInputStream()) {
            byte[] bytes=entrada.readNBytes(5*1024*1024+1);
            if (bytes.length>5*1024*1024 || bytes.length<4 || bytes[0]!='%' || bytes[1]!='P' || bytes[2]!='D' || bytes[3]!='F')
                throw Falha.invalida("Conteúdo PDF inválido");
            try (var saida=Files.newOutputStream(caminho(chave),StandardOpenOption.CREATE_NEW)) {
                try { saida.write(bytes); } catch (IOException e) { remover(chave); throw e; }
            }
            return chave;
        } catch(IOException e) { throw new UncheckedIOException(e); }
    }
    public Resource carregar(String chave) {
        var recurso=new FileSystemResource(caminho(chave)); if (!recurso.exists()) throw Falha.ausente(); return recurso;
    }
    public void remover(String chave) {
        try { Files.deleteIfExists(caminho(chave)); } catch(IOException e) { throw new UncheckedIOException(e); }
    }
    private Path caminho(String chave) {
        if (!chave.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.pdf")) throw Falha.ausente();
        return raiz.resolve(chave);
    }
}
