package com.bookmind.presentation.error;

import com.bookmind.domain.shared.DomainException;
import com.bookmind.domain.shared.ErrorCode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * {@code GlobalExceptionHandler} — Bộ xử lý lỗi toàn cục cho REST API.
 * <p>
 * Lớp này bắt tất cả exception phát sinh từ controller và chuyển đổi thành
 * phản hồi HTTP thống nhất theo định dạng JSON:
 * <pre>{@code
 * {
 *   "code": "BOOK_NOT_FOUND",
 *   "message": "Sách không tồn tại",
 *   "details": { "id": "book-123" }
 * }
 * }</pre>
 * </p>
 * <p>
 * <b>Cơ chế hoạt động:</b>
 * <ol>
 *   <li>{@code DomainException} → map sang HTTP status tương ứng qua {@link #statusFor(ErrorCode)}</li>
 *   <li>{@code MethodArgumentNotValidException} (validation fail) → 400 Bad Request</li>
 *   <li>Mọi exception khác → 500 Internal Server Error, log chi tiết</li>
 * </ol>
 * </p>
 * <p>
 * <b>Vì sao dùng {@code @RestControllerAdvice}?</b>
 * <br>
 * Đây là Spring annotation cho phép định nghĩa các handler exception áp dụng cho
 * TẤT CẢ controller trong package. Thay vì bắt lỗi ở từng controller riêng lẻ,
 * ta gom về một nơi duy nhất → code sạch, dễ bảo trì, consistent response format.
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/** Logger dùng để ghi lại các exception không mong muốn (500 errors). */
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/**
	 * Xử lý {@link DomainException} — lỗi nghiệp vụ do domain layer ném ra.
	 * <p>
	 * Phương thức này:
	 * <ol>
	 *   <li>Tạo {@code ErrorResponse} chứa mã lỗi, thông điệp và chi tiết đính kèm</li>
	 *   <li>Map {@link ErrorCode} sang HTTP status code tương ứng qua {@link #statusFor(ErrorCode)}</li>
	 *   <li>Trả về {@code ResponseEntity} với status và body đã được định dạng</li>
	 * </ol>
	 * </p>
	 * <p>
	 * <b>Ví dụ:</b> {@code DUPLICATE_BOOK} → 409 Conflict, {@code BOOK_NOT_FOUND} → 404 Not Found
	 * </p>
	 *
	 * @param exception lỗi nghiệp vụ được ném từ domain layer
	 * @return ResponseEntity chứa ErrorResponse và HTTP status tương ứng
	 */
	@ExceptionHandler(DomainException.class)
	ResponseEntity<ErrorResponse> handleDomainException(DomainException exception) {
		// Xây dựng response body với code, message và details từ exception
		ErrorResponse body = new ErrorResponse(
				exception.getCode().name(),       // Mã lỗi enum → string (ví dụ: "DUPLICATE_BOOK")
				exception.getMessage(),           // Thông điệp mô tả lỗi
				exception.getDetails());          // Chi tiết phụ trợ (có thể rỗng)
		// Map ErrorCode sang HTTP status và trả về response
		return ResponseEntity.status(statusFor(exception.getCode())).body(body);
	}

	/**
	 * Xử lý {@link MethodArgumentNotValidException} — lỗi khi dữ liệu đầu vào
	 * không vượt qua validation (JSR-380 / Bean Validation).
	 * <p>
	 * Phát sinh khi:
	 * <ul>
	 *   <li>Request body không đúng schema JSON</li>
	 *   <li>Field không đạt constraint (@NotBlank, @Size, @Pattern...)</li>
	 * </ul>
	 * </p>
	 * <p>
	 * Phương thức này gom tất cả lỗi field thành một map (field → message)
	 * và trả về 400 Bad Request với chi tiết từng trường bị sai.
	 * </p>
	 * <p>
	 * <b>Ví dụ response:</b>
	 * <pre>{@code
	 * {
	 *   "code": "VALIDATION_FAILED",
	 *   "message": "Dữ liệu đầu vào không hợp lệ",
	 *   "details": { "title": "phải không được để trống", "isbn": "dài tối đa 13 ký tự" }
	 * }
	 * }</pre>
	 * </p>
	 *
	 * @param exception lỗi validation do Spring ném ra khi parse request body
	 * @return ResponseEntity 400 Bad Request với chi tiết lỗi từng field
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
		// Dùng LinkedHashMap để giữ thứ tự field error theo thứ xuất hiện trong request
		Map<String, Object> fields = new LinkedHashMap<>();
		for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
			// key: tên field bị lỗi, value: thông điệp validation mặc định
			fields.put(fieldError.getField(), fieldError.getDefaultMessage());
		}
		ErrorResponse body = new ErrorResponse(
				ErrorCode.VALIDATION_FAILED.name(),  // Mã lỗi cố định cho validation
				"Dữ liệu đầu vào không hợp lệ",      // Thông điệp chung
				fields);                              // Chi tiết từng field sai
		return ResponseEntity.badRequest().body(body);
	}

	/**
	 * Xử lý các exception không mong muốn — fallback cuối cùng.
	 * <p>
	 * Bắt mọi {@link Exception} còn lại chưa được xử lý bởi các handler trên.
	 * Đây là "safety net" để đảm bảo không có request nào trả về 500 trống rỗng.
	 * </p>
	 * <p>
	 * <b>Hành vi:</b>
	 * <ul>
	 *   <li>Ghi log chi tiết exception (stack trace) để debug</li>
	 *   <li>Trả về 500 Internal Server Error với thông điệp chung "Đã xảy ra lỗi không mong muốn"</li>
	 *   <li><b>Không expose chi tiết lỗi</b> cho client — bảo vệ thông tin nhạy cảm khỏi leak</li>
	 * </ul>
	 * </p>
	 *
	 * @param exception exception chưa được xử lý bởi bất kỳ handler nào khác
	 * @return ResponseEntity 500 Internal Server Error với ErrorResponse chung
	 */
	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
		// Log chi tiết stack trace để developer có thể debug
		log.error("Unhandled exception", exception);
		// Trả về lỗi chung — không expose thông tin nhạy cảm cho client
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR.name(), "Đã xảy ra lỗi không mong muốn"));
	}

	/**
	 * Map {@link ErrorCode} enum sang HTTP {@link HttpStatus}.
	 * <p>
	 * Đây là trung tâm định nghĩa mapping giữa mã lỗi nghiệp vụ và HTTP status code.
	 * Thay vì硬编码 trong từng handler, ta gom về một nơi duy nhất → dễ bảo trì,
	 * không bị trùng lặp logic.
	 * </p>
	 * <table>
	 *   <tr><th>ErrorCode</th><th>HTTP Status</th><th>Mô tả</th></tr>
	 *   <tr><td>INVALID_PATH, VALIDATION_FAILED</td><td>400</td><td>Dữ liệu hoặc đường dẫn sai</td></tr>
	 *   <tr><td>FILE_NOT_FOUND, BOOK_NOT_FOUND</td><td>404</td><td>Resource không tồn tại</td></tr>
	 *   <tr><td>DUPLICATE_BOOK, AI_NOT_CONFIGURED, EMBEDDING_DIM_MISMATCH</td><td>409</td><td>Xung đột nghiệp vụ</td></tr>
	 *   <tr><td>FILE_TOO_LARGE</td><td>413</td><td>Dung lượng file vượt quá giới hạn</td></tr>
	 *   <tr><td>UNSUPPORTED_FORMAT</td><td>415</td><td>Định dạng file không hỗ trợ</td></tr>
	 *   <tr><td>OCR_UNAVAILABLE</td><td>503</td><td>Dịch vụ OCR tạm thời không khả dụng</td></tr>
	 *   <tr><td>INTERNAL_ERROR</td><td>500</td><td>Lỗi server không dự đoán được</td></tr>
	 * </table>
	 *
	 * @param code mã lỗi nghiệp vụ cần map sang HTTP status
	 * @return HTTP status code tương ứng
	 */
	private static HttpStatus statusFor(ErrorCode code) {
		return switch (code) {
			case INVALID_PATH, VALIDATION_FAILED -> HttpStatus.BAD_REQUEST;
			case FILE_NOT_FOUND, BOOK_NOT_FOUND -> HttpStatus.NOT_FOUND;
			case DUPLICATE_BOOK, AI_NOT_CONFIGURED, EMBEDDING_DIM_MISMATCH -> HttpStatus.CONFLICT;
			case FILE_TOO_LARGE -> HttpStatus.CONTENT_TOO_LARGE;
			case UNSUPPORTED_FORMAT -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
			case OCR_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
			case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
		};
	}
}
