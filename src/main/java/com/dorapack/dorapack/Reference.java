package com.dorapack.dorapack;

/**
 * Global constants for the mod. Values here mirror the injected {@link Tags} where possible,
 * but are kept as plain constants so they can be referenced from annotations.
 */
public final class Reference {

    public static final String MOD_ID = "dorapack";
    public static final String MOD_NAME = "Doraemon's Pocket";

    public static final String CLIENT_PROXY = "com.dorapack.dorapack.proxy.ClientProxy";
    public static final String SERVER_PROXY = "com.dorapack.dorapack.proxy.ServerProxy";

    /** IC2 mod id, used for hard dependency declaration and interop guards. */
    public static final String IC2_MOD_ID = "ic2";

    private Reference() {
    }
}
