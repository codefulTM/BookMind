/**
 * {@code setup.ts} — Cấu hình môi trường test cho frontend.
 * <p>
 * File này được import tự động trước mỗi test case, thiết lập:
 * <ul>
 *   <li>{@code @testing-library/jest-dom} — mở rộng Jest assertions (ví dụ: {@code toBeInTheDocument()})</li>
 *   <li>('@/shared/i18n') — khởi tạo i18n để component có thể dùng {@code useTranslation()} trong test</li>
 * </ul>
 * </p>
 */
import '@testing-library/jest-dom/vitest'

// Khởi tạo i18n đồng bộ trước khi render bất kỳ component nào
import '@/shared/i18n'
