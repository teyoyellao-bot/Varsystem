import { useEffect, useState } from 'react';
import AuthScreen from './AuthScreen.jsx';

const API_BASE = import.meta.env.VITE_VAARL_API_URL || 'http://localhost:8080';

export default function App() {
  const [api, setApi] = useState({ loading: true, ok: false, message: 'Checking backend…' });
  const [db, setDb] = useState({ loading: true, ok: false, message: 'Checking database…' });

  const [checkedAt, setCheckedAt] = useState(null);

  useEffect(() => {
    let active = true;
    const checkStatus = async () => {
      if (active) {
        setApi(s => ({ ...s, loading: true }));
        setDb(s => ({ ...s, loading: true }));
      }
      const [apiResult, dbResult] = await Promise.allSettled([
        fetch(API_BASE + '/api/health').then(r => r.ok ? r.json() : Promise.reject(new Error('Backend response error'))),
        fetch(API_BASE + '/api/health/database').then(r => r.ok ? r.json() : Promise.reject(new Error('Database check error')))
      ]);
      if (!active) return;
      setApi(apiResult.status === 'fulfilled'
        ? { loading: false, ok: true, message: apiResult.value.message || 'Backend online' }
        : { loading: false, ok: false, message: 'Backend unavailable. Start the API server first.' });
      setDb(dbResult.status === 'fulfilled'
        ? { loading: false, ok: !!dbResult.value.connected, message: dbResult.value.connected ? 'PostgreSQL connected' : 'Database not connected' }
        : { loading: false, ok: false, message: 'Database connection check failed' });
      setCheckedAt(new Date());
    };
    checkStatus();
    const timer = window.setInterval(checkStatus, 30000);
    return () => { active = false; window.clearInterval(timer); };
  }, []);

  return <main className="page">
    <header className="topbar">
      <div className="brand-mark">V</div><div className="brand-name">VAARL <span>WEB</span></div>
      <div className="top-note">Vehicle Assistance & Repair Locator</div>
    </header>
    <AuthScreen />
    <section className="status-section">
      <div className="section-label">DEVELOPMENT CONNECTION STATUS</div>
      <div className="status-grid"><StatusCard title="Web API" state={api}/><StatusCard title="Database" state={db}/></div>
      <p className="status-refresh-note">Auto-checks every 30 seconds{checkedAt ? ' · Last checked ' + checkedAt.toLocaleTimeString() : ''}</p>
    </section>
    <footer>VAARL Web System · Development preview</footer>
  </main>;
}
function StatusCard({ title, state }) {
  return <article className="status-card"><div className="status-head"><h2>{title}</h2><span className={'status-dot ' + (state.loading ? 'pending' : state.ok ? 'online' : 'offline')}/></div><p>{state.message}</p></article>;
}
