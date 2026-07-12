-- Snapshot dos triggers public — gerado da Management API 2026-07-12

CREATE TRIGGER book_ratings_set_updated_at BEFORE UPDATE ON public.book_ratings FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER book_summaries_set_updated_at BEFORE UPDATE ON public.book_summaries FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER club_books_set_updated_at BEFORE UPDATE ON public.club_books FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE CONSTRAINT TRIGGER enforce_super_admin_invariant_trg AFTER INSERT OR DELETE OR UPDATE OF papel ON public.club_members DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION enforce_super_admin_invariant();
CREATE TRIGGER clubs_set_updated_at BEFORE UPDATE ON public.clubs FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER meeting_minutes_set_updated_at BEFORE UPDATE ON public.meeting_minutes FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER meeting_notes_set_updated_at BEFORE UPDATE ON public.meeting_notes FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER meeting_patterns_set_updated_at BEFORE UPDATE ON public.meeting_patterns FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER meeting_rsvps_set_updated_at BEFORE UPDATE ON public.meeting_rsvps FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER meetings_set_updated_at BEFORE UPDATE ON public.meetings FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER profiles_set_updated_at BEFORE UPDATE ON public.profiles FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER user_progress_set_updated_at BEFORE UPDATE ON public.user_progress FOR EACH ROW EXECUTE FUNCTION set_updated_at();
