package com.jetbrains.qodana.sarif.model

interface PropertyOwner {
    var properties: PropertyBag?

    fun updateProperties(mutator: (PropertyBag) -> Unit) {
        val props = properties ?: PropertyBag()
        mutator(props)
        properties = props
    }
}

@Suppress("unused")
fun <T: PropertyOwner> T.withUpdatedProperties(mutator: (PropertyBag) -> Unit): T =
    apply { updateProperties(mutator) }
