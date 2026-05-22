// app.jsx — Tramabook interactive prototype shell

function TramabookApp({ startScreen = 'main', startTab = 'home', startNextTab = 'meeting', startBookTitle, clipBg, accent }) {
  // navigation state
  const [screen, setScreen] = React.useState(startScreen);
  const [tab, setTab] = React.useState(startTab);
  const [nextTab, setNextTab] = React.useState(startNextTab);
  const [chapter, setChapter] = React.useState(null);
  const [bookTitle, setBookTitle] = React.useState(startBookTitle || 'Olhos d\'água');
  const [clubSheet, setClubSheet] = React.useState(false);
  const [progressSheet, setProgressSheet] = React.useState(false);
  const [userChapter, setUserChapter] = React.useState(8);
  const [theme, setTheme] = React.useState('system');
  const [toast, setToast] = React.useState(null);
  const [unread, setUnread] = React.useState(3);

  // toast helper
  const showToast = (text) => {
    setToast(text);
    setTimeout(() => setToast(null), 2200);
  };

  const goTo = (s, payload) => {
    if (s === 'chapter' && payload?.chapter) {
      setChapter(payload.chapter);
      setScreen('chapter');
      return;
    }
    if (s === 'book-detail' && payload?.title) {
      setBookTitle(payload.title);
      setScreen('book-detail');
      return;
    }
    if (s === 'home' || s === 'book' || s === 'next' || s === 'profile') {
      setTab(s === 'book' ? 'book' : s === 'next' ? 'next' : s === 'profile' ? 'profile' : 'home');
      setScreen('main');
      return;
    }
    if (s === 'next-meeting') {
      setTab('next'); setNextTab('meeting'); setScreen('main');
      return;
    }
    if (s === 'next-shelf') {
      setTab('next'); setNextTab('shelf'); setScreen('main');
      return;
    }
    setScreen(s);
  };

  const onboardingScreens = ['welcome', 'login', 'signup', 'create-club', 'join-club'];
  const inOnboarding = onboardingScreens.includes(screen);

  // build active club info
  const club = TB_DATA.clubs[0];
  const user = TB_DATA.user;

  // book override for userChapter
  React.useEffect(() => {
    TB_DATA.currentBook.userChapter = userChapter;
  }, [userChapter]);

  const renderMain = () => {
    return (
      <>
        <StatusBar />
        <GlobalHeader
          club={club} user={user}
          onAvatar={() => setTab('profile')}
          onClubTap={() => setClubSheet(true)}
          onBell={() => { setScreen('notifications'); setUnread(0); }}
          unread={unread}
        />
        <div style={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          {tab === 'home'    && <HomeScreen goTo={goTo} openClubSheet={() => setClubSheet(true)} />}
          {tab === 'book'    && <CurrentBookScreen goTo={goTo} openProgressSheet={() => setProgressSheet(true)} />}
          {tab === 'next'    && <NextScreen subTab={nextTab} setSubTab={setNextTab} goTo={goTo} />}
          {tab === 'profile' && <ProfileScreen goTo={goTo} theme={theme} setTheme={setTheme} />}
        </div>
        <BottomNav tab={tab} setTab={setTab} />
      </>
    );
  };

  return (
    <Phone bg={clipBg || TB.paper}>
      <div style={{ width: '100%', height: '100%', display: 'flex', flexDirection: 'column', background: TB.paper, position: 'relative' }}>
        {screen === 'welcome' && <WelcomeScreen onCreate={() => setScreen('signup-create')} onJoin={() => setScreen('signup-join')} onLogin={() => setScreen('login')} />}
        {screen === 'login' && <LoginScreen defaultMode="login" onBack={() => setScreen('welcome')} onSubmit={() => setScreen('main')} />}
        {screen === 'signup-create' && <LoginScreen defaultMode="signup" onBack={() => setScreen('welcome')} onSubmit={() => setScreen('create-club')} />}
        {screen === 'signup-join' && <LoginScreen defaultMode="signup" onBack={() => setScreen('welcome')} onSubmit={() => setScreen('join-club')} />}
        {screen === 'create-club' && <CreateClubScreen onBack={() => setScreen('welcome')} onCreate={() => { setScreen('main'); showToast('Clube criado.'); }} />}
        {screen === 'join-club' && <JoinClubScreen onBack={() => setScreen('welcome')} onJoin={() => { setScreen('main'); showToast('Entrou no clube.'); }} />}

        {screen === 'main' && renderMain()}

        {screen === 'chapter' && chapter && (
          <ChapterScreen chapter={chapter} onBack={() => setScreen('main')} />
        )}

        {screen === 'suggest' && (
          <SuggestBookScreen onBack={() => { setScreen('main'); setTab('next'); setNextTab('voting'); }}
            onAdd={() => { setScreen('main'); setTab('next'); setNextTab('voting'); showToast('Sugestão na votação.'); }} />
        )}

        {screen === 'edit-profile' && (
          <EditProfileScreen onBack={() => setScreen('main')} onSave={() => { setScreen('main'); showToast('Perfil salvo.'); }} />
        )}

        {screen === 'notifications' && (
          <NotificationsScreen onBack={() => setScreen('main')} />
        )}

        {screen === 'book-detail' && (
          <BookDetailScreen title={bookTitle} onBack={() => { setScreen('main'); setTab('next'); setNextTab('shelf'); }} />
        )}

        {screen === 'frases' && (
          <FrasesScreen onBack={() => setScreen('main')} />
        )}

        {/* Sheets — only relevant in main */}
        <ClubSheet open={clubSheet} onClose={() => setClubSheet(false)}
          onSelect={() => {}} onJoin={() => { setClubSheet(false); setScreen('join-club'); }} />
        <ProgressSheet open={progressSheet} onClose={() => setProgressSheet(false)}
          current={userChapter} total={TB_DATA.currentBook.chapters}
          onSet={setUserChapter} doneToast={showToast} />

        {/* Toast */}
        <Toast text={toast} visible={!!toast} />
      </div>
    </Phone>
  );
}

window.TramabookApp = TramabookApp;
