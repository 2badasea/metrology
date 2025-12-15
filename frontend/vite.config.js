import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  base: "/admin/",
  plugins: [react()],
  server: {
    port: 3000, // 또는 다른 사용 가능한 포트 번호
    proxy: {
      // 개발 중엔 CORS 피하려고 /api 를 스프링으로 프록시
      "/api": {
        target: "http://localhost:8050",
        changeOrigin: true,
      },
    },
  },
});


// import { defineConfig } from 'vite'
// import react from '@vitejs/plugin-react'

// // https://vite.dev/config/
// export default defineConfig({
//   plugins: [react()],
// })
