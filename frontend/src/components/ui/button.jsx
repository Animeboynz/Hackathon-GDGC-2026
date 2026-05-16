import * as React from "react";
import { cn } from "@/lib/utils";

const variants = {
  default:
    "bg-inspo-blue text-white shadow hover:bg-inspo-blueHover disabled:opacity-50",
  lime:
    "appearance-none border-2 border-[#500040] bg-[#ae0086] font-bold text-white shadow-md hover:bg-[#920071] hover:shadow-lg disabled:border-slate-400 disabled:bg-slate-200 disabled:text-slate-700 disabled:opacity-100 disabled:shadow-none",
  secondary:
    "border border-white/20 bg-white/10 text-white shadow-sm hover:bg-white/15 disabled:opacity-50",
};

export const Button = React.forwardRef(
  ({ className, variant = "default", type = "button", ...props }, ref) => (
    <button
      ref={ref}
      type={type}
      className={cn(
        "inline-flex items-center justify-center whitespace-nowrap rounded-md text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#ae0086] focus-visible:ring-offset-2 disabled:pointer-events-none",
        variants[variant] || variants.default,
        className
      )}
      {...props}
    />
  )
);
Button.displayName = "Button";
