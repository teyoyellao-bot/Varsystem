import { useState } from 'react';

const API_BASE = import.meta.env.VITE_VAARL_API_URL || 'http://localhost:8080';

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
        setSignedIn(data.user); setNotice('Signed in successfully.');
      }
    } catch (err) {
      setNotice(err.message || 'Unable to connect to VAARL API.');
    } finally { setBusy(false); }
  }
  if (signedIn) return <section className="auth-wrap"><div className="auth-intro"><div className="eyebrow">VAARL WEB</div><h1>Welcome,<br/><span>{signedIn.username}</span></h1><p>Account role: {signedIn.role}</p></div><div className="auth-card"><h2>Signed in</h2><p className="auth-subtitle">Your account login was successful. Dashboard modules are the next development step.</p><div className="auth-notice">{notice}</div><button className="auth-submit" onClick={() => {sessionStorage.removeItem('vaarl_auth_token');setSignedIn(null);setNotice('You have signed out.');}}>Sign Out</button></div></section>;
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
