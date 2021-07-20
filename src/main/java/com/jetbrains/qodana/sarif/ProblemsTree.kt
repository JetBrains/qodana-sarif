package com.jetbrains.qodana.sarif

import com.jetbrains.qodana.sarif.model.SarifReport
import com.jetbrains.qodana.sarif.model.Result

class ProblemsTree(val report: SarifReport) {
    class Node(val path: String, var size: Int, val results: MutableList<Result>) {
        val children = mutableListOf<Node>()
        fun addResult(path: List<String>, result: Result) {
            size++
            if (path.isEmpty()) {
                results.add(result)
            } else {
                val child = path[0]
                val childNode = getChild(child)
                childNode.addResult(path.subList(1, path.size), result)
            }
        }

        private fun getChild(element: String): Node {
            children.find { it.path == element }?.let { return it }
            val node = Node(element, 0, mutableListOf())
            children.add(node)
            return node
        }
    }

    private val root: Node = Node("", 0 , mutableListOf())
    init {
        val results = report.runs[0].results ?: throw IllegalStateException()
        results.forEach { result ->
            val path = result.locations[0]?.physicalLocation?.artifactLocation?.uri?.split("/") ?: emptyList()
            root.addResult(path, result)
        }
    }


    fun printTree() {
        printTree("", root)
    }

    private fun printTree(indent: String = "", node: Node) {
        println("${indent}Path:${node.path}, Size: ${node.size}")
        node.children.sortBy { it.path }
        node.children.forEach { printTree("$indent    ", it) }
    }
}