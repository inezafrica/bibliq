"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  FileInput,
  FileQuestion,
  Image as ImageIcon,
  LayoutDashboard,
  ListTree,
  LogOut,
  Settings,
  UploadCloud,
} from "lucide-react";
import { logout } from "@/app/login/actions";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { AdminRole } from "@/lib/content-types";

interface NavItem {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  roles?: AdminRole[];
}

const NAV_ITEMS: NavItem[] = [
  { href: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { href: "/questions", label: "Questions", icon: FileQuestion },
  { href: "/levels", label: "Levels", icon: ListTree },
  { href: "/images", label: "Images", icon: ImageIcon },
  { href: "/publish", label: "Publish", icon: UploadCloud, roles: ["admin"] },
  { href: "/import", label: "Import", icon: FileInput, roles: ["admin"] },
  { href: "/settings", label: "Settings", icon: Settings, roles: ["admin"] },
];

export function AdminSidebar({ name, role }: { name: string; role: AdminRole }) {
  const pathname = usePathname();

  return (
    <aside className="flex w-64 shrink-0 flex-col bg-sidebar text-sidebar-foreground">
      <div className="px-5 py-6">
        <p className="font-heading text-xl font-bold text-sidebar-primary">BibliQ</p>
        <p className="text-xs text-sidebar-foreground/70">Admin</p>
      </div>
      <nav className="flex-1 space-y-1 px-3">
        {NAV_ITEMS.filter((item) => !item.roles || item.roles.includes(role)).map((item) => {
          const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
          const Icon = item.icon;
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
                active
                  ? "bg-sidebar-accent text-sidebar-accent-foreground"
                  : "text-sidebar-foreground/80 hover:bg-sidebar-accent/60 hover:text-sidebar-accent-foreground"
              )}
            >
              <Icon className="h-4 w-4" />
              {item.label}
            </Link>
          );
        })}
      </nav>
      <div className="border-t border-sidebar-border px-3 py-4">
        <p className="truncate px-3 text-sm font-medium">{name}</p>
        <p className="px-3 text-xs capitalize text-sidebar-foreground/60">{role}</p>
        <form action={logout} className="mt-2">
          <Button
            type="submit"
            variant="ghost"
            size="sm"
            className="w-full justify-start gap-2 text-sidebar-foreground/80 hover:bg-sidebar-accent/60 hover:text-sidebar-accent-foreground"
          >
            <LogOut className="h-4 w-4" />
            Sign out
          </Button>
        </form>
      </div>
    </aside>
  );
}
