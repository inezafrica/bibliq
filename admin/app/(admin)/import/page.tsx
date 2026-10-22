import { requireRole } from "@/lib/auth";
import { ImportForm } from "@/components/import-form";

export default async function ImportPage() {
  await requireRole(["admin"]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-heading text-2xl font-bold text-bibliq-ink">Import</h1>
        <p className="text-sm text-muted-foreground">
          Upload an existing pack JSON (e.g. pack_001.json) to bring its questions and levels into the database.
        </p>
      </div>
      <ImportForm />
    </div>
  );
}
