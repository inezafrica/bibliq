"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import {
  ColumnDef,
  flexRender,
  getCoreRowModel,
  getSortedRowModel,
  SortingState,
  useReactTable,
} from "@tanstack/react-table";
import { toast } from "sonner";
import { Checkbox } from "@/components/ui/checkbox";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { QuestionStatusBadge } from "@/components/status-badge";
import { CheckDots } from "@/components/check-dots";
import { QUESTION_STATUSES, QuestionStatus } from "@/lib/content-types";
import { bulkAssignToLevel, bulkDeleteDrafts, bulkUpdateStatus } from "./actions";

export interface QuestionRow {
  id: string;
  type: string;
  difficulty: number;
  status: QuestionStatus;
  book: string | null;
  text: Record<string, string> | null;
  image_key: string | null;
  check_bible: boolean;
  check_en: boolean;
  check_fr: boolean;
  check_es: boolean;
  used: boolean;
}

export function QuestionsTable({ questions }: { questions: QuestionRow[] }) {
  const [sorting, setSorting] = useState<SortingState>([]);
  const [rowSelection, setRowSelection] = useState<Record<string, boolean>>({});
  const [assignDialogOpen, setAssignDialogOpen] = useState(false);
  const [levelInput, setLevelInput] = useState("");
  const [pending, setPending] = useState(false);

  const columns = useMemo<ColumnDef<QuestionRow>[]>(
    () => [
      {
        id: "select",
        header: ({ table }) => (
          <Checkbox
            checked={table.getIsAllPageRowsSelected()}
            onCheckedChange={(v) => table.toggleAllPageRowsSelected(!!v)}
            aria-label="Select all"
          />
        ),
        cell: ({ row }) => (
          <Checkbox
            checked={row.getIsSelected()}
            onCheckedChange={(v) => row.toggleSelected(!!v)}
            onClick={(e) => e.stopPropagation()}
            aria-label="Select row"
          />
        ),
        enableSorting: false,
      },
      { accessorKey: "id", header: "ID" },
      { accessorKey: "type", header: "Type" },
      { accessorKey: "difficulty", header: "Diff." },
      { accessorKey: "book", header: "Book" },
      {
        id: "text",
        header: "Question (EN)",
        accessorFn: (row) => row.text?.en ?? "",
        cell: ({ getValue }) => (
          <span className="line-clamp-1 max-w-xs">{String(getValue()) || "—"}</span>
        ),
      },
      {
        id: "checks",
        header: "Checks",
        cell: ({ row }) => <CheckDots checks={row.original} />,
        enableSorting: false,
      },
      {
        accessorKey: "status",
        header: "Status",
        cell: ({ row }) => <QuestionStatusBadge status={row.original.status} />,
      },
      {
        accessorKey: "used",
        header: "Used",
        cell: ({ getValue }) => (getValue() ? "In a level" : "—"),
      },
    ],
    []
  );

  const table = useReactTable({
    data: questions,
    columns,
    state: { sorting, rowSelection },
    onSortingChange: setSorting,
    onRowSelectionChange: setRowSelection,
    getRowId: (row) => row.id,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
  });

  const selectedIds = Object.keys(rowSelection).filter((id) => rowSelection[id]);

  async function handleStatusChange(status: QuestionStatus) {
    setPending(true);
    const { error } = await bulkUpdateStatus(selectedIds, status);
    setPending(false);
    if (error) toast.error(error);
    else {
      toast.success(`Updated ${selectedIds.length} question(s) to ${status}`);
      setRowSelection({});
    }
  }

  async function handleDeleteDrafts() {
    setPending(true);
    const { error } = await bulkDeleteDrafts(selectedIds);
    setPending(false);
    if (error) toast.error(error);
    else {
      toast.success(`Deleted ${selectedIds.length} draft(s)`);
      setRowSelection({});
    }
  }

  async function handleAssign() {
    const levelNumber = Number(levelInput);
    if (!Number.isInteger(levelNumber) || levelNumber <= 0) {
      toast.error("Enter a valid level number");
      return;
    }
    setPending(true);
    const { error } = await bulkAssignToLevel(selectedIds, levelNumber);
    setPending(false);
    setAssignDialogOpen(false);
    setLevelInput("");
    if (error) toast.error(error);
    else {
      toast.success(`Assigned ${selectedIds.length} question(s) to level ${levelNumber}`);
      setRowSelection({});
    }
  }

  return (
    <div className="space-y-3">
      {selectedIds.length > 0 && (
        <div className="flex items-center gap-3 rounded-lg border bg-bibliq-parchment/60 px-4 py-2">
          <span className="text-sm font-medium">{selectedIds.length} selected</span>
          <Select onValueChange={(v) => handleStatusChange(v as QuestionStatus)}>
            <SelectTrigger className="h-8 w-44" disabled={pending}>
              <SelectValue placeholder="Change status to…" />
            </SelectTrigger>
            <SelectContent>
              {QUESTION_STATUSES.map((s) => (
                <SelectItem key={s} value={s}>
                  {s.replace("_", " ")}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button size="sm" variant="outline" disabled={pending} onClick={() => setAssignDialogOpen(true)}>
            Assign to level…
          </Button>
          <Button size="sm" variant="destructive" disabled={pending} onClick={handleDeleteDrafts}>
            Delete drafts
          </Button>
        </div>
      )}

      <div className="overflow-hidden rounded-lg border bg-background">
        <Table>
          <TableHeader>
            {table.getHeaderGroups().map((headerGroup) => (
              <TableRow key={headerGroup.id}>
                {headerGroup.headers.map((header) => (
                  <TableHead key={header.id}>
                    {header.isPlaceholder ? null : flexRender(header.column.columnDef.header, header.getContext())}
                  </TableHead>
                ))}
              </TableRow>
            ))}
          </TableHeader>
          <TableBody>
            {table.getRowModel().rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={columns.length} className="h-24 text-center text-muted-foreground">
                  No questions match these filters.
                </TableCell>
              </TableRow>
            ) : (
              table.getRowModel().rows.map((row) => (
                <TableRow key={row.id} className="cursor-pointer" data-state={row.getIsSelected() && "selected"}>
                  {row.getVisibleCells().map((cell) => (
                    <TableCell key={cell.id}>
                      {cell.column.id === "select" ? (
                        flexRender(cell.column.columnDef.cell, cell.getContext())
                      ) : (
                        <Link href={`/questions/${row.original.id}`} className="block">
                          {flexRender(cell.column.columnDef.cell, cell.getContext())}
                        </Link>
                      )}
                    </TableCell>
                  ))}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <Dialog open={assignDialogOpen} onOpenChange={setAssignDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Assign {selectedIds.length} question(s) to a level</DialogTitle>
          </DialogHeader>
          <Input
            type="number"
            min={1}
            placeholder="Level number"
            value={levelInput}
            onChange={(e) => setLevelInput(e.target.value)}
          />
          <DialogFooter>
            <Button variant="outline" onClick={() => setAssignDialogOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleAssign} disabled={pending}>
              Assign
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
