package no.nav.dokgen.handlebars

import com.fasterxml.jackson.databind.node.ArrayNode
import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*

interface CustomHelpers {

    /**
     * Allows using switch/case in hbs templates
     *
     * Syntax:
     *  {{#switch variable}}
     *      {{#case "SOME_VALUE"}}
     *          <p>Content that should display if variable=="SOME_VALUE"</p>
     *      {{/case}}
     *      {{#case "SOME_OTHER_VALUE"}}
     *          <p>Content that should display if variable=="SOME_OTHER_VALUE"</p>
     *      {{/case}}
     *  {{/switch}}
     */
    class SwitchHelper() : Helper<Any> {

        @Throws(IOException::class)
        override fun apply(variable: Any, options: Options): Any? {
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
            return null
        }
    }

    /**
     * @see SwitchHelper
     */
    class CaseHelper() : Helper<Any> {
        private val CONDITION_VARIABLE = "__condition_variable"
        private val CONDITION_FULFILLED = "__condition_fulfilled"

        @Throws(IOException::class)
        override fun apply(caseKonstant: Any, options: Options): Any? {
            val konstant = if (options.hash.isEmpty()) caseKonstant else options.hash
            val model = options.context.model() as MutableMap<String, Any>
            val condition_variable = model[CONDITION_VARIABLE]
            if (caseKonstant is Iterable<*>) {
                if ((caseKonstant as List<*>).contains(condition_variable)) {
                    incrementConditionFulfilledCounter(model)
                    return options.fn()
                }
            } else if (konstant == condition_variable) {
                incrementConditionFulfilledCounter(model)
                return options.fn()
            }
            return options.inverse()
        }

        private fun incrementConditionFulfilledCounter(model: MutableMap<String, Any>) {
            var antall = model[CONDITION_FULFILLED] as Int
            model[CONDITION_FULFILLED] = ++antall
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

            if (cells.count() % columnCount > 0) {
                // If there are fewer cells than columns, the cells will stretch unless we do this
                val missingCellsInLastRow = columnCount - (cells.count() % columnCount)
                wrappedInRows += "<td></td>".repeat(missingCellsInLastRow)
            }

            wrappedInRows += "</tr>"

            val classParam = options.hash<String>("class", "")
            val classString = if (classParam.isNotEmpty()) "class=$classParam" else ""
            return "<table ${classString}>${wrappedInRows.joinToString("")}</table>"
        }
    }

    /**
     * Allows simple addition inside a template
     *
     * {{add 3 4}} prints 7 for example. Mostly useful when printing index
     * in loops. For example:
     * {{#each context.questions as | question |}}
     *     {{add @index 1}}. {{question.prompt}}
     * {{/each}}
     *
     * Will print every question prompt in an array of unknown size along with its index incremented by 1
     */
    class AdditionHelper(): Helper<Int> {
        override fun apply(leftOperand: Int, options: Options): Any {
            return leftOperand + options.param<Int>(0)
        }
    }

    /**
     * Allows converting ISO8601 to be formatted as a norwegian dd.mm.yyyy string
     *
     * {{norwegian-date 2020-02-01}} prints 01.02.2020
     */
    class NorwegianDateHelper(): Helper<String> {
        override fun apply(isoFormattedDate: String, options: Options): Any {
            return isoFormattedDate.split('-').reversed().joinToString(separator = ".")
        }
    }

    /**
     * Parses ISO-8601 extended local or offset date-time format, and returns a string in dd.mm.yyyy HH:mm format
     *
     * {{norwegian-datetime 2019-08-19T15:54:01}} prints 19.08.2019 15:54
     */
    class NorwegianDateTimeHelper(): Helper<String> {
        companion object {
            val datetimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        }
        override fun apply(isoFormattedDateTime: String, options: Options): Any {
            return datetimeFormat.format(DateTimeFormatter.ISO_DATE_TIME.parse(isoFormattedDateTime))
        }
    }

    /**
     * Allows to divide a template parameter with a given number and round the resultvalue to the nearest krone
     */
    class DivideHelper() : Helper<Int> {
        override fun apply(antall: Int, options: Options): Any {
            val beløp = options.param<Int>(0)

            val value = BigDecimal.valueOf(beløp.toLong()).divide(BigDecimal.valueOf(antall.toLong()), 0, RoundingMode.HALF_UP)

            return value.toInt()
        }
    }

    /**
     * Format an int with thousand seperator, ex: 10000 will be 10 000
     */
    @Deprecated("Do not use, use ThousandSeperatorHelper.class instead")
    class FormatKronerHelper : Helper<Int> {
        override fun apply(kroner: Int, options: Options?): Any {
            val formatter = NumberFormat.getInstance(Locale.US) as DecimalFormat
            val symbols = formatter.decimalFormatSymbols
            symbols.groupingSeparator = ' '
            formatter.decimalFormatSymbols = symbols
            return formatter.format(kroner)
        }
    }

    /**
     * Format an int with thousand seperator and make the space not breakable to prevent line breaks within an amount,
     * ex: 10000 will be 10 000
     * The function does not handle decimals
     */
    class ThousandSeperatorHelper : Helper<Int> {
        override fun apply(kroner: Int, options: Options?): Any {
            return String.format(Locale.forLanguageTag("NO"), "%,d", kroner)
        }
    }

    /**
     * Removes trailing zeroes from decimals, ex: 10.0 becomes 10 and 90.20 becomes 90.2, while 100.3 remains the same
     */
    class TrimDecimalHelper : Helper<Double> {
        override fun apply(decimal: Double, options: Options?): Any {
            return BigDecimal.valueOf(decimal).stripTrailingZeros().toPlainString()
        }
    }

    /**
     * Block helper that gives a possibility to define a
     * array in a handlebars template. It can be used as
     * an input parameter to in-array function.
     *
     * ```handlebars
     * <!-- array: ['a', 'b', 'c'] -->
     * {{#in-array (array 'a', 'b', 'c') 'd'}}
     *   foo
     * {{else}}
     *   bar
     * {{/in-array}}
     * <!-- results in: 'bar' -->
     * ```
     */
    class ArrayHelper : Helper<Any> {
        @Throws(IOException::class)
        override fun apply(verdi: Any, options: Options): Any {
            val verdier: MutableList<Any> = ArrayList()
            if (options.hash.isEmpty()) {
                verdier.add(verdi)
                verdier.addAll(Arrays.asList(*options.params))
            } else {
                verdier.add(options.hash)
            }
            return verdier
        }
    }

    /**
     * Block helper that renders the block if an caseArray has the
     * given `value`. Optionally specify an inverse block to render
     * when the caseArray does not have the given value.
     *
     * ```handlebars
     * <!-- caseArray: ['a', 'b', 'c'] -->
     * {{#in-array caseArray "d"}}
     *   foo
     * {{else}}
     *   bar
     * {{/in-array}}
     * <!-- results in: 'bar' -->
     * ```
     */
    class InArrayHelper : Helper<Any?> {
        @Throws(IOException::class)
        override fun apply(caseArray: Any?, options: Options): Any {
            if (caseArray is Iterable<*>) {
                val param = options.params[0]
                if ((caseArray as List<*>).contains(param)) {
                    return options.fn()
                }
            }
            return options.inverse()
        }
    }

    /**
     * Returns the size of an array
     */
    class SizeHelper : Helper<Any> {
        override fun apply(o: Any?, options: Options?): Any {
            return if (o is ArrayNode) {
                o.size()
            } else 0
        }
    }
}