-- Snapshot das tabelas public (colunas + constraints) — Management API 2026-07-12

CREATE TABLE public.book_ratings (
  book_id uuid NOT NULL,
  club_id uuid NOT NULL,
  user_id uuid NOT NULL,
  stars integer NOT NULL,
  comment text NOT NULL DEFAULT ''::text,
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT book_ratings_pkey PRIMARY KEY (book_id, club_id, user_id),
  CONSTRAINT book_ratings_book_id_fkey FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
  CONSTRAINT book_ratings_club_id_fkey FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE,
  CONSTRAINT book_ratings_user_id_fkey FOREIGN KEY (user_id) REFERENCES profiles(id) ON DELETE CASCADE,
  CONSTRAINT book_ratings_stars_check CHECK (((stars >= 1) AND (stars <= 5)))
);

CREATE TABLE public.book_suggestions (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  club_id uuid NOT NULL,
  book_id uuid NOT NULL,
  voting_round_id uuid,
  sugerido_por uuid NOT NULL,
  justificativa text,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT book_suggestions_voting_round_id_book_id_key UNIQUE (voting_round_id, book_id),
  CONSTRAINT book_suggestions_pkey PRIMARY KEY (id),
  CONSTRAINT book_suggestions_book_id_fkey FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE RESTRICT,
  CONSTRAINT book_suggestions_club_id_fkey FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE,
  CONSTRAINT book_suggestions_sugerido_por_fkey FOREIGN KEY (sugerido_por) REFERENCES profiles(id) ON DELETE RESTRICT,
  CONSTRAINT book_suggestions_voting_round_id_fkey FOREIGN KEY (voting_round_id) REFERENCES voting_rounds(id) ON DELETE CASCADE,
  CONSTRAINT book_suggestions_justificativa_check CHECK (((justificativa IS NULL) OR (char_length(justificativa) <= 500)))
);

CREATE TABLE public.book_summaries (
  book_id uuid NOT NULL,
  club_id uuid NOT NULL,
  texto text NOT NULL DEFAULT ''::text,
  last_editor_id uuid,
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT book_summaries_pkey PRIMARY KEY (book_id, club_id),
  CONSTRAINT book_summaries_book_id_fkey FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
  CONSTRAINT book_summaries_club_id_fkey FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE,
  CONSTRAINT book_summaries_last_editor_id_fkey FOREIGN KEY (last_editor_id) REFERENCES profiles(id) ON DELETE SET NULL
);

CREATE TABLE public.books (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  title text NOT NULL,
  author text NOT NULL,
  cover_url text,
  openlibrary_id text,
  isbn text,
  is_manual boolean NOT NULL DEFAULT false,
  total_paginas integer,
  editora text,
  ano_publicacao integer,
  idioma text NOT NULL DEFAULT 'pt'::text,
  created_by uuid,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT books_pkey PRIMARY KEY (id),
  CONSTRAINT books_created_by_fkey FOREIGN KEY (created_by) REFERENCES profiles(id) ON DELETE SET NULL,
  CONSTRAINT books_ano_publicacao_check CHECK (((ano_publicacao IS NULL) OR ((ano_publicacao >= 1000) AND (ano_publicacao <= 2200)))),
  CONSTRAINT books_total_paginas_check CHECK (((total_paginas IS NULL) OR ((total_paginas >= 1) AND (total_paginas <= 10000))))
);

CREATE TABLE public.chapters (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  book_id uuid NOT NULL,
  numero integer NOT NULL,
  titulo text NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT chapters_book_id_numero_key UNIQUE (book_id, numero),
  CONSTRAINT chapters_pkey PRIMARY KEY (id),
  CONSTRAINT chapters_book_id_fkey FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
  CONSTRAINT chapters_numero_check CHECK ((numero > 0))
);

CREATE TABLE public.club_books (
  club_id uuid NOT NULL,
  book_id uuid NOT NULL,
  status club_book_status NOT NULL,
  ordem integer NOT NULL DEFAULT 0,
  data_encontro timestamp with time zone,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT club_books_pkey PRIMARY KEY (club_id, book_id),
  CONSTRAINT club_books_book_id_fkey FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
  CONSTRAINT club_books_club_id_fkey FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE
);

CREATE TABLE public.club_members (
  club_id uuid NOT NULL,
  user_id uuid NOT NULL,
  papel member_role NOT NULL DEFAULT 'member'::member_role,
  entrou_em timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT enforce_super_admin_invariant_trg TRIGGER DEFERRABLE INITIALLY DEFERRED,
  CONSTRAINT club_members_pkey PRIMARY KEY (club_id, user_id),
  CONSTRAINT club_members_club_id_fkey FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE,
  CONSTRAINT club_members_user_id_fkey FOREIGN KEY (user_id) REFERENCES profiles(id) ON DELETE CASCADE
);

CREATE TABLE public.clubs (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  nome text NOT NULL,
  descricao text,
  codigo text NOT NULL,
  cor text NOT NULL DEFAULT '0'::text,
  privacidade club_privacy NOT NULL DEFAULT 'convidados'::club_privacy,
  criador_id uuid NOT NULL,
  arquivado boolean NOT NULL DEFAULT false,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT clubs_codigo_key UNIQUE (codigo),
  CONSTRAINT clubs_pkey PRIMARY KEY (id),
  CONSTRAINT clubs_criador_id_fkey FOREIGN KEY (criador_id) REFERENCES profiles(id) ON DELETE RESTRICT,
  CONSTRAINT clubs_codigo_check CHECK ((codigo ~ '^[A-Z0-9]{6}$'::text)),
  CONSTRAINT clubs_descricao_check CHECK (((descricao IS NULL) OR (char_length(descricao) <= 140))),
  CONSTRAINT clubs_nome_check CHECK (((char_length(nome) >= 1) AND (char_length(nome) <= 40)))
);

CREATE TABLE public.comments (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  chapter_id uuid NOT NULL,
  club_id uuid NOT NULL,
  user_id uuid NOT NULL,
  texto text NOT NULL,
  removido boolean NOT NULL DEFAULT false,
  removido_por uuid,
  motivo_remocao text,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT comments_pkey PRIMARY KEY (id),
  CONSTRAINT comments_chapter_id_fkey FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE CASCADE,
  CONSTRAINT comments_club_id_fkey FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE,
  CONSTRAINT comments_removido_por_fkey FOREIGN KEY (removido_por) REFERENCES profiles(id) ON DELETE SET NULL,
  CONSTRAINT comments_user_id_fkey FOREIGN KEY (user_id) REFERENCES profiles(id) ON DELETE RESTRICT,
  CONSTRAINT comments_texto_check CHECK (((char_length(texto) >= 1) AND (char_length(texto) <= 4000)))
);

CREATE TABLE public.meeting_minutes (
  meeting_id uuid NOT NULL,
  texto text NOT NULL DEFAULT ''::text,
  last_editor_id uuid,
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT meeting_minutes_pkey PRIMARY KEY (meeting_id),
  CONSTRAINT meeting_minutes_last_editor_id_fkey FOREIGN KEY (last_editor_id) REFERENCES profiles(id) ON DELETE SET NULL,
  CONSTRAINT meeting_minutes_meeting_id_fkey FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE
);

CREATE TABLE public.meeting_notes (
  meeting_id uuid NOT NULL,
  user_id uuid NOT NULL,
  texto text NOT NULL DEFAULT ''::text,
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT meeting_notes_pkey PRIMARY KEY (meeting_id, user_id),
  CONSTRAINT meeting_notes_meeting_id_fkey FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE,
  CONSTRAINT meeting_notes_user_id_fkey FOREIGN KEY (user_id) REFERENCES profiles(id) ON DELETE CASCADE
);

CREATE TABLE public.meeting_patterns (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  club_id uuid NOT NULL,
  dia_semana integer NOT NULL,
  hora text NOT NULL,
  local text,
  agenda_template text,
  ativo boolean NOT NULL DEFAULT true,
  tipo_recorrencia voting_cadence NOT NULL DEFAULT 'semanal'::voting_cadence,
  valor_recorrencia integer NOT NULL DEFAULT 0,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT meeting_patterns_pkey PRIMARY KEY (id),
  CONSTRAINT meeting_patterns_club_id_fkey FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE,
  CONSTRAINT meeting_patterns_dia_semana_check CHECK (((dia_semana >= 1) AND (dia_semana <= 7)))
);

CREATE TABLE public.meeting_rsvps (
  meeting_id uuid NOT NULL,
  user_id uuid NOT NULL,
  status rsvp_status NOT NULL,
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT meeting_rsvps_pkey PRIMARY KEY (meeting_id, user_id),
  CONSTRAINT meeting_rsvps_meeting_id_fkey FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE,
  CONSTRAINT meeting_rsvps_user_id_fkey FOREIGN KEY (user_id) REFERENCES profiles(id) ON DELETE CASCADE
);

CREATE TABLE public.meetings (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  club_id uuid NOT NULL,
  data timestamp with time zone NOT NULL,
  local text,
  agenda text,
  book_id uuid,
  chapter_start integer,
  chapter_end integer,
  status meeting_status NOT NULL DEFAULT 'agendado'::meeting_status,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT meetings_pkey PRIMARY KEY (id),
  CONSTRAINT meetings_book_id_fkey FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE SET NULL,
  CONSTRAINT meetings_club_id_fkey FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE,
  CONSTRAINT meetings_chapter_end_check CHECK (((chapter_end IS NULL) OR (chapter_end > 0))),
  CONSTRAINT meetings_chapter_start_check CHECK (((chapter_start IS NULL) OR (chapter_start > 0))),
  CONSTRAINT meetings_check CHECK (((chapter_end IS NULL) OR (chapter_start IS NULL) OR (chapter_end >= chapter_start)))
);

CREATE TABLE public.member_removals (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  club_id uuid NOT NULL,
  user_id uuid NOT NULL,
  removed_by uuid NOT NULL,
  motivo text,
  removed_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT member_removals_pkey PRIMARY KEY (id),
  CONSTRAINT member_removals_club_id_fkey FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE,
  CONSTRAINT member_removals_removed_by_fkey FOREIGN KEY (removed_by) REFERENCES profiles(id) ON DELETE RESTRICT,
  CONSTRAINT member_removals_user_id_fkey FOREIGN KEY (user_id) REFERENCES profiles(id) ON DELETE RESTRICT
);

CREATE TABLE public.notifications (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL,
  club_id uuid,
  tipo notification_type NOT NULL,
  payload jsonb NOT NULL DEFAULT '{}'::jsonb,
  lida boolean NOT NULL DEFAULT false,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT notifications_pkey PRIMARY KEY (id),
  CONSTRAINT notifications_club_id_fkey FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE,
  CONSTRAINT notifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES profiles(id) ON DELETE CASCADE
);

CREATE TABLE public.profiles (
  id uuid NOT NULL,
  nome text NOT NULL,
  sobrenome text,
  avatar_key text NOT NULL DEFAULT 'preset:leitor'::text,
  font_scale numeric(3,2) NOT NULL DEFAULT 1.00,
  pronome text,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT profiles_pkey PRIMARY KEY (id),
  CONSTRAINT profiles_id_fkey FOREIGN KEY (id) REFERENCES auth.users(id) ON DELETE CASCADE,
  CONSTRAINT profiles_font_scale_check CHECK (((font_scale >= 0.80) AND (font_scale <= 1.40))),
  CONSTRAINT profiles_nome_check CHECK (((char_length(nome) >= 1) AND (char_length(nome) <= 60))),
  CONSTRAINT profiles_sobrenome_check CHECK (((sobrenome IS NULL) OR (char_length(sobrenome) <= 60))),
  CONSTRAINT profiles_pronome_check CHECK (((pronome IS NULL) OR (char_length(pronome) <= 40)))
);

CREATE TABLE public.reactions (
  comment_id uuid NOT NULL,
  user_id uuid NOT NULL,
  emoji text NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT reactions_pkey PRIMARY KEY (comment_id, user_id, emoji),
  CONSTRAINT reactions_comment_id_fkey FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
  CONSTRAINT reactions_user_id_fkey FOREIGN KEY (user_id) REFERENCES profiles(id) ON DELETE CASCADE,
  CONSTRAINT reactions_emoji_check CHECK (((char_length(emoji) >= 1) AND (char_length(emoji) <= 16)))
);

CREATE TABLE public.saved_quotes (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL,
  club_id uuid NOT NULL,
  book_id uuid NOT NULL,
  texto text NOT NULL,
  capitulo_ref text,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT saved_quotes_pkey PRIMARY KEY (id),
  CONSTRAINT saved_quotes_book_id_fkey FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
  CONSTRAINT saved_quotes_club_id_fkey FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE,
  CONSTRAINT saved_quotes_user_id_fkey FOREIGN KEY (user_id) REFERENCES profiles(id) ON DELETE CASCADE,
  CONSTRAINT saved_quotes_texto_check CHECK (((char_length(texto) >= 1) AND (char_length(texto) <= 2000)))
);

CREATE TABLE public.user_progress (
  user_id uuid NOT NULL,
  club_id uuid NOT NULL,
  book_id uuid NOT NULL,
  current_chapter integer NOT NULL DEFAULT 0,
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT user_progress_pkey PRIMARY KEY (user_id, club_id, book_id),
  CONSTRAINT user_progress_book_id_fkey FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
  CONSTRAINT user_progress_club_id_fkey FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE,
  CONSTRAINT user_progress_user_id_fkey FOREIGN KEY (user_id) REFERENCES profiles(id) ON DELETE CASCADE,
  CONSTRAINT user_progress_current_chapter_check CHECK ((current_chapter >= 0))
);

CREATE TABLE public.votes (
  voting_round_id uuid NOT NULL,
  user_id uuid NOT NULL,
  book_id uuid NOT NULL,
  voted_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT votes_pkey PRIMARY KEY (voting_round_id, user_id),
  CONSTRAINT votes_book_id_fkey FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
  CONSTRAINT votes_user_id_fkey FOREIGN KEY (user_id) REFERENCES profiles(id) ON DELETE CASCADE,
  CONSTRAINT votes_voting_round_id_fkey FOREIGN KEY (voting_round_id) REFERENCES voting_rounds(id) ON DELETE CASCADE
);

CREATE TABLE public.voting_rounds (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  club_id uuid NOT NULL,
  criado_por uuid NOT NULL,
  aberta_em timestamp with time zone NOT NULL DEFAULT now(),
  fecha_em timestamp with time zone NOT NULL,
  n_livros integer NOT NULL DEFAULT 1,
  cadencia voting_cadence NOT NULL DEFAULT 'unica'::voting_cadence,
  status voting_round_status NOT NULL DEFAULT 'aberta'::voting_round_status,
  vencedores jsonb,
  fechada_em timestamp with time zone,
  CONSTRAINT voting_rounds_pkey PRIMARY KEY (id),
  CONSTRAINT voting_rounds_club_id_fkey FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE,
  CONSTRAINT voting_rounds_criado_por_fkey FOREIGN KEY (criado_por) REFERENCES profiles(id) ON DELETE RESTRICT,
  CONSTRAINT voting_rounds_check CHECK ((fecha_em > aberta_em)),
  CONSTRAINT voting_rounds_n_livros_check CHECK (((n_livros >= 1) AND (n_livros <= 12)))
);
