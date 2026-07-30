package cl.bice.createStructure.to.to;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
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
public class HeaderTO {

    @SerializedName("trx-canal")
    @JsonProperty("trx-canal")
    private String trxCanal;

    @SerializedName("trx-id")
    @JsonProperty("trx-id")
    private String trxId;

    @SerializedName("trx-ip-origen")
    @JsonProperty("trx-ip-origen")
    private String trxIpOrigen;

    @SerializedName("trx-usuario")
    @JsonProperty("trx-usuario")
    private String trxUsuario;

    @SerializedName("x-rut-cliente")
    @JsonProperty("x-rut-cliente")
    private String xRutCliente;

    @SerializedName("x-rut-usuario")
    @JsonProperty("x-rut-usuario")
    private String xRutUsuario;

    @SerializedName("x-bice-partner-id")
    @JsonProperty("x-bice-partner-id")
    private String xBicePartnerId;

    @SerializedName("Content-Type")
    @JsonProperty("Content-Type")
    private String ContentType;

}
