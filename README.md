# BookMind

BookMind là ứng dụng web để quản lý thư viện sách cá nhân, tìm kiếm ngữ nghĩa và chat với nội dung sách (RAG). Backend dùng Java 21 + Spring Boot, frontend dùng React + TypeScript, dữ liệu lưu cục bộ trong `~/.bookmind`.

Tài liệu thiết kế: [requirements](docs/requirements.md) · [architecture](docs/architecture.md) · [implementation plan](docs/implementation-plan.md).

## Stack

- Backend: Java 21, Spring Boot 4.1, Spring Data JPA, SQLite, Flyway
- Frontend: Vite, React, TypeScript, Tailwind CSS, shadcn/ui, React Router, TanStack Query, i18next
- Xử lý sách: Apache PDFBox, jsoup, PaddleOCR sidecar (Python)

## Chạy dev

Backend (mặc định `http://127.0.0.1:8090`):

```bash
cd backend
./gradlew bootRun
curl http://127.0.0.1:8090/api/health
```

Frontend (mặc định `http://127.0.0.1:5173`, proxy `/api` sang backend):

```bash
cd frontend
npm install
npm run dev
```

## Kiểm thử

```bash
cd backend && ./gradlew test
cd frontend && npm test
```

## Cấu hình

Mặc định dữ liệu nằm trong `~/.bookmind`. Có thể đổi bằng biến môi trường `BOOKMIND_HOME`:

```bash
cp backend/.env.example backend/.env
export BOOKMIND_HOME=~/.bookmind-dev
```

Các thư mục `library/`, `.tmp/`, `logs/` và file `bookmind.db` được tạo tự động khi backend khởi động.
