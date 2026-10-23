# BookMind — Kế hoạch triển khai (Implementation Plan)

Tài liệu này là kế hoạch thi công chi tiết cho BookMind, dựa trên [requirements.md](./requirements.md) và [architecture.md](./architecture.md). Mục tiêu: biến kiến trúc thành các bước code được, có thứ tự, có tiêu chí hoàn thành và có thể demo sau từng milestone.

---

## 0. Cách dùng tài liệu

- Làm **theo milestone**, không nhảy cóc. Mỗi milestone phải chạy được end-to-end (backend + UI) trước khi sang milestone sau.
- Mỗi bước đều có dạng checklist `- [ ]`. Tick khi xong, không tick khi mới code xong mà chưa có test/không chạy được.
- Khi phát hiện kiến trúc cần sửa, sửa `architecture.md` trước rồi mới code, để tài liệu không lệch.
- Thời lượng trong tài liệu là ước lượng cho **1 người**, đã gồm test cơ bản, chưa gồm học công nghệ mới.

---

## 1. Nguyên tắc thực thi

| Nguyên tắc | Cách áp dụng |
|---|---|
| **Vertical slice trước, layer sau** | Làm sớm luồng "thêm sách → thấy trong danh sách" (M2). Không xây hết domain rồi mới nối UI. |
| **Domain test-first** | `domain/` là phần duy nhất viết unit test trước/song song. Phần còn lại test theo hành vi. |
| **Port out có thật** | Chỉ tạo interface khi có ≥1 adapter thật (JPA, PDFBox, OpenAI...). Không tạo port "để sau". |
| **Không port in** | Controller inject thẳng `*Service` (theo ADR-10). |
| **Schema do Flyway, không `ddl-auto`** | `spring.jpa.hibernate.ddl-auto=validate`; mọi thay đổi bảng = migration mới. |
| **Mọi job phải restart được** | Không giữ trạng thái job trong RAM. Test restart ở M2 và lặp lại ở M5. |
| **Không chặn UI** | Mọi thứ nặng (hash, copy, OCR, embed) chạy trong worker; API chỉ trả `202 Accepted`. |
| **Đo trước khi tối ưu** | Chỉ tối ưu search/embedding sau khi có dữ liệu giả lập ~1000 sách (M10). |

---

## 2. Chuẩn bị môi trường

### 2.1 Công cụ bắt buộc

- [ ] JDK 21 (`java -version` → 21.x)
- [ ] Gradle 8.x (dùng wrapper, không cần cài global)
- [ ] Node.js 20+ và npm
- [ ] Python 3.10+ (cho OCR sidecar)
- [ ] `sqlite3` CLI (debug DB)
- [ ] Git, trình soạn thảo có hỗ trợ Java/TS

### 2.2 Khởi tạo repository

```bash
cd /run/media/codefultm/DATA/LearningMaterial/personal-project/BookMind
mkdir -p backend frontend scripts docs
```

- [ ] `.gitignore` gốc: `backend/build/`, `.gradle/`, `frontend/node_modules/`, `frontend/dist/`, `*.db`, `.env`, `__pycache__/`, `models/`
- [ ] `README.md`: mô tả 1 đoạn + quickstart dự kiến (sẽ hoàn thiện ở M10)

### 2.3 Biến môi trường dev

Tạo `backend/.env.example` (không commit `.env` thật):

```
BOOKMIND_HOME=~/.bookmind-dev
SERVER_PORT=8080
```

---

## 3. Tổng quan milestone

| M | Tên | Kết quả demo được | Ước lượng | Phụ thuộc |
|---|---|---|---|---|
| M0 | Bootstrap | App chạy, `/api/health`, trang Library rỗng | 1 ngày | — |
| M1 | Domain + schema | Tạo/đọc Book qua repository, Flyway chạy sạch | 2 ngày | M0 |
| M2 | Import sách (slice 1) | Thêm PDF → file nằm trong `library/`, hiện trong danh sách, xoá được | 3 ngày | M1 |
| M3 | Trích xuất text | PDF/EPUB hiện text trong Book Detail | 3 ngày | M2 |
| M4 | OCR | PDF scan tiếng Việt ra text, sửa tay được | 4 ngày | M3 |
| M5 | Chunk + Embed + Index | Sách chuyển `READY`, có vector trong store | 3 ngày | M3, M4 |
| M6 | Semantic search | Tìm theo nghĩa, click mở đúng trang | 1.5 ngày | M5 |
| M7 | Chat + RAG | Chat có streaming + trích dẫn bấm được | 3 ngày | M5, M8 |
| M8 | Cấu hình AI | Lưu API key mã hoá, test connection | 1.5 ngày | M0 |
| M9 | Metadata + Collections | Sửa metadata, tag, collection, lọc/sort | 2 ngày | M2 |
| M10 | Hardening + đóng gói | 1 jar chạy được, E2E xanh, backup/restore | 3 ngày | tất cả |

Tổng: **~27 ngày làm việc** (~5.5 tuần full-time; 10–12 tuần nếu làm 20h/tuần).

Thứ tự thực tế nên làm: **M0 → M1 → M2 → M3 → M8 → M5 → M6 → M7 → M4 → M9 → M10**. Lý do: M8 (AI config) là điều kiện để M5/M7 có model; OCR (M4) nặng và ít chặn luồng chính nên đẩy sau khi đã có search/chat với sách có text layer.

---

## 4. Chi tiết từng milestone

### M0 — Bootstrap (1 ngày)

**Mục tiêu:** hai process dev chạy được và nói chuyện với nhau qua `/api`.

Backend:

- [ ] Tạo project Spring Boot 3.x, Java 21, Gradle Kotlin DSL tại `backend/` (start.spring.io hoặc `gradle init`).
- [ ] Dependencies: `web`, `validation`, `data-jpa`, `actuator`; thêm `webflux` **chỉ để có WebClient/Reactor**, `sqlite-jdbc`, `hibernate-community-dialects`, `flyway-core`, `pdfbox`, `jsoup`, `jtokkit`.
- [ ] `BookMindApplication` + `application.yml`:

```yaml
server:
  address: 127.0.0.1
  port: 8080
spring:
  datasource:
    url: jdbc:sqlite:${BOOKMIND_HOME:${user.home}/.bookmind}/bookmind.db
    driver-class-name: org.sqlite.JDBC
  jpa:
    database-platform: org.hibernate.community.dialect.SQLiteDialect
    hibernate.ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration
```

- [ ] `HealthController` (hoặc chỉ Actuator `/actuator/health`) + `GET /api/health`.
- [ ] `ErrorResponse` + `GlobalExceptionHandler` khung: map `DomainException` → 4xx, còn lại 500, body `{code,message,details}`.
- [ ] Tạo `BOOKMIND_HOME` nếu chưa có, kèm `library/`, `.tmp/`, `logs/` (`AppHomeInitializer`).

Frontend:

- [ ] `npm create vite@latest frontend -- --template react-ts`
- [ ] Tailwind + shadcn/ui init; alias `@/`.
- [ ] React Router với route rỗng: `/`, `/books/:id`, `/search`, `/chat`, `/chat/:id`, `/settings`.
- [ ] TanStack Query provider; `shared/api/client.ts` (fetch wrapper, parse `ErrorResponse`).
- [ ] i18next với `vi.json`/`en.json`, mặc định `vi`; layout có sidebar 4 mục.
- [ ] `vite.config.ts` proxy `/api` → `http://127.0.0.1:8080`.

**Definition of Done**

- `./gradlew bootRun` lên 8080, `curl /api/health` → 200.
- `npm run dev` lên 5173, mở trang Library rỗng, không lỗi console.
- `./gradlew test` và `npm test` chạy (có thể 0 test).

---

### M1 — Domain model + schema (2 ngày)

**Mục tiêu:** mô hình hoá đúng `Book`/`BookContent`/`Conversation` và có DB tương ứng.

Domain (`com.bookmind.domain`):

- [ ] `book`: `BookId` (record wrap UUID), `FilePath` (validate relative, không `..`, ext whitelist), `BookFormat` (PDF/EPUB), `BookStatus`, `ProcessingStatus`, `Metadata` (title, author, tags, description, coverPath, pageCount nullable, sizeBytes, addedAt, originalFileName), `Book` aggregate.
- [ ] `Book` methods: `create(...)`, `markImporting/markExtracting/markOcr/markIndexing/markReady/markFailed(reason)/retry()`, `updateMetadata(...)`, `progress(int)`. Invariant: `FAILED` phải có reason; `READY` phải có `contentHash`; `progress` 0–100.
- [ ] `content`: `BookContent`, `Section`, `PageSpan`, `Locator`, `Chunk`, `ChunkId` theo đúng record trong architecture.
- [ ] `chat`: `Conversation`, `Message`, `ChatMode`, `Citation`, `ConversationId`, `MessageId`.
- [ ] `collection`: `Collection`, `CollectionId`.
- [ ] `provider`: `AiProviderConfig`, `ApiKey` (không log ra `toString`).
- [ ] `shared`: `DomainException(code, message)`, `ErrorCode` enum.

Persistence:

- [ ] `V1__init.sql` (xem §5) tạo `book`, `book_content`, `book_page`, `book_chunk`, `collection`, `collection_book`, `conversation`, `message`, `message_citation`, `ai_provider_config`, `processing_job`, `app_meta`, `embedding_cache`.
- [ ] JPA entities + mappers + `JpaBookRepositoryAdapter implements BookRepository`, tương tự cho Collection/Conversation.
- [ ] `BookRepository` port: `save`, `findById`, `findByContentHash`, `list(filter, sort, page)`, `delete`, `existsByHash`.

**Test**

- [ ] Domain unit: tạo Book thiếu hash → lỗi; `markFailed` không reason → lỗi; retry từ `FAILED` OK.
- [ ] `FilePath` reject `../x.pdf`, `/abs/x.pdf` (khi là relative), `.txt`.
- [ ] Integration: lưu/đọc Book trên SQLite file tạm (`@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)`).

**DoD:** Flyway chạy sạch trên DB mới; `validate` pass; repository test xanh.

---

### M2 — Import sách, vertical slice đầu tiên (3 ngày)

**Mục tiêu:** thêm 1 file PDF/EPUB → app copy vào `library/`, hiện trong UI, xoá được; dedupe hoạt động.

Backend — file:

- [ ] `PathValidator`: absolute, tồn tại, regular file, ext ∈ {pdf, epub}, size ≤ `max-book-size`, từ chối symlink nếu cấu hình.
- [ ] `FileHasher`: SHA-256 streaming bằng `DigestInputStream`, buffer 1MB.
- [ ] `LibraryRootResolver`: đọc `bookmind.library.dir`, tạo thư mục, chặn path thoát root.
- [ ] `BookImporter.importFile(source)`:
  - copy vào `<root>/.tmp/<uuid>` **đồng thời** tính SHA-256,
  - `fd.sync()` + `Files.move(..., ATOMIC_MOVE)`,
  - đích `<root>/<sha[0:2]>/<sha[2:4]>/<sha>.<ext>`,
  - trả `ImportResult(hash, relativePath, sizeBytes, originalFileName)`,
  - nếu hash đã tồn tại → `DUPLICATE_BOOK` kèm `existingBookId` (kiểm tra trước khi move).
- [ ] `LocalBookFileReader`: `open/size/exists` resolve trong root, không nhận path tuyệt đối từ ngoài.

Backend — application/API:

- [ ] `AddBookService`: validate → tạo `Book(PROCESSING, QUEUED)` với metadata tạm (title = filename) → `enqueue(IMPORT)` → trả id.
- [ ] `ListBooksService` (paging, filter cơ bản), `DeleteBookService` (xoá DB + file copy, không đụng file gốc).
- [ ] `BookController`: `GET /api/books`, `POST /api/books`, `GET /api/books/{id}`, `DELETE /api/books/{id}`.

Job:

- [ ] `JobQueue` port + `PersistentJobQueue` (JPA) + bảng `processing_job`.
- [ ] `BookProcessingWorker`: `@Scheduled(fixedDelay=1000)` poll job, chạy IMPORT, cập nhật status; `@PreDestroy` dừng êm.
- [ ] Startup: job `RUNNING` → `QUEUED` (attempts giữ nguyên), xoá `<root>/.tmp/*`.
- [ ] `ImportBookStep`: gọi importer, lưu `file_path`/`content_hash`/`size`, chuyển `processing_status=QUEUED` cho bước sau.

Frontend:

- [ ] `features/library/api.ts` + hooks `useBooks`, `useAddBook`, `useDeleteBook`.
- [ ] `LibraryPage`: grid/list sách, badge trạng thái, empty state.
- [ ] `AddBookDialog`: input đường dẫn tuyệt đối (M2 chưa cần file picker native), submit → 202 → hiện sách với trạng thái `QUEUED`.
- [ ] Delete có confirm; hiển thị lỗi `DUPLICATE_BOOK` kèm nút "Mở sách cũ".

**Test**

- [ ] Unit `BookImporter`: copy đúng layout, hash đúng, file nguồn không đổi (so sánh mtime/size).
- [ ] Integration: POST → poll tới khi `file_path` có; POST lần 2 → 409 `DUPLICATE_BOOK`.
- [ ] Restart worker giữa job → job vẫn hoàn tất.
- [ ] DELETE → file trong library mất, file nguồn còn.

**DoD (demo):** thêm 1 PDF thật, thấy trong UI; `ls ~/.bookmind-dev/library` có file hash; xoá sách, file nguồn vẫn còn.

---

### M3 — Trích xuất text PDF/EPUB (3 ngày)

**Mục tiêu:** sách có `BookContent` (plainText + sections + pages) và xem được trong UI.

- [ ] `BookContent` lưu ở `book_content` (plainText có thể lớn → dùng `TEXT`, không load vào list API).
- [ ] `PdfBoxExtractor`: `PDFTextStripper` theo từng trang để dựng `PageSpan` chính xác; `hasTextLayer` = mật độ ký tự/trang ≥ ngưỡng; lưu `book_page(source=TEXT_LAYER)`.
- [ ] `EpubExtractor`: mở zip, đọc `container.xml` → OPF → spine order; parse XHTML bằng jsoup; mỗi tài liệu = 1 section (title từ `<title>`/`<h1>`/`<h2>`, depth theo heading); nối plainText và tính `charStart/charEnd`; `pages` rỗng.
- [ ] `TextExtractionRouter`: chọn extractor theo format; nếu text density < `min-text-density` → đánh dấu `needsOcr=true` (chưa chạy OCR ở M3, chỉ log + trạng thái).
- [ ] `ExtractTextStep` trong worker: chạy sau IMPORT, cập nhật `processing_status=EXTRACTING`, progress 30–50; lỗi → `FAILED` + reason.
- [ ] `GetBookTextService` + `GET /api/books/{id}/text?page=` và `?charStart=&charEnd=` (trả đoạn + locator để UI nhảy tới).

Frontend:

- [ ] `BookDetailPage`: thông tin sách, tabs "Nội dung" / "Trạng thái xử lý".
- [ ] Text viewer: phân trang theo page (PDF) hoặc section (EPUB), hiển thị số trang/chương.
- [ ] Hiển thị rõ khi sách "chưa có text layer, sẽ cần OCR".

**Test**

- [ ] Fixture PDF text layer 3 trang → 3 `PageSpan`, text đúng thứ tự.
- [ ] Fixture EPUB 2 chương → 2 section, `charStart/End` khớp `plainText.substring`.
- [ ] PDF scan (không text) → `needsOcr=true`, không crash.

**DoD:** mở 1 PDF và 1 EPUB, đọc được text trong app.

---

### M4 — OCR với PaddleOCR sidecar (4 ngày)

**Mục tiêu:** PDF scan tiếng Việt/Anh ra text dùng được, sửa tay được.

Sidecar:

- [ ] `scripts/paddle_ocr.py`: CLI `--lang vie|eng --dpi 200 --input <dir ảnh> --output <json>`; đọc ảnh theo thứ tự tên; trả JSON:

```json
{"pages":[{"page":1,"lines":[{"text":"...","confidence":0.97,"box":[[x1,y1],[x2,y2],[x3,y3],[x4,y4]]}]}]}
```

- [ ] Log ra `stderr`, kết quả ra `stdout`/file; exit code khác 0 khi lỗi; không đọc stdin (đơn giản, dễ debug).
- [ ] `scripts/requirements.txt` + `scripts/README.md`: venv, cài paddlepaddle/paddleocr, tải model, biến `BOOKMIND_OCR_PYTHON`.

Java:

- [ ] `PdfPageRenderer`: PDFBox `PDFRenderer` → PNG vào temp, DPI cấu hình (mặc định 200), giới hạn số trang/lần để không hết RAM.
- [ ] `PaddleOcrEngine implements OcrEngine`: tạo temp dir, render, gọi `ProcessBuilder`, timeout (`ocr.timeout-seconds`), parse JSON, gộp lines theo page, map page → `PageSpan`; xoá temp trong `finally`.
- [ ] `OcrStep`: chỉ chạy khi `needsOcr` hoặc `reprocess`; `processing_status=OCR`, progress 50–70; lưu `book_page(source=OCR)`; cập nhật `book_content`.
- [ ] `PUT /api/books/{id}/text`: lưu bản sửa, `manualEdited=true`, đánh dấu cần reindex (job `REINDEX` sẽ xử lý ở M5).
- [ ] `POST /api/books/{id}/reprocess`: enqueue lại từ EXTRACT (hoặc chỉ OCR nếu `?from=ocr`).
- [ ] Preflight: nếu không tìm thấy python/paddle → `FAILED` với message hướng dẫn cài, không treo job.

Frontend:

- [ ] Chọn ngôn ngữ OCR khi thêm sách + trong Book Detail (chỉ áp dụng lần chạy sau).
- [ ] Text editor cho trang đã OCR (textarea + nút Lưu), cảnh báo "sẽ re-index".
- [ ] Progress bar đọc từ `GET /api/books/{id}` (poll 2s; SSE để M10).

**Test**

- [ ] Fake sidecar (script shell in JSON cố định) → engine parse đúng, timeout hoạt động.
- [ ] Trang scan tiếng Việt thật → text chứa dấu, không rỗng.
- [ ] Sửa text → `manualEdited=true` và job REINDEX được enqueue.

**DoD:** 1 PDF scan 5 trang tiếng Việt → text hiển thị, sửa 1 trang và lưu được.

---

### M5 — Chunk + Embedding + Vector index (3 ngày)

**Mục tiêu:** sách chuyển `READY`, search được ở M6.

- [ ] `TokenCounter` (jtokkit, `cl100k_base`); nếu model khác thì vẫn dùng để **đếm tương đối**, ghi rõ trong docs.
- [ ] `ParagraphChunker` đúng thứ bậc trong architecture: section → paragraph → sliding window (overlap 10–15%, config `chunk-overlap`). `chunk-size`/`chunk-overlap` tính bằng **token**.
- [ ] `JpaChunkRepository` + `ChunkRepository` port; lưu `book_chunk` (unique `(book_id, ord)`), xoá chunk cũ trước khi ghi lại (idempotent).
- [ ] `EmbeddingModel` adapter: `POST {baseUrl}/embeddings`, batch 64–128, retry 429/5xx với backoff, validate `dimension`.
- [ ] `embedding_cache(content_hash, model, dim, vector)` — tránh embed lại chunk không đổi (FR-3.3, NFR-6).
- [ ] `SqliteVecVectorStore implements VectorStore`:
  - `ensureTable(dim)` tạo `chunk_embedding` virtual table theo dim lưu ở `app_meta`;
  - `upsert(bookId, items)` dùng transaction, xoá theo book trước;
  - `search(query, scope, topK)` dịch scope thành `book_id IN (...)` (precompute từ collection/tag);
  - `deleteByBook`.
- [ ] `IndexStep`: chunk → embed (cache hit trước) → upsert → `Book.markReady()` + publish `BookReady`; progress 70–100.
- [ ] `ReindexStep` cho sửa text/metadata ảnh hưởng chunk.
- [ ] Phát hiện đổi model/dim: so `app_meta.embedding_model` + `dim` với config; nếu khác → cảnh báo qua `/api/health`/settings và yêu cầu "Reindex toàn bộ".

**Test**

- [ ] Chunker: section dài → cắt theo paragraph, không chunk theo câu; overlap đúng; tổng text phủ gần đủ (không mất chữ).
- [ ] Fake embedding server (WireMock) trả vector cố định → số chunk = số vector, gọi đúng batch.
- [ ] Chạy lại INDEX 2 lần → không nhân bản chunk/vector.
- [ ] Đổi dim → lỗi rõ ràng, không ghi rác.

**DoD:** sách `READY`; `SELECT count(*) FROM book_chunk` = số vector; restart không mất index.

---

### M6 — Semantic search (1.5 ngày)

- [ ] `SearchService`: embed query (không cache) → `VectorStore.search` → lọc `minScore` → dedupe theo chunk → map metadata sách.
- [ ] `SearchScope` parse: `all`, `book:<id>`, `collection:<id>`, `tag:<name>`.
- [ ] `GET /api/search?q=&scope=&topK=` trả `[{chunkId, bookId, bookTitle, snippet, page, sectionPath, score, locator}]`.
- [ ] Snippet: cắt ±200 ký tự quanh chunk, highlight ở UI.
- [ ] Chuẩn hoá tiếng Việt: NFC + tìm cả dạng có/không dấu nếu không có kết quả (fallback, không thay embedding).

Frontend:

- [ ] `SearchPage`: ô query, scope selector (toàn thư viện / sách / collection), danh sách kết quả có score + nguồn.
- [ ] Click kết quả → `/books/:id?highlight=<chunkId>` → Book Detail cuộn tới đúng đoạn/trang.

**Test**

- [ ] 3 sách giả, query trúng đúng sách; scope `book:<id>` chỉ trả sách đó.
- [ ] `minScore` lọc đúng; topK đúng.
- [ ] Không có kết quả → mảng rỗng, UI có empty state.

**DoD:** gõ câu hỏi tiếng Việt, nhận kết quả đúng và mở đúng vị trí.

---

### M7 — Chat + RAG + SSE (3 ngày)

- [ ] `ConversationRepository` (list/create/rename/delete), `MessageRepository`, lưu citations.
- [ ] `PromptBuilder`: system prompt (tiếng Việt) + history window + context chunks có `[n]` + book/page; token budget chia: system ~500, history ~1500, còn lại cho context, chừa 800 cho answer.
- [ ] `ChatService`:
  - `GENERAL`: stream system + history;
  - `BOOK_CONTEXT`: embed câu hỏi → search(scope, topK) → prompt → stream;
  - on complete: parse `[n]` → map `ChunkLocator` → lưu `Message` + `message_citation`;
  - nếu không có chunk nào ≥ `minScore` → trả "không tìm thấy trong sách", không citation (FR-5.6).
- [ ] `OpenAiCompatibleChatModel`: `POST /chat/completions` với `stream=true`, parse SSE `data:` lines, map delta → `Flux<String>`; xử lý `[DONE]`, lỗi HTTP → `Flux.error`.
- [ ] `SseChatBridge`: subscribe Flux trên `boundedElastic`, emit `token`, `citation`, `done`, `error`; huỷ subscription khi client disconnect; timeout tổng.
- [ ] API: `GET/POST/PATCH/DELETE /api/conversations/**`, `POST /api/conversations/{id}/messages` (SSE).

Frontend:

- [ ] `ChatPage`: sidebar conversations, tạo mới, đổi tên, xoá.
- [ ] Mode switch General/Book; khi chọn Book phải chọn scope.
- [ ] Streaming append token; render citation chip `[1]` → click mở Book Detail đúng chỗ.
- [ ] Nút Stop (abort fetch); lỗi hiển thị rõ.

**Test**

- [ ] Fake chat server stream 5 token → UI nhận đủ, `done` có `messageId`.
- [ ] Câu hỏi ngoài phạm vi → không citation, đúng câu "không tìm thấy".
- [ ] Citation map đúng chunk/page.
- [ ] Huỷ giữa stream → không lưu message dở (hoặc lưu với cờ `cancelled`).

**DoD:** hỏi "sách nói gì về X", nhận câu trả lời stream + bấm citation nhảy đúng trang.

---

### M8 — Cấu hình AI + mã hoá key (1.5 ngày)

- [ ] `AesGcmSecretEncryptor`: AES-256-GCM, IV ngẫu nhiên mỗi lần, lưu `base64(iv||cipher)`; key file `secrets.key` tạo lần đầu, quyền `600` (POSIX), best-effort Windows.
- [ ] `AiProviderConfigRepository` + `ConfigureAiService`: validate URL (http/https), model không rỗng; lưu `encryptedApiKey`; chỉ 1 config `active`.
- [ ] `ProviderRegistry`: tạo `ChatModel`/`EmbeddingModel` từ config active; cache theo config version; refresh khi đổi.
- [ ] `GET /api/config/ai` trả key đã mask (`sk-****abcd`), `PUT` nhận key mới hoặc `null` = giữ nguyên.
- [ ] `POST /api/config/ai/test`: thử embed 1 token (hoặc chat 1 token), trả `{ok, latencyMs, model, dimension?, error?}`.
- [ ] `LocalModelClient` không cần class riêng: cùng OpenAI-compatible, chỉ khác `baseUrl` (ghi chú trong docs).

Frontend:

- [ ] `SettingsPage`: form baseUrl, modelChat, modelEmbedding, API key (password, placeholder mask), nút Test, lưu.
- [ ] Hiển thị cảnh báo nếu chưa cấu hình AI (chặn search/chat bằng CTA sang Settings).

**Test**

- [ ] Encrypt/decrypt round-trip; ciphertext khác nhau cho cùng plaintext (IV ngẫu nhiên).
- [ ] Response không bao giờ chứa key thật; log không chứa key.
- [ ] Test connection: 200/401/timeout → thông báo đúng.

**DoD:** nhập key OpenAI-compatible, test xanh, restart vẫn dùng được, DB không có plaintext key.

---

### M9 — Metadata, tag, collections (2 ngày)

- [ ] `PATCH /api/books/{id}`: title, author, tags[], description; validate độ dài.
- [ ] Collections: CRUD + thêm/bớt sách; `GET /api/books?collectionId=&tag=&format=&sort=`.
- [ ] Sort: title, author, addedAt, sizeBytes; paging `page`, `size` (mặc định 24).
- [ ] Frontend: edit metadata dialog, tag input, sidebar collections, filter/sort bar.

**Test**

- [ ] PATCH cập nhật đúng, không đổi `contentHash`/`filePath`.
- [ ] Filter/sort/paging trả đúng; collection remove không xoá sách.

**DoD:** tạo collection, gán sách, lọc theo tag/collection.

---

### M10 — Hardening, hiệu năng, đóng gói (3 ngày)

- [ ] Startup task: dọn `.tmp`, reset job `RUNNING`, optional `verify-hash-on-startup` (đối chiếu hash file trong library, báo sách lỗi).
- [ ] Backup/restore: script `scripts/backup.sh` nén `~/.bookmind` (trừ `logs/`); docs restore + lưu ý path tương đối.
- [ ] `GET /api/books/{id}/events` SSE tiến độ; frontend ưu tiên SSE, fallback polling 2s.
- [ ] Rate/size limit: `spring.servlet.multipart` không dùng (không upload), nhưng giới hạn body JSON; timeout OCR/AI rõ ràng.
- [ ] Logging: structured, chỉ id/trạng thái; không log text sách/API key; log rotation trong `logs/`.
- [ ] Error mapping đầy đủ: `DUPLICATE_BOOK`, `FILE_NOT_FOUND`, `UNSUPPORTED_FORMAT`, `FILE_TOO_LARGE`, `OCR_UNAVAILABLE`, `EMBEDDING_DIM_MISMATCH`, `AI_NOT_CONFIGURED`.
- [ ] E2E Playwright: thêm sách (fixture PDF) → chờ READY → search → chat (AI stub) → xoá.
- [ ] Perf sanity: seed 1000 sách giả (metadata + chunk giả) → đo search p95, mở list; ghi số vào `docs/perf.md`.
- [ ] Đóng gói 1 jar:

```bash
cd frontend && npm ci && npm run build
rm -rf ../backend/src/main/resources/static/*
cp -r dist/* ../backend/src/main/resources/static/
cd ../backend && ./gradlew clean bootJar
java -jar build/libs/bookmind-*.jar
```

- [ ] (Tuỳ chọn) Gradle task `buildFrontend` + `processResources.dependsOn(buildFrontend)` để build 1 lệnh.
- [ ] README: yêu cầu, cài đặt, chạy dev, build, cấu hình AI, OCR sidecar, backup/restore, troubleshooting.

**DoD:** máy sạch (không Node) chạy `java -jar`, mở `http://localhost:8080`, làm được luồng chính; E2E xanh.

---

## 5. Schema DB (Flyway)

### V1 — bảng lõi

```sql
CREATE TABLE book (
  id TEXT PRIMARY KEY,
  file_path TEXT NOT NULL UNIQUE,
  content_hash TEXT NOT NULL,
  format TEXT NOT NULL,                -- PDF | EPUB
  status TEXT NOT NULL,                -- PROCESSING | READY | FAILED
  processing_status TEXT NOT NULL,     -- QUEUED | IMPORTING | EXTRACTING | OCR | INDEXING | DONE | FAILED
  progress INTEGER NOT NULL DEFAULT 0,
  ocr_language TEXT,
  failure_reason TEXT,
  metadata_json TEXT NOT NULL,
  added_at TEXT NOT NULL
);
CREATE UNIQUE INDEX ux_book_hash ON book(content_hash);

CREATE TABLE book_content (
  book_id TEXT PRIMARY KEY REFERENCES book(id) ON DELETE CASCADE,
  plain_text TEXT NOT NULL,
  sections_json TEXT NOT NULL DEFAULT '[]',
  has_text_layer INTEGER NOT NULL DEFAULT 1,
  manual_edited INTEGER NOT NULL DEFAULT 0,
  updated_at TEXT NOT NULL
);

CREATE TABLE book_page (
  book_id TEXT NOT NULL REFERENCES book(id) ON DELETE CASCADE,
  page_no INTEGER NOT NULL,
  text TEXT NOT NULL,
  source TEXT NOT NULL,                -- TEXT_LAYER | OCR
  PRIMARY KEY (book_id, page_no)
);

CREATE TABLE book_chunk (
  id TEXT PRIMARY KEY,
  book_id TEXT NOT NULL REFERENCES book(id) ON DELETE CASCADE,
  ord INTEGER NOT NULL,
  text TEXT NOT NULL,
  chapter TEXT,
  page_from INTEGER,
  page_to INTEGER,
  char_start INTEGER NOT NULL,
  char_end INTEGER NOT NULL,
  content_hash TEXT NOT NULL,
  token_count INTEGER NOT NULL,
  UNIQUE (book_id, ord)
);
CREATE INDEX ix_chunk_book ON book_chunk(book_id);

CREATE TABLE collection (
  id TEXT PRIMARY KEY, name TEXT NOT NULL, created_at TEXT NOT NULL
);
CREATE TABLE collection_book (
  collection_id TEXT NOT NULL REFERENCES collection(id) ON DELETE CASCADE,
  book_id TEXT NOT NULL REFERENCES book(id) ON DELETE CASCADE,
  PRIMARY KEY (collection_id, book_id)
);

CREATE TABLE conversation (
  id TEXT PRIMARY KEY, title TEXT NOT NULL, mode TEXT NOT NULL,
  scope_json TEXT, created_at TEXT NOT NULL, updated_at TEXT NOT NULL
);
CREATE TABLE message (
  id TEXT PRIMARY KEY,
  conversation_id TEXT NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
  role TEXT NOT NULL, content TEXT NOT NULL, mode TEXT,
  created_at TEXT NOT NULL
);
CREATE TABLE message_citation (
  message_id TEXT NOT NULL REFERENCES message(id) ON DELETE CASCADE,
  ref_no INTEGER NOT NULL, chunk_id TEXT NOT NULL,
  book_id TEXT NOT NULL, page INTEGER, snippet TEXT,
  PRIMARY KEY (message_id, ref_no)
);

CREATE TABLE ai_provider_config (
  id TEXT PRIMARY KEY,
  base_url TEXT NOT NULL, model_chat TEXT NOT NULL, model_embedding TEXT NOT NULL,
  encrypted_api_key TEXT, is_active INTEGER NOT NULL DEFAULT 0,
  embedding_dim INTEGER, updated_at TEXT NOT NULL
);

CREATE TABLE processing_job (
  id TEXT PRIMARY KEY, book_id TEXT NOT NULL REFERENCES book(id) ON DELETE CASCADE,
  type TEXT NOT NULL, status TEXT NOT NULL, attempts INTEGER NOT NULL DEFAULT 0,
  next_run_at TEXT, last_error TEXT,
  created_at TEXT NOT NULL, updated_at TEXT NOT NULL
);
CREATE INDEX ix_job_status ON processing_job(status, next_run_at);

CREATE TABLE app_meta (key TEXT PRIMARY KEY, value TEXT NOT NULL);

CREATE TABLE embedding_cache (
  content_hash TEXT NOT NULL, model TEXT NOT NULL, dim INTEGER NOT NULL,
  vector BLOB NOT NULL, created_at TEXT NOT NULL,
  PRIMARY KEY (content_hash, model, dim)
);
```

### Vector table (tạo runtime, không qua Flyway)

```sql
CREATE VIRTUAL TABLE chunk_embedding USING vec0(
  chunk_id TEXT PRIMARY KEY,
  book_id TEXT PARTITION KEY,   -- nếu sqlite-vec bản dùng hỗ trợ partition key
  embedding float[<dim>]
);
```

- `<dim>` lấy từ lần embed đầu tiên, ghi `app_meta.embedding_dim` + `embedding_model`.
- Đổi model/dim → xoá bảng, tạo lại, enqueue `REINDEX` toàn bộ. Ghi rõ trong Settings UI.

### Migration dự kiến

| Version | Nội dung | Milestone |
|---|---|---|
| V1 | Bảng lõi ở trên | M1 |
| V2 | `book_content` + `book_page` (nếu tách khỏi V1) | M3 |
| V3 | `embedding_cache`, cột `embedding_dim` | M5 |
| V4 | Index/filter bổ sung cho list/search | M9 |

---

## 6. API contract (ví dụ then chốt)

### POST `/api/books`

```json
{ "path": "/home/me/books/sach.pdf", "ocrLanguage": "vie" }
```

`202 Accepted`

```json
{ "id": "5b0c...", "status": "PROCESSING", "processingStatus": "QUEUED" }
```

Lỗi: `409 DUPLICATE_BOOK` (`details.existingBookId`), `400 INVALID_PATH`, `413 FILE_TOO_LARGE`, `415 UNSUPPORTED_FORMAT`.

### GET `/api/books/{id}`

```json
{
  "id": "5b0c...", "title": "Sách", "author": null, "format": "PDF",
  "status": "READY", "processingStatus": "DONE", "progress": 100,
  "pageCount": 320, "sizeBytes": 12345678, "tags": [], "collectionIds": [],
  "failureReason": null
}
```

### GET `/api/search?q=...&scope=book:5b0c&topK=10`

```json
[{
  "chunkId": "...", "bookId": "5b0c...", "bookTitle": "Sách",
  "snippet": "...", "page": 42, "sectionPath": ["Chương 3"],
  "score": 0.81, "locator": { "charStart": 12000, "charEnd": 12600, "page": 42 }
}]
```

### POST `/api/conversations/{id}/messages` (SSE)

```
event: token
data: {"text":"Sách "}

event: citation
data: {"refNo":1,"chunkId":"...","bookId":"...","page":42}

event: done
data: {"messageId":"...","citations":[1]}
```

---

## 7. Frontend — route & thành phần

| Route | Page | Thành phần chính |
|---|---|---|
| `/` | `LibraryPage` | grid sách, AddBookDialog, filter/sort, collection sidebar |
| `/books/:id` | `BookDetailPage` | metadata, text viewer (page/section), progress, edit text (OCR), reprocess |
| `/search` | `SearchPage` | query bar, scope, result list, highlight |
| `/chat` | `ChatPage` | conversation list, mode + scope, message stream, citation chips |
| `/settings` | `SettingsPage` | AI form, test connection, reindex all, storage info |

Quy ước:

- Mọi gọi API qua `shared/api/client.ts`; lỗi map sang `ErrorResponse`.
- Query key: `['books', filters]`, `['book', id]`, `['conversations']`, `['messages', id]`.
- Mutation invalidate đúng key; không refetch toàn bộ.
- Mọi string qua `t()`; `vi.json` là nguồn chính, `en.json` cập nhật song song.
- SSE: một `shared/api/sse.ts` dùng `fetch` + `ReadableStream` để có thể gửi header và abort.

---

## 8. Ma trận kiểm thử

| Tầng | Test gì | Công cụ | Khi nào |
|---|---|---|---|
| Domain | Invariant, state transition, FilePath | JUnit 5 + AssertJ | M1, cập nhật mỗi M |
| Application | Service với port mock (hash, AI, vector) | Mockito | M2+ |
| Infrastructure | SQLite thật (file tạm), importer, chunker, vector store | Spring Boot Test | M1, M2, M5 |
| AI adapter | Request/response, stream, retry, lỗi 401/429 | WireMock | M5, M7, M8 |
| Presentation | Controller + DTO contract + error mapping | `@WebMvcTest`, MockMvc | M2+ |
| OCR | Fake sidecar, timeout, JSON hỏng | JUnit + script shell | M4 |
| Frontend | Hook/component, streaming | Vitest + Testing Library + MSW | M2+ |
| E2E | Add → READY → search → chat → delete | Playwright (AI stub) | M10, chạy lại mỗi release |

Tiêu chí tối thiểu trước khi coi 1 milestone là xong:

- [ ] Test mới cho hành vi mới, không chỉ test "không crash".
- [ ] `./gradlew test` xanh; `npm test` xanh.
- [ ] Không có `TODO` bí mật trong code (hoặc ghi issue).
- [ ] Đã chạy tay luồng demo 1 lần trên DB mới.

---

## 9. Rủi ro và cách giảm thiểu

| Rủi ro | Khả năng | Ảnh hưởng | Giảm thiểu |
|---|---|---|---|
| sqlite-vec khó load trong Java/pool | Trung bình | Cao (search) | Làm `SqliteVecVectorStore` đầu tiên trong M5; nếu kẹt quá nửa ngày → fallback Qdrant local qua `VectorStore` port |
| Flyway thiếu module SQLite | Trung bình | Trung bình | Làm migration đầu tiên ngay trong M1; nếu thiếu module → thêm dependency hoặc pin Flyway 9 |
| PaddleOCR nặng/khó cài trên máy người dùng | Cao | Trung bình | Preflight + thông báo hướng dẫn; OCR là job có thể fail và retry; không chặn sách có text layer |
| OCR chậm, tốn RAM với sách 500 trang | Cao | Trung bình | Render theo batch 10–20 trang, DPI 200, timeout/trang, `parallel-jobs=1` |
| Đổi embedding model làm lệch dim | Trung bình | Cao | Lưu model+dim, chặn search nếu lệch, UI yêu cầu reindex |
| Sách 200MB trên ổ ngoài, hash chậm | Trung bình | Trung bình | Hash trong luồng copy (1 lượt đọc), buffer lớn, job nền |
| Worker crash giữa job | Trung bình | Trung bình | Trạng thái trong DB, reset RUNNING→QUEUED khi startup, bước idempotent |
| Tiếng Việt có/không dấu | Cao | Thấp | Chuẩn hoá NFC; fallback query không dấu; snippet giữ nguyên bản gốc |
| Rò API key qua log/response | Thấp | Cao | `ApiKey` che `toString`, mask DTO, test log |
| JVM + OCR + embedding cùng lúc thiếu RAM | Trung bình | Trung bình | `parallel-jobs=1`, batch embed 64, đóng process OCR đúng cách |
| Windows: quyền file, path, symlink | Trung bình | Trung bình | Không symlink (ADR-9), dùng `Path`, key file best-effort, test trên Windows trước release |

---

## 10. Definition of Done & quy ước

Một milestone chỉ "Done" khi:

- [ ] Demo được luồng trong bảng milestone trên **DB mới** (không dựa vào dữ liệu cũ).
- [ ] Có test tự động cho phần mới; test cũ vẫn xanh.
- [ ] Không lệch `architecture.md`: nếu lệch, đã cập nhật docs trong cùng PR/commit.
- [ ] Không có secret/đường dẫn máy cá nhân hard-code.
- [ ] README/`.env.example` cập nhật nếu thêm biến môi trường.

Quy ước commit (gợi ý Conventional Commits):

```
feat(book): import file vào library với hash + atomic move
fix(worker): reset job RUNNING khi restart
test(chunker): cover overlap và section dài
docs(plan): cập nhật tiến độ M3
```

Nhánh: `main` luôn chạy được; làm trên `feat/<milestone>-<việc>` nếu cần.

---

## 11. Lịch trình gợi ý (full-time)

| Tuần | Nội dung |
|---|---|
| 1 | M0 + M1 |
| 2 | M2 (import + worker + UI list) |
| 3 | M3 (extract) + M8 (AI config) |
| 4 | M5 (chunk/embed/index) + M6 (search) |
| 5 | M7 (chat/RAG/SSE) |
| 6 | M4 (OCR) + M9 (metadata/collections) |
| 7 | M10 (hardening, E2E, đóng gói, README) |

Nếu làm bán thời gian: nhân đôi thời lượng, giữ nguyên thứ tự.

---

## 12. Không làm ở giai đoạn này (out of scope có chủ đích)

- Multi-user, auth, sync, mobile app (đã loại trong requirements).
- Upload qua browser (chỉ nhận đường dẫn file local — theo kiến trúc local-first).
- Reranking/LLM-based search, hybrid BM25 (có thể thêm sau qua `SearchService`).
- Dịch tự động, TTS, highlight/annotation, flashcard.
- Hàng đợi phân tán/Redis, microservice, Kubernetes.
- Tự động tải model AI local; chỉ cấu hình `baseUrl` tới Ollama/LM Studio.

---

## 13. Checklist tổng (in ra và tick dần)

- [ ] M0 Bootstrap backend + frontend
- [ ] M1 Domain + Flyway V1 + repository
- [ ] M2 Import + job queue + library UI
- [ ] M3 Extract PDF/EPUB + text viewer
- [ ] M8 AI config + mã hoá key
- [ ] M5 Chunk + embed + vector index
- [ ] M6 Semantic search + điều hướng kết quả
- [ ] M7 Chat + RAG + SSE + citations
- [ ] M4 OCR sidecar + sửa text
- [ ] M9 Metadata + tag + collections
- [ ] M10 SSE tiến độ, backup/restore, E2E, đóng gói jar, README
