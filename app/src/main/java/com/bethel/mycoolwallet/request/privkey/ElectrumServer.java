package com.bethel.mycoolwallet.request.privkey;

import android.content.res.AssetManager;
import android.text.TextUtils;

import com.bethel.mycoolwallet.utils.Commons;
import com.bethel.mycoolwallet.utils.Constants;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.hash.Hashing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class ElectrumServer {
    public enum Type {
        TCP, TLS
    }

    public final Type type;
    public final String certificateFingerprint;
    public final InetSocketAddress socketAddress;

    public ElectrumServer(final String type, final String host, final String port,
                          final String certificateFingerprint) {
        this.type = Type.valueOf(type.toUpperCase());
        this.certificateFingerprint = certificateFingerprint;

        if (!TextUtils.isEmpty(port)) {
            this.socketAddress = InetSocketAddress.createUnresolved(host, Integer.parseInt(port));
        } else if ("tcp".equalsIgnoreCase(type)) {
            this.socketAddress = InetSocketAddress.createUnresolved(host,
                    Constants.ELECTRUM_SERVER_DEFAULT_PORT_TCP);
        } else if ("tls".equalsIgnoreCase(type)) {
            this.socketAddress = InetSocketAddress.createUnresolved(host,
                    Constants.ELECTRUM_SERVER_DEFAULT_PORT_TLS);
        } else {
            throw new IllegalStateException("Cannot handle: " + type);
        }
    }

    public Socket connect() throws IOException {
        if (null==type)
            throw new IllegalStateException("Cannot handle: server type is null" );
        final Socket socket;
        switch (type) {
            case TCP:
                socket = new Socket();
                socket.connect(socketAddress, 5000);
                break;
            case TLS:
                final SocketFactory sf = sslTrustAllCertificates();
                socket = sf.createSocket(this.socketAddress.getHostName(), this.socketAddress.getPort());

                final SSLSession sslSession = ((SSLSocket) socket).getSession();
                final Certificate certificate = sslSession.getPeerCertificates()[0];
                final String fingerprint = sslCertificateFingerprint(certificate);
                // check ssl Certificate Fingerprint
                if (null== this.certificateFingerprint) {
                    // signed  by CA
                    final HostnameVerifier verifier = HttpsURLConnection.getDefaultHostnameVerifier();
                    if (!verifier.verify(socketAddress.getHostName(), sslSession)) {
                        // 校验失败
                        String msg = String.format("Expected %s , got %s ",
                                 socketAddress.getHostName(), sslSession.getPeerPrincipal());
                        throw new SSLPeerUnverifiedException(msg);
                    }
                } else {
                    // self signed
                    if (!this.certificateFingerprint.equals(fingerprint)) {
                        // 签名不一致
                        String msg = String.format("Expected %s for %s , got %s ",
                                certificateFingerprint, socketAddress.getHostName(), fingerprint);
                        throw new SSLPeerUnverifiedException(msg);
                    }
                }
                break;
            default:
                throw new IllegalStateException("Cannot handle: " + type);
        }
        return socket;
    }

    private String sslCertificateFingerprint(final Certificate certificate) {
        try {
            return Hashing.sha256().newHasher().putBytes(certificate.getEncoded()).hash().toString();
        } catch (final Exception x) {
            throw new RuntimeException(x);
        }
    }
    private SSLSocketFactory sslTrustAllCertificates() {
        try {
            final SSLContext context = SSLContext.getInstance("SSL");
            context.init(null, new TrustManager[] { TRUST_ALL_CERTIFICATES }, null);
            return context.getSocketFactory();
        } catch (final Exception x) {
            throw new RuntimeException(x);
        }
    }

    private static final X509TrustManager TRUST_ALL_CERTIFICATES = new X509TrustManager() {
        @Override
        public void checkClientTrusted(final X509Certificate[] chain, final String authType)
                throws CertificateException {
        }

        @Override
        public void checkServerTrusted(final X509Certificate[] chain, final String authType)
                throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };



    public static List<ElectrumServer> loadElectrumServers(final AssetManager assetManager) throws IOException {
        final InputStream is = assetManager.open(Constants.Files.ELECTRUM_SERVERS_ASSET);
        return loadElectrumServers(is);
    }

//    tls:tn.not.fyi:55002:eac147ddf1cb86eb9706f35e181618b6a6e29bc6173939a921f5b0cb8b92bae8
//    tls:bitcoin.cluelessperson.com::bcc078f2860537a0149f920050ffce15fb99591b34fb96fcfa1224558dbd709d
    private static List<ElectrumServer> loadElectrumServers(final InputStream is) throws IOException {
        final List<ElectrumServer> servers = new LinkedList<>();

        final Splitter splitter = Splitter.on(':').trimResults();
        String line = null;

        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(is, Commons.UTF_8));
            while (true) {
                line = reader.readLine();
                if (null==line) {
                    break;
                }
                if (0==line.length() || '#' == line.charAt(0)) {
                    continue;
                }

                // 解析每一行
                final Iterator<String> iterator = splitter.split(line).iterator();
                final String type = iterator.next();
                final  String host = iterator.next();
                final String port = iterator.hasNext() ? Strings.emptyToNull(iterator.next()) : null;
                final String certificateFingerprint = iterator.hasNext() ? Strings.emptyToNull(iterator.next()) : null;

                final ElectrumServer es = new ElectrumServer(type, host, port, certificateFingerprint);
                servers.add(es);
            } // end while
        } catch (final Exception e) {
            throw new IOException("Error while parsing: '" + line + "'", e);
        }
        return servers;
    }
}