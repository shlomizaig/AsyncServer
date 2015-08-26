package AsynServer.com.AsyncServer.server.cm;

import java.util.HashMap;

public class Worker {
    
	public static void DoHardWorkFor(int delay) {
		HashMap<String, String> s = new HashMap<String, String>();
		long currT = System.currentTimeMillis();
		double _value = Math.random()*Integer.MAX_VALUE;
		while( true)
		{
			_value = Math.sqrt(_value);
			_value = Math.exp(Math.random()*10);
			s.put(Integer.toString((int)_value), "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
			if( System.currentTimeMillis() - currT > (delay) )
				break;
			
		}
			
		
	}

}
