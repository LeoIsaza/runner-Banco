package cl.bice.createStructure.config;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ConfigurationRestTemplate {

    @Value("${connection.timeout}")
    private int connectionTimeOut;

    @Value("${request.timeout}")
    private int requestTimeOut;

    @Value("${read.timeout}")
    private int readTimeOut;

    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        CloseableHttpClient httpClient = HttpClients.custom().build();
        requestFactory.setHttpClient(httpClient);
        requestFactory.setConnectionRequestTimeout(requestTimeOut);
        requestFactory.setConnectTimeout(connectionTimeOut);
        requestFactory.setReadTimeout(readTimeOut);
        return new RestTemplate(requestFactory);
    }

}
