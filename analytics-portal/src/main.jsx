import React from 'react';
import ReactDOM from 'react-dom/client';
// Ensure `global` is available for libraries that expect a Node-like global
// (e.g., sockjs-client). Vite doesn't provide `global` by default.
if (typeof window !== 'undefined' && typeof window.global === 'undefined') {
  window.global = window;
}
import App from './App';
import './styles.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
