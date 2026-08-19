package cl.bice.createStructure.utils;

import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;


public class RestTemplateWithCert {

    public RestTemplate createRestTemplate() throws Exception {
        // Cargar el KeyStore con el certificado
        KeyStore keyStore = KeyStore.getInstance("pkcs12"); // O "JKS" según el formato
        try (InputStream keyStoreStream = Files.newInputStream(Paths.get("src/main/java/cl/bice/cert/api-baas-qa.pfx"))) {
            keyStore.load(keyStoreStream, "changeit".toCharArray());
        }

        // Crear el contexto SSL
        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                .loadKeyMaterial(keyStore, "changeit".toCharArray()) // Contraseña del certificado
                .build();


// SSLContext ya creado previamente
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(socketFactory)
                // No establecer route planner => sin proxy
                .build();

        // Integrar con RestTemplate
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(factory);
    }


    public RestTemplate createRestTemplate1() throws Exception {
        // Cargar el KeyStore con el certificado
        KeyStore keyStore = KeyStore.getInstance("pkcs12"); // O "JKS" según el formato
        try (InputStream keyStoreStream = Files.newInputStream(Paths.get("src/main/java/cl/bice/cert/api-baas-qa.pfx"))) {
            keyStore.load(keyStoreStream, "changeit".toCharArray());
        }

        // Crear el contexto SSL
        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                .loadKeyMaterial(keyStore, "changeit".toCharArray()) // Contraseña del certificado
                .build();

        HttpHost proxy = new HttpHost("https://api-baas-dev-test.bice.local/");
        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
        // Configurar el cliente HTTP con SSL
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(socketFactory)
                .setRoutePlanner(routePlanner)
                .build();


        // Integrar con RestTemplate
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(factory);
    }



    public RestTemplate restTemplateSinCertificado() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());


        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                sslContext,
                new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true; // No verifica el hostname
                    }
                }
        );

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(socketFactory)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(factory);
    }




}
