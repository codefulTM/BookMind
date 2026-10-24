/**
 * {@code i18n} — Cấu hình quốc tế hóa (i18n) cho ứng dụng BookMind.
 * <p>
 * Sử dụng thư viện `i18next` kết hợp với `react-i18next` để hỗ trợ đa ngôn ngữ.
 * Ngôn ngữ được lưu trữ trong `localStorage` và tự động khôi phục khi reload trang.
 * </p>
 * <p>
 * <b>Hỗ trợ ngôn ngữ:</b>
 * <ul>
 *   <li>{@code vi} — Tiếng Việt (mặc định)</li>
 *   <li>{@code en} — Tiếng Anh</li>
 * </ul>
 * </p>
 * <p>
 * <b>Quy trình khởi tạo:</b>
 * <ol>
 *   <li>Đọc ngôn ngữ đã lưu từ `localStorage` (key: {@code bookmind.language})</li>
 *   <li>Nếu không có hoặc giá trị không hợp lệ → fallback về {@code vi}</li>
 *   <li>Các resource dịch được bundle sẵn (không cần fetch từ server)</li>
 *   <li>Khởi tạo đồng bộ ({@code initAsync: false}) để đảm bảo test chạy đúng</li>
 * </ol>
 * </p>
 * <p>
 * <b>Sự kiện {@code languageChanged}:</b>
 * <br>
 * Mỗi khi người dùng đổi ngôn ngữ, giá trị mới được lưu vào `localStorage`
 * để giữ lại sau khi reload trang.
 * </p>
 */
import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'

// Import resource dịch cho từng ngôn ngữ
import en from './locales/en.json'
import vi from './locales/vi.json'

/** Key lưu trữ ngôn ngữ trong localStorage */
const LANGUAGE_STORAGE_KEY = 'bookmind.language'

/** Danh sách ngôn ngữ được hỗ trợ (chỉ vi và en) */
const supportedLanguages = ['vi', 'en'] as const

/** Type rút ra từ mảng supportedLanguages — chỉ cho phép 'vi' hoặc 'en' */
type SupportedLanguage = (typeof supportedLanguages)[number]

/** Ngôn ngữ mặc định khi chưa có lựa chọn hoặc giá trị lưu không hợp lệ */
const DEFAULT_LANGUAGE: SupportedLanguage = 'vi'

/**
 * Type guard kiểm tra một giá trị bất kỳ có nằm trong danh sách ngôn ngữ hỗ trợ.
 * <p>
 * TypeScript không thể thu hẹp kiểu từ {@code Array.includes} trên union type,
 * nên cần ép mảng về {@code readonly string[]} để so sánh chuỗi thô.
 * </p>
 *
 * @param value giá trị đọc từ localStorage (có thể null)
 * @returns {@code true} nếu value là ngôn ngữ được hỗ trợ
 */
function isSupportedLanguage(value: string | null): value is SupportedLanguage {
  return value !== null && (supportedLanguages as readonly string[]).includes(value)
}

/**
 * Xác định ngôn ngữ ban đầu để khởi tạo i18n.
 * <p>
 * Ưu tiên đọc từ localStorage. Nếu không có hoặc giá trị không hợp lệ,
 * fallback về {@link DEFAULT_LANGUAGE}.
 * </p>
 *
 * @returns ngôn ngữ được chọn ('vi' hoặc 'en')
 */
function getInitialLanguage(): SupportedLanguage {
  const stored = localStorage.getItem(LANGUAGE_STORAGE_KEY)
  return isSupportedLanguage(stored) ? stored : DEFAULT_LANGUAGE
}

/**
 * Khởi tạo i18next với react-i18next.
 * <p>
 * Cấu hình:
 * <ul>
 *   <li>Resources dịch cho vi và en đã bundle sẵn</li>
 *   <li>Ngôn ngữ ban đầu lấy từ {@link getInitialLanguage}</li>
 *   <li>Fallback về 'vi' nếu người dùng không hỗ trợ</li>
 *   <li>{@code escapeValue: false} — react-i18next tự xử lý escaping, i18next không cần escape nữa</li>
 *   <li>{@code initAsync: false} — khởi tạo đồng bộ (cần thiết để test chạy đúng)</li>
 * </ul>
 * </p>
 */
void i18n.use(initReactI18next).init({
  resources: {
    vi: { translation: vi },   // Resource dịch tiếng Việt
    en: { translation: en },  // Resource dịch tiếng Anh
  },
  lng: getInitialLanguage(),        // Ngôn ngữ ban đầu
  fallbackLng: DEFAULT_LANGUAGE,    // Fallback nếu ngôn ngữ không hỗ trợ
  supportedLngs: supportedLanguages, // Danh sách ngôn ngữ được hỗ trợ
  interpolation: { escapeValue: false }, // react-i18next tự handle escaping
  // Tài nguyên được bundle sẵn nên có thể khởi tạo đồng bộ (quan trọng cho test).
  initAsync: false,
})

/**
 * Xử lý sự kiện khi người dùng đổi ngôn ngữ.
 * <p>
 * Lưu ngôn ngữ mới vào localStorage để giữ lại sau khi reload trang.
 * </p>
 *
 * @param language ngôn ngữ mới được chọn
 */
i18n.on('languageChanged', (language) => {
  localStorage.setItem(LANGUAGE_STORAGE_KEY, language)
})

/** Xuất đối tượng i18n đã được cấu hình — import vào App.tsx hoặc các component cần dùng */
export default i18n
