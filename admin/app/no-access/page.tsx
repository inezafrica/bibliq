import { logout } from "@/app/login/actions";
import { Button } from "@/components/ui/button";

export default function NoAccessPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-bibliq-parchment px-4 text-center">
      <div className="max-w-sm space-y-4">
        <h1 className="font-heading text-2xl text-bibliq-royal">No access</h1>
        <p className="text-muted-foreground">
          Your account isn&apos;t set up as a BibliQ admin yet. Ask an existing admin to add you from
          Settings → Users.
        </p>
        <form action={logout}>
          <Button type="submit" variant="outline">
            Sign out
          </Button>
        </form>
      </div>
    </div>
  );
}
