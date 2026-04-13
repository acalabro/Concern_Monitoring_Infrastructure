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

  // Active rule names in the engine (derived from props)
  const activeRuleNames = (rules || []).map(r => r.name);

  // Fetch rule files list
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
   * Extracts rule names from a .drl file content
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
   * Map: filename -> rule names contained (local cache)
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
   * Checks whether the rules in a file are loaded in the engine.
   * Returns 'active' | 'partial' | 'inactive'
   */
  const getFileStatus = useCallback((filename) => {
    const rulesInFile = fileRuleMap[filename] || [];
    if (rulesInFile.length === 0) {
      // Fallback: compare by filename
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

  // ---- ACTIONS ----

  // ACTIVATE: load the file's rules into the engine
  const handleActivateFile = async (filename) => {
    setTogglingRule(filename);
    setError(null);
    try {
      await axios.post(`/api/rules/load/${filename}`);
      setSuccess(`Rules from "${filename}" activated in CEP engine`);
      if (onRulesChanged) await onRulesChanged();
      setTimeout(() => setSuccess(null), 3000);
    } catch (err) {
      setError(err.response?.data?.error || 'Error during activation');
    } finally {
      setTogglingRule(null);
    }
  };

  // DEACTIVATE: remove the file's rules from the engine (file stays on disk)
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
          setSuccess(`Rules from "${filename}" deactivated from CEP engine`);
        } else {
          setSuccess(`Some rules from "${filename}" have been deactivated`);
        }
      } else {
        // Fallback: try with filename without extension
        const baseName = filename.replace('.drl', '');
        await axios.delete(`/api/rules/active/${encodeURIComponent(baseName)}`);
        setSuccess(`Rule "${baseName}" deactivated from CEP engine`);
      }

      if (onRulesChanged) await onRulesChanged();
      setTimeout(() => setSuccess(null), 3000);
    } catch (err) {
      setError(err.response?.data?.error || 'Error during deactivation');
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

  // DELETE: remove the file from the filesystem (and unload from engine if active)
  const handleDeleteFile = async (filename) => {
    const status = getFileStatus(filename);
    const statusLabel = (status === 'active' || status === 'partial')
      ? '\n\nWARNING: the rules are currently ACTIVE in the engine and will also be deactivated.'
      : '';

    if (!confirm(`Are you sure you want to DELETE the file "${filename}"?${statusLabel}`)) {
      return;
    }

    setDeletingFile(filename);
    setError(null);

    try {
      await axios.delete(`/api/rules/files/${filename}`);
      setSuccess(`File "${filename}" deleted successfully`);
      await fetchRuleFiles();
      if (onRulesChanged) onRulesChanged();
      setTimeout(() => setSuccess(null), 3000);
    } catch (err) {
      setError(err.response?.data?.error || 'Error during deletion');
    } finally {
      setDeletingFile(null);
    }
  };

  // Download .drl file
  const handleDownloadFile = (filename) => {
    const downloadUrl = `/api/rules/files/${encodeURIComponent(filename)}/download`;
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // View file content
  const handleViewFile = async (filename) => {
    setLoading(true);
    try {
      const response = await axios.get(`/api/rules/files/${filename}`);
      setSelectedFile({
        name: filename,
        content: response.data.content
      });
    } catch (err) {
      setError(err.response?.data?.error || 'Error reading file');
    } finally {
      setLoading(false);
    }
  };

  // Upload rule from editor
  const handleUploadRule = async () => {
    if (!ruleName.trim()) {
      setError('Please enter a rule name');
      return;
    }
    if (!ruleContent.trim()) {
      setError('Please enter the rule content');
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
        `Rule "${response.data.ruleName}" uploaded successfully!` +
        (response.data.loadedDynamically ? ' And activated in CEP engine!' : '')
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
      setError(err.response?.data?.error || 'Error uploading rule');
    } finally {
      setLoading(false);
    }
  };

  const validation = useDroolsValidation(ruleContent, 1000);

  // Validate rule
  const handleValidateRule = async () => {
    if (!ruleContent.trim()) {
      setError('Please enter rule content to validate');
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
      setError(err.response?.data?.error || 'Error during validation');
    } finally {
      setLoading(false);
    }
  };

  // Upload .drl file from disk
  const handleFileUpload = async (event) => {
    const file = event.target.files[0];
    if (!file) return;
    if (!file.name.endsWith('.drl')) {
      setError('File must have a .drl extension');
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

  // Empty rule template
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

  // Helper: status badge
  const StatusBadge = ({ status }) => {
    if (status === 'active') {
      return <span className="badge badge-success">Active</span>;
    }
    if (status === 'partial') {
      return <span className="badge badge-warning">Partial</span>;
    }
    return <span className="badge badge-inactive">Inactive</span>;
  };

  return (
    <div className="rules-management">
      {/* Header with actions */}
      <div className="rules-header">
        <div className="rules-tabs">
          <button
            className={activeView === 'files' ? 'active' : ''}
            onClick={() => { setActiveView('files'); fetchRuleFiles(); }}
          >
            <Code size={18} /> Rule Management ({ruleFiles.length})
          </button>
          <button
            className={activeView === 'active' ? 'active' : ''}
            onClick={() => setActiveView('active')}
          >
            <Power size={18} /> Active in Engine ({rules.length})
          </button>
          <button
            className={activeView === 'editor' ? 'active' : ''}
            onClick={() => {
              setActiveView('editor');
              if (!ruleContent) setRuleContent(getEmptyRuleTemplate());
            }}
          >
            <Plus size={18} /> New Rule
          </button>
        </div>

        <div className="rules-actions">
          <label className="btn-upload">
            <Upload size={18} />
            <span>Upload .drl File</span>
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
            title="Refresh"
          >
            <RefreshCw size={18} />
          </button>
        </div>
      </div>

      {/* Messages */}
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

      {/* ===== TAB: RULE MANAGEMENT (files + activate/deactivate toggle) ===== */}
      {activeView === 'files' && (
        <div className="files-list-view">
          <h3>Available Rules</h3>
          <p className="view-description">
            Activate or deactivate rules in the CEP engine. Deactivated rules remain available on disk.
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
                          {new Date(file.lastModified).toLocaleString('en-US')}
                          {rulesInFile.length > 0 && (
                            <span className="rules-count">
                              {' '}• {rulesInFile.length} rule{rulesInFile.length !== 1 ? 's' : ''}
                            </span>
                          )}
                        </div>
                      </div>
                    </div>
                    <div className="file-actions">
                      {/* Toggle Activate/Deactivate */}
                      <button
                        onClick={() => handleToggleFile(file.name)}
                        className={`btn-toggle ${status === 'active' || status === 'partial' ? 'btn-toggle-on' : 'btn-toggle-off'}`}
                        title={status === 'active' || status === 'partial' ? 'Deactivate rule' : 'Activate rule'}
                        disabled={isToggling || isDeleting}
                      >
                        {isToggling ? (
                          <RefreshCw size={18} className="spinning" />
                        ) : status === 'active' || status === 'partial' ? (
                          <><ToggleRight size={18} /> <span className="btn-label">Deactivate</span></>
                        ) : (
                          <><ToggleLeft size={18} /> <span className="btn-label">Activate</span></>
                        )}
                      </button>
                      {/* View */}
                      <button
                        onClick={() => handleViewFile(file.name)}
                        className="btn-icon"
                        title="View"
                      >
                        <Eye size={18} />
                      </button>
                      {/* Download */}
                      <button
                        onClick={() => handleDownloadFile(file.name)}
                        className="btn-icon"
                        title="Download file"
                      >
                        <Download size={18} />
                      </button>
                      {/* Delete */}
                      <button
                        onClick={() => handleDeleteFile(file.name)}
                        className="btn-icon btn-danger"
                        title="Delete file"
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
              <p>No .drl files found</p>
              <p className="empty-state-hint">Upload a file or create a new rule</p>
            </div>
          )}
        </div>
      )}

      {/* ===== TAB: ACTIVE RULES IN ENGINE ===== */}
      {activeView === 'active' && (
        <div className="rules-list-view">
          <h3>Active Rules in CEP Engine</h3>
          <p className="view-description">
            These rules are currently loaded and running in the Drools engine.
          </p>
          {rules.length > 0 ? (
            <div className="rules-grid">
              {rules.map((rule, index) => (
                <div key={index} className="rule-card">
                  <div className="rule-card-header">
                    <CheckCircle size={18} color="#00C49F" />
                    <span className="rule-name">{rule.name}</span>
                    <span className="badge badge-success">Active</span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state">
              <AlertCircle size={48} />
              <p>No active rules in CEP engine</p>
              <p className="empty-state-hint">Go to "Rule Management" to activate rules</p>
            </div>
          )}
        </div>
      )}

      {/* ===== TAB: NEW RULE EDITOR ===== */}
      {activeView === 'editor' && (
        <div className="rule-editor-view">
          <h3>Drools Rule Editor</h3>

          <div className="editor-form">
            <div className="form-group">
              <label>Rule Name</label>
              <input
                type="text"
                className="rule-input"
                placeholder="e.g.: MyNewRule"
                value={ruleName}
                onChange={(e) => setRuleName(e.target.value)}
              />
              <small>The file name will be: {ruleName || 'MyRule'}.drl</small>
            </div>

            <div className="form-group">
              <label>Rule Content (.drl)</label>
              <textarea
                className="rule-textarea"
                rows={20}
                value={ruleContent}
                onChange={(e) => setRuleContent(e.target.value)}
                placeholder="Enter the Drools rule content..."
                spellCheck={false}
              />
              <ValidationIndicator validation={validation} />
            </div>

            {validationResult && (
              <div className={`validation-result ${validationResult.valid ? 'valid' : 'invalid'}`}>
                {validationResult.valid ? (
                  <>
                    <CheckCircle size={18} />
                    <span>Rule is valid!</span>
                  </>
                ) : (
                  <>
                    <XCircle size={18} />
                    <div>
                      <strong>Validation errors:</strong>
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
                <CheckCircle size={18} /> Validate Rule
              </button>
              <button
                onClick={handleUploadRule}
                className="btn-primary"
                disabled={loading || !ruleName.trim() || !ruleContent.trim()}
              >
                {loading ? (
                  <><RefreshCw size={18} className="spinning" /> Uploading...</>
                ) : (
                  <><Upload size={18} /> Upload Rule</>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* File view modal */}
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
                Edit
              </button>
              <button onClick={() => setSelectedFile(null)} className="btn-primary">
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default RulesManagement;
