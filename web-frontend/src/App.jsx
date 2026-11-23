import { useEffect, useState } from 'react';

const API_BASE = import.meta.env.VITE_VAARL_API_URL || 'http://localhost:8080';

export default function App() {
  const [api, setApi] = useState({ loading: true, ok: false, message: 'Checking backend…' });
  const [db, setDb] = useState({ loading: true, ok: false, message: 'Checking database…' });

  useEffect(() => {
    fetch(API_BASE + '/api/health')
      .then(r => r.ok ? r.json() : Promise.reject(new Error('Backend response error')))
      .then(data => setApi({ loading: false, ok: true, message: data.message || 'Backend online' }))
      .catch(() => setApi({ loading: false, ok: false, message: 'Backend unavailable. Start the API server first.' }));

    fetch(API_BASE + '/api/health/database')
      .then(r => r.ok ? r.json() : Promise.reject(new Error('Database check error')))
      .then(data => setDb({ loading: false, ok: !!data.connected, message: data.connected ? 'PostgreSQL connected' : 'Database not connected' }))
      .catch(() => setDb({ loading: false, ok: false, message: 'Database connection check failed' }));
  }, []);

  return (
    <main className="page">
      <header className="topbar">
        <div className="brand-mark">V</div>
        <div className="brand-name">VAARL <span>WEB</span></div>
        <div className="top-note">Vehicle Assistance & Repair Locator</div>
      </header>

      <section className="hero">
        <div className="eyebrow">VEHICLE ASSISTANCE & REPAIR LOCATOR</div>
        <h1>Help for the road.<br/><span>When you need it.</span></h1>
        <p className="intro">Find repair assistance and connect with repair shops. The web experience is being built alongside the VAARL desktop application.</p>
        <div className="hero-actions">
          <button className="primary" disabled>Find a Repair Shop <span>→</span></button>
          <button className="secondary" disabled>Sign In</button>
        </div>
      </section>

      <section className="status-section">
        <div className="section-label">DEVELOPMENT CONNECTION STATUS</div>
        <div className="status-grid">
          <StatusCard title="Web API" state={api} />
          <StatusCard title="Shared Database" state={db} />
        </div>
      </section>

      <footer>VAARL Web System · Development preview</footer>
    </main>
  );
}

function StatusCard({ title, state }) {
  return (
    <article className="status-card">
      <div className="status-head">
        <h2>{title}</h2>
        <span className={'status-dot ' + (state.loading ? 'pending' : state.ok ? 'online' : 'offline')} />
      </div>
      <p>{state.message}</p>
    </article>
  );
}
