package orgKTNet;

import java.awt.Color;
import java.awt.Font;

import repast.simphony.context.Context;
import repast.simphony.context.space.graph.ContextJungNetwork;
import repast.simphony.engine.environment.RunState;

import repast.simphony.visualizationOGL2D.StyleOGL2D;
import saf.v3d.ShapeFactory2D;
import saf.v3d.scene.Position;
import saf.v3d.scene.VSpatial;

public class AgentStyle implements StyleOGL2D<OrgMember> {

    private ShapeFactory2D factory;
    
    @Override
    public void init(ShapeFactory2D factory) {
            this.factory = factory;
    }


  /*
   * (non-Javadoc)
   * 
   * @see
   * repast.simphony.visualizationOGL2D.StyleOGL2D#getBorderColor(java.lang.
   * Object)
   */
  public Color getBorderColor(OrgMember object) {
    return Color.BLACK;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * repast.simphony.visualizationOGL2D.StyleOGL2D#getBorderSize(java.lang.Object
   * )
   */
  public int getBorderSize(OrgMember object) {
    return 0;
  }


  /*
   * (non-Javadoc)
   * 
   * @see
   * repast.simphony.visualizationOGL2D.StyleOGL2D#getColor(java.lang.Object)
   */
  public Color getColor(OrgMember o) {
	  //Color customColor = new Color(o.getSCPreference()*255, o.getNDPreference()*255, o.getRBPreference()*255);
	  //return customColor;
	  //if(o.idle) return Color.GRAY;
	  //else{
		  if(o.getSCPreference()==1 && o.getNDPreference()==1)
			  return Color.BLACK;
		  else if(o.getSCPreference()==1 && o.getNDPreference()==0)
			  return Color.BLUE;
		  else if (o.getSCPreference()==0 && o.getNDPreference()==1)
			  return Color.GREEN;
		  else //o.getSCPreference()==0 && o.getNDPreference()==0
			return Color.RED;
	  //}
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * repast.simphony.visualizationOGL2D.StyleOGL2D#getRotation(java.lang.Object)
   */
  public float getRotation(OrgMember object) {
    return 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * repast.simphony.visualizationOGL2D.StyleOGL2D#getScale(java.lang.Object)
   */
  public float getScale(OrgMember object) {
    return 1f;
  }

  /*
   * Note that the existing VSpatial for the agent is passed in.
   * The first time this is called the spatial will be null and we have to return a VSpatial.
   * If we don't want to change the VSpatial in subsequent calls then we can just return null or the passed in spatial. 
   */
  public VSpatial getVSpatial(OrgMember o, VSpatial spatial) {
	  //if(spatial == null){
		  Context<Object> context = RunState.getInstance().getMasterContext();
		  ContextJungNetwork<OrgMember> net = (ContextJungNetwork<OrgMember>) context.getProjection("socialNetwork");
		  spatial = factory.createCircle((float) (1 + net.getDegree(o)*0.5), 20, true);
	  //}
	  return spatial;
  }


@Override
public String getLabel(OrgMember object) {
	// TODO Auto-generated method stub
	return null;
}


@Override
public Font getLabelFont(OrgMember object) {
	// TODO Auto-generated method stub
	return null;
}


@Override
public float getLabelXOffset(OrgMember object) {
	// TODO Auto-generated method stub
	return 0;
}


@Override
public float getLabelYOffset(OrgMember object) {
	// TODO Auto-generated method stub
	return 0;
}


@Override
public Position getLabelPosition(OrgMember object) {
	// TODO Auto-generated method stub
	return null;
}


@Override
public Color getLabelColor(OrgMember object) {
	// TODO Auto-generated method stub
	return null;
}

}

