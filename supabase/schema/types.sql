-- Enums public — Management API 2026-07-12

CREATE TYPE public.club_book_status AS ENUM ('current', 'finished', 'suggested', 'next');
CREATE TYPE public.club_privacy AS ENUM ('convidados', 'publico');
CREATE TYPE public.meeting_status AS ENUM ('agendado', 'concluido', 'cancelado');
CREATE TYPE public.member_role AS ENUM ('member', 'admin', 'super_admin');
CREATE TYPE public.notification_type AS ENUM ('comment_on_chapter', 'next_book_decided', 'meeting_reminder', 'member_finished', 'voting_open', 'voting_closed', 'member_removed', 'promoted_to_admin', 'super_admin_transferred');
CREATE TYPE public.rsvp_status AS ENUM ('vou', 'talvez', 'nao_vou');
CREATE TYPE public.voting_cadence AS ENUM ('unica', 'semanal', 'quinzenal', 'mensal_dia_semana', 'mensal_dia_mes', 'personalizado_dias');
CREATE TYPE public.voting_round_status AS ENUM ('aberta', 'fechada');
