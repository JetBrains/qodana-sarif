package com.jetbrains.qodana.sarif

import com.google.gson.annotations.SerializedName

data class MetaInformation(
    @SerializedName("total")
    var totalProblem: Int = 0,
    @SerializedName("tools inspection")
    var toolsInspection: Map<String, Int> = emptyMap(),
    var attributes: Map<String, Any>? = null
)
