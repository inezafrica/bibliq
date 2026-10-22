"use client";

import { useCallback, useTransition } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { QUESTION_STATUSES, QUESTION_TYPES } from "@/lib/content-types";

const ANY = "any";

export function QuestionsFilters({ worlds }: { worlds: { id: number; books: string }[] }) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [, startTransition] = useTransition();

  const setParam = useCallback(
    (key: string, value: string | null) => {
      const next = new URLSearchParams(searchParams.toString());
      if (value && value !== ANY) {
        next.set(key, value);
      } else {
        next.delete(key);
      }
      startTransition(() => router.push(`${pathname}?${next.toString()}`));
    },
    [pathname, router, searchParams]
  );

  return (
    <div className="flex flex-wrap items-center gap-2">
      <Input
        placeholder="Search question text…"
        defaultValue={searchParams.get("q") ?? ""}
        className="w-56"
        onChange={(e) => setParam("q", e.target.value)}
      />

      <Select defaultValue={searchParams.get("status") ?? ANY} onValueChange={(v) => setParam("status", v)}>
        <SelectTrigger className="w-40">
          <SelectValue placeholder="Status" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value={ANY}>Any status</SelectItem>
          {QUESTION_STATUSES.map((s) => (
            <SelectItem key={s} value={s}>
              {s.replace("_", " ")}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      <Select defaultValue={searchParams.get("type") ?? ANY} onValueChange={(v) => setParam("type", v)}>
        <SelectTrigger className="w-40">
          <SelectValue placeholder="Type" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value={ANY}>Any type</SelectItem>
          {QUESTION_TYPES.map((t) => (
            <SelectItem key={t} value={t}>
              {t}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      <Select defaultValue={searchParams.get("book") ?? ANY} onValueChange={(v) => setParam("book", v)}>
        <SelectTrigger className="w-44">
          <SelectValue placeholder="Book" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value={ANY}>Any book</SelectItem>
          {worlds.map((w) => (
            <SelectItem key={w.id} value={w.books}>
              {w.books}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      <Select
        defaultValue={searchParams.get("hasImage") ?? ANY}
        onValueChange={(v) => setParam("hasImage", v)}
      >
        <SelectTrigger className="w-36">
          <SelectValue placeholder="Image" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value={ANY}>Image: any</SelectItem>
          <SelectItem value="true">Has image</SelectItem>
          <SelectItem value="false">No image</SelectItem>
        </SelectContent>
      </Select>

      <Select defaultValue={searchParams.get("used") ?? ANY} onValueChange={(v) => setParam("used", v)}>
        <SelectTrigger className="w-36">
          <SelectValue placeholder="Used" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value={ANY}>Used: any</SelectItem>
          <SelectItem value="true">In a level</SelectItem>
          <SelectItem value="false">Unused</SelectItem>
        </SelectContent>
      </Select>

      <Select
        defaultValue={searchParams.get("failedChecks") ?? ANY}
        onValueChange={(v) => setParam("failedChecks", v)}
      >
        <SelectTrigger className="w-44">
          <SelectValue placeholder="Checks" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value={ANY}>Checks: any</SelectItem>
          <SelectItem value="true">Missing a check</SelectItem>
        </SelectContent>
      </Select>

      {searchParams.size > 0 && (
        <Button variant="ghost" size="sm" onClick={() => router.push(pathname)}>
          Clear filters
        </Button>
      )}
    </div>
  );
}
