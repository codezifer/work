package de.carsten.android.muzzic.visualization

import de.carsten.android.muzzic.visualization.render.shaders.BarsFragment
import de.carsten.android.muzzic.visualization.render.shaders.FullscreenVertex
import de.carsten.android.muzzic.visualization.render.shaders.LedFragment
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Guards GLSL uniform declarations against mistakes that silently kill a whole
 * program: a duplicated uniform name (e.g. a `vec4` array and a `float` sharing
 * one name) fails shader compilation, `createProgram` returns 0, and the
 * renderer draws nothing at all — with no crash pointing at the cause.
 */
class ShaderUniformTest {

    companion object {
        private val uniformRegex = Regex("uniform\\s+\\w+\\s+([A-Za-z_]\\w*)")

        private fun declaredUniforms(source: String): List<String> {
            val codeLines = source.lines()
                .map { it.substringBefore("//") }
                .filterNot { it.trimStart().startsWith("#") }
            return uniformRegex.findAll(codeLines.joinToString("\n"))
                .map { it.groupValues[1] }
                .toList()
        }

        /**
         * Every uniform `LedBarRenderer` queries via `glGetUniformLocation` on
         * the LED program. Mirrors the renderer; extend both together.
         */
        private val ledUniforms = listOf(
            "uRes",
            "uBandCount",
            "uSegments",
            "uBands",
            "uPeaks",
            "uTrail",
            "uColLow",
            "uColMid",
            "uColHigh",
            "uZoneStart",
            "uLedHalfSize",
            "uCornerRadius",
            "uOffIntensity",
            "uBackground",
            "uGlow",
            "uShimmer",
            "uHotCore",
            "uSpecular",
            "uBleed",
            "uGlowGrade",
            "uFade",
            "uTrailStrength",
            "uTime",
        )
    }

    @Test
    fun `no shader declares a uniform twice`() {
        val sources = mapOf(
            "FullscreenVertex" to FullscreenVertex.SOURCE,
            "LedFragment" to LedFragment.SOURCE,
            "BarsFragment" to BarsFragment.SOURCE,
        )
        for ((name, source) in sources) {
            val declared = declaredUniforms(source)
            assertThat(declared)
                .`as`("Shader $name declares duplicate uniforms")
                .doesNotHaveDuplicates()
        }
    }

    @Test
    fun `led program declares every uniform the renderer queries`() {
        val declared = declaredUniforms(LedFragment.SOURCE).toSet()
        for (uniform in ledUniforms) {
            assertThat(declared)
                .`as`("LedFragment misses uniform $uniform queried by LedBarRenderer")
                .contains(uniform)
        }
    }
}
