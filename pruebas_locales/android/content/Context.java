package android.content;

public abstract class Context {
    public String getString(int resId) {
        return "StubString_resId_" + resId;
    }
    public String getString(int resId, Object... formatArgs) {
        return String.format("StubString_resId_" + resId + " (with args: %d items)", formatArgs.length);
    }
}
