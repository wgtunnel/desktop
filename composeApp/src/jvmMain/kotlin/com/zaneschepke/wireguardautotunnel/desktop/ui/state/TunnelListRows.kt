package com.zaneschepke.wireguardautotunnel.desktop.ui.state

import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelGroup

sealed class TunnelListRow {
    abstract val key: String
    abstract val scopeKey: String

    data class GroupHeader(
        val group: TunnelGroup,
        val childCount: Int,
        val visibleExpanded: Boolean,
        val activeCount: Int,
    ) : TunnelListRow() {
        override val key: String = "group-${group.id}"
        override val scopeKey: String = ROOT_SCOPE
    }

    data class TunnelRow(val tunnel: TunnelConfig, val grouped: Boolean) : TunnelListRow() {
        override val key: String = "tunnel-${tunnel.id}"
        override val scopeKey: String = tunnel.groupId?.let { "group-$it" } ?: ROOT_SCOPE
    }

    companion object {
        const val ROOT_SCOPE = "root"
    }
}

private sealed interface RootItem {
    val position: Int
    val name: String

    data class Group(val group: TunnelGroup) : RootItem {
        override val position: Int
            get() = group.position

        override val name: String
            get() = group.name
    }

    data class Tunnel(val tunnel: TunnelConfig) : RootItem {
        override val position: Int
            get() = tunnel.position

        override val name: String
            get() = tunnel.name
    }
}

fun nextRootPosition(groups: List<TunnelGroup>, tunnels: List<TunnelConfig>): Int {
    val ungroupedMax = tunnels.filter { it.groupId == null }.maxOfOrNull { it.position } ?: -1
    val groupMax = groups.maxOfOrNull { it.position } ?: -1
    return maxOf(ungroupedMax, groupMax) + 1
}

fun nextChildPosition(tunnels: List<TunnelConfig>, groupId: Long): Int {
    return (tunnels.filter { it.groupId == groupId }.maxOfOrNull { it.position } ?: -1) + 1
}

fun uniqueDisplayName(desired: String, existing: Collection<String>, fallback: String): String {
    val base = desired.trim().ifEmpty { fallback }
    if (base !in existing) return base
    var n = 1
    var candidate = "$base ($n)"
    while (candidate in existing) {
        n++
        candidate = "$base ($n)"
    }
    return candidate
}

fun buildTunnelListRows(
    groups: List<TunnelGroup>,
    tunnels: List<TunnelConfig>,
    activeTunnelIds: Set<Long>,
    collapseGroups: Boolean = false,
): List<TunnelListRow> {
    val childrenByGroup = tunnels.filter { it.groupId != null }.groupBy { it.groupId }
    return rootItems(groups, tunnels).flatMap { item ->
        when (item) {
            is RootItem.Group -> {
                val children =
                    (childrenByGroup[item.group.id] ?: emptyList()).sortedBy { it.position }
                val activeCount = children.count { it.id in activeTunnelIds }
                // Not forced open by an active child, the header shows the active count instead
                val visibleExpanded = !collapseGroups && item.group.expanded
                buildList<TunnelListRow> {
                    add(
                        TunnelListRow.GroupHeader(
                            group = item.group,
                            childCount = children.size,
                            visibleExpanded = visibleExpanded,
                            activeCount = activeCount,
                        )
                    )
                    if (visibleExpanded) {
                        children.forEach { add(TunnelListRow.TunnelRow(it, grouped = true)) }
                    }
                }
            }
            is RootItem.Tunnel ->
                listOf<TunnelListRow>(TunnelListRow.TunnelRow(item.tunnel, grouped = false))
        }
    }
}

fun moveDisplayedRows(
    groups: List<TunnelGroup>,
    tunnels: List<TunnelConfig>,
    fromIndex: Int,
    toIndex: Int,
    rows: List<TunnelListRow>,
): Pair<List<TunnelGroup>, List<TunnelConfig>>? {
    if (fromIndex == toIndex) return groups to tunnels
    if (fromIndex !in rows.indices || toIndex !in rows.indices) return null
    val fromRow = rows[fromIndex]
    val toRow = rows[toIndex]
    if (fromRow.scopeKey != toRow.scopeKey) {
        return moveTunnelAcrossScopes(
            groups,
            tunnels,
            fromRow,
            toRow,
            movingDown = fromIndex < toIndex,
        )
    }
    val scopeGroupId =
        when (fromRow) {
            is TunnelListRow.TunnelRow -> fromRow.tunnel.groupId
            is TunnelListRow.GroupHeader -> null
        }
    if (scopeGroupId != null) {
        val children = tunnels.filter { it.groupId == scopeGroupId }.sortedBy { it.position }
        val fromChild = children.indexOfFirst {
            it.id == (fromRow as TunnelListRow.TunnelRow).tunnel.id
        }
        val toChild = children.indexOfFirst {
            it.id == (toRow as TunnelListRow.TunnelRow).tunnel.id
        }
        if (fromChild < 0 || toChild < 0) return null
        return groups to moveGroupChildren(tunnels, scopeGroupId, fromChild, toChild)
    }
    val root = rootItems(groups, tunnels).toMutableList()
    val fromRoot = rootIndex(root, fromRow)
    val toRoot = rootIndex(root, toRow)
    if (fromRoot < 0 || toRoot < 0) return null
    val moved = root.removeAt(fromRoot)
    root.add(toRoot, moved)
    return reindexRoot(root, tunnels)
}

private fun moveTunnelAcrossScopes(
    groups: List<TunnelGroup>,
    tunnels: List<TunnelConfig>,
    fromRow: TunnelListRow,
    toRow: TunnelListRow,
    movingDown: Boolean,
): Pair<List<TunnelGroup>, List<TunnelConfig>>? {
    val moved =
        (fromRow as? TunnelListRow.TunnelRow)?.tunnel?.let { row ->
            tunnels.firstOrNull { it.id == row.id }
        } ?: return null
    val others = tunnels.filter { it.id != moved.id }

    val targetGroupId = (toRow as? TunnelListRow.TunnelRow)?.tunnel?.groupId
    if (targetGroupId != null) {
        val target =
            others.filter { it.groupId == targetGroupId }.sortedBy { it.position }.toMutableList()
        val toIndex = target.indexOfFirst { it.id == (toRow as TunnelListRow.TunnelRow).tunnel.id }
        if (toIndex < 0) return null
        // Entering from above lands before the row, from below after it
        target.add(
            (toIndex + if (movingDown) 0 else 1).coerceIn(0, target.size),
            moved.copy(groupId = targetGroupId),
        )
        val rest = others.filter { it.groupId != targetGroupId }
        return normalize(
            groups,
            rest + target.mapIndexed { index, tunnel -> tunnel.copy(position = index) },
        )
    }

    // Leaving its group for the top level, next to the row it was dragged onto
    val root = rootItems(groups, others).toMutableList()
    val toRoot = rootIndex(root, toRow)
    if (toRoot < 0) return null
    root.add(toRoot + if (movingDown) 1 else 0, RootItem.Tunnel(moved.copy(groupId = null)))
    val (newGroups, newTunnels) = reindexRoot(root, others)
    return normalizeChildren(newGroups, newTunnels)
}

private fun normalize(
    groups: List<TunnelGroup>,
    tunnels: List<TunnelConfig>,
): Pair<List<TunnelGroup>, List<TunnelConfig>> {
    val (newGroups, newTunnels) = reindexRoot(rootItems(groups, tunnels), tunnels)
    return normalizeChildren(newGroups, newTunnels)
}

private fun normalizeChildren(
    groups: List<TunnelGroup>,
    tunnels: List<TunnelConfig>,
): Pair<List<TunnelGroup>, List<TunnelConfig>> {
    val children =
        tunnels
            .filter { it.groupId != null }
            .groupBy { it.groupId }
            .values
            .flatMap { list ->
                list
                    .sortedBy { it.position }
                    .mapIndexed { index, tunnel -> tunnel.copy(position = index) }
            }
    return groups to (tunnels.filter { it.groupId == null } + children)
}

fun ungroupKeepingOrder(
    groups: List<TunnelGroup>,
    tunnels: List<TunnelConfig>,
    groupId: Long,
): Pair<List<TunnelGroup>, List<TunnelConfig>> {
    val children = tunnels.filter { it.groupId == groupId }.sortedBy { it.position }
    val original = rootItems(groups, tunnels).toMutableList()
    val insertAt = original.indexOfFirst { it is RootItem.Group && it.group.id == groupId }
    if (insertAt >= 0) original.removeAt(insertAt)
    val index = if (insertAt >= 0) insertAt else original.size
    children.forEachIndexed { offset, child ->
        original.add(index + offset, RootItem.Tunnel(child.copy(groupId = null)))
    }
    val remainingGroups = groups.filter { it.id != groupId }
    return reindexRoot(original, tunnels.filter { it.groupId != groupId }, remainingGroups)
}

private fun rootItems(groups: List<TunnelGroup>, tunnels: List<TunnelConfig>): List<RootItem> {
    val ungrouped = tunnels.filter { it.groupId == null }
    return (groups.map { RootItem.Group(it) } + ungrouped.map { RootItem.Tunnel(it) }).sortedWith(
        compareBy({ it.position }, { it.name.lowercase() })
    )
}

private fun rootIndex(root: List<RootItem>, row: TunnelListRow): Int {
    return when (row) {
        is TunnelListRow.GroupHeader ->
            root.indexOfFirst { it is RootItem.Group && it.group.id == row.group.id }
        is TunnelListRow.TunnelRow ->
            root.indexOfFirst { it is RootItem.Tunnel && it.tunnel.id == row.tunnel.id }
    }
}

private fun moveGroupChildren(
    tunnels: List<TunnelConfig>,
    groupId: Long,
    fromIndex: Int,
    toIndex: Int,
): List<TunnelConfig> {
    val children = tunnels.filter { it.groupId == groupId }.sortedBy { it.position }.toMutableList()
    val others = tunnels.filter { it.groupId != groupId }
    if (fromIndex !in children.indices || toIndex !in children.indices) return tunnels
    val moved = children.removeAt(fromIndex)
    children.add(toIndex, moved)
    return others + children.mapIndexed { index, tunnel -> tunnel.copy(position = index) }
}

private fun reindexRoot(
    root: List<RootItem>,
    tunnels: List<TunnelConfig>,
    groupsOverride: List<TunnelGroup>? = null,
): Pair<List<TunnelGroup>, List<TunnelConfig>> {
    val grouped = tunnels.filter { it.groupId != null }
    val newGroups = mutableListOf<TunnelGroup>()
    val newUngrouped = mutableListOf<TunnelConfig>()
    root.forEachIndexed { index, item ->
        when (item) {
            is RootItem.Group -> newGroups += item.group.copy(position = index)
            is RootItem.Tunnel -> newUngrouped += item.tunnel.copy(position = index)
        }
    }
    val groups =
        groupsOverride?.map { group -> newGroups.firstOrNull { it.id == group.id } ?: group }
            ?: newGroups
    return groups to (grouped + newUngrouped)
}
