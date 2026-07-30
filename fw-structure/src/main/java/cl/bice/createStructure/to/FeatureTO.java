/**
 * 
 */
package cl.bice.createStructure.to;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import cl.bice.createStructure.to.to.TestCaseTO;
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
public class FeatureTO implements Serializable {

	private static final long serialVersionUID = 261581948609334166L;

	private File file;
	private List<TestCaseTO> testCases;

}
