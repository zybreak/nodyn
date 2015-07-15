package io.nodyn.crypto;

import java.nio.ByteBuffer;
import org.bouncycastle.crypto.digests.MD5Digest;

/**
 * @author Bob McWhirter
 */
public final class OpenSSLKDF {

    private ByteBuffer key;
    private ByteBuffer iv;

    public OpenSSLKDF(ByteBuffer data, int keyLen, int ivLen) {
        int originalPosition = data.position();
        byte[] bytes = new byte[originalPosition];
        data.position(0);
        data.get(bytes);
        data.position(originalPosition);
        kdf(bytes, keyLen / 8 , ivLen  );
    }

    public ByteBuffer getKey() {
        return this.key;
    }

    public ByteBuffer getIv() {
        return this.iv;
    }

    protected void kdf(byte[] data, int keyLen, int ivLen) {

        int totalLen = keyLen + ivLen;
        int curLen = 0;
        byte[] prev = new byte[0];

        byte[] kiv = new byte[totalLen];
        for (int i = 0; i < totalLen; ++i) {
            kiv[i] = 0;
        }

        while (curLen < totalLen) {
            prev = kdf_d(data, prev, 1);
            for (byte aPrev : prev) {
                if (curLen < (kiv.length)) {
                    kiv[curLen] = aPrev;
                    ++curLen;
                }
            }
        }

        this.key = ByteBuffer.allocate(keyLen);
        this.iv = ByteBuffer.allocate(ivLen);

        this.key.put(kiv, 0, keyLen);
        this.iv.put(kiv, keyLen, ivLen);
    }

    protected byte[] kdf_d(byte[] data, byte[] prev, int iter) {

        byte[] bytes = new byte[prev.length + data.length];

        System.arraycopy(prev, 0, bytes, 0, prev.length);
        System.arraycopy(data, 0, bytes, prev.length + 0, data.length);

        byte[] cur = bytes;

        for (int i = 0; i < iter; ++i) {
            MD5Digest digest = new MD5Digest();
            digest.update(cur, 0, cur.length);

            cur = new byte[digest.getDigestSize()];
            digest.doFinal(cur, 0);
        }

        byte[] out = new byte[16];

        System.arraycopy(cur, 0, out, 0, out.length);

        return out;
    }


}
