package de.carsten.android.muzzic.visualization

import de.carsten.android.muzzic.visualization.render.shaders.BarsFragment
import de.carsten.android.muzzic.visualization.render.shaders.FullscreenVertex
import de.carsten.android.muzzic.visualization.render.shaders.LedFragment
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards all GLSL sources against identifiers reserved for future use by
 * GLSL ES 3.00 (§3.6). Using them is a compile error on spec-compliant drivers,
 * which previously broke the BARS program (`int half`) and silently fell back
 * to the LED style.
 */
class ShaderReservedWordsTest {

    companion object {
        // Keywords reserved for future use per GLSL ES 3.00 §3.6 (excerpt of
        // plausible variable names; the full list also reserves sampler/image types).
        private val reservedWords = setOf(
            "attribute", "varying", "coherent", "volatile", "restrict", "readonly",
            "writeonly", "resource", "atomic_uint", "noperspective", "patch", "sample",
            "subroutine", "common", "partition", "active", "asm", "class", "union",
            "enum", "typedef", "template", "this", "goto", "inline", "noinline",
            "public", "static", "extern", "external", "interface", "long", "short",
            "double", "half", "fixed", "unsigned", "superp", "input", "output",
            "hvec2", "hvec3", "hvec4", "dvec2", "dvec3", "dvec4", "fvec2", "fvec3",
            "fvec4", "sampler3DRect", "filter", "sizeof", "cast", "namespace", "using",
        )

        private val identifierRegex = Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b")

        private fun reservedTokensIn(source: String): Set<String> {
            val codeLines = source.lines()
                .map { it.substringBefore("//") }
                .filterNot { it.trimStart().startsWith("#") }
            return identifierRegex.findAll(codeLines.joinToString("\n"))
                .map { it.value }
                .filter { it in reservedWords }
                .toSet()
        }
    }

    @Test
    fun `no shader uses reserved future-use keywords`() {
        val sources = mapOf(
            "FullscreenVertex" to FullscreenVertex.SOURCE,
            "LedFragment" to LedFragment.SOURCE,
            "BarsFragment" to BarsFragment.SOURCE,
        )
        for ((name, source) in sources) {
            val violations = reservedTokensIn(source)
            assertTrue(
                "Shader $name uses reserved GLSL ES keywords: $violations",
                violations.isEmpty(),
            )
        }
    }

    @Test
    fun `shared SDF snippet is composed into both raster shaders`() {
        for ((name, source) in listOf("LedFragment" to LedFragment.SOURCE, "BarsFragment" to BarsFragment.SOURCE)) {
            assertTrue("$name should contain the shared SDF block", "length(max(q, 0.0))" in source)
        }
        // Both raster shaders must declare what the snippet expects.
        for ((name, source) in listOf("LedFragment" to LedFragment.SOURCE, "BarsFragment" to BarsFragment.SOURCE)) {
            assertTrue("$name should define cellPx", "cellPx" in source)
        }
    }
}
