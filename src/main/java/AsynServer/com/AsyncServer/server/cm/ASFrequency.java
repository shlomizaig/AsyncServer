package AsynServer.com.AsyncServer.server.cm;



import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.async.AsyncClient;
import com.aerospike.client.listener.RecordListener;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.WritePolicy;

public class ASFrequency implements RecordListener {
	
	public interface IASFrequencyReply{
		public abstract void OnReply(String data);
	}
	
	private String _ip;
	private String _userID;
	private IASFrequencyReply _replier;
	private static int _port = 3000;
	private static String _host = "108.168.168.117";//"10.0.0.113";
	private static AsyncClient client = null;
	
	
	static void Init()
	{
		try{
		   client = new AsyncClient(_host,_port);
		} catch (Exception e) {
			
		}
	}
	
	static void Exit()
	{
		try{
			if( client !=null )
				client.close();
			client = null;
		}catch (Exception e) {	}
			
	}
	
	public static long ipToInt(final String addr) {
		if(addr==null){
		//	logger.info("wrong ip: addr is null");
			return 0;
		}
		final String[] addressBytes = addr.trim().split("\\.");

		if(addressBytes.length!=4){
	//		logger.info("wrong ip:"+addr);
			return 0;
		}

		long ip = 0;
		for (int i = 0; i < 4; i++) {
			ip <<= 8;
			try {
				ip |= Long.parseLong(addressBytes[i]);
			} catch (final NumberFormatException e) {
		//		logger.error("wrong ip:"+addr,e);
				return 0;
			}
		}
		return ip;
    }
	
	public ASFrequency(String IP, String userID,IASFrequencyReply replier)
	{
		_replier = replier;
		long l = ipToInt(IP);
		_ip = Long.toString(1808451697);
		_userID = userID;
	}
	
	void AsyncFetchData()
	{
		try {
			if( client == null )
			    return;
				
			ClientPolicy policy = new ClientPolicy();
			policy.timeout = 500;
			 
	
		    WritePolicy wpolicy = new WritePolicy();
            wpolicy.timeout = 500;  
            
            
		    
            // Write a single value.  
            Key key = new Key( "IpFrequency","IP_FREQUENCY", Long.parseLong(_ip));
                        
            client.get(client.asyncReadPolicyDefault, this ,key,"" );
	                        

	            
	        }catch (Exception e) {  
	        	e.printStackTrace();
	        	        
	        }
		
		
	}

	
	public void onFailure(AerospikeException arg0) {
		_replier.OnReply("none");
		
		
	}

	public void onSuccess(Key arg0, Record arg1) {
		try{
		    if( arg1 == null)
		    	_replier.OnReply("none");
		    else
		    	_replier.OnReply(arg1.getValue("").toString());
		}catch(Exception E)
		{
			_replier.OnReply("crash"); 
		}
	
	}	
}

