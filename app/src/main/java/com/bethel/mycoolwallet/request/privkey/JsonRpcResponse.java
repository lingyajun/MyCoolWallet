package com.bethel.mycoolwallet.request.privkey;

public final class JsonRpcResponse {
    public static class Utxo {
        public String tx_hash;
        public int tx_pos;
        public long value;
        public int height;
    }

    public static class Error {
        public int code;
        public String message;
    }

    public int id;
    public Utxo[] result;
    public Error error;
}
