package cl.bice.createStructure.to.test;

import cl.bice.createStructure.to.to.HeaderTO;
import cl.bice.createStructure.to.to.ResponseTestTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TemplateCampo {

    private String feature;
    private String tipo;
    private String nombre;
    private int largo;
    private boolean negativo;
    private Integer status;
    private ResponseTestTO response;
    private HeaderTO header;
}
