package conexao_api_teste_01.cadastra_api.UsuarioController.UsuarioService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import conexao_api_teste_01.cadastra_api.UsuarioController.DTO.UsuarioDTO;

import conexao_api_teste_01.cadastra_api.UsuarioController.UsuarioEntity.UsuarioEntity;
import conexao_api_teste_01.cadastra_api.UsuarioController.UsuarioEntity.UsuarioVerificadorEntity;
import conexao_api_teste_01.cadastra_api.UsuarioController.UsuarioEntity.TipoSituacaoUsuario.TipoSituacaoUsuario;
import conexao_api_teste_01.cadastra_api.UsuarioController.UsuarioRepository.UsuarioRepository;
import conexao_api_teste_01.cadastra_api.UsuarioController.UsuarioRepository.UsuarioVerificadorRepository;


@Service
@Slf4j
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UsuarioVerificadorRepository usuarioVerificadorRepository;

    public List<UsuarioEntity> listaTodos() {
        return (List<UsuarioEntity>) usuarioRepository.findAll();
    }

    public void inserirNovoUsuario(UsuarioDTO usuario) {
        UsuarioEntity usuarioEntity = new UsuarioEntity(usuario);
        usuarioEntity.setSenha(passwordEncoder.encode(usuario.getSenha()));
        usuarioEntity.setSituacao(TipoSituacaoUsuario.PENDENTE);
        usuarioEntity.setId(null);
        usuarioEntity = usuarioRepository.save(usuarioEntity); // 🔹 Salvar usuário antes


UsuarioVerificadorEntity verificador = new UsuarioVerificadorEntity();
verificador.setUsuario(usuarioEntity);
verificador.setUuid(UUID.randomUUID().toString());  // 🔹 Converte para String
verificador.setDataExpiracao(Instant.now().plusMillis(900000));

usuarioVerificadorRepository.save(verificador);  // 🔹 Agora está correto!
    

//---------------------------------------------------------------------------------------
        // Gerar código de validação de 6 dígitos
        String codigoValidacao = String.format("%06d", new Random().nextInt(1000000));

        // Verificar se o código de validação pode ser armazenado
        if (usuarioEntity instanceof UsuarioEntity) {
            try {
                usuarioEntity.setCodigoValidacao(codigoValidacao);
            } catch (Exception e) {
                log.warn("Método setCodigoValidacao não encontrado em UsuarioEntity. O código não será armazenado.");
            }
        }

        usuarioRepository.save(usuarioEntity);

        // Enviar email
        enviarEmailDeConfirmacao(usuario.getEmail(), codigoValidacao);
    }

    private void enviarEmailDeConfirmacao(String email, String codigoValidacao) {
        try {
            emailService.enviarEmailTexto(
                email,
                "Novo usuário cadastrado",
                "Você está recebendo um email de cadastramento. Seu código de validação é: " + codigoValidacao
            );
            log.info("E-mail de confirmação enviado para: {}", email);
        } catch (Exception e) {
            log.error("Erro ao enviar email para: {}", email, e);
            throw new RuntimeException("Erro ao enviar email de confirmação.");
        }
    }

    public Optional<UsuarioEntity> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    public UsuarioEntity inserir(UsuarioEntity usuario) {
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        return usuarioRepository.save(usuario);
    }

    public UsuarioEntity alterar(Long id, UsuarioEntity usuarioAtualizado) {
        return usuarioRepository.findById(id)
            .map(usuario -> {
                usuario.setNome(usuarioAtualizado.getNome());
                usuario.setLogin(usuarioAtualizado.getLogin());
                usuario.setEmail(usuarioAtualizado.getEmail());
                usuario.setSenha(passwordEncoder.encode(usuarioAtualizado.getSenha()));
                return usuarioRepository.save(usuario);
            })
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado com o ID: " + id));
    }

    public void excluir(Long id) {
        usuarioRepository.deleteById(id);
    }
}