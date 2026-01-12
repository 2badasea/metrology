// src/components/Topbar.jsx
export default function Topbar() {
  return (
    <div className="topbar">
      <div className="topbar-left">
        <div className="topbar-title">Dashboard</div>

        <div className="topbar-search">
          <input placeholder="Search" />
        </div>
      </div>

      <div className="topbar-right">
        <button className="topbar-btn" type="button">Account</button>
        <button className="topbar-btn" type="button">Dropdown</button>
        <button className="topbar-btn primary" type="button">Log out</button>
      </div>
    </div>
  );
}
