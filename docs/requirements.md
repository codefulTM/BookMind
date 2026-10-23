# BookMind — Bản yêu cầu người dùng (User Requirements)

Tài liệu này mô tả **yêu cầu người dùng** cho BookMind — một thư viện sách cá nhân hỗ trợ PDF/EPUB, OCR, tìm kiếm ngữ nghĩa bằng AI (semantic search) và trò chuyện thông thường với AI về sách.

Mục tiêu: một người dùng cá nhân quản lý thư viện sách của mình, đọc, tra cứu thông minh và trò chuyện với nội dung sách.

---

## 1. Phạm vi (Scope)

**Trong phạm vi**
- Quản lý thư viện sách cá nhân (PDF, EPUB).
- Trích xuất văn bản, bao gồm OCR cho sách scan/ảnh.
- Semantic search trên toàn thư viện.
- Chat với AI, có thể dựa trên ngữ cảnh sách (RAG) hoặc trò chuyện chung.
- Cấu hình nhà cung cấp AI (API key, model, endpoint).

BookMind chạy hoàn toàn cục bộ trên máy người dùng (single user, dữ liệu local), nên các tính năng sau sẽ **không làm** ở bất kỳ giai đoạn nào:
- Tài khoản, đăng nhập, chia sẻ thư viện giữa nhiều người dùng, phân quyền.
- Đồng bộ đa thiết bị, backend/cloud lưu trữ.
- Ứng dụng mobile native.
- Mua/bán sách, đọc sách DRM.

---

## 2. Đối tượng người dùng

| Đối tượng | Mô tả | Nhu cầu chính |
|---|---|---|
| Chủ thư viện (single user) | Người dùng cá nhân chạy BookMind trên máy của mình | Thêm sách từ file local, tìm kiếm, chat, quản lý AI config |

---

## 3. Yêu cầu chức năng

### FR-1. Thư viện sách

- **FR-1.1** Thêm sách bằng cách chọn file PDF hoặc EPUB có sẵn trên máy. Hệ thống copy file vào thư mục thư viện của app, kiểm tra trùng bằng nội dung (hash); bản gốc không bị thay đổi.
- **FR-1.2** Hiển thị danh sách sách với metadata: tiêu đề, tác giả, bìa, định dạng, số trang, kích thước, ngày thêm, trạng thái xử lý.
- **FR-1.3** Cho phép chỉnh sửa metadata: tiêu đề, tác giả, tags, mô tả.
- **FR-1.4** Sắp xếp và lọc theo tiêu đề, tác giả, tag, định dạng, ngày thêm.
- **FR-1.5** Tìm kiếm theo metadata (tiêu đề/tác giả) — khác với semantic search.
- **FR-1.6** Xoá sách khỏi thư viện (kèm xoá dữ liệu đã xử lý và index liên quan, không xoá file gốc).
- **FR-1.7** Hỗ trợ collections/shelves (nhóm sách do người dùng tạo).

### FR-2. Trích xuất văn bản và OCR

- **FR-2.1** Trích xuất văn bản tự động khi thêm sách, chạy nền (background job).
- **FR-2.2** Với PDF có text layer: trích xuất trực tiếp.
- **FR-2.3** Với PDF scan/ảnh và EPUB dạng ảnh: chạy OCR để lấy chữ.
- **FR-2.4** Hiển thị trạng thái xử lý: queued → đang trích xuất/OCR → hoàn tất / lỗi.
- **FR-2.5** Cho phép xem và sửa text đã trích xuất/OCR (sửa lỗi OCR).
- **FR-2.6** Chọn ngôn ngữ OCR (ít nhất: tiếng Việt có dấu, tiếng Anh).
- **FR-2.7** Hiển thị tiến độ xử lý cho sách.
- **FR-2.8** Cho phép chạy lại OCR cho một sách cụ thể.

### FR-3. Chunking và index

- **FR-3.1** Chia văn bản thành các chunk có ý nghĩa, giữ liên kết về sách/chương/trang.
- **FR-3.2** Tạo embedding cho từng chunk và lưu vào vector store.
- **FR-3.3** Re-index khi nội dung sách thay đổi (sửa OCR, sửa metadata ảnh hưởng chunk).
- **FR-3.4** Hiển thị trạng thái sẵn sàng cho search của từng sách.

### FR-4. Semantic search

- **FR-4.1** Tìm kiếm theo ý nghĩa (ngôn ngữ tự nhiên), không chỉ từ khoá.
- **FR-4.2** Trả về danh sách kết quả xếp hạng theo độ liên quan, có trích đoạn, tên sách, số trang/chương.
- **FR-4.3** Cho phép click vào kết quả để mở đúng vị trí trong sách.
- **FR-4.4** Phạm vi tìm kiếm: toàn thư viện hoặc giới hạn theo sách/collection/tag.
- **FR-4.5** Hiển thị kèm "điểm liên quan" để người dùng hiểu độ tin cậy.

### FR-5. Chat với AI

- **FR-5.1** Chat trong ngữ cảnh sách: hỏi đáp dựa trên nội dung sách (RAG), trả lời kèm trích dẫn nguồn (sách + trang).
- **FR-5.2** Chat tổng quát: trò chuyện tự do, không ràng buộc tài liệu.
- **FR-5.3** Người dùng có thể chuyển chế độ trong cùng cuộc trò chuyện hoặc tạo cuộc trò chuyện theo chế độ.
- **FR-5.4** Lưu lịch sử các cuộc trò chuyện, cho phép đổi tên, xoá, tiếp tục.
- **FR-5.5** Giới hạn phạm vi ngữ cảnh theo sách hoặc collection.
- **FR-5.6** Khi không đủ thông tin trong sách, AI phải nói rõ là không tìm thấy thay vì bịa.
- **FR-5.7** Hỗ trợ streaming câu trả lời.
- **FR-5.8** Từ câu trả lời có thể điều hướng tới đoạn trích nguồn.
- **FR-5.9** (Tùy chọn) Tóm tắt sách/chương bằng AI.

### FR-6. Cấu hình AI

- **FR-6.1** Cấu hình nhà cung cấp AI: API base URL, API key, tên model.
- **FR-6.2** Hỗ trợ ít nhất một provider tương thích OpenAI API; thiết kế mở để thêm provider khác.
- **FR-6.3** Cấu hình riêng model cho chat và model cho embedding.
- **FR-6.4** Kiểm tra kết nối (test connection) trước khi lưu.
- **FR-6.5** Không bao giờ hiển thị lại API key đầy đủ trong UI (mask).
- **FR-6.6** Có tùy chọn chạy AI cục bộ (local model) nếu người dùng cấu hình.

---

## 4. Yêu cầu phi chức năng

### NFR-1. Hiệu năng
- Semantic search trên thư viện ~1.000 sách trả kết quả trong vài giây.
- Thêm sách và bắt đầu xử lý nền không chặn UI.
- Chat bắt đầu phản hồi (first token) nhanh; streaming để giảm cảm giác chờ.

### NFR-2. Độ tin cậy
- Job xử lý (extract/OCR/chunk/embed) có thể retry và không mất trạng thái khi restart.
- Sách lỗi xử lý được đánh dấu rõ và có thể chạy lại thủ công.

### NFR-3. Bảo mật
- API key được mã hoá khi lưu trữ.
- Sách được copy vào thư mục thư viện riêng của app để thư viện tự chứa và backup được; file nguồn của người dùng chỉ đọc, không bị sửa/xoá.
- Không thao tác ghi/xoá lên bất kỳ file gốc nào của người dùng; kiểm tra tồn tại và định dạng trước khi xử lý.
- Chỉ nhận đường dẫn tới file PDF/EPUB hợp lệ, giới hạn kích thước cấu hình được.
- Chỉ chạy cục bộ (bind localhost); không mở ra mạng ngoài nên không cần đăng nhập.

### NFR-4. Khả dụng
- UI tiếng Việt và tiếng Anh (ít nhất tiếng Việt).
- Hoạt động tốt trên desktop; responsive cơ bản cho tablet.
- Thông báo lỗi rõ ràng, hướng dẫn khắc phục.

### NFR-5. Khả năng mở rộng và bảo trì
- Kiến trúc tách biệt rõ: domain / application / infrastructure / presentation.
- Vector store và AI provider thay thế được qua interface.
- Dữ liệu (metadata, text, embedding) lưu bền, có thể backup/restore.

### NFR-6. Hiệu quả chi phí
- Người dùng kiểm soát chi phí AI qua cấu hình provider/model.
- Tránh gọi AI lặp lại không cần thiết (cache embedding, tránh re-index vô ích).

---

## 5. Luồng người dùng chính

### 5.1 Thêm và xử lý sách
1. Người dùng chọn file PDF/EPUB trên máy.
2. Hệ thống kiểm tra file, kiểm tra trùng theo hash và copy vào thư viện của app. Đọc metadata, xếp hàng xử lý.
3. Hệ thống trích xuất text (và OCR nếu cần), chia chunk, tạo embedding.
4. Sách chuyển trạng thái "sẵn sàng", có thể tìm kiếm và chat.

### 5.2 Tìm kiếm ngữ nghĩa
1. Người dùng nhập câu hỏi/mô tả bằng ngôn ngữ tự nhiên.
2. Hệ thống embedding câu truy vấn, tìm chunk tương đồng.
3. Hiển thị kết quả xếp hạng kèm trích đoạn và nguồn.
4. Người dùng mở đúng vị trí trong sách.

### 5.3 Chat với sách
1. Người dùng chọn chế độ "chat với sách" và phạm vi (sách/collection).
2. Đặt câu hỏi.
3. Hệ thống truy hồi ngữ cảnh liên quan, gửi cho LLM.
4. AI trả lời kèm trích dẫn nguồn, stream về UI.

### 5.4 Chat tổng quát
1. Người dùng mở chat không gắn sách.
2. Trò chuyện tự do; có thể chuyển sang chế độ sách khi cần.