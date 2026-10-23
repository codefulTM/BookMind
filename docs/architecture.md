# BookMind — Kiến trúc hệ thống (Architecture)

Tài liệu này mô tả kiến trúc kỹ thuật của BookMind: một ứng dụng desktop-local, single user, gồm backend Java Spring Boot và frontend React, áp dụng Clean Architecture.

Dựa trên: [requirements.md](./requirements.md) · Kế hoạch thi công: [implementation-plan.md](./implementation-plan.md)

---

## 1. Nguyên tắc kiến trúc

| Nguyên tắc | Áp dụng |
|---|---|
| **Local-first, single user** | Không auth, không multi-tenant, không cloud sync. Backend bind `127.0.0.1`. |
| **File sách thuộc thư viện** | Import **copy** file vào library root của app (content-addressed), ghi/verify bằng SHA-256. Không bao giờ sửa/xoá file nguồn ngoài app. Không dùng symlink (xem ADR-9). |
| **Clean Architecture** | Domain không phụ thuộc framework/thư viện. Application điều phối use case. Infrastructure là adapter. Presentation là adapter vào. |
| **Provider/Store thay thế được** | AI provider và vector store nằm sau interface ở domain/application. |
| **Job nền bền vững** | Pipeline xử lý sách chạy queue, trạng thái lưu DB, retry được, không mất khi restart. |

---

## 2. Stack công nghệ

### Backend

| Thành phần | Lựa chọn | Ghi chú |
|---|---|---|
| Ngôn ngữ / runtime | Java 21 (LTS) | Virtual threads cho I/O blocking (OCR, AI call, đọc file) |
| Framework | Spring Boot 3.x (Spring Framework 6) | Nền tảng chung: IoC container, auto-configuration, property binding, lifecycle. Các starter dùng trong dự án: `spring-boot-starter-web`, `-validation`, `-data-jpa`, `-actuator`, `-test`. Spring Boot không bắt buộc phải có web — ở đây web starter chỉ phục vụ lớp presentation REST/SSE |
| API | REST + SSE | SSE cho streaming chat |
| Persistence | SQLite + Spring Data JPA | Đơn file, phù hợp local, backup = copy file |
| Migration | Flyway | Version hoá schema |
| Vector store | SQLite + `sqlite-vec` (port), mặc định | Có thể thay bằng LanceDB/Qdrant qua interface |
| PDF | Apache PDFBox | Text layer, render trang |
| EPUB | epublib hoặc XHTML parser tự viết | Tách chương + text |
| OCR | PaddleOCR (PP-OCRv4/v5) | `vie`, `eng`; chạy qua Python sidecar (subprocess) — chính xác hơn Tesseract, nhất là tiếng Việt |
| HTTP client | Spring `WebClient` / `RestClient` | Gọi OpenAI-compatible API |
| Build | Gradle (Kotlin DSL) | |
| Test | JUnit 5, AssertJ, Testcontainers (nếu cần), Mockito | |

### Frontend

| Thành phần | Lựa chọn |
|---|---|
| Build | Vite |
| Ngôn ngữ | TypeScript (strict) |
| Framework | React 18+ |
| Routing | React Router |
| Server state | TanStack Query |
| Local UI state | Zustand |
| Styling | Tailwind CSS + shadcn/ui |
| i18n | i18next (`vi`, `en`) |
| Test | Vitest + Testing Library, Playwright (E2E) |

### Đóng gói

- Dev: Vite dev server (5173) proxy `/api` → Spring Boot (8080).
- Dist: React build tĩnh được copy vào `static/` của Spring Boot → một jar duy nhất, mở tại `http://localhost:8080`.

---

## 3. Sơ đồ tổng thể

```
┌───────────────────────────────────────────────────────┐
│ Browser (React SPA)                                   │
│  Library · Search · Chat · Settings · Processing      │
└───────────────┬───────────────────────────────────────┘
                │ REST / SSE (localhost)
┌───────────────▼───────────────────────────────────────┐
│ Presentation (Spring MVC)                             │
│  Controllers · DTO · ExceptionHandler · SSE Emitter   │
├───────────────────────────────────────────────────────┤
│ Application                                           │
│  Use cases (services) · Ports (out) · Command/Query   │
├───────────────────────────────────────────────────────┤
│ Domain                                                │
│  Aggregates · Value Objects · Domain Events · Policy  │
├───────────────────────────────────────────────────────┤
│ Infrastructure (adapters)                             │
│   JPA repos · Files · PDF/EPUB/OCR · Vector store     │
│  AI providers · Job queue · Encryption · Config       │
└───┬───────────┬────────────┬────────────┬─────────────┘
    │           │            │            │
  SQLite    Local files   PaddleOCR   AI API / local model
```

---

## 4. Cấu trúc package (Clean Architecture)

```
com.bookmind
├── domain
│   ├── book          Book, BookId, BookFormat, BookStatus, ProcessingStatus, FilePath, Metadata
│   ├── collection    Collection, CollectionId
│   ├── content       BookContent, ExtractedText, Section, PageSpan, Chunk, ChunkId, Locator
│   ├── search        SearchQuery, SearchResult, Score, SearchScope
│   ├── chat          Conversation, ConversationId, Message, ChatMode, Citation
│   ├── provider      AiProviderConfig, ProviderId, ModelName, ApiKey (VO)
│   └── shared        DomainException, Page/PageRequest (nếu cần)
│
├── application
│   ├── port
│   │   └── out       BookRepository, CollectionRepository, ConversationRepository,
│   │                 BookFileReader, TextExtractor, OcrEngine, Chunker,
│   │                 EmbeddingModel, ChatModel, VectorStore, JobQueue,
│   │                 SecretEncryptor, AppConfigRepository, Clock
│   ├── service       AddBookService, DeleteBookService, ListBooksService,
│   │                 ExtractTextService, SearchService, ChatService,
│   │                 ConfigureAiService, ... (transaction boundary ở đây)
│   └── dto           lệnh/kết quả nội bộ của use case
│
├── infrastructure
│   ├── persistence
│   │   ├── jpa       entities, repositories, mappers, adapter impl
│   │   └── vector    SqliteVecVectorStore
│   ├── file          LocalBookFileReader, PathValidator, BookImporter,
│   │                 FileHasher, LibraryRootResolver
│   ├── extraction    PdfBoxExtractor, EpubExtractor, PaddleOcrEngine,
│   │                 TextExtractionRouter, ParagraphChunker
│   ├── ai            OpenAiCompatibleClient, *EmbeddingModel, *ChatModel,
│   │                 ProviderRegistry, LocalModelClient
│   ├── job           PersistentJobQueue, BookProcessingWorker, JobRetryPolicy
│   ├── security      AesGcmSecretEncryptor
│   └── config        SpringConfig, DataSourceConfig, WebConfig
│
└── presentation
    ├── rest
    │   ├── book        BookController
    │   ├── search      SearchController
    │   ├── chat        ChatController (SSE)
    │   ├── config      AiConfigController
    │   ├── job         ProcessingController
    │   └── dto         request/response records + mappers
    └── error           GlobalExceptionHandler, ErrorResponse
```

Quy tắc: `presentation` và `infrastructure` chỉ được phụ thuộc vào `application`/`domain`. Không có chiều ngược lại.

**Không có port in.** Use case là class cụ thể (`AddBookService`), controller inject trực tiếp. Lý do: `presentation → application` đã là detail → policy, đúng chiều phụ thuộc, không cần DIP. Không có nhu cầu nhiều implementation hay tách module, nên interface `*UseCase` chỉ là ceremony. Port **out** vẫn bắt buộc vì `application → JPA/AI/vector store` là policy → detail (sai chiều), phải đảo qua interface.

---

## 5. Domain model (tóm tắt)

### Book (aggregate root)

```
Book
├── BookId (UUID)
├── FilePath           tương đối so với library root
├── ContentHash        SHA-256, dùng để dedupe và verify integrity
├── BookFormat         PDF | EPUB
├── Metadata           title, author, coverPath(derived), pageCount(nullable), sizeBytes, addedAt
├── BookStatus         PROCESSING | READY | FAILED
├── ProcessingStatus   QUEUED | IMPORTING | EXTRACTING | OCR | INDEXING | DONE | FAILED
├── progressPercent
├── ocrLanguage        vie | eng | ...
└── collectionIds
```

Bất biến:

- `FilePath` là relative path nằm trong library root, không chứa `..`, đuôi `.pdf`/`.epub`; file nguồn chỉ đọc, không bị sửa/xoá.
- Book `FAILED` phải kèm `failureReason`; retry hợp lệ từ `FAILED`.
- Xuất bản domain event: `BookAdded`, `BookReady`, `BookFailed`, `BookTextChanged`.

### Content model (không phải sách nào cũng có chương/trang)

Nguyên tắc: **toạ độ gốc là offset ký tự trong `plainText`**; page và section chỉ là nhãn tuỳ chọn gắn lên trên. Không format nào bị ép phải có đủ cấu trúc.

```java
record BookContent(BookId bookId, String plainText,
                   List<Section> sections,   // rỗng nếu không detect được mục lục
                   List<PageSpan> pages,     // rỗng với EPUB/TXT
                   boolean hasTextLayer, Stats stats) {}

record Section(SectionId id, String title, int depth, int order,
               SectionId parentId, int charStart, int charEnd) {}  // parentId null = gốc

record PageSpan(int pageNumber, int charStart, int charEnd) {}     // chỉ là index, không phải entity

record Locator(int charStart, int charEnd,
               Integer page,                 // nullable: chỉ PDF/scan
               List<SectionId> sectionPath) {}  // rỗng nếu sách không có mục lục

record Chunk(ChunkId id, BookId bookId, int ordinal,
             String text, Locator locator, String hash, int tokenCount) {}
```

Vì sao **không** gộp page vào `Section`: page và chapter là hai trục vuông góc (chương 3 có thể bắt đầu giữa trang 41), không xếp thành một cây được. Vì sao **không** có `Page` entity: page không có hành vi/invariant, chỉ là ánh xạ số trang → khoảng offset.

| Format | `sections` | `pages` | UI hiển thị |
|---|---|---|---|
| PDF text layer | thường rỗng | đầy đủ | "tr. 42" |
| PDF scan + OCR | rỗng | đầy đủ | "tr. 42" |
| EPUB | có thể rỗng | rỗng | "Ch. 3" hoặc "% sách" |
| TXT/MD | rỗng | rỗng | "% sách" |

Thứ bậc fallback khi chunk: có `Section` → chunk theo section (cắt tiếp theo paragraph nếu quá dài); không có → theo paragraph; paragraph quá dài hoặc không tách được (TXT không có dòng trống) → cửa sổ trượt token, overlap 10–15%. **Không chunk theo câu** — chunk quá nhỏ, mất ngữ cảnh và nổ số lượng.

### Các aggregate khác

| Aggregate | Nội dung |
|---|---|
| `Collection` | id, name, bookIds |
| `BookContent` | bookId, plainText, sections[], pages[], hasTextLayer, stats |
| `Chunk` | id, bookId, ordinal, text, locator, hash, tokenCount (embedding lưu ở vector store) |
| `Conversation` | id, title, mode (`BOOK_CONTEXT`/`GENERAL`), scope(bookIds/collectionId), messages[] |
| `Message` | role, content, citations[] |
| `AiProviderConfig` | id, baseUrl, modelChat, modelEmbedding, encryptedApiKey, active |

### Port quan trọng

```java
public interface BookFileReader { InputStream open(FilePath path); long size(FilePath path); boolean exists(FilePath path); }
public interface BookImporter { ImportResult importFile(AbsolutePath source); }   // copy + hash + atomic rename
public interface FileHasher { String sha256(AbsolutePath path); }
public interface TextExtractor { BookContent extract(BookFileInput input); }
public interface OcrEngine { BookContent ocr(BookFileInput input, OcrLanguage lang); }
public interface Chunker { List<Chunk> chunk(BookContent content); }
public interface EmbeddingModel { List<float[]> embed(List<String> texts); int dimension(); }
public interface ChatModel { Flux<String> stream(ChatPrompt prompt); }
public interface VectorStore {
    void upsert(BookId id, List<ChunkEmbedding> items);
    List<ScoredChunk> search(float[] query, SearchScope scope, int topK);
    void deleteByBook(BookId id);
}
public interface JobQueue { JobId enqueue(JobPayload payload); Optional<JobPayload> poll(); void complete(JobId id); void fail(JobId id, String reason); }
public interface SecretEncryptor { String encrypt(String plain); String decrypt(String cipher); }
```

---

## 6. Pipeline xử lý sách (background)

```
AddBookService
  └─ validate path → lưu Book(PROCESSING/QUEUED) → enqueue IMPORT job

BookProcessingWorker (persistent queue)
  0. IMPORT    SHA-256 → dedupe check → copy vào temp trong library root
               → fsync + atomic rename → FilePath = relative path
  1. EXTRACT   đọc file → detect format
  2. OCR       nếu không có text layer hoặc mật độ text thấp → PaddleOCR(lang)
  3. CHUNK     ParagraphChunker, gắn locator page/chapter
  4. EMBED     batch 64–128 chunk → EmbeddingModel
  5. INDEX     VectorStore.upsert
  6. DONE      Book → READY, phát BookReady
  FAILED       lưu lý do, cho phép retry từ bước lỗi
```

Ghi chú cho bước IMPORT:

- **Dedupe:** nếu `ContentHash` đã tồn tại trong thư viện → không copy, trả lỗi `DUPLICATE_BOOK` kèm id sách cũ (người dùng quyết định mở sách cũ hay vẫn thêm bản ghi mới).
- **Atomic:** copy vào `<root>/.tmp/<uuid>` rồi `Files.move(ATOMIC_MOVE)`; crash giữa chừng để lại file tạm, dọn khi khởi động.
- **Layout:** `<root>/<sha[0:2]>/<sha[2:4]>/<sha>.<ext>` — không trùng tên, không collision; tên file gốc lưu ở metadata để hiển thị.
- **Hash một lượt đọc:** tính SHA-256 trong luồng copy, không đọc file hai lần (quan trọng với sách 200MB trên ổ ngoài).

Đặc tính:

- **Bền vững:** trạng thái job trong bảng `processing_job`; restart backend → worker tiếp tục job dở dang (`EXTRACTING` → reset về `QUEUED`).
- **Idempotent:** mỗi bước ghi kết quả vào DB/vector store theo `bookId` + `step`; chạy lại ghi đè chứ không nhân bản (xem FR-3.3).
- **Retry:** exponential backoff, tối đa N lần; hết lượt → `FAILED` + nút chạy lại.
- **Tiến độ:** worker cập nhật `progressPercent` → frontend poll `GET /api/books/{id}` hoặc SSE `GET /api/books/{id}/events`.
- **Concurrency:** giới hạn 1–2 job song song (IO/CPU nặng), config được.
- **Sửa OCR thủ công:** lưu `ExtractedText` bản người dùng sửa, đánh dấu `manualEdited=true`, enqueue `REINDEX` (chunk + embed lại) — không chạy lại OCR.

Lựa chọn queue: bảng DB tự viết (poll + `SELECT ... FOR UPDATE SKIP LOCKED` nếu cần) thay vì Redis/RabbitMQ, để giữ ứng dụng chạy như một tiến trình đơn.

---

## 7. Semantic search

```
Query → EmbeddingModel.embed([query]) → VectorStore.search(scope, topK)
      → merge/dedupe → score (cosine) → map Chunk + Book metadata
      → SearchResult[] (snippet, bookTitle, page/chapter, score)
```

- **Scope:** toàn thư viện / bookIds / collectionId / tag — dịch thành filter trong vector store hoặc precompute tập `bookIds` hợp lệ.
- **topK mặc định** 10, cấu hình được; lọc ngưỡng `minScore`.
- **Không cache query embedding** ở MVP (khác nhau mỗi lần); cache embedding của chunk theo `(bookId, chunkHash)` để tránh re-embedding khi không đổi.

---

## 8. Chat và RAG

```
ChatService(question, conversation)
  ├─ mode = GENERAL  → ChatModel.stream(system + history)
  └─ mode = BOOK_CONTEXT
        ├─ embed(question) → VectorStore.search(scope, topK)
        ├─ build prompt: system + retrieved chunks (kèm [n] + book/page) + history
        └─ ChatModel.stream(...) → SSE "token" events
              → on complete: parse citation markers → lưu Message với citations
                                → SSE "done" event (messageId, citations[])
```

- **Streaming:** `SseEmitter`; event `token`, `citation`, `done`, `error`.
- **Chống bịa (FR-5.6):** system prompt bắt buộc chỉ trả lời từ context; nếu context không đủ → trả câu trả lời "không tìm thấy trong sách" + không citation.
- **Trích dẫn:** mỗi chunk đưa vào prompt có `refId`; model sinh `[1]`, `[2]`; backend map về `ChunkLocator` để UI điều hướng.
- **Lịch sử:** lưu mọi message; title auto từ câu hỏi đầu tiên, cho đổi tên/xoá.
- **Giới hạn context:** token budget cấu hình; cắt history theo cửa sổ gần nhất.
- **Chuyển mode:** conversation giữ `mode` hiện tại; khi chuyển sang BOOK_CONTEXT yêu cầu chọn scope. Tin nhắn cũ giữ mode lúc gửi.

---

## 9. AI provider abstraction

```java
public interface ChatModel { Flux<String> stream(ChatPrompt prompt); }
public interface EmbeddingModel { List<float[]> embed(List<String> texts); int dimension(); }
```

- `OpenAiCompatibleClient` dùng chung cho OpenAI, LM Studio, Ollama (OpenAI-compat), OpenRouter, v.v. Chỉ khác `baseUrl` + `model`.
- `ProviderRegistry` (infrastructure) tạo `ChatModel`/`EmbeddingModel` từ `AiProviderConfig` đang active; application chỉ thấy interface.
- **Không hard-code provider.** Thêm provider = thêm adapter hoặc config, không đổi domain.
- **Test connection:** gọi 1 request nhỏ (embed 1 token hoặc chat 1 token) và trả kết quả rõ ràng.
- **API key:** mã hoá AES-GCM bằng khóa cục bộ sinh lần đầu, lưu trong thư mục app; DTO trả về luôn mask (`sk-****abcd`).
- **Local model:** cùng interface; cấu hình `baseUrl` trỏ tới server local (Ollama/LM Studio).

---

## 10. Lưu trữ dữ liệu

Thư mục dữ liệu ứng dụng: `~/.bookmind/` (cấu hình được qua `BOOKMIND_HOME`).

```
~/.bookmind/
├── bookmind.db          SQLite: metadata, text, chunk, chat, job, config
├── secrets.key          khóa mã hoá API key (quyền 600)
├── library/             file sách (content-addressed)
│   └── ab/cd/abcd….pdf
├── .tmp/                file đang import, dọn khi khởi động
└── logs/
```

Mọi sách nằm trong `library/` dưới dạng bản copy do app quản lý; file gốc của người dùng không bị đụng tới.

### Bảng chính

| Bảng | Nội dung |
|---|---|
| `book` | id, file_path, content_hash, format, status, processing_status, progress, ocr_language, failure_reason, metadata_json, added_at |
| `book_page` | id, book_id, page_no, text, source (`TEXT_LAYER`/`OCR`) |
| `book_chunk` | id, book_id, ord, text, chapter, page_from, page_to, content_hash |
| `chunk_embedding` | chunk_id, vector (sqlite-vec virtual table), dim |
| `collection`, `collection_book` | collections |
| `conversation`, `message`, `message_citation` | chat |
| `ai_provider_config` | base_url, model_chat, model_embedding, encrypted_api_key, is_active |
| `processing_job` | id, book_id, type, status, attempts, last_error, updated_at |

- **Backup/restore (FR-NFR-5):** copy `bookmind.db` + `secrets.key` + `library/`; nén cả `~/.bookmind`. Restore được sang máy khác vì path lưu tương đối.
- **Flyway** quản lý version schema.
- **Xoá sách (FR-1.6):** cascade xoá page, chunk, embedding, job liên quan, gỡ collection member; xoá file copy trong `library/`; **không** xoá file gốc của người dùng.
- **Verify định kỳ:** đối chiếu `content_hash` của file trong `library/` với DB (chạy tay/lúc khởi động) để phát hiện file bị hỏng hoặc bị sửa từ ngoài.

---

## 11. API surface (tóm tắt)

| Method | Path | Mô tả |
|---|---|---|
| `GET` | `/api/books` | list + filter/sort/paging |
| `POST` | `/api/books` | thêm sách từ `{ "path": "/abs/path/book.pdf" }` (copy vào `library/`) |
| `GET` | `/api/books/{id}` | chi tiết + processing status |
| `PATCH` | `/api/books/{id}` | sửa metadata |
| `DELETE` | `/api/books/{id}` | xoá khỏi thư viện (xoá file copy trong `library/`, không xoá file gốc) |
| `GET` | `/api/books/{id}/events` | SSE tiến độ xử lý |
| `GET` | `/api/books/{id}/text` | đọc text theo trang/chương |
| `PUT` | `/api/books/{id}/text` | sửa text OCR → re-index |
| `POST` | `/api/books/{id}/reprocess` | chạy lại OCR/extract |
| `GET/POST/PATCH/DELETE` | `/api/collections/**` | collections |
| `GET` | `/api/search?q=&scope=` | semantic search |
| `GET/POST/PATCH/DELETE` | `/api/conversations/**` | chat history |
| `POST` | `/api/conversations/{id}/messages` | gửi tin nhắn (SSE stream) |
| `GET/PUT` | `/api/config/ai` | đọc/cập nhật provider config |
| `POST` | `/api/config/ai/test` | test connection |
| `GET` | `/api/health` | Actuator health |

- Lỗi thống nhất: `{ "code": "...", "message": "...", "details": {...} }`, HTTP status đúng nghĩa.
- Tất cả endpoint bind localhost; CORS chỉ cho Vite dev origin.

---

## 12. Frontend architecture

```
src/
├── app/            App shell, router, providers
├── pages/          LibraryPage, SearchPage, ChatPage, SettingsPage, BookDetailPage
├── features/
│   ├── library/    api.ts, hooks, components
│   ├── processing/ progress polling / SSE
│   ├── search/     query, scope, result list
│   ├── chat/       conversation list, message stream, citation rendering
│   └── settings/   AI config form, test-connection
├── shared/
│   ├── api/        fetch client, error mapping, SSE client
│   ├── ui/         shadcn components
│   ├── i18n/       vi.json, en.json
│   └── types/      DTO types (sinh từ OpenAPI hoặc viết tay)
└── main.tsx
```

- **Server state:** TanStack Query cho list/detail; invalidate theo mutation.
- **Streaming chat:** `EventSource`/`fetch` stream đọc SSE, append token vào message đang hiển thị.
- **Tiến độ xử lý:** ưu tiên SSE `book/{id}/events`; fallback polling 2s.
- **Types:** sinh từ OpenAPI (`/v3/api-docs`) để tránh lệch DTO.
- **i18n:** mọi string qua `t()`; mặc định `vi`.

---

## 13. Bảo mật (local-only)

| Hạng mục | Biện pháp |
|---|---|
| Bind | `server.address=127.0.0.1` |
| CORS | Chỉ cho `http://localhost:5173` (dev) và same-origin (prod) |
| File path | `PathValidator`: absolute, exists, regular file, extension whitelist. File nguồn chỉ đọc, copy vào `library/` |
| Library root | Mọi path resolve bên trong `library/`; từ chối path thoát root (`..`, symlink) |
| Path traversal | Không dùng path để tạo tên file; tên file trong `library/` do hash sinh ra; không expose arbitrary read endpoint |
| API key | AES-GCM, key file quyền `600`; không log; mask trong mọi response |
| Upload size | Giới hạn kích thước file được đọc (`max-book-size`) |
| Logs | Không log nội dung sách/API key; chỉ log id và trạng thái |

Lưu ý: vì backend đọc file tại đường dẫn người dùng nhập, endpoint `POST /api/books` chỉ được gọi từ UI local; không có endpoint đọc nội dung theo path tuỳ ý (chỉ đọc path đã lưu trong `book`).

---

## 14. Cấu hình ứng dụng

Tệp `application.yml` + `~/.bookmind/config.json` (ghi đè). Các nhóm chính:

```yaml
server:
  address: 127.0.0.1
  port: 8080
bookmind:
  home: ${BOOKMIND_HOME:~/.bookmind}
  library:
    dir: ${BOOKMIND_HOME}/library
    verify-hash-on-startup: false
    max-book-size: 200MB
    allowed-formats: [pdf, epub]
  processing:
    parallel-jobs: 1
    max-attempts: 3
    chunk-size: 800
    chunk-overlap: 120
  ocr:
    default-language: vie
    min-text-density: 30
    engine: paddleocr
    sidecar-command: python3
    sidecar-script: scripts/paddle_ocr.py
    model-dir: ${BOOKMIND_HOME}/models/paddleocr
    device: cpu            # cpu | gpu
    timeout-seconds: 120
  search:
    top-k: 10
    min-score: 0.25
  chat:
    max-context-tokens: 6000
    history-window: 10
```

---

## 15. Các quyết định kiến trúc (ADR tóm tắt)

| # | Quyết định | Lý do | Đánh đổi |
|---|---|---|---|
| ADR-1 | SQLite thay vì PostgreSQL | Single user, zero setup, backup = copy file | Không multi-writer, phải giới hạn job song song |
| ADR-2 | Vector store nhúng (sqlite-vec) | Không cần chạy service riêng | Ít filter nâng cao hơn Qdrant; đặt sau interface để thay |
| ADR-3 | Persistent job queue trong DB | Đơn tiến trình, không Redis | Throughput thấp — đủ cho xử lý sách |
| ADR-4 | Luôn copy file vào `library/` khi import | Backup/restore thành thật (NFR-5); path ổn định, không vỡ khi file gốc bị move; xoá an toàn vì file thuộc app; dedupe bằng SHA-256; một nguồn sự thật duy nhất | Nhân đôi dung lượng (cần ở mức 20–100GB cho 1.000 sách); import chậm → phải là job nền |
| ADR-5 | SSE thay vì WebSocket | Streaming một chiều, đơn giản hơn | Không dùng cho tương tác hai chiều |
| ADR-6 | OpenAI-compatible client chung | Một adapter cho nhiều provider | Provider khác chuẩn cần adapter riêng |
| ADR-7 | Frontend build nhúng vào Spring Boot | Phân phối 1 jar, không cần Node khi chạy | Dev/build tách hai phase |
| ADR-8 | PaddleOCR thay Tesseract | Chính xác hơn rõ rệt (nhất là `vie`), xử lý layout/ảnh nghiêng tốt hơn | Không có binding Java → cần Python sidecar + đóng gói runtime |
| ADR-9 | Không dùng symlink cho file sách | Symlink vẫn trỏ absolute path nên không chống được move; cần quyền admin/Developer Mode trên Windows; không hỗ trợ trên FAT/exFAT và nhiều NAS; backup `rsync -L` sẽ copy toàn bộ sách; rủi ro xoá nhầm file gốc nếu follow link; tạo hai nguồn sự thật | Không có "cửa sổ" duyệt sách bằng Files/Finder trong thư mục app — dùng UI của app thay thế |
| ADR-10 | Không dùng port in (`*UseCase` interface) | `presentation → application` đã là detail → policy; không có nhiều implementation, không tách module, không cần chặn bề mặt API | Controller phụ thuộc class cụ thể; muốn thay/thêm decorator phải sửa chỗ inject |

---

## 16. Kiểm thử

| Tầng | Chiến lược | Công cụ |
|---|---|---|
| Domain | Unit thuần, không Spring | JUnit 5 + AssertJ |
| Application | Unit với port mock | Mockito |
| Infrastructure | Integration với SQLite file tạm, fake AI server | Spring Boot Test, WireMock |
| Presentation | `@WebMvcTest` cho controller + contract DTO | MockMvc |
| E2E | Luồng thêm sách → search → chat với AI stub | Playwright |
| Frontend | Component/hook test, MSW giả API | Vitest, Testing Library, MSW |

Tiêu chí quan trọng: pipeline có thể chạy lại sau restart; xoá sách không đụng file gốc; API key không xuất hiện trong log/response.

---

## 17. Cấu trúc repository

```
BookMind/
├── backend/         Gradle project (Spring Boot)
│   ├── src/main/java/com/bookmind/{domain,application,infrastructure,presentation}
│   ├── src/main/resources/db/migration
│   └── src/test/java/...
├── frontend/        Vite + React + TS
├── docs/            requirements.md, architecture.md, implementation-plan.md
└── README.md
```

Giai đoạn đóng gói: build frontend → copy `dist/*` vào `backend/src/main/resources/static/`.
