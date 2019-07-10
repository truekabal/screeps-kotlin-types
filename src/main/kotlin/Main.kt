import main.kotlin.Core
import main.kotlin.GC

// Entry point
@Suppress("unused")

val gc:GC = GC()
val core:Core = Core()

fun loop() {
    gc.run()
    core.tick()
}
