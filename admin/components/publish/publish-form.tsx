"use client";

import { useState, useTransition } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cdnUrl } from "@/lib/cdn";
import { dryRunPublish, publishPack, PublishDryRunResult } from "@/app/(admin)/publish/actions";

interface ReadyLevel {
  number: number;
  isBoss: boolean;
  questionCount: number;
}

interface ExistingPack {
  id: number;
  version: number;
  levelFrom: number;
  levelTo: number;
}

export function PublishForm({
  readyLevels,
  existingPacks,
  defaultNextDropDate,
}: {
  readyLevels: ReadyLevel[];
  existingPacks: ExistingPack[];
  defaultNextDropDate: string;
}) {
  const [mode, setMode] = useState<"new" | "republish">("new");
  const [selectedLevels, setSelectedLevels] = useState<Set<number>>(
    new Set(readyLevels.slice(0, 15).map((l) => l.number))
  );
  const [republishPackId, setRepublishPackId] = useState<string>(existingPacks[0] ? String(existingPacks[0].id) : "");
  const [nextDropDate, setNextDropDate] = useState(defaultNextDropDate);
  const [result, setResult] = useState<PublishDryRunResult | null>(null);
  const [pending, startTransition] = useTransition();

  function toggleLevel(number: number) {
    setSelectedLevels((prev) => {
      const next = new Set(prev);
      if (next.has(number)) next.delete(number);
      else next.add(number);
      return next;
    });
  }

  function currentInput() {
    if (mode === "republish") {
      const pack = existingPacks.find((p) => p.id === Number(republishPackId));
      const levelNumbers = pack
        ? Array.from({ length: pack.levelTo - pack.levelFrom + 1 }, (_, i) => pack.levelFrom + i)
        : [];
      return { mode: "republish" as const, packId: Number(republishPackId), levelNumbers, nextDropDate };
    }
    return { mode: "new" as const, levelNumbers: Array.from(selectedLevels).sort((a, b) => a - b), nextDropDate };
  }

  function handleDryRun() {
    startTransition(async () => {
      const res = await dryRunPublish(currentInput());
      setResult(res);
      if (res.errors.length > 0) toast.error(`${res.errors.length} issue(s) found`);
      else toast.success("Dry run looks good -- ready to publish");
    });
  }

  function handlePublish() {
    if (!result || result.errors.length > 0) return;
    if (!confirm(`Publish pack ${result.packId} v${result.packVersion} covering levels ${result.pack.levels[0]?.number}-${result.pack.levels.at(-1)?.number}?`)) {
      return;
    }
    startTransition(async () => {
      const res = await publishPack(currentInput());
      if (res.error) {
        toast.error(res.error);
        setResult(res.result);
      } else {
        toast.success("Published!");
        setResult(res.result);
      }
    });
  }

  return (
    <div className="space-y-6">
      <div className="flex gap-2 rounded-full bg-muted p-1 w-fit">
        <button
          type="button"
          onClick={() => setMode("new")}
          className={`rounded-full px-4 py-1.5 text-sm font-medium ${mode === "new" ? "bg-bibliq-royal text-white" : ""}`}
        >
          New pack
        </button>
        <button
          type="button"
          onClick={() => setMode("republish")}
          className={`rounded-full px-4 py-1.5 text-sm font-medium ${mode === "republish" ? "bg-bibliq-royal text-white" : ""}`}
        >
          Republish existing pack
        </button>
      </div>

      {mode === "new" ? (
        <div className="space-y-2">
          <Label>Ready levels to include</Label>
          <div className="grid max-h-64 grid-cols-3 gap-2 overflow-y-auto rounded-lg border p-3 sm:grid-cols-5">
            {readyLevels.map((l) => (
              <label key={l.number} className="flex items-center gap-2 text-sm">
                <Checkbox checked={selectedLevels.has(l.number)} onCheckedChange={() => toggleLevel(l.number)} />
                Level {l.number} {l.isBoss ? "👑" : ""}
              </label>
            ))}
            {readyLevels.length === 0 && <p className="text-sm text-muted-foreground">No ready levels.</p>}
          </div>
        </div>
      ) : (
        <div className="space-y-2">
          <Label>Pack to republish</Label>
          <Select value={republishPackId} onValueChange={(v) => setRepublishPackId(v ?? "")}>
            <SelectTrigger className="w-72">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {existingPacks.map((p) => (
                <SelectItem key={p.id} value={String(p.id)}>
                  Pack {p.id} (v{p.version}) -- levels {p.levelFrom}-{p.levelTo}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      )}

      <div className="space-y-2">
        <Label>Next drop date</Label>
        <Input type="date" value={nextDropDate} onChange={(e) => setNextDropDate(e.target.value)} className="w-48" />
      </div>

      <div className="flex gap-2">
        <Button variant="outline" onClick={handleDryRun} disabled={pending}>
          Dry run
        </Button>
        <Button
          className="bg-bibliq-royal hover:bg-bibliq-royal-dark"
          onClick={handlePublish}
          disabled={pending || !result || result.errors.length > 0}
        >
          Publish
        </Button>
      </div>

      {result && (
        <div className="space-y-4">
          {result.errors.length > 0 && (
            <Alert variant="destructive">
              <AlertTitle>{result.errors.length} error(s) -- publishing is disabled</AlertTitle>
              <AlertDescription>
                <ul className="max-h-48 list-inside list-disc overflow-y-auto">
                  {result.errors.map((e, i) => (
                    <li key={i}>{e.message}</li>
                  ))}
                </ul>
              </AlertDescription>
            </Alert>
          )}
          {result.missingImages.length > 0 && (
            <Alert variant="destructive">
              <AlertTitle>Missing images in S3</AlertTitle>
              <AlertDescription>{result.missingImages.join(", ")}</AlertDescription>
            </Alert>
          )}
          {result.warnings.length > 0 && (
            <Alert>
              <AlertTitle>{result.warnings.length} warning(s)</AlertTitle>
              <AlertDescription>
                <ul className="max-h-32 list-inside list-disc overflow-y-auto">
                  {result.warnings.map((w, i) => (
                    <li key={i}>{w.message}</li>
                  ))}
                </ul>
              </AlertDescription>
            </Alert>
          )}

          <div className="rounded-lg border p-4 text-sm">
            <p>
              <strong>{result.isRepublish ? "Republishing" : "Publishing"}</strong> pack {result.packId} as version{" "}
              {result.packVersion} -- {result.pack.questions.length} question(s) across {result.pack.levels.length}{" "}
              level(s)
              {result.previousQuestionCount !== null &&
                ` (previous version had ${result.previousQuestionCount} questions)`}
              .
            </p>
            <p className="mt-1 text-muted-foreground">
              Will upload to <code>{result.packFile}</code> and update <code>manifest.json</code>.
            </p>
          </div>

          <details className="rounded-lg border p-4">
            <summary className="cursor-pointer font-medium">Generated pack JSON</summary>
            <pre className="mt-2 max-h-96 overflow-auto rounded bg-muted p-3 text-xs">{result.packJson}</pre>
          </details>
          <details className="rounded-lg border p-4">
            <summary className="cursor-pointer font-medium">Generated manifest.json</summary>
            <pre className="mt-2 max-h-96 overflow-auto rounded bg-muted p-3 text-xs">
              {JSON.stringify(result.manifest, null, 2)}
            </pre>
          </details>

          {result.manifest.packs.map((p) => (
            <a
              key={p.id}
              href={cdnUrl(p.file)}
              target="_blank"
              rel="noreferrer"
              className="block text-sm text-bibliq-royal underline underline-offset-2"
            >
              {cdnUrl(p.file)}
            </a>
          ))}
        </div>
      )}
    </div>
  );
}
