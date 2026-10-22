import { updatePassword } from "./actions";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

// Reads searchParams directly at the top level.
// With cacheComponents enabled, use ensureStatic = false to prevent static
// prerendering (searchParams are request-time data), and instant = false
// to allow the route to block on navigation.
export const ensureStatic = false;
export const instant = false;

export default async function UpdatePasswordPage({
  searchParams,
}: {
  searchParams: Promise<{ error?: string }>;
}) {
  const { error } = await searchParams;

  return (
    <div className="flex min-h-screen items-center justify-center bg-bibliq-parchment px-4">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle className="font-heading text-2xl text-bibliq-royal">Set your password</CardTitle>
          <CardDescription>Choose a password for your BibliQ admin account.</CardDescription>
        </CardHeader>
        <CardContent>
          <form action={updatePassword} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="password">New password</Label>
              <Input id="password" name="password" type="password" required minLength={8} autoComplete="new-password" />
            </div>
            <div className="space-y-2">
              <Label htmlFor="confirm">Confirm password</Label>
              <Input id="confirm" name="confirm" type="password" required minLength={8} autoComplete="new-password" />
            </div>
            {error && <p className="text-sm text-destructive">{error}</p>}
            <Button type="submit" className="w-full bg-bibliq-royal hover:bg-bibliq-royal-dark">
              Save password
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
