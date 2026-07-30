/**
 * 
 */
package cl.bice.createStructure.to;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class ResponseAzureTO implements Serializable {

	private static final long serialVersionUID = -7230737707748203020L;

	private String id;
	private String key;
	private String self;

}
