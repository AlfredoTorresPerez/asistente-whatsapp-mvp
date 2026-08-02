import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './app/App'
import { GlobalErrorBoundary } from './components/feedback/GlobalErrorBoundary'
import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <GlobalErrorBoundary>
      <App />
    </GlobalErrorBoundary>
  </StrictMode>,
)
