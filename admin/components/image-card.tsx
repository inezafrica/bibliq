"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { deleteImage } from "@/app/(admin)/images/actions";

interface ImageCardProps {
  imageKey: string;
  url: string;
  width: number;
  height: number;
  bytes: number;
  usedByQuestions: string[];
  inPacks: number[];
}

export function ImageCard({ imageKey, url, width, height, bytes, usedByQuestions, inPacks }: ImageCardProps) {
  const router = useRouter();
  const [deleting, setDeleting] = useState(false);

  const blockedReason = usedByQuestions.length
    ? `Used by ${usedByQuestions.join(", ")}. Remove or replace it in the question first.`
    : inPacks.length
      ? `Players still download it in published pack ${inPacks.join(", ")}. Republish that pack first.`
      : null;

  async function handleDelete() {
    if (!confirm(`Delete ${imageKey}? This cannot be undone.`)) return;
    setDeleting(true);
    const { error } = await deleteImage(imageKey);
    setDeleting(false);
    if (error) toast.error(error);
    else {
      toast.success("Image deleted");
      router.refresh();
    }
  }

  return (
    <div className="space-y-2 rounded-lg border p-2">
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img src={url} alt="" className="aspect-[3/2] w-full rounded-md object-cover" />
      <p className="truncate font-mono text-xs text-muted-foreground">{imageKey}</p>
      <p className="text-xs text-muted-foreground">
        {width}×{height} · {(bytes / 1024).toFixed(0)} KB
      </p>
      <div className="flex items-start justify-between gap-1">
        <div className="flex flex-wrap gap-1">
          {usedByQuestions.map((id) => (
            <Link key={id} href={`/questions/${id}`}>
              <Badge variant="outline" className="bg-bibliq-olive/15 text-bibliq-olive">
                Used by {id}
              </Badge>
            </Link>
          ))}
          {inPacks.map((id) => (
            <Badge key={id} variant="outline" className="bg-bibliq-royal/10 text-bibliq-royal">
              Live in pack {id}
            </Badge>
          ))}
          {!blockedReason && (
            <Badge variant="outline" className="bg-bibliq-coral/15 text-bibliq-coral">
              Unused
            </Badge>
          )}
        </div>
        <Button
          variant="ghost"
          size="icon-sm"
          aria-label={`Delete ${imageKey}`}
          title={blockedReason ?? "Delete image"}
          disabled={deleting}
          onClick={blockedReason ? () => toast.error(blockedReason) : handleDelete}
        >
          <Trash2 className={blockedReason ? "h-3.5 w-3.5 opacity-40" : "h-3.5 w-3.5 text-destructive"} />
        </Button>
      </div>
    </div>
  );
}
