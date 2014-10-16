package orgKTNet;

import java.awt.Color;

import repast.simphony.space.graph.RepastEdge;
import repast.simphony.visualizationOGL2D.EdgeStyleOGL2D;

public class TieStyle implements EdgeStyleOGL2D {

	@Override
	public int getLineWidth(RepastEdge<?> edge) {
		return (int)((edge.getWeight()*0.01));
		//return (int)((edge.getWeight()*0.5));
	}

	@Override
	public Color getColor(RepastEdge<?> edge) {
		if(edge.getWeight() > 5)
			return Color.GREEN;
		else
			return Color.GRAY;
	}

}
