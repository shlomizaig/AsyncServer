package AsynServer.com.AsyncServer.server.cm;




import java.util.List;
import java.util.Map;
import java.util.Set;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;


/**
 * Handles a server-side channel.
 */
class HttpSnoopServerHandler extends SimpleChannelInboundHandler<Object>  { // (1)

	private HttpRequest request;
	/** Buffer that stores the response content */
	private final StringBuilder buf = new StringBuilder();
	private boolean _exit = false;
	private int _delay = -1;
	private boolean _handled = false;
	  
	private ChannelHandlerContext _ctx;
	
	void logReq(HttpRequest request)
	{
		buf.setLength(0);
		buf.append("WELCOME TO THE WILD WILD WEB SERVER\r\n");
	    buf.append("===================================\r\n");
	    buf.append("VERSION: ").append(request.getProtocolVersion()).append("\r\n");
	    buf.append("HOSTNAME: ").append(HttpHeaders.getHost(request, "unknown")).append("\r\n");
	    buf.append("REQUEST_URI: ").append(request.getUri()).append("\r\n\r\n");
	}
	
	void logHeaders(HttpHeaders headers)
	{
		if (!headers.isEmpty()) {
            for (Map.Entry<String, String> h: headers) {
                String key = h.getKey();
                String value = h.getValue();
                buf.append("HEADER: ").append(key).append(" = ").append(value).append("\r\n");
            }
            buf.append("\r\n");
        }
	}
	
	
	void logReqParams(String uri)
	{
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
        Map<String, List<String>> params = queryStringDecoder.parameters();
        if (!params.isEmpty()) {
            for (Map.Entry<String, List<String>> p: params.entrySet()) {
                String key = p.getKey();
                List<String> vals = p.getValue();
                if( key.contains("exit"))
              	  _exit =true;
                if( key.startsWith("delay") )
              	  _delay = Integer.parseInt(vals.get(0));
                for (String val : vals) {
                    buf.append("PARAM: ").append(key).append(" = ").append(val).append("\r\n");
                }
            }
            buf.append("\r\n");
        }

	}
	
	void logContent(HttpContent httpContent)
	{
	    ByteBuf content = httpContent.content();
        if (content.isReadable()) {
            buf.append("CONTENT: ");
            buf.append(content.toString(CharsetUtil.UTF_8));
            buf.append("\r\n");
            appendDecoderResult(buf, request);
        }
	}
	
	private static void appendDecoderResult(StringBuilder buf, HttpObject o) {
	    DecoderResult result = o.getDecoderResult();
	    if (result.isSuccess()) {
	          return;
	    }
	 
	   buf.append(".. WITH DECODER FAILURE: ");
	   buf.append(result.cause());
	   buf.append("\r\n");
    }
	
	void logTrailing(LastHttpContent trailer)
	{
        buf.append("END OF CONTENT\r\n");
        if (!trailer.trailingHeaders().isEmpty()) {
            buf.append("\r\n");
            for (String name: trailer.trailingHeaders().names()) {
                for (String value: trailer.trailingHeaders().getAll(name)) {
                    buf.append("TRAILING HEADER: ");
                    buf.append(name).append(" = ").append(value).append("\r\n");
                }
            }
            buf.append("\r\n");
        }

	}
	void logMyParam()
	{
		if( _delay > 0 )
        	buf.append(">>>> Delay ").append(_delay).append(" ms");
        
		if( _exit )
        	buf.append("============== BYE ======================");
	}
	
	boolean HandleResponseHeaders(FullHttpResponse response)
	{
        boolean keepAlive = isKeepAlive(request);
        if (keepAlive) {
            // Add 'Content-Length' header only for a keep-alive connection.
            response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes());
            // Add keep alive header as per:
            // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
            response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }
        
        response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
        
        
        // Encode the cookie.
        /* String cookieString = request.headers().get(HttpHeaders.Names.COOKIE);
        if (cookieString != null) {
            Set<Cookie> cookies = ServerCookieDecoder.STRICT.decode(cookieString);
            if (!cookies.isEmpty()) {
                // Reset the cookies if necessary.
                for (Cookie cookie: cookies) {
                    response.headers().add(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));
                }
            }
        } else {
            // Browser sent no cookie.  Add some.
            response.headers().add(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.STRICT.encode("key1", "value1"));
            response.headers().add(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.STRICT.encode("key2", "value2"));
        }*/
        
       return keepAlive;
	}
	
	boolean hasHandler(ChannelHandlerContext ctx,String URI,ByteBuf bbuf)
	{
		if( _handled )
			return true;
		_handled = true;
		
		_ctx = ctx;
		if( URI.contains("ping"))
		{
			writeResponse("PONG", ctx, HttpResponseStatus.OK );
			return true;
		}
		
		ASFrequency asf= new ASFrequency("109.67.22.235","123456",new ASFrequency.IASFrequencyReply() {
			
			public void OnReply(String data) {
				
				class RunWithReply implements Runnable {
					private String _finalData;
					public RunWithReply(String data)
					{
						_finalData = data;
					}
					public void run()
					{
						writeResponse("IP data:" + _finalData, _ctx, HttpResponseStatus.OK);
						_ctx.flush();
					}
				}
				
				_ctx.executor().execute(new RunWithReply(data));
				
								
	          			
			}
		} );
		
		asf.AsyncFetchData();
		return true;
		
		//		return false;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
	         if (msg instanceof HttpRequest) {
	             HttpRequest request = this.request = (HttpRequest) msg;
	  
	              if (HttpHeaders.is100ContinueExpected(request)) {
	                  send100Continue(ctx);
	              }
	  
	              logReq(request);
	              logHeaders(request.headers());
	
	              logReqParams(request.getUri());
	                            
	              appendDecoderResult(buf, request);
	          }
	  
	         
	         if (msg instanceof HttpContent) {
	             HttpContent httpContent = (HttpContent) msg;
	             logContent(httpContent);
	             
	             if (msg instanceof LastHttpContent) {
	 
	                 LastHttpContent trailer = (LastHttpContent) msg;
	                 
	                 logTrailing(trailer);
	                 logMyParam();
	                 
	                 if( !hasHandler(ctx,request.getUri(),httpContent.content()))
	                	  if (!writeTestResponse(trailer, ctx)) {
	                     // If keep-alive is off, close the connection once the content is fully written.
	                		  ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
	                 }
	             }
	         }
	     }
	
	 public static boolean isKeepAlive(HttpRequest message) {
	        CharSequence connection = message.headers().get(HttpHeaders.Names.CONNECTION);
	        if (connection != null && HttpHeaders.Values.CLOSE.equalsIgnoreCase((String)connection) ) {
	            return false;
	        }

	        if (message.getProtocolVersion().isKeepAliveDefault()) {
	            return !HttpHeaders.Values.CLOSE.equalsIgnoreCase((String)connection);
	        } else {
	            return HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase((String)connection);
	        }
	}
	 
	 
	private boolean writeResponse(String strReply,ChannelHandlerContext ctx,HttpResponseStatus hstatus)
	{
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,hstatus ,
                Unpooled.copiedBuffer(strReply, CharsetUtil.UTF_8));

        
        
        boolean ret = HandleResponseHeaders(response);
        
        ctx.write(response).addListener(new ChannelFutureListener() {
        	public void operationComplete(ChannelFuture f) throws Exception {
        		if( _exit )
                	f.channel().parent().close();	          		
        	}
		});
        return ret;
	}
	
	
	 
	private boolean writeTestResponse(HttpObject currentObj, ChannelHandlerContext ctx) {
        
		 if( _delay > 0 )
	        	Worker.DoHardWorkFor(_delay);
	        
		HttpResponseStatus hstatus = currentObj.getDecoderResult().isSuccess()? HttpResponseStatus.OK : HttpResponseStatus.BAD_REQUEST;
        return writeResponse(buf.toString(),ctx,hstatus);                               
        
    }

	
	
	
	
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
	          ctx.flush();
	}

    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
        ctx.write(response);
    } 
    
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}


class HttpSnoopServerInitializer extends ChannelInitializer<SocketChannel> {

    public HttpSnoopServerInitializer() {
        
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        
        p.addLast(new HttpRequestDecoder());
        // Uncomment the following line if you don't want to handle HttpChunks.
        //p.addLast(new HttpObjectAggregator(1048576));
        p.addLast(new HttpResponseEncoder());
        //p.addLast(new HttpObjectAggregator(4000)); //~4k
        // Remove the following line if you don't want automatic content compression.
        //p.addLast(new HttpContentCompressor());
        p.addLast(new HttpSnoopServerHandler());
    }
}



/**
 * Discards any incoming data.
 */
public class CMServer {

    private int port;
    static final private EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
    static final private EventLoopGroup workerGroup = new NioEventLoopGroup();
    
    public CMServer(int port) {
        this.port = port;
    }

    public void run() throws Exception {
        
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class) // (3)
           //  .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
           //      @Override
           //      public void initChannel(SocketChannel ch) throws Exception {
           //          ch.pipeline().addLast(new DiscardServerHandler());
           //      }
           //  })
             .childHandler(new HttpSnoopServerInitializer())
             .option(ChannelOption.SO_BACKLOG, 128)          // (5)
             .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(port).sync(); // (7)

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } catch(Exception e){ 
           System.out.println(e.getMessage());
        }finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    
    
    public static void main(String[] args) throws Exception {
        int port;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 5555;
        }
        
        
        //All init here
        ASFrequency.Init();
                		
        new CMServer(port).run();
        
        
        //All Exit here
        ASFrequency.Exit();
    }
}

