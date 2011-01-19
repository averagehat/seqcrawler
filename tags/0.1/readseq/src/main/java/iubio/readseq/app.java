//iubio/readseq/app.java
//split4javac// iubio/readseq/readseqapp.java date=25-Sep-2001

// iubio.readseq.readseqapp.java

package iubio.readseq;


import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;//?
//import javax.swing.tree.*;//?
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import java.awt.datatransfer.*;
 
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import iubio.readseq.*;

import flybase.Args;
import flybase.Debug;
import flybase.AppResources;
import flybase.FastVector;
import flybase.FastProperties;
import flybase.Utils;
import flybase.Environ;


/**
  Readseq GUI driver application <p>
	iubio.readseq.app => window/gui interface (swing-based)
	@author  Don Gilbert
	@version 	July 1999
*/

//split4javac// iubio/readseq/readseqapp.java line=45
public class app extends run 
{	
	public static void main(String[] args) {
		new app(args); 	 
		}
		
	public app(String[] args) 
	{
		verbose= true; //? make it default
		getargs( args); // super call
		
		try {  
		 	AppFrame appframe = new AppFrame(this);
	    BioseqReader.messageapp= appframe;
	    appframe.setVisible(true);
			}
		catch (NoClassDefFoundError e) {
			e.printStackTrace();
			appusage();
			}
	}

	public static void appusage() { appusage(System.out); }

	public static void appusage(PrintStream out)
	{
		out.println();
    out.println("  Graphic interface to Readseq (Java swing)");
		out.println("  Usage:");
		out.println("    java -cp readseq.jar " + app.class.getName() + " (java version 1.2)");
		out.println("    jre -cp readseq.jar:/path/to/swingall.jar " + app.class.getName() + " (java version 1.1.x)");
    out.println("  " + Readseq.version);

		out.println();
    out.println("  Java Foundation Classes (Swing) are required for this interface");
    out.println("  This is available for Java versions 1.1.7 and newer at");
    out.println("     http://java.sun.com/products/jfc/download.html");
    out.println("  Put the swingall.jar of this package in your classpath, as with");
		out.println("    jre -cp readseq.jar:swingall.jar " + app.class.getName() + " (java version 1.1.x)");
		out.println();
    out.println("  Macintosh users: See also ReadseqApp, a small Mac Java application");
    out.println("  to run readseq without a command line.");
		out.println();
	}
}


//public 
class BioseqFileFilter extends FileFilter 
{
	Hashtable exts= new Hashtable();
	public BioseqFileFilter() {
		int n= BioseqFormats.nFormats();
		for (int i= 0; i<=n; i++) {
			String suf= BioseqFormats.formatSuffix(i); //? allow multiple suffixes per format?
			addExtension(suf);
			}
		}
	public void addExtension(String ext) { exts.put(ext,ext); }
	
  public boolean accept(File f) {
  	if (f.isDirectory()) return true; //?!
  	String fn= f.getName();
  	int at= fn.lastIndexOf('.');
  	if (at>0) fn= fn.substring(at);
  	return (exts.get(fn)!=null);
		}
  public String getDescription() { 
  	return "Biosequence files";
  	}
}


class MakeFileChooser extends Thread {
	// UI is slow to make this beast
	AppFrame appf;
	MakeFileChooser(AppFrame appf) {
  	setPriority(4);
    this.appf = appf;
    }
 	public void run() { appf.initFileChooser(); }
	} 
	

	//for features, nofeatures...
class GetFeaturelist //extends AbstractAction 
{
	//static String title= "Select feature values";  
	static String[] flist;
	static { initlist(); }
	static String footer= "Selections apply when input data have feature tables.";
	
	String   title, message;
	Object[] messagepack;
	int[] 	selindices= new int[0];  
 	JFrame  frame;
	JList   jlist;
 	
	static void initlist() {
		flist= BioseqDocImpl.getStandardFeatureList();
		if (flist==null) flist= new String[] { "exon", "intron", "CDS" }; // ? error
		}
	
	GetFeaturelist(JFrame frame, String title, String message) { 
		this.message= message;  
		this.title= title;
		this.frame= frame;
		makeUi();
		}
		
	String getValue() 
	{ 
		StringBuffer sb= new StringBuffer();
		for (int i=0; i<selindices.length; i++) {
			if (i>0) sb.append(',');
			sb.append( flist[ selindices[i] ]);
			}
		String val= sb.toString();
		//Debug.println("GetFeaturelist.value= "+val);
		return val;
	}
	
	void setValue(String vals) 
	{
		String[] ss= Utils.splitString(vals,", "); 
		int n= 0;
		if (ss!=null) for (int i=0; i<ss.length; i++) {
			for (int k=0; k<flist.length; k++) if (flist[k].equals(ss[i])) n++;
			}
		selindices= new int[n]; n= 0;
		if (ss!=null) for (int i=0; i<ss.length; i++) {
			for (int k=0; k<flist.length; k++) if (flist[k].equals(ss[i])) selindices[n++]= k;
			}
	}
		
	public boolean choose() 
	{ 
		jlist.setSelectedIndices( selindices);
if (true) {	
		int result= JOptionPane.showConfirmDialog( frame, messagepack, title,
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE );
		if (result != JOptionPane.OK_OPTION) return false; 
		selindices= jlist.getSelectedIndices(); // never null, int[0] if no selection
		return (selindices.length > 0);
} else {			
		Object[] selectionValues= null; //flist - if want Jlist allowing only 1 selection!
		Object initialValue= null; //selindices;  
		Object result= JOptionPane.showInputDialog( frame, messagepack, title, 
				JOptionPane.QUESTION_MESSAGE, null, // icon
				selectionValues, initialValue );
		Debug.println("JOptionPane result="+result); // returning only 1 string !
		//! result instanceof String may be typed value
		
		if (result==null) return false; // means cancelled -- will this work w/ our funky dialog?
		selindices= jlist.getSelectedIndices(); // never null, int[0] if no selection
		if (selindices.length > 0) return true;
		else if (result instanceof String) {
			setValue( (String) result);
			return (selindices.length > 0);
			}
		else 
			return false;
}
	}


	void makeUi()
	{
		jlist = new JList(flist);
		jlist.setVisibleRowCount(10);
		//jlist.addMouseListener( new ListSelectionListener());

		JScrollPane sp = new JScrollPane(jlist);
		JLabel jfoot= new JLabel(footer,JLabel.CENTER);
		jfoot.setFont( new Font("sanserif",0,10) );
		jfoot.setEnabled(true); // to keep ui from graying out !
		jfoot.setForeground(UIManager.getColor("OptionPane.messageForeground"));

		//jlist.setToolTipText(footer); //? will this do - works, but distracting - label better
		//JTextArea jfoot= new JTextArea(footer,2,20);
		//jfoot.setLineWrap(true);
		//jfoot.setEditable(false);
		messagepack= new Object[3];
		messagepack[0]= message;
		messagepack[1]= jfoot;
		messagepack[2]= sp;
	}
				
  /*private class ListSelectionListener extends MouseAdapter
  {
		public void mousePressed(MouseEvent e) {
	    if (e.getClickCount() == 2) {
				JList list = (JList)e.getSource();
				int  index = list.locationToIndex(e.getPoint());
				//optionPane.setInputValue(list.getModel().getElementAt(index));
	    	}
			}
 	}*/

}
 
	

class GetUrls //extends AbstractAction 
{
	static String title= "Open sequence URLs";  
	static String message= "Enter URL(s) of sequence data, one per line";  
	static String footer= "URL template (above non-URL values will insert in %% of template)";
	static String sUrlInsert= "%%";
	
	Object[] messagepack;
 	JFrame  frame;
	JTextArea jlist;
 	JTextField jurltempl;
 	
	GetUrls(JFrame frame) { 
		this.frame= frame;
		makeUi();
		}
				
	FastVector getValue() //? return URL[] instead, for certainty?
	{ 
		String txt= jlist.getText();
		String[] ss= Utils.splitString(txt,"\r\n, ");
		String urltempl= jurltempl.getText().trim();
		if (urltempl.length()>0) {
			int at= urltempl.indexOf(sUrlInsert);
			String tmp1, tmp2;
			if (at<0) { tmp1= urltempl; tmp2= ""; }
			else { 
				tmp1= urltempl.substring(0,at); 
				tmp2= urltempl.substring(at+sUrlInsert.length()); 
				}
			for (int i=0; i<ss.length; i++) {
				if (ss[i].indexOf("://")<0) ss[i]= tmp1 + ss[i] + tmp2;
				}	
			}
		//return ss;
		FastVector v= new FastVector(ss.length);
		for (int i=0; i<ss.length; i++) {
			try { v.addElement( new URL(ss[i]) ); }
			catch (Exception e) {}
			}	
		return v;
	}
			
	public boolean choose() 
	{ 
		//jlist.setSelectedIndices( selindices);
		int result= JOptionPane.showConfirmDialog( frame, messagepack, title,
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE );
		if (result != JOptionPane.OK_OPTION) return false; 
		//selindices= jlist.getSelectedIndices(); // never null, int[0] if no selection
		//return (selindices.length > 0);
		return true;
	}


	void makeUi()
	{
		jlist= new JTextArea(7,40);
		jlist.setLineWrap(false);
		jlist.setEditable(true);

		String urltempl= Environ.gEnv.get("SEQ_URL_TEMPLATE");
		jurltempl= new JTextField(urltempl, 40);
		//jurltempl.setFont( new Font("sanserif",0,10) );
		
		JScrollPane sp = new JScrollPane(jlist);
		
		JLabel jfoot= new JLabel(footer,JLabel.CENTER);
		jfoot.setFont( new Font("sanserif",0,10) );
		jfoot.setEnabled(true); // to keep ui from graying out !
		jfoot.setForeground(UIManager.getColor("OptionPane.messageForeground"));

		messagepack= new Object[] { message, sp, jurltempl, jfoot };
	}

}
 
	


//public 
class AppFrame extends JFrame 
	implements MessageApp
	//implements ActionListener //, TextListener 
{
	public static String propname= "AppFrame";
	public static final String 
			//? set these from Action class? or not
		labelSuffix = "Label", 
		tooltipSuffix= "Tip",
		checkedSuffix = "Checked", 
		radioSuffix= "Radio", 
		radioGroupSuffix= "Group", // drop this use
		actionSuffix = "Action";

	private app 	myapp;
	private FastProperties props = new FastProperties(); //default props?
  private Hashtable commands;
  private Hashtable menuItems;
  private JMenuBar 	menubar;
	protected JFileChooser fchooser;
	protected BioseqFileFilter bioseqfilter;
	protected JTextArea messageText;
	private String choosenOutputName;
	//private File[] selfiles;
	//private String[] selurls;
	private FastVector inputitems= new FastVector();
	//private Readseq	readseq;
	
	
	
	public AppFrame(app myapp) 
	{
		this.myapp= myapp;
  	String pname= System.getProperty( propname, propname);
		props.loadProperties(pname);
 		
 		initframe();
		myapp.pretty.userset(); //? always

		Thread makechooser= new MakeFileChooser( this);
		makechooser.start();
 	}
 	
 	protected void initframe()
 	{

			// install the command table
		commands = new Hashtable();
		Action[] actions = getActions();
		for (int i = 0; i < actions.length; i++) {
	    Action a = actions[i];
	    commands.put( a.getValue(Action.NAME), a);
			}

			// install the menus
		menuItems = new Hashtable();
		menubar = createMenubar("menubar", commands);

			//? add Edit menu - copy/cut/clear/select all for messageText ??
			
    this.setJMenuBar( menubar); //(buildMenus());
		
  	this.setTitle( props.getProperty("title", "Readseq"));

		this.getContentPane().setBackground(Color.white); //lightGray
		this.getContentPane().setLayout(new BorderLayout());
    this.addWindowListener(new java.awt.event.WindowAdapter() {
     	public void windowClosing(java.awt.event.WindowEvent e) {  
				try { finalize(); } catch (Throwable ex) {}
     		System.exit(0); 	
     		}
    	});
    //this.getContentPane().add( createUI(fname));

    JPanel northbar= new JPanel();
    JComponent fc= makeFormatChoice();
		JLabel jlab= new JLabel("Output format:");
		jlab.setFont( new Font("dialog",0,12) );
		jlab.setForeground(UIManager.getColor("OptionPane.messageForeground"));
		jlab.setEnabled(true); // to keep ui from graying out !
		jlab.setToolTipText("Select sequence format for saving output");  

    northbar.add( jlab);
    northbar.add( fc);

		this.getContentPane().add("North", northbar); // need comp bar below menus?
		this.getContentPane().add("Center", makeMessageBox());
		this.pack();
		
    Dimension  screenSize = getToolkit().getScreenSize();
		int width= Environ.gEnv.getInt("ReadseqApp.width", 450);
		int height= Environ.gEnv.getInt("ReadseqApp.height", 350);
		this.setSize(width, height);  //?
			// center me
    this.setLocation((screenSize.width - width) / 2, (screenSize.height - height) / 2);
	}

		//
		// interface MessageApp
		//
		
	public void errmessage(String msg)
	{
		//Debug.println(msg);
  	//messageText.setForeground(Color.red);
		messageText.append( msg+"\n");
		int dlen= messageText.getDocument().getLength();
 		messageText.select(dlen,dlen); // scroll into view?
	}

	public void infomessage(String msg)
	{
		//Debug.println(msg);
  	//messageText.setForeground(Color.black);
		messageText.append( msg+"\n");
		int dlen= messageText.getDocument().getLength();
 		messageText.select(dlen,dlen); // scroll into view?
	}
	

	
	void initFileChooser() {
		if (fchooser == null) {
	  	fchooser = new JFileChooser();
	    fchooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES); //FILES_ONLY FILES_AND_DIRECTORIES
	    fchooser.setMultiSelectionEnabled(true);

			bioseqfilter= new BioseqFileFilter();
	   	FileFilter[] ff= fchooser.getChoosableFileFilters(); // reset order so *.* isn't 1st
	    for (int i=0; i<ff.length; i++)
	    	fchooser.removeChoosableFileFilter(ff[i]); // doesn't seem to be working...
	 		fchooser.addChoosableFileFilter( bioseqfilter);  
	 		fchooser.addChoosableFileFilter( fchooser.getAcceptAllFileFilter());  
	 		
	 		File dir= new File( System.getProperty("user.dir")); //"user.home" is default - bad
	 		fchooser.setCurrentDirectory(dir);
	  	}
	}
	
	JComponent makeFormatChoice() 
	{
		JComboBox cb = new JComboBox();
    // cb.setEditable(false);
		int n= BioseqFormats.nFormats();
		for (int i= 0; i<=n; i++) 
		 if (BioseqFormats.canwrite(i)) {
			String nm= BioseqFormats.formatName(i);
			cb.addItem( nm);
			}
		String selname= BioseqFormats.formatName( BioseqFormats.getFormatId(myapp.outformat));
  	cb.setSelectedItem( selname); 
		cb.getAccessibleContext().setAccessibleName("Biosequence formats");
  	cb.getAccessibleContext().setAccessibleDescription("Choose a file format");

		Action act = getAction("outformat");
		if (act != null) {
			cb.addActionListener(act);
			//act.addPropertyChangeListener( actli);
			cb.setEnabled( act.isEnabled());
		} else {
			cb.setEnabled(false);
		}
		return cb;
	}
		
	
	JComponent makeMessageBox() 
	{
		String fontspec= Environ.gEnv.get("ReadseqApp.textFont","monospaced-plain-10");
    messageText = new JTextArea(20,85);
    messageText.setFont( Font.decode(fontspec)); // "monospaced", Font.PLAIN, 10
    messageText.setEditable(false);
    JPanel messagePanel = new JPanel(new BorderLayout());
    messagePanel.add(new JScrollPane(messageText) {
        public Dimension getPreferredSize(){
            Dimension size = AppFrame.this.getSize();
            return new Dimension(size.width, size.height);
            }
        public Dimension getMinimumSize(){ return new Dimension(100, 50); }
        },
        BorderLayout.CENTER);
        
		messageText.setBorder( new EmptyBorder(4, 4, 4, 4));
   	messagePanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createTitledBorder("Messages"),
        BorderFactory.createEmptyBorder(2,2,2,2)
        ));
		return messagePanel;
	}
	
			
	protected JMenuItem getMenuItem(String cmd) {
		return (JMenuItem) menuItems.get(cmd);
    }

	protected Action getAction(String cmd) {
		return (Action) commands.get(cmd);
    }

	protected Frame getFrame(Component c) {
		if (c!=null)
		 for (Container p = c.getParent(); p != null; p = p.getParent()) 
	    	if (p instanceof Frame) return (Frame) p;
		return null;
	}

		//
    // --- action implementations -----------------------------------
		//

	/* add actions:
		select seq items in file / or all
		set degap char -- part of cmdDegap 
		set translate chars -- part of cmdTranslate
		select output file name
		select output format
		select input format
	*/
	
		
	private Action[] defaultActions; // can't initialize static w/o myapp.
	private RunAction runact;
	
	public Action[] getActions() {
		//return TextAction.augmentList(editor.getActions(), defaultActions);
		if (defaultActions==null) {
			runact= new RunAction();
			
			defaultActions= new Action[] { new ListAction(), 
				new LcAction(), new UcAction(), new NcAction(),
				new DegapAction(), new ChecksumAction(), new ReverseAction(),
				new TranslateAction(), new VerboseAction(),
				new OutformatAction(), new InformatAction(),
				new NameleftAction(), new NamerightAction(), new NametopAction(), new NamewidthAction(), 
				new NumleftAction(), new NumrightAction(), new NumtopAction(), new NumbotAction(), 
				new MatchcAction(), new GapcountAction(), new SeqwidthAction(), new TabAction(),
				new ColtabAction(), new XmlDtdAction(),
				new ChoosefeatAction(), new RemovefeatAction(), new AllfeatAction(),
				new SubrangeAction(), new ExtractRangeAction(),
				new ClearopenAction(), 
				new AboutAction(), new AboutDetailsAction(), new AboutFormatAction(), new AboutAllFormatsAction(),
				runact, new OpenurlAction(), new OpenAction(), new SaveAction(), new QuitAction()
		    };
		   }
		return defaultActions;
    }

	void checkEnabledActions()
	{
	 	runact.checkEnabled();
	}

	class OutformatAction extends AbstractAction {
		OutformatAction() { super("outformat"); }
		public void actionPerformed(ActionEvent e) 
		{
 			if (e.getSource() instanceof JComboBox) {
				JComboBox cb= (JComboBox) e.getSource();
				String fname= (String) cb.getSelectedItem();
				infomessage("Selected out format="+fname);
				myapp.outformat= fname;
				}
		}
	}
	
	class InformatAction extends AbstractAction {
		InformatAction() { 
			super("informat"); 
			setEnabled(false); // not ready
			}
		public void actionPerformed(ActionEvent e) 
		{
 			if (e.getSource() instanceof JRadioButtonMenuItem) {
				JRadioButtonMenuItem ck= (JRadioButtonMenuItem) e.getSource();
				//ButtonGroup bg= radiogroups.get("informat");
	     	//if (bg!=null) ButtonModel bm= bg.getSelection();
	   		} 
 			else
 			if (e.getSource() instanceof JComboBox) {
				JComboBox cb= (JComboBox) e.getSource();
				String fname= (String) cb.getSelectedItem();
				infomessage("Selected in format="+fname);
				myapp.informat= fname;
				}
		}
	}
 
	
	// options actions: list lc uc degap checksum trans verbose
	// pretty actions: Width Tab Coltab Gapcount Nameleft Nameright Nametop Namewidth Numleft Numright Numtop Numbot

	class CheckboxAction extends AbstractAction {
 		boolean val= true;
		CheckboxAction(String nm) { super(nm); }
		void setval(boolean val) {
			this.val= val; 
			putValue("BoolValue", new Boolean(val));
			}
			
		public void actionPerformed(ActionEvent e) { 
 			if (e.getSource() instanceof JCheckBoxMenuItem) {
				JCheckBoxMenuItem ck= (JCheckBoxMenuItem) e.getSource();
				val= ck.isSelected(); // UI does the val change
	     	//ck.setSelected(val);
	   		} 
 			else if (e.getSource() instanceof JRadioButtonMenuItem) {
				//ButtonGroup bg= radiogroups.get("case");
				JRadioButtonMenuItem ck= (JRadioButtonMenuItem) e.getSource();
				val= ck.isSelected(); // UI does the val change
	     	//ck.setSelected(val);
	   		} 
			}
			
		public void setValue(ActionEvent e, boolean newval) { 
 			if (e.getSource() instanceof JCheckBoxMenuItem) {
				JCheckBoxMenuItem ck= (JCheckBoxMenuItem) e.getSource();
	     	ck.setSelected(newval);
	     	val= newval;
	   		} 
 			else if (e.getSource() instanceof JRadioButtonMenuItem) {
				//ButtonGroup bg= radiogroups.get("case");
				JRadioButtonMenuItem ck= (JRadioButtonMenuItem) e.getSource();
	     	ck.setSelected(newval);
	     	val= newval;
	   		} 
			}
    }

	class GetvalueAction extends AbstractAction {
 		String val, message;
		GetvalueAction(String nm) { 
			super(nm); 
			message= props.getProperty(nm + labelSuffix, "Select a value");  
			}
		void setval(String val) {
			this.val= val; 
			if (val!=null) putValue("StringValue",  val); // for menu name
			}
			
		public void actionPerformed(ActionEvent e) { 
 			if (true) { // (e.getSource() instanceof JMenuItem)
				//JMenuItem mi= (JMenuItem) e.getSource();
				// open dialog window to collect new value
				String title= "Get value";  
				//String message= "Select a value";  
				Object[] selectionValues= null;
				Object initialValue= val;
				Object result= JOptionPane.showInputDialog( AppFrame.this,
					message, title,
					JOptionPane.QUESTION_MESSAGE, null, // icon
					selectionValues, initialValue
					);
					
				if (result instanceof String) {  
        	firePropertyChange("StringValue", val, (String)result);
					val= (String)result;
					}
	   		} 
			}
			
    }



	class ListAction extends CheckboxAction {
		ListAction() { super("list"); setval(myapp.dolist); }
		public void actionPerformed(ActionEvent e) { super.actionPerformed(e); myapp.dolist= val; }
		}
	class LcAction extends CheckboxAction {
		LcAction() { super("lc"); putValue("Group", "case"); setval(myapp.dolowercase); }
		public void actionPerformed(ActionEvent e) { super.actionPerformed(e); 
			myapp.dolowercase= val; //if (val) myapp.douppercase= false;
			}
		}
	class UcAction extends CheckboxAction {
		UcAction() { super("uc"); putValue("Group", "case"); setval(myapp.douppercase); }
		public void actionPerformed(ActionEvent e) { super.actionPerformed(e); 
			myapp.douppercase= val; //if (val) myapp.dolowercase= false;
			}
		}
	class NcAction extends CheckboxAction {
		NcAction() { super("nc"); putValue("Group", "case"); setval(!(myapp.dolowercase||myapp.douppercase)); }
		public void actionPerformed(ActionEvent e) { super.actionPerformed(e); 
			if (val) { myapp.dolowercase= false; myapp.douppercase= false; }
			}
		}
		
	class DegapAction extends CheckboxAction {
		DegapAction() { super("degap"); setval(myapp.degap); }
		public void actionPerformed(ActionEvent e) { super.actionPerformed(e); myapp.degap= val; }
		}
	class ChecksumAction extends CheckboxAction {
		ChecksumAction() { super("checksum"); setval(myapp.dochecksum); }
		public void actionPerformed(ActionEvent e) { super.actionPerformed(e); myapp.dochecksum= val; }
		}
		
	class ReverseAction extends CheckboxAction {
		ReverseAction() { super("reverse"); setval(myapp.doreverse); }
		public void actionPerformed(ActionEvent e) { super.actionPerformed(e); myapp.doreverse= val; }
		}
		
	class TranslateAction extends GetvalueAction {  
		TranslateAction() { super("trans"); setval(""); } //myapp.dotranslate
		public void actionPerformed(ActionEvent e) { 
			super.actionPerformed(e); 
			myapp.setTranslation(val,false);
			}
		}
	class VerboseAction extends CheckboxAction {
		VerboseAction() { super("verbose"); setval(myapp.verbose); }
		public void actionPerformed(ActionEvent e) { super.actionPerformed(e); myapp.verbose= val; }
		}
	
	class XmlDtdAction extends CheckboxAction {
		XmlDtdAction() { super("xmldtd"); setval(XmlSeqWriter.includeDTD); }
		public void actionPerformed(ActionEvent e) { super.actionPerformed(e); XmlSeqWriter.includeDTD= val; }
		}
	
	class AllfeatAction extends CheckboxAction {
		AllfeatAction() { super("allfeat"); putValue("Group", "feats"); setval(!(myapp.hasFeatlist()||myapp.hasNofeatlist())); }
		public void actionPerformed(ActionEvent e) { super.actionPerformed(e); 
			if (val) { myapp.featlist= null; myapp.nofeatlist= null; }
			}
		}
		
	class ChoosefeatAction extends CheckboxAction {
		ChoosefeatAction() { super("choosefeat"); putValue("Group", "feats");  }
		public void actionPerformed(ActionEvent e) { 
			super.actionPerformed(e); 
			String msg= props.getProperty("choosefeat" + radioSuffix, "Select features");
			GetFeaturelist gf= new GetFeaturelist(AppFrame.this, "Select features", msg);
			gf.setValue(myapp.featlist);
			if (gf.choose()) {
				myapp.featlist= gf.getValue();  
    		infomessage("Selected features: " +myapp.featlist); 
    		}
    	else {
    		// need to uncheck selection...
    		setValue(e, false);
    		}
			}
		}
		
	class RemovefeatAction extends CheckboxAction {
		RemovefeatAction() { super("removefeat"); putValue("Group", "feats");  }
		public void actionPerformed(ActionEvent e) { 
			super.actionPerformed(e); 
			String msg= props.getProperty("removefeat" + radioSuffix, "Remove features");
			GetFeaturelist gf= new GetFeaturelist(AppFrame.this, "Remove features", msg);
			gf.setValue(myapp.nofeatlist);
			if (gf.choose()) {
				myapp.nofeatlist= gf.getValue();  
    		infomessage("Removed features: " +myapp.nofeatlist); 
    		}
    	else {
    		// need to uncheck selection...
    		setValue(e, false);
    		}
			}
		}

	class SubrangeAction extends GetvalueAction {
		SubrangeAction() { super("subrange"); setval( myapp.featsubrange ); }
		public void actionPerformed(ActionEvent e) { 
			super.actionPerformed(e);   
			myapp.featsubrange= val;  
			}
		}
	class ExtractRangeAction extends GetvalueAction {
		ExtractRangeAction() { super("extract"); setval( myapp.extractrange ); }
		public void actionPerformed(ActionEvent e) { 
			super.actionPerformed(e);   
			myapp.extractrange= val;  
			}
		}


		// pretty print opts
	class NameleftAction extends CheckboxAction {
		NameleftAction() { super("Nameleft"); setval(myapp.pretty.nameleft); }
		public void actionPerformed(ActionEvent e) { super.actionPerformed(e); myapp.pretty.nameleft= val; myapp.pretty.userset(); }
		}
	class NamerightAction extends CheckboxAction {
		NamerightAction() { super("Nameright"); setval(myapp.pretty.nameright); }
		public void actionPerformed(ActionEvent e) { super.actionPerformed(e); myapp.pretty.nameright= val;  myapp.pretty.userset(); }
		}
	class NametopAction extends CheckboxAction {
		NametopAction() { super("Nametop"); setval(myapp.pretty.nametop); }
		public void actionPerformed(ActionEvent e) { super.actionPerformed(e); myapp.pretty.nametop= val;  myapp.pretty.userset(); }
		}
	class NumleftAction extends CheckboxAction {
		NumleftAction() { super("Numleft"); setval(myapp.pretty.numleft); }
		public void actionPerformed(ActionEvent e) { super.actionPerformed(e); myapp.pretty.numleft= val;  myapp.pretty.userset(); }
		}
	class NumrightAction extends CheckboxAction {
		NumrightAction() { super("Numright"); setval(myapp.pretty.numright); }
		public void actionPerformed(ActionEvent e) { super.actionPerformed(e); myapp.pretty.numright= val;  myapp.pretty.userset(); }
		}
	class NumtopAction extends CheckboxAction {
		NumtopAction() { super("Numtop"); setval(myapp.pretty.numtop); }
		public void actionPerformed(ActionEvent e) { super.actionPerformed(e); myapp.pretty.numtop= val;  myapp.pretty.userset(); }
		}
	class NumbotAction extends CheckboxAction {
		NumbotAction() { super("Numbot"); setval(myapp.pretty.numbot); }
		public void actionPerformed(ActionEvent e) { super.actionPerformed(e); myapp.pretty.numbot= val;  myapp.pretty.userset(); }
		}
	class MatchcAction extends CheckboxAction {
		MatchcAction() { super("Matchc"); setval(myapp.pretty.domatch); }
		public void actionPerformed(ActionEvent e) { super.actionPerformed(e); myapp.pretty.domatch= val;  myapp.pretty.userset(); }
		}
	class GapcountAction extends CheckboxAction {
		GapcountAction() { super("Gapcount"); setval(myapp.pretty.degap); }
		public void actionPerformed(ActionEvent e) { super.actionPerformed(e); myapp.pretty.degap= val;  myapp.pretty.userset(); }
		}

	class SeqwidthAction extends GetvalueAction {
		SeqwidthAction() { super("Seqwidth"); setval( String.valueOf(myapp.pretty.seqwidth) ); }
		public void actionPerformed(ActionEvent e) { 
			super.actionPerformed(e); 
			try { myapp.pretty.seqwidth= Integer.parseInt(val);  myapp.pretty.userset(); } catch (Exception ex) {}
			}
		}
	class TabAction extends GetvalueAction { 
		TabAction() { super("Tab"); setval(String.valueOf(myapp.pretty.tab)); }
		public void actionPerformed(ActionEvent e) { 
			super.actionPerformed(e); 
			try { myapp.pretty.tab= Integer.parseInt(val);  myapp.pretty.userset(); } catch (Exception ex) {}
			}
		}
	class ColtabAction extends GetvalueAction { 
		ColtabAction() { super("Coltab"); setval(String.valueOf(myapp.pretty.spacer)); }
		public void actionPerformed(ActionEvent e) { 
			super.actionPerformed(e); 
			try { myapp.pretty.spacer= Integer.parseInt(val);  myapp.pretty.userset(); } catch (Exception ex) {}
			}
		}
	class NamewidthAction extends GetvalueAction { 
		NamewidthAction() { super("Namewidth"); setval(String.valueOf(myapp.pretty.namewidth)); }
		public void actionPerformed(ActionEvent e) { 
			super.actionPerformed(e); 
			try { myapp.pretty.namewidth= Integer.parseInt(val);  myapp.pretty.userset(); } catch (Exception ex) {}
			}
		}
	
 	abstract class TextViewAction extends AbstractAction {

		TextViewAction( String name ) { super(name);  }

		void makeHtmlUI( String html, String title) {
			JEditorPane jview = new JEditorPane( "text/html", html);
 			jview.setBorder( new EmptyBorder(6,6,6,6));
			//jview.setFont( new Font("times",0,12) );
			jview.setBackground(Color.white);  
			jview.setEditable(false);
	   	jview.addHyperlinkListener( new Hyperactive() );

  		HTMLEditorKit ed= (HTMLEditorKit) jview.getEditorKit();
  		if (getStyleSheet()!=null) ed.setStyleSheet( getStyleSheet() );

 			AppFrame.this.openActionFrame(jview, 	title, "details.menubar");
			}

		void makeTextUI( String text, String title) {
			JTextArea jtext= new JTextArea( text, 40, 80);
			jtext.setBorder( new EmptyBorder(6,6,6,6));
			jtext.setFont( new Font("monospaced",0,12) );
			jtext.setBackground(Color.white);  
			jtext.setEditable(false);

 			AppFrame.this.openActionFrame(jtext, 	title, "details.menubar");
			}

	};
	
	class AboutAction extends TextViewAction {
		AboutAction() { super("about"); }
		public void actionPerformed(ActionEvent e) {  
			ByteArrayOutputStream baos= new ByteArrayOutputStream();
			PrintStream pr= new PrintStream(baos);
			help h= new help(pr);
			String text= baos.toString();
			text= text.replace('\r', '\n');// dang linefeeds
			makeTextUI( text, "About " + props.getProperty("title", "Readseq"));
			}
	}

	class AboutAllFormatsAction extends AboutFormatAction {
		AboutAllFormatsAction() { super("formatallhelp"); }
		public void actionPerformed(ActionEvent e) {  
			showFmt( 0);
			}
	}
	
	class AboutFormatAction extends TextViewAction {
		AboutFormatAction() { super("formathelp"); }
 		AboutFormatAction(String name) { super(name); }
		
		public void actionPerformed(ActionEvent e) {  
			showFmt( BioseqFormats.getFormatId(myapp.outformat) );
			}
			
		void showFmt(int ifmt) {  
			ByteArrayOutputStream baos= new ByteArrayOutputStream();
			PrintStream pr= new PrintStream(baos);
			
			if (ifmt==0) {
				help h= new help(false,true,pr); // dohtml = 2nd
				h.formatHelp();
 				}
			else {
				help h= new help(false,false,pr);  
				h.formatDoc(ifmt); 
				h.formatDocEnd();
				}
			
			String text= baos.toString();
			text= text.replace('\r', '\n'); // dang linefeeds

			if (ifmt==0) {
				makeHtmlUI( text, "Format details ");
				}
			else {
				makeTextUI( text, "Format details for " + BioseqFormats.formatName(ifmt));
				}
			}
	}

   class Hyperactive implements HyperlinkListener {
			public void hyperlinkUpdate(HyperlinkEvent e) {
	    	if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
		      JEditorPane pane = (JEditorPane) e.getSource();
 		     	try { pane.setPage(e.getURL());  } 
		    	catch (Throwable t) { infomessage( "Hyperlink error: "+t.toString()); }
	      	}
	      }
  	 }
	
	StyleSheet defaultStyles;
	boolean gotStyles;

	public StyleSheet getStyleSheet() {
		if (!gotStyles) {
			StyleSheet dss = new StyleSheet();
			gotStyles= true;
			try {
				InputStream is = AppResources.global.getStream("readseq.css");
				Reader r = new BufferedReader(new InputStreamReader(is));
				dss.loadRules(r, null);
				r.close();
		    } catch (Throwable e) { return null; }
			defaultStyles= dss;
		}
		return defaultStyles;
 	}


	class AboutDetailsAction extends TextViewAction {
		AboutDetailsAction() { super("details"); }
		
		public void actionPerformed(ActionEvent e) {  
			ByteArrayOutputStream baos= new ByteArrayOutputStream();
			PrintStream pr= new PrintStream(baos);
			help h= new help(false,true,pr);
					/// test -- works, slow, needs tuning - wrap to width, smaller font ...
			baos.reset();
			h.extraHelp();   // >> need html display
			makeHtmlUI(baos.toString(), "More about " + props.getProperty("title", "Readseq"));
			}
			
	}
		
	class QuitAction extends AbstractAction {
		QuitAction() { super("quit"); }
		public void actionPerformed(ActionEvent e) { 
			// need to run app/run .finalize()
			try { finalize(); } catch (Throwable ex) {}
			System.exit(0); 
			}
    }

 	abstract class TextAction extends AbstractAction {
		JTextComponent jtext;
		TextAction( String name, JTextComponent jtext) { super(name); this.jtext= jtext; }
	};

	class PrintTextAction extends TextAction {
		PrintTextAction(JTextComponent jtext) { super("printd", jtext);  }
		public void actionPerformed(ActionEvent e) 
		{
			if (jtext==null) return;
			Frame frame= AppFrame.this.getFrame(jtext);
			PrintJob printjob= frame.getToolkit().getPrintJob( frame, frame.getTitle(), null);
			// need to page thru text...
			//jtext.print(  printjob.getGraphics() );
			printpages(  printjob  );
			printjob.end();
		}


		void printpages(PrintJob printjob) {
			Graphics g= printjob.getGraphics();
			Dimension paged= printjob.getPageDimension();
			Dimension viewd= jtext.getSize();
			int npages= (int) ( paged.height - 1 + viewd.height) / paged.height;
			int ipage= 0;
			int ytop= 0;
			int xleft= 0;
			boolean morepages= (npages>0); //(xleft < tablewidth || (lastrow+1) < nrows);
			while (morepages) {
				Rectangle oldclip = g.getClipBounds();
				Rectangle clip = new Rectangle( oldclip);
				clip.width = Math.min( clip.width, viewd.width);
 				clip.y= ytop;  
				clip.x= xleft;  
				g.translate( -xleft, -ytop); //!! need convert vis.y to clip.y -- this is it
 				
				//lastrow = lastVisibleRow( clip);  
				//lastrow= paintRows( g, firstrow, lastrow,  clip);// may pagebreak...
				jtext.print( g);
				
				g.translate( xleft, ytop); 
				g.setClip(oldclip);
				
				//xleft += clip.width; // next right panel
				
				PrintJob pj= ((PrintGraphics)g).getPrintJob();
				g.dispose(); // prints the page  
				ipage++;
				
				ytop += clip.height;
				morepages= (ipage<npages && ytop < viewd.height); //(xleft < tablewidth || (lastrow+1) < nrows);
				if ( morepages && pj!=null ) {
	  			g= pj.getGraphics(); // next page...
					if (g==null) morepages= false;  // error ?!?
	  			}
	  		else morepages= false;
				}
			}	
					
	};
	
	class CloseTextAction extends TextAction {
		CloseTextAction(JTextComponent jtext) { super("closed", jtext); }
		public void actionPerformed(ActionEvent e) 
		{
			Frame frame= AppFrame.this.getFrame(jtext);
			if (frame!=null) { frame.setVisible(false); frame.dispose(); }
		}
	};
	
	class CopyTextAction extends TextAction {
		CopyTextAction(JTextComponent jtext) { super("copyd", jtext); }
		public void actionPerformed(ActionEvent e) 
		{
			if (jtext==null) return;
			if (jtext.getSelectionStart() < jtext.getSelectionEnd()) 
				jtext.copy(); // to system clipboard -- only if selected!
			else {
	    	Clipboard clipboard = jtext.getToolkit().getSystemClipboard();
				String srcData = jtext.getText();
				clipboard.setContents(new StringSelection(srcData), null);  
				}
		}
	};
	
	class SaveTextAction extends TextAction {
		SaveTextAction(JTextComponent jtext) { super("saved", jtext); }
		public void actionPerformed(ActionEvent e) 
		{
			if (jtext==null) return;
			JFileChooser jchooser = new JFileChooser( System.getProperty("user.dir"));
			int ok = jchooser.showSaveDialog( AppFrame.this.getFrame(jtext)); //frame
   	 	if (ok == JFileChooser.APPROVE_OPTION) try {
	 			FileWriter wr= new FileWriter(jchooser.getSelectedFile());
				jtext.write(wr); 
				wr.close();
				} 
			catch (Exception ex) { errmessage("Error: " +ex);  }
		}
		
	}
		
	class SaveAction extends AbstractAction {
		SaveAction() { super("save"); }
		
		public void actionPerformed(ActionEvent e) 
		{
	    if (fchooser == null) initFileChooser();
		 	fchooser.setFileSelectionMode(JFileChooser.FILES_ONLY); //FILES_ONLY FILES_AND_DIRECTORIES DIRECTORIES_ONLY
	    fchooser.setMultiSelectionEnabled(false);

    	File savef= fchooser.getSelectedFile();
    	if (savef!=null) {
    		// guessOutname needs 			
    		myapp.setInputObjects( inputitems);
    		String savename= myapp.guessOutname( BioseqFormats.getFormatId(myapp.outformat));
	    	//String suff= BioseqFormats.formatSuffix( BioseqFormats.getFormatId(myapp.outformat));
	    	//String savename= savef.getName();
	    	//int at= savename.lastIndexOf('.');
	    	//if (at>0) savename= savename.substring(0,at) + suff;
	    	//else savename += suff;
	    	savef= new File( savef.getParent(), savename);
	   		fchooser.setSelectedFile(savef);
				}
				    	
			int returnVal = fchooser.showSaveDialog( AppFrame.this); //frame
   	 	if (returnVal == JFileChooser.APPROVE_OPTION) {
	 			File f= fchooser.getSelectedFile();
	 			boolean okay= false;
	 			if (f.exists()) {
     			//	errmessage("\n File already exists: " + f.toString() + "\n Please choose a new name" ); 
					String title= "Replace file?";
					String message= "Replace existing '" + f.getName() + "' ?";
					Object[] options = { "Replace", "Cancel" };
					int result= JOptionPane.showOptionDialog( AppFrame.this, message, title,
							JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, // icon
							options, options[0]);
					okay= (result == JOptionPane.YES_OPTION);
          }
	 			else okay= true;
	 			if (okay) {
     			choosenOutputName= f.toString(); //myapp.outname= f.toString(); //? 
     			infomessage("Save output to: " +choosenOutputName); 
   				checkEnabledActions();  
     			}
      	}
		}
	}
	
	class ClearopenAction extends AbstractAction 
	{
		ClearopenAction() { super("clearopen"); }
		public void actionPerformed(ActionEvent e) 
		{
			inputitems.removeAllElements();
			//readseq= null;
	 		infomessage("Input data cleared. "); 
 			checkEnabledActions();
 		}
	}

	class OpenurlAction extends AbstractAction 
	{
		OpenurlAction() { super("openurl"); }

		public void actionPerformed(ActionEvent e) 
		{
			//super.actionPerformed(e);  //? none
			GetUrls getu= new GetUrls(AppFrame.this);
			//getu.setValue(selurls);
			if (getu.choose()) {
				//selurls= getu.getValue();  
	 			//inputitems.removeAllElements(); //? or append!?
				//readseq= null;
				FastVector v= getu.getValue(); //? or append to inputitems
	 			for (int i= 0; i < v.size(); i++) inputitems.addElement( v.elementAt(i)); 
   			infomessage("Will read data from these items: "); 
	 			for (int i= 0; i < inputitems.size(); i++) 
 					infomessage( String.valueOf(inputitems.elementAt(i))); 
 				checkEnabledActions();
				}
		}
	}


	class OpenAction extends AbstractAction {
		OpenAction() { super("open"); }

		public void actionPerformed(ActionEvent e) 
		{
	    if (fchooser == null) initFileChooser();
		 	fchooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES); //FILES_ONLY FILES_AND_DIRECTORIES
	    fchooser.setMultiSelectionEnabled(true);
	    	
			int returnVal = fchooser.showOpenDialog( AppFrame.this); //frame

   	 	if (returnVal == JFileChooser.APPROVE_OPTION) {
	 			//selfiles= fchooser.getSelectedFiles();
	 			File[] files= fchooser.getSelectedFiles();
				if (files.length==0) {
	 				File f= fchooser.getSelectedFile();
	 				if (f!=null) files= new File[] { f };
	 				}
				if (files.length==0) {
					errmessage("\n No files selected");
				} else {
	 				//inputitems.removeAllElements(); //? or append!?
					//readseq= null;
		 			for (int i= 0; i <files.length; i++) inputitems.addElement(files[i]); 
	   			infomessage("Will read data from these items: "); 
		 			for (int i= 0; i < inputitems.size(); i++) 
	 					infomessage( String.valueOf(inputitems.elementAt(i))); 
 					checkEnabledActions();
					}
      	}
			}
	}
    

 	class RunAction extends AbstractAction {
		RunAction() { super("run"); checkEnabled();  }

		void checkEnabled() {
			setEnabled( choosenOutputName!=null && (
				inputitems!=null && !inputitems.isEmpty()
				//   (selfiles!=null && selfiles.length>0)
				//|| (selurls!=null && selurls.length>0) 
				) );
			}

		/*public boolean isEnabled() {
			checkEnabled();
			return enabled;
    	}*/
	
		public void actionPerformed(ActionEvent e) 
		{
     	myapp.outname= choosenOutputName;  
			//myapp.setInputFiles( selfiles); 
			//myapp.setInputNames( selurls); 
			myapp.setInputObjects( inputitems);
			Thread loader= new Thread( myapp);// myapp is runnable
			loader.start();

			choosenOutputName= null; //??
			checkEnabled();
			}
	}
    
   
	/* class AppRunner extends Thread {
	AppRunner(app myapp, File[] inputfiles) {
	    //setPriority(4);
	    this.myapp = myapp;
	    this.inputfiles = inputfiles;
	    }
 		public void run() { }
	} */
	
	private class ValueMenuItem extends JMenuItem
	{
		String val, lab;
		ValueMenuItem(String label, String val) {
			super(label);
			this.lab= label;
			setValue(val);
			}
		void setValue(String val) {
			this.val= val;
			String title= lab;
			if (val!=null) title += " ["+val+"]";
			setText(title);
			}
	}	

  	
	protected void openActionFrame(JTextComponent jview, String title, String menuprop) 
	{
		JScrollPane jscroll= new JScrollPane(jview);
		Dimension  screenSize = getToolkit().getScreenSize();
		jscroll.setMaximumSize( new Dimension( screenSize.width - 200, screenSize.height - 100) );   
		//jscroll.setPreferredSize( new Dimension( screenSize.width - 200, screenSize.height - 100) );   

		Action[] actions = new Action[] {  
			new SaveTextAction(jview), 
			new CloseTextAction(jview), 
			new CopyTextAction(jview), 
			new PrintTextAction(jview) 
			};
		Hashtable acthash= new Hashtable();
		for (int i = 0; i < actions.length; i++) {
	    Action a = actions[i];
	    acthash.put( a.getValue(Action.NAME), a);
			}
		
		//JPanel jpan = new JPanel(new BorderLayout());
 		//jpan.add( jscroll,  BorderLayout.CENTER);
		//jpan.setBorder( new LineBorder( Color.lightGray, 6)); 
				
		JFrame frame= new JFrame(title);  
		JMenuBar mbar= AppFrame.this.createMenubar(menuprop, acthash); //"details.menubar"   
		if (mbar!=null) frame.setJMenuBar( mbar);  

		frame.getContentPane().add(jscroll);//jpan
    frame.pack();
    frame.setLocation( 90, 50);  
    frame.setVisible(true);
	}
	   
		//
    // --- menu methods -----------------------------------
		//

	protected JMenuBar createMenubar(String propkey, Hashtable acthash) {
		JMenuBar mb = new JMenuBar();
		String[] menuKeys = tokenize( props.getProperty(propkey));
		for (int i = 0; i < menuKeys.length; i++) {
		 	JMenu m = createMenu( menuKeys[i], acthash);
		 	if (m != null) mb.add(m);
			}
		return mb;
    }


	protected JMenu createMenu(String key, Hashtable acthash) 
	{
		String[] itemKeys = tokenize( props.getProperty(key));
		String label=  props.getProperty(key + labelSuffix);
		if (label==null) label= key;
		JMenu menu = new JMenu( label);
		for (int i = 0; i < itemKeys.length; i++) {
	    if (itemKeys[i].equals("-")) menu.addSeparator();
	  	else {
				JMenuItem mi = createMenuItem(itemKeys[i], acthash);
				menu.add(mi);
	    	}
			}
		return menu;
	}
	
	//ButtonGroup bgroup;
	Hashtable radiogroups= new Hashtable();

	
	protected JMenuItem createMenuItem(String cmd, Hashtable acthash) 
	{
		String astr = props.getProperty(cmd + actionSuffix);
		if (astr == null) astr = cmd;
		//Action act = getAction(astr);
		Action act = (acthash==null) ? null : (Action) acthash.get(cmd); 

		boolean ischeck= false, isradio= false, checkval= false;
		String cmdlab= props.getProperty(cmd + checkedSuffix);
		if (cmdlab!=null) ischeck= true;
		else { 
			cmdlab= props.getProperty(cmd + radioSuffix); 
			if (cmdlab!=null) isradio= true;
			else cmdlab= props.getProperty(cmd + labelSuffix);  
			}
			
		JMenuItem mi;
		if (ischeck) {
			Boolean bn= (act==null) ? null : (Boolean)act.getValue("BoolValue");
			boolean val= (bn!=null) ? bn.booleanValue() : checkval;
			mi= new JCheckBoxMenuItem(cmdlab, val); 
			}
			
		else if (isradio) {
			Boolean bn= (act==null) ? null : (Boolean)act.getValue("BoolValue");
			boolean val= (bn!=null) ? bn.booleanValue() : checkval;
			JRadioButtonMenuItem rbm= new JRadioButtonMenuItem(cmdlab, val);   
			mi= rbm;
			
			String bgname= (act==null) ? "bgroup" : (String)act.getValue("Group");
			//String bgname = props.getProperty(cmd + radioGroupSuffix);
			ButtonGroup bg= (ButtonGroup)radiogroups.get(bgname);
			if (bg==null) {
				bg= new ButtonGroup();
				radiogroups.put(bgname, bg);
				}
			bg.add(rbm);
			}
			
		else {
			String sval= (act==null) ? null : (String)act.getValue("StringValue");
			if (sval!=null) mi= new ValueMenuItem(cmdlab, sval);
			else mi = new JMenuItem(cmdlab);
			}

		mi.setActionCommand(astr);
		String tip= props.getProperty(cmd + tooltipSuffix);  
		if (tip!=null) mi.setToolTipText(tip);  
		
		if (act != null) {
			PropertyChangeListener actli= new ActionChangedListener(mi);
			mi.addActionListener(act);
			act.addPropertyChangeListener( actli);
			mi.setEnabled( act.isEnabled());
		} else {
			mi.setEnabled(false);
		}
		menuItems.put(cmd, mi);
		return mi;
	}


	
    // Yarked from JMenu, ideally this would be public.
	private class ActionChangedListener implements PropertyChangeListener 
	{
    JMenuItem menuItem;
    
    ActionChangedListener(JMenuItem mi) {
    	super();
     	this.menuItem = mi;
    	}
    public void propertyChange( PropertyChangeEvent e) {
      String propertyName = e.getPropertyName();
      if (propertyName.equals(Action.NAME)) {
        String text = (String) e.getNewValue();
        menuItem.setText(text);
      } else if (propertyName.equals("enabled")) {
        Boolean enabledState = (Boolean) e.getNewValue();
        menuItem.setEnabled(enabledState.booleanValue());
      } else if (propertyName.equals("StringValue")) {
        String text = (String) e.getNewValue();
        if (menuItem instanceof ValueMenuItem) ((ValueMenuItem)menuItem).setValue(text);
      }
    }
	}


	protected String[] tokenize(String input) {
		Vector v = new Vector();
		StringTokenizer t = new StringTokenizer(input);
		String cmd[];
		while (t.hasMoreTokens())  v.addElement(t.nextToken());
		cmd = new String[v.size()];
		for (int i = 0; i < cmd.length; i++)  cmd[i] = (String) v.elementAt(i);
		return cmd;
    }

}


/*
	//! have to capture all the print() and println() of PrintStrem class
class StdoutPrintStream extends PrintStream
{
	AppFrame apf;
	
	public StdoutPrintStream(AppFrame apf) {
		this(apf, System.out, false);
		}

	protected StdoutPrintStream(AppFrame apf, PrintStream out, boolean flush) {
		super(out, flush);
		this.apf= apf;
		}
		
	protected void write(String s) {
		apf.infomessage(s);
		}
}

class StderrPrintStream extends StdoutPrintStream
{
	AppFrame apf;
	
	public StdoutPrintStream(AppFrame apf) {
		super(apf, System.err, true);
		this.apf= apf;
		}

	protected void write(String s) {
		apf.errmessage(s);
		}

}

*/

