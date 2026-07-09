package com.jetbrains.qodana.sarif.baseline

import com.jetbrains.qodana.sarif.model.Run

internal class DescriptorLookup(private val run: Run) {
    /**
     * One-time snapshot index so repeated hits are O(1) instead of re-scanning every rule.
     * First-wins ([MutableMap.putIfAbsent]) reproduces the previous `findFirst()` semantics over the descriptors.
     */
    private val index: MutableMap<String?, DescriptorWithLocation> by lazy(LazyThreadSafetyMode.NONE) {
        HashMap<String?, DescriptorWithLocation>().apply {
            descriptors().forEach { putIfAbsent(it.descriptor.id, it) }
        }
    }

    fun findById(ruleId: String?): DescriptorWithLocation? {
        index[ruleId]?.let { return it }

        // A miss re-scans live: new descriptors might be added between calls (but never removed), so a
        // not-found result must not be cached as absent.
        val found = descriptors().firstOrNull { it.descriptor.id == ruleId } ?: return null
        index[ruleId] = found
        return found
    }

    private fun descriptors(): Sequence<DescriptorWithLocation> {
        val tool = run.tool
        val driverRules = tool?.driver?.let { driver ->
            driver.rules.orEmpty().asSequence().map { DescriptorWithLocation(it, driver) }
        } ?: emptySequence()
        val extensionRules = tool?.extensions.orEmpty().asSequence().flatMap { extension ->
            extension.rules.orEmpty().asSequence().map { DescriptorWithLocation(it, extension) }
        }
        return driverRules + extensionRules
    }
}
