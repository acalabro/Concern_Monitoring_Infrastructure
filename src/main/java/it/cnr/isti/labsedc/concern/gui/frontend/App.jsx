import React, { useState, useEffect } from 'react';
import axios from 'axios';
import {
  LineChart, Line, BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts';
import {
  Activity, AlertTriangle, Play, Square, RefreshCw, 
  Database, Cpu, HardDrive, CheckCircle, XCircle, Clock
} from 'lucide-react';
import './App.css';

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884D8'];

function App() {
  const [systemStatus, setSystemStatus] = useState(null);
  const [metrics, setMetrics] = useState(null);
  const [rules, setRules] = useState([]);
  const [eventsStats, setEventsStats] = useState(null);
  const [violationsStats, setViolationsStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('overview');

  // Fetch dati dal backend
  const fetchData = async () => {
    try {
      const [statusRes, metricsRes, rulesRes, eventsRes, violationsRes] = await Promise.all([
        axios.get('/api/status'),
        axios.get('/api/metrics'),
        axios.get('/api/rules'),
        axios.get('/api/stats/events'),
        axios.get('/api/stats/violations')
      ]);

      setSystemStatus(statusRes.data);
      setMetrics(metricsRes.data);
      setRules(rulesRes.data.rules);
      setEventsStats(eventsRes.data);
      setViolationsStats(violationsRes.data);
      setLoading(false);
    } catch (error) {
      console.error('Errore nel recupero dei dati:', error);
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 5000); // Refresh ogni 5 secondi
    return () => clearInterval(interval);
  }, []);

  // Azioni di controllo
  const startMonitoring = async () => {
    try {
      await axios.post('/api/start');
      fetchData();
    } catch (error) {
      console.error('Errore nell\'avvio:', error);
    }
  };

  const stopMonitoring = async () => {
    try {
      await axios.post('/api/stop');
      fetchData();
    } catch (error) {
      console.error('Errore nella fermata:', error);
    }
  };

  if (loading) {
    return (
      <div className="loading-screen">
        <RefreshCw className="spinning" size={48} />
        <p>Caricamento dashboard...</p>
      </div>
    );
  }

  return (
    <div className="dashboard">
      {/* Header */}
      <header className="dashboard-header">
        <div className="header-content">
          <h1><Activity /> Concern Monitoring Dashboard</h1>
          <div className="header-controls">
            <button onClick={fetchData} className="btn-refresh">
              <RefreshCw size={18} /> Aggiorna
            </button>
            {systemStatus?.running ? (
              <button onClick={stopMonitoring} className="btn-stop">
                <Square size={18} /> Stop
              </button>
            ) : (
              <button onClick={startMonitoring} className="btn-start">
                <Play size={18} /> Start
              </button>
            )}
          </div>
        </div>
      </header>

      {/* Status Cards */}
      <div className="status-cards">
        <StatusCard
          icon={<Activity />}
          title="Stato Sistema"
          value={systemStatus?.running ? 'Running' : 'Stopped'}
          status={systemStatus?.running ? 'success' : 'warning'}
        />
        <StatusCard
          icon={<Database />}
          title="Eventi Ricevuti"
          value={systemStatus?.eventsReceived || 0}
          status="info"
        />
        <StatusCard
          icon={<AlertTriangle />}
          title="Violazioni Totali"
          value={metrics?.totalViolations || 0}
          status="danger"
        />
        <StatusCard
          icon={<CheckCircle />}
          title="Regole Attive"
          value={systemStatus?.rulesLoaded || 0}
          status="success"
        />
      </div>

      {/* Tabs Navigation */}
      <div className="tabs-nav">
        <button
          className={activeTab === 'overview' ? 'active' : ''}
          onClick={() => setActiveTab('overview')}
        >
          Overview
        </button>
        <button
          className={activeTab === 'events' ? 'active' : ''}
          onClick={() => setActiveTab('events')}
        >
          Eventi
        </button>
        <button
          className={activeTab === 'violations' ? 'active' : ''}
          onClick={() => setActiveTab('violations')}
        >
          Violazioni
        </button>
        <button
          className={activeTab === 'rules' ? 'active' : ''}
          onClick={() => setActiveTab('rules')}
        >
          Regole
        </button>
        <button
          className={activeTab === 'system' ? 'active' : ''}
          onClick={() => setActiveTab('system')}
        >
          Sistema
        </button>
      </div>

      {/* Tab Content */}
      <div className="tab-content">
        {activeTab === 'overview' && (
          <OverviewTab
            metrics={metrics}
            eventsStats={eventsStats}
            violationsStats={violationsStats}
          />
        )}
        {activeTab === 'events' && <EventsTab stats={eventsStats} />}
        {activeTab === 'violations' && <ViolationsTab stats={violationsStats} />}
        {activeTab === 'rules' && <RulesTab rules={rules} />}
        {activeTab === 'system' && <SystemTab metrics={metrics} systemStatus={systemStatus} />}
      </div>
    </div>
  );
}

// Componente Card di stato
function StatusCard({ icon, title, value, status }) {
  return (
    <div className={`status-card status-${status}`}>
      <div className="card-icon">{icon}</div>
      <div className="card-content">
        <div className="card-title">{title}</div>
        <div className="card-value">{value}</div>
      </div>
    </div>
  );
}

// Tab Overview
function OverviewTab({ metrics, eventsStats, violationsStats }) {
  return (
    <div className="overview-grid">
      {/* Eventi ultima ora */}
      <div className="chart-card">
        <h3>Eventi Ultima Ora</h3>
        <div className="metric-value">{metrics?.eventsLastHour || 0}</div>
      </div>

      {/* Violazioni ultima ora */}
      <div className="chart-card">
        <h3>Violazioni Ultima Ora</h3>
        <div className="metric-value danger">{metrics?.violationsLastHour || 0}</div>
      </div>

      {/* Timeline Eventi 24h */}
      <div className="chart-card wide">
        <h3>Andamento Eventi (24h)</h3>
        <ResponsiveContainer width="100%" height={250}>
          <LineChart data={eventsStats?.timeline24h || []}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="hour" />
            <YAxis />
            <Tooltip />
            <Legend />
            <Line type="monotone" dataKey="count" stroke="#0088FE" name="Eventi" />
          </LineChart>
        </ResponsiveContainer>
      </div>

      {/* Timeline Violazioni 24h */}
      <div className="chart-card wide">
        <h3>Andamento Violazioni (24h)</h3>
        <ResponsiveContainer width="100%" height={250}>
          <LineChart data={violationsStats?.timeline24h || []}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="hour" />
            <YAxis />
            <Tooltip />
            <Legend />
            <Line type="monotone" dataKey="count" stroke="#FF8042" name="Violazioni" />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}

// Tab Eventi
function EventsTab({ stats }) {
  return (
    <div className="events-grid">
      {/* Eventi per Sender */}
      <div className="chart-card">
        <h3>Eventi per Sender</h3>
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={stats?.bySender || []}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="senderID" />
            <YAxis />
            <Tooltip />
            <Bar dataKey="count" fill="#0088FE" />
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* Eventi per Classe */}
      <div className="chart-card">
        <h3>Eventi per Classe</h3>
        <ResponsiveContainer width="100%" height={300}>
          <PieChart>
            <Pie
              data={stats?.byClass || []}
              dataKey="count"
              nameKey="className"
              cx="50%"
              cy="50%"
              outerRadius={100}
              label
            >
              {(stats?.byClass || []).map((entry, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      </div>

      {/* Timeline 24h */}
      <div className="chart-card wide">
        <h3>Timeline Eventi (24h)</h3>
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={stats?.timeline24h || []}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="hour" />
            <YAxis />
            <Tooltip />
            <Legend />
            <Line type="monotone" dataKey="count" stroke="#00C49F" strokeWidth={2} />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}

// Tab Violazioni
function ViolationsTab({ stats }) {
  return (
    <div className="violations-grid">
      {/* Violazioni per Regola */}
      <div className="chart-card">
        <h3>Violazioni per Regola</h3>
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={stats?.byRule || []}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="ruleName" angle={-45} textAnchor="end" height={100} />
            <YAxis />
            <Tooltip />
            <Bar dataKey="count" fill="#FF8042" />
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* Violazioni per Probe */}
      <div className="chart-card">
        <h3>Violazioni per Probe</h3>
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={stats?.byProbe || []}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="probeName" angle={-45} textAnchor="end" height={100} />
            <YAxis />
            <Tooltip />
            <Bar dataKey="count" fill="#FFBB28" />
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* Violazioni Recenti */}
      <div className="chart-card wide">
        <h3>Violazioni Recenti</h3>
        <div className="violations-list">
          {(stats?.recent || []).map((v) => (
            <div key={v.id} className="violation-item">
              <div className="violation-header">
                <AlertTriangle size={18} color="#FF8042" />
                <strong>{v.rule}</strong>
                <span className="timestamp">
                  <Clock size={14} /> {new Date(v.timestamp).toLocaleString('it-IT')}
                </span>
              </div>
              <div className="violation-body">
                <div className="violation-probe">Probe: {v.probe}</div>
                <div className="violation-message">{v.message}</div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// Tab Regole
function RulesTab({ rules }) {
  return (
    <div className="rules-container">
      <div className="chart-card">
        <h3>Regole Caricate ({rules.length})</h3>
        <div className="rules-list">
          {rules.map((rule, index) => (
            <div key={index} className="rule-item">
              <CheckCircle size={18} color="#00C49F" />
              <span>{rule.name}</span>
              {rule.enabled && <span className="badge badge-success">Attiva</span>}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// Tab Sistema
function SystemTab({ metrics, systemStatus }) {
  const memoryData = metrics?.system ? [
    { name: 'Usata', value: metrics.system.usedMemoryMB },
    { name: 'Libera', value: metrics.system.freeMemoryMB }
  ] : [];

  return (
    <div className="system-grid">
      {/* Memoria */}
      <div className="chart-card">
        <h3>Utilizzo Memoria</h3>
        <ResponsiveContainer width="100%" height={250}>
          <PieChart>
            <Pie
              data={memoryData}
              dataKey="value"
              nameKey="name"
              cx="50%"
              cy="50%"
              outerRadius={80}
              label={(entry) => `${entry.name}: ${entry.value} MB`}
            >
              <Cell fill="#FF8042" />
              <Cell fill="#00C49F" />
            </Pie>
            <Tooltip />
          </PieChart>
        </ResponsiveContainer>
        <div className="memory-stats">
          <div>Max: {metrics?.system?.maxMemoryMB} MB</div>
          <div>Totale: {metrics?.system?.totalMemoryMB} MB</div>
        </div>
      </div>

      {/* Componenti */}
      <div className="chart-card">
        <h3>Stato Componenti</h3>
        <div className="components-list">
          <ComponentStatus
            name="Broker"
            running={systemStatus?.components?.broker}
          />
          <ComponentStatus
            name="CEP Engine"
            running={systemStatus?.components?.cep}
          />
          <ComponentStatus
            name="Storage"
            running={systemStatus?.components?.storage}
          />
          <ComponentStatus
            name="Notification Manager"
            running={systemStatus?.components?.notification}
          />
        </div>
      </div>

      {/* Info Sistema */}
      <div className="chart-card wide">
        <h3>Informazioni Sistema</h3>
        <div className="system-info">
          <div className="info-row">
            <Cpu size={20} />
            <span>Eventi Ricevuti:</span>
            <strong>{systemStatus?.eventsReceived || 0}</strong>
          </div>
          <div className="info-row">
            <Database size={20} />
            <span>Eventi Totali:</span>
            <strong>{metrics?.totalEvents || 0}</strong>
          </div>
          <div className="info-row">
            <AlertTriangle size={20} />
            <span>Violazioni Totali:</span>
            <strong>{metrics?.totalViolations || 0}</strong>
          </div>
          <div className="info-row">
            <CheckCircle size={20} />
            <span>Regole Caricate:</span>
            <strong>{systemStatus?.rulesLoaded || 0}</strong>
          </div>
        </div>
      </div>
    </div>
  );
}

// Componente Stato Componente
function ComponentStatus({ name, running }) {
  return (
    <div className="component-status">
      {running ? (
        <CheckCircle size={20} color="#00C49F" />
      ) : (
        <XCircle size={20} color="#FF8042" />
      )}
      <span>{name}</span>
      <span className={`status-badge ${running ? 'running' : 'stopped'}`}>
        {running ? 'Running' : 'Stopped'}
      </span>
    </div>
  );
}

export default App;
