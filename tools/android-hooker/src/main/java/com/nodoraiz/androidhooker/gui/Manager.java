package com.nodoraiz.androidhooker.gui;

import com.nodoraiz.androidhooker.models.HookerException;
import com.nodoraiz.androidhooker.utils.AdbHandler;
import com.nodoraiz.androidhooker.utils.Basics;
import com.nodoraiz.androidhooker.utils.Configuration;
import com.nodoraiz.androidhooker.utils.Worker;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;

public class Manager {

    private JPanel panel1;
    private JList listAPKs;
    private JButton buttonExtract;
    private JButton buttonRefresh;
    private JList listPackages;
    private JLabel logLabel;
    private JButton buttonHook;
    private JButton buttonLogcat;
    private JTextField textFieldIP;
    private JFrame myJFrame;
    private String deviceId;

    public Manager(JFrame jFrame) {

        this.myJFrame = jFrame;
        this.myJFrame.setJMenuBar(this.createJMenuBar());
        this.prepareEvents();

        this.textFieldIP.setHorizontalAlignment(JTextField.CENTER);

        this.handleToolConfiguration();
    }

    /**
     * Check if the configuration has valid values and if they aren't valid then shows the setup dialog.
     *
     * @return TRUE if all is OK, FALSE if the configuration is invalid in the end
     */
    private boolean handleToolConfiguration(){

        boolean needsSetup = true;
        try {
            needsSetup = !Configuration.loadSetup();
        } catch (HookerException e) {
            Basics.logError(e);
        }

        if(needsSetup){
            Setup.showDialog();
        }

        return Configuration.isValidConfiguration();
    }

    private void submitWork(Callable<Void> callable){

        if(!this.handleToolConfiguration()){
            JOptionPane.showMessageDialog(Manager.this.myJFrame, "You have to configure the tool before use it");
            return;
        }

        if(!Worker.work(callable, Configuration.COMMAND_SECONDS_TIMEOUT)){
            JOptionPane.showMessageDialog(Manager.this.myJFrame, "Already processing an action, wait for the response or the timeout");
        }
    }

    private void prepareEvents() {

        buttonRefresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                Manager.this.submitWork(new Callable<Void>() {
                    public Void call() throws Exception {

                        try {
                            Manager.this.log("Connecting and retrieving apps from the device");

                            //connect to the device
                            if(!Manager.this.textFieldIP.getText().isEmpty()) {
                                AdbHandler.connectDevice(Manager.this.textFieldIP.getText());
                            }

                            Manager.this.deviceId = AdbHandler.getDeviceId(Manager.this.textFieldIP.getText());
                            if(Manager.this.deviceId == null || Manager.this.deviceId.isEmpty()){
                                Manager.this.log("Can't connect to the specified device");
                                return null;
                            }

                            String[] apps = AdbHandler.getInstalledApps(
                                    Manager.this.deviceId,
                                    new Observer() {
                                        @Override
                                        public void update(Observable o, Object arg) {
                                            if (arg instanceof String) Manager.this.log((String) arg);
                                        }
                                    }
                            );

                            if(apps == null) {
                                Manager.this.log("Can't retrieve installed apps from " + Manager.this.textFieldIP.getText() + ":5555, try again");

                            } else {
                                Manager.this.listAPKs.removeAll();
                                Manager.this.listAPKs.setListData(apps);
                                Manager.this.log("Connected to device!");
                            }

                        } catch (Exception e) {
                            Manager.this.log("There was a problem: " + e.getMessage());
                            Basics.logError(e);
                        }

                        return null;
                    }
                });
            }
        });

        buttonExtract.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                Manager.this.submitWork(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {

                        if(Manager.this.deviceId == null || Manager.this.deviceId.isEmpty()){
                            Manager.this.log("You have to be connected with a device first");
                            return null;
                        }

                        if(Manager.this.listAPKs.getSelectedValue() == null || Manager.this.listAPKs.getSelectedValue().toString().isEmpty()){
                            Manager.this.log("You have to select and app first");
                            return null;
                        }

                        try {
                            String[] classes = AdbHandler.extractClassesFromSelectedApp(
                                    listAPKs.getSelectedValue().toString(),
                                    Manager.this.deviceId,
                                    new Observer() {
                                        @Override
                                        public void update(Observable o, Object arg) {
                                            if(arg instanceof String) Manager.this.log((String)arg);
                                        }
                                    }
                            );
                            if(classes != null) {
                                Manager.this.listPackages.setListData(classes);
                            }

                        } catch (Exception e){
                            Manager.this.log("There was a problem: " + e.getMessage());
                            Basics.logError(e);
                        }

                        return null;
                    }
                });
            }
        });

        buttonHook.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                Manager.this.submitWork(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {

                        if(Manager.this.deviceId == null || Manager.this.deviceId.isEmpty()){
                            Manager.this.log("You have to be connected with a device first");
                            return null;
                        }

                        if(Manager.this.listPackages.getSelectedValuesList().isEmpty()){
                            Manager.this.log("You have to select first some classes");
                            return null;
                        }

                        try {
                            AdbHandler.compileAndInstall(
                                    Manager.this.listPackages.getSelectedValuesList(),
                                    Manager.this.deviceId,
                                    new Observer() {
                                        @Override
                                        public void update(Observable o, Object arg) {
                                            if(arg instanceof String) Manager.this.log((String)arg);
                                        }
                                    }
                            );

                        } catch (Exception e) {
                            Manager.this.log("There was a problem: " + e.getMessage());
                            Basics.logError(e);
                        }

                        return null;
                    }
                });
            }
        });

        buttonLogcat.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {

                try {
                    Logcat.showDialog(Manager.this.deviceId);

                } catch (HookerException e) {
                    Manager.this.log("There was a problem: " + e.getHookerExceptionMessage());
                    Basics.logError(e);
                }
            }
        });
    }

    private JMenuBar createJMenuBar() {

        JMenuBar result = new JMenuBar();
        JMenu jMenu = new JMenu("File");
        result.add(jMenu);

        JMenuItem jMenuItem = new JMenuItem("Setup");
        jMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Setup.showDialog();
            }
        });
        jMenu.add(jMenuItem);

        jMenuItem = new JMenuItem("Exit");
        jMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Manager.this.myJFrame.dispose();
            }
        });
        jMenu.add(jMenuItem);

        return result;
    }

    private void log(String text){
        this.logLabel.setText(text);
    }

    public static void main(String[] args) {

//        Configuration.setUItheme();

        try {
            Configuration.initApp();
        } catch (HookerException e) {
            Basics.logError(e);
            JOptionPane.showMessageDialog(null, "The app can't be started. Reason: " + e.getHookerExceptionMessage());
            return;
        }

        JFrame frame = new JFrame("Android hooker");
        frame.setContentPane(new Manager(frame).panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
