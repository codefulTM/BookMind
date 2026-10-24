/* eslint-disable react-refresh/only-export-components -- shadcn/ui exports buttonVariants alongside Button */
import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "@/shared/lib/utils"
import { Slot } from "radix-ui"

/**
 * {@code buttonVariants} — Định nghĩa các biến thể (variant) cho button component.
 * <p>
 * Sử dụng `class-variance-authority` để tạo nhiều kiểu button:
 * <ul>
 *   <li>{@code variant}: default, destructive, outline, secondary, ghost, link</li>
 *   <li>{@code size}: default, xs, sm, lg, icon, icon-xs, icon-sm, icon-lg</li>
 * </ul>
 * Mỗi variant có class Tailwind CSS riêng, được merge thông qua {@link cn}.
 * </p>
 */
const buttonVariants = cva(
  // Base styles — áp dụng cho tất cả button
  "inline-flex shrink-0 items-center justify-center gap-2 rounded-md text-sm font-medium whitespace-nowrap transition-all outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:pointer-events-none disabled:opacity-50 aria-invalid:border-destructive aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
  {
    variants: {
      variant: {
        default: "bg-primary text-primary-foreground hover:bg-primary/90",
        destructive:
          "bg-destructive text-white hover:bg-destructive/90 focus-visible:ring-destructive/20 dark:bg-destructive/60 dark:focus-visible:ring-destructive/40",
        outline:
          "border bg-background shadow-xs hover:bg-accent hover:text-accent-foreground dark:border-input dark:bg-input/30 dark:hover:bg-input/50",
        secondary:
          "bg-secondary text-secondary-foreground hover:bg-secondary/80",
        ghost:
          "hover:bg-accent hover:text-accent-foreground dark:hover:bg-accent/50",
        link: "text-primary underline-offset-4 hover:underline",
      },
      size: {
        default: "h-9 px-4 py-2 has-[>svg]:px-3",
        xs: "h-6 gap-1 rounded-md px-2 text-xs has-[>svg]:px-1.5 [&_svg:not([class*='size-'])]:size-3",
        sm: "h-8 gap-1.5 rounded-md px-3 has-[>svg]:px-2.5",
        lg: "h-10 rounded-md px-6 has-[>svg]:px-4",
        icon: "size-9",
        "icon-xs": "size-6 rounded-md [&_svg:not([class*='size-'])]:size-3",
        "icon-sm": "size-8",
        "icon-lg": "size-10",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

/**
 * {@code Button} — Component button tái sử dụng với các biến thể.
 * <p>
 * Props:
 * <ul>
 *   <li>{@code className} — class Tailwind CSS bổ sung</li>
 *   <li>{@code variant} — kiểu button (default, destructive, outline...)</li>
 *   <li>{@code size} — kích thước button (default, sm, lg, icon...)</li>
 *   <li>{@code asChild} — nếu true, render {@code Slot.Root} thay vì {@code &lt;button&gt;} để nhúng vào component khác</li>
 * </ul>
 * </p>
 */
function Button({
  className,
  variant = "default",
  size = "default",
  asChild = false,
  ...props
}: React.ComponentProps<"button"> &
  VariantProps<typeof buttonVariants> & {
    asChild?: boolean
  }) {
  // Chọn component: Slot.Root nếu asChild=true (để nhúng), otherwise <button>
  const Comp = asChild ? Slot.Root : "button"

  return (
    <Comp
      data-slot="button"              // Data attribute để CSS scoped
      data-variant={variant}          // Variant hiện tại (dùng cho debug)
      data-size={size}                // Size hiện tại (dùng cho debug)
      className={cn(buttonVariants({ variant, size, className }))} // Merge variants + custom className
      {...props}                      // Truyền tất cả props còn lại (onClick, disabled...)
    />
  )
}

/** Xuất Button component và buttonVariants (cho phép import riêng lẻ) */
export { Button, buttonVariants }
