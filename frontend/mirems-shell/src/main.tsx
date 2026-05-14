import React from 'react';
import { createRoot } from 'react-dom/client';

function App() {
  return <main>MiREMS Platform</main>;
}

const root = document.getElementById('root');

if (!root) {
  throw new Error('Root element #root was not found.');
}

createRoot(root).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
