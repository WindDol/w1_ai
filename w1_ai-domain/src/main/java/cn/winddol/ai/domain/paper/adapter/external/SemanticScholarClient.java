package cn.winddol.ai.domain.paper.adapter.external;

import cn.winddol.ai.domain.paper.adapter.external.dto.S2PaperResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.net.Proxy;


@Component
@Slf4j
public class SemanticScholarClient {

    private final RestClient restClient;

    public SemanticScholarClient(RestClient.Builder builder) {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 10810));

        // 2. 创建一个支持代理的 RequestFactory
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setProxy(proxy);

        // 可选：设置超时时间，防止网络不好卡死
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(10000);

        // 3. 构建 RestClient
        this.restClient = builder
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * 根据原始引用文本搜索论文信息
     * @param rawReference e.g. "25. N. J. Zabusky..."
     */
    public S2PaperResponse.S2PaperData searchPaper(String rawReference) {
        // 1. 简单的清洗：去掉开头的数字编号 (如 "[25]", "25.")
        // 否则 API 可能会因为数字干扰搜索结果
        String query = rawReference.replaceAll("^(\\[?\\d+\\]?\\.?)\\s*", "").trim();

        // 截断：如果 query 太长，API 可能会报错，通常取前 100-200 字符足够搜索
        if (query.length() > 200) {
            query = query.substring(0, 200);
        }

        try {
            String finalQuery = query;
            S2PaperResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.semanticscholar.org")
                            .path("/graph/v1/paper/search")
                            .queryParam("query", finalQuery)
                            .queryParam("limit", 1)
                            .queryParam("fields", "title,abstract,citationCount,year,authors")
                            .build())
                    .retrieve()
                    .body(S2PaperResponse.class);

            // 3. 【关键】防御性空指针检查
            // 必须同时检查 response 是否为 null，以及 response.getData() 是否为 null
            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                return response.getData().get(0);
            }

            // 如果搜不到，或者 data 为 null，安全返回 null
            return null;

        }  catch (HttpClientErrorException.TooManyRequests e) {
            // 【关键】如果是 429 异常，直接抛出，不要在里面处理
            throw e;
        } catch (Exception e) {
            // 其他异常（比如 404 或解析错误）可以捕获并返回 null
            log.warn("S2 API search failed (Normal): {}", query);
            return null;
        }
    }
}
