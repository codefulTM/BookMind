/**
 * {@code api} — HTTP client wrapper dùng `fetch` native cho frontend.
 * <p>
 * Module này cung cấp các phương thức CRUD ({@code get}, {@code post}, {@code patch}, {@code delete})
 * với:
 * <ul>
 *   <li>Tự động set header `Accept: application/json` và `Content-Type: application/json`</li>
 *   <li>Parse lỗi từ response body theo định dạng {@link ApiErrorBody}</li>
 *   <li>Ném {@link ApiError} nếu HTTP status không ok (≥ 400)</li>
 * </ul>
 * </p>
 * <p>
 * <b>Base URL:</b> Lấy từ biến môi trường `VITE_API_BASE_URL` (nếu có),
 * mặc định là chuỗi rỗng → dùng relative path so với trang hiện tại.
 * </p>
 */

/** Định dạng body lỗi trả về từ backend API. */
export interface ApiErrorBody {
  /** Mã lỗi nghiệp vụ, ví dụ: "BOOK_NOT_FOUND", "DUPLICATE_BOOK" */
  code: string
  /** Thông điệp mô tả lỗi (có thể bằng tiếng Việt) */
  message: string
  /** Chi tiết phụ trợ — map các trường mở rộng (có thể null) */
  details?: Record<string, unknown>
}

/**
 * {@code ApiError} — Lỗi HTTP do frontend bắt được từ API response.
 * <p>
 * Kế thừa `Error` để tích hợp với try-catch truyền thống của JavaScript.
 * Chứa thêm các trường `status`, `code`, `details` giúp caller xử lý lỗi chi tiết hơn.
 * </p>
 */
export class ApiError extends Error {
  /** HTTP status code (400, 404, 500...) */
  readonly status: number
  /** Mã lỗi nghiệp vụ từ backend (ví dụ: "BOOK_NOT_FOUND") */
  readonly code: string
  /** Chi tiết phụ trợ từ backend (có thể rỗng) */
  readonly details: Record<string, unknown>

  /**
   * Constructor — nhận HTTP status và body lỗi từ response.
   *
   * @param status HTTP status code trả về từ server
   * @param body đối tượng ApiErrorBody đã parse từ JSON response
   */
  constructor(status: number, body: ApiErrorBody) {
    super(body.message)           // Truyền message cho Error.prototype.name = "ApiError"
    this.name = 'ApiError'        // Đặt tên class để dễ nhận biết trong console.error()
    this.status = status          // Giữ lại HTTP status code
    this.code = body.code         // Giữ lại mã lỗi nghiệp vụ
    this.details = body.details ?? {} // Fallback empty object nếu details null
  }
}

/** Base URL cho tất cả API requests. Dùng env var hoặc fallback rỗng (relative path). */
const baseUrl = import.meta.env.VITE_API_BASE_URL ?? ''

/**
 * Parse lỗi từ response HTTP không ok.
 * <p>
 * Thử parse JSON body để lấy {@code code}, {@code message}, {@code details}.
 * Nếu parse thất bại (response không phải JSON), fallback dùng status text.
 * </p>
 *
 * @param response đối tượng Response từ `fetch()`
 * @returns ApiErrorBody đã parse hoặc giá trị fallback
 */
async function parseErrorBody(response: Response): Promise<ApiErrorBody> {
  try {
    // Thử parse JSON body — có thể throw nếu body không phải JSON hợp lệ
    const body = (await response.json()) as Partial<ApiErrorBody>
    return {
      code: body.code ?? `HTTP_${response.status}`,          // Fallback dùng HTTP status nếu không có code
      message: body.message ?? response.statusText,          // Fallback dùng statusText
      details: body.details,                                  // Giữ nguyên hoặc undefined
    }
  } catch {
    // Parse thất bại — trả về fallback an toàn
    return {
      code: `HTTP_${response.status}`,
      message: response.statusText || 'Request failed',
    }
  }
}

/**
 * Thực thi HTTP request chung cho tất cả methods (GET, POST, PATCH, DELETE).
 * <p>
 * Quy trình:
 * <ol>
 *   <li>Set header `Accept: application/json` và `Content-Type: application/json` (nếu có body)</li>
 *   <li>Gọi `fetch()` với base URL + path</li>
 *   <li>Nếu response không ok → ném {@link ApiError}</li>
 *   <li>Nếu status 204 (No Content) → trả về `undefined`</li>
 *   <li>Trường hợp khác → parse JSON và trả về</li>
 * </ol>
 * </p>
 *
 * @param path đường dẫn API (relative, ví dụ: "/api/books")
 * @param init tùy chọn RequestInit (headers, body, method override...)
 * @returns Promise<T> — dữ liệu trả về từ API đã parse JSON
 */
async function request<T>(path: string, init?: RequestInit): Promise<T> {
  // Khởi tạo headers mặc định
  const headers = new Headers(init?.headers)
  headers.set('Accept', 'application/json')                    // Yêu cầu server trả về JSON
  if (init?.body !== undefined && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')            // Tự động set nếu có body mà không có Content-Type
  }

  // Thực thi fetch với base URL + path
  const response = await fetch(`${baseUrl}${path}`, { ...init, headers })

  // Nếu response không ok (status ≥ 400) → ném ApiError với chi tiết từ body
  if (!response.ok) {
    throw new ApiError(response.status, await parseErrorBody(response))
  }
  // Status 204 No Content → trả về undefined (không có body)
  if (response.status === 204) {
    return undefined as T
  }
  // Parse JSON response và trả về dữ liệu
  return (await response.json()) as T
}

/**
 * Đối tượng API chứa các phương thức CRUD đã được bind method mặc định.
 * <ul>
 *   <li>{@code api.get<T>(path)} — GET request, parse response thành T</li>
 *   <li>{@code api.post<T>(path, body)} — POST request, stringify body thành JSON</li>
 *   <li>{@code api.patch<T>(path, body)} — PATCH request, tương tự POST</li>
 *   <li>{@code api.delete<T>(path)} — DELETE request, không có body</li>
 * </ul>
 * Tất cả đều hỗ trợ truyền thêm {@code RequestInit} để override method, headers...
 */
export const api = {
  /** GET request — trả về dữ liệu đã parse JSON */
  get: <T>(path: string, init?: RequestInit) => request<T>(path, { ...init, method: 'GET' }),
  /** POST request — tự động stringify body thành JSON */
  post: <T>(path: string, body?: unknown, init?: RequestInit) =>
    request<T>(path, {
      ...init,
      method: 'POST',
      body: body === undefined ? undefined : JSON.stringify(body),
    }),
  /** PATCH request — tương tự POST nhưng dùng method PATCH */
  patch: <T>(path: string, body?: unknown, init?: RequestInit) =>
    request<T>(path, {
      ...init,
      method: 'PATCH',
      body: body === undefined ? undefined : JSON.stringify(body),
    }),
  /** DELETE request — không có body */
  delete: <T>(path: string, init?: RequestInit) => request<T>(path, { ...init, method: 'DELETE' }),
}
