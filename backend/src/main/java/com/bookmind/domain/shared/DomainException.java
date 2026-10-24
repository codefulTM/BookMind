package com.bookmind.domain.shared;

import java.util.Map;
import java.util.Objects;

/**
 * {@code DomainException} — Lỗi nghiệp vụ (business domain exception).
 * <p>
 * Đây là checked-runtime exception dùng để báo hiệu các lỗi phát sinh từ lớp
 * <b>domain</b>, ví dụ: sách trùng tên, định dạng file không hỗ trợ, đường dẫn
 * file không hợp lệ...
 * </p>
 * <h3>Đặc điểm thiết kế</h3>
 * <ul>
 *   <li>Mỗi lỗi mang theo một {@link ErrorCode} — mã lỗi enum, giúp presentation
 *       layer (REST controller) map chính xác sang HTTP status code và JSON body.</li>
 *   <li>Cho phép đính kèm {@code details} — bản Map các trường phụ trợ (ví dụ:
 *       tên file sai, dung lượng vượt quá...) để frontend hiển thị thông báo chi tiết.</li>
 *   <li>Kế thừa {@link RuntimeException} nên không bắt buộc {@code throws} ở
 *       signature method, giữ code sạch sẽ. Nhưng vẫn có {@link Throwable getCause()}
 *       để wrap exception gốc từ infrastructure layer (file system, database...). </li>
 * </ul>
 * <h3>Ví dụ sử dụng</h3>
 * <pre>{@code
 * throw new DomainException(BOOK_NOT_FOUND, "Sách không tồn tại", Map.of("id", bookId));
 * }</pre>
 * <p>
 * <b>Lưu ý:</b> Không ném exception này ở lớp infrastructure (repository, file IO).
 * Lớp đó chỉ ném {@link RuntimeException} gốc, domain layer mới transform sang
 * {@code DomainException}.
 * </p>
 */
public class DomainException extends RuntimeException {

	/**
	 * Mã lỗi nghiệp vụ.
	 * <p>
	 * Đây là enum xác định loại lỗi (ví dụ: {@code BOOK_NOT_FOUND},
	 * {@code DUPLICATE_BOOK}...). Mỗi mã tương ứng một HTTP status code
	 * do presentation layer map sang.
	 * </p>
	 */
	private final ErrorCode code;

	/**
	 * Các trường phụ trợ đính kèm lỗi.
	 * <p>
	 * Dùng để truyền thêm thông tin chi tiết cho client, ví dụ:
	 * <ul>
	 *   <li>{@code "fileName"} — tên file gây lỗi</li>
	 *   <li>{@code "fileSize"} — dung lượng file vượt quá giới hạn</li>
	 *   <li>{@code "bookId"} — ID sách không tìm thấy</li>
	 * </ul>
	 * Bản map được copy bằng {@link Map#copyOf(Map)} để bất biến (immutable)
	 * sau khi khởi tạo.
	 * </p>
	 */
	private final Map<String, Object> details;

	/**
	 * Constructor ngắn gọn — chỉ cần mã lỗi và thông điệp.
	 *
	 * @param code    mã lỗi từ {@link ErrorCode} enum
	 * @param message thông điệp mô tả lỗi (dùng cho log hoặc response body)
	 */
	public DomainException(ErrorCode code, String message) {
		this(code, message, Map.of(), null);
	}

	/**
	 * Constructor đầy đủ — có thêm chi tiết đính kèm.
	 *
	 * @param code    mã lỗi từ {@link ErrorCode} enum
	 * @param message thông điệp mô tả lỗi
	 * @param details bản map các trường phụ trợ (có thể null → tự chuyển thành {@code Map.of()})
	 */
	public DomainException(ErrorCode code, String message, Map<String, Object> details) {
		this(code, message, details, null);
	}

	/**
	 * Constructor — có exception gốc để wrap.
	 * <p>
	 * Dùng khi domain layer bắt exception từ infrastructure (ví dụ: {@code IOException})
	 * và transform thành {@code DomainException}, giữ lại stack trace gốc qua {@code cause}.
	 * </p>
	 *
	 * @param code    mã lỗi từ {@link ErrorCode} enum
	 * @param message thông điệp mô tả lỗi
	 * @param cause   exception gốc bị ném ra ở lớp thấp hơn (file system, database...)
	 */
	public DomainException(ErrorCode code, String message, Throwable cause) {
		this(code, message, Map.of(), cause);
	}

	/**
	 * Constructor đầy đủ nhất — có cả chi tiết và exception gốc.
	 * <p>
	 * Gọi lên {@code super(message, cause)} để truyền thông điệp và stack trace
	 * cho {@link RuntimeException}. Mã lỗi được kiểm tra null bằng
	 * {@link Objects#requireNonNull(Object, String)}.
	 * </p>
	 *
	 * @param code    mã lỗi từ {@link ErrorCode} enum (bắt buộc khác null)
	 * @param message thông điệp mô tả lỗi
	 * @param details bản map chi tiết phụ trợ (null → {@code Map.of()})
	 * @param cause   exception gốc (có thể null)
	 */
	public DomainException(ErrorCode code, String message, Map<String, Object> details, Throwable cause) {
		super(message, cause);
		// Đảm bảo code luôn khác null, ném NullPointerException nếu người dùng vô tình truyền null
		this.code = Objects.requireNonNull(code, "code");
		// Copy bất biến: tránh caller sửa Map sau khi truyền vào
		this.details = details == null ? Map.of() : Map.copyOf(details);
	}

	/**
	 * Lấy mã lỗi nghiệp vụ.
	 *
	 * @return {@link ErrorCode} tương ứng với lỗi này
	 */
	public ErrorCode getCode() {
		return code;
	}

	/**
	 * Lấy bản map chi tiết phụ trợ.
	 * <p>
	 * Trả về bản copy bất biến, caller không thể sửa đổi nội dung bên trong.
	 * </p>
	 *
	 * @return {@code Map<String, Object>} chứa các trường phụ trợ
	 */
	public Map<String, Object> getDetails() {
		return details;
	}
}
