package com.njust.test;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;

import com.njust.helper.RServeConnection;

public class Panel_Test_SWT
{

	protected Shell shell;

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args)
	{
		try
		{
			Panel_Test_SWT window = new Panel_Test_SWT();
			window.open();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Open the window.
	 */
	public void open()
	{
		Display display = Display.getDefault();
		createContents();
		shell.open();
		shell.layout();
		while (!shell.isDisposed())
		{
			if (!display.readAndDispatch())
			{
				display.sleep();
			}
		}
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents()
	{
		shell = new Shell();
		shell.setSize(800, 660);
		shell.setText("SWT Application");
		
		Button btn_run = new Button(shell, SWT.NONE);
		btn_run.setBounds(694, 583, 80, 27);
		btn_run.setText("运行");
		
		final Label lblNewLabel = new Label(shell, SWT.NONE);
		lblNewLabel.setBounds(10, 10, 600, 600);
		
		final RServeConnection rsc = new RServeConnection();
		rsc.setFolderPath("D", "R-Data", "garch");
		
		final List list = new ArrayList();
		
		
		btn_run.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				rsc.make(list);
				Display display = Display.getDefault();
				Image image = new Image(display, rsc.getFilePath());
				ImageData data=image.getImageData();
				data=data.scaledTo(600, 600);
				image=new Image(display,data);
				lblNewLabel.setImage(image);
			}
		});
	}
}