import { useState, useEffect } from 'react';

const API_BASE = import.meta.env.VITE_VAARL_API_URL || 'http://localhost:8080';

const roleLabels = { VEHICLE_OWNER: 'Vehicle Owner', REPAIR_SHOP: 'Repair Shop', ADMIN: 'Administrator' };
const dashboardContent = {
  VEHICLE_OWNER: [
    ['Find Repair Shops', 'Explore repair shops and their services.'],
    ['My Assistance Requests', 'Review the status of your vehicle assistance requests.'],
    ['Request History', 'See your past assistance activity.'],
    ['Feedback & Ratings', 'Rate completed assistance.']
  ],
  REPAIR_SHOP: [
    ['Incoming Requests', 'Review assistance requests sent to your shop.'],
    ['Mechanics', 'Manage mechanic profiles and availability.'],
    ['Service Jobs', 'Track accepted and ongoing jobs.'],
    ['Shop History', 'Review completed assistance records.']
  ],
  ADMIN: [
    ['Account Management', 'Manage registered vehicle owners and repair shops.'],
    ['Shop Verification', 'Review repair shop account information.'],
    ['Support Requests', 'Review user support requests.'],
    ['System Overview', 'Review system activity and administration.']
  ]
};

export default function AuthScreen() {
  const [mode, setMode] = useState('login');
  const [role, setRole] = useState('VEHICLE_OWNER');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const [signedIn, setSignedIn] = useState(null);
  const [form, setForm] = useState({ name: '', email: '', username: '', phone: '', shopName: '', password: '', confirm: '' });
  const update = (key, value) => setForm(prev => ({ ...prev, [key]: value }));
  async function submit(e) {
    e.preventDefault(); setNotice(''); setBusy(true);
    try {
      const endpoint = mode === 'register' ? '/api/auth/register' : '/api/auth/login';
      const payload = mode === 'register'
        ? { fullName: form.name, email: form.email, username: form.username, phone: form.phone, shopName: role === 'REPAIR_SHOP' ? form.shopName : null, role, password: form.password, confirmPassword: form.confirm }
        : { usernameOrEmail: form.username, password: form.password };
      const response = await fetch(API_BASE + endpoint, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || 'Request failed. Check the server and your details.');
      if (mode === 'register') {
        setNotice(data.message || 'Account registered. Please sign in.');
        setMode('login'); setForm(prev => ({ ...prev, password: '', confirm: '' }));
      } else {
        sessionStorage.setItem('vaarl_auth_token', data.token);
        setSignedIn(data.user); setNotice('');
      }
    } catch (err) {
      setNotice(err.message || 'Unable to connect to VAARL API.');
    } finally { setBusy(false); }
  }
  function signOut() {
    sessionStorage.removeItem('vaarl_auth_token');
    setSignedIn(null); setNotice('Signed out.');
  }
  if (signedIn) return <RoleDashboard user={signedIn} signOut={signOut}/>;
  return (
    <section className="auth-wrap">
      <div className="auth-intro">
        <div className="eyebrow">VEHICLE ASSISTANCE & REPAIR LOCATOR</div>
        <h1>Find help.<br/><span>Get back on the road.</span></h1>
        <p>Discover repair shops, request vehicle assistance, and keep track of your service requests—all in one place.</p>
        <div className="auth-benefits">
          <div><span className="benefit-icon">⌖</span><div><b>Nearby repair assistance</b><small>Connect with repair shops</small></div></div>
          <div><span className="benefit-icon">✓</span><div><b>Track your requests</b><small>Stay updated on service progress</small></div></div>
          <div><span className="benefit-icon">◇</span><div><b>Trusted shop profiles</b><small>Explore services and ratings</small></div></div>
        </div>
      </div>
      <div className="auth-card">
        <div className="auth-tabs">
          <button type="button" className={mode === 'login' ? 'active' : ''} onClick={() => {setMode('login');setNotice('');}}>Sign In</button>
          <button type="button" className={mode === 'register' ? 'active' : ''} onClick={() => {setMode('register');setNotice('');}}>Create Account</button>
        </div>
        <h2>{mode === 'login' ? 'Welcome back' : 'Join VAARL'}</h2>
        <p className="auth-subtitle">{mode === 'login' ? 'Sign in to continue to your account.' : 'Create your account to get started.'}</p>
        {mode === 'register' && <div className="role-select">
          <button type="button" className={role === 'VEHICLE_OWNER' ? 'selected' : ''} onClick={() => setRole('VEHICLE_OWNER')}>Vehicle Owner</button>
          <button type="button" className={role === 'REPAIR_SHOP' ? 'selected' : ''} onClick={() => setRole('REPAIR_SHOP')}>Repair Shop</button>
        </div>}
        <form onSubmit={submit}>
          {mode === 'register' && <>
            <label>Full Name<input required maxLength={120} autoComplete="name" value={form.name} onChange={e=>update('name',e.target.value)} placeholder="Enter full name"/></label>
            <label>Email Address<input type="email" required autoComplete="email" value={form.email} onChange={e=>update('email',e.target.value)} placeholder="you@example.com"/></label>
            <label>Username<input required minLength={3} maxLength={60} autoComplete="username" value={form.username} onChange={e=>update('username',e.target.value)} placeholder="Choose username"/></label>
            <label>Phone Number<input required maxLength={40} autoComplete="tel" value={form.phone} onChange={e=>update('phone',e.target.value)} placeholder="09xx xxx xxxx"/></label>
            {role === 'REPAIR_SHOP' && <label>Repair Shop Name<input required maxLength={160} value={form.shopName} onChange={e=>update('shopName',e.target.value)} placeholder="Shop name"/></label>}
            <label>Password<input required minLength={8} maxLength={72} type="password" autoComplete="new-password" value={form.password} onChange={e=>update('password',e.target.value)} placeholder="Create password"/></label>
            <label>Confirm Password<input required type="password" autoComplete="new-password" value={form.confirm} onChange={e=>update('confirm',e.target.value)} placeholder="Repeat password"/></label>
          </>}
          {mode === 'login' && <>
            <label>Email or Username<input required autoComplete="username" value={form.username} onChange={e=>update('username',e.target.value)} placeholder="Enter email or username"/></label>
            <label>Password<input required type="password" autoComplete="current-password" value={form.password} onChange={e=>update('password',e.target.value)} placeholder="Enter password"/></label>
            <div className="forgot-row"><button type="button" onClick={()=>setNotice('Password recovery is not connected yet.')}>Forgot password?</button></div>
          </>}
          <button className="auth-submit" disabled={busy} type="submit">{busy ? 'Please wait…' : mode === 'login' ? 'Sign In' : 'Create Account'} <span>→</span></button>
        </form>
        {notice && <div className="auth-notice" role="status">{notice}</div>}
        <div className="auth-footnote">Protected account access · VAARL Web</div>
      </div>
    </section>
  );
}

function RoleDashboard({ user, signOut }) {
  const [active, setActive] = useState('Dashboard');
  const role = user.role || 'VEHICLE_OWNER';
  const modules = dashboardContent[role] || dashboardContent.VEHICLE_OWNER;
  const menu = ['Dashboard', ...modules.map(m => m[0]), 'My Profile'];
  return <section className="role-dashboard">
    <aside className="dashboard-sidebar">
      <div className="dash-brand"><span>V</span><div>VAARL <small>WEB</small></div></div>
      <div className="dash-role">{roleLabels[role] || role}</div>
      <nav>{menu.map(item=><button key={item} className={active===item?'dash-nav-active':''} onClick={()=>setActive(item)}>{item}</button>)}</nav>
      <button className="dash-logout" onClick={signOut}>Sign Out</button>
    </aside>
    <div className="dashboard-main">
      <header className="dash-header"><div><small>VAARL WEB / {roleLabels[role] || role}</small><h1>{active}</h1></div><div className="dash-user"><span>{(user.username||'U').slice(0,1).toUpperCase()}</span><div><b>{user.username}</b><small>{roleLabels[role] || role}</small></div></div></header>
      {active === 'Dashboard' ? <>
      <div className="dash-welcome"><div><small>WELCOME TO VAARL</small><h2>Good day, {user.username}.</h2><p>Choose a module below to get started.</p></div><div className="dash-graphic">V</div></div>
      <div className="dash-module-grid">{modules.map((m,i)=><button className="dash-module" key={m[0]} onClick={()=>setActive(m[0])}><span className="dash-module-number">0{i+1}</span><h3>{m[0]}</h3><p>{m[1]}</p><span className="dash-open">Open module ↗</span></button>)}</div>
      </> : active === 'My Profile' ? <div className="dash-panel"><h2>Account Profile</h2><p><b>Username:</b> {user.username}</p><p><b>Email:</b> {user.email}</p><p><b>Role:</b> {roleLabels[role] || role}</p><p className="dash-hint">Profile editing will be connected to the account API in a later step.</p></div> : active === 'Find Repair Shops' ? <FindRepairShops /> : <div className="dash-panel"><div className="dash-module-number">VAARL MODULE</div><h2>{active}</h2><p>{modules.find(m=>m[0]===active)?.[1] || 'Account module'}</p><div className="dash-hint">Dashboard navigation is ready. Live database features for this module are the next integration step.</div><button className="dash-back" onClick={()=>setActive('Dashboard')}>← Back to Dashboard</button></div>}
      <footer className="dash-footer">VAARL Web System · Development Preview</footer>
    </div>
  </section>;
}


function FindRepairShops() {
  const [shops, setShops] = useState([]);
  const [search, setSearch] = useState('');
  const [busy, setBusy] = useState(true);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState(null);

  useEffect(() => {
    let ignore = false;
    async function load() {
      setBusy(true); setError('');
      try {
        const token = sessionStorage.getItem('vaarl_auth_token');
        const response = await fetch(API_BASE + '/api/shops?q=' + encodeURIComponent(search), {
          headers: token ? { Authorization: 'Bearer ' + token } : {}
        });
        const data = await response.json().catch(() => []);
        if (!response.ok) throw new Error(data.message || 'Unable to load repair shops.');
        if (!ignore) setShops(Array.isArray(data) ? data : []);
      } catch (err) {
        if (!ignore) setError(err.message || 'Could not connect to the shops API.');
      } finally { if (!ignore) setBusy(false); }
    }
    load();
    return () => { ignore = true; };
  }, [search]);

  return <div className="shops-module">
    <div className="shops-toolbar">
      <div><h2>Find Repair Shops</h2><p>Search active repair shops registered in VAARL.</p></div>
      <input aria-label="Search repair shops" value={search} onChange={e=>setSearch(e.target.value)} placeholder="Search shop name or location…" />
    </div>
    {busy ? <div className="shops-state">Loading repair shops…</div> :
      error ? <div className="shops-state shops-error">{error}<p>Ensure the VAARL API and database are running.</p></div> :
      shops.length === 0 ? <div className="shops-state">No active repair shops found{search ? ' matching your search.' : ' in the database yet.'}</div> :
      <div className="shops-grid">{shops.map(shop=><article className="shop-card" key={shop.id}>
        <div className="shop-card-top"><span className="shop-mark">V</span><span className="shop-active">Active</span></div>
        <h3>{shop.shopName}</h3><p className="shop-location">{shop.location || 'Location not provided'}</p>
        <div className="shop-rating">★ {Number(shop.rating || 0).toFixed(1)} <span>({shop.reviewCount || 0} reviews)</span></div>
        <div className="shop-services">{(shop.services || []).length ? shop.services.slice(0,4).map(s=><span key={s}>{s}</span>) : <span>Services not listed</span>}</div>
        <button className="shop-details-button" onClick={()=>setSelected(shop)}>View Details</button>
      </article>)}</div>}
    {selected && <div className="shop-detail-overlay" role="dialog" aria-modal="true" aria-label="Repair shop details">
      <div className="shop-detail-modal"><button className="shop-detail-close" onClick={()=>setSelected(null)} aria-label="Close">×</button>
        <span className="shop-active">Active Shop</span><h2>{selected.shopName}</h2>
        <p>{selected.location || 'Location not provided'}</p><p><b>Phone:</b> {selected.phone || 'Not provided'}</p><p><b>Hours:</b> {selected.hours || 'Not provided'}</p>
        <p><b>About:</b> {selected.about || 'No shop description yet.'}</p><p><b>Rating:</b> ★ {Number(selected.rating || 0).toFixed(1)} ({selected.reviewCount || 0} reviews)</p>
        <h3>Services</h3><div className="shop-services-detail">{(selected.services || []).length ? selected.services.map(s=><span key={s}>{s}</span>) : 'No services listed yet.'}</div>
        <div className="dash-hint">Assistance request and mechanic selection will be connected in the next module.</div>
        <button className="dash-back" onClick={()=>setSelected(null)}>Close Details</button>
      </div></div>}
  </div>;
}
