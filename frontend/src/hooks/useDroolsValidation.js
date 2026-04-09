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
