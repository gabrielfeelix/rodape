-- Snapshot das RLS policies public — gerado da Management API 2026-07-12

CREATE POLICY "book_ratings delete self" ON public.book_ratings AS PERMISSIVE FOR DELETE TO authenticated
  USING ((user_id = ( SELECT auth.uid() AS uid)));

CREATE POLICY "book_ratings insert self in club" ON public.book_ratings AS PERMISSIVE FOR INSERT TO authenticated
  WITH CHECK (((user_id = ( SELECT auth.uid() AS uid)) AND ( SELECT is_club_member(book_ratings.club_id) AS is_club_member)));

CREATE POLICY "book_ratings select members" ON public.book_ratings AS PERMISSIVE FOR SELECT TO authenticated
  USING (( SELECT is_club_member(book_ratings.club_id) AS is_club_member));

CREATE POLICY "book_ratings update self" ON public.book_ratings AS PERMISSIVE FOR UPDATE TO authenticated
  USING ((user_id = ( SELECT auth.uid() AS uid)))
  WITH CHECK ((user_id = ( SELECT auth.uid() AS uid)));

CREATE POLICY "book_suggestions delete self or admin" ON public.book_suggestions AS PERMISSIVE FOR DELETE TO authenticated
  USING (((sugerido_por = ( SELECT auth.uid() AS uid)) OR ( SELECT is_club_admin(book_suggestions.club_id) AS is_club_admin)));

CREATE POLICY "book_suggestions insert members" ON public.book_suggestions AS PERMISSIVE FOR INSERT TO authenticated
  WITH CHECK ((( SELECT is_club_member(book_suggestions.club_id) AS is_club_member) AND (sugerido_por = ( SELECT auth.uid() AS uid))));

CREATE POLICY "book_suggestions select members" ON public.book_suggestions AS PERMISSIVE FOR SELECT TO authenticated
  USING (( SELECT is_club_member(book_suggestions.club_id) AS is_club_member));

CREATE POLICY "book_suggestions update self" ON public.book_suggestions AS PERMISSIVE FOR UPDATE TO authenticated
  USING ((sugerido_por = ( SELECT auth.uid() AS uid)))
  WITH CHECK ((sugerido_por = ( SELECT auth.uid() AS uid)));

CREATE POLICY "book_summaries insert members" ON public.book_summaries AS PERMISSIVE FOR INSERT TO authenticated
  WITH CHECK ((( SELECT is_club_member(book_summaries.club_id) AS is_club_member) AND (last_editor_id = ( SELECT auth.uid() AS uid))));

CREATE POLICY "book_summaries select members" ON public.book_summaries AS PERMISSIVE FOR SELECT TO authenticated
  USING (( SELECT is_club_member(book_summaries.club_id) AS is_club_member));

CREATE POLICY "book_summaries update members" ON public.book_summaries AS PERMISSIVE FOR UPDATE TO authenticated
  USING (( SELECT is_club_member(book_summaries.club_id) AS is_club_member))
  WITH CHECK ((( SELECT is_club_member(book_summaries.club_id) AS is_club_member) AND (last_editor_id = ( SELECT auth.uid() AS uid))));

CREATE POLICY "books insert authenticated" ON public.books AS PERMISSIVE FOR INSERT TO authenticated
  WITH CHECK (((created_by = ( SELECT auth.uid() AS uid)) OR (created_by IS NULL)));

CREATE POLICY "books select authenticated" ON public.books AS PERMISSIVE FOR SELECT TO authenticated
  USING (true);

CREATE POLICY "books update creator manual" ON public.books AS PERMISSIVE FOR UPDATE TO authenticated
  USING (((is_manual = true) AND (created_by = ( SELECT auth.uid() AS uid))))
  WITH CHECK (((is_manual = true) AND (created_by = ( SELECT auth.uid() AS uid))));

CREATE POLICY "chapters delete admin" ON public.chapters AS PERMISSIVE FOR DELETE TO authenticated
  USING ((EXISTS ( SELECT 1
   FROM club_books cb
  WHERE ((cb.book_id = chapters.book_id) AND ( SELECT is_club_admin(cb.club_id) AS is_club_admin)))));

CREATE POLICY "chapters insert admin" ON public.chapters AS PERMISSIVE FOR INSERT TO authenticated
  WITH CHECK ((EXISTS ( SELECT 1
   FROM club_books cb
  WHERE ((cb.book_id = chapters.book_id) AND ( SELECT is_club_admin(cb.club_id) AS is_club_admin)))));

CREATE POLICY "chapters select authenticated" ON public.chapters AS PERMISSIVE FOR SELECT TO authenticated
  USING (true);

CREATE POLICY "chapters update admin" ON public.chapters AS PERMISSIVE FOR UPDATE TO authenticated
  USING ((EXISTS ( SELECT 1
   FROM club_books cb
  WHERE ((cb.book_id = chapters.book_id) AND ( SELECT is_club_admin(cb.club_id) AS is_club_admin)))));

CREATE POLICY "club_books delete admin" ON public.club_books AS PERMISSIVE FOR DELETE TO authenticated
  USING (( SELECT is_club_admin(club_books.club_id) AS is_club_admin));

CREATE POLICY "club_books insert admin" ON public.club_books AS PERMISSIVE FOR INSERT TO authenticated
  WITH CHECK (( SELECT is_club_admin(club_books.club_id) AS is_club_admin));

CREATE POLICY "club_books select members" ON public.club_books AS PERMISSIVE FOR SELECT TO authenticated
  USING (( SELECT is_club_member(club_books.club_id) AS is_club_member));

CREATE POLICY "club_books update admin" ON public.club_books AS PERMISSIVE FOR UPDATE TO authenticated
  USING (( SELECT is_club_admin(club_books.club_id) AS is_club_admin))
  WITH CHECK (( SELECT is_club_admin(club_books.club_id) AS is_club_admin));

CREATE POLICY "club_members delete self or super" ON public.club_members AS PERMISSIVE FOR DELETE TO authenticated
  USING (((user_id = ( SELECT auth.uid() AS uid)) OR ( SELECT is_club_super(club_members.club_id) AS is_club_super)));

CREATE POLICY "club_members select members" ON public.club_members AS PERMISSIVE FOR SELECT TO authenticated
  USING (( SELECT is_club_member(club_members.club_id) AS is_club_member));

CREATE POLICY "club_members update admin" ON public.club_members AS PERMISSIVE FOR UPDATE TO authenticated
  USING (( SELECT is_club_admin(club_members.club_id) AS is_club_admin))
  WITH CHECK (( SELECT is_club_admin(club_members.club_id) AS is_club_admin));

CREATE POLICY "clubs delete super" ON public.clubs AS PERMISSIVE FOR DELETE TO authenticated
  USING (( SELECT is_club_super(clubs.id) AS is_club_super));

CREATE POLICY "clubs insert authenticated" ON public.clubs AS PERMISSIVE FOR INSERT TO authenticated
  WITH CHECK ((criador_id = ( SELECT auth.uid() AS uid)));

CREATE POLICY "clubs select members or public" ON public.clubs AS PERMISSIVE FOR SELECT TO authenticated
  USING (((privacidade = 'publico'::club_privacy) OR ( SELECT is_club_member(clubs.id) AS is_club_member)));

CREATE POLICY "clubs update admin" ON public.clubs AS PERMISSIVE FOR UPDATE TO authenticated
  USING (( SELECT is_club_admin(clubs.id) AS is_club_admin))
  WITH CHECK (( SELECT is_club_admin(clubs.id) AS is_club_admin));

CREATE POLICY "comments insert members" ON public.comments AS PERMISSIVE FOR INSERT TO authenticated
  WITH CHECK (((user_id = ( SELECT auth.uid() AS uid)) AND ( SELECT is_club_member(comments.club_id) AS is_club_member) AND (removido = false)));

CREATE POLICY "comments select members" ON public.comments AS PERMISSIVE FOR SELECT TO authenticated
  USING (( SELECT is_club_member(comments.club_id) AS is_club_member));

CREATE POLICY "comments update author or admin" ON public.comments AS PERMISSIVE FOR UPDATE TO authenticated
  USING ((((user_id = ( SELECT auth.uid() AS uid)) AND (NOT removido)) OR ( SELECT is_club_admin(comments.club_id) AS is_club_admin)))
  WITH CHECK ((((user_id = ( SELECT auth.uid() AS uid)) AND (NOT removido)) OR ( SELECT is_club_admin(comments.club_id) AS is_club_admin)));

CREATE POLICY "meeting_minutes insert admin" ON public.meeting_minutes AS PERMISSIVE FOR INSERT TO authenticated
  WITH CHECK (((EXISTS ( SELECT 1
   FROM meetings m
  WHERE ((m.id = meeting_minutes.meeting_id) AND ( SELECT is_club_admin(m.club_id) AS is_club_admin)))) AND (last_editor_id = ( SELECT auth.uid() AS uid))));

CREATE POLICY "meeting_minutes select members" ON public.meeting_minutes AS PERMISSIVE FOR SELECT TO authenticated
  USING ((EXISTS ( SELECT 1
   FROM meetings m
  WHERE ((m.id = meeting_minutes.meeting_id) AND ( SELECT is_club_member(m.club_id) AS is_club_member)))));

CREATE POLICY "meeting_minutes update admin" ON public.meeting_minutes AS PERMISSIVE FOR UPDATE TO authenticated
  USING ((EXISTS ( SELECT 1
   FROM meetings m
  WHERE ((m.id = meeting_minutes.meeting_id) AND ( SELECT is_club_admin(m.club_id) AS is_club_admin)))))
  WITH CHECK (((last_editor_id = ( SELECT auth.uid() AS uid)) AND (EXISTS ( SELECT 1
   FROM meetings m
  WHERE ((m.id = meeting_minutes.meeting_id) AND ( SELECT is_club_admin(m.club_id) AS is_club_admin))))));

CREATE POLICY "meeting_notes delete self" ON public.meeting_notes AS PERMISSIVE FOR DELETE TO authenticated
  USING ((user_id = ( SELECT auth.uid() AS uid)));

CREATE POLICY "meeting_notes insert self" ON public.meeting_notes AS PERMISSIVE FOR INSERT TO authenticated
  WITH CHECK ((user_id = ( SELECT auth.uid() AS uid)));

CREATE POLICY "meeting_notes select self" ON public.meeting_notes AS PERMISSIVE FOR SELECT TO authenticated
  USING ((user_id = ( SELECT auth.uid() AS uid)));

CREATE POLICY "meeting_notes update self" ON public.meeting_notes AS PERMISSIVE FOR UPDATE TO authenticated
  USING ((user_id = ( SELECT auth.uid() AS uid)))
  WITH CHECK ((user_id = ( SELECT auth.uid() AS uid)));

CREATE POLICY "meeting_patterns delete admin" ON public.meeting_patterns AS PERMISSIVE FOR DELETE TO authenticated
  USING (( SELECT is_club_admin(meeting_patterns.club_id) AS is_club_admin));

CREATE POLICY "meeting_patterns insert admin" ON public.meeting_patterns AS PERMISSIVE FOR INSERT TO authenticated
  WITH CHECK (( SELECT is_club_admin(meeting_patterns.club_id) AS is_club_admin));

CREATE POLICY "meeting_patterns select members" ON public.meeting_patterns AS PERMISSIVE FOR SELECT TO authenticated
  USING (( SELECT is_club_member(meeting_patterns.club_id) AS is_club_member));

CREATE POLICY "meeting_patterns update admin" ON public.meeting_patterns AS PERMISSIVE FOR UPDATE TO authenticated
  USING (( SELECT is_club_admin(meeting_patterns.club_id) AS is_club_admin))
  WITH CHECK (( SELECT is_club_admin(meeting_patterns.club_id) AS is_club_admin));

CREATE POLICY "meeting_rsvps delete self" ON public.meeting_rsvps AS PERMISSIVE FOR DELETE TO authenticated
  USING ((user_id = ( SELECT auth.uid() AS uid)));

CREATE POLICY "meeting_rsvps insert self in club" ON public.meeting_rsvps AS PERMISSIVE FOR INSERT TO authenticated
  WITH CHECK (((user_id = ( SELECT auth.uid() AS uid)) AND (EXISTS ( SELECT 1
   FROM meetings m
  WHERE ((m.id = meeting_rsvps.meeting_id) AND ( SELECT is_club_member(m.club_id) AS is_club_member))))));

CREATE POLICY "meeting_rsvps select members" ON public.meeting_rsvps AS PERMISSIVE FOR SELECT TO authenticated
  USING ((EXISTS ( SELECT 1
   FROM meetings m
  WHERE ((m.id = meeting_rsvps.meeting_id) AND ( SELECT is_club_member(m.club_id) AS is_club_member)))));

CREATE POLICY "meeting_rsvps update self" ON public.meeting_rsvps AS PERMISSIVE FOR UPDATE TO authenticated
  USING ((user_id = ( SELECT auth.uid() AS uid)))
  WITH CHECK ((user_id = ( SELECT auth.uid() AS uid)));

CREATE POLICY "meetings delete admin" ON public.meetings AS PERMISSIVE FOR DELETE TO authenticated
  USING (( SELECT is_club_admin(meetings.club_id) AS is_club_admin));

CREATE POLICY "meetings insert admin" ON public.meetings AS PERMISSIVE FOR INSERT TO authenticated
  WITH CHECK (( SELECT is_club_admin(meetings.club_id) AS is_club_admin));

CREATE POLICY "meetings select members" ON public.meetings AS PERMISSIVE FOR SELECT TO authenticated
  USING (( SELECT is_club_member(meetings.club_id) AS is_club_member));

CREATE POLICY "meetings update admin" ON public.meetings AS PERMISSIVE FOR UPDATE TO authenticated
  USING (( SELECT is_club_admin(meetings.club_id) AS is_club_admin))
  WITH CHECK (( SELECT is_club_admin(meetings.club_id) AS is_club_admin));

CREATE POLICY "member_removals select admin" ON public.member_removals AS PERMISSIVE FOR SELECT TO authenticated
  USING (( SELECT is_club_admin(member_removals.club_id) AS is_club_admin));

CREATE POLICY "notifications delete self" ON public.notifications AS PERMISSIVE FOR DELETE TO authenticated
  USING ((user_id = ( SELECT auth.uid() AS uid)));

CREATE POLICY "notifications select self" ON public.notifications AS PERMISSIVE FOR SELECT TO authenticated
  USING ((user_id = ( SELECT auth.uid() AS uid)));

CREATE POLICY "notifications update self mark read" ON public.notifications AS PERMISSIVE FOR UPDATE TO authenticated
  USING ((user_id = ( SELECT auth.uid() AS uid)))
  WITH CHECK ((user_id = ( SELECT auth.uid() AS uid)));

CREATE POLICY "profiles insert self" ON public.profiles AS PERMISSIVE FOR INSERT TO authenticated
  WITH CHECK ((id = ( SELECT auth.uid() AS uid)));

CREATE POLICY "profiles select authenticated" ON public.profiles AS PERMISSIVE FOR SELECT TO authenticated
  USING (true);

CREATE POLICY "profiles update self" ON public.profiles AS PERMISSIVE FOR UPDATE TO authenticated
  USING ((id = ( SELECT auth.uid() AS uid)))
  WITH CHECK ((id = ( SELECT auth.uid() AS uid)));

CREATE POLICY "reactions delete self" ON public.reactions AS PERMISSIVE FOR DELETE TO authenticated
  USING ((user_id = ( SELECT auth.uid() AS uid)));

CREATE POLICY "reactions insert self in club" ON public.reactions AS PERMISSIVE FOR INSERT TO authenticated
  WITH CHECK (((user_id = ( SELECT auth.uid() AS uid)) AND (EXISTS ( SELECT 1
   FROM comments c
  WHERE ((c.id = reactions.comment_id) AND ( SELECT is_club_member(c.club_id) AS is_club_member))))));

CREATE POLICY "reactions select members" ON public.reactions AS PERMISSIVE FOR SELECT TO authenticated
  USING ((EXISTS ( SELECT 1
   FROM comments c
  WHERE ((c.id = reactions.comment_id) AND ( SELECT is_club_member(c.club_id) AS is_club_member)))));

CREATE POLICY "saved_quotes delete self" ON public.saved_quotes AS PERMISSIVE FOR DELETE TO authenticated
  USING ((user_id = ( SELECT auth.uid() AS uid)));

CREATE POLICY "saved_quotes insert self in club" ON public.saved_quotes AS PERMISSIVE FOR INSERT TO authenticated
  WITH CHECK (((user_id = ( SELECT auth.uid() AS uid)) AND ( SELECT is_club_member(saved_quotes.club_id) AS is_club_member)));

CREATE POLICY "saved_quotes select members" ON public.saved_quotes AS PERMISSIVE FOR SELECT TO authenticated
  USING (( SELECT is_club_member(saved_quotes.club_id) AS is_club_member));

CREATE POLICY "saved_quotes update self" ON public.saved_quotes AS PERMISSIVE FOR UPDATE TO authenticated
  USING ((user_id = ( SELECT auth.uid() AS uid)))
  WITH CHECK ((user_id = ( SELECT auth.uid() AS uid)));

CREATE POLICY "user_progress delete self" ON public.user_progress AS PERMISSIVE FOR DELETE TO authenticated
  USING ((user_id = ( SELECT auth.uid() AS uid)));

CREATE POLICY "user_progress insert self" ON public.user_progress AS PERMISSIVE FOR INSERT TO authenticated
  WITH CHECK (((user_id = ( SELECT auth.uid() AS uid)) AND ( SELECT is_club_member(user_progress.club_id) AS is_club_member)));

CREATE POLICY "user_progress select self" ON public.user_progress AS PERMISSIVE FOR SELECT TO authenticated
  USING ((user_id = ( SELECT auth.uid() AS uid)));

CREATE POLICY "user_progress update self" ON public.user_progress AS PERMISSIVE FOR UPDATE TO authenticated
  USING ((user_id = ( SELECT auth.uid() AS uid)))
  WITH CHECK ((user_id = ( SELECT auth.uid() AS uid)));

CREATE POLICY "votes delete self in open round" ON public.votes AS PERMISSIVE FOR DELETE TO authenticated
  USING (((user_id = ( SELECT auth.uid() AS uid)) AND (EXISTS ( SELECT 1
   FROM voting_rounds vr
  WHERE ((vr.id = votes.voting_round_id) AND (vr.status = 'aberta'::voting_round_status))))));

CREATE POLICY "votes insert self in open round" ON public.votes AS PERMISSIVE FOR INSERT TO authenticated
  WITH CHECK (((user_id = ( SELECT auth.uid() AS uid)) AND (EXISTS ( SELECT 1
   FROM voting_rounds vr
  WHERE ((vr.id = votes.voting_round_id) AND (vr.status = 'aberta'::voting_round_status) AND ( SELECT is_club_member(vr.club_id) AS is_club_member))))));

CREATE POLICY "votes select members" ON public.votes AS PERMISSIVE FOR SELECT TO authenticated
  USING ((EXISTS ( SELECT 1
   FROM voting_rounds vr
  WHERE ((vr.id = votes.voting_round_id) AND ( SELECT is_club_member(vr.club_id) AS is_club_member)))));

CREATE POLICY "votes update self in open round" ON public.votes AS PERMISSIVE FOR UPDATE TO authenticated
  USING (((user_id = ( SELECT auth.uid() AS uid)) AND (EXISTS ( SELECT 1
   FROM voting_rounds vr
  WHERE ((vr.id = votes.voting_round_id) AND (vr.status = 'aberta'::voting_round_status))))))
  WITH CHECK (((user_id = ( SELECT auth.uid() AS uid)) AND (EXISTS ( SELECT 1
   FROM voting_rounds vr
  WHERE ((vr.id = votes.voting_round_id) AND (vr.status = 'aberta'::voting_round_status))))));

CREATE POLICY "voting_rounds delete admin" ON public.voting_rounds AS PERMISSIVE FOR DELETE TO authenticated
  USING (( SELECT is_club_admin(voting_rounds.club_id) AS is_club_admin));

CREATE POLICY "voting_rounds insert admin" ON public.voting_rounds AS PERMISSIVE FOR INSERT TO authenticated
  WITH CHECK ((( SELECT is_club_admin(voting_rounds.club_id) AS is_club_admin) AND (criado_por = ( SELECT auth.uid() AS uid))));

CREATE POLICY "voting_rounds select members" ON public.voting_rounds AS PERMISSIVE FOR SELECT TO authenticated
  USING (( SELECT is_club_member(voting_rounds.club_id) AS is_club_member));

CREATE POLICY "voting_rounds update admin" ON public.voting_rounds AS PERMISSIVE FOR UPDATE TO authenticated
  USING (( SELECT is_club_admin(voting_rounds.club_id) AS is_club_admin))
  WITH CHECK (( SELECT is_club_admin(voting_rounds.club_id) AS is_club_admin));;
