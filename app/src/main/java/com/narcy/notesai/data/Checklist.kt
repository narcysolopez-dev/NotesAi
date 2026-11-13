package com.narcy.notesai.data

/** Elemento de una nota de lista. Se guarda en `Note.content`, una línea por elemento. */
data class CheckItem(val text: String, val checked: Boolean = false)

private const val DONE = "[x] "
private const val TODO = "[ ] "

fun parseChecklist(content: String): List<CheckItem> =
    content.lines()
        .filter { it.isNotBlank() }
        .map { line ->
            when {
                line.startsWith(DONE) -> CheckItem(line.removePrefix(DONE), checked = true)
                line.startsWith(TODO) -> CheckItem(line.removePrefix(TODO), checked = false)
                else -> CheckItem(line, checked = false)
            }
        }

fun serializeChecklist(items: List<CheckItem>): String =
    items.filter { it.text.isNotBlank() }
        .joinToString("\n") { (if (it.checked) DONE else TODO) + it.text.replace('\n', ' ') }

/** Texto plano para compartir. */
fun checklistToPlainText(content: String): String =
    parseChecklist(content).joinToString("\n") { (if (it.checked) "☑ " else "☐ ") + it.text }
