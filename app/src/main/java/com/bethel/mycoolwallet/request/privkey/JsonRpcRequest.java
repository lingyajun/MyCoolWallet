package com.bethel.mycoolwallet.request.privkey;

public final class JsonRpcRequest {
    public final int id;
    public final String method;
    public final String[] params;

    private static transient int idCounter = 0;

    public JsonRpcRequest(final String method, final String[] params) {
        this(idCounter++, method, params);
    }

    public JsonRpcRequest(final int id, final String method, final String[] params) {
        this.id = id;
        this.method = method;
        this.params = params;
    }
}
