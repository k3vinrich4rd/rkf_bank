// Explicação em palavras simples de cada pasta e de cada requisição da coleção do Insomnia.
//
// Para quem nunca testou uma API: o que o passo faz, por que ele existe no banco e o que olhar na resposta.
// O texto técnico (campos, regras do contrato) continua na "descricao" do gerar-colecao-insomnia.mjs; este vem antes
// dele, com 💡, no Insomnia (aba Docs) e nos roteiros gerados por gerar-roteiro-e2e.mjs.
//
// Chave: "<2 dígitos da pasta>|<nome da requisição>". O gerador recusa requisição sem explicação aqui.

export const PASTAS_SIMPLES = {
  "00": "Antes de testar qualquer coisa, confira se a API está ligada. É como ver se a luz da maquininha acendeu.",
  "01": "Entrar no banco, como no app: CPF e senha. A API devolve um \"crachá\" (o token) que as próximas requisições mostram para provar quem você é. O Insomnia guarda esse crachá sozinho.",
  "02": "A vida inteira de um cliente novo, na ordem em que acontece de verdade: criar o cadastro, entrar, abrir duas contas, colocar dinheiro, tirar, transferir e conferir o extrato centavo a centavo.",
  "03": "O mesmo dinheiro, agora entre duas pessoas diferentes (Ana manda para o Bruno), com as contas que já vêm prontas no banco de teste.",
  "04": "Proteção contra o \"cliquei duas vezes\": se a internet cair e o app mandar o mesmo depósito de novo, o dinheiro não pode entrar duas vezes.",
  "05": "O que o administrador do banco faz (e o que ele não pode fazer). Só a equipe testa esta parte: o perfil de visitante não tem administrador.",
  "06": "Os direitos da pessoa sobre os próprios dados (LGPD): ver o que o banco guarda, corrigir, trocar a senha e encerrar o cadastro.",
  "07": "Os erros que o banco TEM que dar. Um teste que espera um erro e recebe o erro certo está passando: é a prova de que a proteção funciona.",
  "08": "Uma regra do navegador (CORS): só o site do RKF Bank pode chamar a API de dentro de uma página. Um site estranho é barrado.",
  "09": "Ficar logado sem digitar a senha de novo, e sair de verdade. O crachá curto (token) é renovado com um segundo crachá, mais longo (refresh token).",
};

export const EXPLICACOES = {
  // 00 · Comece aqui
  "00|Saúde da API": "A primeira pergunta: \"você está aí?\". Resposta `UP` = a API está ligada e pronta. Sem resposta, nada mais vai funcionar: suba a API antes.",
  "00|Versão dos termos (pública)": "Mostra qual versão dos Termos de Uso está valendo. O cadastro precisa dizer que a pessoa aceitou esses termos, igual ao \"li e aceito\" de qualquer app.",

  // 01 · Login
  "01|Login · cliente ORIGEM (Ana / Fellipe)": "Entra como o cliente que vai MANDAR dinheiro. Na resposta, o `token` é o crachá de acesso; o Insomnia guarda ele em `token_origem` e as próximas requisições usam sozinhas.",
  "01|Login · cliente DESTINO (Bruno / Renan)": "Entra como o cliente que vai RECEBER dinheiro. Ter dois clientes logados permite provar que um não enxerga a conta do outro.",
  "01|Login · ADMIN (só perfil equipe ou MySQL)": "Entra como administrador. Repare em `\"perfil\": \"ADMIN\"` na resposta: é esse campo, dentro do token, que libera as rotas de gestão.",

  // 02 · Fluxo E2E
  "02|Cadastrar cliente (público)": "Cria um cliente do zero, sem precisar estar logado (como abrir conta pelo app). O CPF é sorteado a cada envio, então dá para repetir. Repare: a resposta devolve o CPF escondido (`***.982.247-**`) e o endereço que a API buscou sozinha pelo CEP.",
  "02|Login · cliente novo": "O cadastro não faz login automático: agora o cliente novo entra com o CPF e a senha que acabou de criar.",
  "02|Abrir conta ORIGEM: corrente": "Abre uma conta corrente. Você só diz o tipo: o banco sabe de quem é pelo token e gera o número da conta sozinho. Status `201` = criado.",
  "02|Abrir conta DESTINO: poupança": "Abre uma segunda conta, poupança, para o mesmo cliente. Ela vai receber a transferência mais adiante.",
  "02|Minhas contas (as duas com saldo 0,00)": "Lista as contas do cliente logado. As duas acabaram de nascer, então os saldos são zero.",
  "02|Depositar R$ 150,00 na ORIGEM": "Coloca dinheiro na conta. A resposta `204` vem vazia de propósito: quer dizer \"deu certo, não tenho nada para mostrar\". O saldo novo aparece em \"Minhas contas\".",
  "02|Sacar R$ 50,00 da ORIGEM": "Tira dinheiro da conta. Também responde `204`, vazio. Sobram R$ 100,00.",
  "02|Transferir R$ 25,00 ORIGEM → DESTINO": "Move dinheiro de uma conta para a outra. O banco garante que o débito e o crédito acontecem juntos: nunca sai de um lado sem chegar no outro.",
  "02|Saldos: ORIGEM 75,00 · DESTINO 25,00": "A conferência: 150 − 50 − 25 = 75 na ORIGEM e 25 na DESTINO. Se os números baterem, o dinheiro andou certinho.",
  "02|Ver só a conta ORIGEM": "Busca uma conta só, pelo id. É o que a tela usa para atualizar o saldo sem pedir a lista inteira.",
  "02|Extrato da ORIGEM (3 lançamentos)": "O extrato: cada movimento da conta, do mais novo para o mais antigo. Aqui são três (depósito, saque e transferência).",
  "02|Extrato do DESTINO (1 lançamento)": "O extrato do outro lado mostra a transferência chegando. É a prova de que a transferência é um lançamento só, visto pelas duas contas.",

  // 03 · Dinheiro entre clientes
  "03|Minhas contas (ORIGEM)": "Lista as contas da Ana (ou do Fellipe, na equipe), que já vêm com dinheiro do banco de teste. O Insomnia guarda o id da primeira conta para os próximos passos.",
  "03|Minhas contas paginadas": "A mesma lista, em páginas (`page=0&size=10`). Com muitas contas, a tela pede de 10 em 10 em vez de tudo de uma vez.",
  "03|Depositar R$ 150,00": "Depósito na conta da ORIGEM. `204`, sem corpo, é sucesso.",
  "03|Sacar R$ 50,00": "Saque da conta da ORIGEM. `204` de novo.",
  "03|Transferir R$ 25,00 ORIGEM → DESTINO": "Agora de uma PESSOA para OUTRA: Ana manda para o Bruno. Só o dono da conta de origem pode mandar.",
  "03|Extrato da ORIGEM": "O extrato da Ana mostra o depósito, o saque e a transferência que saiu.",
  "03|Extrato do DESTINO (token do destino)": "O Bruno entra com o crachá DELE e vê a transferência que chegou. Com o crachá da Ana, este mesmo pedido daria `403` (proibido).",

  // 04 · Idempotência
  "04|1ª vez: depósito com chave fixa": "Um depósito com uma \"senha de pedido\" (`Idempotency-Key`) que fica guardada. É como o número do pedido de uma compra.",
  "04|2ª vez: mesma chave, mesmo corpo → repetição": "Manda o MESMO pedido de novo, como se a internet tivesse caído e o app tentasse outra vez. O banco reconhece o número, responde igual e NÃO deposita de novo. Repare no header `Idempotency-Replayed: true`.",
  "04|Mesma chave, outro valor → 422": "Mesmo número de pedido, mas outro valor: isso não é repetição, é engano. O banco recusa (`422`) em vez de adivinhar.",
  "04|Sem a chave → 400": "Movimentar dinheiro sem o número do pedido é recusado (`400`). Assim nenhum app esquece essa proteção.",

  // 05 · ADMIN
  "05|Listar clientes (array)": "O ADMIN vê a lista de todos os clientes, com o CPF escondido. Um cliente comum que tentar a mesma rota recebe `403`.",
  "05|Listar clientes (paginado)": "A mesma lista, em páginas. Repare em `totalElements` (quantos existem) e `totalPages` (quantas páginas).",
  "05|Listar TODAS as contas (paginado)": "A mesma rota que o cliente usa para ver as dele; para o ADMIN, ela devolve as contas de todo mundo.",
  "05|Listar usuários (guarda o id do cliente novo)": "Lista os logins (quem pode entrar) com o perfil de cada um. O Insomnia guarda o id do cliente novo para o ADMIN mexer nele a seguir.",
  "05|Promover o cliente novo a ADMIN": "Transforma o cliente novo em administrador. Vale na hora: não precisa reiniciar nada.",
  "05|Token antigo do promovido → 401": "O crachá que o cliente novo tinha antes da promoção para de funcionar (`401`). Mudou o perfil, o crachá velho cai: ninguém fica com permissão desatualizada.",
  "05|Login do promovido (já vem ADMIN)": "Ele entra de novo e o crachá novo já vem com `\"perfil\": \"ADMIN\"`.",
  "05|Promovido lista clientes → 200": "Agora ele consegue o que antes dava `403`: a promoção funcionou.",
  "05|Promovido tenta rebaixar o ADMIN do .env → 422": "O ADMIN criado pelo `.env` não é cliente, então não pode virar CLIENTE. O banco recusa (`422`) e explica no `codigo`.",
  "05|Rebaixar de volta a CLIENTE": "Desfaz a promoção: o cliente novo volta a ser cliente comum.",
  "05|Desativar o acesso": "Bloqueia o login da pessoa, sem apagar nada. No banco, usuário nunca é apagado: só desativado (o histórico fica).",
  "05|Login do desativado → 401": "Com o acesso desativado, nem a senha certa entra.",
  "05|Reativar o acesso": "Desbloqueia: a pessoa volta a conseguir entrar.",
  "05|Redefinir a senha (quem esqueceu)": "O ADMIN cria uma senha temporária para quem esqueceu a dele. A pessoa vai ser obrigada a trocar no primeiro acesso.",
  "05|Login com a senha temporária (deveTrocarSenha)": "Entra com a senha temporária. Repare em `\"deveTrocarSenha\": true`: é o aviso para a tela pedir uma senha nova.",
  "05|Senha temporária em outra rota → 403": "Com a senha temporária, nada além da troca de senha funciona (`403`). Ninguém usa o banco com uma senha que o ADMIN conhece.",
  "05|Trocar a senha temporária": "A pessoa escolhe a própria senha. A partir daqui, só ela sabe.",
  "05|Login · cliente novo (de novo, senha escolhida)": "Entra com a senha que escolheu: `deveTrocarSenha` volta a ser `false`.",
  "05|Trava: ADMIN mexe no próprio perfil → 422": "O ADMIN não pode mudar o próprio perfil. Sem essa trava, ele poderia se rebaixar sem querer e o banco ficaria sem nenhum administrador.",
  "05|Trava: ADMIN redefine a própria senha pela gestão → 422": "Para a própria senha, o ADMIN usa a troca normal (que pede a senha atual), não a gestão de usuários.",
  "05|Perfil que não existe → 400": "Pedir um perfil que não existe (`GERENTE`) é recusado: só existem CLIENTE e ADMIN.",
  "05|Usuário que não existe → 404": "Um id de usuário que não existe dá `404` (não encontrado), com o `codigo` explicando.",
  "05|Acesso sem o campo → 400": "Esqueceu de dizer se é para ativar ou desativar? `400`: o banco não chuta.",
  "05|Senha curta na redefinição → 400": "Senha temporária curta demais é recusada: a regra de senha forte vale até para o ADMIN.",
  "05|ADMIN do .env abre conta → 422": "O ADMIN do `.env` não é cliente (não tem cadastro), então não pode abrir conta.",
  "05|ADMIN do .env em /clientes/me → 422": "\"Meus dados\" é para quem tem cadastro de cliente; o ADMIN do `.env` não tem.",
  "05|ADMIN deposita na conta de um cliente → 403": "O ADMIN vê as contas, mas NÃO mexe no dinheiro dos outros. Ser administrador não dá direito de movimentar conta alheia.",
  "05|ADMIN vê o extrato de um cliente → 403": "O extrato é privado: nem o ADMIN vê o detalhe das movimentações de outra pessoa.",
  "05|ADMIN vê uma conta de cliente → 200": "Ver a conta (número e saldo) o ADMIN pode: é o que ele precisa para atender um cliente.",
  "05|Conciliação: saldo × extrato": "A auditoria: para cada conta, o banco soma o extrato e compara com o saldo. A resposta é a lista das contas que NÃO batem: vazia (`[]`) quer dizer que o dinheiro de todo mundo está certo.",

  // 06 · Meus dados
  "06|Ver meus dados (ORIGEM)": "O cliente vê tudo o que o banco guarda sobre ele. É um direito da LGPD.",
  "06|Corrigir meu telefone (ORIGEM)": "Corrige um dado. Só o campo enviado muda; os outros ficam como estavam.",
  "06|Mudar de endereço: CEP + número (ORIGEM)": "Muda o endereço: com o CEP novo, a API busca a rua e a cidade sozinha. O número da casa é obrigatório junto.",
  "06|CEP sem número → 400": "CEP novo sem o número da casa não dá para montar o endereço: `400`.",
  "06|Corpo vazio → 400": "Pedir para corrigir sem mandar nada é recusado: não há o que mudar.",
  "06|E-mail de outra pessoa → 409": "Um e-mail que já é de outro cliente dá `409` (conflito): cada e-mail é de uma pessoa só.",
  "06|Trocar senha com a atual errada → 422": "Para trocar a senha, é preciso saber a atual. Se alguém pegar o celular desbloqueado, não troca a senha sem saber a antiga.",
  "06|Nova senha igual à atual → 422": "Trocar a senha pela mesma não é troca: recusado.",
  "06|Trocar a própria senha (cliente novo)": "A troca certa: senha atual + senha nova. `204` = trocou.",
  "06|Token antigo depois da troca → 401": "Trocou a senha, todos os crachás antigos caem. Se alguém estava logado com a senha velha, foi desconectado.",
  "06|Login · cliente novo com a senha nova": "Entra de novo, agora com a senha nova.",
  "06|Encerrar com a senha errada → 422": "Encerrar o cadastro pede a senha, para ninguém encerrar a conta de outra pessoa por engano.",
  "06|Encerrar com saldo (ORIGEM) → 422": "Não dá para encerrar com dinheiro na conta: primeiro a pessoa saca ou transfere tudo.",
  "06|Zerar a ORIGEM (saque de R$ 75,00)": "Saca o que sobrou na conta ORIGEM do cliente novo, para poder encerrar.",
  "06|Zerar o DESTINO (saque de R$ 25,00)": "Saca o que sobrou na conta DESTINO. As duas ficam com zero.",
  "06|⚠️ Encerrar o cadastro (cliente novo)": "Encerra o cadastro: o login para de funcionar na hora. Os dados ficam guardados só pelo prazo que a lei exige de um banco (repare em `anonimizacaoPrevistaPara`, 5 anos depois) e então são anonimizados, sem que ninguém precise lembrar.",
  "06|Token do encerrado → 401": "Depois de encerrar, o crachá para de funcionar na hora.",
  "06|Login do encerrado → 401": "E não dá mais para entrar, nem com a senha certa.",
  "06|ADMIN tenta reativar o encerrado → 422": "Nem o ADMIN traz de volta quem encerrou: a decisão foi da própria pessoa.",

  // 07 · Erros
  "07|Sem token → 401": "Pedir as contas sem o crachá: `401` (\"quem é você?\").",
  "07|Token adulterado → 401": "Um crachá falsificado, que diz ser ADMIN. A assinatura não bate e o banco recusa: não dá para inventar o próprio crachá.",
  "07|Senha errada no login → 401": "Senha errada não entra. A mensagem não diz se o erro foi o CPF ou a senha, para não ajudar quem tenta adivinhar.",
  "07|CLIENTE em rota de ADMIN → 403": "Um cliente comum tentando ver a lista de todos os clientes: `403` (\"sei quem você é, mas você não pode\").",
  "07|CLIENTE na gestão de usuários → 403": "Um cliente tentando entrar na gestão de usuários do ADMIN: `403`.",
  "07|Depositar na conta de outro cliente → 403": "O Bruno tentando mexer na conta da Ana: `403`. Só o dono movimenta a própria conta.",
  "07|Ver o extrato de outro cliente → 403": "O Bruno tentando ver o extrato da Ana: `403`.",
  "07|Ver a conta de outro cliente → 403": "O Bruno tentando ver o saldo da Ana: `403`.",
  "07|CLIENTE na conciliação → 403": "A auditoria do banco é só do ADMIN: `403` para cliente.",
  "07|Sacar mais que o saldo → 422": "Sacar mais do que tem: recusado com o `codigo` `SALDO_INSUFICIENTE`. Nenhum centavo sai.",
  "07|Transferir mais que o saldo → 422": "Transferir mais do que tem: recusado, e nada muda em nenhuma das duas contas.",
  "07|Transferir para a mesma conta → 422": "Transferir de uma conta para ela mesma não faz sentido: recusado.",
  "07|Conta que não existe → 404": "O extrato de uma conta que não existe: `404` (não encontrado).",
  "07|Ver conta que não existe → 404": "Uma conta que não existe: `404`.",
  "07|Id que não é UUID → 400": "O id das contas tem um formato fixo (UUID). `abc` nem chega a ser procurado: `400`.",
  "07|Paginação inválida → 400": "Pedir páginas de tamanho zero: `400`.",
  "07|Tipo de conta inválido → 400": "Só existem conta CORRENTE e POUPANCA. Outro tipo: `400`.",
  "07|JSON quebrado → 400": "O corpo do pedido com erro de digitação (JSON inválido): `400`, sem quebrar a API.",
  "07|Valor com 3 casas → 400": "Dinheiro tem duas casas depois da vírgula. R$ 10,999 não existe: `400`.",
  "07|Cadastro inválido → 400 com os campos": "Um cadastro todo errado. Repare em `campos`: a resposta diz o que está errado em CADA campo, para a tela mostrar embaixo de cada um.",
  "07|CPF já cadastrado → 409": "Um CPF que já tem cadastro: `409` (conflito). Cada CPF é um cliente só.",
  "07|Cadastro sem aceitar os termos → 400": "Sem aceitar os termos, não tem cadastro (LGPD).",
  "07|Cadastro com termos antigos → 422": "Aceitar uma versão velha dos termos não vale: a pessoa precisa aceitar a versão atual.",
  "07|CEP que não existe → 422": "Um CEP que o ViaCEP não conhece: o cadastro é recusado em vez de salvar um endereço vazio.",

  // 08 · CORS
  "08|Preflight de origem permitida → 200": "Antes de chamar a API, o navegador pergunta \"posso?\" (o preflight). Vindo do site do RKF Bank (`localhost:3000`), a resposta é sim.",
  "08|Preflight de origem estranha → 403": "A mesma pergunta vinda de um site desconhecido: não (`403`). Assim um site malicioso não usa a sua sessão para mexer na sua conta.",

  // 09 · Sessão
  "09|Login · cliente DESTINO": "Entra de novo para ter um par novinho de crachás: o de acesso (curto) e o de renovação (7 dias).",
  "09|Renovar: troca o par": "Troca o crachá de renovação por um par novo, sem digitar a senha. É assim que o app te mantém logado. O de renovação antigo deixa de valer.",
  "09|O token novo funciona": "O crachá novo abre as contas normalmente.",
  "09|Reusar o token já trocado → 401": "Usar de novo um crachá de renovação que já foi trocado é sinal de roubo: o banco recusa E derruba todas as sessões da pessoa.",
  "09|Depois do reuso, a renovação legítima também caiu → 401": "Prova do passo anterior: até o crachá novo, que era legítimo, foi derrubado. Quem roubou fica sem acesso; o dono só precisa entrar de novo.",
  "09|Login · cliente DESTINO (de novo)": "Entra de novo depois da derrubada.",
  "09|Sair (logout)": "Sair de verdade: o crachá de renovação é cancelado no banco, não só apagado do aparelho.",
  "09|Renovar depois de sair → 401": "Depois de sair, o crachá de renovação não serve mais para nada.",
  "09|Sair com token desconhecido → 204": "Sair com um crachá que o banco não conhece também responde `204`: a API não revela se o crachá existia.",
  "09|Renovar sem o token → 400": "Pedir renovação sem mandar o crachá: `400`.",
  "09|Login · cliente DESTINO (para as outras pastas)": "Deixa o Bruno logado no fim, para você poder mandar as outras pastas de novo sem refazer o login.",
};
