package com.bigjson.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;

public class MainFrame extends JFrame {
	private static final long serialVersionUID = 1L;

	// TODO: remove this
	private static final String defaultFileFolder = "C:/Users/nikanka/JSONViewer/JSONFiles";

	private JTabbedPane tabbedPane;

	public static void main(String[] args) throws IOException {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
	}
	
	public MainFrame(String title){
		super(title);
		// TODO: add close Xs to tabs and dispose tabs properly 
		tabbedPane = new JTabbedPane();
	    setContentPane(tabbedPane);
		setJMenuBar(createMenuBar());  
	}
	
	private static void createAndShowGUI() {
		JFrame mainFrame = new MainFrame("JSON Tree View");
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setPreferredSize(new Dimension(1200, 800));
//		mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH); 
		mainFrame.pack();
		mainFrame.setVisible(true);
    }
	
	private JMenuBar createMenuBar(){
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenu loadFileMenu = new JMenu("Load tree from JSON file");
		JMenuItem justLoadItem = new JMenuItem("Just load");
		JMenuItem validateItem = new JMenuItem("Load and vaidate (can take more time)");
		loadFileMenu.add(justLoadItem);
		loadFileMenu.add(validateItem);
		fileMenu.add(loadFileMenu);
		menuBar.add(fileMenu);
		justLoadItem.addActionListener(new OpenFileListener(false));
		validateItem.addActionListener(new OpenFileListener(true));
		return menuBar;
	}
	
	
	private void createNewTabForFile(File file, boolean validate){
		JSONTreeViewPanel treeViewer = new JSONTreeViewPanel(file, validate);
		tabbedPane.addTab(file.getName(), treeViewer);
		tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
	}

	public static void showDialog(String message){
		JOptionPane.showMessageDialog(null, message);
	}

	private class OpenFileListener implements ActionListener{
		boolean validate = false;
		JFileChooser fileChooser = new JFileChooser(defaultFileFolder);
		
		OpenFileListener(boolean validate) {
			this.validate = validate;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			int returnVal = fileChooser.showOpenDialog(MainFrame.this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				createNewTabForFile(file, validate);
			}
		}
		
	}

}
