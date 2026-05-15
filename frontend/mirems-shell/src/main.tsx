import React from 'react';
import { createRoot } from 'react-dom/client';
import { colors } from '@mirems/ui-core';
import { AppRouter } from './router';
import './styles.css';

const root = document.getElementById('root');

if (!root) {
  throw new Error('Root element #root was not found.');
}

createRoot(root).render(
  <React.StrictMode>
    <AppRouter />
  </React.StrictMode>,
);

void colors;
