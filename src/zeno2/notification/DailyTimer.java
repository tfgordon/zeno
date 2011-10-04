package zeno2.notification;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;
import java.util.Vector;
import java.awt.List;
import java.lang.String;
import java.lang.StringBuffer;
import java.lang.Long;
import java.lang.Integer;
import java.util.Calendar;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.net.InetAddress;

import javax.servlet.Servlet;
import org.apache.velocity.context.Context;

import zeno2.notification.MailWrapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import zeno2.kernel.ZenoException;
import zeno2.kernel.Factory;
import zeno2.kernel.Constants;
import zeno2.kernel.Monitor;
import zeno2.db.MonitorImpl;
import zeno2.db.DBNotify;
import zeno2.db.PrincipalFactory;
import zeno2.util.ZenoUtilities;

/**
 * This class handles the notification timer schedule and prepares email
 * messages to be sent to recipients. For each recipient there will be a message with 
 * new articles from subscribed journals since last notification.
 * 
 */

public class DailyTimer {
		Timer timer;
		DBNotify dbNotify = null;

		public DailyTimer(Monitor monitor) {
	Date today = Calendar.getInstance().getTime();
	String hour_of_day = "";
	String minute = "";
	String second = "";
// Test Lothar 02-12-02
//System.out.println("DailyTimer.init.dailyNotificationAt="
//+monitor.getProperty("dailyNotificationAt","cannot get it!!"));

//System.out.println("DailyTimer.init.domainName="
//+monitor.getProperty("domainName","cannot get it!!"));

// Test Lothar 02-12-02
	StringTokenizer st = new StringTokenizer(monitor.getProperty("dailyNotificationAt","12:00:00"),":");
	if (st.hasMoreTokens())
			hour_of_day = st.nextToken();
	if (st.hasMoreTokens())	
			minute = st.nextToken();
	if (st.hasMoreTokens())
			second = st.nextToken();
System.out.println("Got String: " + hour_of_day + minute + second);
				timer = new Timer(true);
	Calendar calendar = Calendar.getInstance(); //set the first notification time for today
	calendar.set(Calendar.HOUR_OF_DAY, new Integer(hour_of_day).intValue()); 
	calendar.set(Calendar.MINUTE, new Integer(minute).intValue());
	calendar.set(Calendar.SECOND, new Integer(second).intValue());
	
	Date scheduleTime = calendar.getTime();

System.out.println("DailyTimer.Schedule time: " +  scheduleTime.toString());

	if ((scheduleTime.compareTo(today)) < 0 ) {
			scheduleTime = ZenoUtilities.plusDays(scheduleTime, 1);
			calendar.setTime(scheduleTime);
			calendar.set(Calendar.HOUR_OF_DAY, new Integer(hour_of_day).intValue()); 
			calendar.set(Calendar.MINUTE, new Integer(minute).intValue());
			calendar.set(Calendar.SECOND, new Integer(second).intValue());
			scheduleTime = calendar.getTime();
	}

System.out.println("[Notification Module INFO]: Daily notification scheduled for: "+ scheduleTime.toString());
	this.dbNotify = new DBNotify((MonitorImpl)monitor);
	
	timer.scheduleAtFixedRate(new NotifyTask(monitor, dbNotify), 
				//scheduleTime,  120000);
				scheduleTime, 86400000);
		}

class NotifyTask extends TimerTask {

	protected StringBuffer message = new StringBuffer();
	protected MonitorImpl monitor = new MonitorImpl();	
	protected DBNotify dbNotify = new DBNotify();

	public NotifyTask(Monitor mon, DBNotify dbnotify) {	

			getMessageFromFile();
			this.monitor = (MonitorImpl)mon;
			this.dbNotify = dbnotify;
			run();
	}

	protected void getMessageFromFile() {

			String msg = new String("");

			/* mail body to be read from file
				try {
				BufferedReader in =  new BufferedReader(new FileReader("mail.txt"));
				
				do{
				msg += in.readLine();
				} while(msg != null);
				} catch (java.io.FileNotFoundException fnf){
				System.out.println("File mail.txt not found!");
				} catch (java.io.IOException io) {
				System.out.println("IOException caught!");
				}	
			*/	   
	}

	public void run() {
		List emailList = new List();
		Vector principalList = new Vector();
		Vector valueList = new Vector();
		String principal = new String("");
		String email = new String("");
		String value = new String("");
		String query = new String("");
		String note = new String("");
		String eol = System.getProperty("line.separator");
		Long milisec;	      
		ResultSet articleRs;
		Date date;
			int articleRsCounter = 0; // counts the number of records in articleRs

System.out.println("DailyTimer Task Started at " + Calendar.getInstance().getTime().toString());

		ResultSet rs = dbNotify.executeQuery("select * from principal_property where name='zeno2.dailyNotify';");
		MailWrapper mail = new MailWrapper();
			
		// get all principal ids that want to be notified
		try {
			while(rs.next()) {
				principalList.add(new String(rs.getString("principal")));
			}
			rs.close();
		} catch (SQLException sqle) {
			System.out.println("DailyTimer: Could not get emails and subscribed journals from DB!");
		}

		// if there is something to be done
		if (principalList.size() > 0) {
		
		// create email recipient list and message
			for (int x=0;x<principalList.size();x++) {
				principal = (principalList.elementAt(x)).toString();
				email = dbNotify.getEmail(principal);	
				emailList.add(email);		    	
				value = dbNotify.getDailyNotifyJournal(principal);

				this.message.append("Welcome to ZENO notification letter." + eol + eol);
				this.message.append("These articles are new since your last notification:" +eol+eol); 

				query = "select value from principal_property where principal='"
					+ principal +"' and name='zeno2.lastDailyNotify';";
				rs = dbNotify.executeQuery(query);

				try {
					while(rs.next()) {			    
						String val = rs.getString("value");
						if(val == null) val ="0";
						date = new Date(new Long(val).longValue());
						articleRs = dbNotify.loadNewArticles(date,value);
						if ( articleRs == null ) continue;
						while(articleRs.next()){
							articleRsCounter ++;
							this.message.append("Journal: " + dbNotify.getJournalTitle(articleRs.getString("parent"))+eol);
							this.message.append("Article Label: ");
							this.message.append(articleRs.getString("label") + eol);
							this.message.append("Article Title: ");
							this.message.append(articleRs.getString("title") + eol+eol);

							// get 30 words of article note
							note = articleRs.getString("note");
							StringTokenizer st = new StringTokenizer(note);
							if (st.countTokens() <= 30) {
								this.message.append(note + eol + eol + eol + "For the full text of article please visit the URL below." +eol);
							} else {
								note = "";
								for (int token=0;token < 30; token++) {
									note = note + st.nextToken() + " ";
								}
								note = note + "..." + eol + eol + "For the full text of article please visit the URL below." +eol;
								this.message.append(note);
							}

								//this.message.append(note);
				
							String domainName = monitor.getProperty("domainName","noDomainName");
							int p = domainName.indexOf(":");
							String mailDomain = (p<0) ? domainName : domainName.substring(0,p);
							this.message.append("URL: http://");
							this.message.append(domainName);
							this.message.append("/zeno");
							this.message.append("/forum?action=editArticle&id=");
							this.message.append(articleRs.getString("id"));
							this.message.append("&view=print" + eol);
							String mailAlias = dbNotify.getMailAlias(articleRs.getString("parent"));
//System.out.println("DailyTimer.run.mailAlias="+mailAlias);
							if ((mailAlias != null) && !mailAlias.equals("null")&& !mailAlias.equals("") ) {
								this.message.append("You can respond directly to the Journal using mailto:" 
								+ mailAlias 
								+ "@" + mailDomain
								+ eol +eol + eol);
							}
							else {
								this.message.append(eol + eol + eol);
							}
						}
					}
				} catch (SQLException sqle) { 
					System.out.println("OOOPS Something is gone wrong in DailyTimer while emailing:!"); 
				} catch (ZenoException ze){
					ze.printStackTrace();
				}
			

				try {
					if (articleRsCounter>0) {
						String subject = new String(monitor.getProperty("notificationMailSubject","Notification"));
						String mailserver = new String(monitor.getProperty("zenoMailServer","localhost"));
						String account = new String(monitor.getProperty("notificationMailAccount","notification")+"@"+mailserver);

						mail.postMail(emailList.getItems(), subject, this.message.toString(), account, mailserver);
						Calendar now = Calendar.getInstance();
						PrincipalFactory pr = dbNotify.getPrincipalFactory(this.monitor);
						milisec = new Long(now.getTime().getTime());
						pr.setProperty(principal, "system", "zeno2.lastDailyNotify", milisec.toString());
						System.out.println("**********Mailed to: " +eol+ principal + this.message.toString());
					}

					// reset lists, vectors and counters for the next email recipient
					emailList.removeAll();
					this.message.delete(0,this.message.length());
					articleRsCounter = 0;


				} catch (javax.mail.MessagingException me) {
					System.out.println("Timer Task: Messaging Exception caught: " + me);		    
				} catch (ZenoException ze) {}		   
			}	
		}
	}
}


}
