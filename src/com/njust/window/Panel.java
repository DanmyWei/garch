package com.njust.window;

import java.util.ArrayList;
import java.util.List;
import java.io.*;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;

import com.njust.helper.RServeConnection;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;

public class Panel
{
	protected Shell shell;
	private String inputFilePath;
	private Text text_input;

	/**
	 * Launch the application.
	 * 
	 * @param args
	 */
	public String getInputFilePath()
	{
		return inputFilePath;
	}

	public static void main(String[] args)
	{
		try
		{
			Panel window = new Panel();
			window.open();
		} catch (Exception e1)
		{
			e1.printStackTrace();
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
		shell.setSize(634, 729);
		shell.setText("基于GARCH（异方差时间序列模型）的价格预测");

		Button btn_run = new Button(shell, SWT.NONE);
		btn_run.setBounds(531, 653, 80, 27);
		btn_run.setText("分析");

		Button btn_open = new Button(shell, SWT.NONE);
		btn_open.setBounds(531, 616, 80, 27);
		btn_open.setText("打开");

		final Label lblNewLabel = new Label(shell, SWT.BORDER);
		lblNewLabel.setBounds(10, 10, 600, 600);

		final FileDialog fileDialog = new FileDialog(shell);

		text_input = new Text(shell, SWT.BORDER | SWT.LEFT);
		text_input.setBounds(97, 616, 417, 27);

		Label label = new Label(shell, SWT.NONE);
		label.setFont(SWTResourceManager.getFont("Microsoft YaHei UI", 14,
				SWT.NORMAL));
		label.setAlignment(SWT.CENTER);
		label.setBounds(10, 616, 81, 27);
		label.setText("数据文件");

		final RServeConnection rsc = new RServeConnection();
		rsc.setFolderPath("D", "R-Data", "garch");

		final List list = new ArrayList();

		list.add("library(TSA)");
		list.add("set.seed(1234567)");
		list.add("test.sim = garch.sim(alpha = c(0.02,0.05), beta = .9, n = 500)");
		list.add("plot(test.sim, type = 'o', ylab = expression(r[t]), xlab = 't')");

		btn_run.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				try
				{
					rsc.make(list);
					Display display = Display.getDefault();
					Image image = new Image(display, rsc.getFilePath());
					ImageData data = image.getImageData();
					data = data.scaledTo(600, 600);
					image = new Image(display, data);
					lblNewLabel.setImage(image);
				}catch(Exception ex)
				{
					System.out.println(ex.toString());
				}
			}
		});

		btn_open.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				fileDialog.open();

				if (!fileDialog.getFileName().isEmpty())
					inputFilePath = fileDialog.getFilterPath() + "\\"
							+ fileDialog.getFileName();
				else
					inputFilePath = "";
				text_input.setText(inputFilePath);
				inputFilePath = inputFilePath.replace("\\", "/");
				System.out.println("FilePath: "+inputFilePath);
			}
		});
	}
}
