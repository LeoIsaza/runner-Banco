/**
 * 
 */
package cl.bice.createStructure.to;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

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
public class FieldTO implements Serializable {

	private static final long serialVersionUID = 8858857343905900189L;

	private ProjectTO project;

	private String summary;

	private String description;

	private IssuetypeTO issuetype;

	@JsonProperty("customfield_10506")
	private CustomfieldTO customfield10506;

	@JsonProperty("customfield_10507")
	private CustomfieldTO customfield10507;

	@JsonProperty("customfield_11105")
	private CustomfieldTO customfield11105;

	private PriorityTO priority;

	private List<String> labels;

}
