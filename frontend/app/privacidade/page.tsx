import type { Metadata } from "next";
import Link from "next/link";
import { DocumentoLegal } from "@/components/legal/documento-legal";
import { ROUTES } from "@/lib/routes";

export const metadata: Metadata = {
  title: "Política de Privacidade",
  description: "Quais dados o RKF Bank guarda, para quê, com quem compartilha e por quanto tempo.",
};

export default function PrivacidadePage() {
  return (
    <DocumentoLegal
      titulo="Política de Privacidade"
      resumo="Quais dados o RKF Bank guarda, para quê, com quem compartilha e por quanto tempo (LGPD, Lei 13.709/2018)."
    >
      <section>
        <h2>1. Dados que guardamos</h2>
        <ul>
          <li>Cadastro: nome completo, CPF, e-mail, celular e endereço (o CEP, o número e o complemento você informa; rua, bairro, cidade e UF vêm do ViaCEP).</li>
          <li>Acesso: a sua senha, guardada só como hash BCrypt (nem o time consegue lê-la), e o perfil de acesso.</li>
          <li>Contas e movimentações: depósitos, saques, transferências, saldos e a descrição que você escreve em cada uma.</li>
          <li>O aceite destes documentos: a versão aceita e a data.</li>
          <li>Registros técnicos (logs) de cada requisição, sem CPF completo, senha ou token.</li>
        </ul>
      </section>
      <section>
        <h2>2. Para que usamos</h2>
        <p>
          Para abrir e manter a sua conta, fazer as operações que você pede, mostrar o extrato, proteger o acesso (login,
          limite de tentativas) e cumprir a lei. Base legal: execução do contrato (LGPD, art. 7º, V) e cumprimento de
          obrigação legal (art. 7º, II). Não usamos os seus dados para publicidade e não os vendemos.
        </p>
      </section>
      <section>
        <h2>3. Com quem compartilhamos</h2>
        <p>
          Só com o <strong>ViaCEP</strong>, serviço público de consulta de endereços: enviamos apenas o CEP digitado, para
          completar o endereço. Nenhum outro dado sai do RKF Bank.
        </p>
      </section>
      <section>
        <h2>4. Por quanto tempo guardamos</h2>
        <p>
          Enquanto a sua conta existir. Se você encerrar o cadastro, as contas ficam inativas e o acesso termina na hora;
          os dados ficam guardados por <strong>5 anos</strong>, como manda a lei de prevenção à lavagem de dinheiro (Lei
          9.613/1998, art. 10), e depois são anonimizados: nome, CPF, e-mail, celular, endereço e as descrições das
          movimentações deixam de identificar você.
        </p>
      </section>
      <section>
        <h2>5. Os seus direitos</h2>
        <p>
          Em <Link href={ROUTES.meusDados} className="underline underline-offset-4">Meus dados</Link> você vê o que
          guardamos, corrige nome, e-mail, celular e endereço e pede o encerramento do cadastro (LGPD, art. 18). O CPF não
          muda por ali, porque é o seu login.
        </p>
      </section>
      <section>
        <h2>6. Segurança e cookies</h2>
        <p>
          A sessão usa um único cookie, <code>httpOnly</code>, que guarda o seu acesso e não pode ser lido por scripts da
          página. Não usamos cookies de rastreamento nem de publicidade. O acesso expira sozinho em poucos minutos.
        </p>
      </section>
      <section>
        <h2>7. Contato</h2>
        <p>
          Dúvidas sobre os seus dados: abra uma{" "}
          <a href="https://github.com/k3vinrich4rd/rkf_bank/issues" className="underline underline-offset-4">
            issue no repositório do projeto
          </a>{" "}
          sem colocar dados pessoais nela. O canal oficial do encarregado será publicado aqui antes do lançamento.
        </p>
      </section>
    </DocumentoLegal>
  );
}
