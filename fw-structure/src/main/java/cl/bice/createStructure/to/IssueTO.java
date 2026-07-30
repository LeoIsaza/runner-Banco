/**
 * 
 */
package cl.bice.createStructure.to;

import java.io.Serializable;

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
public class IssueTO implements Serializable {

	private static final long serialVersionUID = -6528440140818452194L;

	private FieldTO fields;

}
