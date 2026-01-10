// src/pages/Dashboard.jsx
export default function Dashboard() {
  return (
    <div className="page">
      <div className="kpi-row">
        <div className="card kpi">
          <div className="kpi-label">Number</div>
          <div className="kpi-value">150GB</div>
          <div className="kpi-foot">Update now</div>
        </div>

        <div className="card kpi">
          <div className="kpi-label">Revenue</div>
          <div className="kpi-value">$ 1,345</div>
          <div className="kpi-foot">Last day</div>
        </div>

        <div className="card kpi">
          <div className="kpi-label">Errors</div>
          <div className="kpi-value">23</div>
          <div className="kpi-foot">In the last hour</div>
        </div>

        <div className="card kpi">
          <div className="kpi-label">Followers</div>
          <div className="kpi-value">+45K</div>
          <div className="kpi-foot">Update now</div>
        </div>
      </div>

      <div className="grid-row">
        <div className="card panel">
          <div className="panel-title">Users Behavior</div>
          <div className="panel-sub">24 Hours performance</div>
          <div className="panel-body placeholder">Chart 영역 (추후)</div>
        </div>

        <div className="card panel">
          <div className="panel-title">Email Statistics</div>
          <div className="panel-sub">Last Campaign Performance</div>
          <div className="panel-body placeholder">Pie/Stat 영역 (추후)</div>
        </div>
      </div>
    </div>
  );
}
