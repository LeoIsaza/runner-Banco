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
public class IssuetypeTO implements Serializable {

	private static final long serialVersionUID = 7389800528573184678L;

	private String name;

}
