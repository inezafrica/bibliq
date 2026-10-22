"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { ADMIN_ROLES, AdminRole } from "@/lib/content-types";
import { inviteUser, removeUser, updateUserRole } from "@/app/(admin)/settings/actions";

export interface AdminUserRow {
  userId: string;
  name: string;
  role: AdminRole;
  email: string | null;
}

export function UsersPanel({ users, currentUserId }: { users: AdminUserRow[]; currentUserId: string }) {
  const router = useRouter();
  const [pending, startTransition] = useTransition();
  const [open, setOpen] = useState(false);
  const [email, setEmail] = useState("");
  const [name, setName] = useState("");
  const [role, setRole] = useState<AdminRole>("editor");

  function handleInvite() {
    if (!email.trim() || !name.trim()) {
      toast.error("Name and email are required");
      return;
    }
    startTransition(async () => {
      const { error } = await inviteUser(email.trim(), name.trim(), role);
      if (error) toast.error(error);
      else {
        toast.success(`Invited ${email}`);
        setOpen(false);
        setEmail("");
        setName("");
        router.refresh();
      }
    });
  }

  function handleRoleChange(userId: string, newRole: AdminRole) {
    startTransition(async () => {
      const { error } = await updateUserRole(userId, newRole);
      if (error) toast.error(error);
      else router.refresh();
    });
  }

  function handleRemove(userId: string) {
    if (!confirm("Remove this user's admin access?")) return;
    startTransition(async () => {
      const { error } = await removeUser(userId);
      if (error) toast.error(error);
      else {
        toast.success("Removed");
        router.refresh();
      }
    });
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="font-heading text-lg font-semibold">Admin users</h2>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger render={<Button className="bg-bibliq-royal hover:bg-bibliq-royal-dark" />}>
            Invite user
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Invite an admin</DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              <div className="space-y-2">
                <Label>Name</Label>
                <Input value={name} onChange={(e) => setName(e.target.value)} />
              </div>
              <div className="space-y-2">
                <Label>Email</Label>
                <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
              </div>
              <div className="space-y-2">
                <Label>Role</Label>
                <Select value={role} onValueChange={(v) => setRole((v as AdminRole) ?? "editor")}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {ADMIN_ROLES.map((r) => (
                      <SelectItem key={r} value={r}>
                        {r}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setOpen(false)}>
                Cancel
              </Button>
              <Button onClick={handleInvite} disabled={pending}>
                Send invite
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>

      <ul className="divide-y rounded-lg border">
        {users.map((u) => (
          <li key={u.userId} className="flex items-center gap-3 p-3">
            <div className="flex-1">
              <p className="font-medium">{u.name}</p>
              <p className="text-xs text-muted-foreground">{u.email ?? u.userId}</p>
            </div>
            <Select
              value={u.role}
              onValueChange={(v) => v && handleRoleChange(u.userId, v as AdminRole)}
              disabled={pending || u.userId === currentUserId}
            >
              <SelectTrigger className="w-32">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {ADMIN_ROLES.map((r) => (
                  <SelectItem key={r} value={r}>
                    {r}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button
              variant="ghost"
              size="sm"
              disabled={pending || u.userId === currentUserId}
              onClick={() => handleRemove(u.userId)}
            >
              Remove
            </Button>
          </li>
        ))}
      </ul>
    </div>
  );
}
