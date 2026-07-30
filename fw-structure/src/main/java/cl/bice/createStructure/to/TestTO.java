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
public class TestTO implements Serializable {

	private static final long serialVersionUID = -3773159618627218335L;

	private String key;
	private int line;
	private IssueTO issueTO;
	private String urlAzure;

}
