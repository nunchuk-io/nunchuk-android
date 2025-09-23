package com.nunchuk.android.compose.miniscript

import com.nunchuk.android.core.miniscript.ScriptNodeType
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.SigningPath

/**
 * Result of a path selection operation including the new signing path and disabled paths
 */
data class PathSelectionResult(
    val signingPath: SigningPath,
    val disabledPaths: Set<List<Int>>,
    val subNodeFollowParent: Set<List<Int>>,
)


/**
 * Creates a new SigningPath with the given node ID added or removed based on selection
 * and the parent node type. Also calculates which paths should be disabled.
 */
fun updateSigningPathForNode(
    currentPath: SigningPath,
    currentDisabledPaths: Set<List<Int>>,
    nodeId: List<Int>,
    isSelected: Boolean,
    currentNode: ScriptNode? = null,
    parentNode: ScriptNode?,
): PathSelectionResult {
    val currentPaths = currentPath.path.toMutableList()
    val disabledPaths = currentDisabledPaths.toMutableSet()
    val subNodeFollowParent = mutableSetOf<List<Int>>()

    when {
        isSelected -> {
            // Add the node to the path
            when (parentNode?.type) {
                ScriptNodeType.OR.name, ScriptNodeType.OR_TAPROOT.name, ScriptNodeType.ANDOR.name -> {
                    // For OR/ANDOR nodes: only one child can be selected at a time
                    val siblingNodes = getSiblingNodes(parentNode)

                    // Remove all sibling paths from current selection
                    siblingNodes.forEach { siblingId ->
                        currentPaths.removeAll { path ->
                            path.size >= siblingId.size && path.take(siblingId.size) == siblingId
                        }
                    }

                    // Add sibling paths to disabled paths (disable the other options)
                    disabledPaths.addAll(siblingNodes)

                    // Add the selected node
                    if (!currentPaths.contains(nodeId)) {
                        currentPaths.add(nodeId)
                    }

                    // Remove the selected node from disabled paths if it was there
                    disabledPaths.removeAll { disabled ->
                        nodeId.size >= disabled.size && nodeId.take(disabled.size) == disabled
                    }
                }

                ScriptNodeType.AND.name -> {
                    parentNode.subs.forEach { childNode ->
                        if (!currentPaths.contains(childNode.id)) {
                            currentPaths.add(childNode.id)
                        }
                        subNodeFollowParent.add(childNode.id)
                    }
                }

                ScriptNodeType.THRESH.name -> {
                    // For THRESH nodes, we can add the node directly
                    if (!currentPaths.contains(nodeId)) {
                        currentPaths.add(nodeId)
                    }
                    val selectCount = parentNode.subs.count { childNode ->
                        currentPaths.any { path ->
                            path.size >= childNode.id.size && path.take(childNode.id.size) == childNode.id
                        }
                    }
                    if (selectCount == parentNode.k) {
                        // If we've reached the threshold, disable other siblings
                        val siblingNodes = getSiblingNodes(parentNode)
                        siblingNodes.forEach { siblingId ->
                            if (!currentPaths.any { path -> path == siblingId }) {
                                disabledPaths.add(siblingId)
                            }
                        }
                    }
                }

                else -> {
                    if (!currentPaths.contains(nodeId)) {
                        currentPaths.add(nodeId)
                    }
                }
            }
        }

        else -> {
            // Remove the node and all its descendants from the path
            currentPaths.removeAll { path ->
                path.size >= nodeId.size && path.take(nodeId.size) == nodeId
            }

            // For OR/ANDOR nodes: if deselecting, re-enable sibling paths
            when (parentNode?.type) {
                ScriptNodeType.OR.name, ScriptNodeType.OR_TAPROOT.name, ScriptNodeType.ANDOR.name -> {
                    val siblingNodes = getSiblingNodes(parentNode)
                    // Check if any sibling is still selected
                    val hasSiblingSelected = siblingNodes.any { siblingId ->
                        currentPaths.any { path ->
                            path.size >= siblingId.size && path.take(siblingId.size) == siblingId
                        }
                    }

                    // If no sibling is selected, remove siblings from disabled paths
                    if (!hasSiblingSelected) {
                        siblingNodes.forEach { siblingId ->
                            disabledPaths.removeAll { disabled ->
                                disabled == siblingId
                            }
                        }
                    }
                }

                ScriptNodeType.THRESH.name -> {
                    val siblingNodes = getSiblingNodes(parentNode)
                    siblingNodes.forEach { siblingId ->
                        disabledPaths.removeAll { disabled ->
                            disabled == siblingId
                        }
                    }
                }
            }
        }
    }

    // special case if current node is AND, we need to ensure all children are selected
    if (currentNode?.type == ScriptNodeType.AND.name && isSelected) {
        currentNode.subs.forEach { childNode ->
            if (!currentPaths.contains(childNode.id)) {
                currentPaths.add(childNode.id)
            }
            subNodeFollowParent.add(childNode.id)
        }
    }

    return PathSelectionResult(
        signingPath = SigningPath(path = currentPaths),
        disabledPaths = disabledPaths,
        subNodeFollowParent = subNodeFollowParent
    )
}

/**
 * Gets all sibling node IDs for a given node
 */
private fun getSiblingNodes(
    parentNode: ScriptNode,
): List<List<Int>> {
    return parentNode.subs.map { it.id }
}

/**
 * Finds the parent node for a given node ID
 */
fun findParentNode(nodeId: List<Int>, allNodes: List<ScriptNode>): ScriptNode? {
    if (nodeId.size <= 1) return null

    val parentId = nodeId.take(nodeId.size - 1)
    return findNodeById(parentId, allNodes)
}

fun findNodeById(nodeId: List<Int>, allNodes: List<ScriptNode>): ScriptNode? {
    return allNodes.find { it.id == nodeId }
}

/**
 * Collects all nodes in the script tree
 */
fun collectAllNodes(rootNode: ScriptNode): List<ScriptNode> {
    val nodes = mutableListOf<ScriptNode>()

    fun traverse(node: ScriptNode) {
        nodes.add(node)
        node.subs.forEach { traverse(it) }
    }

    traverse(rootNode)
    return nodes
}

/**
 * Extension function to update signing path with smart selection logic
 */
fun SigningPath.updateWithSmartSelection(
    nodeId: List<Int>,
    isSelected: Boolean,
    rootNode: ScriptNode,
    currentDisabledPaths: Set<List<Int>> = emptySet()
): PathSelectionResult {
    val allNodes = collectAllNodes(rootNode)
    val parentNode = findParentNode(nodeId, allNodes)
    val currentNode = findNodeById(nodeId, allNodes)
    return updateSigningPathForNode(
        currentPath = this,
        currentDisabledPaths = currentDisabledPaths,
        nodeId = nodeId,
        isSelected = isSelected,
        currentNode = currentNode,
        parentNode = parentNode,
    )
}

/**
 * Checks if a signing path contains all the required longest leaf nodes.
 * Returns true if the signing path contains all the leaf nodes from the selected path.
 */
fun SigningPath.containsAllLeafNodes(requiredLeafNodes: List<List<Int>>): Boolean {
    if (requiredLeafNodes.isEmpty()) return false

    return requiredLeafNodes.all { requiredNode ->
        path.any { node ->
            node.size >= requiredNode.size && node.take(requiredNode.size) == requiredNode
        }
    }
}

/**
 * Filters signing paths based on the current selected path.
 * Returns all signing paths that contain all the longest leaf nodes from the current path.
 */
fun filterMatchingSigningPaths(
    currentPath: SigningPath,
    availableSigningPaths: List<SigningPath>
): List<SigningPath> {
    val signPathSet = currentPath.path.toMutableSet()
    // exclude non-leaf nodes
    currentPath.path.forEach { path ->
        if (path.size > 2) {
            for (size in 2 until path.size) {
                signPathSet.remove(path.take(size))
            }
        }
    }
    if (signPathSet.isEmpty()) return emptyList()
    val finalPaths = signPathSet.toList()

    return availableSigningPaths.filter { signingPath ->
        signingPath.containsAllLeafNodes(finalPaths)
    }
}