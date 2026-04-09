import React from 'react';
import './ValidationIndicator.css';

/**
 * Componente per mostrare i risultati della validazione
 */
export function ValidationIndicator({ validation }) {
  if (validation.isValidating) {
    return (
      <div className="validation-indicator validating">
        <div className="validation-spinner"></div>
        <span>Validazione in corso...</span>
      </div>
    );
  }

  if (validation.isValid === null) {
    return null;
  }

  if (validation.isValid) {
    return (
      <div className="validation-indicator valid">
        <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
          <circle cx="9" cy="9" r="9" fill="#10b981"/>
          <path d="M5 9l2.5 2.5L13 6" stroke="white" strokeWidth="2" strokeLinecap="round"/>
        </svg>
        <span>Sintassi valida</span>
      </div>
    );
  }

  return (
    <div className="validation-indicator invalid">
      <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
        <circle cx="9" cy="9" r="9" fill="#ef4444"/>
        <path d="M6 6l6 6M12 6l-6 6" stroke="white" strokeWidth="2" strokeLinecap="round"/>
      </svg>
      <div className="validation-errors">
        <span className="error-count">{validation.errors.length} errori</span>
        <ul className="error-list">
          {validation.errors.map((error, i) => (
            <li key={i}>{error}</li>
          ))}
        </ul>
      </div>
    </div>
  );
}
