package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.model.Card

sealed class DragDropTarget {
    data class RackSlot(val rowIndex: Int, val slotIndex: Int) : DragDropTarget()
    data object DiscardPile : DragDropTarget()
    data object StockPile : DragDropTarget()
    data class TableMeld(val meldId: String) : DragDropTarget()
    data class Meld(val meldId: String) : DragDropTarget()
    data class MeldWorkspaceSlot(val slotIndex: Int) : DragDropTarget()
}

data class DraggedCardState(
    val card: Card,
    val sourceRow: Int,
    val sourceSlotIndex: Int,
    val dragCurrentPosition: Offset,
    val dragStartOffset: Offset,
    val isDragging: Boolean = true
)

class DragDropRegistry {
    private val rowBounds = mutableMapOf<Int, Rect>()
    private val slotBoundsInRows = mutableMapOf<Int, MutableMap<Int, Rect>>()
    private var discardBounds: Rect? = null
    private var stockBounds: Rect? = null
    private val meldBounds = mutableMapOf<String, Rect>()

    fun registerRow(rowIndex: Int, bounds: Rect) {
        rowBounds[rowIndex] = bounds
    }

    fun registerSlot(rowIndex: Int, slotIndex: Int, bounds: Rect) {
        val rowSlots = slotBoundsInRows.getOrPut(rowIndex) { mutableMapOf() }
        rowSlots[slotIndex] = bounds
    }

    fun registerDiscardPile(bounds: Rect) {
        discardBounds = bounds
    }

    fun registerStockPile(bounds: Rect) {
        stockBounds = bounds
    }

    fun registerMeld(meldId: String, bounds: Rect) {
        meldBounds[meldId] = bounds
    }

    fun unregisterMeld(meldId: String) {
        meldBounds.remove(meldId)
    }

    fun clear() {
        rowBounds.clear()
        slotBoundsInRows.clear()
        discardBounds = null
        stockBounds = null
        meldBounds.clear()
    }

    fun registerTarget(target: DragDropTarget, bounds: Rect) {
        when (target) {
            is DragDropTarget.RackSlot -> registerSlot(target.rowIndex, target.slotIndex, bounds)
            is DragDropTarget.DiscardPile -> registerDiscardPile(bounds)
            is DragDropTarget.StockPile -> registerStockPile(bounds)
            is DragDropTarget.TableMeld -> registerMeld(target.meldId, bounds)
            is DragDropTarget.Meld -> registerMeld(target.meldId, bounds)
            is DragDropTarget.MeldWorkspaceSlot -> {}
        }
    }

    fun findTarget(position: Offset): DragDropTarget? {
        discardBounds?.let { rect ->
            val expanded = rect.inflate(22f)
            if (expanded.contains(position)) {
                return DragDropTarget.DiscardPile
            }
        }

        for ((meldId, bounds) in meldBounds) {
            val expanded = bounds.inflate(16f)
            if (expanded.contains(position)) {
                return DragDropTarget.TableMeld(meldId)
            }
        }

        for ((rowIndex, rowRect) in rowBounds) {
            val expanded = rowRect.inflate(18f)
            if (expanded.contains(position)) {
                val slotMap = slotBoundsInRows[rowIndex] ?: emptyMap()
                if (slotMap.isNotEmpty()) {
                    for ((slotIdx, sRect) in slotMap) {
                        if (sRect.inflate(6f).contains(position)) {
                            return DragDropTarget.RackSlot(rowIndex, slotIdx)
                        }
                    }
                    var closestSlot = 0
                    var minDistance = Float.MAX_VALUE
                    for ((slotIdx, sRect) in slotMap) {
                        val dist = kotlin.math.abs(position.x - sRect.center.x)
                        if (dist < minDistance) {
                            minDistance = dist
                            closestSlot = slotIdx
                        }
                    }
                    return DragDropTarget.RackSlot(rowIndex, closestSlot)
                }
                return DragDropTarget.RackSlot(rowIndex, 0)
            }
        }

        stockBounds?.let { rect ->
            val expanded = rect.inflate(16f)
            if (expanded.contains(position)) {
                return DragDropTarget.StockPile
            }
        }

        return null
    }

    private fun Rect.inflate(amount: Float): Rect {
        return Rect(
            left = this.left - amount,
            top = this.top - amount,
            right = this.right + amount,
            bottom = this.bottom + amount
        )
    }
}
