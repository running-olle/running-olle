import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import './styles/global.css'
import './styles/course.css'
import './styles/running.css'
import './styles/tourism-events.css'

type LockableScreenOrientation = ScreenOrientation & {
  lock?: (orientation: 'portrait-primary') => Promise<void>
}

function lockInstalledPwaOrientation() {
  const isStandalone = window.matchMedia('(display-mode: standalone)').matches
    || Boolean((navigator as Navigator & { standalone?: boolean }).standalone)
  if (!isStandalone) return

  const orientation = screen.orientation as LockableScreenOrientation | undefined
  void orientation?.lock?.('portrait-primary').catch(() => {
    // Some browsers rely solely on the web app manifest or require a user gesture.
  })
}

lockInstalledPwaOrientation()
document.addEventListener('visibilitychange', () => {
  if (document.visibilityState === 'visible') lockInstalledPwaOrientation()
})

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </React.StrictMode>,
)
