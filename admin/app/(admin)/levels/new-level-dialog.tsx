"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Plus } from "lucide-react";
import { createLevel } from "./actions";

export function NewLevelDialog({ worlds }: { worlds: { id: number; books: string }[] }) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [number, setNumber] = useState("");
  const [worldId, setWorldId] = useState(worlds[0] ? String(worlds[0].id) : "");
  const [isBoss, setIsBoss] = useState(false);
  const [pending, setPending] = useState(false);

  async function handleCreate() {
    const n = Number(number);
    if (!Number.isInteger(n) || n <= 0 || !worldId) {
      toast.error("Enter a valid level number and world");
      return;
    }
    setPending(true);
    const { error } = await createLevel({ number: n, worldId: Number(worldId), isBoss });
    setPending(false);
    if (error) {
      toast.error(error);
      return;
    }
    setOpen(false);
    toast.success(`Level ${n} created`);
    router.push(`/levels/${n}`);
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger render={<Button className="bg-bibliq-royal hover:bg-bibliq-royal-dark" />}>
        <Plus className="h-4 w-4" /> New level
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>New level</DialogTitle>
        </DialogHeader>
        <div className="space-y-4">
          <div className="space-y-2">
            <Label>Level number</Label>
            <Input type="number" min={1} value={number} onChange={(e) => setNumber(e.target.value)} />
          </div>
          <div className="space-y-2">
            <Label>World</Label>
            <Select value={worldId} onValueChange={(v) => setWorldId(v ?? "")}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {worlds.map((w) => (
                  <SelectItem key={w.id} value={String(w.id)}>
                    World {w.id} -- {w.books}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <label className="flex items-center gap-2">
            <Checkbox checked={isBoss} onCheckedChange={(v) => setIsBoss(!!v)} />
            <span className="text-sm">Boss level (15 questions)</span>
          </label>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => setOpen(false)}>
            Cancel
          </Button>
          <Button onClick={handleCreate} disabled={pending}>
            Create
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
