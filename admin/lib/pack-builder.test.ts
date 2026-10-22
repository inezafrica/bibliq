import { describe, expect, it } from "vitest";
import { buildManifest, buildPack, LevelRow, QuestionRow, serializePack, sha256Of } from "./pack-builder";
import { Manifest } from "./content-types";

function question(id: string, overrides: Partial<QuestionRow> = {}): QuestionRow {
  return {
    id,
    type: "MCQ",
    difficulty: 1,
    text: { en: `Text ${id}`, fr: `Texte ${id}`, es: `Texto ${id}` },
    options: {
      en: ["A", "B", "C", "D"],
      fr: ["A", "B", "C", "D"],
      es: ["A", "B", "C", "D"],
    },
    correctIndex: 0,
    imageKey: null,
    reference: { en: "Genesis 1:1", fr: "Genèse 1:1", es: "Génesis 1:1" },
    ...overrides,
  };
}

function level(number: number, questionIds: string[], overrides: Partial<LevelRow> = {}): LevelRow {
  return { number, world: 1, isBoss: false, starThresholds: [7, 9, 10], questionIds, ...overrides };
}

describe("buildPack", () => {
  it("includes only the questions referenced by the given levels, in order", () => {
    const questions = [question("q_0001"), question("q_0002"), question("q_0003")];
    const levels = [level(1, ["q_0001", "q_0002"])];
    const pack = buildPack(1, levels, questions);

    expect(pack.packId).toBe(1);
    expect(pack.questions.map((q) => q.id)).toEqual(["q_0001", "q_0002"]);
    expect(pack.levels).toHaveLength(1);
  });

  it("does not duplicate a question shared by two levels in the same pack", () => {
    const questions = [question("q_0001"), question("q_0002")];
    const levels = [level(1, ["q_0001"]), level(2, ["q_0001", "q_0002"])];
    const pack = buildPack(1, levels, questions);

    expect(pack.questions.map((q) => q.id)).toEqual(["q_0001", "q_0002"]);
  });

  it("omits imageUrl for non-IMAGE questions and includes it for IMAGE questions", () => {
    const img = question("q_0001", { type: "IMAGE", imageKey: "images/q_0001_v1.webp" });
    const mcq = question("q_0002");
    const pack = buildPack(1, [level(1, ["q_0001", "q_0002"])], [img, mcq]);

    expect(pack.questions[0].imageUrl).toBe("images/q_0001_v1.webp");
    expect(pack.questions[1].imageUrl).toBeUndefined();
  });

  it("sorts levels by number regardless of input order", () => {
    const questions = [question("q_0001"), question("q_0002")];
    const levels = [level(2, ["q_0002"]), level(1, ["q_0001"])];
    const pack = buildPack(1, levels, questions);

    expect(pack.levels.map((l) => l.number)).toEqual([1, 2]);
  });

  it("throws when a level references a question id that doesn't exist", () => {
    const levels = [level(1, ["q_missing"])];
    expect(() => buildPack(1, levels, [])).toThrow(/q_missing/);
  });
});

describe("sha256Of / serializePack", () => {
  it("produces a stable hash for the same content", () => {
    const pack = buildPack(1, [level(1, ["q_0001"])], [question("q_0001")]);
    const json = serializePack(pack);
    expect(sha256Of(json)).toBe(sha256Of(json));
    expect(sha256Of(json)).toHaveLength(64);
  });

  it("produces a different hash when content changes", () => {
    const packA = buildPack(1, [level(1, ["q_0001"])], [question("q_0001")]);
    const packB = buildPack(1, [level(1, ["q_0001"])], [question("q_0001", { difficulty: 2 })]);
    expect(sha256Of(serializePack(packA))).not.toBe(sha256Of(serializePack(packB)));
  });
});

describe("buildManifest", () => {
  it("creates the first manifest when there is no previous one", () => {
    const manifest = buildManifest({
      previousManifest: null,
      packId: 1,
      packFile: "packs/pack_001_v1.json",
      packVersion: 1,
      levelRange: [1, 15],
      packSha256: "abc123",
      nextDropDate: "2026-10-16",
    });

    expect(manifest.packs).toEqual([
      { id: 1, file: "packs/pack_001_v1.json", version: 1, levels: [1, 15], sha256: "abc123" },
    ]);
    expect(manifest.latestLevel).toBe(15);
    expect(manifest.schemaVersion).toBe(1);
  });

  it("appends a new pack without touching earlier ones", () => {
    const previous: Manifest = {
      schemaVersion: 1,
      minAppVersion: 1,
      latestLevel: 15,
      nextDropDate: "2026-10-16",
      packs: [{ id: 1, file: "packs/pack_001_v1.json", version: 1, levels: [1, 15], sha256: "abc123" }],
    };
    const manifest = buildManifest({
      previousManifest: previous,
      packId: 2,
      packFile: "packs/pack_002_v1.json",
      packVersion: 1,
      levelRange: [16, 30],
      packSha256: "def456",
      nextDropDate: "2026-10-23",
    });

    expect(manifest.packs).toHaveLength(2);
    expect(manifest.packs[0]).toEqual(previous.packs[0]);
    expect(manifest.latestLevel).toBe(30);
    expect(manifest.nextDropDate).toBe("2026-10-23");
  });

  it("bumps the version in place when republishing the same pack id", () => {
    const previous: Manifest = {
      schemaVersion: 1,
      minAppVersion: 1,
      latestLevel: 15,
      nextDropDate: "2026-10-16",
      packs: [{ id: 1, file: "packs/pack_001_v1.json", version: 1, levels: [1, 15], sha256: "abc123" }],
    };
    const manifest = buildManifest({
      previousManifest: previous,
      packId: 1,
      packFile: "packs/pack_001_v2.json",
      packVersion: 2,
      levelRange: [1, 15],
      packSha256: "newhash",
      nextDropDate: "2026-10-16",
    });

    expect(manifest.packs).toHaveLength(1);
    expect(manifest.packs[0]).toEqual({
      id: 1,
      file: "packs/pack_001_v2.json",
      version: 2,
      levels: [1, 15],
      sha256: "newhash",
    });
  });

  it("carries schemaVersion and minAppVersion forward when not overridden", () => {
    const previous: Manifest = {
      schemaVersion: 2,
      minAppVersion: 3,
      latestLevel: 15,
      nextDropDate: "2026-10-16",
      packs: [{ id: 1, file: "packs/pack_001_v1.json", version: 1, levels: [1, 15], sha256: "abc123" }],
    };
    const manifest = buildManifest({
      previousManifest: previous,
      packId: 2,
      packFile: "packs/pack_002_v1.json",
      packVersion: 1,
      levelRange: [16, 30],
      packSha256: "def456",
      nextDropDate: "2026-10-23",
    });

    expect(manifest.schemaVersion).toBe(2);
    expect(manifest.minAppVersion).toBe(3);
  });
});
