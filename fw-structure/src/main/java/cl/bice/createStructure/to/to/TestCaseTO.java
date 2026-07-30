package cl.bice.createStructure.to.to;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.json.JSONObject;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestCaseTO {

    private String idAzure;

    private String feature;

    private String testCase;

    private HeaderTO header;
    private JSONObject headers;

    private String campo;

    private String  valor;

    private int status;

    private JSONObject response;

}
