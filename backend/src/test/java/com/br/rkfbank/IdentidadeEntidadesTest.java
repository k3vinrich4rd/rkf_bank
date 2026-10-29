package com.br.rkfbank;

import static org.assertj.core.api.Assertions.assertThat;

import com.br.rkfbank.autenticacao.RefreshToken;
import com.br.rkfbank.autenticacao.Usuario;
import com.br.rkfbank.clientes.Cliente;
import com.br.rkfbank.clientes.Endereco;
import com.br.rkfbank.contas.Conta;
import com.br.rkfbank.contas.Lancamento;
import com.br.rkfbank.contas.RegistroIdempotencia;
import com.br.rkfbank.contas.comprovante.EventoOutbox;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

// equals/hashCode das entidades: identidade pelo id do banco e hash estavel antes e depois do persist.
class IdentidadeEntidadesTest {

    static Stream<Arguments> entidades() {
        return Stream.of(
                Arguments.of("Conta", (Supplier<Object>) Conta::new),
                Arguments.of("Cliente", (Supplier<Object>) Cliente::new),
                Arguments.of("Endereco", (Supplier<Object>) Endereco::new),
                Arguments.of("Lancamento", (Supplier<Object>) Lancamento::new),
                Arguments.of("RefreshToken", (Supplier<Object>) () -> novaPorReflexao(RefreshToken.class)),
                Arguments.of("RegistroIdempotencia", (Supplier<Object>) RegistroIdempotencia::new),
                Arguments.of("Usuario", (Supplier<Object>) Usuario::new),
                Arguments.of("EventoOutbox", (Supplier<Object>) () -> novaPorReflexao(EventoOutbox.class)));
    }

    @ParameterizedTest(name = "{0}: duas instancias novas (sem id) sao diferentes")
    @MethodSource("entidades")
    void deveDiferenciarInstanciasSemId(String nome, Supplier<Object> nova) {
        Object a = nova.get();
        Object b = nova.get();

        assertThat(a).isEqualTo(a).isNotEqualTo(b).isNotEqualTo(null).isNotEqualTo("outro tipo");
    }

    @ParameterizedTest(name = "{0}: mesmo id = mesma entidade")
    @MethodSource("entidades")
    void deveSerIgualQuandoTemOMesmoId(String nome, Supplier<Object> nova) {
        UUID id = UUID.randomUUID();
        Object a = comId(nova.get(), id);
        Object b = comId(nova.get(), id);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(comId(nova.get(), UUID.randomUUID()));
    }

    @ParameterizedTest(name = "{0}: continua no HashSet depois de ganhar id no persist")
    @MethodSource("entidades")
    void deveManterOHashQuandoOIdEGerado(String nome, Supplier<Object> nova) {
        Object entidade = nova.get();
        Set<Object> conjunto = new HashSet<>();
        conjunto.add(entidade);

        comId(entidade, UUID.randomUUID()); // o que o Hibernate faz no persist

        assertThat(conjunto).contains(entidade);
    }

    private static Object comId(Object entidade, UUID id) {
        ReflectionTestUtils.setField(entidade, "id", id);
        return entidade;
    }

    // O construtor vazio do RefreshToken e do EventoOutbox e protected (so a JPA usa), e o teste nao esta no pacote deles.
    private static Object novaPorReflexao(Class<?> tipo) {
        try {
            var construtor = tipo.getDeclaredConstructor();
            construtor.setAccessible(true);
            return construtor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
