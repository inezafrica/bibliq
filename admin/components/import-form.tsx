"use client";

import { useRef, useState, useTransition } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { previewImport, runImport, ImportPreviewResult } from "@/app/(admin)/import/actions";

export function ImportForm() {
  const inputRef = useRef<HTMLInputElement>(null);
  const [fileText, setFileText] = useState<string | null>(null);
  const [fileName, setFileName] = useState<string | null>(null);
  const [alreadyReviewed, setAlreadyReviewed] = useState(false);
  const [preview, setPreview] = useState<ImportPreviewResult | null>(null);
  const [pending, startTransition] = useTransition();
  const [report, setReport] = useState<string | null>(null);

  function handleFile(file: File) {
    setFileName(file.name);
    setPreview(null);
    setReport(null);
    const reader = new FileReader();
    reader.onload = () => setFileText(String(reader.result));
    reader.readAsText(file);
  }

  function handlePreview() {
    if (!fileText) return;
    startTransition(async () => {
      const { error, result } = await previewImport(fileText);
      if (error) {
        toast.error(error);
        return;
      }
      setPreview(result);
    });
  }

  function handleImport() {
    if (!fileText) return;
    startTransition(async () => {
      const result = await runImport(fileText, alreadyReviewed);
      if (result.error) {
        toast.error(result.error);
        return;
      }
      setReport(
        `Imported ${result.imported} question(s) and ${result.importedLevels} level(s). Skipped ${result.skipped}.`
      );
      toast.success("Import complete");
    });
  }

  return (
    <div className="space-y-6">
      <div className="space-y-2">
        <input
          ref={inputRef}
          type="file"
          accept="application/json"
          className="hidden"
          onChange={(e) => {
            const file = e.target.files?.[0];
            if (file) handleFile(file);
          }}
        />
        <Button variant="outline" onClick={() => inputRef.current?.click()}>
          Choose pack JSON file…
        </Button>
        {fileName && <p className="text-sm text-muted-foreground">{fileName}</p>}
      </div>

      <label className="flex items-center gap-2 text-sm">
        <Checkbox checked={alreadyReviewed} onCheckedChange={(v) => setAlreadyReviewed(!!v)} />
        Already reviewed (import as approved instead of in-review)
      </label>

      <div className="flex gap-2">
        <Button variant="outline" onClick={handlePreview} disabled={!fileText || pending}>
          Validate &amp; preview
        </Button>
        <Button
          className="bg-bibliq-royal hover:bg-bibliq-royal-dark"
          onClick={handleImport}
          disabled={!preview || pending}
        >
          Import
        </Button>
      </div>

      {report && <p className="rounded-lg border bg-bibliq-parchment/60 p-3 text-sm">{report}</p>}

      {preview && (
        <div className="grid gap-4 md:grid-cols-2">
          <div>
            <h2 className="mb-2 font-heading text-lg font-semibold">
              Questions ({preview.questions.filter((q) => q.willImport).length}/{preview.questions.length} will import)
            </h2>
            <ul className="max-h-80 space-y-1 overflow-y-auto text-sm">
              {preview.questions.map((q) => (
                <li key={q.id} className={q.willImport ? "" : "text-muted-foreground"}>
                  {q.willImport ? "✅" : "⏭️"} {q.id} {q.reason && `-- ${q.reason}`}
                </li>
              ))}
            </ul>
          </div>
          <div>
            <h2 className="mb-2 font-heading text-lg font-semibold">
              Levels ({preview.levels.filter((l) => l.willImport).length}/{preview.levels.length} will import)
            </h2>
            <ul className="max-h-80 space-y-1 overflow-y-auto text-sm">
              {preview.levels.map((l) => (
                <li key={l.id} className={l.willImport ? "" : "text-muted-foreground"}>
                  {l.willImport ? "✅" : "⏭️"} {l.id} {l.reason && `-- ${l.reason}`}
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}
    </div>
  );
}
