"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { SUPPORTED_LANGUAGES } from "@/lib/content-types";
import { updateSetting } from "@/app/(admin)/settings/actions";

export function GeneralSettings({
  nextDropDate,
  minAppVersion,
}: {
  nextDropDate: string;
  minAppVersion: number;
}) {
  const router = useRouter();
  const [date, setDate] = useState(nextDropDate);
  const [version, setVersion] = useState(String(minAppVersion));
  const [pending, startTransition] = useTransition();

  function save() {
    startTransition(async () => {
      const [{ error: e1 }, { error: e2 }] = await Promise.all([
        updateSetting("next_drop_date", date),
        updateSetting("min_app_version", Number(version)),
      ]);
      if (e1 || e2) toast.error(e1 ?? e2 ?? "Could not save");
      else {
        toast.success("Saved");
        router.refresh();
      }
    });
  }

  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-2">
          <Label>Next drop date</Label>
          <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        </div>
        <div className="space-y-2">
          <Label>Minimum app version</Label>
          <Input type="number" min={1} value={version} onChange={(e) => setVersion(e.target.value)} />
        </div>
      </div>
      <Button className="bg-bibliq-royal hover:bg-bibliq-royal-dark" onClick={save} disabled={pending}>
        Save
      </Button>

      <div className="space-y-2">
        <Label>Supported languages</Label>
        <p className="text-sm text-muted-foreground">
          Defined in code ({`SUPPORTED_LANGUAGES`}), read-only here: {SUPPORTED_LANGUAGES.join(", ").toUpperCase()}
        </p>
      </div>
    </div>
  );
}
