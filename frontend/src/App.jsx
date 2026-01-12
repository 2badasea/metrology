import { Routes, Route, Navigate } from "react-router-dom";
import AdminLayout from "./layouts/AdminLayout";

import Dashboard from "./pages/Dashboard";
import CompanyAccounts from "./pages/CompanyAccounts";
import LoginHistory from "./pages/LoginHistory";
import WorkHistory from "./pages/WorkHistory";
import MenuPermissions from "./pages/MenuPermissions";
import Notices from "./pages/Notices";

export default function App() {
  return (
    <Routes>
      {/* 어드민 레이아웃 아래로 페이지들이 들어감 */}
      <Route element={<AdminLayout />}>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<Dashboard />} />

        <Route path="/company-accounts" element={<CompanyAccounts />} />
        <Route path="/login-history" element={<LoginHistory />} />
        <Route path="/work-history" element={<WorkHistory />} />
        <Route path="/menu-permissions" element={<MenuPermissions />} />
        <Route path="/notices" element={<Notices />} />
      </Route>

      <Route path="*" element={<div className="page">Not Found</div>} />
    </Routes>
  );
}


// import { useState } from 'react'
// import reactLogo from './assets/react.svg'
// import viteLogo from '/vite.svg'
// import './App.css'

// function App() {
//   const [count, setCount] = useState(0)

//   return (
//     <>
//       <div>
//         <a href="https://vite.dev" target="_blank">
//           <img src={viteLogo} className="logo" alt="Vite logo" />
//         </a>
//         <a href="https://react.dev" target="_blank">
//           <img src={reactLogo} className="logo react" alt="React logo" />
//         </a>
//       </div>
//       <h1>Vite + React</h1>
//       <div className="card">
//         <button onClick={() => setCount((count) => count + 1)}>
//           count is {count}
//         </button>
//         <p>
//           Edit <code>src/App.jsx</code> and save to test HMR
//         </p>
//       </div>
//       <p className="read-the-docs">
//         Click on the Vite and React logos to learn more
//       </p>
//     </>
//   )
// }

// export default App
