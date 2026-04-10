import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import { useDroolsValidation } from './hooks/useDroolsValidation';
import { ValidationIndicator } from './components/ValidationIndicator';
import {
  Upload, FileText, Plus, Trash2, Eye, CheckCircle, 
  XCircle, AlertCircle, Download, RefreshCw, Code,
  ToggleLeft, ToggleRight, Power
} from 'lucide-react';

function RulesManagement({ rules, onRulesChanged }) {
  const [activeView, setActiveView] = useState('files');
  const [ruleFiles, setRuleFiles] = useState([]);
  const [selectedFile, setSelectedFile] = useState(null);
  const [ruleContent, setRuleContent] = useState('');
  const [ruleName, setRuleName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [validationResult, setValidationResult] = useState(null);
  const [togglingRule, setTogglingRule] = useState(null);
  const [deletingFile, setDeletingFile] = useState(null);

  // Nomi regole attive nel motore (derivato da props)
  const activeRuleNames = (rules || []).map(r => r.name);

  // Fetch lista file regole
  const fetchRuleFiles = async () => {
    try {
      const response = await axios.get('/api/rules/files');
      setRuleFiles(response.data.files || []);
    } catch (err) {
      console.error('Error fetching rule files:', err);
    }
  };

  useEffect(() => {
    fetchRuleFiles();
  }, []);

  /**
   * Estrae i nomi delle regole dal contenuto di un file .drl
   */
  const extractRuleNamesFromContent = (content) => {
    const matches = [];
    const regex = /rule\s+["']([^"']+)["']/g;
    let match;
    while ((match = regex.exec(content)) !== null) {
      matches.push(match[1]);
    }
    return matches;
  };

  /**
   * Mappa file -> nomi regole contenute (cache locale)
   */
  const [fileRuleMap, setFileRuleMap] = useState({});

  const buildFileRuleMap = useCallback(async () => {
    const newMap = {};
    for (const file of ruleFiles) {
      try {
        const response = await axios.get(`/api/rules/files/${file.name}`);
        newMap[file.name] = extractRuleNamesFromContent(response.data.content);
      } catch (err) {
        newMap[file.name] = [];
      }
    }
    setFileRuleMap(newMap);
  }, [ruleFiles]);

  useEffect(() => {
    if (ruleFiles.length > 0) {
      buildFileRuleMap();
    }
  }, [ruleFiles, buildFileRuleMap]);

  /**
   * Verifica se le regole di un file sono caricate nel motore
   * Ritorna 'active' | 'partial' | 'inactive'
   */
  const getFileStatus = useCallback((filename) => {
    const rulesInFile = fileRuleMap[filename] || [];
    if (rulesInFile.length === 0) {
      // Fallback: confronto per nome file
      const baseName = filename.replace('.drl', '');
      return activeRuleNames.some(rn =>
        rn === baseName || rn.toLowerCase() === baseName.toLowerCase()
      ) ? 'active' : 'inactive';
    }
    const activeCount = rulesInFile.filter(rn => activeRuleNames.includes(rn)).length;
    if (activeCount === rulesInFile.length) return 'active';
    if (activeCount > 0) return 'partial';
    return 'inactive';
  }, [fileRuleMap, activeRuleNames]);

  // ---- AZIONI ----

  // ATTIVA: carica le regole del file nel motore
  const handleActivateFile = async (filename) => {
    setTogglingRule(filename);
    setError(null);
    try {
      await axios.post(`/api/rules/load/${filename}`);
      setSuccess(`Regole di "${filename}" attivate nel motore CEP`);
      if (onRulesChanged) await onRulesChanged();
      setTimeout(() => setSuccess(null), 3000);
    } catch (err) {
      setError(err.response?.data?.error || 'Errore durante l\'attivazione');
    } finally {
      setTogglingRule(null);
    }
  };

  // DISATTIVA: rimuove le regole del file dal motore (il file resta su disco)
  const handleDeactivateFile = async (filename) => {
    setTogglingRule(filename);
    setError(null);
    try {
      const rulesInFile = fileRuleMap[filename] || [];

      if (rulesInFile.length > 0) {
        let allRemoved = true;
        for (const rName of rulesInFile) {
          try {
            const resp = await axios.delete(`/api/rules/active/${encodeURIComponent(rName)}`);
            if (!resp.data.success) allRemoved = false;
          } catch (e) {
            allRemoved = false;
          }
        }
        if (allRemoved) {
          setSuccess(`Regole di "${filename}" disattivate dal motore CEP`);
        } else {
          setSuccess(`Alcune regole di "${filename}" sono state disattivate`);
        }
      } else {
        // Fallback: prova col nome file senza estensione
        const baseName = filename.replace('.drl', '');
        await axios.delete(`/api/rules/active/${encodeURIComponent(baseName)}`);
        setSuccess(`Regola "${baseName}" disattivata dal motore CEP`);
      }

      if (onRulesChanged) await onRulesChanged();
      setTimeout(() => setSuccess(null), 3000);
    } catch (err) {
      setError(err.response?.data?.error || 'Errore durante la disattivazione');
    } finally {
      setTogglingRule(null);
    }
  };

  // TOGGLE
  const handleToggleFile = async (filename) => {
    const status = getFileStatus(filename);
    if (status === 'active' || status === 'partial') {
      await handleDeactivateFile(filename);
    } else {
      await handleActivateFile(filename);
    }
  };

  // RIMUOVI: elimina il file dal filesystem (e scarica dal motore se attivo)
  const handleDeleteFile = async (filename) => {
    const status = getFileStatus(filename);
    const statusLabel = (status === 'active' || status === 'partial')
      ? '\n\nATTENZIONE: le regole sono attualmente ATTIVE nel motore e verranno anche disattivate.'
      : '';

    if (!confirm(`Sei sicuro di voler ELIMINARE il file "${filename}"?${statusLabel}`)) {
      return;
    }

    setDeletingFile(filename);
    setError(null);

    try {
      await axios.delete(`/api/rules/files/${filename}`);
      setSuccess(`File "${filename}" eliminato con successo`);
      await fetchRuleFiles();
      if (onRulesChanged) onRulesChanged();
      setTimeout(() => setSuccess(null), 3000);
    } catch (err) {
      setError(err.response?.data?.error || "Errore durante l'eliminazione");
    } finally {
      setDeletingFile(null);
    }
  };

  // Download file .drl
  const handleDownloadFile = (filename) => {
    const downloadUrl = `/api/rules/files/${encodeURIComponent(filename)}/download`;
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // Visualizza contenuto file
  const handleViewFile = async (filename) => {
    setLoading(true);
    try {
      const response = await axios.get(`/api/rules/files/${filename}`);
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

  // Upload regola da editor
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

      setSuccess(
        `Regola "${response.data.ruleName}" caricata con successo!` +
        (response.data.loadedDynamically ? ' E attivata nel motore CEP!' : '')
      );
      setRuleName('');
      setRuleContent('');
      setValidationResult(null);

      await fetchRuleFiles();
      if (onRulesChanged) onRulesChanged();

      setTimeout(() => {
        setActiveView('files');
        setSuccess(null);
      }, 2000);
    } catch (err) {
      setError(err.response?.data?.error || 'Errore durante il caricamento della regola');
    } finally {
      setLoading(false);
    }
  };

  const validation = useDroolsValidation(ruleContent, 1000);

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

  // Upload file .drl da disco
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

  // Helper: badge stato
  const StatusBadge = ({ status }) => {
    if (status === 'active') {
      return <span className="badge badge-success">Attiva</span>;
    }
    if (status === 'partial') {
      return <span className="badge badge-warning">Parziale</span>;
    }
    return <span className="badge badge-inactive">Disattiva</span>;
  };

  return (
    <div className="rules-management">
      {/* Header con azioni */}
      <div className="rules-header">
        <div className="rules-tabs">
          <button
            className={activeView === 'files' ? 'active' : ''}
            onClick={() => { setActiveView('files'); fetchRuleFiles(); }}
          >
            <Code size={18} /> Gestione Regole ({ruleFiles.length})
          </button>
          <button
            className={activeView === 'active' ? 'active' : ''}
            onClick={() => setActiveView('active')}
          >
            <Power size={18} /> Attive nel Motore ({rules.length})
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
            title="Aggiorna"
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

      {/* ===== TAB: GESTIONE REGOLE (file + toggle attiva/disattiva) ===== */}
      {activeView === 'files' && (
        <div className="files-list-view">
          <h3>Regole Disponibili</h3>
          <p className="view-description">
            Attiva o disattiva le regole nel motore CEP. Le regole disattivate restano disponibili su disco.
          </p>
          {ruleFiles.length > 0 ? (
            <div className="files-table">
              {ruleFiles.map((file, index) => {
                const status = getFileStatus(file.name);
                const isToggling = togglingRule === file.name;
                const isDeleting = deletingFile === file.name;
                const rulesInFile = fileRuleMap[file.name] || [];

                return (
                  <div key={index} className={`file-row ${status === 'active' ? 'file-row-active' : ''}`}>
                    <div className="file-info">
                      <FileText size={20} />
                      <div>
                        <div className="file-name">
                          {file.name}
                          {' '}<StatusBadge status={status} />
                        </div>
                        <div className="file-meta">
                          {(file.size / 1024).toFixed(2)} KB •{' '}
                          {new Date(file.lastModified).toLocaleString('it-IT')}
                          {rulesInFile.length > 0 && (
                            <span className="rules-count">
                              {' '}• {rulesInFile.length} regol{rulesInFile.length === 1 ? 'a' : 'e'}
                            </span>
                          )}
                        </div>
                      </div>
                    </div>
                    <div className="file-actions">
                      {/* Toggle Attiva/Disattiva */}
                      <button
                        onClick={() => handleToggleFile(file.name)}
                        className={`btn-toggle ${status === 'active' || status === 'partial' ? 'btn-toggle-on' : 'btn-toggle-off'}`}
                        title={status === 'active' || status === 'partial' ? 'Disattiva regola' : 'Attiva regola'}
                        disabled={isToggling || isDeleting}
                      >
                        {isToggling ? (
                          <RefreshCw size={18} className="spinning" />
                        ) : status === 'active' || status === 'partial' ? (
                          <><ToggleRight size={18} /> <span className="btn-label">Disattiva</span></>
                        ) : (
                          <><ToggleLeft size={18} /> <span className="btn-label">Attiva</span></>
                        )}
                      </button>
                      {/* Visualizza */}
                      <button
                        onClick={() => handleViewFile(file.name)}
                        className="btn-icon"
                        title="Visualizza"
                      >
                        <Eye size={18} />
                      </button>
                      {/* Download */}
                      <button
                        onClick={() => handleDownloadFile(file.name)}
                        className="btn-icon"
                        title="Scarica file"
                      >
                        <Download size={18} />
                      </button>
                      {/* Elimina */}
                      <button
                        onClick={() => handleDeleteFile(file.name)}
                        className="btn-icon btn-danger"
                        title="Elimina file"
                        disabled={isToggling || isDeleting}
                      >
                        {isDeleting ? (
                          <RefreshCw size={16} className="spinning" />
                        ) : (
                          <Trash2 size={18} />
                        )}
                      </button>
                    </div>
                  </div>
                );
              })}
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

      {/* ===== TAB: REGOLE ATTIVE NEL MOTORE ===== */}
      {activeView === 'active' && (
        <div className="rules-list-view">
          <h3>Regole Attive nel Motore CEP</h3>
          <p className="view-description">
            Queste regole sono attualmente caricate e in esecuzione nel motore Drools.
          </p>
          {rules.length > 0 ? (
            <div className="rules-grid">
              {rules.map((rule, index) => (
                <div key={index} className="rule-card">
                  <div className="rule-card-header">
                    <CheckCircle size={18} color="#00C49F" />
                    <span className="rule-name">{rule.name}</span>
                    <span className="badge badge-success">Attiva</span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state">
              <AlertCircle size={48} />
              <p>Nessuna regola attiva nel motore CEP</p>
              <p className="empty-state-hint">Vai su "Gestione Regole" per attivare le regole</p>
            </div>
          )}
        </div>
      )}

      {/* ===== TAB: EDITOR NUOVA REGOLA ===== */}
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
              <ValidationIndicator validation={validation} />
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
                  <><RefreshCw size={18} className="spinning" /> Caricamento...</>
                ) : (
                  <><Upload size={18} /> Carica Regola</>
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