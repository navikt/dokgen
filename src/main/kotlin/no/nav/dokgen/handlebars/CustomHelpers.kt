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
}