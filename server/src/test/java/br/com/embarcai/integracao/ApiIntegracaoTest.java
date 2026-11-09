package br.com.embarcai.integracao;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegracaoTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void saudeConsultaOBancoERespondeOk() throws Exception {
        mvc.perform(get("/api/saude"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    void listaALinha020SemeadaEOrdenaPelaDistancia() throws Exception {
        mvc.perform(get("/api/linhas").param("lat", "-8.2275").param("lng", "-34.9378"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].codigo").value("020"))
            .andExpect(jsonPath("$[0].distancia_km").value(0.0))
            .andExpect(jsonPath("$.length()").value(greaterThan(5)));
    }

    @Test
    void detalheDaLinhaTrazOAlertaSemeado() throws Exception {
        mvc.perform(get("/api/linhas/020"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.codigo").value("020"))
            .andExpect(jsonPath("$.status.tipo").value("atencao"))
            .andExpect(jsonPath("$.relatos[0].tipo").exists());
    }

    @Test
    void linhaInexistenteRetorna404() throws Exception {
        mvc.perform(get("/api/linhas/9999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.erro").value("Linha não encontrada."));
    }

    @Test
    void qrCodeDaLinhaEUmaImagemPng() throws Exception {
        mvc.perform(get("/api/linhas/020/qrcode"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andExpect(header().string("Content-Type", startsWith("image/png")));
    }

    @Test
    void cadastroLoginEConsultaDoPerfil() throws Exception {
        String email = "passageiro" + System.nanoTime() + "@embarcai.test";
        MvcResult criado = mvc.perform(post("/api/cadastrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Passageiro Teste","email":"%s","senha":"123456"}
                    """.formatted(email)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.usuario.role").value("PASSAGEIRO"))
            .andReturn();

        String token = com.jayway.jsonpath.JsonPath.read(criado.getResponse().getContentAsString(), "$.token");

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email));

        mvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","senha":"errada"}
                    """.formatted(email)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void relatoExigeLoginEBloqueiaRepeticaoEmTrintaMinutos() throws Exception {
        mvc.perform(post("/api/linhas/020/relatos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"bloqueio\",\"mensagem\":\"obra na pista\"}"))
            .andExpect(status().isUnauthorized());

        String token = tokenDeNovoUsuario();
        String corpo = "{\"tipo\":\"bloqueio\",\"mensagem\":\"obra na pista\",\"local\":\"Caxangá\"}";

        mvc.perform(post("/api/linhas/020/relatos")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipo").value("bloqueio"))
            .andExpect(jsonPath("$.confirmacoes").value(1))
            .andExpect(jsonPath("$.confiabilidade").value("nao_confirmado"));

        mvc.perform(post("/api/linhas/020/relatos")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo))
            .andExpect(status().isTooManyRequests());
    }

    @Test
    void segundaPessoaConfirmaOAlerta() throws Exception {
        String autor = tokenDeNovoUsuario();
        MvcResult criado = mvc.perform(post("/api/linhas/101/relatos")
                .header("Authorization", "Bearer " + autor)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"acidente\",\"mensagem\":\"batida no cruzamento\"}"))
            .andExpect(status().isCreated())
            .andReturn();
        Number id = com.jayway.jsonpath.JsonPath.read(criado.getResponse().getContentAsString(), "$.id");

        mvc.perform(post("/api/relatos/" + id + "/confirmar")
                .header("Authorization", "Bearer " + autor))
            .andExpect(status().isConflict());

        mvc.perform(post("/api/relatos/" + id + "/confirmar")
                .header("Authorization", "Bearer " + tokenDeNovoUsuario()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.confirmacoes").value(2));
    }

    @Test
    void adminCadastraLinhaSomenteComASenha() throws Exception {
        mvc.perform(post("/api/linhas")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-admin-senha", "errada")
                .content("""
                    {"codigo":"777","nome":"Teste","origem":"A","destino":"B"}
                    """))
            .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/linhas")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-admin-senha", "admin123")
                .content("""
                    {"codigo":"777","nome":"Linha de teste","origem":"Derby","destino":"Boa Viagem"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.codigo").value("777"));
    }

    @Test
    void classificadorDaApiReconheceAlagamento() throws Exception {
        mvc.perform(post("/api/ia/classificar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"texto\":\"A rua tá toda alagada depois da chuva\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tipo").value("alagamento"))
            .andExpect(jsonPath("$.confiavel").value(true));
    }

    private String tokenDeNovoUsuario() throws Exception {
        String email = "u" + System.nanoTime() + "@embarcai.test";
        MvcResult criado = mvc.perform(post("/api/cadastrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Usuário Teste","email":"%s","senha":"123456"}
                    """.formatted(email)))
            .andExpect(status().isCreated())
            .andReturn();
        return com.jayway.jsonpath.JsonPath.read(criado.getResponse().getContentAsString(), "$.token");
    }
}
