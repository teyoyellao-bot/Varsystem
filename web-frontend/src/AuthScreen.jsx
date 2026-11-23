import { useState } from 'react';

export default function AuthScreen() {
  const [mode, setMode] = useState('login');
  const [role, setRole] = useState('owner');
  const [notice, setNotice] = useState('');
  const [form, setForm] = useState({ name: '', email: '', username: '', phone: '', shopName: '', password: '', confirm: '' });
  const update = (key, value) => setForm(prev => ({ ...prev, [key]: value }));
  function submit(e) {
    e.preventDefault();
    setNotice('');
    if (mode === 'register' && form.password !== form.confirm) {
      setNotice('Passwords do not match. Please check and try again.');
      return;
    }
    setNotice('The interface is ready. Secure login and registration API will be enabled in the next backend step.');
  }
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
          <button type="button" className={mode === 'login' ? 'active' : ''} onClick={() => {setMode('login');setNotice('')}}>Sign In</button>
          <button type="button" className={mode === 'register' ? 'active' : ''} onClick={() => {setMode('register');setNotice('')}}>Create Account</button>
        </div>
        <h2>{mode === 'login' ? 'Welcome back' : 'Join VAARL'}</h2>
        <p className="auth-subtitle">{mode === 'login' ? 'Sign in to continue to your account.' : 'Create your account to get started.'}</p>
        {mode === 'register' && <>
          <div className="role-select">
            <button type="button" className={role === 'owner' ? 'selected' : ''} onClick={() => setRole('owner')}>Vehicle Owner</button>
            <button type="button" className={role === 'shop' ? 'selected' : ''} onClick={() => setRole('shop')}>Repair Shop</button>
          </div>
          <form onSubmit={submit}>
            <label>Full Name<input required autoComplete="name" value={form.name} onChange={e=>update('name',e.target.value)} placeholder="Enter full name"/></label>
            <label>Email Address<input type="email" required autoComplete="email" value={form.email} onChange={e=>update('email',e.target.value)} placeholder="you@example.com"/></label>
            <label>Username<input required autoComplete="username" value={form.username} onChange={e=>update('username',e.target.value)} placeholder="Choose username"/></label>
            <label>Phone Number<input required autoComplete="tel" value={form.phone} onChange={e=>update('phone',e.target.value)} placeholder="09xx xxx xxxx"/></label>
            {role === 'shop' && <label>Repair Shop Name<input required value={form.shopName} onChange={e=>update('shopName',e.target.value)} placeholder="Shop name"/></label>}
            <label>Password<input required type="password" autoComplete="new-password" value={form.password} onChange={e=>update('password',e.target.value)} placeholder="Create password"/></label>
            <label>Confirm Password<input required type="password" autoComplete="new-password" value={form.confirm} onChange={e=>update('confirm',e.target.value)} placeholder="Repeat password"/></label>
            <button className="auth-submit" type="submit">Create Account <span>→</span></button>
          </form>
        </>}
        {mode === 'login' && <form onSubmit={submit}>
          <label>Email or Username<input required autoComplete="username" value={form.username} onChange={e=>update('username',e.target.value)} placeholder="Enter email or username"/></label>
          <label>Password<input required type="password" autoComplete="current-password" value={form.password} onChange={e=>update('password',e.target.value)} placeholder="Enter password"/></label>
          <div className="forgot-row"><button type="button" onClick={()=>setNotice('Password recovery will be connected after authentication API is implemented.')}>Forgot password?</button></div>
          <button className="auth-submit" type="submit">Sign In <span>→</span></button>
        </form>}
        {notice && <div className="auth-notice" role="status">{notice}</div>}
        <div className="auth-footnote">Protected account access · VAARL Web</div>
      </div>
    </section>
  );
}
