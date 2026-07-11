package cn.winddol.ai.test;

import org.junit.Assume;
import org.junit.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.net.Proxy;

import static org.junit.Assert.assertFalse;

public class NetworkExternalIT {

    @Test
    public void verifyExternalNetworkThroughConfiguredProxy() {
        Assume.assumeTrue("Set RUN_EXTERNAL_TESTS=true to run this external test",
                "true".equalsIgnoreCase(System.getenv("RUN_EXTERNAL_TESTS")));

        String proxyHost = System.getenv().getOrDefault("TEST_PROXY_HOST", "127.0.0.1");
        int proxyPort = Integer.parseInt(System.getenv().getOrDefault("TEST_PROXY_PORT", "10810"));
        String testUrl = System.getenv().getOrDefault("TEST_NETWORK_URL", "https://api.semanticscholar.org");

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setProxy(proxy);

        RestClient client = RestClient.builder().requestFactory(factory).build();
        String result = client.get()
                .uri(testUrl)
                .retrieve()
                .body(String.class);

        assertFalse(result == null || result.isBlank());
    }
}
