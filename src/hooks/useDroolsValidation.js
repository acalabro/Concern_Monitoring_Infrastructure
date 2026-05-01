import { useState, useEffect, useRef } from 'react';
import axios from 'axios';

/**
 * Hook for real-time Drools syntax validation.
 * Validates as the user types, with debounce to avoid excessive API calls.
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
    // Skip validation if content is empty
    if (!ruleContent || ruleContent.trim().length === 0) {
      setValidation({
        isValidating: false,
        isValid: null,
        errors: [],
        warnings: []
      });
      return;
    }

    // Cancel previous timeout
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }

    // Set validating state
    setValidation(prev => ({ ...prev, isValidating: true }));

    // Start new validation timeout
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
          errors: ['Error during validation'],
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
