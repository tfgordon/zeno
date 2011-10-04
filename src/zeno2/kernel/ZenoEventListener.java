package zeno2.kernel;

import java.util.EventListener;

/**  Listener associated to ZenoEvents according to Java Bean conventions 
 See "Java in a Nutshell, 3d Edition", page 185. */

public interface ZenoEventListener extends java.util.EventListener {

	public void handleEvent(ZenoEvent event);

}