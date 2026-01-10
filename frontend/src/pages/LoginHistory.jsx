import { useEffect, useMemo, useRef, useState } from "react";
import TuiGrid from "tui-grid";

function formatDateTime(isoString) {
  const d = new Date(isoString);
  const pad = (n) => String(n).padStart(2, "0");
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  );
}

function makeDummyRows() {
  const now = Date.now();
  const results = ["SUCCESS", "FAIL"];
  const reasons = ["WRONG_PASSWORD", "LOCKED", "EXPIRED", "UNKNOWN"];
  const ips = ["121.134.22.10", "59.6.123.77", "211.45.9.3", "175.210.88.21"];

  return Array.from({ length: 53 }).map((_, i) => {
    const result = results[i % results.length];
    return {
      id: i + 1,
      loginAt: new Date(now - i * 1000 * 60 * 15).toISOString(),
      companyName: `업체-${(i % 7) + 1}`,
      userId: `admin${(i % 5) + 1}`,
      ip: ips[i % ips.length],
      userAgent: i % 2 === 0 ? "Chrome" : "Safari",
      result,
      failReason: result === "FAIL" ? reasons[i % reasons.length] : "",
    };
  });
}

export default function LoginHistory() {
  const containerRef = useRef(null);
  const gridInstanceRef = useRef(null);

  const [rows] = useState(() => makeDummyRows());

  const columns = useMemo(
    () => [
      { name: "id", header: "ID", width: 70, align: "center", sortable: true },
      {
        name: "loginAt",
        header: "로그인 시각",
        minWidth: 170,
        sortable: true,
        formatter: ({ value }) => formatDateTime(value),
      },
      { name: "companyName", header: "업체", minWidth: 140, sortable: true },
      { name: "userId", header: "계정", minWidth: 120, sortable: true },
      { name: "ip", header: "IP", minWidth: 140, sortable: true },
      { name: "userAgent", header: "User-Agent", minWidth: 140 },
      { name: "result", header: "결과", width: 100, align: "center", sortable: true },
      { name: "failReason", header: "실패사유", minWidth: 160 },
    ],
    []
  );

  useEffect(() => {
    if (!containerRef.current) return;

    // StrictMode(개발)에서 mount/unmount가 반복될 수 있으므로, 안전하게 정리
    if (gridInstanceRef.current) {
      gridInstanceRef.current.destroy();
      gridInstanceRef.current = null;
    }

    gridInstanceRef.current = new TuiGrid({
      el: containerRef.current,
      data: rows,
      columns,
      rowHeaders: ["rowNum"],
      bodyHeight: 520,
      pageOptions: {
        useClient: true,
        perPage: 15,
      },
      columnOptions: {
        resizable: true,
      },
    });

    return () => {
      if (gridInstanceRef.current) {
        gridInstanceRef.current.destroy();
        gridInstanceRef.current = null;
      }
    };
  }, [columns, rows]);

  return (
    <div className="page">
      <div className="card panel">
        <div className="panel-title">로그인 이력관리</div>
        <div className="panel-sub">최근 로그인 시도 내역</div>

        <div style={{ marginTop: 12 }}>
          <div ref={containerRef} />
        </div>
      </div>
    </div>
  );
}
