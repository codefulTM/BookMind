/**
 * {@code cn()} — Hàm tiện ích hợp nhất và merge các class name.
 * <p>
 * Kết hợp hai thư viện:
 * <ul>
 *   <li>{@link clsx} — hợp nhất mảng class name, loại bỏ giá trị falsy (null, undefined, false)</li>
 *   <li>{@link twMerge} — merge Tailwind CSS classes, ưu tiên class đứng sau (ví dụ: "text-red-500" override "text-blue-500")</li>
 * </ul>
 * </p>
 * <p>
 * <b>Ví dụ:</b>
 * <pre>{@code
 * cn("px-4", null, "py-2", undefined) // → "px-4 py-2"
 * cn("text-blue-500", "text-red-500")  // → "text-red-500" (twMerge override)
 * }</pre>
 * </p>
 *
 * @param inputs danh sách các class value (string, object, array...) cần hợp nhất
 * @returns chuỗi class name đã được merge và chuẩn hóa
 */
import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
