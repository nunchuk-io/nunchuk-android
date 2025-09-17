package com.nunchuk.android.model

/**
 * Represents a signing path in a miniscript policy tree.
 * The path contains a list of node IDs that form a valid signing path.
 */
/**
 * Checks if this signing path contains the given node ID
 */
fun SigningPath.contains(nodeId: List<Int>): Boolean {
    return path.any { idList ->
        (nodeId.size <= idList.size && idList.take(nodeId.size) == nodeId) ||
                (idList.size <= nodeId.size && nodeId.take(idList.size) == idList)
    }
}

/**
 * Returns true if the signing path is empty
 */
fun SigningPath.isEmpty(): Boolean = path.isEmpty()

/**
 * Returns true if the signing path is not empty
 */
fun SigningPath.isNotEmpty(): Boolean = path.isNotEmpty()


