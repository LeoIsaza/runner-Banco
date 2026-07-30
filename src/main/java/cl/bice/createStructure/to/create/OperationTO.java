package cl.bice.createStructure.to.create;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OperationTO implements Serializable {

    private String operationId;
    private String path;
    private HttpMethod httpMethod;
    private List<ParameterTO> parameters;
    private RequestBodyTO requestBody;
    private RequestBodyTO requestFuntionalBody;
    private RequestBodyTO headers;
    private List<ResponseBodyTO> responsesBodyTO;
}
