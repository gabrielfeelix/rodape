// data.jsx — sample data for the prototype (Brazilian Portuguese)

const TB_DATA = {
  user: {
    name: 'Bia',
    fullName: 'Beatriz Almeida',
    email: 'bia.almeida@gmail.com',
    booksRead: 23,
    activeClubs: 3,
  },

  clubs: [
    {
      id: 'leituras-de-domingo',
      name: 'Leituras de domingo',
      color: '#5C7349',
      colorSoft: '#E5EBDA',
      letter: 'L',
      description: 'Um lugar pra falar dos livros que a gente lê devagar, com calma de domingo.',
      lastActivity: 'Lendo: A Hora da Estrela',
      members: 7,
      isActive: true,
      rules: [
        'Sem spoilers fora do capítulo liberado',
        'Encontros no último domingo do mês',
        'Cada membro sugere um livro por trimestre',
      ],
    },
    {
      id: 'clube-da-mae',
      name: 'Clube da mãe',
      color: '#B85838',
      colorSoft: '#FBE5DA',
      letter: 'C',
      lastActivity: 'Lendo: Becos da memória',
      members: 4,
      isActive: false,
    },
    {
      id: 'sextas-literarias',
      name: 'Sextas literárias',
      color: '#7A4F2B',
      colorSoft: '#EFE5D6',
      letter: 'S',
      lastActivity: 'Acabou: Torto arado',
      members: 9,
      isActive: false,
    },
  ],

  currentBook: {
    title: 'A Hora da Estrela',
    author: 'Clarice Lispector',
    genre: 'Romance',
    rating: 4.6,
    readers: 7,
    chapters: 13,
    userChapter: 8,
    chapterList: [
      { n: 1,  title: 'A culpa é minha',        state: 'read',     comments: 12 },
      { n: 2,  title: 'Deixa eu pelo menos pensar', state: 'read', comments: 8 },
      { n: 3,  title: 'O canto de Macabéa',     state: 'read',     comments: 15 },
      { n: 4,  title: 'A família',              state: 'read',     comments: 9 },
      { n: 5,  title: 'No princípio era o verbo', state: 'read',   comments: 7 },
      { n: 6,  title: 'A foto',                  state: 'read',    comments: 5 },
      { n: 7,  title: 'Encontro com Olímpico',  state: 'read',     comments: 11 },
      { n: 8,  title: 'A vidente',               state: 'current', comments: 4 },
      { n: 9,  title: 'A coca-cola',             state: 'locked' },
      { n: 10, title: 'O dia seguinte',          state: 'locked' },
      { n: 11, title: 'Macabéa atravessa',       state: 'locked' },
      { n: 12, title: 'Quanto ao futuro',        state: 'locked' },
      { n: 13, title: 'Tudo no mundo começou com um sim', state: 'locked' },
    ],
  },

  members: [
    { name: 'Marina',  status: 'Cap. 9',   chapter: 9,  ahead: true },
    { name: 'Rafael',  status: 'Cap. 8',   chapter: 8 },
    { name: 'Júlia',   status: 'Terminou', chapter: 13, done: true },
    { name: 'Leo',     status: 'Cap. 8',   chapter: 8 },
    { name: 'Helena',  status: 'Cap. 6',   chapter: 6, late: true },
    { name: 'Tomás',   status: 'Cap. 10',  chapter: 10, ahead: true },
  ],

  comments: [
    {
      author: 'Marina', when: '2h',
      text: 'Esse capítulo me deixou com um nó. A vidente fala umas coisas que a Macabéa nem entende direito, mas a gente sente o peso de cada uma. Clarice é cruel e doce ao mesmo tempo.',
      reactions: { '❤': 4, '🤯': 2, '💀': 0, '✨': 1 },
    },
    {
      author: 'Bia', when: '4h', own: true,
      text: 'Concordo, Marina. Tinha lido um trecho desse capítulo solto e não tinha entendido nada. No contexto inteiro faz muito mais sentido — sobretudo a previsão do estrangeiro.',
      reactions: { '❤': 2, '🤯': 0, '💀': 0, '✨': 3 },
    },
    {
      author: 'Rafael', when: 'ontem',
      text: 'Reparem como o narrador (Rodrigo SM) some quase por completo nessa parte. É como se ele deixasse a Macabéa respirar sozinha por uma página inteira.',
      reactions: { '❤': 5, '🤯': 1, '💀': 0, '✨': 2 },
    },
    {
      author: 'Júlia', when: 'ontem',
      text: 'O detalhe do anel comprado por dinheiro emprestado destruiu meu coração. Macabéa querendo acreditar em qualquer coisa que não fosse a vida dela.',
      reactions: { '❤': 6, '🤯': 0, '💀': 2, '✨': 0 },
    },
  ],

  meeting: {
    monthShort: 'OUT',
    day: '24',
    weekday: 'Domingo',
    title: 'Discussão: A Hora da Estrela',
    timeStart: '19:00',
    timeEnd: '21:00',
    place: 'Café Lispector, Vila Madalena',
    confirmed: ['Marina', 'Rafael', 'Júlia', 'Leo'],
    maybe:     ['Helena'],
    no:        ['Tomás'],
    agenda: [
      'Abertura — primeiras impressões',
      'O narrador Rodrigo SM: invenção ou Clarice?',
      'Macabéa como personagem ou como espelho?',
      'Bate-papo aberto sobre o final',
      'Próximo livro: anúncio da votação',
    ],
  },

  votingBooks: [
    { id: 'b1', title: 'Becos da memória',   author: 'Conceição Evaristo', suggester: 'Marina',
      reason: 'Pra ler mais Conceição depois de Olhos d\'água. Curto, intenso, fica.',
      votes: 4, total: 7, mine: false },
    { id: 'b2', title: 'O quinze',            author: 'Rachel de Queiroz', suggester: 'Júlia',
      reason: 'Quero a gente lendo as autoras dos anos 30. Apetece um sertão.',
      votes: 2, total: 7, mine: false },
    { id: 'b3', title: 'A vegetariana',        author: 'Han Kang',          suggester: 'Bia',
      reason: 'Já que a gente tem lido brasileiras, pensei em sair pra Coreia. Estranho do jeito bom.',
      votes: 1, total: 7, mine: true },
  ],

  shelf: [
    { title: 'Olhos d\'água',          author: 'Conceição Evaristo', when: 'set/25', rating: 4.8 },
    { title: 'Pedro Páramo',           author: 'Juan Rulfo',         when: 'ago/25', rating: 4.4 },
    { title: 'Torto arado',            author: 'Itamar Vieira Júnior', when: 'jun/25', rating: 4.9 },
    { title: 'Quarto de despejo',      author: 'Carolina Maria de Jesus', when: 'mai/25', rating: 4.7 },
    { title: 'O verão tardio',          author: 'Luiz Ruffato',      when: 'mar/25', rating: 4.2 },
    { title: 'A paixão segundo G.H.',   author: 'Clarice Lispector', when: 'jan/25', rating: 4.5 },
  ],

  notifications: {
    HOJE: [
      { kind: 'comment', who: 'Marina', text: 'comentou no Capítulo 8', context: '"A vidente" · 2h', unread: true },
      { kind: 'vote',    who: 'Rafael', text: 'votou em Becos da memória', context: 'Votação · 3h', unread: true },
      { kind: 'meet',    who: null,     text: 'Lembrete: encontro do clube amanhã', context: 'Domingo 19:00 · 4h', unread: true },
    ],
    ONTEM: [
      { kind: 'comment', who: 'Júlia',  text: 'respondeu ao teu comentário', context: 'Capítulo 7 · ontem' },
      { kind: 'done',    who: 'Tomás',  text: 'terminou A Hora da Estrela', context: 'ontem' },
    ],
    'ESTA SEMANA': [
      { kind: 'vote',    who: null,     text: 'A votação do próximo livro começou', context: '3 sugestões · 3 dias' },
    ],
  },
};

window.TB_DATA = TB_DATA;
