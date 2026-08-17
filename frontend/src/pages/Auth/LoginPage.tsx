const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

export function LoginPage() {
  const oauthError = new URLSearchParams(window.location.search).get('oauth_error')
  const startKakaoLogin = () => {
    window.location.href = `${API_BASE_URL}/oauth2/authorization/kakao`
  }

  return (
    <main className="login-page">
      <div className="login-overlay" />
      <section className="login-copy">
        <div className="brand"><span className="brand-mark">🏃</span><strong>러닝올레</strong></div>
        <h1>제주를 달리며<br />여행하다 🌊</h1>
        <p>런트립의 새로운 시작, 러닝올레</p>
      </section>
      <section className="login-action">
        <h2>시작해볼까요?</h2>
        <p>카카오 계정으로 1초 만에 시작하세요</p>
        <button className="kakao-button" onClick={startKakaoLogin}>
          <span className="kakao-icon">K</span> 카카오 로그인/시작하기
        </button>
        {oauthError && <p className="login-error">{oauthError}</p>}
        <small>로그인 시 이용약관 및 개인정보처리방침에 동의하게 됩니다.</small>
      </section>
    </main>
  )
}
