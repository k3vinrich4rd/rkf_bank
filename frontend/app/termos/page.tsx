import type { Metadata } from "next";
import Link from "next/link";
import { DocumentoLegal } from "@/components/legal/documento-legal";
import { ROUTES } from "@/lib/routes";

export const metadata: Metadata = {
  title: "Termos de Uso",
  description: "As regras para usar o RKF Bank: conta, senha, operações e encerramento.",
};

export default function TermosPage() {
  return (
    <DocumentoLegal titulo="Termos de Uso" resumo="As regras para usar o RKF Bank: conta, senha, operações e encerramento.">
      <section>
        <h2>1. O que é o RKF Bank</h2>
        <p>
          Um banco digital criado em Osasco, SP, por Kevin, Renan e Fellipe, como projeto de estudo. Os saldos e as
          operações são simulados: nenhum valor real é movimentado e nenhuma conta aqui substitui uma conta bancária.
        </p>
      </section>
      <section>
        <h2>2. Cadastro e conta</h2>
        <ul>
          <li>O cadastro é pessoal: use os seus dados (o CPF precisa ser válido e só pode ter um cadastro).</li>
          <li>Você pode abrir conta corrente e poupança. A agência e o número da conta são definidos pelo banco.</li>
          <li>O aceite destes Termos e da <Link href={ROUTES.privacidade} className="underline underline-offset-4">Política de Privacidade</Link> é registrado com a versão e a data.</li>
        </ul>
      </section>
      <section>
        <h2>3. Senha e acesso</h2>
        <ul>
          <li>A senha é sua e não deve ser compartilhada. Ninguém do RKF pede a sua senha.</li>
          <li>Depois de 5 tentativas erradas seguidas, o acesso fica bloqueado por alguns minutos, para proteger você.</li>
          <li>Trocar a senha encerra todas as sessões abertas.</li>
        </ul>
      </section>
      <section>
        <h2>4. Operações</h2>
        <ul>
          <li>Depósito, saque e transferência acontecem na hora e aparecem no extrato.</li>
          <li>Não é possível sacar ou transferir mais do que o saldo, nem transferir para a mesma conta.</li>
          <li>Se a conexão cair, reenviar a mesma operação não cobra duas vezes.</li>
        </ul>
      </section>
      <section>
        <h2>5. Encerramento</h2>
        <p>
          Você pode encerrar o cadastro quando quiser, em <Link href={ROUTES.meusDados} className="underline underline-offset-4">Meus dados</Link>,
          com as contas zeradas. O que acontece com os dados depois disso está na Política de Privacidade.
        </p>
      </section>
      <section>
        <h2>6. Mudanças nestes Termos</h2>
        <p>
          Quando o texto mudar, a versão no topo muda junto, e um cadastro novo só é aceito com a versão atual.
        </p>
      </section>
    </DocumentoLegal>
  );
}
