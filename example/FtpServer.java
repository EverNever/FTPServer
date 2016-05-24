import java.io.*;
import java.net.*;
import java.util.*;

public class FtpServer
{

	private int counter;                //记录当前登入服务器的用户个数
	public static String initDir;
	public static ArrayList users = new ArrayList();      //当前的用户数组
	public static ArrayList usersInfo = new ArrayList();  //用户详细信息数组
	
	public FtpServer()
	{
		// 启动一个线程，专门用于接收服务器端的指令；
		FtpConsole fc = new FtpConsole();
		fc.start();

		// 从本地文件中载入所有用户信息到 UserInfo 数组中；
		loadUsersInfo();

		// 显示欢迎词，您是第 Counter 个新用户；
		int counter = 1;
		int i = 0;
		try
		{

			//监听21号端口
			ServerSocket s = new ServerSocket(21);
			for(;;)
			{
				//接受客户端请求
				Socket incoming = s.accept();
				BufferedReader in = new BufferedReader(new InputStreamReader(incoming.getInputStream()));
			    PrintWriter out = new PrintWriter(incoming.getOutputStream(),true);
				out.println("220 Service ready for new user,已登录用户数："+counter);

				//为新登录的用户创建服务线程
				FtpHandler h = new FtpHandler(incoming,i);
				h.start();

				// 在当前用户的数组 users 中增加一项。
				// 注意：users 中保存的是线程对象，而非 UserInfo 对象。
				users.add(h);
				counter++;
				i++;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

	} // FtpServer() end
	


	public void loadUsersInfo()   //从本地user.cfg文件中读取用户的信息
	{
		String s = getClass().getResource("user.cfg").toString();
		s = s.substring(6,s.length());
		int p1 = 0;
		int p2 = 0;

		// 打开文件，将文件中的用户信息读至 UserInfo 数组中
		if(new File(s).exists())
		{
			try
			{
				BufferedReader fin = new BufferedReader(new InputStreamReader(new FileInputStream(s)));
				String line;
				String field;

				int i = 0;
				while((line = fin.readLine())!=null)  //从user.cfg逐行读入用户信息
				{
					UserInfo tempUserInfo = new UserInfo();
					p1 = 0;
					p2 = 0;
					i = 0;
					while((p2 = line.indexOf("|",p1))!=-1) 
					{
						field = line.substring(p1,p2);
						p2 = p2 +1;
						p1 = p2;
						switch(i)            //截取字符串中的不同值放到新建的用户信息中
						{
							case 0:
								tempUserInfo.user = field;    
								//System.out.println(tempUserInfo.user);
								break;
							case 1:
								tempUserInfo.password = field;
								//System.out.println(tempUserInfo.password);
								break;
							case 2:
								tempUserInfo.workDir = field;
								//System.out.println(tempUserInfo.workDir);
								break;
						}
						i++;
					} //while((p2 = line.indexOf("|",p1))!=-1) end
					usersInfo.add(tempUserInfo);
				}//while((line = fin.readLine())!=null) end
				fin.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}//if(new File(s).exists()) end
	}// loadUsersInfo() end
	
	public static void main(String[] args)  
	{
		if(args.length != 0) 
		{
			initDir = args[0];    
		}
		else
		{ 
			initDir = "c:/";	   
		}
		FtpServer ftpServer = new FtpServer();

	} // main end
}

class FtpHandler extends Thread    //Ftp服务器管理（监听客户端输入的指定）
{
	Socket csocket;
	Socket dsocket;
	int id;
	String cmd = "";
	String param = "";
	String user;
	String remoteHost = " ";
	int remotePort = 0;
	String dir = FtpServer.initDir;
	String rootdir = "c:/";
	int state = 0;
	String reply;
	PrintWriter out; 
	int type = FtpState.FTYPE_IMAGE;
	String requestfile = "";
	boolean isrest = false;

	int parseInput(String s)  //根据输入的指定返回一个整数（用于调用具体方法时的识别）
	{
		int p = 0;
		int i = -1;

		// 判断是否有参数，如果有，则将参数放置在Param变量中，将命令放置在cmd变量中
		p = s.indexOf(" ");
		if(p == -1)
			cmd = s;
		else 
			cmd = s.substring(0,p);

		if(p >= s.length() || p ==-1)
			param = "";
		else
			param = s.substring(p+1,s.length());
		cmd = cmd.toUpperCase();

		//命令放置在cmd变量中，参数放置在Param变量中
		if(cmd.equals("USER"))
				i = 1;
		if(cmd.equals("PASS"))
				i = 2;
		if(cmd.equals("ACCT"))
				i = 3;
	  	if(cmd.equals("CDUP"))	
				i = 4;
		if(cmd.equals("SMNT"))
				i = 5;
		if(cmd.equals("CWD"))	
				i = 6;
		if(cmd.equals("QUIT"))	// 退出系统
				i = 7;
	  	if(cmd.equals("REIN"))
				i = 8;
		if(cmd.equals("PORT"))	
				i = 9;
		if(cmd.equals("PASV"))  //进入被动传输方式
				i = 10;
		if(cmd.equals("TYPE"))	// 查看状态模式
				i = 11;
	  	if(cmd.equals("STRU"))
				i = 12;
		if(cmd.equals("MODE"))
				i = 13;
		if(cmd.equals("RETR"))	
				i = 14;
		if(cmd.equals("STOR"))	
				i = 15;
	  	if(cmd.equals("STOU"))
				i = 16;
		if(cmd.equals("APPE"))
				i = 17;
		if(cmd.equals("ALLO"))
				i = 18;
		if(cmd.equals("REST"))
				i = 19;
	  	if(cmd.equals("RNFR"))
				i = 20;
		if(cmd.equals("RNTO"))
				i = 21;
		if(cmd.equals("ABOR"))	
				i = 22;
		if(cmd.equals("DELE"))	// 删除文件
				i = 23;
	  	if(cmd.equals("RMD"))	//删除目录
				i = 24;
		if(cmd.equals("XMKD"))	
				i = 25;				
		if(cmd.equals("MKD"))  //创建目录
				i = 25;
		if(cmd.equals("PWD"))  //显示远程主机的当前工作目录
				i = 26;
		if(cmd.equals("LIST"))	
				i = 27;
	  	if(cmd.equals("NLST"))
				i = 28;
		if(cmd.equals("SITE"))
				i = 29;
		if(cmd.equals("SYST"))
				i = 30;
		if(cmd.equals("HELP"))  //显示ftp内部命令cmd的帮助信息，
				i = 31;
	  	if(cmd.equals("NOOP"))
				i = 32;
		if(cmd.equals("XPWD"))
				i = 33;
	 return i;
	}//parseInput() end

	int validatePath(String s)  
	{
		File f = new File(s);
		if(f.exists() && !f.isDirectory())
		{
			String s1 = s.toLowerCase();
			String s2 = rootdir.toLowerCase();
			if(s1.startsWith(s2))
				return 1;
			else
				return 0;
		}
		f = new File(addTail(dir)+s);
		if(f.exists() && !f.isDirectory())
		{
			String s1 = (addTail(dir)+s).toLowerCase();
			String s2 = rootdir.toLowerCase();
			if(s1.startsWith(s2))
				return 2;
			else 
				return 0;
		}
		return 0;
	}// validatePath() end

	boolean checkPASS(String s) 
	{
		for(int i = 0; i<FtpServer.usersInfo.size();i++)
		{
			if(((UserInfo)FtpServer.usersInfo.get(i)).user.equals(user) && 
				((UserInfo)FtpServer.usersInfo.get(i)).password.equals(s))  //判断该用户是否存在
			{
				rootdir = ((UserInfo)FtpServer.usersInfo.get(i)).workDir;
				dir = ((UserInfo)FtpServer.usersInfo.get(i)).workDir;  
				return true;
			}
		}
		return false;
	}// checkPASS() end

	boolean commandUSER()  //响应客户端User指定，切换用户
	{
		if(cmd.equals("USER"))
		{
			reply = "331 User name okay, need password";
			user = param;
		  	state = FtpState.FS_WAIT_PASS;
			return false;
		}
		else
		{
			reply = "501 Syntax error in parameters or arguments";
			return true;
		}

	}//commandUser() end

	boolean commandPASS()  
	{
		if(cmd.equals("PASS"))
		{
			if(checkPASS(param))
			{
				reply = "230 User logged in, proceed";
				state = FtpState.FS_LOGIN;
				System.out.println("Message: user "+user+" Form "+remoteHost+"Login");
				System.out.print("->");
				return false;
			}
			else
			{
				reply = "530 Not logged in";
				return true;
			}
		}
		else
		{
			reply = "501 Syntax error in parameters or arguments";
			return true;
		}

	}//commandPass() end

	void errCMD()
	{
		reply = "500 Syntax error, command unrecognized";
	}	
	
	boolean commandCDUP() 
	{
		dir = FtpServer.initDir;
		File f = new File(dir);
		if(f.getParent()!=null &&(!dir.equals(rootdir)))
		{
			dir = f.getParent();
			reply = "200 Command okay";
		}
		else
		{
			reply = "550 Current directory has no parent";
		}
		
		return false;
	}// commandCDUP() end

	boolean commandCWD() 
	{
		File f = new File(param);
		String s = "";
		String s1 = "";
		if(dir.endsWith("/"))
			s = dir;
		else
			s = dir + "/";
		File f1 = new File(s+param);
		
		if(f.isDirectory() && f.exists())
		{
			if(param.equals("..") || param.equals("..\\"))
			{
				if(dir.compareToIgnoreCase(rootdir)==0)
				{
					reply = "550 The directory does not exists";
					//return false;
				}
				else
				{
					s1 = new File(dir).getParent();
					if(s1!=null)
					{
						dir = s1;
						reply = "250 Requested file action okay, directory change to "+dir;
					}
					else
						reply = "550 The directory does not exists";
				}
			}
			else if(param.equals(".") || param.equals(".\\"))
			{
			}
			else 
			{
				dir = param;
				reply = "250 Requested file action okay, directory change to "+dir;
			}		
		}
		else if(f1.isDirectory() && f1.exists())
		{
			dir = s+param;
			reply = "250 Requested file action okay, directory change to "+dir;
		}
		else
			reply = "501 Syntax error in parameters or arguments";
		
		return false;
	} // commandCDW() end

	boolean commandQUIT() //实现QUIT指令，退出Ftp
	{
		reply = "221 Service closing control connection";
		return true;
	}// commandQuit() end

	boolean commandPORT()  
	{
		int p1 = 0;
		int p2 = 0;
		int[] a = new int[6];
		int i = 0;
		try
		{
			while((p2 = param.indexOf(",",p1))!=-1)
			{
				 a[i] = Integer.parseInt(param.substring(p1,p2));
				 p2 = p2+1;
				 p1 = p2;
				 i++;
			}
			a[i] = Integer.parseInt(param.substring(p1,param.length()));
		}
		catch(NumberFormatException e)
		{
			reply = "501 Syntax error in parameters or arguments";
			return false;
		}
		
		remoteHost = a[0]+"."+a[1]+"."+a[2]+"."+a[3];
		remotePort = a[4] * 256+a[5];
		reply = "200 Command okay";
		return false;
	}//commandPort() end	
	
	boolean commandLIST()  
	{
		try
		{
			dsocket = new Socket(remoteHost,remotePort,InetAddress.getLocalHost(),20);
			PrintWriter dout = new PrintWriter(dsocket.getOutputStream(),true);
			if(param.equals("") || param.equals("LIST"))
			{
				out.println("150 Opening ASCII mode data connection for /bin/ls. ");
				File f = new File(dir);
				String[] dirStructure = f.list();
				String fileType;
				for(int i =0; i<dirStructure.length;i++)
				{
					if(dirStructure[i].indexOf(".")!=-1)
					{
						fileType = "- ";
					}
					else
					{
						fileType = "d ";
					}
					dout.println(fileType+dirStructure[i]);
				}
			} 
			dout.close();
			dsocket.close();
			reply = "226 Transfer complete !";
		}
		catch(Exception e)
		{
			e.printStackTrace();
			reply = "451 Requested action aborted: local error in processing";
			return false;
		}
		
		return false;
	}// commandLIST() end

	boolean commandTYPE() //响应客户端TYPE指令,切换文件传输的方式，当输入"A"时为ASCII传输，当输入"I"时为二进制编码传输
	{
		if(param.equals("A"))
		{
			type = FtpState.FTYPE_ASCII;
			reply = "200 Command okay Change to ASCII mode";
		}
		else if(param.equals("I"))
		{
			type = FtpState.FTYPE_IMAGE;
			reply = "200 Command okay Change to BINARY mode";
		}
		else
			reply = "504 Command not implemented for that parameter";
			
		return false;
	}//commandTYPE() end

	boolean commandRETR() 
	{
		requestfile = param;
		File f =  new File(requestfile);
  		if(!f.exists())
		{
	  		f = new File(addTail(dir)+param);
			if(!f.exists())
			{
	   			reply = "550 File not found";
	   			return  false;
			}
			requestfile = addTail(dir)+param;
		}
  
  		if(isrest)
		{
     
		}
		else
		{
	 		if(type==FtpState.FTYPE_IMAGE)
			{
				try
				{
					out.println("150 Opening Binary mode data connection for "+ requestfile);
					dsocket = new Socket(remoteHost,remotePort,InetAddress.getLocalHost(),20);
    				BufferedInputStream  fin = new BufferedInputStream(new FileInputStream(requestfile));
	  				PrintStream dout = new PrintStream(dsocket.getOutputStream(),true);
					byte[] buf = new byte[1024];
					int l = 0;
					while((l=fin.read(buf,0,1024))!=-1)
					{
			  			dout.write(buf,0,l);
					}
		 			fin.close();
     				dout.close();
		 			dsocket.close();
		 			reply ="226 Transfer complete !";

				}
				catch(Exception e)
				{
					e.printStackTrace();
					reply = "451 Requested action aborted: local error in processing";
					return false;
				}

			}
			if(type==FtpState.FTYPE_ASCII)
			{
	  			try
				{
					out.println("150 Opening ASCII mode data connection for "+ requestfile);
					dsocket = new Socket(remoteHost,remotePort,InetAddress.getLocalHost(),20);
    				BufferedReader  fin = new BufferedReader(new FileReader(requestfile));
	  				PrintWriter dout = new PrintWriter(dsocket.getOutputStream(),true);
					String s;
					while((s=fin.readLine())!=null)
					{
		   				dout.println(s);
					}
		 			fin.close();
     				dout.close();
		 			dsocket.close();
		 			reply ="226 Transfer complete !";
				}
				catch(Exception e)
				{
					e.printStackTrace();
					reply = "451 Requested action aborted: local error in processing";
					return false;
				}
			}
		}
  		return false;

	}//commandRETR() end

	boolean commandSTOR() 
	{
		if(param.equals(""))
		{
			reply = "501 Syntax error in parameters or arguments";
			return false;
		}
		requestfile = addTail(dir)+param;
		if(type == FtpState.FTYPE_IMAGE)
		{
			try
			{
				out.println("150 Opening Binary mode data connection for "+ requestfile);
				dsocket = new Socket(remoteHost,remotePort,InetAddress.getLocalHost(),20);
				BufferedOutputStream fout = new BufferedOutputStream(new FileOutputStream(requestfile));
				BufferedInputStream din = new BufferedInputStream(dsocket.getInputStream());
				byte[] buf = new byte[1024];
				int l = 0;
				while((l = din.read(buf,0,1024))!=-1)
				{
					fout.write(buf,0,l);
				}//while()
				din.close();
				fout.close();
				dsocket.close();
				reply = "226 Transfer complete !";
			}
			catch(Exception e)
			{
				e.printStackTrace();
				reply = "451 Requested action aborted: local error in processing";
				return false;
			}
		}
		if(type == FtpState.FTYPE_ASCII)
		{
			try
			{
				out.println("150 Opening ASCII mode data connection for "+ requestfile);
				dsocket = new Socket(remoteHost,remotePort,InetAddress.getLocalHost(),20);
				PrintWriter fout = new PrintWriter(new FileOutputStream(requestfile));
				BufferedReader din = new BufferedReader(new InputStreamReader(dsocket.getInputStream()));
				String line;
				while((line = din.readLine())!=null)
				{
					fout.println(line);					
				}
				din.close();
				fout.close();
				dsocket.close();
				reply = " 226 Transfer complete !";
			}
			catch(Exception e)
			{
				e.printStackTrace();
				reply = "451 Requested action aborted: local error in processing";
				return false;
			}
		}
		return false;
	}//commandSTOR() end
	
	boolean commandPWD() // 响应客户端pwd指令，显示服务器上的当前目录
	{  
		reply = "257 " + dir + " is current directory.";
		return false;
	}//commandPWD() end
	
	boolean commandNOOP()  
	{
		reply = "200 OK.";
		return false;
	}//commandNOOP() end
	
	boolean commandABOR() 
	{
		try
		{
			dsocket.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			reply = "451 Requested action aborted: local error in processing";
			return false; 
		}
		reply = "421 Service not available, closing control connection";
		return false;
	}//commandABOR() end
	
	boolean commandDELE()  //响应DELE删除文件指令
	{
		int i = validatePath(param);
		if(i == 0)
		{
			reply = "550 Request action not taken";
	    	return false;
		}
		if(i == 1)
    	{
	    	File f = new File(param);
			f.delete();
    	}
		if(i == 2)
		{
			File f= new File(addTail(dir)+param);
			f.delete();
		} 
		
		reply = "250 Request file action ok,complete";
		return false;

	}//commandDELE() end

	boolean commandMKD() //响应客户端MKD指令,创建目录
	{
		String s1 = param.toLowerCase();
		String s2 = rootdir.toLowerCase();
		if(s1.startsWith(s2))
		{
			File f = new File(param);
			if(f.exists())
			{
				reply = "550 Request action not taken";
				return false;
			}
			else 
			{
				f.mkdirs();
				reply = "250 Request file action ok,complete";
			}
		}
		else 
		{
			File f = new File(addTail(dir)+param);
			if(f.exists())
			{
				reply = "550 Request action not taken";
				return false;
			}
			else 
			{
				f.mkdirs();
				reply = "250 Request file action ok,complete";
			}
		}
		
		return false;
	}//commandMKD() end

	String addTail(String s)
	{
		if(!s.endsWith("/"))
			s = s + "/";
		return s;
	}
	
	public FtpHandler(Socket s,int i) 
	{
		csocket = s;
		id = i;	
	}
	
	public void run()   //监听客户端输入的指令
	{
		String str = "";
		int parseResult;
		
		try
		{
			BufferedReader in = new BufferedReader
								(new InputStreamReader(csocket.getInputStream()));
			out = new PrintWriter
								(csocket.getOutputStream(),true);
			state  = FtpState.FS_WAIT_LOGIN;
			boolean finished = false;
			while(!finished)
			{
				str = in.readLine();
				if(str == null) finished = true;
				else
				{
					parseResult = parseInput(str);
					System.out.println("Command:"+cmd+" Parameter:"+param);
					System.out.print("->");
					switch(state)
					{
						case FtpState.FS_WAIT_LOGIN:
								finished = commandUSER();
								break;
						case FtpState.FS_WAIT_PASS:
								finished = commandPASS();
								break;
						case FtpState.FS_LOGIN:
						{
							switch(parseResult)
							{
								case -1:
									errCMD();
									break;
								case 4:
									finished = commandCDUP();
									break;
								case 6:
									finished = commandCWD();
									break;
								case 7:
									finished = commandQUIT();
									break;
								case 9:
									finished = commandPORT();
									break;
								case 27:
									finished = commandLIST();
									break;
								case 11:
									finished = commandTYPE();
									break;
								case 14:
									finished = commandRETR();
									break;
								case 15:
									finished = commandSTOR();
									break;
								case 26:
								case 33:
									finished = commandPWD();
									break;
								case 32:
									finished = commandNOOP();
									break;
								case 22:
									finished = commandABOR();
									break;
								case 23:
									finished = commandDELE();
									break;
								case 25:
									finished = commandMKD();
									break;

							}// switch(parseResult) end
						}// case FtpState.FS_LOGIN: end
						break;
						

					}// switch(state) end
				} // else
				out.println(reply);
			} //while
			csocket.close();
		} //try
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}

class FtpConsole extends Thread  //接受服务器指定的线程，根据传来的指定实现具体的响应
{
	BufferedReader cin;
	String conCmd;
	String conParam;
	
	int consoleQUIT() 
	{
		System.exit(0);
		return 0;
	}//consoleQUIT() end
	
	boolean consoleLISTUSER() //实现服务器端LISTUSER指令 列出所有用户信息
	{
		System.out.println("username \t\t workdirectory");
		for(int i = 0 ; i<FtpServer.usersInfo.size();i++)
		{
			System.out.println(((UserInfo)FtpServer.usersInfo.get(i)).user+" \t\t\t "+((UserInfo)FtpServer.usersInfo.get(i)).workDir);
		}
		return false;
	}//consoleLISTUSER() end
	
	boolean consoleLIST()  //实现服务器端List指令 列出已经连接的用户名和IP
	{
		int i = 0;
  		for(i=0;i<FtpServer.users.size();i++)
		{
			System.out.println((i+1)+":"+((FtpHandler)(FtpServer.users.get(i))).user + " From " +((FtpHandler)(FtpServer.users.get(i))).csocket.getInetAddress().toString());
		}

  	    return false;
	}//consoleLIST() end
	
	boolean validateUserName(String s) //判断用户是否已经存在
	{
		for(int i = 0 ; i<FtpServer.usersInfo.size();i++)
		{
			if(((UserInfo)FtpServer.usersInfo.get(i)).user.equals(s))
				return false;	
		}
		return true;
	}//validateUserName() end

	boolean consoleADDUSER()  //服务器端增加一用户
	{
		System.out.print("please enter username:");
		try
		{
			cin = new BufferedReader(new InputStreamReader(System.in));
			UserInfo tempUserInfo = new UserInfo();
			String line = cin.readLine();
			if(line != "")
			{
				if(!validateUserName(line))     //判断用户是否存在
				{
					System.out.println("user "+line+" already exists!");
					return false;               //当用户已经存在时告诉用户该用户已经存在，注册失败	                              
				}
			}
			else
			{
				System.out.println("username cannot be null!");
				return false;
			}
			tempUserInfo.user = line;
			System.out.print("enter password :");
			line = cin.readLine();
			if(line != "")
				tempUserInfo.password = line;  
			else
			{
				System.out.println("password cannot be null!");
				return false;
			}
			System.out.print("enter the initial directory: ");
			line = cin.readLine();
			if(line != "")
			{
				File f = new File(line);
				if(!f.exists())
					f.mkdirs();
				tempUserInfo.workDir = line;
			}
			else
			{
				System.out.println("the directory cannot be null!");
				return false;
			}
			FtpServer.usersInfo.add(tempUserInfo);
			saveUserInfo();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return false;
	}//consoleADDUSER() end
	
	void saveUserInfo()  //在创建新用户时把用户信息保存到user.cfg文件中
	{
		String s = "";
		try
		{
			BufferedWriter fout = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("user.cfg")));
			for(int i = 0; i < FtpServer.usersInfo.size();i++)
			{
				s = ((UserInfo)FtpServer.usersInfo.get(i)).user+"|"+((UserInfo)FtpServer.usersInfo.get(i)).password+"|"+((UserInfo)FtpServer.usersInfo.get(i)).workDir+"|";
				fout.write(s);    //把username,password,和操作文件域拼成字符串，中间用"|"隔开
				fout.newLine();
			}
			fout.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}//saveUserInfo() end
	
	boolean consoleDELUSER()   //在服务器端删除一用户信息
	{
		String s = "";
		//System.out.println(conParam);
		if(conParam.equals(""))
		{
			System.out.println("usage:deluser username");
			return false;      //在没输入用户名的情况下，提示用户操作
		}
		for(int i=0;i<FtpServer.usersInfo.size();i++)
		{
			s = ((UserInfo)FtpServer.usersInfo.get(i)).user;
			if(s.equals(conParam))
			{
				System.out.println("User "+conParam+" deleted");
                FtpServer.usersInfo.remove(i);
				saveUserInfo();
				return false;
			}
		}
		System.out.println("User "+conParam+" not exists");					
		return false;

	}//consoleDELUSER() end
	
	boolean consoleHELP()   //实现服务器端Help指令，列出各个指令
	{
		if(conParam.equals(""))
		{
			System.out.println("adduser :add new user");
			System.out.println("deluser <username> :delete a user");
			System.out.println("quit  :quit");
			System.out.println("list  :list all user connect to server");
			System.out.println("listuser : list all account of this server");
			System.out.println("help :show  this help");
		}
		else if(conParam.equals("adduser"))
			System.out.println("adduser :add new user");
		else if(conParam.equals("deluser"))
			System.out.println("deluser <username> :delete a user");
		else if(conParam.equals("quit"))
			System.out.println("quit  :quit");
		else if(conParam.equals("list"))
			System.out.println("list  :list all user connect to server");
		else if(conParam.equals("listuser"))
			System.out.println("listuser : list all account of this server");
		else if(conParam.equals("help"))
			System.out.println("help :show  this help");
		else
			return false;
		return false;
		
	}//consoleHELP() end
	
	boolean consoleERR()    //当操作错误时提示"bad command!"
	{
		System.out.println("bad command!");
		return false;
	}//consoleERR() end

	public FtpConsole()     //当启动服务器时提示"ftp server started!"
	{
		System.out.println("ftp server started!");
		cin = new BufferedReader(new InputStreamReader(System.in));
	}
	public void run()   //启动线程 监听服务器端的指令
	{
		boolean ok = false;
		String input = "";
		while(!ok)
		{
			System.out.print("->");
			try
			{
				input = cin.readLine(); 
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			switch(parseInput(input))  //当输入指令返回不同的数值时执行不同的方法
			{
				case 1:
					consoleQUIT();   
					break;
				case 8:
					ok = consoleLISTUSER();
					break;
				case 0:
					ok = consoleLIST();
					break;
				case 2:
					ok = consoleADDUSER();
					break;
				case 3:
				 	ok = consoleDELUSER();
				 	break;
				 case 7:
				 	ok = consoleHELP();
				 	break;
				 case -1:
				 	ok = consoleERR();
				 	break;
			}
		}//while end
	}// run() end

	int parseInput(String s)    //当输入不同的指令时返回不同的整数值（用于调用具体方法时的识别）
	{
		String upperCmd;
		int p = 0;
		conCmd = "";
		conParam = "";
		p = s.indexOf(" ");
		if(p == -1)             
			conCmd = s;
		else 
			conCmd = s.substring(0,p);  //当输入参数不为空时把参数赋给变量conCmd

		if(p >= s.length() || p ==-1)
			conParam = "";
		else
			conParam = s.substring(p+1,s.length());
		upperCmd = conCmd.toUpperCase();

        //根据不同的输入参数返回不同的数值，根据不同的数值再调用相映的方法

		if(upperCmd.equals("LIST"))
			return 0;
		else if(upperCmd.equals("QUIT")||upperCmd.equals("EXIT"))
			return 1;
		else if(upperCmd.equals("ADDUSER"))
			return 2;
		else if(upperCmd.equals("DELUSER"))
			return 3;
		else if(upperCmd.equals("EDITUSER"))
			return 4;
		else if(upperCmd.equals("ADDDIR"))
			return 5;
		else if(upperCmd.equals("REMOVEDIR"))
			return 6;
		else if(upperCmd.equals("HELP") ||upperCmd.equals("?"))
			return 7;
		else if(upperCmd.equals("LISTUSER"))
			return 8;						
		return -1;
	}// parseInput end
}

class FtpState
{
	final static int FS_WAIT_LOGIN = 0;
	final static int FS_WAIT_PASS = 1;
	final static int FS_LOGIN = 2;
	final static int FTYPE_ASCII = 0;
	final static int FTYPE_IMAGE  = 1;
	final static int FMODE_STREAM = 0;
	final static int FMODE_COMPRESSED = 1;
	final static int FSTRU_FILE = 0;
	final static int FSTRU_PAGE = 1;
}

class UserInfo
{
	String user;
	String password;
	String workDir;
}
