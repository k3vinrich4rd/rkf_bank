---
name: explicar-codigo
description: Explica um código que já existe no RKF Bank (arquivo, classe, método, fluxo ou conceito usado no projeto) de forma conceitual e prática, sem alterar nada. Use quando o usuário pedir "/explicar-codigo <alvo>", "me explica o ContaService", "como funciona o filtro de log", "o que esse @Version faz aqui" ou similar.
---

# /explicar-codigo <alvo>

Modo **somente leitura**: não edite arquivos. O objetivo é a pessoa entender o código a ponto de explicá-lo numa entrevista.

## Premissas
- Português, linguagem acessível; termo em inglês traduzido na primeira vez.
- Sem quiz, sem "teste seu conhecimento". Explique e pronto.
- Cite sempre `arquivo:linha` para cada afirmação sobre o código.
- Backend e frontend valem; no frontend, respeite as regras de `frontend/CLAUDE.md` ao descrever os padrões.

## Como achar o alvo (barato)
- Arquivo ou classe: `Glob` pelo nome e leia só ele.
- Fluxo ("como funciona a transferência"): comece pelo controller (`Grep "@PostMapping" backend/src/main/java/com/br/rkfbank`; cada modulo, como `contas/`, tem o controller, o service e o repository dele), siga para o service e o repository. No frontend: `app/<rota>/page.tsx` → `components/<tela>/<tela>-screen.tsx` → `use-*.ts` → `lib/api`.
- Conceito ("@Transactional", "X-Request-Id"): `Grep` pelo termo e escolha os 2 ou 3 usos mais representativos.
- Leia só o necessário. Não abra o projeto inteiro.

## Formato da resposta

1. **Em uma frase:** o que esse código faz e para quem.
2. **🧠 Conceitos envolvidos:** tabela `conceito | o que é | onde aparece (arquivo:linha)`, no máximo 6 linhas.
3. **🔍 Passo a passo:** o caminho de uma execução real, numerado, citando as linhas. Use um diagrama Mermaid (`sequenceDiagram` ou `flowchart`) quando houver mais de 3 participantes.
4. **❓ Por que foi feito assim:** as decisões de design e a alternativa que foi descartada (ex.: "lock otimista em vez de pessimista porque...").
5. **🧪 Onde está testado:** testes que cobrem esse código (`Grep` pelo nome da classe em `backend/src/test`). Se não houver teste, diga.
6. **⚠️ Pontos de atenção:** riscos, débitos técnicos relacionados (`planing/debitos-tecnicos.md`) e pegadinhas.
7. **🎤 Como explicar em entrevista:** 3 a 5 linhas em primeira pessoa.

Se o usuário quiser guardar a explicação, ofereça `/registrar` (nota no Obsidian).
