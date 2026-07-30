package cl.bice.createStructure.to.create;

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
public class RestrictionTO implements Serializable {

    private String type;
    private List<String> values;
    private String format;
    private Integer maxLength;
    private Integer minLength;
    private String pattern;
    private boolean numberString;
    private String example;
}
