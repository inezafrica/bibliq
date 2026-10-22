import { login } from "./actions";

// Reads searchParams directly at the top level.
// With cacheComponents enabled, use ensureStatic = false to prevent static
// prerendering (searchParams are request-time data), and instant = false
// to allow the route to block on navigation.
export const ensureStatic = false;
export const instant = false;
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ error?: string; next?: string }>;
}) {
  const { error, next } = await searchParams;

  return (
    <div className="flex min-h-screen items-center justify-center bg-bibliq-parchment px-4">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle className="font-heading text-2xl text-bibliq-royal">BibliQ Admin</CardTitle>
          <CardDescription>Sign in with your admin account.</CardDescription>
        </CardHeader>
        <CardContent>
          <form action={login} className="space-y-4">
            <input type="hidden" name="next" value={next ?? "/dashboard"} />
            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input id="email" name="email" type="email" required autoComplete="email" />
            </div>
            <div className="space-y-2">
              <Label htmlFor="password">Password</Label>
              <Input id="password" name="password" type="password" required autoComplete="current-password" />
            </div>
            {error && <p className="text-sm text-destructive">{error}</p>}
            <Button type="submit" className="w-full bg-bibliq-royal hover:bg-bibliq-royal-dark">
              Sign in
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
