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
      </> : active === 'My Profile' ? <div className="dash-panel"><h2>Account Profile</h2><p><b>Username:</b> {user.username}</p><p><b>Email:</b> {user.email}</p><p><b>Role:</b> {roleLabels[role] || role}</p><p className="dash-hint">Profile editing will be connected to the account API in a later step.</p></div> : active === 'Find Repair Shops' ? <FindRepairShops /> : active === 'My Assistance Requests' ? <MyAssistanceRequests /> : active === 'Incoming Requests' ? <ShopIncomingRequests /> : active === 'Mechanics' ? <ShopMechanics /> : active === 'Service Jobs' ? <ShopServiceJobs /> : <div className="dash-panel"><div className="dash-module-number">VAARL MODULE</div><h2>{active}</h2><p>{modules.find(m=>m[0]===active)?.[1] || 'Account module'}</p><div className="dash-hint">Dashboard navigation is ready. Live database features for this module are the next integration step.</div><button className="dash-back" onClick={()=>setActive('Dashboard')}>← Back to Dashboard</button></div>}
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
  const [shopDetail, setShopDetail] = useState(null);
  const [detailBusy, setDetailBusy] = useState(false);
  const [detailError, setDetailError] = useState('');
  const [assistOpen, setAssistOpen] = useState(false);
  const [assistBusy, setAssistBusy] = useState(false);
  const [assistNotice, setAssistNotice] = useState('');
  const [assistForm, setAssistForm] = useState({ mechanicId: '', vehicle: '', problem: '', location: '', details: '', paymentMethod: 'Cash / GCash' });
  const assistUpdate = (key, value) => setAssistForm(prev => ({ ...prev, [key]: value }));
  async function submitAssistance(e) {
    e.preventDefault(); setAssistBusy(true); setAssistNotice('');
    try {
      const token = sessionStorage.getItem('vaarl_auth_token');
      const response = await fetch(API_BASE + '/api/assistance-requests', { method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token }, body: JSON.stringify({ shopId: selected.id, ...assistForm }) });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || 'Unable to submit assistance request.');
      setAssistNotice(data.message || 'Assistance request submitted.');
      setAssistForm({ mechanicId: '', vehicle: '', problem: '', location: '', details: '', paymentMethod: 'Cash / GCash' });
    } catch (err) { setAssistNotice(err.message || 'Request submission failed.'); }
    finally { setAssistBusy(false); }
  }

  useEffect(() => {
    if (!selected) { setShopDetail(null); setDetailError(''); return; }
    let ignore = false;
    async function loadDetails() {
      setDetailBusy(true); setDetailError('');
      try {
        const token = sessionStorage.getItem('vaarl_auth_token');
        const response = await fetch(API_BASE + '/api/shops/' + encodeURIComponent(selected.id), { headers: token ? { Authorization: 'Bearer ' + token } : {} });
        const data = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(data.message || 'Unable to load shop details.');
        if (!ignore) setShopDetail(data);
      } catch (err) { if (!ignore) setDetailError(err.message || 'Could not load details.'); }
      finally { if (!ignore) setDetailBusy(false); }
    }
    loadDetails();
    return () => { ignore = true; };
  }, [selected]);

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
        {detailBusy ? <div className="shops-state">Loading shop details…</div> : detailError ? <div className="shops-state shops-error">{detailError}</div> : <>
          <span className="shop-active">Active Shop</span><h2>{(shopDetail || selected).shopName}</h2>
          <p>{(shopDetail || selected).location || 'Location not provided'}</p><p><b>Phone:</b> {(shopDetail || selected).phone || 'Not provided'}</p><p><b>Hours:</b> {(shopDetail || selected).hours || 'Not provided'}</p>
          <p><b>About:</b> {(shopDetail || selected).about || 'No shop description yet.'}</p><p><b>Rating:</b> ★ {Number((shopDetail || selected).rating || 0).toFixed(1)} ({(shopDetail || selected).reviewCount || 0} reviews)</p>
          <h3>Services</h3><div className="shop-services-detail">{((shopDetail || selected).services || []).length ? (shopDetail || selected).services.map(s=><span key={s}>{s}</span>) : 'No services listed yet.'}</div>
          <h3>Available Mechanics</h3><div className="shop-mechanics-list">{((shopDetail || {}).availableMechanics || []).length ? (shopDetail.availableMechanics).map(m=><div className="shop-mechanic-row" key={m.id}><b>{m.name}</b><span>{m.availability}</span></div>) : 'No mechanics currently marked available.'}</div>
          <button className="shop-details-button" onClick={() => {setAssistOpen(true);setAssistNotice('');}}>Request Assistance</button>
        </>}
        {assistOpen && <div className="assist-form-wrap"><h3>Request Vehicle Assistance</h3><p>Fill in your vehicle and assistance details.</p><form className="assist-form" onSubmit={submitAssistance}>{((shopDetail || {}).availableMechanics || []).length > 0 && <label>Mechanic<select value={assistForm.mechanicId} onChange={e=>assistUpdate('mechanicId',e.target.value)}><option value="">No preference</option>{shopDetail.availableMechanics.map(m=><option key={m.id} value={m.id}>{m.name}</option>)}</select></label>}<label>Vehicle Information<input required maxLength={120} value={assistForm.vehicle} onChange={e=>assistUpdate('vehicle',e.target.value)} placeholder="Vehicle make/model/year"/></label><label>Problem<input required maxLength={255} value={assistForm.problem} onChange={e=>assistUpdate('problem',e.target.value)} placeholder="Describe the issue briefly"/></label><label>Location<input required maxLength={255} value={assistForm.location} onChange={e=>assistUpdate('location',e.target.value)} placeholder="Where is assistance needed?"/></label><label>Additional Details<textarea maxLength={2000} rows={3} value={assistForm.details} onChange={e=>assistUpdate('details',e.target.value)} placeholder="Additional information (optional)"/></label><label>Payment Method<select value={assistForm.paymentMethod} onChange={e=>assistUpdate('paymentMethod',e.target.value)}><option>Cash / GCash</option><option>Cash</option><option>GCash</option></select></label><button className="shop-details-button" disabled={assistBusy} type="submit">{assistBusy ? 'Submitting…' : 'Submit Request'}</button></form>{assistNotice && <div className="auth-notice" role="status">{assistNotice}</div>}</div>}
        <button className="dash-back" onClick={()=>{setAssistOpen(false);setSelected(null);}}>Close Details</button>
      </div></div>}
  </div>;
}

function MyAssistanceRequests() {
  const [items, setItems] = useState([]);
  const [busy, setBusy] = useState(true);
  const [error, setError] = useState('');
  useEffect(() => {
    let ignore = false;
    async function load() {
      setBusy(true); setError('');
      try {
        const token = sessionStorage.getItem('vaarl_auth_token');
        const response = await fetch(API_BASE + '/api/assistance-requests', { headers: token ? { Authorization: 'Bearer ' + token } : {} });
        const data = await response.json().catch(() => []);
        if (!response.ok) throw new Error(data.message || 'Unable to load requests.');
        if (!ignore) setItems(Array.isArray(data) ? data : []);
      } catch (err) { if (!ignore) setError(err.message || 'Unable to connect to requests API.'); }
      finally { if (!ignore) setBusy(false); }
    }
    load();
    return () => { ignore = true; };
  }, []);
  const statusClass = status => String(status || 'Pending').toLowerCase().replace(/[^a-z]/g, '-');
  return <div className="request-module"><div className="request-toolbar"><div><h2>My Assistance Requests</h2><p>Track the requests you submitted to repair shops.</p></div></div>
    {busy ? <div className="shops-state">Loading your requests…</div> : error ? <div className="shops-state shops-error">{error}</div> : items.length === 0 ? <div className="shops-state">You haven't submitted any assistance requests yet.</div> :
      <div className="request-list">{items.map(r=><article className="request-card" key={r.id}><header><div><small>REQUEST ID</small><strong>{r.id}</strong></div><span className={'request-status '+statusClass(r.status)}>{r.status}</span></header><h3>{r.shopName}</h3><div className="request-detail-grid"><div><small>Vehicle</small><b>{r.vehicle || '—'}</b></div><div><small>Problem</small><b>{r.problem || '—'}</b></div><div><small>Mechanic</small><b>{r.mechanicName || 'No preference'}</b></div><div><small>Location</small><b>{r.location || '—'}</b></div><div><small>Payment</small><b>{r.paymentMethod || '—'}</b></div><div><small>Submitted</small><b>{r.submittedAt ? new Date(r.submittedAt).toLocaleString() : '—'}</b></div></div>{r.details && <p className="request-extra">{r.details}</p>}<div className="request-status-track"><span className={['Pending','Accepted','On Going','Completed'].includes(r.status) ? 'step-current' : ''}>Pending</span><span className={['Accepted','On Going','Completed'].includes(r.status) ? 'step-current' : ''}>Accepted</span><span className={['On Going','Completed'].includes(r.status) ? 'step-current' : ''}>On Going</span><span className={r.status === 'Completed' ? 'step-current' : ''}>Completed</span></div></article>)}</div>}
  </div>;
}

function ShopIncomingRequests() {
  const [items, setItems] = useState([]);
  const [busy, setBusy] = useState(true);
  const [error, setError] = useState('');
  const [workingId, setWorkingId] = useState('');
  const [notice, setNotice] = useState('');
  async function load() {
    setBusy(true); setError('');
    try {
      const token = sessionStorage.getItem('vaarl_auth_token');
      const response = await fetch(API_BASE + '/api/shop/requests', { headers: token ? { Authorization: 'Bearer ' + token } : {} });
      const data = await response.json().catch(() => []);
      if (!response.ok) throw new Error(data.message || 'Unable to load incoming requests.');
      setItems(Array.isArray(data) ? data : []);
    } catch (err) { setError(err.message || 'Could not connect to requests API.'); }
    finally { setBusy(false); }
  }
  useEffect(() => { load(); }, []);
  async function decide(id, decision) {
    setWorkingId(id); setNotice('');
    try {
      const token = sessionStorage.getItem('vaarl_auth_token');
      const response = await fetch(API_BASE + '/api/shop/requests/' + encodeURIComponent(id) + '/decision', {
        method: 'PATCH', headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token },
        body: JSON.stringify({ decision })
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || 'Unable to update request.');
      setNotice(data.message || 'Request updated.');
      await load();
    } catch (err) { setNotice(err.message || 'Action failed.'); }
    finally { setWorkingId(''); }
  }
  const pending = items.filter(r => r.status === 'Pending').length;
  return <div className="request-module shop-incoming-module">
    <div className="request-toolbar"><div><h2>Incoming Requests</h2><p>Review assistance requests sent to your repair shop.</p></div><div className="incoming-count">{pending} pending</div></div>
    {notice && <div className="auth-notice" role="status">{notice}</div>}
    {busy ? <div className="shops-state">Loading incoming requests…</div> : error ? <div className="shops-state shops-error">{error}</div> : items.length === 0 ? <div className="shops-state">No assistance requests have been sent to your shop.</div> :
      <div className="request-list">{items.map(r=><article className="request-card" key={r.id}><header><div><small>REQUEST ID</small><strong>{r.id}</strong></div><span className={'request-status '+String(r.status||'Pending').toLowerCase().replace(/[^a-z]/g,'-')}>{r.status}</span></header><h3>{r.customerName || 'Customer'}</h3><div className="request-detail-grid"><div><small>Phone</small><b>{r.customerPhone || '—'}</b></div><div><small>Vehicle</small><b>{r.vehicle || '—'}</b></div><div><small>Problem</small><b>{r.problem || '—'}</b></div><div><small>Assigned Mechanic</small><b>{r.mechanicName || 'No preference'}</b></div><div><small>Location</small><b>{r.location || '—'}</b></div><div><small>Payment</small><b>{r.paymentMethod || '—'}</b></div><div><small>Submitted</small><b>{r.submittedAt ? new Date(r.submittedAt).toLocaleString() : '—'}</b></div></div>{r.details && <p className="request-extra">{r.details}</p>}{r.status === 'Pending' && <div className="incoming-actions"><button disabled={workingId===r.id} onClick={()=>decide(r.id,'accept')}>{workingId===r.id?'Updating…':'Accept Request'}</button><button disabled={workingId===r.id} className="incoming-decline" onClick={()=>decide(r.id,'decline')}>Decline</button></div>}</article>)}</div>}
  </div>;
}

function ShopMechanics() {
  const [items, setItems] = useState([]);
  const [busy, setBusy] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [saving, setSaving] = useState(false);
  const [editingId, setEditingId] = useState('');
  const [form, setForm] = useState({name:'', phone:'', availability:'Available'});
  async function load() {
    setBusy(true); setError('');
    try {
      const token = sessionStorage.getItem('vaarl_auth_token');
      const response = await fetch(API_BASE + '/api/shop/mechanics', {headers: token ? {Authorization:'Bearer '+token} : {}});
      const data = await response.json().catch(()=>[]);
      if(!response.ok) throw new Error(data.message || 'Unable to load mechanics.');
      setItems(Array.isArray(data)?data:[]);
    } catch(err) { setError(err.message || 'Mechanics API error.'); }
    finally { setBusy(false); }
  }
  useEffect(()=>{load();},[]);
  function resetForm(){setEditingId('');setForm({name:'',phone:'',availability:'Available'});}
  function startEdit(m){setEditingId(m.id);setForm({name:m.name||'',phone:m.phone||'',availability:m.availability||'Available'});setNotice('');}
  async function save(e){
    e.preventDefault();setSaving(true);setNotice('');
    try{
      const token=sessionStorage.getItem('vaarl_auth_token');
      const response=await fetch(API_BASE+(editingId?'/api/shop/mechanics/'+encodeURIComponent(editingId):'/api/shop/mechanics'),{
        method:editingId?'PUT':'POST',headers:{'Content-Type':'application/json',Authorization:'Bearer '+token},body:JSON.stringify(form)
      });
      const data=await response.json().catch(()=>({}));
      if(!response.ok)throw new Error(data.message||'Unable to save mechanic.');
      setNotice(editingId?'Mechanic updated.':'Mechanic added.');resetForm();await load();
    }catch(err){setNotice(err.message||'Save failed.');}
    finally{setSaving(false);}
  }
  async function remove(id){
    if(!window.confirm('Remove this mechanic from your shop?'))return;
    setNotice('');
    try{
      const token=sessionStorage.getItem('vaarl_auth_token');
      const response=await fetch(API_BASE+'/api/shop/mechanics/'+encodeURIComponent(id),{method:'DELETE',headers:{Authorization:'Bearer '+token}});
      const data=await response.json().catch(()=>({}));
      if(!response.ok)throw new Error(data.message||'Unable to remove mechanic.');
      setNotice(data.message||'Mechanic removed.');await load();
    }catch(err){setNotice(err.message||'Remove failed.');}
  }
  return <div className="mechanics-module">
    <div className="request-toolbar"><div><h2>Mechanics Management</h2><p>Add mechanics and manage their availability for assistance requests.</p></div><div className="incoming-count">{items.length} mechanics</div></div>
    {notice&&<div className="auth-notice" role="status">{notice}</div>}
    <form className="mechanic-form" onSubmit={save}><h3>{editingId?'Edit Mechanic':'Add Mechanic'}</h3>
      <label>Mechanic Name<input required maxLength={120} value={form.name} onChange={e=>setForm(p=>({...p,name:e.target.value}))} placeholder="Full name"/></label>
      <label>Phone Number<input maxLength={40} value={form.phone} onChange={e=>setForm(p=>({...p,phone:e.target.value}))} placeholder="Contact number (optional)"/></label>
      <label>Availability<select value={form.availability} onChange={e=>setForm(p=>({...p,availability:e.target.value}))}><option>Available</option><option>Busy</option></select></label>
      <div className="mechanic-form-actions"><button type="submit" disabled={saving}>{saving?'Saving…':editingId?'Save Changes':'Add Mechanic'}</button>{editingId&&<button type="button" className="mechanic-cancel" onClick={resetForm}>Cancel Edit</button>}</div>
    </form>
    <h3 className="mechanics-list-title">Shop Mechanics</h3>
    {busy?<div className="shops-state">Loading mechanics…</div>:error?<div className="shops-state shops-error">{error}</div>:items.length===0?<div className="shops-state">No mechanics added yet.</div>:
      <div className="mechanic-cards">{items.map(m=><article className="mechanic-card" key={m.id}><header><div className="mechanic-avatar">{(m.name||'M').slice(0,1).toUpperCase()}</div><span className={'mechanic-availability '+String(m.availability).toLowerCase()}>{m.availability}</span></header><h3>{m.name}</h3><p>{m.phone||'No phone provided'}</p><div className="mechanic-card-actions"><button onClick={()=>startEdit(m)}>Edit</button><button className="mechanic-remove" onClick={()=>remove(m.id)}>Remove</button></div></article>)}</div>}
  </div>;
}

function ShopServiceJobs() {
  const [jobs,setJobs]=useState([]);
  const [busy,setBusy]=useState(true);
  const [error,setError]=useState('');
  const [notice,setNotice]=useState('');
  const [working,setWorking]=useState('');
  async function load(){
    setBusy(true);setError('');
    try{
      const token=sessionStorage.getItem('vaarl_auth_token');
      const res=await fetch(API_BASE+'/api/shop/jobs',{headers:token?{Authorization:'Bearer '+token}:{}});
      const data=await res.json().catch(()=>[]);
      if(!res.ok)throw new Error(data.message||'Unable to load service jobs.');
      setJobs(Array.isArray(data)?data:[]);
    }catch(e){setError(e.message||'Service jobs API error.');}
    finally{setBusy(false);}
  }
  useEffect(()=>{load();},[]);
  async function advance(job){
    const next=job.status==='Accepted'?'On Going':'Completed';
    setWorking(job.id);setNotice('');
    try{
      const token=sessionStorage.getItem('vaarl_auth_token');
      const res=await fetch(API_BASE+'/api/shop/jobs/'+encodeURIComponent(job.id)+'/status',{method:'PATCH',headers:{'Content-Type':'application/json',Authorization:'Bearer '+token},body:JSON.stringify({status:next})});
      const data=await res.json().catch(()=>({}));
      if(!res.ok)throw new Error(data.message||'Could not update job status.');
      setNotice(data.message||'Service job updated.');await load();
    }catch(e){setNotice(e.message||'Update failed.');}
    finally{setWorking('');}
  }
  return <div className="request-module">
    <div className="request-toolbar"><div><h2>Service Jobs</h2><p>Manage accepted assistance requests and update service progress.</p></div><div className="incoming-count">{jobs.length} active jobs</div></div>
    {notice&&<div className="auth-notice" role="status">{notice}</div>}
    {busy?<div className="shops-state">Loading service jobs…</div>:error?<div className="shops-state shops-error">{error}</div>:jobs.length===0?<div className="shops-state">No accepted or ongoing jobs at this time.</div>:
    <div className="request-list">{jobs.map(j=><article className="request-card" key={j.id}><header><div><small>REQUEST ID</small><strong>{j.id}</strong></div><span className={'request-status '+String(j.status).toLowerCase().replace(/[^a-z]/g,'-')}>{j.status}</span></header><h3>{j.customerName||'Customer'}</h3><div className="request-detail-grid"><div><small>Phone</small><b>{j.customerPhone||'—'}</b></div><div><small>Vehicle</small><b>{j.vehicle||'—'}</b></div><div><small>Problem</small><b>{j.problem||'—'}</b></div><div><small>Mechanic</small><b>{j.mechanicName||'Unassigned'}</b></div><div><small>Location</small><b>{j.location||'—'}</b></div><div><small>Payment</small><b>{j.paymentMethod||'—'}</b></div><div><small>Accepted</small><b>{j.acceptedAt?new Date(j.acceptedAt).toLocaleString():'—'}</b></div></div>{j.details&&<p className="request-extra">{j.details}</p>}<div className="service-job-actions"><button disabled={working===j.id} onClick={()=>advance(j)}>{working===j.id?'Updating…':j.status==='Accepted'?'Start Service':'Mark Completed'}</button></div></article>)}</div>}
  </div>;
}
