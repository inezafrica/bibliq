package com.yourname.bibliq.data.model

/**
 * Catholic Bible canon (73 books: 46 Old Testament + 27 New Testament).
 * Includes the 7 deuterocanonical books: Tobit, Judith, Wisdom of Solomon,
 * Sirach (Ecclesiasticus), Baruch, 1 Maccabees, 2 Maccabees.
 *
 * Localized book names for English (en), French (fr), and Spanish (es).
 */
data class BibleBook(
    val id: String,
    val names: Map<String, String>,
    val testament: Testament,
    val isDeuterocanonical: Boolean = false
)

enum class Testament { OLD, NEW }

object CatholicBibleBooks {

    val books: List<BibleBook> = listOf(
        // ===== Old Testament (46 books) =====
        // Pentateuch
        BibleBook("genesis", mapOf("en" to "Genesis", "fr" to "Genèse", "es" to "Génesis"), Testament.OLD),
        BibleBook("exodus", mapOf("en" to "Exodus", "fr" to "Exode", "es" to "Éxodo"), Testament.OLD),
        BibleBook("leviticus", mapOf("en" to "Leviticus", "fr" to "Lévitique", "es" to "Levítico"), Testament.OLD),
        BibleBook("numbers", mapOf("en" to "Numbers", "fr" to "Nombres", "es" to "Números"), Testament.OLD),
        BibleBook("deuteronomy", mapOf("en" to "Deuteronomy", "fr" to "Deutéronome", "es" to "Deuteronomio"), Testament.OLD),

        // Historical books
        BibleBook("joshua", mapOf("en" to "Joshua", "fr" to "Josué", "es" to "Josué"), Testament.OLD),
        BibleBook("judges", mapOf("en" to "Judges", "fr" to "Juges", "es" to "Jueces"), Testament.OLD),
        BibleBook("ruth", mapOf("en" to "Ruth", "fr" to "Ruth", "es" to "Rut"), Testament.OLD),
        BibleBook("1_samuel", mapOf("en" to "1 Samuel", "fr" to "1 Samuel", "es" to "1 Samuel"), Testament.OLD),
        BibleBook("2_samuel", mapOf("en" to "2 Samuel", "fr" to "2 Samuel", "es" to "2 Samuel"), Testament.OLD),
        BibleBook("1_kings", mapOf("en" to "1 Kings", "fr" to "1 Rois", "es" to "1 Reyes"), Testament.OLD),
        BibleBook("2_kings", mapOf("en" to "2 Kings", "fr" to "2 Rois", "es" to "2 Reyes"), Testament.OLD),
        BibleBook("1_chronicles", mapOf("en" to "1 Chronicles", "fr" to "1 Chroniques", "es" to "1 Crónicas"), Testament.OLD),
        BibleBook("2_chronicles", mapOf("en" to "2 Chronicles", "fr" to "2 Chroniques", "es" to "2 Crónicas"), Testament.OLD),
        BibleBook("ezra", mapOf("en" to "Ezra", "fr" to "Esdras", "es" to "Esdras"), Testament.OLD),
        BibleBook("nehemiah", mapOf("en" to "Nehemiah", "fr" to "Néhémie", "es" to "Nehemías"), Testament.OLD),
        // --- Deuterocanonical ---
        BibleBook("tobit", mapOf("en" to "Tobit", "fr" to "Tobie", "es" to "Tobías"), Testament.OLD, isDeuterocanonical = true),
        BibleBook("judith", mapOf("en" to "Judith", "fr" to "Judith", "es" to "Judit"), Testament.OLD, isDeuterocanonical = true),
        // ---
        BibleBook("esther", mapOf("en" to "Esther", "fr" to "Esther", "es" to "Ester"), Testament.OLD),
        // --- Deuterocanonical ---
        BibleBook("1_maccabees", mapOf("en" to "1 Maccabees", "fr" to "1 Maccabées", "es" to "1 Macabeos"), Testament.OLD, isDeuterocanonical = true),
        BibleBook("2_maccabees", mapOf("en" to "2 Maccabees", "fr" to "2 Maccabées", "es" to "2 Macabeos"), Testament.OLD, isDeuterocanonical = true),
        // ---

        // Wisdom books
        BibleBook("job", mapOf("en" to "Job", "fr" to "Job", "es" to "Job"), Testament.OLD),
        BibleBook("psalms", mapOf("en" to "Psalms", "fr" to "Psaumes", "es" to "Salmos"), Testament.OLD),
        BibleBook("proverbs", mapOf("en" to "Proverbs", "fr" to "Proverbes", "es" to "Proverbios"), Testament.OLD),
        BibleBook("ecclesiastes", mapOf("en" to "Ecclesiastes", "fr" to "Ecclésiaste", "es" to "Eclesiastés"), Testament.OLD),
        BibleBook("song_of_songs", mapOf("en" to "Song of Songs", "fr" to "Cantique des Cantiques", "es" to "Cantar de los Cantares"), Testament.OLD),
        // --- Deuterocanonical ---
        BibleBook("wisdom", mapOf("en" to "Wisdom of Solomon", "fr" to "Sagesse", "es" to "Sabiduría"), Testament.OLD, isDeuterocanonical = true),
        BibleBook("sirach", mapOf("en" to "Sirach (Ecclesiasticus)", "fr" to "Siracide (Ecclésiastique)", "es" to "Sirácida (Eclesiastés)"), Testament.OLD, isDeuterocanonical = true),
        // ---

        // Prophets
        BibleBook("isaiah", mapOf("en" to "Isaiah", "fr" to "Ésaïe", "es" to "Isaías"), Testament.OLD),
        BibleBook("jeremiah", mapOf("en" to "Jeremiah", "fr" to "Jérémie", "es" to "Jeremías"), Testament.OLD),
        BibleBook("lamentations", mapOf("en" to "Lamentations", "fr" to "Lamentations", "es" to "Lamentaciones"), Testament.OLD),
        // --- Deuterocanonical ---
        BibleBook("baruch", mapOf("en" to "Baruch", "fr" to "Baruch", "es" to "Baruc"), Testament.OLD, isDeuterocanonical = true),
        // ---
        BibleBook("ezekiel", mapOf("en" to "Ezekiel", "fr" to "Ézéchiel", "es" to "Ezequiel"), Testament.OLD),
        BibleBook("daniel", mapOf("en" to "Daniel", "fr" to "Daniel", "es" to "Daniel"), Testament.OLD),
        BibleBook("hosea", mapOf("en" to "Hosea", "fr" to "Osée", "es" to "Oseas"), Testament.OLD),
        BibleBook("joel", mapOf("en" to "Joel", "fr" to "Joël", "es" to "Joel"), Testament.OLD),
        BibleBook("amos", mapOf("en" to "Amos", "fr" to "Amos", "es" to "Amós"), Testament.OLD),
        BibleBook("obadiah", mapOf("en" to "Obadiah", "fr" to "Abdias", "es" to "Abdías"), Testament.OLD),
        BibleBook("jonah", mapOf("en" to "Jonah", "fr" to "Jonas", "es" to "Jonás"), Testament.OLD),
        BibleBook("micah", mapOf("en" to "Micah", "fr" to "Michée", "es" to "Miqueas"), Testament.OLD),
        BibleBook("nahum", mapOf("en" to "Nahum", "fr" to "Nahum", "es" to "Nahúm"), Testament.OLD),
        BibleBook("habakkuk", mapOf("en" to "Habakkuk", "fr" to "Habacuc", "es" to "Habacuc"), Testament.OLD),
        BibleBook("zephaniah", mapOf("en" to "Zephaniah", "fr" to "Sophonie", "es" to "Sofonías"), Testament.OLD),
        BibleBook("haggai", mapOf("en" to "Haggai", "fr" to "Aggée", "es" to "Hageo"), Testament.OLD),
        BibleBook("zechariah", mapOf("en" to "Zechariah", "fr" to "Zacharie", "es" to "Zacarías"), Testament.OLD),
        BibleBook("malachi", mapOf("en" to "Malachi", "fr" to "Malachie", "es" to "Malaquías"), Testament.OLD),

        // ===== New Testament (27 books) =====
        BibleBook("matthew", mapOf("en" to "Matthew", "fr" to "Matthieu", "es" to "Mateo"), Testament.NEW),
        BibleBook("mark", mapOf("en" to "Mark", "fr" to "Marc", "es" to "Marcos"), Testament.NEW),
        BibleBook("luke", mapOf("en" to "Luke", "fr" to "Luc", "es" to "Lucas"), Testament.NEW),
        BibleBook("john", mapOf("en" to "John", "fr" to "Jean", "es" to "Juan"), Testament.NEW),
        BibleBook("acts", mapOf("en" to "Acts", "fr" to "Actes", "es" to "Hechos"), Testament.NEW),
        BibleBook("romans", mapOf("en" to "Romans", "fr" to "Romains", "es" to "Romanos"), Testament.NEW),
        BibleBook("1_corinthians", mapOf("en" to "1 Corinthians", "fr" to "1 Corinthiens", "es" to "1 Corintios"), Testament.NEW),
        BibleBook("2_corinthians", mapOf("en" to "2 Corinthians", "fr" to "2 Corinthiens", "es" to "2 Corintios"), Testament.NEW),
        BibleBook("galatians", mapOf("en" to "Galatians", "fr" to "Galates", "es" to "Gálatas"), Testament.NEW),
        BibleBook("ephesians", mapOf("en" to "Ephesians", "fr" to "Éphésiens", "es" to "Efesios"), Testament.NEW),
        BibleBook("philippians", mapOf("en" to "Philippians", "fr" to "Philippiens", "es" to "Filipenses"), Testament.NEW),
        BibleBook("colossians", mapOf("en" to "Colossians", "fr" to "Colossiens", "es" to "Colosenses"), Testament.NEW),
        BibleBook("1_thessalonians", mapOf("en" to "1 Thessalonians", "fr" to "1 Thessaloniciens", "es" to "1 Tesalonicenses"), Testament.NEW),
        BibleBook("2_thessalonians", mapOf("en" to "2 Thessalonians", "fr" to "2 Thessaloniciens", "es" to "2 Tesalonicenses"), Testament.NEW),
        BibleBook("1_timothy", mapOf("en" to "1 Timothy", "fr" to "1 Timothée", "es" to "1 Timoteo"), Testament.NEW),
        BibleBook("2_timothy", mapOf("en" to "2 Timothy", "fr" to "2 Timothée", "es" to "2 Timoteo"), Testament.NEW),
        BibleBook("titus", mapOf("en" to "Titus", "fr" to "Tite", "es" to "Tito"), Testament.NEW),
        BibleBook("philemon", mapOf("en" to "Philemon", "fr" to "Philémon", "es" to "Filemón"), Testament.NEW),
        BibleBook("hebrews", mapOf("en" to "Hebrews", "fr" to "Hébreux", "es" to "Hebreos"), Testament.NEW),
        BibleBook("james", mapOf("en" to "James", "fr" to "Jacques", "es" to "Santiago"), Testament.NEW),
        BibleBook("1_peter", mapOf("en" to "1 Peter", "fr" to "1 Pierre", "es" to "1 Pedro"), Testament.NEW),
        BibleBook("2_peter", mapOf("en" to "2 Peter", "fr" to "2 Pierre", "es" to "2 Pedro"), Testament.NEW),
        BibleBook("1_john", mapOf("en" to "1 John", "fr" to "1 Jean", "es" to "1 Juan"), Testament.NEW),
        BibleBook("2_john", mapOf("en" to "2 John", "fr" to "2 Jean", "es" to "2 Juan"), Testament.NEW),
        BibleBook("3_john", mapOf("en" to "3 John", "fr" to "3 Jean", "es" to "3 Juan"), Testament.NEW),
        BibleBook("jude", mapOf("en" to "Jude", "fr" to "Jude", "es" to "Judas"), Testament.NEW),
        BibleBook("revelation", mapOf("en" to "Revelation", "fr" to "Apocalypse", "es" to "Apocalipsis"), Testament.NEW)
    )

    private val bookById: Map<String, BibleBook> = books.associateBy { it.id }

    fun getBookById(id: String): BibleBook? = bookById[id]

    fun getName(id: String, language: String = FALLBACK_LANGUAGE): String? =
        bookById[id]?.names?.get(language)

    fun oldTestament(): List<BibleBook> = books.filter { it.testament == Testament.OLD }

    fun newTestament(): List<BibleBook> = books.filter { it.testament == Testament.NEW }

    fun deuterocanonical(): List<BibleBook> = books.filter { it.isDeuterocanonical }

    fun totalCount(): Int = books.size
}
