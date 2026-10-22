import { cn } from "@/lib/utils";

interface ChecksState {
  check_bible: boolean;
  check_en: boolean;
  check_fr: boolean;
  check_es: boolean;
}

const LABELS: { key: keyof ChecksState; label: string }[] = [
  { key: "check_bible", label: "Bible" },
  { key: "check_en", label: "EN" },
  { key: "check_fr", label: "FR" },
  { key: "check_es", label: "ES" },
];

export function CheckDots({ checks }: { checks: ChecksState }) {
  return (
    <div className="flex items-center gap-1" title="Bible / EN / FR / ES checks">
      {LABELS.map(({ key, label }) => (
        <span
          key={key}
          className={cn(
            "flex h-5 w-5 items-center justify-center rounded-full text-[9px] font-bold",
            checks[key] ? "bg-bibliq-olive text-white" : "bg-muted text-muted-foreground"
          )}
        >
          {label[0]}
        </span>
      ))}
    </div>
  );
}
