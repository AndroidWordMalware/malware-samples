package com.nodoraiz.androidhooker.gui;

import com.nodoraiz.androidhooker.models.HookerException;
import com.nodoraiz.androidhooker.utils.AdbHandler;
import com.nodoraiz.androidhooker.utils.Basics;
import com.nodoraiz.androidhooker.utils.Configuration;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Logcat extends JDialog {

    private static class MethodInvoked{

        private final static String SPACE = "  ";

        private String signature;
        private String parameters;
        private int depth;

        public String getSignature() {
            return signature;
        }

        public String getParameters() {
            return parameters;
        }

        public int getDepth() {
            return depth;
        }

        public MethodInvoked(String signature, String parameters, int depth) {
            this.signature = signature;
            this.parameters = parameters;
            this.depth = depth;
        }

        private String getSpaces(){

            StringBuffer stringBuffer = new StringBuffer();
            for(int i=0 ; i<this.depth ; i++){
                stringBuffer.append(SPACE);
            }
            return stringBuffer.toString();
        }

        @Override
        public String toString() {
            return this.getSpaces() + this.signature;
        }
    }

    private final static String BREADCRUMB_TOKEN = "HOOK_LOG_BREADCRUMB";
    private final static String TOKEN_ENTER_METHOD = "ENTER ";
    private final static String TOKEN_EXIT_METHOD = "EXIT";
    private final static String PARAMETERS_SEPARATOR = "##P_S##";
    private final static int MAX_LOG_LINES = 1024;
    private final static int CHECK_LOGCAT_INTERVAL_IN_MILISECONDS = 2000;
    private final static int HIDE_ERROR_MESSAGE = -1;

    private JPanel contentPane;
    private JButton buttonOK;
    private JList listLog;
    private JTextArea textAreaParameters;
    private Timer timer;

    private LinkedList<MethodInvoked> methodInvokedList;
    private int spaces = 0;
    private boolean dumpEnabled = true;
    private String deviceId;
    private ExecutorService executorService;
    private boolean working = false;
    private int retryCounter = 0;

    public Logcat() {

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        this.methodInvokedList = new LinkedList<MethodInvoked>();
        this.prepareEvents();
    }

    private void prepareEvents() {

        this.buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        this.listLog.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (Logcat.this.listLog.getSelectedValue() != null) {
                    MethodInvoked methodInvoked = (MethodInvoked) Logcat.this.listLog.getSelectedValue();
                    Logcat.this.textAreaParameters.setText(methodInvoked.getParameters());
                }
            }
        });

        this.addWindowListener(new WindowListener() {

            @Override
            public void windowClosed(WindowEvent e) {

                // disable timer before exit
                if (Logcat.this.timer != null) {
                    Logcat.this.timer.stop();
                    for (ActionListener actionListener : Logcat.this.timer.getActionListeners()) {
                        Logcat.this.timer.removeActionListener(actionListener);
                    }
                }

                // dump last invoked methods detected
                Logcat.this.dumpInvokedMethodsToFile(Logcat.this.methodInvokedList);
            }

            @Override
            public void windowOpened(WindowEvent e) {
            }

            @Override
            public void windowClosing(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
            }

            @Override
            public void windowActivated(WindowEvent e) {
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }
        });

        this.timer = new Timer(Logcat.CHECK_LOGCAT_INTERVAL_IN_MILISECONDS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    if(!Logcat.this.working){

                        Logcat.this.working = true;
                        Logcat.this.executorService = Executors.newSingleThreadExecutor();
                        executorService.submit(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {

                                Logcat.this.refreshFromLogcat();
                                Logcat.this.working = false;
                                return null;
                            }
                        });
                        Logcat.this.executorService.shutdown();

                    } else if(Logcat.this.retryCounter != HIDE_ERROR_MESSAGE && Logcat.this.retryCounter < 5) {
                        Logcat.this.retryCounter++;

                    } else if (Logcat.this.retryCounter != HIDE_ERROR_MESSAGE) {
                        JOptionPane.showMessageDialog(null, "The connection with the device seems to be broken, please try to reconnect with it.");
                        Logcat.this.retryCounter = HIDE_ERROR_MESSAGE;
                    }

                } catch (Exception exception){
                    Basics.logError(exception);
                }
            }
        });

        this.timer.start();
    }

    private void refreshFromLogcat() {

        try{
            // look for breadcrumbs
            for (String line : AdbHandler.readLogcat(true, deviceId)) {

                if (line.contains(BREADCRUMB_TOKEN)){

                    if(line.contains(TOKEN_ENTER_METHOD)) {

                        String[] tokens = line.substring(line.indexOf(TOKEN_ENTER_METHOD) + TOKEN_ENTER_METHOD.length()).split(PARAMETERS_SEPARATOR);
                        if(tokens.length == 2) {
                            Logcat.this.methodInvokedList.addFirst(new MethodInvoked(tokens[0], tokens[1], spaces));
                            spaces++;
                        }

                    } else if(line.contains(TOKEN_EXIT_METHOD)){
                        if(spaces > 0) spaces--;
                    }
                }
            }

            if(Logcat.this.methodInvokedList.size() > MAX_LOG_LINES){
                Logcat.this.handleLogOverflow();
            }

            Logcat.this.listLog.setListData(Logcat.this.methodInvokedList.toArray(new Object[Logcat.this.methodInvokedList.size()]));

        } catch (Exception exception){
            Basics.logError(exception);
            JOptionPane.showMessageDialog(null, "There was a problem: " + exception.getMessage());
        }
    }

    private void handleLogOverflow() {

        ArrayList<MethodInvoked> exceededSublist = null;
        if(this.dumpEnabled) {
            // get exceeded sublist to dump into file
            exceededSublist = new ArrayList<MethodInvoked>(
                    this.methodInvokedList.subList(Logcat.MAX_LOG_LINES, this.methodInvokedList.size())
            );
            // reverse the order of the list, in the dump last line will be the latest method invoked detected
            Collections.reverse(exceededSublist);
        }

        // remove exceeded part from the list
        LinkedList<MethodInvoked> linkedList = new LinkedList<MethodInvoked>(this.methodInvokedList.subList(0, Logcat.MAX_LOG_LINES));
        this.methodInvokedList.clear();
        this.methodInvokedList = linkedList;

        if(this.dumpEnabled && exceededSublist != null) {
            this.dumpInvokedMethodsToFile(exceededSublist);
        }
    }

    private void dumpInvokedMethodsToFile(List<MethodInvoked> methodInvokedList){

        if(this.dumpEnabled) {
            // dump into file in inverted order, last line will be the latest method invoked detected
            StringBuffer stringBuffer = new StringBuffer();
            for (MethodInvoked methodInvoked : methodInvokedList) {
                stringBuffer.append(methodInvoked.toString() + " => " + methodInvoked.getParameters() + "\n");
            }

            try {
                Basics.writeFile(Configuration.LOGCAT_FILE.getAbsolutePath(), stringBuffer.toString(), true);

            } catch (HookerException e) {
                Basics.logError(e);
                this.dumpEnabled = false;
                JOptionPane.showMessageDialog(null, "The dump of the detected methods invoked was disabled. Reason: " + e.getMessage());
            }
        }
    }

    private void onOK() {

        dispose();
    }

    public static void showDialog(String deviceId) throws HookerException {

        if(deviceId == null || deviceId.isEmpty()){
            throw new HookerException(new IllegalArgumentException(), "You have to be connected with a device first");
        }

        Logcat dialog = new Logcat();
        dialog.deviceId = deviceId;
        dialog.pack();
        dialog.setTitle("Hooker logcat");
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
}
