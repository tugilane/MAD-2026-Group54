package ee.ut.cs.mad.strideset.running

class FakeLocationSource : LocationSource {
    var starts = 0
    var stops = 0
    private var onFix: ((LocationFix) -> Unit)? = null
    private var onUnavailable: (() -> Unit)? = null
    override fun start(onFix: (LocationFix) -> Unit, onUnavailable: () -> Unit) {
        starts++
        this.onFix = onFix
        this.onUnavailable = onUnavailable
    }
    override fun stop() { stops++; onFix = null; onUnavailable = null }
    fun emit(fix: LocationFix) { onFix?.invoke(fix) }
    fun disable() { onUnavailable?.invoke() }
}
