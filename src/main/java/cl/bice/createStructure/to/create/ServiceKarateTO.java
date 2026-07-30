package cl.bice.createStructure.to.create;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.io.Serializable;
import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceKarateTO implements Serializable {

    private String name;
    private String tagName;
    private String description;
    /** true si la API es de tipo OBAPI (Oracle Banking), false si es Services/BaaS */
    private boolean isObapi;
    /** Clave para env_qa.json, ej: "api_baas" o "obapi_qa" */
    private String envKey;
    /** Origen del host, ej: "https://mi-servicio-qa.midominio.com" */
    private String origin;
    private List<OperationTO> operations;
}
