package cl.bice.createStructure.to.create;

import lombok.Getter;

import java.util.stream.Stream;

@Getter
public enum ParameterType {

    PATH,
    QUERY,
    HEADER,
    NONE;

	public static Stream<ParameterType> stream() {
		return Stream.of(ParameterType.values());
	}
	
	

    public static ParameterType getParameterType(String codigo) {
        return ParameterType.stream()
                .filter(d -> codigo.equals(d.name()))
                .findFirst()
                .orElse(null);

    }
}
