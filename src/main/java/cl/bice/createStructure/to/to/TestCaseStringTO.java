package cl.bice.createStructure.to.to;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TestCaseStringTO {

    private String idAzure;

    private String feature;

    private String testCase;

    private HeaderTO header;

    private String campo;

    private String valor;

    private int status;

    private String codigo;

    private String mensaje;

}
