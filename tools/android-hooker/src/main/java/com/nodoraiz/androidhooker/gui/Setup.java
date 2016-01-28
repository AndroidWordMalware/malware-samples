package com.nodoraiz.androidhooker.gui;

import com.nodoraiz.androidhooker.utils.Basics;
import com.nodoraiz.androidhooker.utils.Configuration;
import com.nodoraiz.androidhooker.models.HookerException;

import javax.swing.*;
import java.awt.event.*;
import java.io.File;

public class Setup extends JDialog {

    private static class ComboItem{
        private String key;
        private String value;

        public ComboItem(String key, String value)
        {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString()
        {
            return key;
        }

        public String getKey()
        {
            return key;
        }

        public String getValue()
        {
            return value;
        }
    }

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox comboBoxBuildTools;
    private JTextField textFieldSdkDir;
    private JComboBox comboBoxPlatform;
    private JButton buttonSetSdkDir;
    private JTextField textFieldTimeout;

    public Setup() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        this.textFieldTimeout.setHorizontalAlignment(JTextField.CENTER);

        if(Configuration.isValidConfiguration()){
            this.loadFormValues();
        }

        this.prepareEvents();
    }

    private void prepareEvents() {

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        buttonSetSdkDir.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Setup.this.setupSdkDir();
            }
        });
    }

    private void loadFormValues() {

        this.textFieldSdkDir.setText(Configuration.ANDROID_SDK_DIR.getAbsolutePath());
        this.textFieldTimeout.setText("" + Configuration.COMMAND_SECONDS_TIMEOUT);
        this.setupSdkDir();

        for(int i=0 ; i<this.comboBoxBuildTools.getItemCount() ; i++){
            if(Configuration.ANDROID_BUILD_TOOLS.getAbsolutePath().equals(((ComboItem)this.comboBoxBuildTools.getItemAt(i)).getValue())){
                this.comboBoxBuildTools.setSelectedIndex(i);
                break;
            }
        }

        for(int i=0 ; i<this.comboBoxPlatform.getItemCount() ; i++){
            if(Configuration.ANDROID_PLATFORM.getAbsolutePath().equals(((ComboItem) this.comboBoxPlatform.getItemAt(i)).getValue())){
                this.comboBoxPlatform.setSelectedIndex(i);
                break;
            }
        }
    }

    private void setupSdkDir() {

        File sdkDir = new File(this.textFieldSdkDir.getText());
        File buildToolsDir = new File(this.textFieldSdkDir.getText() + "/build-tools");
        File platformsDir = new File(this.textFieldSdkDir.getText() + "/platforms");
        if(!sdkDir.exists() || !buildToolsDir.exists() || !platformsDir.exists()){
            JOptionPane.showMessageDialog(this, "The specified Android SDK dir doesn't exist or doesn't contain a dir with the build-tools and platforms");
            return;
        }

        for(File dir : buildToolsDir.listFiles()){
            this.comboBoxBuildTools.addItem(new ComboItem(dir.getName(), dir.getAbsolutePath()));
        }
        if(this.comboBoxBuildTools.getItemCount() == 0){
            JOptionPane.showMessageDialog(this, "There aren't build tools in " + buildToolsDir.getAbsolutePath());
            return;
        }

        for(File dir : platformsDir.listFiles()){
            this.comboBoxPlatform.addItem(new ComboItem(dir.getName(), dir.getAbsolutePath()));
        }
        if(this.comboBoxPlatform.getItemCount() == 0){
            JOptionPane.showMessageDialog(this, "There aren't platforms in " + platformsDir.getAbsolutePath());
            return;
        }
    }

    private void onOK() {

        try {
            if( !Configuration.saveConfiguration(this.textFieldSdkDir.getText(),
                    ((ComboItem) this.comboBoxBuildTools.getSelectedItem()).getValue(),
                    ((ComboItem) this.comboBoxPlatform.getSelectedItem()).getValue(),
                    Integer.parseInt(this.textFieldTimeout.getText())) ){

                JOptionPane.showMessageDialog(this, "Please review the setup, there are invalid fields.");
                return;
            }

        } catch (HookerException e) {
            JOptionPane.showMessageDialog(this, "There was a problem: " + e.getHookerExceptionMessage());
            Basics.logError(e);
            return;

        } catch (NumberFormatException e){
            JOptionPane.showMessageDialog(this, "Please review the setup, there are invalid fields.");
            Basics.logError(e);
            return;
        }

        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public static void showDialog(){
        Setup dialog = new Setup();
        dialog.pack();
        dialog.setTitle("Configuration");
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
}
