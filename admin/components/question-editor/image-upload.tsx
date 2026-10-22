"use client";

import { useRef, useState } from "react";
import { toast } from "sonner";
import { RefreshCw, Trash2, UploadCloud } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

interface ImageUploadProps {
  questionId: string | null;
  previewUrl: string | null;
  onUploaded: (key: string, localPreviewUrl: string) => void;
  /** Detaches the image from the question only; the S3 file stays, since published packs may still use it. */
  onRemoved: () => void;
}

export function ImageUpload({ questionId, previewUrl, onUploaded, onRemoved }: ImageUploadProps) {
  const [dragOver, setDragOver] = useState(false);
  const [uploading, setUploading] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  async function handleFile(file: File) {
    if (!questionId) {
      toast.error("Save this question as a draft first, then add the image.");
      return;
    }
    setUploading(true);
    const localUrl = URL.createObjectURL(file);

    const formData = new FormData();
    formData.append("file", file);
    formData.append("questionId", questionId);

    try {
      const res = await fetch("/api/images/upload", { method: "POST", body: formData });
      const body = await res.json();
      if (!res.ok) {
        toast.error(body.error ?? "Upload failed");
        return;
      }
      toast.success("Image uploaded");
      onUploaded(body.key as string, localUrl);
    } catch {
      toast.error("Upload failed -- check your connection and try again");
    } finally {
      setUploading(false);
    }
  }

  return (
    <div
      className={cn(
        "flex min-h-[140px] flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed p-4 text-center transition-colors",
        dragOver ? "border-bibliq-royal bg-bibliq-royal/5" : "border-border"
      )}
      onDragOver={(e) => {
        e.preventDefault();
        setDragOver(true);
      }}
      onDragLeave={() => setDragOver(false)}
      onDrop={(e) => {
        e.preventDefault();
        setDragOver(false);
        const file = e.dataTransfer.files?.[0];
        if (file) handleFile(file);
      }}
    >
      {previewUrl ? (
        <>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={previewUrl} alt="" className="h-28 rounded-md object-cover" />
          <div className="flex gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={uploading}
              onClick={() => inputRef.current?.click()}
            >
              <RefreshCw className="h-3.5 w-3.5" /> {uploading ? "Uploading…" : "Replace"}
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={uploading}
              className="text-destructive hover:text-destructive"
              onClick={() => {
                onRemoved();
                toast("Image removed. Save the question to keep this change.");
              }}
            >
              <Trash2 className="h-3.5 w-3.5" /> Remove
            </Button>
          </div>
        </>
      ) : (
        <>
          <UploadCloud className="h-8 w-8 text-muted-foreground" />
          <p className="text-sm text-muted-foreground">
            {uploading ? "Uploading…" : "Drag an image here, or"}{" "}
            {!uploading && (
              <button
                type="button"
                className="text-bibliq-royal underline underline-offset-2"
                onClick={() => inputRef.current?.click()}
              >
                browse
              </button>
            )}
          </p>
        </>
      )}
      <p className="text-xs text-muted-foreground">
        PNG, JPG or WebP. No text inside the image -- it&apos;s shared by all three languages.
      </p>
      <input
        ref={inputRef}
        type="file"
        accept="image/png,image/jpeg,image/webp"
        className="hidden"
        onChange={(e) => {
          const file = e.target.files?.[0];
          if (file) handleFile(file);
          e.target.value = "";
        }}
      />
    </div>
  );
}
