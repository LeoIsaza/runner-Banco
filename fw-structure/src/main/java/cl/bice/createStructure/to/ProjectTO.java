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
public class ProjectTO implements Serializable {

	private static final long serialVersionUID = -3060069143036251491L;

	private String key;
}
