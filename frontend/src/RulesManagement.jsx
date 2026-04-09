import React, { useState, useEffect } from 'react';
import axios from 'axios';
import {
  Upload, FileText, Plus, Trash2, Eye, CheckCircle, 
  XCircle, AlertCircle, Download, RefreshCw, Code
} from 'lucide-react';

function RulesManagement({ rules, onRulesChanged }) {
  const [activeView, setActiveView] = useState('list');
  const [ruleFiles, setRuleFiles] = useState([]);
  const [selectedFile, setSelectedFile] = useState(null);
  const [ruleContent, setRuleContent] = useState('');
  const [ruleName, setRuleName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [validationResult, setValidationResult] = useState(null);

  // Fetch lista file regole
  const fetchRuleFiles = async () => {
    try {
      const response = await axios.get('/api/rules/list');
      setRuleFiles(response.data.files || []);
    } catch (err) {
      console.error('Error fetching rule files:', err);
    }
  };

  useEffect(() => {
    fetchRuleFiles();
  }, []);

  // Upload regola da testo
  const handleUploadRule = async () => {
    if (!ruleName.trim()) {
      setError('Inserisci un nome per la regola');
      return;
    }

    if (!ruleContent.trim()) {
      setError('Inserisci il contenuto della regola');
      return;
    }

    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      const response = await axios.post('/api/rules/upload', {
        ruleName: ruleName,
        ruleContent: ruleContent
      });

      setSuccess(`Regola "${response.data.ruleName}" caricata con successo!${response.data.loadedDynamically ? ' E caricata nel motore CEP!' : ''}`);
      setRuleName('');
      setRuleContent('');
      setValidationResult(null);
      
      // Refresh lista file
      await fetchRuleFiles();
      
      // IMPORTANTE: Triggera refresh delle regole nel parent
      if (onRulesChanged) {
        onRulesChanged();
      }
      
      // Torna alla lista dopo 2 secondi
      setTimeout(() => {
        setActiveView('list');
        setSuccess(null);
      }, 2000);

    } catch (err) {
      setError(err.response?.data?.error || 'Errore durante il caricamento della regola');
    } finally {
      setLoading(false);
    }
  };

  // Validazione regola
  const handleValidateRule = async () => {
    if (!ruleContent.trim()) {
      setError('Inserisci il contenuto della regola da validare');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await axios.post('/api/rules/validate', {
        ruleContent: ruleContent
      });

      setValidationResult(response.data);
    } catch (err) {
      setError(err.response?.data?.error || 'Errore durante la validazione');
    } finally {
      setLoading(false);
    }
  };

  // Upload file
  const handleFileUpload = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    if (!file.name.endsWith('.drl')) {
      setError('Il file deve avere estensione .drl');
      return;
    }

    const reader = new FileReader();
    reader.onload = async (e) => {
      const content = e.target.result;
      setRuleName(file.name.replace('.drl', ''));
      setRuleContent(content);
      setActiveView('editor');
    };
    reader.readAsText(file);
  };

  // Visualizza contenuto file
  const handleViewFile = async (filename) => {
    setLoading(true);
    try {
      const response = await axios.get(`/api/rules/content/${filename}`);
      setSelectedFile({
        name: filename,
        content: response.data.content
      });
    } catch (err) {
      setError(err.response?.data?.error || 'Errore durante la lettura del file');
    } finally {
      setLoading(false);
    }
  };

  // Elimina file
  const handleDeleteFile = async (filename) => {
    if (!confirm(`Sei sicuro di voler eliminare "${filename}"?`)) {
      return;
    }

    setLoading(true);
    setError(null);

    try {
      await axios.delete(`/api/rules/delete/${filename}`);
      setSuccess(`File "${filename}" eliminato con successo`);
      await fetchRuleFiles();
      
      // Triggera refresh
      if (onRulesChanged) {
        onRulesChanged();
      }
      
      setTimeout(() => setSuccess(null), 3000);
    } catch (err) {
      setError(err.response?.data?.error || 'Errore durante l\'eliminazione');
    } finally {
      setLoading(false);
    }
  };

  // Carica file nel motore - CON AUTO-REFRESH
  const handleLoadFile = async (filename) => {
    setLoading(true);
    setError(null);

    try {
      const response = await axios.post(`/api/rules/load/${filename}`);
      setSuccess(`Regola "${filename}" caricata nel motore CEP!`);
      
      // IMPORTANTE: Aspetta un attimo poi refresha le regole
      setTimeout(async () => {
        if (onRulesChanged) {
          await onRulesChanged();
        }
        setSuccess(null);
      }, 1500);
      
    } catch (err) {
      setError(err.response?.data?.error || 'Errore durante il caricamento nel motore');
    } finally {
      setLoading(false);
    }
  };

  // Template regola vuota
  const getEmptyRuleTemplate = () => {
    return `package it.cnr.isti.labsedc.concern.event;

import it.cnr.isti.labsedc.concern.event.ConcernBaseEvent;
import it.cnr.isti.labsedc.concern.utils.KieLauncher;

dialect "java"

declare ConcernBaseEvent
    @role( event )
    @timestamp( timestamp )
end

rule "my-new-rule"
    no-loop
    salience 10
    dialect "java"
    when
        $event: ConcernBaseEvent(
            this.getName == "EventName",
            this.getSenderID == "ProbeID"
        )
    then
        KieLauncher.printer("Rule matched: my-new-rule");
end`;
  };

  return (
    <div className="rules-management">
      {/* Header con azioni */}
      <div className="rules-header">
        <div className="rules-tabs">
          <button
            className={activeView === 'list' ? 'active' : ''}
            onClick={() => setActiveView('list')}
          >
            <FileText size={18} /> Regole Caricate ({rules.length})
          </button>
          <button
            className={activeView === 'files' ? 'active' : ''}
            onClick={() => { setActiveView('files'); fetchRuleFiles(); }}
          >
            <Code size={18} /> File Regole ({ruleFiles.length})
          </button>
          <button
            className={activeView === 'editor' ? 'active' : ''}
            onClick={() => {
              setActiveView('editor');
              if (!ruleContent) setRuleContent(getEmptyRuleTemplate());
            }}
          >
            <Plus size={18} /> Nuova Regola
          </button>
        </div>

        <div className="rules-actions">
          <label className="btn-upload">
            <Upload size={18} />
            <span>Carica File .drl</span>
            <input
              type="file"
              accept=".drl"
              onChange={handleFileUpload}
              style={{ display: 'none' }}
            />
          </label>
          <button 
            onClick={() => {
              fetchRuleFiles();
              if (onRulesChanged) onRulesChanged();
            }} 
            className="btn-refresh-small"
            title="Aggiorna regole"
          >
            <RefreshCw size={18} />
          </button>
        </div>
      </div>

      {/* Messaggi */}
      {error && (
        <div className="message-banner error">
          <XCircle size={18} />
          <span>{error}</span>
          <button onClick={() => setError(null)}>×</button>
        </div>
      )}

      {success && (
        <div className="message-banner success">
          <CheckCircle size={18} />
          <span>{success}</span>
        </div>
      )}

      {/* Contenuto */}
      {activeView === 'list' && (
        <div className="rules-list-view">
          <h3>Regole Attive nel Motore CEP</h3>
          {rules.length > 0 ? (
            <div className="rules-grid">
              {rules.map((rule, index) => (
                <div key={index} className="rule-card">
                  <div className="rule-card-header">
                    <CheckCircle size={18} color="#00C49F" />
                    <span className="rule-name">{rule.name}</span>
                    {rule.enabled && <span className="badge badge-success">Attiva</span>}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state">
              <AlertCircle size={48} />
              <p>Nessuna regola caricata nel motore CEP</p>
              <p className="empty-state-hint">Avvia il monitoring per caricare le regole</p>
            </div>
          )}
        </div>
      )}

      {activeView === 'files' && (
        <div className="files-list-view">
          <h3>File Regole Disponibili</h3>
          {ruleFiles.length > 0 ? (
            <div className="files-table">
              {ruleFiles.map((file, index) => (
                <div key={index} className="file-row">
                  <div className="file-info">
                    <FileText size={20} />
                    <div>
                      <div className="file-name">{file.name}</div>
                      <div className="file-meta">
                        {(file.size / 1024).toFixed(2)} KB • 
                        {new Date(file.lastModified).toLocaleString('it-IT')}
                      </div>
                    </div>
                  </div>
                  <div className="file-actions">
                    <button
                      onClick={() => handleViewFile(file.name)}
                      className="btn-icon"
                      title="Visualizza"
                    >
                      <Eye size={18} />
                    </button>
                    <button
                      onClick={() => handleLoadFile(file.name)}
                      className="btn-icon btn-primary-icon"
                      title="Carica nel motore"
                      disabled={loading}
                    >
                      <Upload size={18} />
                    </button>
                    <button
                      onClick={() => handleDeleteFile(file.name)}
                      className="btn-icon btn-danger"
                      title="Elimina"
                    >
                      <Trash2 size={18} />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state">
              <FileText size={48} />
              <p>Nessun file .drl trovato</p>
              <p className="empty-state-hint">Carica un file o crea una nuova regola</p>
            </div>
          )}
        </div>
      )}

      {activeView === 'editor' && (
        <div className="rule-editor-view">
          <h3>Editor Regola Drools</h3>
          
          <div className="editor-form">
            <div className="form-group">
              <label>Nome Regola</label>
              <input
                type="text"
                className="rule-input"
                placeholder="es: MyNewRule"
                value={ruleName}
                onChange={(e) => setRuleName(e.target.value)}
              />
              <small>Il nome del file sarà: {ruleName || 'MyRule'}.drl</small>
            </div>

            <div className="form-group">
              <label>Contenuto Regola (.drl)</label>
              <textarea
                className="rule-textarea"
                rows={20}
                value={ruleContent}
                onChange={(e) => setRuleContent(e.target.value)}
                placeholder="Inserisci il contenuto della regola Drools..."
                spellCheck={false}
              />
            </div>

            {validationResult && (
              <div className={`validation-result ${validationResult.valid ? 'valid' : 'invalid'}`}>
                {validationResult.valid ? (
                  <>
                    <CheckCircle size={18} />
                    <span>Regola valida!</span>
                  </>
                ) : (
                  <>
                    <XCircle size={18} />
                    <div>
                      <strong>Errori di validazione:</strong>
                      <ul>
                        {validationResult.errors.map((err, i) => (
                          <li key={i}>{err}</li>
                        ))}
                      </ul>
                    </div>
                  </>
                )}
              </div>
            )}

            <div className="editor-actions">
              <button
                onClick={handleValidateRule}
                className="btn-secondary"
                disabled={loading}
              >
                <CheckCircle size={18} /> Valida Regola
              </button>
              <button
                onClick={handleUploadRule}
                className="btn-primary"
                disabled={loading || !ruleName.trim() || !ruleContent.trim()}
              >
                {loading ? (
                  <>
                    <RefreshCw size={18} className="spinning" /> Caricamento...
                  </>
                ) : (
                  <>
                    <Upload size={18} /> Carica Regola
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal visualizzazione file */}
      {selectedFile && (
        <div className="modal-overlay" onClick={() => setSelectedFile(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{selectedFile.name}</h3>
              <button onClick={() => setSelectedFile(null)} className="modal-close">
                ×
              </button>
            </div>
            <div className="modal-body">
              <pre className="code-viewer">{selectedFile.content}</pre>
            </div>
            <div className="modal-footer">
              <button
                onClick={() => {
                  setRuleName(selectedFile.name.replace('.drl', ''));
                  setRuleContent(selectedFile.content);
                  setSelectedFile(null);
                  setActiveView('editor');
                }}
                className="btn-secondary"
              >
                Modifica
              </button>
              <button onClick={() => setSelectedFile(null)} className="btn-primary">
                Chiudi
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default RulesManagement;