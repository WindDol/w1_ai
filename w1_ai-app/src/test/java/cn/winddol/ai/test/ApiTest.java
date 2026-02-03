package cn.winddol.ai.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.net.Proxy;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ApiTest {

    @Test
    public void test() {
        log.info("测试完成");
    }

    @Test
    public void testNetwork() {
        // 临时构造一个带代理的 client
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 10810));
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setProxy(proxy);

        RestClient client = RestClient.builder().requestFactory(factory).build();

        // 访问 Google 或 Semantic Scholar 检查连通性
        String result = client.get()
                .uri("https://www.google.com") // 或者 https://api.semanticscholar.org/graph/v1/paper/search?query=test
                .retrieve()
                .body(String.class);

        System.out.println("网络连通成功，返回长度：" + result.length());
    }
}
