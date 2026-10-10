package com.github.andreyasadchy.xtra.ui.player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Modifier
class LiveLoadControlContractTest {
    @Test fun forwardsEveryModernMethodUsedByMedia3() {
        val contract = LoadControl::class.java.methods.map { it.name to it.parameterTypes.toList() }.toSet()
        val methods = DefaultLoadControl::class.java.declaredMethods.filter {
            Modifier.isPublic(it.modifiers) && !Modifier.isStatic(it.modifiers) && (it.name to it.parameterTypes.toList()) in contract
        }
        assertTrue(methods.size >= 10)
        methods.forEach {
            assertEquals("Missing explicit forwarding: ${it.name}", LiveLoadControl::class.java,
                LiveLoadControl::class.java.getMethod(it.name, *it.parameterTypes).declaringClass)
        }
    }
}
