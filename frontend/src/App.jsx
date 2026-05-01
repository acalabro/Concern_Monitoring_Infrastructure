import React, { useState, useEffect } from 'react';
import axios from 'axios';
import RulesManagement from './RulesManagement';
import {
  LineChart, Line, BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts';
import {
  Activity, AlertTriangle, Play, Square, RefreshCw, 
  Database, Cpu, HardDrive, CheckCircle, XCircle, Clock,
  ExternalLink
} from 'lucide-react';
import './App.css';

// URL del Probes Manager — sovrascrivibile via VITE_PROBES_MANAGER_URL in .env.local
const PROBES_MANAGER_URL = import.meta.env.VITE_PROBES_MANAGER_URL ?? 'http://localhost:8080/ui/';

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884D8'];

function App() {
  const [systemStatus, setSystemStatus] = useState(null);
  const [metrics, setMetrics] = useState(null);
  const [rules, setRules] = useState([]);
  const [eventsStats, setEventsStats] = useState(null);
  const [violationsStats, setViolationsStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('overview');
  const [actionLoading, setActionLoading] = useState(false);
  
  // Fetch data from backend
  const fetchData = async () => {
    try {
      setError(null);
      
      const [statusRes, metricsRes, rulesRes, eventsRes, violationsRes] = await Promise.allSettled([
        axios.get('/api/status'),
        axios.get('/api/metrics'),
        axios.get('/api/rules'),
        axios.get('/api/stats/events'),
        axios.get('/api/stats/violations')
      ]);

      // Status (required)
      if (statusRes.status === 'fulfilled') {
        setSystemStatus(statusRes.value.data);
      } else {
        console.error('Error fetching status:', statusRes.reason);
        setSystemStatus({ running: false, timestamp: Date.now() });
      }

      // Metrics
      if (metricsRes.status === 'fulfilled') {
        setMetrics(metricsRes.value.data);
      } else {
        console.error('Error fetching metrics:', metricsRes.reason);
        setMetrics({ totalEvents: 0, totalViolations: 0 });
      }

      // Rules
      if (rulesRes.status === 'fulfilled') {
        setRules(rulesRes.value.data.rules || []);
      } else {
        console.error('Error fetching rules:', rulesRes.reason);
        setRules([]);
      }

      // Events stats
      if (eventsRes.status === 'fulfilled') {
        setEventsStats(eventsRes.value.data);
      } else {
        console.error('Error fetching events stats:', eventsRes.reason);
        setEventsStats({ bySender: [], byClass: [], timeline24h: [] });
      }

      // Violations stats
      if (violationsRes.status === 'fulfilled') {
        setViolationsStats(violationsRes.value.data);
      } else {
        console.error('Error fetching violations stats:', violationsRes.reason);
        setViolationsStats({ byRule: [], byProbe: [], timeline24h: [], recent: [] });
      }

      setLoading(false);
    } catch (error) {
      console.error('Error retrieving data:', error);
      setError('Unable to connect to the backend. Please check that it is running on port 8181.');
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 5000); // Refresh every 5 seconds
    return () => clearInterval(interval);
  }, []);

  // Control actions
  const startMonitoring = async () => {
    try {
      setActionLoading(true);
      setError(null);
      
      const response = await axios.post('/api/start');
      console.log('Start response:', response.data);
      
      // Wait a bit before refreshing to give the system time to start
      setTimeout(() => {
        fetchData();
        setActionLoading(false);
      }, 2000);
      
    } catch (error) {
      console.error('Error starting:', error);
      setError(error.response?.data?.error || 'Error starting monitoring');
      setActionLoading(false);
    }
  };

  const stopMonitoring = async () => {
    try {
      setActionLoading(true);
      setError(null);
      
      const response = await axios.post('/api/stop');
      console.log('Stop response:', response.data);
      
      setTimeout(() => {
        fetchData();
        setActionLoading(false);
      }, 1000);
      
    } catch (error) {
      console.error('Error stopping:', error);
      setError(error.response?.data?.error || 'Error stopping monitoring');
      setActionLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="loading-screen">
        <RefreshCw className="spinning" size={48} />
        <p>Loading dashboard...</p>
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
            <button
              onClick={() => window.open(PROBES_MANAGER_URL, '_blank', 'noopener,noreferrer')}
              className="btn-probes"
              title="Open Probes Manager"
            >
              <ExternalLink size={18} /> Probes Manager
            </button>
            <button onClick={fetchData} className="btn-refresh" disabled={actionLoading}>
              <RefreshCw size={18} className={actionLoading ? 'spinning' : ''} /> Refresh
            </button>
            {systemStatus?.running ? (
              <button onClick={stopMonitoring} className="btn-stop" disabled={actionLoading}>
                <Square size={18} /> {actionLoading ? 'Stopping...' : 'Stop'}
              </button>
            ) : (
              <button onClick={startMonitoring} className="btn-start" disabled={actionLoading}>
                <Play size={18} /> {actionLoading ? 'Starting...' : 'Start'}
              </button>
            )}
          </div>
        </div>
        {error && (
          <div className="error-banner">
            <AlertTriangle size={18} />
            <span>{error}</span>
          </div>
        )}
      </header>

      {/* Status Cards */}
      <div className="status-cards">
        <StatusCard
          icon={<Activity />}
          title="System Status"
          value={systemStatus?.running ? 'Running' : 'Stopped'}
          status={systemStatus?.running ? 'success' : 'warning'}
        />
        <StatusCard
          icon={<Database />}
          title="Events Received Since Start"
          value={systemStatus?.eventsReceived || 0}
          status="info"
        />
        <StatusCard
          icon={<AlertTriangle />}
          title="Total Violations"
          value={metrics?.totalViolations || 0}
          status="danger"
        />
        <StatusCard
          icon={<CheckCircle />}
          title="Active Rules"
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
          Events
        </button>
        <button
          className={activeTab === 'violations' ? 'active' : ''}
          onClick={() => setActiveTab('violations')}
        >
          Violations
        </button>
        <button
          className={activeTab === 'rules' ? 'active' : ''}
          onClick={() => setActiveTab('rules')}
        >
          Rules
        </button>
        <button
          className={activeTab === 'system' ? 'active' : ''}
          onClick={() => setActiveTab('system')}
        >
          System
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
		{activeTab === 'rules' && <RulesTab rules={rules} onRefresh={fetchData} />}
        {activeTab === 'system' && <SystemTab metrics={metrics} systemStatus={systemStatus} />}
      </div>
    </div>
  );
}

// Status Card component
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

// Overview Tab
function OverviewTab({ metrics, eventsStats, violationsStats }) {
  return (
    <div className="overview-grid">
      {/* Events last hour */}
      <div className="chart-card">
        <h3>Events Last Hour</h3>
        <div className="metric-value">{metrics?.eventsLastHour || 0}</div>
      </div>

      {/* Violations last hour */}
      <div className="chart-card">
        <h3>Violations Last Hour</h3>
        <div className="metric-value danger">{metrics?.violationsLastHour || 0}</div>
      </div>

      {/* Events Timeline 24h */}
      <div className="chart-card wide">
        <h3>Events Trend (24h)</h3>
        {eventsStats?.timeline24h && eventsStats.timeline24h.length > 0 ? (
          <ResponsiveContainer width="100%" height={250}>
            <LineChart data={eventsStats.timeline24h}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="hour" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="count" stroke="#0088FE" name="Events" />
            </LineChart>
          </ResponsiveContainer>
        ) : (
          <div className="no-data">No data available</div>
        )}
      </div>

      {/* Violations Timeline 24h */}
      <div className="chart-card wide">
        <h3>Violations Trend (24h)</h3>
        {violationsStats?.timeline24h && violationsStats.timeline24h.length > 0 ? (
          <ResponsiveContainer width="100%" height={250}>
            <LineChart data={violationsStats.timeline24h}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="hour" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="count" stroke="#FF8042" name="Violations" />
            </LineChart>
          </ResponsiveContainer>
        ) : (
          <div className="no-data">No data available</div>
        )}
      </div>
    </div>
  );
}

// Events Tab
function EventsTab({ stats }) {
  return (
    <div className="events-grid">
      {/* Events by Sender */}
      <div className="chart-card">
        <h3>Events by Sender</h3>
        {stats?.bySender && stats.bySender.length > 0 ? (
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={stats.bySender}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="senderID" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="count" fill="#0088FE" />
            </BarChart>
          </ResponsiveContainer>
        ) : (
          <div className="no-data">No events recorded</div>
        )}
      </div>

      {/* Events by Class */}
      <div className="chart-card">
        <h3>Events by Class</h3>
        {stats?.byClass && stats.byClass.length > 0 ? (
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={stats.byClass}
                dataKey="count"
                nameKey="className"
                cx="50%"
                cy="50%"
                outerRadius={100}
                label
              >
                {stats.byClass.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        ) : (
          <div className="no-data">No events recorded</div>
        )}
      </div>

      {/* Timeline 24h */}
      <div className="chart-card wide">
        <h3>Events Timeline (24h)</h3>
        {stats?.timeline24h && stats.timeline24h.length > 0 ? (
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={stats.timeline24h}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="hour" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="count" stroke="#00C49F" strokeWidth={2} />
            </LineChart>
          </ResponsiveContainer>
        ) : (
          <div className="no-data">No data available</div>
        )}
      </div>
    </div>
  );
}

// Violations Tab
function ViolationsTab({ stats }) {
  return (
    <div className="violations-grid">
      {/* Violations by Rule */}
      <div className="chart-card">
        <h3>Violations by Rule</h3>
        {stats?.byRule && stats.byRule.length > 0 ? (
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={stats.byRule}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="ruleName" angle={-45} textAnchor="end" height={100} />
              <YAxis />
              <Tooltip />
              <Bar dataKey="count" fill="#FF8042" />
            </BarChart>
          </ResponsiveContainer>
        ) : (
          <div className="no-data">No violations recorded</div>
        )}
      </div>

      {/* Violations by Probe */}
      <div className="chart-card">
        <h3>Violations by Probe</h3>
        {stats?.byProbe && stats.byProbe.length > 0 ? (
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={stats.byProbe}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="probeName" angle={-45} textAnchor="end" height={100} />
              <YAxis />
              <Tooltip />
              <Bar dataKey="count" fill="#FFBB28" />
            </BarChart>
          </ResponsiveContainer>
        ) : (
          <div className="no-data">No violations recorded</div>
        )}
      </div>

      {/* Recent Violations */}
      <div className="chart-card wide">
        <h3>Recent Violations</h3>
        {stats?.recent && stats.recent.length > 0 ? (
          <div className="violations-list">
            {stats.recent.map((v) => (
              <div key={v.id} className="violation-item">
                <div className="violation-header">
                  <AlertTriangle size={18} color="#FF8042" />
                  <strong>{v.rule}</strong>
                  <span className="timestamp">
                    <Clock size={14} /> {new Date(v.timestamp).toLocaleString('en-US')}
                  </span>
                </div>
                <div className="violation-body">
                  <div className="violation-probe">Probe: {v.probe}</div>
                  <div className="violation-message">{v.message}</div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="no-data">No recent violations</div>
        )}
      </div>
    </div>
  );
}

// Rules Tab
function RulesTab({ rules, onRefresh }) {
  const handleRulesChanged = async () => {
    console.log('Rules changed, refreshing in 1.5s...');
    setTimeout(() => {
      if (onRefresh) {
        onRefresh();
      }
    }, 1500);
  };
 
  return (
    <RulesManagement 
      rules={rules} 
      onRulesChanged={handleRulesChanged}
    />
  );
}
// System Tab
function SystemTab({ metrics, systemStatus }) {
  const memoryData = metrics?.system ? [
    { name: 'Used', value: metrics.system.usedMemoryMB },
    { name: 'Free', value: metrics.system.freeMemoryMB }
  ] : [];

  return (
    <div className="system-grid">
      {/* Memory */}
      <div className="chart-card">
        <h3>Memory Usage</h3>
        {memoryData.length > 0 ? (
          <>
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
              <div>Total: {metrics?.system?.totalMemoryMB} MB</div>
            </div>
          </>
        ) : (
          <div className="no-data">Memory data not available</div>
        )}
      </div>

      {/* Components */}
      <div className="chart-card">
        <h3>Component Status</h3>
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

      {/* System Info */}
      <div className="chart-card wide">
        <h3>System Information</h3>
        <div className="system-info">
          <div className="info-row">
            <Cpu size={20} />
            <span>Events Received Since Start:</span>
            <strong>{systemStatus?.eventsReceived || 0}</strong>
          </div>
          <div className="info-row">
            <Database size={20} />
            <span>Total Events:</span>
            <strong>{metrics?.totalEvents || 0}</strong>
          </div>
          <div className="info-row">
            <AlertTriangle size={20} />
            <span>Total Violations:</span>
            <strong>{metrics?.totalViolations || 0}</strong>
          </div>
          <div className="info-row">
            <CheckCircle size={20} />
            <span>Rules Loaded:</span>
            <strong>{systemStatus?.rulesLoaded || 0}</strong>
          </div>
        </div>
      </div>
    </div>
  );
}

// Component Status component
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