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
        <div className="brand"><img className="brand-mark" src="/images/running-olle-logo.png" alt="" /><strong>러닝올레</strong></div>
        <h1>제주를 달리며<br />여행하다</h1>
        <p>제주도로 러닝하러 올레?</p>
      </section>
      <section className="login-action">
        <h2>시작해볼까요?</h2>
        <p>카카오 계정으로 1초 만에 시작하세요</p>
        <button className="kakao-button" onClick={startKakaoLogin}>
          <span className="kakao-icon">K</span> 카카오 로그인/시작하기
        </button>
        {oauthError && <p className="login-error">{oauthError}</p>}
        <small>로그인 후 이용약관과 개인정보 수집·이용 내용을 확인하고 동의할 수 있습니다.</small>
      </section>
    </main>
  )
}
