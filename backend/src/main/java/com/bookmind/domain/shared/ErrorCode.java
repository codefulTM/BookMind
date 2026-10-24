package com.bookmind.domain.shared;

/**
 * Mã lỗi nghiệp vụ. Không gắn HTTP status ở đây để domain không phụ thuộc framework.
 * Việc map sang HTTP status do lớp presentation đảm nhận.
 */
public enum ErrorCode {
	INVALID_PATH,
	UNSUPPORTED_FORMAT,
	FILE_TOO_LARGE,
	FILE_NOT_FOUND,
	DUPLICATE_BOOK,
	BOOK_NOT_FOUND,
	AI_NOT_CONFIGURED,
	OCR_UNAVAILABLE,
	EMBEDDING_DIM_MISMATCH,
	VALIDATION_FAILED,
	INTERNAL_ERROR
}
