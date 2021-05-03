package no.nav.dokgen.handlebars

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options
import java.io.IOException
import java.util.*

interface CustomHelpers {
    class SwitchHelper() : Helper<Any> {

        @Throws(IOException::class)
        override fun apply(variable: Any, options: Options): Any {
            val variabelNavn: MutableList<String> = ArrayList()
            val variabelVerdier: MutableList<Any> = ArrayList()
            variabelNavn.add("__condition_fulfilled")
            variabelVerdier.add(0)
            variabelNavn.add("__condition_variable")
            variabelVerdier.add(if (options.hash.isEmpty()) variable else options.hash)
            val ctx = Context.newBlockParamContext(options.context, variabelNavn, variabelVerdier)
            val resultat: String = options.fn.apply(ctx)

            val antall = ctx["__condition_fulfilled"] as Int
            if (Integer.valueOf(1) == antall) {
                return resultat
            }
            throw IllegalArgumentException("Switch-case må treffe i 1 case, men traff i " + antall + " med verdien " + ctx["__condition_variable"])
        }
    }

    class CaseHelper() : Helper<Any> {

        @Throws(IOException::class)
        override fun apply(caseKonstant: Any, options: Options): Any {
            val konstant: Any = if (options.hash.isEmpty()) caseKonstant else options.hash
            val model = options.context.model() as MutableMap<String, Any>
            val condition_variable = model["__condition_variable"]
            if (konstant == condition_variable) {
                val antall = model["__condition_fulfilled"] as Int?
                if (antall != null) {
                    val nyAntall = antall.inc()
                    model.put("__condition_fulfilled", nyAntall)
                }
                return options.fn()
            }
            return options.inverse()
        }
    }


    /**
     * Allows to create a table with a set number of columns from only td cells
     * Useful if you have to render table cells but you don't know how many cells you will have.
     *
     * Example:
     * {{#table [columns=2] [class=""]]}}
     *      <td>This is one cell</td>
     *      <td>This is another cell</td>
     *      <td>This is a third cell</td>
     * {{/table}}
     *
     * will render a table with two tr rows with two cells in each
     *
     * +----------------------+----------------------+
     * | This is one cell     | This is another cell |
     * +----------------------+----------------------+
     * | This is a third cell |                      |
     * +----------------------+----------------------+
     *
     * Only supports td elements, if you want th you can give the header cells their own css classes.
     * You can also supply an optional class parameter to the helper which will be added to the table.
     *
     */
    class TableHelper() : Helper<Any> {

        override fun apply(context: Any, options: Options): Any {
            val columnCount = options.hash<Int>("columns", 2)
            val tableContents = options.fn(context)
            val cells = tableContents.trim()
                .split("</td>")
                .filter { it.isNotEmpty() }
                .map { "$it</td>" }

            val wrappedInRows = mutableListOf("<tr>")
            cells.forEachIndexed { index, cell ->
                run {
                    if (index > 0 && index % columnCount == 0) {
                        wrappedInRows += "</tr><tr>"
                    }
                    wrappedInRows += cell
                }
            }
            wrappedInRows += "</tr>"

            val classParam = options.hash<String>("class", "")
            val classString = if (classParam.isNotEmpty()) "class=$classParam" else ""
            return "<table ${classString}>${wrappedInRows.joinToString("")}</table>"
        }
    }

    /**
     * Allows simple addition inside
     */
    class AdditionHelper(): Helper<Int> {
        override fun apply(leftOperand: Int, options: Options): Any {
            return leftOperand + options.param<Int>(0)
        }
    }
}