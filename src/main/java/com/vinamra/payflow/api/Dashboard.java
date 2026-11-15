package com.vinamra.payflow.api;

/** The web app: one self-contained HTML page (inline SVG art, no libraries, no external files) served at "/". */
final class Dashboard {
    private Dashboard() {
    }

    static final String HTML = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>PayFlow</title>
<style>
  :root {
    --bg: #f3f6fc; --card: #ffffff; --text: #18202f; --muted: #667085; --line: #e2e8f2;
    --brand: #0b5cff; --brand2: #00b4ff; --ok: #0a7a3d; --okbg: #e4f6ea; --err: #b42318; --errbg: #fdeceb;
    --shadow: 0 6px 24px rgba(20, 40, 90, 0.08);
  }
  @media (prefers-color-scheme: dark) {
    :root { --bg: #0e1320; --card: #161d2d; --text: #e8ecf5; --muted: #99a3b6; --line: #28324a;
            --brand: #5b8cff; --brand2: #36c2ff; --ok: #5fd08a; --okbg: #11301f; --err: #ff8b80; --errbg: #3a1714;
            --shadow: 0 6px 24px rgba(0, 0, 0, 0.35); }
  }
  [hidden] { display: none !important; }
  * { box-sizing: border-box; }
  body { margin: 0; font-family: Arial, Helvetica, sans-serif; background: var(--bg); color: var(--text); }
  button { font-family: inherit; cursor: pointer; }
  .ico { width: 18px; height: 18px; fill: none; stroke: currentColor; stroke-width: 2;
         stroke-linecap: round; stroke-linejoin: round; vertical-align: middle; }

  /* ---------- login / register ---------- */
  .auth { min-height: 100vh; display: grid; grid-template-columns: 1.1fr 1fr; }
  .hero { background: linear-gradient(145deg, #0b3fd0, #0b8cff 60%, #29d0ff); color: #fff; padding: 48px 40px;
          display: flex; flex-direction: column; justify-content: center; }
  .hero h1 { font-size: 34px; margin: 18px 0 8px; }
  .hero p { font-size: 16px; opacity: 0.92; max-width: 460px; line-height: 1.5; }
  .hero svg.art { width: 100%; max-width: 420px; margin: 0 auto 8px; display: block; }
  .perks { list-style: none; padding: 0; margin: 22px 0 0; display: grid; gap: 12px; max-width: 460px; }
  .perks li { display: flex; gap: 12px; align-items: flex-start; font-size: 14px; line-height: 1.4; }
  .perks .dot { flex: none; width: 32px; height: 32px; border-radius: 10px; background: rgba(255,255,255,0.18);
                display: grid; place-items: center; }
  .formside { display: grid; place-items: center; padding: 32px 20px; }
  .authcard { width: 100%; max-width: 400px; background: var(--card); border: 1px solid var(--line);
              border-radius: 16px; padding: 26px; box-shadow: var(--shadow); }
  .brandrow { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; }
  .brandrow b { font-size: 22px; }
  .tabs { display: flex; background: var(--bg); border-radius: 10px; padding: 4px; margin: 16px 0 6px; }
  .tabs button { flex: 1; border: 0; background: transparent; color: var(--muted); padding: 9px; border-radius: 8px;
                 font-size: 14px; }
  .tabs button.on { background: var(--card); color: var(--text); box-shadow: 0 1px 4px rgba(0,0,0,0.12); font-weight: bold; }
  label { display: block; font-size: 12px; color: var(--muted); margin: 12px 0 4px; }
  input, select { width: 100%; padding: 10px 12px; border: 1px solid var(--line); border-radius: 8px;
                  background: var(--bg); color: var(--text); font-size: 14px; }
  input:focus, select:focus { outline: 2px solid var(--brand); outline-offset: -1px; }
  .btn { margin-top: 16px; width: 100%; padding: 11px 14px; border: 0; border-radius: 9px; color: #fff; font-size: 15px;
         background: linear-gradient(135deg, var(--brand), var(--brand2)); }
  .btn:hover { filter: brightness(1.07); }
  .btn.small { width: auto; margin-top: 10px; padding: 9px 16px; font-size: 14px; }
  .btn.ghost { background: transparent; color: var(--brand); border: 1px solid var(--brand); }
  .showpw { display: flex; align-items: center; gap: 6px; margin: 8px 0 0; font-size: 12px; color: var(--muted); }
  .showpw input { width: auto; }
  .autherr { color: var(--err); background: var(--errbg); border-radius: 8px; padding: 9px 11px; font-size: 13px;
             margin-top: 12px; }
  .note { font-size: 12px; color: var(--muted); margin-top: 14px; line-height: 1.5; }

  /* ---------- app ---------- */
  .topbar { display: flex; align-items: center; justify-content: space-between; padding: 12px 24px;
            background: var(--card); border-bottom: 1px solid var(--line); position: sticky; top: 0; z-index: 5; }
  .who { display: flex; align-items: center; gap: 10px; font-size: 14px; }
  .avatar { width: 36px; height: 36px; border-radius: 50%; color: #fff; display: grid; place-items: center;
            font-weight: bold; font-size: 14px; }
  .logout { border: 1px solid var(--line); background: transparent; color: var(--text); padding: 7px 12px;
            border-radius: 8px; font-size: 13px; display: flex; align-items: center; gap: 6px; }
  .logout:hover { border-color: var(--brand); color: var(--brand); }
  main { max-width: 1120px; margin: 0 auto; padding: 22px 20px 60px; }
  .welcome h2 { margin: 0; font-size: 24px; }
  .welcome p { margin: 4px 0 0; color: var(--muted); font-size: 14px; }
  .tiles { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 14px; margin: 18px 0; }
  .tile { background: var(--card); border: 1px solid var(--line); border-radius: 14px; padding: 16px; box-shadow: var(--shadow); }
  .tile small { color: var(--muted); font-size: 12px; }
  .tile div { font-size: 26px; font-weight: bold; margin-top: 6px; }
  h3 { margin: 26px 0 12px; font-size: 16px; }
  .cards { display: flex; gap: 14px; overflow-x: auto; padding: 4px 4px 12px; }
  .acard { flex: none; width: 270px; min-height: 160px; border-radius: 16px; padding: 16px; color: #fff; position: relative;
           cursor: pointer; border: 3px solid transparent; transition: transform 0.15s, box-shadow 0.15s; box-shadow: var(--shadow); }
  .acard:hover { transform: translateY(-3px); }
  .acard.sel { border-color: var(--text); box-shadow: 0 10px 30px rgba(11, 92, 255, 0.35); }
  .c0 { background: linear-gradient(135deg, #0b5cff, #00b4ff); }
  .c1 { background: linear-gradient(135deg, #6a3df0, #c24dff); }
  .c2 { background: linear-gradient(135deg, #009e78, #27d4a6); }
  .acard .top { display: flex; justify-content: space-between; align-items: center; font-size: 13px; opacity: 0.95; }
  .acard .abal { font-size: 28px; font-weight: bold; margin: 18px 0 10px; }
  .acard .aid { display: flex; align-items: center; gap: 8px; font-size: 13px; letter-spacing: 1px; }
  .acard .aown { font-size: 12px; opacity: 0.9; margin-top: 6px; }
  .copy { border: 0; background: rgba(255,255,255,0.22); color: #fff; width: 28px; height: 28px; border-radius: 8px;
          display: grid; place-items: center; }
  .copy:hover { background: rgba(255,255,255,0.38); }
  .acard.add { background: transparent; color: var(--brand); border: 2px dashed var(--brand); display: grid;
               place-items: center; text-align: center; font-size: 15px; box-shadow: none; }
  .empty { background: var(--card); border: 1px dashed var(--line); border-radius: 14px; padding: 26px; text-align: center;
           color: var(--muted); font-size: 14px; }
  .empty svg { width: 120px; margin-bottom: 8px; }
  .panel { background: var(--card); border: 1px solid var(--line); border-radius: 14px; padding: 18px; box-shadow: var(--shadow); }
  .panels { display: grid; grid-template-columns: repeat(auto-fit, minmax(340px, 1fr)); gap: 16px; }
  .modes { display: flex; gap: 8px; margin-bottom: 4px; }
  .modes button { flex: 1; border: 1px solid var(--line); background: transparent; color: var(--text); border-radius: 10px;
                  padding: 10px 6px; font-size: 14px; display: flex; gap: 6px; align-items: center; justify-content: center; }
  .modes button.on { background: linear-gradient(135deg, var(--brand), var(--brand2)); color: #fff; border-color: transparent; }
  .chips { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
  .chip { border: 1px solid var(--line); background: var(--bg); color: var(--text); border-radius: 999px; padding: 4px 11px;
          font-size: 12px; }
  .chip:hover { border-color: var(--brand); color: var(--brand); }
  .rinfo { font-size: 13px; margin-top: 6px; min-height: 18px; }
  .rinfo.good { color: var(--ok); }
  .rinfo.bad { color: var(--err); }
  .hint { font-size: 12px; color: var(--muted); margin-top: 10px; line-height: 1.5; }
  .stress { font-size: 13px; margin-top: 12px; line-height: 1.5; }
  table { width: 100%; border-collapse: collapse; font-size: 13px; }
  th, td { text-align: left; padding: 9px 6px; border-bottom: 1px solid var(--line); }
  th { color: var(--muted); font-weight: normal; font-size: 12px; }
  .num { text-align: right; font-weight: bold; }
  .plus { color: var(--ok); }
  .minus { color: var(--err); }
  .badge { display: inline-flex; align-items: center; gap: 6px; }
  .badge i { width: 24px; height: 24px; border-radius: 8px; display: grid; place-items: center; }
  .badge i.in { background: var(--okbg); color: var(--ok); }
  .badge i.out { background: var(--errbg); color: var(--err); }
  .scroll { overflow-x: auto; }
  #toasts { position: fixed; right: 16px; bottom: 16px; display: grid; gap: 8px; z-index: 20; max-width: 360px; }
  .toast { padding: 11px 14px; border-radius: 10px; font-size: 13px; line-height: 1.4; box-shadow: var(--shadow);
           animation: slide 0.25s ease-out; border: 1px solid transparent; }
  .toast.ok { background: var(--okbg); color: var(--ok); border-color: var(--ok); }
  .toast.err { background: var(--errbg); color: var(--err); border-color: var(--err); }
  @keyframes slide { from { transform: translateX(30px); opacity: 0; } to { transform: none; opacity: 1; } }
  .atabs { display: flex; gap: 8px; margin-bottom: 12px; flex-wrap: wrap; }
  .atabs button { border: 1px solid var(--line); background: transparent; color: var(--text); border-radius: 10px;
                  padding: 9px 16px; font-size: 14px; }
  .atabs button.on { background: linear-gradient(135deg, var(--brand), var(--brand2)); color: #fff; border-color: transparent; }
  .filters { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; margin-bottom: 12px; }
  .filters input[type=text], .filters select { width: auto; min-width: 170px; }
  .filters label { margin: 0; display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--muted); }
  .filters input[type=checkbox] { width: auto; }
  .tag { display: inline-block; padding: 2px 9px; border-radius: 999px; font-size: 11px; color: #fff; }
  .tag.deposit { background: #0a9b57; }
  .tag.withdrawal { background: #d4380d; }
  .tag.transfer { background: #2f6bff; }
  .tag.owner { background: linear-gradient(135deg, #b8860b, #f0b429); }
  .vrow { display: grid; grid-template-columns: 110px 1fr 130px; gap: 10px; align-items: center; margin: 10px 0; font-size: 13px; }
  .bar { height: 10px; border-radius: 6px; background: var(--line); overflow: hidden; }
  .bar span { display: block; height: 100%; border-radius: 6px; transition: width 0.5s; }
  .good { color: var(--ok); }
  .bad { color: var(--err); }
  @media (max-width: 820px) {
    .auth { grid-template-columns: 1fr; }
    .hero { padding: 28px 22px; }
    .hero svg.art { max-width: 260px; }
  }
</style>
</head>
<body>

<!-- ================= LOGIN / REGISTER ================= -->
<div id="authView" class="auth" hidden>
  <section class="hero">
    <svg class="art" viewBox="0 0 400 290" aria-hidden="true">
      <defs>
        <linearGradient id="wl" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#ffffff"/><stop offset="1" stop-color="#cfe6ff"/></linearGradient>
        <linearGradient id="co" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#ffe27a"/><stop offset="1" stop-color="#ffb100"/></linearGradient>
      </defs>
      <circle cx="200" cy="150" r="120" fill="rgba(255,255,255,0.10)"/>
      <circle cx="200" cy="150" r="85" fill="rgba(255,255,255,0.10)"/>
      <rect x="70" y="105" width="230" height="140" rx="20" fill="url(#wl)"/>
      <path d="M70 135 a20 20 0 0 1 20 -20 h170 a20 20 0 0 1 20 20 v6 h-210 z" fill="rgba(11,92,255,0.18)"/>
      <rect x="225" y="160" width="90" height="48" rx="14" fill="#0b5cff"/>
      <circle cx="250" cy="184" r="8" fill="#ffffff"/>
      <rect x="95" y="165" width="70" height="8" rx="4" fill="rgba(11,92,255,0.25)"/>
      <rect x="95" y="185" width="46" height="8" rx="4" fill="rgba(11,92,255,0.18)"/>
      <circle cx="120" cy="82" r="24" fill="url(#co)"/><text x="120" y="91" text-anchor="middle" font-size="24" font-weight="bold" fill="#8a5a00" font-family="Arial">$</text>
      <circle cx="170" cy="58" r="18" fill="url(#co)"/><text x="170" y="65" text-anchor="middle" font-size="18" font-weight="bold" fill="#8a5a00" font-family="Arial">$</text>
      <circle cx="68" cy="70" r="14" fill="url(#co)"/>
      <path d="M318 38 l46 17 v40 c0 30 -21 49 -46 59 c-25 -10 -46 -29 -46 -59 v-40 z" fill="#ffffff"/>
      <path d="M318 38 l46 17 v40 c0 30 -21 49 -46 59 z" fill="rgba(11,92,255,0.12)"/>
      <path d="M298 96 l14 14 l27 -28" fill="none" stroke="#0a9b57" stroke-width="9" stroke-linecap="round" stroke-linejoin="round"/>
      <circle cx="350" cy="230" r="7" fill="rgba(255,255,255,0.5)"/><circle cx="40" cy="200" r="5" fill="rgba(255,255,255,0.5)"/>
    </svg>
    <h1>Money that moves safely</h1>
    <p>PayFlow is a digital wallet. Create your own accounts, move money between them, and send it to other people by account ID.</p>
    <ul class="perks">
      <li><span class="dot"><svg class="ico" viewBox="0 0 24 24"><rect x="5" y="11" width="14" height="9" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/></svg></span>
        <span><b>Only you can touch your accounts.</b> Passwords are stored as salted hashes, and every request is checked against its owner.</span></li>
      <li><span class="dot"><svg class="ico" viewBox="0 0 24 24"><path d="M20 6L9 17l-5-5"/></svg></span>
        <span><b>Transfers are all-or-nothing.</b> Money is never lost or created, even when many transfers happen at once.</span></li>
      <li><span class="dot"><svg class="ico" viewBox="0 0 24 24"><path d="M21 12a9 9 0 1 1-3-6.7"/><path d="M21 4v5h-5"/></svg></span>
        <span><b>Safe to retry.</b> If a request is sent twice, the money moves only once.</span></li>
    </ul>
  </section>
  <section class="formside">
    <div class="authcard">
      <div class="brandrow">
        <svg width="34" height="34" viewBox="0 0 32 32" aria-hidden="true"><defs><linearGradient id="lg1" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#0b5cff"/><stop offset="1" stop-color="#00b4ff"/></linearGradient></defs><rect width="32" height="32" rx="9" fill="url(#lg1)"/><path d="M9 20h10l-3 -3m3 3l-3 3M23 12H13l3 -3m-3 3l3 3" fill="none" stroke="#fff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/></svg>
        <b>PayFlow</b>
      </div>
      <div class="tabs">
        <button id="tabLogin" class="on">Log in</button>
        <button id="tabRegister">Register</button>
      </div>

      <div id="loginForm">
        <label for="liUser">Username</label>
        <input id="liUser" autocomplete="username">
        <label for="liPass">Password</label>
        <input id="liPass" type="password" autocomplete="current-password">
        <label class="showpw"><input type="checkbox" id="liShow"> Show password</label>
        <button class="btn" id="btnLogin">Log in</button>
      </div>

      <div id="registerForm" hidden>
        <label for="rgName">Your name</label>
        <input id="rgName" placeholder="e.g. Asha Kumar" autocomplete="name">
        <label for="rgUser">Choose a username</label>
        <input id="rgUser" placeholder="3-20 letters, digits or _" autocomplete="username">
        <label for="rgPass">Password (at least 8 characters)</label>
        <input id="rgPass" type="password" autocomplete="new-password">
        <label for="rgPass2">Confirm password</label>
        <input id="rgPass2" type="password" autocomplete="new-password">
        <label class="showpw"><input type="checkbox" id="rgShow"> Show passwords</label>
        <button class="btn" id="btnRegister">Create my account</button>
      </div>

      <div id="authError" class="autherr" hidden></div>
      <div class="note">This is a demo with play money. Everything is kept in memory and is erased when the server restarts.</div>
    </div>
  </section>
</div>

<!-- ================= APP ================= -->
<div id="appView" hidden>
  <div class="topbar">
    <div class="brandrow" style="margin:0">
      <svg width="30" height="30" viewBox="0 0 32 32" aria-hidden="true"><defs><linearGradient id="lg2" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#0b5cff"/><stop offset="1" stop-color="#00b4ff"/></linearGradient></defs><rect width="32" height="32" rx="9" fill="url(#lg2)"/><path d="M9 20h10l-3 -3m3 3l-3 3M23 12H13l3 -3m-3 3l3 3" fill="none" stroke="#fff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/></svg>
      <b style="font-size:18px">PayFlow</b>
    </div>
    <div class="who">
      <div class="avatar" id="avatar"></div>
      <div><b id="meName"></b><div style="font-size:12px;color:var(--muted)" id="meUser"></div></div>
      <button class="logout" id="btnLogout"><svg class="ico" viewBox="0 0 24 24"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="M16 17l5-5-5-5"/><path d="M21 12H9"/></svg>Log out</button>
    </div>
  </div>

  <main>
    <div class="welcome">
      <h2 id="hello"></h2>
      <p>Select an account card to work with it. Share your account ID so others can send you money.</p>
    </div>

    <div class="tiles">
      <div class="tile"><small>Total balance</small><div id="tTotal">0.00</div></div>
      <div class="tile"><small>Your accounts</small><div id="tCount">0</div></div>
      <div class="tile"><small>Selected account</small><div id="tSel" style="font-size:18px">None</div></div>
    </div>

    <h3>Your accounts</h3>
    <div id="cards" class="cards"></div>
    <div id="emptyBox" class="empty" hidden>
      <svg viewBox="0 0 120 80" aria-hidden="true"><rect x="10" y="14" width="100" height="58" rx="10" fill="rgba(11,92,255,0.12)"/><rect x="10" y="26" width="100" height="12" fill="rgba(11,92,255,0.25)"/><rect x="20" y="50" width="34" height="7" rx="3" fill="rgba(11,92,255,0.3)"/></svg>
      <div>You have no accounts yet. Create your first one to start moving money.</div>
    </div>

    <div id="newForm" class="panel" style="margin-top:12px" hidden>
      <b>New account</b>
      <div class="panels" style="margin-top:6px;gap:12px">
        <div><label for="nwLabel">Name this account</label><input id="nwLabel" placeholder="e.g. Savings" maxlength="20"></div>
        <div><label for="nwBal">Opening balance (optional)</label><input id="nwBal" placeholder="e.g. 500.00" inputmode="decimal"></div>
      </div>
      <button class="btn small" id="btnCreate">Create account</button>
      <button class="btn small ghost" id="btnCancelNew">Cancel</button>
    </div>

    <h3>Move money</h3>
    <div class="panels">
      <div class="panel">
        <div class="modes">
          <button data-mode="deposit" class="on"><svg class="ico" viewBox="0 0 24 24"><path d="M12 3v12m0 0l-5-5m5 5l5-5M5 21h14"/></svg>Deposit</button>
          <button data-mode="withdraw"><svg class="ico" viewBox="0 0 24 24"><path d="M12 15V3m0 0L7 8m5-5l5 5M5 21h14"/></svg>Withdraw</button>
          <button data-mode="transfer"><svg class="ico" viewBox="0 0 24 24"><path d="M4 8h15l-4-4m4 4l-4 4M20 16H5l4-4m-4 4l4 4"/></svg>Transfer</button>
        </div>
        <div class="hint" id="fromLine" style="margin-top:10px"></div>
        <div id="recipBox" hidden>
          <label for="recipient">Recipient account ID</label>
          <input id="recipient" placeholder="e.g. ACC-1002" autocomplete="off">
          <div id="recipientInfo" class="rinfo"></div>
          <div id="ownChips" class="chips"></div>
        </div>
        <label for="amount">Amount</label>
        <input id="amount" placeholder="e.g. 250.00" inputmode="decimal">
        <div class="chips">
          <button class="chip" data-amt="100">+100</button>
          <button class="chip" data-amt="500">+500</button>
          <button class="chip" data-amt="1000">+1000</button>
        </div>
        <button class="btn small" id="btnGo">Deposit</button>
        <button class="btn small ghost" id="btnRetry" hidden>Retry same request</button>
        <div class="hint" id="retryHint" hidden>Every transfer carries an idempotency key. "Retry same request" sends the exact same request again, as a phone would after a timeout. The same transaction comes back and the money moves only once.</div>
      </div>

      <div class="panel">
        <b>Concurrency stress test</b>
        <div class="hint" style="margin-top:6px">Fires 200 random transfers between your own accounts at the same time, then checks that the total money did not change. Needs at least two accounts.</div>
        <button class="btn small" id="btnStress">Run 200 concurrent transfers</button>
        <div class="stress" id="stressResult"></div>
      </div>
    </div>

    <h3 id="histTitle">Transaction history</h3>
    <div class="panel scroll">
      <table>
        <thead><tr><th>Type</th><th>Details</th><th>ID</th><th>Time</th><th class="num">Amount</th></tr></thead>
        <tbody id="history"></tbody>
      </table>
      <div id="noHistory" class="hint">Select an account to see its history.</div>
    </div>
  </main>
</div>

<!-- ================= BANK OWNER CONSOLE ================= -->
<div id="adminView" hidden>
  <div class="topbar">
    <div class="brandrow" style="margin:0">
      <svg width="30" height="30" viewBox="0 0 32 32" aria-hidden="true"><defs><linearGradient id="lg3" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#0b5cff"/><stop offset="1" stop-color="#00b4ff"/></linearGradient></defs><rect width="32" height="32" rx="9" fill="url(#lg3)"/><path d="M9 20h10l-3 -3m3 3l-3 3M23 12H13l3 -3m-3 3l3 3" fill="none" stroke="#fff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/></svg>
      <b style="font-size:18px">PayFlow</b> <span class="tag owner">Bank Owner</span>
    </div>
    <div class="who">
      <div class="avatar" id="adAvatar"></div>
      <div><b id="adName"></b><div style="font-size:12px;color:var(--muted)">Administrator</div></div>
      <button class="logout" id="btnLogoutAdmin"><svg class="ico" viewBox="0 0 24 24"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="M16 17l5-5-5-5"/><path d="M21 12H9"/></svg>Log out</button>
    </div>
  </div>
  <main>
    <div class="welcome">
      <h2>Bank overview</h2>
      <p>Every customer, account and transaction on the platform. Figures come from one consistent snapshot.</p>
    </div>
    <div class="tiles">
      <div class="tile"><small>Customers</small><div id="adCustomers">0</div></div>
      <div class="tile"><small>Accounts</small><div id="adAccounts">0</div></div>
      <div class="tile"><small>Total money held</small><div id="adTotal">0.00</div></div>
      <div class="tile"><small>Transactions</small><div id="adTxns">0</div></div>
    </div>
    <div class="tiles">
      <div class="tile"><small>Total deposited</small><div id="adDeposited" style="font-size:20px">0.00</div></div>
      <div class="tile"><small>Total withdrawn</small><div id="adWithdrawn" style="font-size:20px">0.00</div></div>
      <div class="tile"><small>Transfer volume</small><div id="adTransferred" style="font-size:20px">0.00</div></div>
      <div class="tile"><small>Ledger check</small><div id="adLedger" style="font-size:18px">-</div></div>
    </div>
    <div class="panel">
      <b>Money flow by type</b>
      <div class="vrow"><span>Deposits</span><div class="bar"><span id="barDep" style="background:#0a9b57;width:0"></span></div><span id="lblDep"></span></div>
      <div class="vrow"><span>Withdrawals</span><div class="bar"><span id="barWd" style="background:#d4380d;width:0"></span></div><span id="lblWd"></span></div>
      <div class="vrow"><span>Transfers</span><div class="bar"><span id="barTr" style="background:#2f6bff;width:0"></span></div><span id="lblTr"></span></div>
      <div class="hint">Ledger check: money only enters through deposits and leaves through withdrawals, so total money held must equal deposits minus withdrawals. A mismatch would mean money was created or lost.</div>
    </div>

    <h3>Details</h3>
    <div class="atabs">
      <button data-atab="transactions" class="on">All transactions</button>
      <button data-atab="accounts">All accounts</button>
      <button data-atab="customers">Customers</button>
    </div>
    <div class="panel scroll">
      <div class="filters">
        <input type="text" id="adSearch" placeholder="Search name, username, account or ID">
        <select id="adType">
          <option value="">All types</option>
          <option value="DEPOSIT">Deposits</option>
          <option value="WITHDRAWAL">Withdrawals</option>
          <option value="TRANSFER">Transfers</option>
        </select>
        <label><input type="checkbox" id="adLive"> Live updates</label>
        <button class="btn small ghost" style="margin:0" id="btnAdRefresh">Refresh</button>
        <button class="btn small ghost" style="margin:0" id="btnCsv">Download CSV</button>
      </div>
      <div id="adTabTxns"><table>
        <thead><tr><th>Time</th><th>Type</th><th>Who</th><th>Details</th><th>ID</th><th class="num">Amount</th></tr></thead>
        <tbody id="adTxnRows"></tbody></table></div>
      <div id="adTabAccounts" hidden><table>
        <thead><tr><th>Account</th><th>Name</th><th>Owner</th><th>Username</th><th>Created</th><th class="num">Balance</th></tr></thead>
        <tbody id="adAccRows"></tbody></table></div>
      <div id="adTabCustomers" hidden><table>
        <thead><tr><th>Name</th><th>Username</th><th>Accounts</th><th class="num">Total balance</th></tr></thead>
        <tbody id="adCusRows"></tbody></table></div>
      <div id="adEmpty" class="hint" hidden>Nothing to show yet.</div>
    </div>
  </main>
</div>

<div id="toasts"></div>

<script>
const $ = id => document.getElementById(id);
const ICON = {
  copy: '<svg class="ico" viewBox="0 0 24 24"><rect x="9" y="9" width="11" height="11" rx="2"/><path d="M5 15V6a2 2 0 0 1 2-2h9"/></svg>',
  down: '<svg class="ico" viewBox="0 0 24 24"><path d="M12 5v14m0 0l-6-6m6 6l6-6"/></svg>',
  up: '<svg class="ico" viewBox="0 0 24 24"><path d="M12 19V5m0 0l-6 6m6-6l6 6"/></svg>',
  chip: '<svg width="34" height="26" viewBox="0 0 34 26"><rect width="34" height="26" rx="5" fill="rgba(255,255,255,0.35)"/><path d="M0 9h34M0 17h34M12 0v26M22 0v26" stroke="rgba(255,255,255,0.55)" stroke-width="1.5"/></svg>'
};
let token = sessionStorage.getItem('pf_token');
let me = null, accounts = [], selected = null, mode = 'deposit', lastTransfer = null, lookupTimer = null;
const prevBal = {};

/* ---------- helpers ---------- */
function money(n) { return Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
function esc(s) { return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;'); }
function toast(msg, ok) {
  const d = document.createElement('div');
  d.className = 'toast ' + (ok ? 'ok' : 'err');
  d.textContent = msg;
  $('toasts').appendChild(d);
  setTimeout(() => d.remove(), ok ? 4500 : 7000);
}
function colorFor(text) {
  let h = 0;
  for (let i = 0; i < text.length; i++) h = (h * 31 + text.charCodeAt(i)) % 360;
  return 'hsl(' + h + ', 60%, 45%)';
}
function initials(name) {
  const parts = name.trim().split(' ').filter(Boolean);
  return ((parts[0] || '?')[0] + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase();
}
function countUp(el, from, to) {
  const start = performance.now(), dur = 450;
  function step(now) {
    const t = Math.min(1, (now - start) / dur);
    el.textContent = money(from + (to - from) * t);
    if (t < 1) requestAnimationFrame(step);
  }
  requestAnimationFrame(step);
}
async function api(method, path, body) {
  const opts = { method: method, headers: {} };
  if (token) opts.headers['Authorization'] = 'Bearer ' + token;
  if (body !== undefined) { opts.body = JSON.stringify(body); opts.headers['Content-Type'] = 'application/json'; }
  const res = await fetch(path, opts);
  let data = null;
  try { data = await res.json(); } catch (e) { data = null; }
  if (res.status === 401 && token && path !== '/auth/login' && path !== '/auth/register') {
    expire();
  }
  return { status: res.status, data: data };
}
function errText(r) {
  return r.data && r.data.message ? r.data.message : 'Something went wrong (HTTP ' + r.status + ')';
}

/* ---------- auth ---------- */
function showAuth() {
  stopAdminTimer();
  $('adminView').hidden = true;
  $('appView').hidden = true;
  $('authView').hidden = false;
  $('authError').hidden = true;
}
function authError(msg) { $('authError').textContent = msg; $('authError').hidden = false; }
function setTab(which) {
  const login = which === 'login';
  $('tabLogin').className = login ? 'on' : '';
  $('tabRegister').className = login ? '' : 'on';
  $('loginForm').hidden = !login;
  $('registerForm').hidden = login;
  $('authError').hidden = true;
}
function expire() {
  token = null; me = null; accounts = []; selected = null;
  sessionStorage.removeItem('pf_token');
  showAuth();
  toast('Your session ended. Please log in again.', false);
}
function startSession(data) {
  token = data.token; me = data.user;
  sessionStorage.setItem('pf_token', token);
  accounts = []; selected = null;
  showApp();
}
async function doLogin() {
  const r = await api('POST', '/auth/login', { username: $('liUser').value, password: $('liPass').value });
  if (r.status === 200) { $('liPass').value = ''; startSession(r.data); } else { authError(errText(r)); }
}
async function doRegister() {
  const name = $('rgName').value.trim(), user = $('rgUser').value.trim();
  const p1 = $('rgPass').value, p2 = $('rgPass2').value;
  if (!/^[A-Za-z0-9_]{3,20}$/.test(user)) { authError('Username must be 3-20 characters: letters, digits or underscore'); return; }
  if (p1.length < 8) { authError('Password must be at least 8 characters'); return; }
  if (p1 !== p2) { authError('The two passwords do not match'); return; }
  const r = await api('POST', '/auth/register', { name: name, username: user, password: p1 });
  if (r.status === 201) { $('rgPass').value = ''; $('rgPass2').value = ''; startSession(r.data); toast('Welcome to PayFlow, ' + r.data.user.name + '!', true); }
  else { authError(errText(r)); }
}
async function doLogout() {
  await api('POST', '/auth/logout');
  token = null; me = null; accounts = []; selected = null;
  sessionStorage.removeItem('pf_token');
  showAuth();
}

/* ---------- app ---------- */
function showApp() {
  if (me.role === 'ADMIN') { showAdmin(); return; }
  $('adminView').hidden = true;
  $('authView').hidden = true;
  $('appView').hidden = false;
  $('meName').textContent = me.name;
  $('meUser').textContent = '@' + me.username;
  const av = $('avatar');
  av.textContent = initials(me.name);
  av.style.background = colorFor(me.username);
  $('hello').textContent = 'Hello, ' + me.name.split(' ')[0];
  refresh();
}
function selectedAccount() { return accounts.find(a => a.id === selected) || null; }

async function refresh() {
  const r = await api('GET', '/accounts');
  if (r.status !== 200) return;
  accounts = r.data;
  if (!selectedAccount()) selected = accounts.length ? accounts[0].id : null;
  renderCards();
  renderTiles();
  renderForm();
  await loadHistory();
}
function renderCards() {
  const html = accounts.map((a, i) =>
    '<div class="acard c' + (i % 3) + (a.id === selected ? ' sel' : '') + '" data-id="' + esc(a.id) + '">' +
    '<div class="top"><b>' + esc(a.label) + '</b>' + ICON.chip + '</div>' +
    '<div class="abal" data-id="' + esc(a.id) + '">' + money(prevBal[a.id] !== undefined ? prevBal[a.id] : a.balance) + '</div>' +
    '<div class="aid"><span>' + esc(a.id) + '</span><button class="copy" data-copy="' + esc(a.id) + '" title="Copy account ID">' + ICON.copy + '</button></div>' +
    '<div class="aown">' + esc(a.owner) + '</div></div>').join('');
  const add = accounts.length < 5
    ? '<div class="acard add" id="addCard"><div><div style="font-size:30px;line-height:1">+</div>New account</div></div>' : '';
  $('cards').innerHTML = html + add;
  $('emptyBox').hidden = accounts.length > 0;
  accounts.forEach(a => {
    const el = document.querySelector('.abal[data-id="' + a.id + '"]');
    const from = prevBal[a.id] !== undefined ? prevBal[a.id] : a.balance;
    if (el && from !== a.balance) countUp(el, from, a.balance);
    prevBal[a.id] = a.balance;
  });
}
let lastTotal = 0;
function renderTiles() {
  const total = accounts.reduce((s, a) => s + Math.round(a.balance * 100), 0) / 100;
  countUp($('tTotal'), lastTotal, total);
  lastTotal = total;
  $('tCount').textContent = accounts.length + ' / 5';
  const s = selectedAccount();
  $('tSel').textContent = s ? s.label + ' (' + s.id + ')' : 'None';
}
function renderForm() {
  const s = selectedAccount();
  const verb = mode === 'deposit' ? 'Deposit into' : mode === 'withdraw' ? 'Withdraw from' : 'Send from';
  $('fromLine').textContent = s ? verb + ': ' + s.label + ' (' + s.id + ')' : 'Create an account to get started';
  document.querySelectorAll('.modes button').forEach(b => { b.className = b.getAttribute('data-mode') === mode ? 'on' : ''; });
  $('recipBox').hidden = mode !== 'transfer';
  $('btnRetry').hidden = mode !== 'transfer';
  $('retryHint').hidden = mode !== 'transfer';
  $('btnGo').textContent = mode === 'deposit' ? 'Deposit' : mode === 'withdraw' ? 'Withdraw' : 'Send money';
  $('ownChips').innerHTML = accounts.filter(a => a.id !== selected).map(a =>
    '<button class="chip" data-to="' + esc(a.id) + '">My ' + esc(a.label) + ' (' + esc(a.id) + ')</button>').join('');
}
async function loadHistory() {
  const s = selectedAccount();
  if (!s) {
    $('history').innerHTML = '';
    $('noHistory').hidden = false;
    $('noHistory').textContent = 'Select an account to see its history.';
    $('histTitle').textContent = 'Transaction history';
    return;
  }
  const r = await api('GET', '/accounts/' + encodeURIComponent(s.id) + '/transactions');
  const list = Array.isArray(r.data) ? r.data.slice().reverse() : [];
  $('histTitle').textContent = 'Transaction history: ' + s.label + ' (' + s.id + ')';
  $('history').innerHTML = list.map(t => {
    let incoming, detail;
    if (t.type === 'DEPOSIT') { incoming = true; detail = 'Deposit'; }
    else if (t.type === 'WITHDRAWAL') { incoming = false; detail = 'Withdrawal'; }
    else if (t.from === s.id) { incoming = false; detail = 'Sent to ' + t.to; }
    else { incoming = true; detail = 'Received from ' + t.from; }
    return '<tr><td><span class="badge"><i class="' + (incoming ? 'in' : 'out') + '">' + (incoming ? ICON.down : ICON.up) + '</i>' +
      esc(t.type.charAt(0) + t.type.slice(1).toLowerCase()) + '</span></td><td>' + esc(detail) + '</td><td>' + esc(t.id) +
      '</td><td>' + new Date(t.createdAt).toLocaleTimeString() + '</td><td class="num ' + (incoming ? 'plus' : 'minus') + '">' +
      (incoming ? '+' : '-') + money(t.amount) + '</td></tr>';
  }).join('');
  $('noHistory').hidden = list.length > 0;
  $('noHistory').textContent = 'No transactions on this account yet.';
}

/* ---------- actions ---------- */
async function createAccount() {
  const body = { label: $('nwLabel').value };
  const bal = $('nwBal').value.trim();
  if (bal) body.initialBalance = bal;
  const r = await api('POST', '/accounts', body);
  if (r.status === 201) {
    toast('Account ' + r.data.id + ' created', true);
    $('nwLabel').value = ''; $('nwBal').value = ''; $('newForm').hidden = true;
    selected = r.data.id;
    await refresh();
  } else { toast(errText(r), false); }
}
async function submitMove() {
  const s = selectedAccount();
  if (!s) { toast('Create an account first', false); return; }
  const amount = $('amount').value.trim();
  if (!amount) { toast('Please enter an amount', false); return; }
  if (mode === 'transfer') {
    const to = $('recipient').value.trim().toUpperCase();
    if (!to) { toast('Enter the recipient account ID', false); return; }
    lastTransfer = { from: s.id, to: to, amount: amount, idempotencyKey: 'req-' + Date.now() };
    await sendTransfer(lastTransfer, 'Transfer sent');
    return;
  }
  const r = await api('POST', '/accounts/' + encodeURIComponent(s.id) + '/' + mode, { amount: amount });
  if (r.status === 201) {
    toast((mode === 'deposit' ? 'Deposited ' : 'Withdrew ') + money(r.data.amount) + ' (' + r.data.id + ')', true);
    $('amount').value = '';
  } else { toast(errText(r), false); }
  await refresh();
}
async function sendTransfer(payload, okLabel) {
  const r = await api('POST', '/transfers', payload);
  if (r.status === 201) {
    toast(okLabel + ': ' + money(r.data.amount) + ' to ' + payload.to + ' (' + r.data.id + ')', true);
  } else { toast(errText(r), false); }
  await refresh();
}
async function runStress() {
  await refresh();
  if (accounts.length < 2) { toast('Create at least two accounts first', false); return; }
  const before = accounts.reduce((s, a) => s + Math.round(a.balance * 100), 0);
  const jobs = [];
  for (let i = 0; i < 200; i++) {
    const a = Math.floor(Math.random() * accounts.length);
    let b = Math.floor(Math.random() * (accounts.length - 1));
    if (b >= a) b++;
    const amt = (1 + Math.floor(Math.random() * 2000) / 100).toFixed(2);
    jobs.push(api('POST', '/transfers', { from: accounts[a].id, to: accounts[b].id, amount: amt }));
  }
  const results = await Promise.all(jobs);
  const okCount = results.filter(r => r.status === 201).length;
  await refresh();
  const after = accounts.reduce((s, a) => s + Math.round(a.balance * 100), 0);
  const same = before === after;
  $('stressResult').textContent = okCount + ' transfers succeeded and ' + (results.length - okCount) +
    ' were rejected (insufficient funds). Total before: ' + money(before / 100) + ', after: ' + money(after / 100) +
    (same ? '. Money conserved.' : '. MISMATCH!');
  toast(same ? 'Stress test passed: total money unchanged' : 'Stress test FAILED: totals differ', same);
}
function lookupRecipient() {
  clearTimeout(lookupTimer);
  const v = $('recipient').value.trim().toUpperCase();
  const el = $('recipientInfo');
  if (!v) { el.className = 'rinfo'; el.textContent = ''; return; }
  if (!/^ACC-[0-9]+$/.test(v)) { el.className = 'rinfo bad'; el.textContent = 'Account IDs look like ACC-1002'; return; }
  lookupTimer = setTimeout(async () => {
    const r = await api('GET', '/lookup/' + encodeURIComponent(v));
    if (r.status === 200) { el.className = 'rinfo good'; el.textContent = 'Recipient: ' + r.data.owner; }
    else { el.className = 'rinfo bad'; el.textContent = 'No account found with this ID'; }
  }, 300);
}
function copyText(text) {
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(text).then(() => toast('Copied ' + text, true), () => toast('Could not copy', false));
  } else { toast('Your account ID is ' + text, true); }
}

/* ---------- bank owner console ---------- */
let adminTab = 'transactions', adminTimer = null;
let adminData = { overview: null, txns: [], accounts: [], customers: [] };

function stopAdminTimer() {
  if (adminTimer) { clearInterval(adminTimer); adminTimer = null; }
  if ($('adLive')) $('adLive').checked = false;
}
function showAdmin() {
  $('authView').hidden = true;
  $('appView').hidden = true;
  $('adminView').hidden = false;
  $('adName').textContent = me.name;
  const av = $('adAvatar');
  av.textContent = initials(me.name);
  av.style.background = colorFor(me.username);
  loadAdmin();
}
async function loadAdmin() {
  const res = await Promise.all([
    api('GET', '/admin/overview'), api('GET', '/admin/transactions?limit=500'),
    api('GET', '/admin/accounts'), api('GET', '/admin/users')]);
  if (res[0].status !== 200) { toast(errText(res[0]), false); return; }
  adminData = { overview: res[0].data, txns: res[1].data || [], accounts: res[2].data || [], customers: res[3].data || [] };
  renderAdmin();
}
function renderAdmin() {
  const o = adminData.overview;
  $('adCustomers').textContent = o.customers;
  $('adAccounts').textContent = o.accounts;
  $('adTotal').textContent = money(o.totalBalance);
  $('adTxns').textContent = o.transactions;
  $('adDeposited').textContent = money(o.deposited);
  $('adWithdrawn').textContent = money(o.withdrawn);
  $('adTransferred').textContent = money(o.transferred);
  const led = $('adLedger');
  led.textContent = o.ledgerBalanced ? 'Balanced' : 'MISMATCH';
  led.className = o.ledgerBalanced ? 'good' : 'bad';
  const mx = Math.max(o.deposited, o.withdrawn, o.transferred, 1);
  $('barDep').style.width = (o.deposited / mx * 100) + '%';
  $('barWd').style.width = (o.withdrawn / mx * 100) + '%';
  $('barTr').style.width = (o.transferred / mx * 100) + '%';
  $('lblDep').textContent = money(o.deposited) + ' (' + o.depositCount + ')';
  $('lblWd').textContent = money(o.withdrawn) + ' (' + o.withdrawalCount + ')';
  $('lblTr').textContent = money(o.transferred) + ' (' + o.transferCount + ')';
  renderAdminTable();
}
function who(name, user) { return name ? name + ' (@' + user + ')' : ''; }
function txnWho(t) {
  if (t.type === 'DEPOSIT') return who(t.toOwner, t.toUsername);
  return who(t.fromOwner, t.fromUsername);
}
function txnDetails(t) {
  if (t.type === 'DEPOSIT') return 'Deposit into ' + t.toAccount;
  if (t.type === 'WITHDRAWAL') return 'Withdrawal from ' + t.fromAccount;
  return who(t.fromOwner, t.fromUsername) + ' (' + t.fromAccount + ') sent to ' + who(t.toOwner, t.toUsername) + ' (' + t.toAccount + ')';
}
function filteredTxns() {
  const q = $('adSearch').value.trim().toLowerCase();
  const type = $('adType').value;
  return adminData.txns.filter(t => {
    if (type && t.type !== type) return false;
    if (!q) return true;
    return (txnWho(t) + ' ' + txnDetails(t) + ' ' + t.id).toLowerCase().indexOf(q) >= 0;
  });
}
function renderAdminTable() {
  const q = $('adSearch').value.trim().toLowerCase();
  $('adTabTxns').hidden = adminTab !== 'transactions';
  $('adTabAccounts').hidden = adminTab !== 'accounts';
  $('adTabCustomers').hidden = adminTab !== 'customers';
  $('adType').hidden = adminTab !== 'transactions';
  $('btnCsv').hidden = adminTab !== 'transactions';
  document.querySelectorAll('.atabs button').forEach(b => { b.className = b.getAttribute('data-atab') === adminTab ? 'on' : ''; });
  let count = 0;
  if (adminTab === 'transactions') {
    const list = filteredTxns();
    count = list.length;
    $('adTxnRows').innerHTML = list.map(t =>
      '<tr><td>' + esc(new Date(t.createdAt).toLocaleString()) + '</td>' +
      '<td><span class="tag ' + esc(t.type.toLowerCase()) + '">' + esc(t.type.charAt(0) + t.type.slice(1).toLowerCase()) + '</span></td>' +
      '<td>' + esc(txnWho(t)) + '</td><td>' + esc(txnDetails(t)) + '</td><td>' + esc(t.id) +
      '</td><td class="num">' + money(t.amount) + '</td></tr>').join('');
  } else if (adminTab === 'accounts') {
    const list = adminData.accounts.filter(a => !q || (a.id + ' ' + a.owner + ' ' + a.username + ' ' + a.label).toLowerCase().indexOf(q) >= 0);
    count = list.length;
    $('adAccRows').innerHTML = list.map(a =>
      '<tr><td>' + esc(a.id) + '</td><td>' + esc(a.label) + '</td><td>' + esc(a.owner) + '</td><td>@' + esc(a.username) +
      '</td><td>' + esc(new Date(a.createdAt).toLocaleString()) + '</td><td class="num">' + money(a.balance) + '</td></tr>').join('');
  } else {
    const list = adminData.customers.filter(c => !q || (c.name + ' ' + c.username).toLowerCase().indexOf(q) >= 0);
    count = list.length;
    $('adCusRows').innerHTML = list.map(c =>
      '<tr><td>' + esc(c.name) + '</td><td>@' + esc(c.username) + '</td><td>' + c.accounts +
      '</td><td class="num">' + money(c.balance) + '</td></tr>').join('');
  }
  $('adEmpty').hidden = count > 0;
}
function csvCell(v) { return '"' + String(v === null || v === undefined ? '' : v).split('"').join('""') + '"'; }
function downloadCsv() {
  const rows = [['Time', 'Type', 'Who', 'Details', 'Transaction ID', 'Amount']];
  filteredTxns().forEach(t => rows.push([t.createdAt, t.type, txnWho(t), txnDetails(t), t.id, t.amount]));
  const text = rows.map(r => r.map(csvCell).join(',')).join(String.fromCharCode(10));
  const blob = new Blob([text], { type: 'text/csv' });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = 'payflow-transactions.csv';
  document.body.appendChild(a);
  a.click();
  a.remove();
}

/* ---------- wiring ---------- */
$('tabLogin').addEventListener('click', () => setTab('login'));
$('tabRegister').addEventListener('click', () => setTab('register'));
$('btnLogin').addEventListener('click', doLogin);
$('btnRegister').addEventListener('click', doRegister);
$('btnLogout').addEventListener('click', doLogout);
['liUser', 'liPass'].forEach(id => $(id).addEventListener('keydown', e => { if (e.key === 'Enter') doLogin(); }));
['rgName', 'rgUser', 'rgPass', 'rgPass2'].forEach(id => $(id).addEventListener('keydown', e => { if (e.key === 'Enter') doRegister(); }));
$('liShow').addEventListener('change', e => { $('liPass').type = e.target.checked ? 'text' : 'password'; });
$('rgShow').addEventListener('change', e => {
  $('rgPass').type = e.target.checked ? 'text' : 'password';
  $('rgPass2').type = e.target.checked ? 'text' : 'password';
});
$('cards').addEventListener('click', e => {
  const copy = e.target.closest('.copy');
  if (copy) { e.stopPropagation(); copyText(copy.getAttribute('data-copy')); return; }
  if (e.target.closest('#addCard')) { $('newForm').hidden = false; $('nwLabel').focus(); return; }
  const card = e.target.closest('.acard');
  if (card && card.getAttribute('data-id')) {
    selected = card.getAttribute('data-id');
    renderCards(); renderTiles(); renderForm(); loadHistory();
  }
});
$('btnCreate').addEventListener('click', createAccount);
$('btnCancelNew').addEventListener('click', () => { $('newForm').hidden = true; });
document.querySelectorAll('.modes button').forEach(b => b.addEventListener('click', () => {
  mode = b.getAttribute('data-mode');
  renderForm();
}));
document.querySelectorAll('[data-amt]').forEach(b => b.addEventListener('click', () => { $('amount').value = b.getAttribute('data-amt'); }));
$('ownChips').addEventListener('click', e => {
  const c = e.target.closest('[data-to]');
  if (c) { $('recipient').value = c.getAttribute('data-to'); lookupRecipient(); }
});
$('recipient').addEventListener('input', lookupRecipient);
$('btnGo').addEventListener('click', submitMove);
$('btnRetry').addEventListener('click', () => {
  if (!lastTransfer) { toast('Send a transfer first, then retry it', false); return; }
  sendTransfer(lastTransfer, 'Retry returned the same transfer');
});
$('btnStress').addEventListener('click', runStress);

$('btnLogoutAdmin').addEventListener('click', doLogout);
$('btnAdRefresh').addEventListener('click', loadAdmin);
$('btnCsv').addEventListener('click', downloadCsv);
$('adSearch').addEventListener('input', renderAdminTable);
$('adType').addEventListener('change', renderAdminTable);
$('adLive').addEventListener('change', e => {
  if (adminTimer) { clearInterval(adminTimer); adminTimer = null; }
  if (e.target.checked) adminTimer = setInterval(loadAdmin, 4000);
});
document.querySelectorAll('[data-atab]').forEach(b => b.addEventListener('click', () => {
  adminTab = b.getAttribute('data-atab');
  renderAdminTable();
}));

/* ---------- start ---------- */
(async function init() {
  if (token) {
    const r = await api('GET', '/auth/me');
    if (r.status === 200) { me = r.data; showApp(); return; }
    token = null; sessionStorage.removeItem('pf_token');
  }
  showAuth();
})();
</script>
</body>
</html>
""";
}
