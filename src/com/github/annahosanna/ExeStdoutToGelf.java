package com.github.annahosanna;

import java.io.InputStreamReader;
import java.io.BufferedReader;

import java.util.HashMap;
import java.util.Map;

import org.graylog2.GelfMessage;
import org.graylog2.GelfSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.net.UnknownHostException;


// Inspired by https://github.com/gurkanoluc/gelfj-tester

public class ExeStdoutToGelf {
	public static void main (String[] args) {
		
		int i = 0;
		
		Map<String, String> logLevels = new HashMap<String, String>();		
		logLevels.put("debug", "7");
		logLevels.put("info", "6");
		logLevels.put("notice", "5");
		logLevels.put("warn", "4");
		logLevels.put("error", "3");
		logLevels.put("crit", "2");
		logLevels.put("alert", "1");
		logLevels.put("emerg", "0");

		// Field name restrictions
		// http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-fields.html
		// https://github.com/Graylog2/graylog2-docs/wiki/GELF
		Map<String, String> invalidFields = new HashMap<String, String>();
		invalidFields.put("id", "bad");
		invalidFields.put("key", "bad");
		invalidFields.put("uid", "bad");
		invalidFields.put("type", "bad");
		invalidFields.put("source", "bad");
		invalidFields.put("all", "bad");
		invalidFields.put("analyzer", "bad");
		invalidFields.put("boost", "bad");
		invalidFields.put("parent", "bad");
		invalidFields.put("routing", "bad");
		invalidFields.put("index", "bad");
		invalidFields.put("size", "bad");
		invalidFields.put("timestamp", "bad");
		invalidFields.put("ttl", "bad");
		
		String hostName = new String();
	    try {
	    	hostName = java.net.InetAddress.getLocalHost().getHostName();
	    } catch ( UnknownHostException uhe ) {
	    	hostName = "localhost";
	    }
		
		String host = new String();
		host = "localhost";
		
		String message = new String();
		message = "Empty Short Summary";
		
		String logLevelInNumber = new String();
		logLevelInNumber = "6";
		
		int exitCode = 0;
		
		boolean justOne = false;
		
		// Fast concatenations 
		StringBuffer longMessage = new StringBuffer();
		// This is easy but not efficient. Double down on memory. Hopefully not to much of a performance impact
		List<String> justOneList = new ArrayList<String>();
		StringBuffer justOneShortMessage = new StringBuffer();
		
		Map<String, String> additionalFields = new HashMap<String, String>();
		
		try {

			 while ( (i < args.length) && (args[i].startsWith("-")) ) {
				int argumentsRemaining = args.length - (i + 1);
				if ((args[i].compareToIgnoreCase("-h") == 0) && (argumentsRemaining > 0)) {
					if (!(args[i+1].startsWith("-"))) {
						// set graylog server hostname
						i = i + 1;
						
						try {
							// If it does not work an exception will occur and no assignment will take place
							java.net.InetAddress.getByName(args[i]);
							host = args[i];
						} catch (UnknownHostException uhe) {
							host = "localhost";
						}
						System.out.println("The Graylog2 host has been set to: '" + host + "'");
					} else {
						// Error
						// Right now just ignore
					}
				} else if ((args[i].compareToIgnoreCase("-addField")==0) && (argumentsRemaining > 2)) {
					if ( (args[i+1].matches("^[A-Za-z0-9_]+$")) && (!(invalidFields.containsKey(args[i+1]))) ) {
						i = i + 1;
						String fieldName = args[i];
						if (fieldName.startsWith("_")) {
							System.out.println("Warning: Your field name starts with an underscore which is redundant to the automatically added underscore.");
						}
						if (args[i+1].compareToIgnoreCase("-fieldValue") == 0) {
							i = i + 2;
							// Set field value
							additionalFields.put(fieldName.replaceAll("[\\n\\r]", "").trim(),args[i].replaceAll("[\\n\\r]", "").trim());
							System.out.println("A custom field named: '" + fieldName + "' was added with the value: '" + args[i] + "'");
						} else {
							// Error
							// Right now just ignore
						}
					} else {
						i = i + 3;
						// Error
						// Right now just ignore
					}
				} else if ((args[i].compareToIgnoreCase("-s") == 0) && (argumentsRemaining > 0)) {
					i = i + 1;
					// Set short summary i.e. An error occurred. Err 401 
					message = args[i].replaceAll("[\\n\\r]", "").trim();
					System.out.println("The short summary is: '" + message + "'");
				} else if ( (args[i].compareToIgnoreCase("-l") == 0) && (argumentsRemaining > 0) ) {
					i = i + 1;
					// Set log level
					if (logLevels.containsKey(args[i])) {
						logLevelInNumber = logLevels.get(args[i]);
						System.out.println("The log level has been set to: '" + args[i] + "'"); 
					}
				} else if ((args[i].compareToIgnoreCase("-r") == 0) && (argumentsRemaining > 0)) {
					if (!(args[i+1].startsWith("-"))) {
						// set local hostname
						i = i + 1;
						
						try {
							// If it does not work an exception will occur and no assignment will take place
							java.net.InetAddress.getByName(args[i]);
							hostName = args[i];
						} catch (UnknownHostException uhe) {
							hostName = "localhost";
						}
						System.out.println("The local host name will be reported as: '" + hostName + "'");
					} else {
						// Error
						// Right now just ignore
					}					
				} else if (args[i].compareToIgnoreCase("-1") == 0) {
					// Break up captured stdout by double newline - useful for scripts which would like many log entries without wrapping each command within the script
					justOne = true;
				} else if ( (args[i].compareToIgnoreCase("-?") == 0) || (args[i].compareToIgnoreCase("-help") == 0) ) {
					System.out.println("ExeStdoutToGelf <ExeStdoutToGelf arguments> <application and arguments>");
					System.out.println("-h <Graylog2 hostname>");
					System.out.println("-s <Short summary>");
					System.out.println("-l <debug|info|notice|warn|error|crit|alert|emerg>");
					System.out.println("-r <hostname to report as>");
					System.out.println("-1 (creates short messages seperated by double newlines)");
					System.out.println("-addField <Field_Name> -fieldValue \"A Value\"");
				} else {
					// Unknown switch
				}
				i = i + 1;
			}
			
			// create a new list of arguments for our process
			List<String> cmd = new ArrayList<String>();
			 while (i < args.length) {
				cmd.add(args[i]);
				i++;
			 }

			if (!(cmd.isEmpty())) {
				ProcessBuilder pb = new ProcessBuilder(cmd);
				pb.redirectErrorStream(true);
				Process p = pb.start();
				BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
	
				String s = null;
	
				while ((s = stdInput.readLine()) != null) {
					// readLine trims off the newline character which we need
					String line = s + "\n";
					// But do not log empty lines
					if (!(line.trim().isEmpty())) {
						longMessage.append(line);
						justOneShortMessage.append(line);
					} else if (justOneShortMessage.length() > 0) {
						justOneList.add(justOneShortMessage.toString());
						justOneShortMessage.setLength(0);
					}
				}
				
				p.waitFor();		
				exitCode = p.exitValue();
				additionalFields.put("exitCode", Integer.toString(exitCode));

				if (justOneShortMessage.length() != 0) {
					justOneList.add(justOneShortMessage.toString());
					justOneShortMessage.setLength(0);					
				}
				
			} else {
				longMessage.append("");
			}

			if (justOne) {
				Iterator<String> justOneItr = justOneList.iterator();
				while (justOneItr.hasNext()) {
					GelfMessage gelfMessage = new GelfMessage(message, justOneItr.next(), System.currentTimeMillis(), logLevelInNumber);
					gelfMessage.setHost(hostName);
		
					Iterator<Map.Entry<String, String>> itr = additionalFields.entrySet().iterator();
					
					while (itr.hasNext()) { 
						Map.Entry<String, String> entry = itr.next(); 
						String key = (String)entry.getKey(); 
						String val = (String)entry.getValue(); 
						gelfMessage.addField(key, val);
					}
		
					GelfSender gelfSender = new GelfSender(host);
									
					if (gelfMessage.isValid()) {
					   gelfSender.sendMessage(gelfMessage);
					   System.out.println("The log message has been sent");
					}
					// Sending messages too fast results in out of order time stamps and processing in Graylog
					Thread.sleep(5);
				}
			} else {
				GelfMessage gelfMessage = new GelfMessage(message, longMessage.toString(), System.currentTimeMillis(), logLevelInNumber);
				gelfMessage.setHost(hostName);
	
				Iterator<Map.Entry<String, String>> itr = additionalFields.entrySet().iterator();
				
				while (itr.hasNext()) { 
					Map.Entry<String, String> entry = itr.next(); 
					String key = (String)entry.getKey(); 
					String val = (String)entry.getValue(); 
					gelfMessage.addField(key, val);
				}
	
				GelfSender gelfSender = new GelfSender(host);
								
				if (gelfMessage.isValid()) {
				   gelfSender.sendMessage(gelfMessage);
				   System.out.println("The log message has been sent");
				}				
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.exit(exitCode);
		}
	}
}
