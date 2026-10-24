package com.bookmind.presentation.error;

import java.util.Map;

/**
 * {@code ErrorResponse} — Định dạng response thống nhất cho tất cả lỗi API.
 * <p>
 * Đây là một <b>record</b> (Java 17+ feature) thay vì class truyền thống, giúp:
 * <ul>
 *   <li>Tự động sinh constructor, getter, {@code equals()}, {@code hashCode()}</li>
 *   <li>Không thể sửa đổi state sau khi tạo (immutable)</li>
 *   <li>Code ngắn gọn, dễ đọc</li>
 * </ul>
 * </p>
 * <p>
 * <b>Cấu trúc JSON trả về client:</b>
 * <pre>{@code
 * {
 *   "code": "DUPLICATE_BOOK",
 *   "message": "Sách đã tồn tại trong thư viện",
 *   "details": { "existingBookId": "book-123" }
 * }
 * }</pre>
 * </p>
 * <p>
 * <b>Trường chi tiết ({@code details}):</b>
 * <ul>
 *   <li>Nếu null → tự động chuyển thành {@code Map.of()} (empty map bất biến)</li>
 *   <li>Dùng {@link Map#copyOf(Map)} để đảm bảo immutable — caller không thể sửa sau khi truyền vào</li>
 * </ul>
 * </p>
 */
public record ErrorResponse(String code, String message, Map<String, Object> details) {

	/**
	 * Compact constructor của record — thực thi logic sau khi field được gán.
	 * <p>
	 * Đảm bảo {@code details} luôn là bản map bất biến (immutable), ngăn ngừa
	 * caller vô tình sửa đổi nội dung Map sau khi truyền vào ErrorResponse.
	 * </p>
	 */
	public ErrorResponse {
		details = details == null ? Map.of() : Map.copyOf(details);
	}

	/**
	 * Factory method ngắn gọn để tạo ErrorResponse chỉ với code và message (không có details).
	 * <p>
	 * Dùng khi lỗi không cần chi tiết phụ trợ, ví dụ: lỗi server chung (500),
	 * hoặc lỗi đơn giản không cần truyền thêm thông tin.
	 * </p>
	 *
	 * @param code mã lỗi string (ví dụ: "INTERNAL_ERROR")
	 * @param message thông điệp mô tả lỗi
	 * @return ErrorResponse với details rỗng ({@code Map.of()})
	 */
	public static ErrorResponse of(String code, String message) {
		return new ErrorResponse(code, message, Map.of());
	}
}
