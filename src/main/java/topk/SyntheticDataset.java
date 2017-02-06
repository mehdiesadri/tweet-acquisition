package topk;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class SyntheticDataset implements Serializable {
	private String name;
	private List<EntityBlock> EntityBlocks = new ArrayList<EntityBlock>();
	private List<String> GroundTruth = new ArrayList<String>();

	public List<EntityBlock> getEntityBlocks() {
		return EntityBlocks;
	}

	public void setEntityBlocks(List<EntityBlock> entityBlocks) {
		EntityBlocks = entityBlocks;
	}

	public List<String> getGroundTruth() {
		return GroundTruth;
	}

	public void setGroundTruth(List<String> groundTruth) {
		GroundTruth = groundTruth;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
