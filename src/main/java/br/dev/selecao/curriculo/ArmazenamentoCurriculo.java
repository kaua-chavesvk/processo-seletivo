package br.dev.selecao.curriculo;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
public interface ArmazenamentoCurriculo {
    String salvar(MultipartFile arquivo);
    Resource carregar(String chave);
    void remover(String chave);
}
