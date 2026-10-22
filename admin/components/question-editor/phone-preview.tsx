"use client";

import { useState } from "react";
import { cn } from "@/lib/utils";
import { LanguageCode, QuestionType, SUPPORTED_LANGUAGES } from "@/lib/content-types";

interface PhonePreviewProps {
  type: QuestionType;
  text: Record<LanguageCode, string>;
  options: Record<LanguageCode, string[]>;
  correctIndex: number;
  reference: Record<LanguageCode, string>;
  imageUrl: string | null;
}

const TYPE_LABEL: Record<LanguageCode, Record<string, string>> = {
  en: { MCQ: "MULTIPLE CHOICE", TRUE_FALSE: "TRUE OR FALSE", IMAGE: "PICTURE", WHO_SAID: "WHO SAID IT?" },
  fr: { MCQ: "CHOIX MULTIPLE", TRUE_FALSE: "VRAI OU FAUX", IMAGE: "IMAGE", WHO_SAID: "QUI L'A DIT ?" },
  es: { MCQ: "OPCIÓN MÚLTIPLE", TRUE_FALSE: "VERDADERO O FALSO", IMAGE: "IMAGEN", WHO_SAID: "¿QUIÉN LO DIJO?" },
};

type AnswerMode = "waiting" | "correct" | "wrong";

export function PhonePreview({ type, text, options, correctIndex, reference, imageUrl }: PhonePreviewProps) {
  const [language, setLanguage] = useState<LanguageCode>("en");
  const [mode, setMode] = useState<AnswerMode>("waiting");

  const opts = options[language] ?? [];
  const wrongChosenIndex = opts.findIndex((_, i) => i !== correctIndex);
  const chosenIndex = mode === "correct" ? correctIndex : mode === "wrong" ? wrongChosenIndex : null;
  const revealed = mode !== "waiting";
  const typeLabel = TYPE_LABEL[language][type] ?? TYPE_LABEL[language].MCQ;

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex gap-1 rounded-full bg-muted p-1">
          {SUPPORTED_LANGUAGES.map((lang) => (
            <button
              key={lang}
              type="button"
              onClick={() => setLanguage(lang)}
              className={cn(
                "rounded-full px-3 py-1 text-xs font-bold uppercase transition-colors",
                language === lang ? "bg-bibliq-royal text-white" : "text-muted-foreground hover:text-foreground"
              )}
            >
              {lang}
            </button>
          ))}
        </div>
        <div className="flex gap-1 rounded-full bg-muted p-1">
          {(["waiting", "correct", "wrong"] as AnswerMode[]).map((m) => (
            <button
              key={m}
              type="button"
              onClick={() => setMode(m)}
              className={cn(
                "rounded-full px-3 py-1 text-xs font-bold capitalize transition-colors",
                mode === m ? "bg-bibliq-ink text-white" : "text-muted-foreground hover:text-foreground"
              )}
            >
              {m}
            </button>
          ))}
        </div>
      </div>

      <div
        className="mx-auto overflow-y-auto rounded-[36px] border-8 border-bibliq-ink bg-[#D6EBFF] p-4"
        style={{ width: 390, height: 844 }}
      >
        <span className="mb-3 inline-block rounded-full bg-bibliq-royal px-3 py-1 text-[11px] font-extrabold text-white">
          {typeLabel}
        </span>

        {type === "IMAGE" && (
          <div className="relative mb-3 aspect-[3/2] w-full overflow-hidden rounded-2xl border-4 border-white bg-white shadow-sm">
            {imageUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={imageUrl} alt="" className="h-full w-full object-cover" />
            ) : (
              <div className="flex h-full w-full items-center justify-center text-xs text-muted-foreground">
                No image uploaded
              </div>
            )}
          </div>
        )}

        <div className="mb-3 rounded-3xl border-2 border-[#EED9B4] bg-bibliq-parchment p-4 font-heading font-extrabold text-bibliq-ink">
          {text[language] || <span className="text-muted-foreground">(no text yet)</span>}
        </div>

        <div className={cn("grid gap-3", type === "IMAGE" ? "grid-cols-2" : "grid-cols-1")}>
          {opts.map((label, index) => {
            const isCorrect = index === correctIndex;
            const isChosen = index === chosenIndex;
            const bg = !revealed
              ? "bg-white border-[#E6DDCB]"
              : isCorrect
                ? "bg-bibliq-olive border-bibliq-olive text-white"
                : isChosen
                  ? "bg-bibliq-coral border-[#D9483D]"
                  : "bg-white border-[#E6DDCB] opacity-50";
            return (
              <div
                key={index}
                className={cn(
                  "flex min-h-[48px] items-center gap-2 rounded-2xl border-2 px-3 py-2 text-sm font-bold",
                  bg
                )}
              >
                {type === "MCQ" && (
                  <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-[#EAF2FF] text-xs font-extrabold text-bibliq-royal">
                    {String.fromCharCode(65 + index)}
                  </span>
                )}
                <span className="whitespace-pre-wrap">{label || "—"}</span>
              </div>
            );
          })}
        </div>

        {revealed && (
          <div className="mt-4 rounded-2xl bg-white/70 p-3 text-center text-xs italic text-[#5A3220]">
            {reference[language] || "(no reference yet)"}
          </div>
        )}
      </div>
    </div>
  );
}
