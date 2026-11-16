package de.familienwecker.famwake.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Startup-Macrobenchmarks zum Vergleich von Cold-Start-Zeiten mit und ohne Baseline Profile.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class StartupBenchmarks {

    @get:Rule
    val rule = MacrobenchmarkRule()

    private val targetPackage: String
        get() = InstrumentationRegistry.getArguments().getString("targetAppId")
            ?: "de.familienwecker.famwake"

    @Test
    fun startupCompilationNone() = benchmark(CompilationMode.None())

    @Test
    fun startupCompilationBaselineProfiles() =
        benchmark(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun benchmark(compilationMode: CompilationMode) {
        rule.measureRepeated(
            packageName = targetPackage,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = compilationMode,
            startupMode = StartupMode.COLD,
            iterations = 5,
            setupBlock = {
                pressHome()
            }
        ) {
            startActivityAndWait()
        }
    }
}
