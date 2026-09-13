package de.carsten.android.muzzic.id3.model.frame.binary

import de.carsten.android.muzzic.id3.model.frame.BinaryDataFrame
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Equalisation (EQU2).
 *
 * Points should be ordered by frequency. Multiple frames per tag
 * allowed, unique by identification.
 *
 * @property header frame header.
 * @property interpolation preferred interpolation method.
 * @property identification situation or device this curve applies to.
 * @property points adjustment points ordered by frequency.
 */
data class Equ2Frame(override val header: FrameHeader, val interpolation: Equ2Interpolation, val identification: String, val points: List<Equ2Point>) : BinaryDataFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitEqu2(this)
}
