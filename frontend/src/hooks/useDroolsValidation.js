import { useState, useEffect, useRef } from 'react';
import axios from 'axios';

/**
 * Hook per validazione real-time della sintassi Drools
 * Valida mentre l'utente scrive, con debounce per evitare troppe chiamate
 */
export function useDroolsValidation(ruleContent, debounceMs = 1000) {
  const [validation, setValidation] = useState({
    isValidating: false,
    isValid: null,
    errors: [],
    warnings: []
  });

  const timeoutRef = useRef(null);

  useEffect(() => {
    // Non validare se il contenuto è vuoto
    if (!ruleContent || ruleContent.trim().length === 0) {
      setValidation({
        isValidating: false,
        isValid: null,
        errors: [],
        warnings: []
      });
      return;
    }

    // Cancella il timeout precedente
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }

    // Imposta lo stato di validazione in corso
    setValidation(prev => ({ ...prev, isValidating: true }));

    // Avvia nuovo timeout per validazione
    timeoutRef.current = setTimeout(async () => {
      try {
        const response = await axios.post('/api/rules/validate', {
          ruleContent: ruleContent
        });

        setValidation({
          isValidating: false,
          isValid: response.data.valid,
          errors: response.data.errors || [],
          warnings: response.data.warnings || []
        });
      } catch (error) {
        console.error('Validation error:', error);
        setValidation({
          isValidating: false,
          isValid: false,
          errors: ['Errore durante la validazione'],
          warnings: []
        });
      }
    }, debounceMs);

    // Cleanup
    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
    };
  }, [ruleContent, debounceMs]);

  return validation;
}

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

/**
 * CSS per ValidationIndicator
 * Aggiungi questo in RulesManagement.css o in un file separato
 */
export const validationStyles = `
.validation-indicator {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  border-radius: 0.5rem;
  margin-top: 0.75rem;
  animation: slideDown 0.3s ease;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.validation-indicator.validating {
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.3);
  color: #93c5fd;
}

.validation-indicator.valid {
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.3);
  color: #6ee7b7;
}

.validation-indicator.invalid {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: #fca5a5;
  flex-direction: column;
  align-items: stretch;
}

.validation-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(59, 130, 246, 0.3);
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.validation-errors {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  width: 100%;
}

.error-count {
  font-weight: 600;
  font-size: 0.875rem;
}

.error-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.error-list li {
  font-size: 0.875rem;
  padding-left: 1.5rem;
  position: relative;
}

.error-list li::before {
  content: "•";
  position: absolute;
  left: 0.5rem;
}
`;
