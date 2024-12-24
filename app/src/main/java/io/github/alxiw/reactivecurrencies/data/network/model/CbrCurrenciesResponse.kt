package io.github.alxiw.reactivecurrencies.data.network.model

import io.github.alxiw.reactivecurrencies.data.network.model.CbrCurrenciesResponse.Currency
import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

typealias CurrencyList = ArrayList<Currency>

@Root(name = "ValCurs")
class CbrCurrenciesResponse() {

    @field:Attribute(name = "Date", required = true)
    var date: String? = null

    @field:Attribute(name = "name", required = false)
    var name: String? = null

    @field:ElementList(inline = true)
    var list: CurrencyList? = null

    @Root(name="Valute")
    class Currency() {

        @field:Attribute(name = "ID", required = false)
        var id: String? = null

        @field:Element(name = "NumCode", required = false)
        var numCode: String? = null

        @field:Element(name = "CharCode", required = true)
        var charCode: String? = null

        @field:Element(name = "Nominal", required = false)
        var nominal: String? = null

        @field:Element(name = "Name", required = false)
        var name: String? = null

        @field:Element(name = "Value", required = false)
        var value: String? = null

        @field:Element(name = "VunitRate", required = true)
        var rate: String? = null
    }
}
