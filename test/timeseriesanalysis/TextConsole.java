package timeseriesanalysis;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.rosuda.JRI.RMainLoopCallbacks;
import org.rosuda.JRI.Rengine;

public class TextConsole implements RMainLoopCallbacks {

	public TextConsole() {
		// TODO Auto-generated constructor stub
		
	}

	@Override
	public void rBusy(Rengine arg0, int which) {
		// TODO Auto-generated method stub
		   System.out.println("rBusy("+which+")");
	}

	@Override
	  public String rChooseFile(Rengine re, int newFile) {
		FileDialog fd = new FileDialog(new Frame(), (newFile==0)?"Select a file":"Select a new file", (newFile==0)?FileDialog.LOAD:FileDialog.SAVE);
		fd.show();
		String res=null;
		if (fd.getDirectory()!=null) res=fd.getDirectory();
		if (fd.getFile()!=null) res=(res==null)?fd.getFile():(res+fd.getFile());
		return res;
	    }

	@Override
	public void rFlushConsole(Rengine arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void rLoadHistory(Rengine arg0, String arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public String rReadConsole(Rengine re, String prompt, int addToHistory) {
        System.out.print(prompt);
        try {
            BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
            String s=br.readLine();
            return (s==null||s.length()==0)?s:s+"\n";
        } catch (Exception e) {
            System.out.println("jriReadConsole exception: "+e.getMessage());
        }
        return null;
    }

	@Override
	public void rSaveHistory(Rengine arg0, String arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	  public void rShowMessage(Rengine re, String message) {
        System.out.println("rShowMessage \""+message+"\"");
    }

	@Override
	  public void rWriteConsole(Rengine re, String text, int oType) {
		
        System.out.print(text);
    }

}

