package cl.bice.createStructure.to.create;

import lombok.Getter;

import java.util.stream.Stream;

@Getter
public enum HttpMethod {

    GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE;

	public static Stream<HttpMethod> stream() {
		return Stream.of(HttpMethod.values());
	}
	
	

    public static HttpMethod getParameterType(String codigo) {
        return HttpMethod.stream()
                .filter(d -> codigo.equals(d.name()))
                .findFirst()
                .orElse(null);

    }
}
