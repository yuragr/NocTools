/*
 * NocToolsView.java
 */

package noctools;

import noctools.settings.NocToolsSettings;
import noctools.settings.NocToolsSettingsDialog;
import noctools.link.LinksEditor;
import noctools.link.LinkHandler;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.*;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import noctools.log.*;
import noctools.rdp.*;
import noctools.script.*;
import noctools.endpoint.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import javax.swing.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import noctools.startup.StartupManager;
import noctools.startup.StartupQueue;
import org.apache.log4j.*;
/**
 * The application's main frame.
 */
public class NocToolsView extends FrameView
{

    public NocToolsView(SingleFrameApplication app)
    {
        super(app);
        //org.apache.log4j.BasicConfigurator.configure();
        DailyRollingFileAppender dailyAppender = new DailyRollingFileAppender();

        // some logging settings
        dailyAppender.setDatePattern("'.'yyyy-MM-dd'.log'");
        dailyAppender.setFile(NocToolsSettings.getLogsDir() + "nocTools.log");
        PatternLayout logLayout = new PatternLayout("[%5p] %d{dd-MM-yyyy HH:mm:ss,SSS} (%F:%M:%L)%n%m%n%n");
        dailyAppender.setLayout(logLayout);
        dailyAppender.setName("FileAppender");
        dailyAppender.activateOptions();
        Logger.getRootLogger().addAppender(dailyAppender);
        Logger.getRootLogger().setLevel(Level.INFO);
        // when updating version number, please make sure to update it also in noctools.resources.NocToolsApp.properties
        Logger.getRootLogger().info("********************* Starting Noc Tools 5.7.3 *********************");

        // start the NocToolsWorker (a SwingWorker that waits for tasks and executs them)
        NocToolsWorker.getInstance().execute();

        //init all the GUI
        initComponents();

        //load the icon
        try {
            this.getFrame().setIconImage(new ImageIcon("icon.png").getImage());
        }
        catch (Exception e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }

        initAllSettings();

        // init the startupQueue
        serversQueueList.setModel(new DefaultListModel());
        StartupQueue.getInstance(serversQueueList);

        // init the server startup manager
        StartupManager.getInstance(serverSartupStatusList).setDaemon(true);
        StartupManager.getInstance(serverSartupStatusList).start();

        deleteOldLogFiles();

        /////////////////////////// GENERATED NETBEANS CODE FROM HERE ///////////////////////////////////////

        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++)
        {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener()
        {
            public void propertyChange(java.beans.PropertyChangeEvent evt)
            {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName))
                {
                    if (!busyIconTimer.isRunning())
                    {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                }
                else if ("done".equals(propertyName))
                {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                }
                else if ("message".equals(propertyName))
                {
                    String text = (String)(evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                }
                else if ("progress".equals(propertyName))
                {
                    int value = (Integer)(evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });
    }

    public void initAllSettings()
    {
        // temporarily disable the logs tab
        //tabsPane.setEnabledAt(2, false);

        // temporarily select this option, because there are no other options
        byLogNumberRadioButton.setSelected(true);

        remotesArray = new javax.swing.JButton[REMOTES];
        remotesArray[0] = remote1Button;
        remotesArray[1] = remote2Button;
        remotesArray[2] = remote3Button;
        remotesArray[3] = remote4Button;
        remotesArray[4] = remote5Button;
        remotesArray[5] = remote6Button;
        remotesArray[6] = remote7Button;
        remotesArray[7] = remote8Button;

        // load the settings.xml file
        NocToolsSettings.initSettings();

        // init logging level
        setLoggingLevel();

        // hide remote buttons if needed (except remote1)
        for (int i = 1; i < remotesArray.length; i++) // don't hide remote1
            remotesArray[i].setVisible(NocToolsSettings.getShowRemoteButtons());        

        // set the initial state for the menu checkbox items
        showCountriesCheckBoxMenuItem.setState(NocToolsSettings.getShowCountries());
        showFarmsCheckBoxMenuItem.setState(NocToolsSettings.getShowFarms());
        enableLinkMenusCheckBoxMenuItem.setState(NocToolsSettings.getShowLinks());
        rdpRestrictionCheckBoxMenuItem.setState(NocToolsSettings.getRdpRestriction());
        showRemoteButtonsCheckBoxMenuItem.setState(NocToolsSettings.getShowRemoteButtons());
        showServerStartuplCheckBoxMenuItem.setState(NocToolsSettings.getShowServerStartupPanel());
        serverStartupPanel.setVisible(NocToolsSettings.getShowServerStartupPanel());

        // load the scripts to the list objects from scripts.xml file and keep them in the ScriptsManager
        ScriptsManager.loadScriptsFromXml();

        initScriptsTable();

        try
        {
            // generate a tree of ComparableMutableTreeNodes(Servers/farms/countries) from config.xml
            ComparableMutableTreeNode root = NocToolsSettings.getTree();

            // create a new CheckboxTree out of the root of the tree
            serversTree = new CheckboxTree(root);

            // remove CTRL + A from the tree
            KeyStroke ks = KeyStroke.getKeyStroke("ctrl A");
            serversTree.getInputMap().put(ks, "none");

            // add listeners and other configuration to the tree
            initTree(serversTree);
            ServersTreeManager.createServersList(root);
            nocButton.setEnabled(true);
            opssupButton.setEnabled(true);
            serversScriptsList.setEnabled(true);
            updateTreeButtons();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            serversTree = null;
            nocButton.setEnabled(false);
            opssupButton.setEnabled(false);
            serversScriptsList.clearSelection();
            serversScriptsList.setEnabled(false);
            clearAllButton.setEnabled(false);
            expandOneLevelButton.setEnabled(false);
            expandAllButton.setEnabled(false);
            collapseAllButton.setEnabled(false);
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().fatal(e.getMessage());
        }

        // add the servers tree to the screen
        treeScrollPane.setViewportView(serversTree);

        // get the "quick" scripts from the ScriptsManager and put them in to the scriptlists
        updateServersScriptsList();
        updateCustomScriptsList();
        updateStatusMessage();
        updateRemoteButtons();
        updateEmailRecipientsComboBox();
        updateShowWorkMode();
        updateShowTreeButtons();

        leftDividerLocation = leftSplitPane.getDividerLocation();
        rightDividerLocation = rightSplitPane.getDividerLocation();

        DefaultListModel logExtractionStatusListModel = new DefaultListModel();
        extractionStatusList.setModel(logExtractionStatusListModel);

        DefaultListModel serverStartupStatusListModel = new DefaultListModel();
        serverSartupStatusList.setModel(serverStartupStatusListModel);

        //set the initial state of the short/long server names in the view menu
        if (NocToolsSettings.getUseShortServerNames() == true)
        {
            shortNamesRadioButtonMenuItem.setSelected(true);
            longNamesRadioButtonMenuItem.setSelected(false);
        }
        else
        {
            shortNamesRadioButtonMenuItem.setSelected(false);
            longNamesRadioButtonMenuItem.setSelected(true);
        }
    }
    @Action
    public void showAboutBox()
    {
        if (aboutBox == null)
        {
            JFrame mainFrame = NocToolsApp.getApplication().getMainFrame();
            aboutBox = new NocToolsAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        NocToolsApp.getApplication().show(aboutBox);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        mainScrollPane = new javax.swing.JScrollPane();
        mainSplitPane = new javax.swing.JSplitPane();
        rightSplitPane = new javax.swing.JSplitPane();
        workModePanel = new javax.swing.JPanel();
        selectionModeRadioButton = new javax.swing.JRadioButton();
        checkedModeRadioButton = new javax.swing.JRadioButton();
        tabsPane = new javax.swing.JTabbedPane();
        mainTabPanel = new javax.swing.JPanel();
        quickAccountsPanel = new javax.swing.JPanel();
        remote1Button = new javax.swing.JButton();
        remote2Button = new javax.swing.JButton();
        remote3Button = new javax.swing.JButton();
        remote4Button = new javax.swing.JButton();
        remote5Button = new javax.swing.JButton();
        remote6Button = new javax.swing.JButton();
        nocButton = new javax.swing.JButton();
        opssupButton = new javax.swing.JButton();
        remote7Button = new javax.swing.JButton();
        remote8Button = new javax.swing.JButton();
        quickScriptsPanel = new javax.swing.JPanel();
        serverScriptsLabel = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        serversScriptsList = new javax.swing.JList();
        customScriptsLabel = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        customScriptsList = new javax.swing.JList();
        executeQuickScriptButton = new javax.swing.JButton();
        serverStartupPanel = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        serverSartupStatusList = new javax.swing.JList();
        clearStartupLogButton = new javax.swing.JButton();
        copyToClipboardButton = new javax.swing.JButton();
        pauseButton = new javax.swing.JButton();
        jScrollPane5 = new javax.swing.JScrollPane();
        serversQueueList = new javax.swing.JList();
        queueLabel = new javax.swing.JLabel();
        tasksLabel = new javax.swing.JLabel();
        removeAllButton = new javax.swing.JButton();
        scriptsTabPanel = new javax.swing.JPanel();
        allScriptsPanel = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        scriptsTable = new javax.swing.JTable();
        editScriptButton = new javax.swing.JButton();
        executeScriptButton = new javax.swing.JButton();
        newScriptButton = new javax.swing.JButton();
        deleteScriptButton = new javax.swing.JButton();
        duplicateScriptButton = new javax.swing.JButton();
        logsPanel = new javax.swing.JPanel();
        extractSettingsPanel = new javax.swing.JPanel();
        byLogNumberRadioButton = new javax.swing.JRadioButton();
        jSeparator1 = new javax.swing.JSeparator();
        byUserIdRadioButton = new javax.swing.JRadioButton();
        userIdTextField = new javax.swing.JTextField();
        endTimeLabel = new javax.swing.JLabel();
        startTimeLabel = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        emailTheLogCheckBox = new javax.swing.JCheckBox();
        emailRecipientsComboBox = new javax.swing.JComboBox();
        addRecipientButton = new javax.swing.JButton();
        removeRecipientButton = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JSeparator();
        getLogButton = new javax.swing.JButton();
        timeFilterRadioButton = new javax.swing.JRadioButton();
        todayRadioButton = new javax.swing.JRadioButton();
        latestLogRadioButton = new javax.swing.JRadioButton();
        startTimeFormattedTextField = new javax.swing.JFormattedTextField();
        endTimeFormattedTextField = new javax.swing.JFormattedTextField();
        logNumberTextField = new javax.swing.JTextField();
        webDbButton = new javax.swing.JButton();
        extractionStatusPanel = new javax.swing.JPanel();
        extractionStatusScrollPane = new javax.swing.JScrollPane();
        extractionStatusList = new javax.swing.JList();
        leftSplitPane = new javax.swing.JSplitPane();
        treeButtonsPanel = new javax.swing.JPanel();
        clearAllButton = new javax.swing.JButton();
        expandOneLevelButton = new javax.swing.JButton();
        expandAllButton = new javax.swing.JButton();
        collapseAllButton = new javax.swing.JButton();
        treeScrollPane = new javax.swing.JScrollPane();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        refreshServersTreeMenuItem = new javax.swing.JMenuItem();
        exportCheckedServersToFileMenuItem = new javax.swing.JMenuItem();
        saveSettingsMenuItem = new javax.swing.JMenuItem();
        fileMenuSeparator = new javax.swing.JSeparator();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        serverNamesMenu = new javax.swing.JMenu();
        shortNamesRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        longNamesRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        showCountriesCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        showFarmsCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        enableLinkMenusCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        rdpRestrictionCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        workModeCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        treeButtonsCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        showRemoteButtonsCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        showServerStartuplCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        toolsMenu = new javax.swing.JMenu();
        settingsMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        documentationMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        workModeButtonGroup = new javax.swing.ButtonGroup();
        serverNamesModeButtonGroup = new javax.swing.ButtonGroup();
        extractLogButtonGroup = new javax.swing.ButtonGroup();
        advancedExtractLogButtonGroup = new javax.swing.ButtonGroup();

        mainPanel.setName("mainPanel"); // NOI18N

        mainScrollPane.setName("mainScrollPane"); // NOI18N

        mainSplitPane.setDividerLocation(372);
        mainSplitPane.setName("mainSplitPane"); // NOI18N
        mainSplitPane.setPreferredSize(new java.awt.Dimension(700, 550));

        rightSplitPane.setDividerLocation(57);
        rightSplitPane.setDividerSize(2);
        rightSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        rightSplitPane.setName("rightSplitPane"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(noctools.NocToolsApp.class).getContext().getResourceMap(NocToolsView.class);
        workModePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, resourceMap.getString("workModePanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, resourceMap.getFont("workModePanel.border.titleFont"))); // NOI18N
        workModePanel.setName("workModePanel"); // NOI18N
        workModePanel.setPreferredSize(new java.awt.Dimension(500, 53));

        selectionModeRadioButton.setText(resourceMap.getString("selectionModeRadioButton.text")); // NOI18N
        selectionModeRadioButton.setName("selectionModeRadioButton"); // NOI18N
        selectionModeRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectionModeRadioButtonActionPerformed(evt);
            }
        });

        checkedModeRadioButton.setText(resourceMap.getString("checkedModeRadioButton.text")); // NOI18N
        checkedModeRadioButton.setName("checkedModeRadioButton"); // NOI18N
        checkedModeRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkedModeRadioButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout workModePanelLayout = new javax.swing.GroupLayout(workModePanel);
        workModePanel.setLayout(workModePanelLayout);
        workModePanelLayout.setHorizontalGroup(
            workModePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(workModePanelLayout.createSequentialGroup()
                .addComponent(selectionModeRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(checkedModeRadioButton)
                .addContainerGap(291, Short.MAX_VALUE))
        );
        workModePanelLayout.setVerticalGroup(
            workModePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(workModePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(selectionModeRadioButton)
                .addComponent(checkedModeRadioButton))
        );

        selectionModeRadioButton.setSelected(true);

        rightSplitPane.setTopComponent(workModePanel);

        tabsPane.setName("tabsPane"); // NOI18N
        tabsPane.setPreferredSize(new java.awt.Dimension(500, 669));

        mainTabPanel.setName("mainTabPanel"); // NOI18N

        quickAccountsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, resourceMap.getString("quickAccountsPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, resourceMap.getFont("quickAccountsPanel.border.titleFont"))); // NOI18N
        quickAccountsPanel.setName("quickAccountsPanel"); // NOI18N

        remote1Button.setText(resourceMap.getString("remote1Button.text")); // NOI18N
        remote1Button.setName("remote1Button"); // NOI18N
        remote1Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remote1ButtonActionPerformed(evt);
            }
        });

        remote2Button.setText(resourceMap.getString("remote2Button.text")); // NOI18N
        remote2Button.setName("remote2Button"); // NOI18N
        remote2Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remote2ButtonActionPerformed(evt);
            }
        });

        remote3Button.setText(resourceMap.getString("remote3Button.text")); // NOI18N
        remote3Button.setName("remote3Button"); // NOI18N
        remote3Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remote3ButtonActionPerformed(evt);
            }
        });

        remote4Button.setText(resourceMap.getString("remote4Button.text")); // NOI18N
        remote4Button.setName("remote4Button"); // NOI18N
        remote4Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remote4ButtonActionPerformed(evt);
            }
        });

        remote5Button.setText(resourceMap.getString("remote5Button.text")); // NOI18N
        remote5Button.setName("remote5Button"); // NOI18N
        remote5Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remote5ButtonActionPerformed(evt);
            }
        });

        remote6Button.setText(resourceMap.getString("remote6Button.text")); // NOI18N
        remote6Button.setName("remote6Button"); // NOI18N
        remote6Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remote6ButtonActionPerformed(evt);
            }
        });

        nocButton.setText(resourceMap.getString("nocButton.text")); // NOI18N
        nocButton.setName("nocButton"); // NOI18N
        nocButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nocButtonActionPerformed(evt);
            }
        });

        opssupButton.setText(resourceMap.getString("opssupButton.text")); // NOI18N
        opssupButton.setName("opssupButton"); // NOI18N
        opssupButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                opssupButtonActionPerformed(evt);
            }
        });

        remote7Button.setText(resourceMap.getString("remote7Button.text")); // NOI18N
        remote7Button.setName("remote7Button"); // NOI18N
        remote7Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remote7ButtonActionPerformed(evt);
            }
        });

        remote8Button.setText(resourceMap.getString("remote8Button.text")); // NOI18N
        remote8Button.setName("remote8Button"); // NOI18N
        remote8Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remote8ButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout quickAccountsPanelLayout = new javax.swing.GroupLayout(quickAccountsPanel);
        quickAccountsPanel.setLayout(quickAccountsPanelLayout);
        quickAccountsPanelLayout.setHorizontalGroup(
            quickAccountsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(quickAccountsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(quickAccountsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(remote1Button)
                    .addComponent(remote2Button)
                    .addComponent(remote3Button)
                    .addComponent(remote4Button)
                    .addComponent(remote5Button)
                    .addComponent(remote6Button)
                    .addComponent(remote7Button)
                    .addComponent(remote8Button)
                    .addComponent(nocButton)
                    .addComponent(opssupButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        quickAccountsPanelLayout.setVerticalGroup(
            quickAccountsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(quickAccountsPanelLayout.createSequentialGroup()
                .addComponent(remote1Button)
                .addGap(11, 11, 11)
                .addComponent(remote2Button)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(remote3Button)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(remote4Button)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(remote5Button)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(remote6Button)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(remote7Button)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(remote8Button)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(nocButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(opssupButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        quickScriptsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, resourceMap.getString("quickScriptsPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, resourceMap.getFont("quickScriptsPanel.border.titleFont"))); // NOI18N
        quickScriptsPanel.setName("quickScriptsPanel"); // NOI18N

        serverScriptsLabel.setFont(resourceMap.getFont("serverScriptsLabel.font")); // NOI18N
        serverScriptsLabel.setText(resourceMap.getString("serverScriptsLabel.text")); // NOI18N
        serverScriptsLabel.setName("serverScriptsLabel"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        serversScriptsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        serversScriptsList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        serversScriptsList.setName("serversScriptsList"); // NOI18N
        serversScriptsList.setSelectedIndex(0);
        serversScriptsList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                serversScriptsListMousePressed(evt);
            }
        });
        jScrollPane1.setViewportView(serversScriptsList);

        customScriptsLabel.setFont(resourceMap.getFont("customScriptsLabel.font")); // NOI18N
        customScriptsLabel.setText(resourceMap.getString("customScriptsLabel.text")); // NOI18N
        customScriptsLabel.setName("customScriptsLabel"); // NOI18N

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        customScriptsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customScriptsList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        customScriptsList.setName("customScriptsList"); // NOI18N
        customScriptsList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                customScriptsListMouseClicked(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                customScriptsListMousePressed(evt);
            }
        });
        jScrollPane2.setViewportView(customScriptsList);

        executeQuickScriptButton.setText(resourceMap.getString("executeQuickScriptButton.text")); // NOI18N
        executeQuickScriptButton.setName("executeQuickScriptButton"); // NOI18N
        executeQuickScriptButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                executeQuickScriptButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout quickScriptsPanelLayout = new javax.swing.GroupLayout(quickScriptsPanel);
        quickScriptsPanel.setLayout(quickScriptsPanelLayout);
        quickScriptsPanelLayout.setHorizontalGroup(
            quickScriptsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, quickScriptsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(quickScriptsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 319, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 319, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, quickScriptsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(serverScriptsLabel)
                        .addComponent(executeQuickScriptButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(customScriptsLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        quickScriptsPanelLayout.setVerticalGroup(
            quickScriptsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(quickScriptsPanelLayout.createSequentialGroup()
                .addComponent(serverScriptsLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 178, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(customScriptsLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 178, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(executeQuickScriptButton)
                .addContainerGap())
        );

        serverStartupPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createTitledBorder(""), resourceMap.getString("serverStartupPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, resourceMap.getFont("serverStartupPanel.border.titleFont"))); // NOI18N
        serverStartupPanel.setName("serverStartupPanel"); // NOI18N

        jScrollPane4.setName("jScrollPane4"); // NOI18N

        serverSartupStatusList.setName("serverSartupStatusList"); // NOI18N
        jScrollPane4.setViewportView(serverSartupStatusList);

        clearStartupLogButton.setText(resourceMap.getString("clearStartupLogButton.text")); // NOI18N
        clearStartupLogButton.setName("clearStartupLogButton"); // NOI18N
        clearStartupLogButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearStartupLogButtonActionPerformed(evt);
            }
        });

        copyToClipboardButton.setText(resourceMap.getString("copyToClipboardButton.text")); // NOI18N
        copyToClipboardButton.setName("copyToClipboardButton"); // NOI18N
        copyToClipboardButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyToClipboardButtonActionPerformed(evt);
            }
        });

        pauseButton.setText(resourceMap.getString("pauseButton.text")); // NOI18N
        pauseButton.setName("pauseButton"); // NOI18N
        pauseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pauseButtonActionPerformed(evt);
            }
        });

        jScrollPane5.setName("jScrollPane5"); // NOI18N

        serversQueueList.setName("serversQueueList"); // NOI18N
        jScrollPane5.setViewportView(serversQueueList);

        queueLabel.setFont(resourceMap.getFont("queueLabel.font")); // NOI18N
        queueLabel.setText(resourceMap.getString("queueLabel.text")); // NOI18N
        queueLabel.setName("queueLabel"); // NOI18N

        tasksLabel.setFont(resourceMap.getFont("tasksLabel.font")); // NOI18N
        tasksLabel.setText(resourceMap.getString("tasksLabel.text")); // NOI18N
        tasksLabel.setName("tasksLabel"); // NOI18N

        removeAllButton.setText(resourceMap.getString("removeAllButton.text")); // NOI18N
        removeAllButton.setName("removeAllButton"); // NOI18N
        removeAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeAllButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout serverStartupPanelLayout = new javax.swing.GroupLayout(serverStartupPanel);
        serverStartupPanel.setLayout(serverStartupPanelLayout);
        serverStartupPanelLayout.setHorizontalGroup(
            serverStartupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, serverStartupPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(serverStartupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(queueLabel)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(removeAllButton))
                .addGap(6, 6, 6)
                .addGroup(serverStartupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, serverStartupPanelLayout.createSequentialGroup()
                        .addComponent(pauseButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 70, Short.MAX_VALUE)
                        .addComponent(clearStartupLogButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(copyToClipboardButton))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 315, Short.MAX_VALUE)
                    .addComponent(tasksLabel))
                .addContainerGap())
        );
        serverStartupPanelLayout.setVerticalGroup(
            serverStartupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, serverStartupPanelLayout.createSequentialGroup()
                .addGroup(serverStartupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(queueLabel)
                    .addComponent(tasksLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(serverStartupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 23, Short.MAX_VALUE)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 23, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(serverStartupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(copyToClipboardButton)
                    .addComponent(clearStartupLogButton)
                    .addComponent(pauseButton)
                    .addComponent(removeAllButton))
                .addContainerGap())
        );

        javax.swing.GroupLayout mainTabPanelLayout = new javax.swing.GroupLayout(mainTabPanel);
        mainTabPanel.setLayout(mainTabPanelLayout);
        mainTabPanelLayout.setHorizontalGroup(
            mainTabPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainTabPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainTabPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(serverStartupPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(mainTabPanelLayout.createSequentialGroup()
                        .addComponent(quickAccountsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(quickScriptsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        mainTabPanelLayout.setVerticalGroup(
            mainTabPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainTabPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainTabPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(quickAccountsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(quickScriptsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(serverStartupPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        tabsPane.addTab(resourceMap.getString("mainTabPanel.TabConstraints.tabTitle"), mainTabPanel); // NOI18N

        scriptsTabPanel.setName("scriptsTabPanel"); // NOI18N
        scriptsTabPanel.setPreferredSize(new java.awt.Dimension(496, 500));

        allScriptsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, resourceMap.getString("allScriptsPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, resourceMap.getFont("allScriptsPanel.border.titleFont"))); // NOI18N
        allScriptsPanel.setName("allScriptsPanel"); // NOI18N
        allScriptsPanel.setPreferredSize(new java.awt.Dimension(476, 450));

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        scriptsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Description", "Script File", "Type"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        scriptsTable.setName("scriptsTable"); // NOI18N
        jScrollPane3.setViewportView(scriptsTable);
        scriptsTable.getColumnModel().getColumn(0).setHeaderValue(resourceMap.getString("scriptsTable.columnModel.title0")); // NOI18N
        scriptsTable.getColumnModel().getColumn(1).setHeaderValue(resourceMap.getString("scriptsTable.columnModel.title1")); // NOI18N
        scriptsTable.getColumnModel().getColumn(2).setHeaderValue(resourceMap.getString("scriptsTable.columnModel.title2")); // NOI18N
        scriptsTable.setAutoCreateRowSorter(true);
        scriptsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        editScriptButton.setText(resourceMap.getString("editScriptButton.text")); // NOI18N
        editScriptButton.setName("editScriptButton"); // NOI18N
        editScriptButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editScriptButtonActionPerformed(evt);
            }
        });

        executeScriptButton.setText(resourceMap.getString("executeScriptButton.text")); // NOI18N
        executeScriptButton.setName("executeScriptButton"); // NOI18N
        executeScriptButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                executeScriptButtonActionPerformed(evt);
            }
        });

        newScriptButton.setText(resourceMap.getString("newScriptButton.text")); // NOI18N
        newScriptButton.setName("newScriptButton"); // NOI18N
        newScriptButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newScriptButtonActionPerformed(evt);
            }
        });

        deleteScriptButton.setText(resourceMap.getString("deleteScriptButton.text")); // NOI18N
        deleteScriptButton.setName("deleteScriptButton"); // NOI18N
        deleteScriptButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteScriptButtonActionPerformed(evt);
            }
        });

        duplicateScriptButton.setText(resourceMap.getString("duplicateScriptButton.text")); // NOI18N
        duplicateScriptButton.setName("duplicateScriptButton"); // NOI18N
        duplicateScriptButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                duplicateScriptButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout allScriptsPanelLayout = new javax.swing.GroupLayout(allScriptsPanel);
        allScriptsPanel.setLayout(allScriptsPanelLayout);
        allScriptsPanelLayout.setHorizontalGroup(
            allScriptsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(allScriptsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(allScriptsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE)
                    .addGroup(allScriptsPanelLayout.createSequentialGroup()
                        .addComponent(executeScriptButton)
                        .addGap(18, 18, 18)
                        .addComponent(editScriptButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(newScriptButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(duplicateScriptButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteScriptButton)))
                .addContainerGap())
        );
        allScriptsPanelLayout.setVerticalGroup(
            allScriptsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, allScriptsPanelLayout.createSequentialGroup()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 534, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(allScriptsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(executeScriptButton)
                    .addComponent(editScriptButton)
                    .addComponent(newScriptButton)
                    .addComponent(duplicateScriptButton)
                    .addComponent(deleteScriptButton))
                .addContainerGap())
        );

        javax.swing.GroupLayout scriptsTabPanelLayout = new javax.swing.GroupLayout(scriptsTabPanel);
        scriptsTabPanel.setLayout(scriptsTabPanelLayout);
        scriptsTabPanelLayout.setHorizontalGroup(
            scriptsTabPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(scriptsTabPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(allScriptsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        scriptsTabPanelLayout.setVerticalGroup(
            scriptsTabPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(scriptsTabPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(allScriptsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 604, Short.MAX_VALUE)
                .addContainerGap())
        );

        tabsPane.addTab(resourceMap.getString("scriptsTabPanel.TabConstraints.tabTitle"), scriptsTabPanel); // NOI18N

        logsPanel.setName("logsPanel"); // NOI18N
        logsPanel.setPreferredSize(new java.awt.Dimension(500, 550));

        extractSettingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, resourceMap.getString("extractSettingsPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, resourceMap.getFont("extractSettingsPanel.border.titleFont"))); // NOI18N
        extractSettingsPanel.setName("extractSettingsPanel"); // NOI18N

        byLogNumberRadioButton.setText(resourceMap.getString("byLogNumberRadioButton.text")); // NOI18N
        byLogNumberRadioButton.setName("byLogNumberRadioButton"); // NOI18N
        byLogNumberRadioButton.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                byLogNumberRadioButtonStateChanged(evt);
            }
        });

        jSeparator1.setName("jSeparator1"); // NOI18N

        byUserIdRadioButton.setText(resourceMap.getString("byUserIdRadioButton.text")); // NOI18N
        byUserIdRadioButton.setEnabled(false);
        byUserIdRadioButton.setName("byUserIdRadioButton"); // NOI18N
        byUserIdRadioButton.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                byUserIdRadioButtonStateChanged(evt);
            }
        });

        userIdTextField.setText(resourceMap.getString("userIdTextField.text")); // NOI18N
        userIdTextField.setEnabled(false);
        userIdTextField.setName("userIdTextField"); // NOI18N

        endTimeLabel.setText(resourceMap.getString("endTimeLabel.text")); // NOI18N
        endTimeLabel.setName("endTimeLabel"); // NOI18N

        startTimeLabel.setText(resourceMap.getString("startTimeLabel.text")); // NOI18N
        startTimeLabel.setName("startTimeLabel"); // NOI18N

        jSeparator2.setName("jSeparator2"); // NOI18N

        emailTheLogCheckBox.setText(resourceMap.getString("emailTheLogCheckBox.text")); // NOI18N
        emailTheLogCheckBox.setName("emailTheLogCheckBox"); // NOI18N

        emailRecipientsComboBox.setName("emailRecipientsComboBox"); // NOI18N

        addRecipientButton.setText(resourceMap.getString("addRecipientButton.text")); // NOI18N
        addRecipientButton.setName("addRecipientButton"); // NOI18N
        addRecipientButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addRecipientButtonActionPerformed(evt);
            }
        });

        removeRecipientButton.setText(resourceMap.getString("removeRecipientButton.text")); // NOI18N
        removeRecipientButton.setName("removeRecipientButton"); // NOI18N
        removeRecipientButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeRecipientButtonActionPerformed(evt);
            }
        });

        jSeparator3.setName("jSeparator3"); // NOI18N

        getLogButton.setText(resourceMap.getString("getLogButton.text")); // NOI18N
        getLogButton.setName("getLogButton"); // NOI18N
        getLogButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                getLogButtonActionPerformed(evt);
            }
        });

        timeFilterRadioButton.setText(resourceMap.getString("timeFilterRadioButton.text")); // NOI18N
        timeFilterRadioButton.setEnabled(false);
        timeFilterRadioButton.setName("timeFilterRadioButton"); // NOI18N

        todayRadioButton.setText(resourceMap.getString("todayRadioButton.text")); // NOI18N
        todayRadioButton.setEnabled(false);
        todayRadioButton.setName("todayRadioButton"); // NOI18N

        latestLogRadioButton.setText(resourceMap.getString("latestLogRadioButton.text")); // NOI18N
        latestLogRadioButton.setEnabled(false);
        latestLogRadioButton.setName("latestLogRadioButton"); // NOI18N

        startTimeFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.DateFormatter(java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT))));
        startTimeFormattedTextField.setText(resourceMap.getString("startTimeFormattedTextField.text")); // NOI18N
        startTimeFormattedTextField.setEnabled(false);
        startTimeFormattedTextField.setName("startTimeFormattedTextField"); // NOI18N

        endTimeFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.DateFormatter(java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT))));
        endTimeFormattedTextField.setText(resourceMap.getString("endTimeFormattedTextField.text")); // NOI18N
        endTimeFormattedTextField.setEnabled(false);
        endTimeFormattedTextField.setName("endTimeFormattedTextField"); // NOI18N

        logNumberTextField.setText(resourceMap.getString("logNumberTextField.text")); // NOI18N
        logNumberTextField.setToolTipText(resourceMap.getString("logNumberTextField.toolTipText")); // NOI18N
        logNumberTextField.setName("logNumberTextField"); // NOI18N

        webDbButton.setText(resourceMap.getString("webDbButton.text")); // NOI18N
        webDbButton.setName("webDbButton"); // NOI18N
        webDbButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                webDbButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout extractSettingsPanelLayout = new javax.swing.GroupLayout(extractSettingsPanel);
        extractSettingsPanel.setLayout(extractSettingsPanelLayout);
        extractSettingsPanelLayout.setHorizontalGroup(
            extractSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(extractSettingsPanelLayout.createSequentialGroup()
                .addGroup(extractSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(extractSettingsPanelLayout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addGroup(extractSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(latestLogRadioButton)
                            .addComponent(todayRadioButton)
                            .addComponent(timeFilterRadioButton)
                            .addGroup(extractSettingsPanelLayout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addGroup(extractSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(startTimeLabel)
                                    .addComponent(startTimeFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(extractSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(endTimeFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(endTimeLabel)))))
                    .addGroup(extractSettingsPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(extractSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(extractSettingsPanelLayout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addComponent(userIdTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(byUserIdRadioButton)))
                    .addGroup(extractSettingsPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(extractSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(extractSettingsPanelLayout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addComponent(logNumberTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(webDbButton))
                            .addComponent(byLogNumberRadioButton))))
                .addContainerGap(283, Short.MAX_VALUE))
            .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 460, Short.MAX_VALUE)
            .addGroup(extractSettingsPanelLayout.createSequentialGroup()
                .addComponent(jSeparator2, javax.swing.GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(extractSettingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(extractSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(emailTheLogCheckBox)
                    .addGroup(extractSettingsPanelLayout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addGroup(extractSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(extractSettingsPanelLayout.createSequentialGroup()
                                .addComponent(addRecipientButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(removeRecipientButton))
                            .addComponent(emailRecipientsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 193, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(240, Short.MAX_VALUE))
            .addGroup(extractSettingsPanelLayout.createSequentialGroup()
                .addComponent(jSeparator3, javax.swing.GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(extractSettingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(getLogButton)
                .addContainerGap(383, Short.MAX_VALUE))
        );
        extractSettingsPanelLayout.setVerticalGroup(
            extractSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(extractSettingsPanelLayout.createSequentialGroup()
                .addComponent(byLogNumberRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(extractSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(logNumberTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(webDbButton))
                .addGap(11, 11, 11)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(byUserIdRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(userIdTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(latestLogRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(todayRadioButton)
                .addGap(3, 3, 3)
                .addComponent(timeFilterRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(extractSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(startTimeLabel)
                    .addComponent(endTimeLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(extractSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(startTimeFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(endTimeFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 8, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(emailTheLogCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(emailRecipientsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(extractSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addRecipientButton)
                    .addComponent(removeRecipientButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(getLogButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        extractionStatusPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, resourceMap.getString("extractionStatusPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, resourceMap.getFont("extractionStatusPanel.border.titleFont"))); // NOI18N
        extractionStatusPanel.setName("extractionStatusPanel"); // NOI18N

        extractionStatusScrollPane.setName("extractionStatusScrollPane"); // NOI18N

        extractionStatusList.setName("extractionStatusList"); // NOI18N
        extractionStatusScrollPane.setViewportView(extractionStatusList);

        javax.swing.GroupLayout extractionStatusPanelLayout = new javax.swing.GroupLayout(extractionStatusPanel);
        extractionStatusPanel.setLayout(extractionStatusPanelLayout);
        extractionStatusPanelLayout.setHorizontalGroup(
            extractionStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(extractionStatusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(extractionStatusScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE)
                .addContainerGap())
        );
        extractionStatusPanelLayout.setVerticalGroup(
            extractionStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(extractionStatusPanelLayout.createSequentialGroup()
                .addComponent(extractionStatusScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 121, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout logsPanelLayout = new javax.swing.GroupLayout(logsPanel);
        logsPanel.setLayout(logsPanelLayout);
        logsPanelLayout.setHorizontalGroup(
            logsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, logsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(logsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(extractionStatusPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(extractSettingsPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        logsPanelLayout.setVerticalGroup(
            logsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(logsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(extractSettingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(extractionStatusPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        tabsPane.addTab(resourceMap.getString("logsPanel.TabConstraints.tabTitle"), logsPanel); // NOI18N

        rightSplitPane.setRightComponent(tabsPane);

        mainSplitPane.setRightComponent(rightSplitPane);

        leftSplitPane.setDividerLocation(30);
        leftSplitPane.setDividerSize(2);
        leftSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        leftSplitPane.setName("leftSplitPane"); // NOI18N

        treeButtonsPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        treeButtonsPanel.setName("treeButtonsPanel"); // NOI18N

        clearAllButton.setText(resourceMap.getString("clearAllButton.text")); // NOI18N
        clearAllButton.setFocusable(false);
        clearAllButton.setName("clearAllButton"); // NOI18N
        clearAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearAllButtonActionPerformed(evt);
            }
        });

        expandOneLevelButton.setText(resourceMap.getString("expandOneLevelButton.text")); // NOI18N
        expandOneLevelButton.setFocusable(false);
        expandOneLevelButton.setName("expandOneLevelButton"); // NOI18N
        expandOneLevelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                expandOneLevelButtonActionPerformed(evt);
            }
        });

        expandAllButton.setText(resourceMap.getString("expandAllButton.text")); // NOI18N
        expandAllButton.setFocusable(false);
        expandAllButton.setName("expandAllButton"); // NOI18N
        expandAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                expandAllButtonActionPerformed(evt);
            }
        });

        collapseAllButton.setText(resourceMap.getString("collapseAllButton.text")); // NOI18N
        collapseAllButton.setFocusable(false);
        collapseAllButton.setName("collapseAllButton"); // NOI18N
        collapseAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                collapseAllButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout treeButtonsPanelLayout = new javax.swing.GroupLayout(treeButtonsPanel);
        treeButtonsPanel.setLayout(treeButtonsPanelLayout);
        treeButtonsPanelLayout.setHorizontalGroup(
            treeButtonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(treeButtonsPanelLayout.createSequentialGroup()
                .addComponent(clearAllButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(expandOneLevelButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(expandAllButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(collapseAllButton))
        );
        treeButtonsPanelLayout.setVerticalGroup(
            treeButtonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(treeButtonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(clearAllButton)
                .addComponent(expandOneLevelButton)
                .addComponent(expandAllButton)
                .addComponent(collapseAllButton))
        );

        leftSplitPane.setTopComponent(treeButtonsPanel);

        treeScrollPane.setName("treeScrollPane"); // NOI18N
        leftSplitPane.setRightComponent(treeScrollPane);

        mainSplitPane.setLeftComponent(leftSplitPane);

        mainScrollPane.setViewportView(mainSplitPane);

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 883, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 718, Short.MAX_VALUE)
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        refreshServersTreeMenuItem.setText(resourceMap.getString("refreshServersTreeMenuItem.text")); // NOI18N
        refreshServersTreeMenuItem.setName("refreshServersTreeMenuItem"); // NOI18N
        refreshServersTreeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshServersTreeMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(refreshServersTreeMenuItem);

        exportCheckedServersToFileMenuItem.setText(resourceMap.getString("exportCheckedServersToFileMenuItem.text")); // NOI18N
        exportCheckedServersToFileMenuItem.setName("exportCheckedServersToFileMenuItem"); // NOI18N
        exportCheckedServersToFileMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportCheckedServersToFileMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exportCheckedServersToFileMenuItem);

        saveSettingsMenuItem.setText(resourceMap.getString("saveSettingsMenuItem.text")); // NOI18N
        saveSettingsMenuItem.setName("saveSettingsMenuItem"); // NOI18N
        saveSettingsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveSettingsMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveSettingsMenuItem);

        fileMenuSeparator.setName("fileMenuSeparator"); // NOI18N
        fileMenu.add(fileMenuSeparator);

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(noctools.NocToolsApp.class).getContext().getActionMap(NocToolsView.class, this);
        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setText(resourceMap.getString("editMenu.text")); // NOI18N
        editMenu.setName("editMenu"); // NOI18N

        jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem1.setText(resourceMap.getString("jMenuItem1.text")); // NOI18N
        jMenuItem1.setName("jMenuItem1"); // NOI18N
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        editMenu.add(jMenuItem1);

        jMenuItem2.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem2.setText(resourceMap.getString("jMenuItem2.text")); // NOI18N
        jMenuItem2.setName("jMenuItem2"); // NOI18N
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        editMenu.add(jMenuItem2);

        menuBar.add(editMenu);

        viewMenu.setText(resourceMap.getString("viewMenu.text")); // NOI18N
        viewMenu.setName("viewMenu"); // NOI18N

        serverNamesMenu.setText(resourceMap.getString("serverNamesMenu.text")); // NOI18N
        serverNamesMenu.setName("serverNamesMenu"); // NOI18N

        shortNamesRadioButtonMenuItem.setSelected(true);
        shortNamesRadioButtonMenuItem.setText(resourceMap.getString("shortNamesRadioButtonMenuItem.text")); // NOI18N
        shortNamesRadioButtonMenuItem.setName("shortNamesRadioButtonMenuItem"); // NOI18N
        shortNamesRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                shortNamesRadioButtonMenuItemActionPerformed(evt);
            }
        });
        serverNamesMenu.add(shortNamesRadioButtonMenuItem);

        longNamesRadioButtonMenuItem.setSelected(true);
        longNamesRadioButtonMenuItem.setText(resourceMap.getString("longNamesRadioButtonMenuItem.text")); // NOI18N
        longNamesRadioButtonMenuItem.setName("longNamesRadioButtonMenuItem"); // NOI18N
        longNamesRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                longNamesRadioButtonMenuItemActionPerformed(evt);
            }
        });
        serverNamesMenu.add(longNamesRadioButtonMenuItem);

        viewMenu.add(serverNamesMenu);

        showCountriesCheckBoxMenuItem.setSelected(true);
        showCountriesCheckBoxMenuItem.setText(resourceMap.getString("showCountriesCheckBoxMenuItem.text")); // NOI18N
        showCountriesCheckBoxMenuItem.setName("showCountriesCheckBoxMenuItem"); // NOI18N
        showCountriesCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showCountriesCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(showCountriesCheckBoxMenuItem);

        showFarmsCheckBoxMenuItem.setSelected(true);
        showFarmsCheckBoxMenuItem.setText(resourceMap.getString("showFarmsCheckBoxMenuItem.text")); // NOI18N
        showFarmsCheckBoxMenuItem.setName("showFarmsCheckBoxMenuItem"); // NOI18N
        showFarmsCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showFarmsCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(showFarmsCheckBoxMenuItem);

        enableLinkMenusCheckBoxMenuItem.setSelected(true);
        enableLinkMenusCheckBoxMenuItem.setText(resourceMap.getString("enableLinkMenusCheckBoxMenuItem.text")); // NOI18N
        enableLinkMenusCheckBoxMenuItem.setName("enableLinkMenusCheckBoxMenuItem"); // NOI18N
        enableLinkMenusCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enableLinkMenusCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(enableLinkMenusCheckBoxMenuItem);

        rdpRestrictionCheckBoxMenuItem.setSelected(true);
        rdpRestrictionCheckBoxMenuItem.setText(resourceMap.getString("rdpRestrictionCheckBoxMenuItem.text")); // NOI18N
        rdpRestrictionCheckBoxMenuItem.setToolTipText(resourceMap.getString("rdpRestrictionCheckBoxMenuItem.toolTipText")); // NOI18N
        rdpRestrictionCheckBoxMenuItem.setName("rdpRestrictionCheckBoxMenuItem"); // NOI18N
        rdpRestrictionCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rdpRestrictionCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(rdpRestrictionCheckBoxMenuItem);

        workModeCheckBoxMenuItem.setSelected(true);
        workModeCheckBoxMenuItem.setText(resourceMap.getString("workModeCheckBoxMenuItem.text")); // NOI18N
        workModeCheckBoxMenuItem.setName("workModeCheckBoxMenuItem"); // NOI18N
        workModeCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                workModeCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(workModeCheckBoxMenuItem);

        treeButtonsCheckBoxMenuItem.setSelected(true);
        treeButtonsCheckBoxMenuItem.setText(resourceMap.getString("treeButtonsCheckBoxMenuItem.text")); // NOI18N
        treeButtonsCheckBoxMenuItem.setName("treeButtonsCheckBoxMenuItem"); // NOI18N
        treeButtonsCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                treeButtonsCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(treeButtonsCheckBoxMenuItem);

        showRemoteButtonsCheckBoxMenuItem.setSelected(true);
        showRemoteButtonsCheckBoxMenuItem.setText(resourceMap.getString("showRemoteButtonsCheckBoxMenuItem.text")); // NOI18N
        showRemoteButtonsCheckBoxMenuItem.setName("showRemoteButtonsCheckBoxMenuItem"); // NOI18N
        showRemoteButtonsCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showRemoteButtonsCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(showRemoteButtonsCheckBoxMenuItem);

        showServerStartuplCheckBoxMenuItem.setSelected(true);
        showServerStartuplCheckBoxMenuItem.setText(resourceMap.getString("showServerStartuplCheckBoxMenuItem.text")); // NOI18N
        showServerStartuplCheckBoxMenuItem.setName("showServerStartuplCheckBoxMenuItem"); // NOI18N
        showServerStartuplCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showServerStartuplCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(showServerStartuplCheckBoxMenuItem);

        menuBar.add(viewMenu);

        toolsMenu.setText(resourceMap.getString("toolsMenu.text")); // NOI18N
        toolsMenu.setName("toolsMenu"); // NOI18N

        settingsMenuItem.setText(resourceMap.getString("settingsMenuItem.text")); // NOI18N
        settingsMenuItem.setName("settingsMenuItem"); // NOI18N
        settingsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                settingsMenuItemActionPerformed(evt);
            }
        });
        toolsMenu.add(settingsMenuItem);

        menuBar.add(toolsMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        documentationMenuItem.setText(resourceMap.getString("documentationMenuItem.text")); // NOI18N
        documentationMenuItem.setName("documentationMenuItem"); // NOI18N
        documentationMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                documentationMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(documentationMenuItem);

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N
        statusPanel.setPreferredSize(new java.awt.Dimension(700, 25));

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 883, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 713, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        workModeButtonGroup.add(selectionModeRadioButton);
        workModeButtonGroup.add(checkedModeRadioButton);

        serverNamesModeButtonGroup.add(shortNamesRadioButtonMenuItem);
        serverNamesModeButtonGroup.add(longNamesRadioButtonMenuItem);

        extractLogButtonGroup.add(byLogNumberRadioButton);
        extractLogButtonGroup.add(byUserIdRadioButton);

        advancedExtractLogButtonGroup.add(latestLogRadioButton);
        advancedExtractLogButtonGroup.add(todayRadioButton);
        advancedExtractLogButtonGroup.add(timeFilterRadioButton);

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    ///////////////////////////////// END OF GENERATED NETBEANS CODE ////////////////////////////////////////
    private void selectionModeRadioButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_selectionModeRadioButtonActionPerformed
    {//GEN-HEADEREND:event_selectionModeRadioButtonActionPerformed
        // selection mode has been chosen
        if (selectionModeRadioButton.isSelected())
            ServersTreeManager.setSelectionMode();
        else
            ServersTreeManager.setCheckedMode();

        updateStatusMessage();
        updateRemoteButtons();

        if (ServersTreeManager.areThereCheckedServers())
            serversTree.paintAll(serversTree.getGraphics());
        serversTree.grabFocus();
    }//GEN-LAST:event_selectionModeRadioButtonActionPerformed

    private void checkedModeRadioButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_checkedModeRadioButtonActionPerformed
    {//GEN-HEADEREND:event_checkedModeRadioButtonActionPerformed
        // checked mode have been chosen
        if (checkedModeRadioButton.isSelected())
            ServersTreeManager.setCheckedMode();
        else
            ServersTreeManager.setSelectionMode();

        updateStatusMessage();
        updateRemoteButtons();

        if (ServersTreeManager.areThereCheckedServers())
            serversTree.paintAll(serversTree.getGraphics());
        serversTree.grabFocus();
    }//GEN-LAST:event_checkedModeRadioButtonActionPerformed

    /**
     * Expands the tree only one level
     */
    private void expandOneLevelOnly()
    {
        serversTree.expandAll();
        int row = serversTree.getRowCount() - 1;
        while (row >= 0)
        {
            serversTree.collapseRow(row);
            row--;
        }

        serversTree.expandRow(0);
        row = serversTree.getRowCount() - 1;
        while (row >= 0)
        {
            serversTree.expandRow(row);
            row--;
        }
        updateStatusMessage();
        updateRemoteButtons();

        serversTree.grabFocus();
    }

    /**
     * Collapses the servers tree
     */
    private void collapseAll()
    {
        int row = serversTree.getRowCount() - 1;
        while (row >= 0)
        {
            serversTree.collapseRow(row);
            row--;
        }
        serversTree.expandRow(0);
        updateStatusMessage();
        updateRemoteButtons();

        serversTree.grabFocus();
    }

    private void showCountriesCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_showCountriesCheckBoxMenuItemActionPerformed
    {//GEN-HEADEREND:event_showCountriesCheckBoxMenuItemActionPerformed
        try
        {
            if (showCountriesCheckBoxMenuItem.getState() && !NocToolsSettings.getShowCountries())
            {
                NocToolsSettings.setShowCountries(true);
                reloadTree();
            }
            else if (!showCountriesCheckBoxMenuItem.getState() && NocToolsSettings.getShowCountries())
            {
                NocToolsSettings.setShowCountries(false);
                reloadTree();
            }
        }
        catch (Exception e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }
        updateStatusMessage();
        updateRemoteButtons();

        serversTree.grabFocus();
    }//GEN-LAST:event_showCountriesCheckBoxMenuItemActionPerformed

    private void showFarmsCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_showFarmsCheckBoxMenuItemActionPerformed
    {//GEN-HEADEREND:event_showFarmsCheckBoxMenuItemActionPerformed
        try
        {
            if (showFarmsCheckBoxMenuItem.getState() && !NocToolsSettings.getShowFarms())
            {
                NocToolsSettings.setShowFarms(true);
                reloadTree();
            }
            else if (!showFarmsCheckBoxMenuItem.getState() && NocToolsSettings.getShowFarms())
            {
                NocToolsSettings.setShowFarms(false);
                reloadTree();
            }
        }
        catch (Exception e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }
        updateStatusMessage();
        updateRemoteButtons();

        serversTree.grabFocus();
    }//GEN-LAST:event_showFarmsCheckBoxMenuItemActionPerformed

    private void refreshServersTreeMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_refreshServersTreeMenuItemActionPerformed
    {//GEN-HEADEREND:event_refreshServersTreeMenuItemActionPerformed
        try
        {
            NocToolsSettings.readConfigXmlFiles();
            NocToolsSettings.readLinksSettingsXmlFile();
            reloadTree();
            ServersTreeManager.createServersList((ComparableMutableTreeNode)serversTree.getModel().getRoot());
        }
        catch (Exception e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }
    }//GEN-LAST:event_refreshServersTreeMenuItemActionPerformed

    private void exportCheckedServersToFileMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_exportCheckedServersToFileMenuItemActionPerformed
    {//GEN-HEADEREND:event_exportCheckedServersToFileMenuItemActionPerformed
        boolean result = false;
        if (ServersTreeManager.areThereCheckedServers())
            result = noctools.endpoint.ServersTreeManager.exportCheckedServersList(this.getComponent());
        else
            JOptionPane.showMessageDialog(this.getComponent(), "Please check a few servers");

        // only if the servers were exported - clear the checked servers
        if (result == true)
        {
            serversTree.clearChecking();
            if (!selectionModeRadioButton.isSelected())
                selectionModeRadioButton.doClick();
        }

        updateStatusMessage();
        updateRemoteButtons();

        serversTree.grabFocus();
    }//GEN-LAST:event_exportCheckedServersToFileMenuItemActionPerformed

    private void byUserIdRadioButtonStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_byUserIdRadioButtonStateChanged
    {//GEN-HEADEREND:event_byUserIdRadioButtonStateChanged
        if (byUserIdRadioButton.isSelected())
        {
            userIdTextField.setEditable(true);
            latestLogRadioButton.setEnabled(true);
            todayRadioButton.setEnabled(true);
            timeFilterRadioButton.setEnabled(true);
            startTimeFormattedTextField.setEditable(true);
            endTimeFormattedTextField.setEditable(true);
            logNumberTextField.setEnabled(false);
        }
}//GEN-LAST:event_byUserIdRadioButtonStateChanged

    private void byLogNumberRadioButtonStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_byLogNumberRadioButtonStateChanged
    {//GEN-HEADEREND:event_byLogNumberRadioButtonStateChanged
        if (byLogNumberRadioButton.isSelected())
        {
            advancedExtractLogButtonGroup.clearSelection();
            userIdTextField.setEditable(false);
            latestLogRadioButton.setEnabled(false);
            todayRadioButton.setEnabled(false);
            timeFilterRadioButton.setEnabled(false);
            startTimeFormattedTextField.setEditable(false);
            endTimeFormattedTextField.setEditable(false);
            logNumberTextField.setEnabled(true);
        }
}//GEN-LAST:event_byLogNumberRadioButtonStateChanged

    private void customScriptsListMousePressed(java.awt.event.MouseEvent evt)//GEN-FIRST:event_customScriptsListMousePressed
    {//GEN-HEADEREND:event_customScriptsListMousePressed
        serversScriptsList.clearSelection();
        serversTree.grabFocus();
}//GEN-LAST:event_customScriptsListMousePressed

    private void serversScriptsListMousePressed(java.awt.event.MouseEvent evt)//GEN-FIRST:event_serversScriptsListMousePressed
    {//GEN-HEADEREND:event_serversScriptsListMousePressed
        customScriptsList.clearSelection();
        serversTree.grabFocus();
}//GEN-LAST:event_serversScriptsListMousePressed

    private void documentationMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_documentationMenuItemActionPerformed
    {//GEN-HEADEREND:event_documentationMenuItemActionPerformed
        NocToolsWorker.getInstance().addTask(new LinkHandler(NocToolsSettings.getDocumentationLink()));
    }//GEN-LAST:event_documentationMenuItemActionPerformed

    private void saveSettingsMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_saveSettingsMenuItemActionPerformed
    {//GEN-HEADEREND:event_saveSettingsMenuItemActionPerformed
        NocToolsSettings.saveSettingsToFile();
    }//GEN-LAST:event_saveSettingsMenuItemActionPerformed

    private void enableLinkMenusCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_enableLinkMenusCheckBoxMenuItemActionPerformed
    {//GEN-HEADEREND:event_enableLinkMenusCheckBoxMenuItemActionPerformed
        if (enableLinkMenusCheckBoxMenuItem.isSelected())
            NocToolsSettings.setShowLinks(true);
        else
            NocToolsSettings.setShowLinks(false);           
    }//GEN-LAST:event_enableLinkMenusCheckBoxMenuItemActionPerformed

    private void shortNamesRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_shortNamesRadioButtonMenuItemActionPerformed
    {//GEN-HEADEREND:event_shortNamesRadioButtonMenuItemActionPerformed
        // if the server names were long, then we have to make them short and reload the tree
        if (NocToolsSettings.getUseShortServerNames() == false)
        {
            NocToolsSettings.setUseShortServerNames(true);
            try
            {
                reloadTree();
            }
            catch (Exception e)
            {
                if (e.getMessage()  != null)
                    org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
                e.printStackTrace();
            }
            updateStatusMessage();
            updateRemoteButtons();
            serversTree.grabFocus();
        }
    }//GEN-LAST:event_shortNamesRadioButtonMenuItemActionPerformed

    private void longNamesRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_longNamesRadioButtonMenuItemActionPerformed
    {//GEN-HEADEREND:event_longNamesRadioButtonMenuItemActionPerformed
        // if the server names were short, then we have to make them long and reload the tree
        if (NocToolsSettings.getUseShortServerNames() == true)
        {
            NocToolsSettings.setUseShortServerNames(false);
            try
            {
                reloadTree();
            }
            catch (Exception e)
            {
                if (e.getMessage()  != null)
                    org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
                e.printStackTrace();
            }
            updateStatusMessage();
            updateRemoteButtons();
            serversTree.grabFocus();
        }
    }//GEN-LAST:event_longNamesRadioButtonMenuItemActionPerformed

    private void clearAllButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_clearAllButtonActionPerformed
    {//GEN-HEADEREND:event_clearAllButtonActionPerformed
        serversTree.clearChecking();
        if (!selectionModeRadioButton.isSelected())
            selectionModeRadioButton.doClick();

        updateStatusMessage();
        updateRemoteButtons();

        serversTree.grabFocus();
    }//GEN-LAST:event_clearAllButtonActionPerformed

    private void expandOneLevelButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_expandOneLevelButtonActionPerformed
    {//GEN-HEADEREND:event_expandOneLevelButtonActionPerformed
        expandOneLevelOnly();
    }//GEN-LAST:event_expandOneLevelButtonActionPerformed

    private void expandAllButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_expandAllButtonActionPerformed
    {//GEN-HEADEREND:event_expandAllButtonActionPerformed
        serversTree.expandAll();
        updateStatusMessage();
        serversTree.grabFocus();
    }//GEN-LAST:event_expandAllButtonActionPerformed

    private void collapseAllButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_collapseAllButtonActionPerformed
    {//GEN-HEADEREND:event_collapseAllButtonActionPerformed
        collapseAll();
    }//GEN-LAST:event_collapseAllButtonActionPerformed

    private void remote1ButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_remote1ButtonActionPerformed
    {//GEN-HEADEREND:event_remote1ButtonActionPerformed
        generateRdpForSelectedServers(1);
        serversTree.grabFocus();
    }//GEN-LAST:event_remote1ButtonActionPerformed

    private void remote2ButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_remote2ButtonActionPerformed
    {//GEN-HEADEREND:event_remote2ButtonActionPerformed
        generateRdpForSelectedServers(2);
        serversTree.grabFocus();
    }//GEN-LAST:event_remote2ButtonActionPerformed

    private void remote3ButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_remote3ButtonActionPerformed
    {//GEN-HEADEREND:event_remote3ButtonActionPerformed
        generateRdpForSelectedServers(3);
        serversTree.grabFocus();
    }//GEN-LAST:event_remote3ButtonActionPerformed

    private void remote4ButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_remote4ButtonActionPerformed
    {//GEN-HEADEREND:event_remote4ButtonActionPerformed
        generateRdpForSelectedServers(4);
        serversTree.grabFocus();
    }//GEN-LAST:event_remote4ButtonActionPerformed

    private void remote5ButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_remote5ButtonActionPerformed
    {//GEN-HEADEREND:event_remote5ButtonActionPerformed
        generateRdpForSelectedServers(5);
        serversTree.grabFocus();
    }//GEN-LAST:event_remote5ButtonActionPerformed

    private void remote6ButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_remote6ButtonActionPerformed
    {//GEN-HEADEREND:event_remote6ButtonActionPerformed
        generateRdpForSelectedServers(6);
        serversTree.grabFocus();
    }//GEN-LAST:event_remote6ButtonActionPerformed

    private void nocButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_nocButtonActionPerformed
    {//GEN-HEADEREND:event_nocButtonActionPerformed
        RdpManager.addRdp("noc");
        serversTree.grabFocus();
    }//GEN-LAST:event_nocButtonActionPerformed

    private void opssupButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_opssupButtonActionPerformed
    {//GEN-HEADEREND:event_opssupButtonActionPerformed
        RdpManager.addRdp("opssup");
        serversTree.grabFocus();
    }//GEN-LAST:event_opssupButtonActionPerformed

    private void executeQuickScriptButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_executeQuickScriptButtonActionPerformed
    {//GEN-HEADEREND:event_executeQuickScriptButtonActionPerformed
        if (!serversScriptsList.isSelectionEmpty())
            executeScript(serversScriptsList.getSelectedValue().toString());
        else if (!customScriptsList.isSelectionEmpty())
            executeScript(customScriptsList.getSelectedValue().toString());
        else
        {
            // this block is needed in case where no quick script is selected
            JOptionPane.showMessageDialog(this.getComponent(), "Please select a script to execute");
            serversTree.grabFocus();
            updateStatusMessage();
            updateRemoteButtons();
        }
    }//GEN-LAST:event_executeQuickScriptButtonActionPerformed

    private void executeScriptButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_executeScriptButtonActionPerformed
    {//GEN-HEADEREND:event_executeScriptButtonActionPerformed
        int [] rows = scriptsTable.getSelectedRows();
        if (rows.length == 1)
        {
            String desctiption = scriptsTable.getModel().getValueAt(rows[0], 0).toString();
            executeScript(desctiption);
        }
        else
        {
            // this block is needed in case where no script selected
            JOptionPane.showMessageDialog(this.getComponent(), "Please select a script to execute");
            serversTree.grabFocus();
            updateStatusMessage();
            updateRemoteButtons();
        }
    }//GEN-LAST:event_executeScriptButtonActionPerformed

    private void editScriptButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_editScriptButtonActionPerformed
    {//GEN-HEADEREND:event_editScriptButtonActionPerformed
        int [] rows = scriptsTable.getSelectedRows();
        if (rows.length == 1)
        {
            JFrame mainFrame = NocToolsApp.getApplication().getMainFrame();
            String desctiption = scriptsTable.getModel().getValueAt(rows[0], 0).toString();
            Script selectedScript = ScriptsManager.getScript(desctiption);
            scriptEditor = new ScriptEditor(mainFrame, true, selectedScript, ScriptEditor.EditorMode.EDIT);
            scriptEditor.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            scriptEditor.pack();
            scriptEditor.setLocationRelativeTo(mainFrame);
            //scriptEditor.setVisible(true);
            NocToolsApp.getApplication().show(scriptEditor);

            // update all script tables
            int column0Width = scriptsTable.getColumnModel().getColumn(0).getWidth();
            int column1Width = scriptsTable.getColumnModel().getColumn(1).getWidth();
            int column2Width = scriptsTable.getColumnModel().getColumn(2).getWidth();
            scriptsTable.setModel(new ScriptsTableModel(ScriptsManager.getScripts()));
            scriptsTable.getColumnModel().getColumn(0).setPreferredWidth(column0Width);
            scriptsTable.getColumnModel().getColumn(1).setPreferredWidth(column1Width);
            scriptsTable.getColumnModel().getColumn(2).setPreferredWidth(column2Width);
            updateServersScriptsList();
            updateCustomScriptsList();
        }
        else
        {
            JOptionPane.showMessageDialog(this.getComponent(), "Please select one script to edit");
        }
    }//GEN-LAST:event_editScriptButtonActionPerformed

    private void newScriptButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_newScriptButtonActionPerformed
    {//GEN-HEADEREND:event_newScriptButtonActionPerformed
        JFrame mainFrame = NocToolsApp.getApplication().getMainFrame();
        scriptEditor = new ScriptEditor(mainFrame, true);
        scriptEditor.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        scriptEditor.pack();
        scriptEditor.setLocationRelativeTo(mainFrame);
        //scriptEditor.setVisible(true);
        NocToolsApp.getApplication().show(scriptEditor);

        // update all script tables
        int column0Width = scriptsTable.getColumnModel().getColumn(0).getWidth();
        int column1Width = scriptsTable.getColumnModel().getColumn(1).getWidth();
        int column2Width = scriptsTable.getColumnModel().getColumn(2).getWidth();
        scriptsTable.setModel(new ScriptsTableModel(ScriptsManager.getScripts()));
        scriptsTable.getColumnModel().getColumn(0).setPreferredWidth(column0Width);
        scriptsTable.getColumnModel().getColumn(1).setPreferredWidth(column1Width);
        scriptsTable.getColumnModel().getColumn(2).setPreferredWidth(column2Width);
        updateServersScriptsList();
        updateCustomScriptsList();
    }//GEN-LAST:event_newScriptButtonActionPerformed

    private void duplicateScriptButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_duplicateScriptButtonActionPerformed
    {//GEN-HEADEREND:event_duplicateScriptButtonActionPerformed
        int [] rows = scriptsTable.getSelectedRows();
        if (rows.length == 1)
        {
            JFrame mainFrame = NocToolsApp.getApplication().getMainFrame();
            String desctiption = scriptsTable.getModel().getValueAt(rows[0], 0).toString();
            Script selectedScript = ScriptsManager.getScript(desctiption);
            scriptEditor = new ScriptEditor(mainFrame, true, selectedScript, ScriptEditor.EditorMode.DUPLICATE);
            scriptEditor.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            scriptEditor.pack();
            scriptEditor.setLocationRelativeTo(mainFrame);
            //scriptEditor.setVisible(true);
            NocToolsApp.getApplication().show(scriptEditor);

            // update all script tables
            int column0Width = scriptsTable.getColumnModel().getColumn(0).getWidth();
            int column1Width = scriptsTable.getColumnModel().getColumn(1).getWidth();
            int column2Width = scriptsTable.getColumnModel().getColumn(2).getWidth();
            scriptsTable.setModel(new ScriptsTableModel(ScriptsManager.getScripts()));
            scriptsTable.getColumnModel().getColumn(0).setPreferredWidth(column0Width);
            scriptsTable.getColumnModel().getColumn(1).setPreferredWidth(column1Width);
            scriptsTable.getColumnModel().getColumn(2).setPreferredWidth(column2Width);
            updateServersScriptsList();
            updateCustomScriptsList();
        }
        else
        {
            JOptionPane.showMessageDialog(this.getComponent(), "Please select one script to duplicate");
        }
    }//GEN-LAST:event_duplicateScriptButtonActionPerformed

    private void deleteScriptButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_deleteScriptButtonActionPerformed
    {//GEN-HEADEREND:event_deleteScriptButtonActionPerformed
        int [] rows = scriptsTable.getSelectedRows();
        if (rows.length == 1)
        {
            String desctiption = scriptsTable.getModel().getValueAt(rows[0], 0).toString();
            int answer = JOptionPane.showConfirmDialog(null, desctiption, "Are you sure you want to delete?", JOptionPane.YES_NO_OPTION);

            if (answer == javax.swing.JOptionPane.YES_OPTION)
            {
                // create backup and delete the script from the scripts.xml
                // TODO add a check that this script is not used as "before" or "after" script (despite filtering)
                ScriptsManager.deleteScript(desctiption);

                // update all script tables
                int column0Width = scriptsTable.getColumnModel().getColumn(0).getWidth();
                int column1Width = scriptsTable.getColumnModel().getColumn(1).getWidth();
                int column2Width = scriptsTable.getColumnModel().getColumn(2).getWidth();
                scriptsTable.setModel(new ScriptsTableModel(ScriptsManager.getScripts()));
                scriptsTable.getColumnModel().getColumn(0).setPreferredWidth(column0Width);
                scriptsTable.getColumnModel().getColumn(1).setPreferredWidth(column1Width);
                scriptsTable.getColumnModel().getColumn(2).setPreferredWidth(column2Width);
                updateServersScriptsList();
                updateCustomScriptsList();
            }
        }
        else
        {
            JOptionPane.showMessageDialog(this.getComponent(), "Please select one script to delete");
        }
    }//GEN-LAST:event_deleteScriptButtonActionPerformed

    private void rdpRestrictionCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_rdpRestrictionCheckBoxMenuItemActionPerformed
    {//GEN-HEADEREND:event_rdpRestrictionCheckBoxMenuItemActionPerformed
        if (rdpRestrictionCheckBoxMenuItem.getState() == true)
        {
            NocToolsSettings.setRdpRestriction(true);
            updateRemoteButtons();
        }
        else
        {
            NocToolsSettings.setRdpRestriction(false);
            for (int i = 0; i < REMOTES; i++)
                remotesArray[i].setEnabled(true);
        }
    }//GEN-LAST:event_rdpRestrictionCheckBoxMenuItemActionPerformed

    private void settingsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_settingsMenuItemActionPerformed
        if (StartupManager.isIdle() || StartupManager.isPaused())
        {
            showSettingsDialog();
        }
        else
        {
            StartupManager.pause();
            showSettingsDialog();
            StartupManager.unpause();
        }

        NocToolsSettings.saveSettingsToFile();
        ServersTreeManager.removeAllcheckedServers();
        serversTree.clearChecking();
        collapseAll();
        initAllSettings();
        serversTree.grabFocus();
}//GEN-LAST:event_settingsMenuItemActionPerformed

    private void showSettingsDialog()
    {
        JFrame mainFrame = NocToolsApp.getApplication().getMainFrame();
        settingsDialog = new NocToolsSettingsDialog(mainFrame, true);
        settingsDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        settingsDialog.pack();
        settingsDialog.setLocationRelativeTo(mainFrame);
        NocToolsApp.getApplication().show(settingsDialog);
    }
    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        ServersTreeManager.CopySelectedServersToClipboard();
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void webDbButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_webDbButtonActionPerformed
        if (NocToolsSettings.getWebDb() != null)
        {
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();

            try {
                NocToolsWorker.getInstance().addTask(new LinkHandler(NocToolsSettings.getWebDb()));
                logNumberTextField.grabFocus();
            }
            catch ( Exception e )
            {
                if (e.getMessage()  != null)
                    org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
                e.printStackTrace();
            }
        }
    }//GEN-LAST:event_webDbButtonActionPerformed

    private void getLogButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_getLogButtonActionPerformed
        LogJob job;

        if (byLogNumberRadioButton.isSelected())
        {
            Server selectedServer = ServersTreeManager.getSelectedServer();
            if (selectedServer != null)
            {
                int logNumber = parseMiniserverNumber(logNumberTextField.getText());
                if (logNumber > -1)
                {
                    job = new SimpleLogJob(selectedServer, logNumber, extractionStatusList);

                    String email = null;
                    if (emailTheLogCheckBox.isSelected())
                        email = emailRecipientsComboBox.getSelectedItem().toString();

                    LogsManager manager = LogsManager.getInstance(job, email, extractionStatusList, "Log " + logNumber + " from " + selectedServer.getShortName());
                    if (manager != null)
                    {
                        // before starting a new job, clear all the messages from the extractionStatusList
                        updateStatusMessage(extractionStatusList, null);
                        org.apache.log4j.Logger.getRootLogger().info("Starting to extract log " + logNumber + " from " + selectedServer.getShortName());
                        NocToolsWorker.getInstance().addTask(manager);
                        serversTree.grabFocus();
                    }
                    else
                        JOptionPane.showMessageDialog(this.getFrame(), "Cannot extract multiple logs. Please wait");
                }
                else
                {
                    JOptionPane.showMessageDialog(this.getFrame(), "Please enter a valid log number (one of the following):\n- port number (decimal or hexadecimal)\n- log number");
                    logNumberTextField.grabFocus();
                }
            }
            else
            {
                JOptionPane.showMessageDialog(this.getFrame(), "Please select a server");
                serversTree.grabFocus();
            }

        }
        else if (byUserIdRadioButton.isSelected())
        {

        }

    }//GEN-LAST:event_getLogButtonActionPerformed

    private void addRecipientButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addRecipientButtonActionPerformed
        String newEmail = "";
        boolean done = false;
        do
        {
            newEmail = JOptionPane.showInputDialog(this.getFrame(), "Please enter a valid email", newEmail);
            if (newEmail != null)
            {
                if (vaildEmail(newEmail))
                    done = true;
                else
                    JOptionPane.showMessageDialog(this.getFrame(), "Invalid email address", "Error", JOptionPane.ERROR_MESSAGE);
            }
            else
                done = true;
        }
        while (!done && newEmail != null);

        if (newEmail != null)
        {
            NocToolsSettings.getEmailsList().add(newEmail);
            java.util.Collections.sort(NocToolsSettings.getEmailsList());
            updateEmailRecipientsComboBox();
            emailRecipientsComboBox.setSelectedItem(newEmail);
            NocToolsSettings.saveSettingsToFile();
        }
    }//GEN-LAST:event_addRecipientButtonActionPerformed

    private boolean vaildEmail(String email)
    {
        boolean valid = true;

        valid = valid && email.indexOf('@') > 0;
        if (valid)
        {
            valid = valid && email.indexOf('@') != 0;
            if (valid)
            {
                valid = valid && email.indexOf('@') != email.length() - 1;
                if (valid)
                {
                    valid = valid && email.indexOf('\\') < 0;
                    valid = valid && email.indexOf('/') < 0;
                    valid = valid && email.indexOf(',') < 0;
                    valid = valid && email.indexOf('*') < 0;
                    valid = valid && email.indexOf('#') < 0;
                    valid = valid && email.indexOf('%') < 0;
                    valid = valid && email.indexOf('{') < 0;
                    valid = valid && email.indexOf('}') < 0;
                    valid = valid && email.indexOf('[') < 0;
                    valid = valid && email.indexOf(']') < 0;
                    valid = valid && email.indexOf('(') < 0;
                    valid = valid && email.indexOf(')') < 0;
                    valid = valid && email.indexOf('+') < 0;
                    valid = valid && email.indexOf('=') < 0;
                    valid = valid && email.indexOf(' ') < 0;
                    valid = valid && email.indexOf('^') < 0;
                    valid = valid && email.indexOf(';') < 0;
                    valid = valid && email.indexOf(':') < 0;
                }
            }
        }
        return valid;
    }

    private void removeRecipientButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeRecipientButtonActionPerformed
        if (emailRecipientsComboBox.getSelectedIndex() != -1)
        {
            // ask the user if he really wants to delete the email
            String emailToRemove = emailRecipientsComboBox.getSelectedItem().toString();
            int answer = JOptionPane.showConfirmDialog(null, emailToRemove, "Are you sure you want to delete?", JOptionPane.YES_NO_OPTION);
            if (answer == javax.swing.JOptionPane.YES_OPTION)
            {
                // update the settings, refresh GUI, and save settings
                List <String> emailsList = NocToolsSettings.getEmailsList();
                int indexToDelete = java.util.Collections.binarySearch(emailsList, emailToRemove);
                emailsList.remove(indexToDelete);
                updateEmailRecipientsComboBox();
                NocToolsSettings.saveSettingsToFile();
            }
        }
    }//GEN-LAST:event_removeRecipientButtonActionPerformed

    private void remote7ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remote7ButtonActionPerformed
        generateRdpForSelectedServers(7);
        serversTree.grabFocus();
    }//GEN-LAST:event_remote7ButtonActionPerformed

    private void remote8ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remote8ButtonActionPerformed
        generateRdpForSelectedServers(8);
        serversTree.grabFocus();
    }//GEN-LAST:event_remote8ButtonActionPerformed

    private void workModeCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_workModeCheckBoxMenuItemActionPerformed
       updateShowWorkMode();
    }//GEN-LAST:event_workModeCheckBoxMenuItemActionPerformed

    private void treeButtonsCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_treeButtonsCheckBoxMenuItemActionPerformed
        updateShowTreeButtons();
    }//GEN-LAST:event_treeButtonsCheckBoxMenuItemActionPerformed

    private void customScriptsListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_customScriptsListMouseClicked
        if (evt.getClickCount() == 2 && evt.getButton() == evt.BUTTON1)
             if (!customScriptsList.isSelectionEmpty())
                executeScript(customScriptsList.getSelectedValue().toString());
    }//GEN-LAST:event_customScriptsListMouseClicked

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        ServersTreeManager.CopySelectedServersIpToClipboard();
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void clearStartupLogButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearStartupLogButtonActionPerformed
        updateStatusMessage(serverSartupStatusList, null);
    }//GEN-LAST:event_clearStartupLogButtonActionPerformed

    private void copyToClipboardButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyToClipboardButtonActionPerformed
        // get the system clipboard
        Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        String logToClipboard = new String();

        DefaultListModel model = (DefaultListModel)serverSartupStatusList.getModel();

        Object [] strings = model.toArray();

        for (int i = 0; i < strings.length; i++)
            logToClipboard += strings[i] + "\n";
        
        Transferable transferableText = new StringSelection(new String(logToClipboard));
        systemClipboard.setContents(transferableText, null);
    }//GEN-LAST:event_copyToClipboardButtonActionPerformed

    private void pauseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pauseButtonActionPerformed
        if (StartupManager.isPaused())
        {
            StartupManager.unpause();
            pauseButton.setText("Pause");
        }
        else
        {
            StartupManager.pause();
            pauseButton.setText("Resume");
        }
        serversTree.grabFocus();
    }//GEN-LAST:event_pauseButtonActionPerformed

    private void removeAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeAllButtonActionPerformed
        StartupManager.getInstance(serverSartupStatusList).removeAllServers();
        serversTree.grabFocus();
    }//GEN-LAST:event_removeAllButtonActionPerformed

    private void showRemoteButtonsCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showRemoteButtonsCheckBoxMenuItemActionPerformed
        boolean showButtons = NocToolsSettings.getShowRemoteButtons();

        showButtons = !showButtons;
        NocToolsSettings.setShowRemoteButtons(showButtons);

        for (int i = 1; i < remotesArray.length; i++) // don't hide remote1
            remotesArray[i].setVisible(showButtons);
    }//GEN-LAST:event_showRemoteButtonsCheckBoxMenuItemActionPerformed

    private void showServerStartuplCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_showServerStartuplCheckBoxMenuItemActionPerformed
    {//GEN-HEADEREND:event_showServerStartuplCheckBoxMenuItemActionPerformed
        boolean showServerStartupPanel = NocToolsSettings.getShowServerStartupPanel();

        showServerStartupPanel = !showServerStartupPanel;
        NocToolsSettings.setShowServerStartupPanel(showServerStartupPanel);

        serverStartupPanel.setVisible(showServerStartupPanel);
    }//GEN-LAST:event_showServerStartuplCheckBoxMenuItemActionPerformed

    /**
     * reloads the servers tree from the config.xml
     */
    private void reloadTree( )
    {
        try
        {
            ComparableMutableTreeNode root = NocToolsSettings.getTree();
            serversTree = new CheckboxTree(root);
            initTree(serversTree);
            ServersTreeManager.setSelectedServer(root);
            ServersTreeManager.removeAllcheckedServers();
            treeScrollPane.setViewportView(serversTree);
            updateStatusMessage();
            updateRemoteButtons();
            updateTreeButtons();
        }
        catch (Exception e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Reloads the tree from the config.xml file that was loaded to the NocToolsSettings class
     * @param tree Tree to rebuild
     */
    private void initTree(final CheckboxTree tree)
    {
        tree.getCheckingModel().setCheckingMode(TreeCheckingModel.CheckingMode.PROPAGATE_PRESERVING_CHECK);
        tree.setCellRenderer(new CheckBoxTreeCellRenderer());
        tree.setRowHeight(17);
        tree.setShowsRootHandles(false);

        /*
         * adds a mouse listener to the tree that will ldetect double clicks
         */
        tree.addMouseListener(new MouseAdapter()
        {
            @Override
            @SuppressWarnings("static-access")
            public void mousePressed(MouseEvent e)
            {
                int cellRow = tree.getRowForLocation(e.getX(), e.getY());
                TreePath cellPath = tree.getPathForLocation(e.getX(), e.getY());
                if (cellRow != -1)
                {
                    if (e.getClickCount() == 2)
                    {
                         // a double click was performed on the tree.
                        Object endpointObj = cellPath.getLastPathComponent();
                        if (endpointObj instanceof ComparableMutableTreeNode)
                        {
                            ComparableMutableTreeNode tempNode = (ComparableMutableTreeNode)endpointObj;
                            Object tempEndpoint = tempNode.getUserObject();
                            if (tempEndpoint instanceof Server)
                            {

                                // when a server is double clicked, the "next" RDP will open for all selected servers
                                if (NocToolsSettings.getDoubleClickOnServerOpensRdp())
                                    generateRdpForSelectedServers(0);

                            }
                        }
                    } // end of if (e.getClickCount() == 2)

                    // if the user clicked a right click on a country or a farm, then we have to display the links menu
                    else if (e.getButton() == e.BUTTON3 && NocToolsSettings.getShowLinks())
                    {
                        Object endpointObj = cellPath.getLastPathComponent();
                        if (endpointObj instanceof ComparableMutableTreeNode)
                        {
                            final ComparableMutableTreeNode tempNode = (ComparableMutableTreeNode)endpointObj;
                            final Object tempEndpoint = tempNode.getUserObject();
                            if (tempEndpoint instanceof Farm || tempEndpoint instanceof Country || tempEndpoint instanceof World)
                            {
                                // create a popup menu
                                JPopupMenu linksPopupMenu = new JPopupMenu();

                                // check if this endpoint has any links attached to it
                                final HashMap <String, String> linksMap = ((Endpoint)tempEndpoint).getLinks();
                                if (linksMap != null)
                                {

                                    // move all descriptions from the set to a list in order to sort them
                                    Set<String> descriptionSet = linksMap.keySet();
                                    java.util.List<String> descriptionsList = new java.util.LinkedList<String>();

                                    java.util.Iterator<String> setIterator = descriptionSet.iterator();
                                    while (setIterator.hasNext())
                                        descriptionsList.add(setIterator.next());

                                    java.util.Collections.sort(descriptionsList);
                                    java.util.Iterator<String> listIterator = descriptionsList.iterator();
                                    while (listIterator.hasNext())
                                    //for (String description : descriptionSet)
                                    {
                                        final String finalDescription = listIterator.next();
                                        JMenuItem menuLink = new JMenuItem(finalDescription);
                                        
                                        // add action listener to menu item, to that links will be opened in the browser
                                        menuLink.addActionListener(new ActionListener()
                                        {
                                            public void actionPerformed(ActionEvent e)
                                            {
                                                NocToolsWorker.getInstance().addTask(new LinkHandler(linksMap.get(finalDescription)));
                                            }
                                        });
                                        linksPopupMenu.add(menuLink);
                                    }

                                    linksPopupMenu.addSeparator();
                                }

                                // links editor menu item will be created for every world / farm /country
                                JMenuItem linksEditor = new JMenuItem(((Endpoint)tempEndpoint).getName() + " links editor...");
                                linksPopupMenu.add(linksEditor);
                                linksEditor.addActionListener(new ActionListener()
                                {
                                    public void actionPerformed(ActionEvent e)
                                    {
                                        if (!NocToolsSettings.getShowCountries() && NocToolsSettings.getShowFarms() && tempEndpoint instanceof Farm)
                                        {
                                            JOptionPane.showMessageDialog(null, "Please enable showing countries in order to edit this farm's links");
                                        }
                                        else
                                        {
                                            JFrame mainFrame = NocToolsApp.getApplication().getMainFrame();
                                            LinksEditor linksEditor= new LinksEditor(mainFrame, true, tempNode);
                                            linksEditor.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                                            linksEditor.pack();
                                            linksEditor.setLocationRelativeTo(mainFrame);
                                            NocToolsApp.getApplication().show(linksEditor);
                                        }

                                    }
                                }); // end of linksEditor.addActionListener(new ActionListener()

                                linksPopupMenu.show(e.getComponent(),e.getX(), e.getY());
                            }

                            // if the user right clicked on a server
                            else if (tempEndpoint instanceof Server)
                            {
                                serversTree.setSelectionPath(cellPath);
                                // if we should show a popup menu when a user right-clicks a server
                                if (NocToolsSettings.getShowServerPopupMenu())
                                {
                                    // find all the scripts that should be shown in the popup menu
                                    List<Script> popupScripts = new LinkedList<Script>();
                                    TreeMap<String, Script> scriptsMap = ScriptsManager.getScripts();
                                    Iterator <Entry<String, Script>> scriptsIterator =  scriptsMap.entrySet().iterator();
                                    while (scriptsIterator.hasNext())
                                    {
                                        Script tempScript = scriptsIterator.next().getValue();
                                        if (tempScript.getAppearInPopupMenu())
                                        popupScripts.add(tempScript);
                                    }

                                    // if we have some scripts that should be in the popup menu, than create the popup menu
                                    if (!popupScripts.isEmpty())
                                    {
                                        JPopupMenu serverPopupMenu = new JPopupMenu();

                                        // TODO - if this is checked mode, and the user right clicked on a server that is not chedked,
                                        // then we must notify the user that the scripts will run only on checked servers, and add a seperator
                                        if (ServersTreeManager.isCheckedMode())
                                        {
                                            JMenuItem messageMenuItem = new JMenuItem("ONLY CHECKED SERVERS WILL BE AFFECTED");
                                            serverPopupMenu.add(messageMenuItem);
                                            serverPopupMenu.add(new JSeparator());
                                        }

                                        for (Script tempScript : popupScripts)
                                        {

                                            JMenuItem scriptMenuItem = new JMenuItem(tempScript.getDescription());

                                            scriptMenuItem.addActionListener(new ActionListener()
                                            {
                                                public void actionPerformed(ActionEvent e)
                                                {
                                                    JMenuItem menuItem = (JMenuItem)e.getSource();
                                                    ScriptsManager.runScript(menuItem.getText());
                                                }
                                            });

                                            serverPopupMenu.add(scriptMenuItem);
                                        }
                                        serverPopupMenu.show(e.getComponent(),e.getX(), e.getY());

                                    }
                                }
                            }
                        }
                    }
                }// end of if  (cellRow != -1)

                // if the user double clicked on an empty space on the tree, it will expand one level only
                else if (e.getClickCount() == 2 && e.getButton() == e.BUTTON1)
                {
                    if (NocToolsSettings.getDoubleClickOnEmptyCollapsesTree())
                        collapseAll();
                }

                // right click on an empty space will clear the tree
                else if (e.getClickCount() == 1 && e.getButton() == e.BUTTON3)
                {
                    if (NocToolsSettings.getRightClickOnEmptyClearsSelection())
                    {
                        serversTree.clearChecking();
                        if (!selectionModeRadioButton.isSelected())
                            selectionModeRadioButton.doClick();

                        updateStatusMessage();
                        updateRemoteButtons();

                        serversTree.grabFocus();
                    }
                }
            }
        }); // end of mouse listener

        /*
         * add a  TreeCheckingListener so that every time a server is checked
         * the checking mode is activated
         */
        tree.addTreeCheckingListener(new TreeCheckingListener()
        {
            public void valueChanged(TreeCheckingEvent e)
            {
                if (e.isCheckedPath())
                {
                    if (!checkedModeRadioButton.isSelected())
                        checkedModeRadioButton.doClick();
                    ServersTreeManager.addCheckedPath(e.getPath().getLastPathComponent());
                    //org.apache.log4j.Logger.getRootLogger().debug("Check event");
                    updateStatusMessage();
                    updateRemoteButtons();

                }
                else
                {
                    ServersTreeManager.removeUncheckedPath(e.getPath().getLastPathComponent());
                    if (!ServersTreeManager.areThereCheckedServers())
                        if (!selectionModeRadioButton.isSelected())
                            selectionModeRadioButton.doClick();
                    //org.apache.log4j.Logger.getRootLogger().debug("Uncheck event");
                    updateStatusMessage();
                    updateRemoteButtons();
                }
            }
        }
        ); // end of addTreeCheckingListener

       /*
        * add TreeSelectionListener so that every time a server is selected,
        * the selection mode is activated. Also, this listener will hanle cases: if
        * user will want to mark a few servers by holding shift
        */
        tree.addTreeSelectionListener(new TreeSelectionListener()
        {
            public void valueChanged(TreeSelectionEvent e)
            {
                ServersTreeManager.setSelectedServer(e.getPath().getLastPathComponent());
                if (!selectionModeRadioButton.isSelected() && !ServersTreeManager.areThereCheckedServers())
                    selectionModeRadioButton.doClick();
                //org.apache.log4j.Logger.getRootLogger().debug("Selection event");

                updateStatusMessage();
                updateRemoteButtons();

            }
        });

        /**
         * add keyListener to the tree in order to capture keyboard shortcuts
         */
        tree.addKeyListener(new KeyListener ()
        {
            public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_CONTROL)
                    isControlKeyPressed = true;
                else if (isControlKeyPressed) // if CTRL was already pressed before
                {
  
                    // CTRL + 1 = remote1 rdp
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_1 && isControlKeyPressed)
                        generateRdpForSelectedServers(1);

                    // CTRL + 2
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_2 && isControlKeyPressed)
                        generateRdpForSelectedServers(2);

                    // CTRL + 3
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_3 && isControlKeyPressed)
                        generateRdpForSelectedServers(3);

                    // CTRL + 4
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_4 && isControlKeyPressed)
                        generateRdpForSelectedServers(4);

                    // CTRL + 5
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_5 && isControlKeyPressed)
                        generateRdpForSelectedServers(5);

                    // CTRL + 6
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_6 && isControlKeyPressed)
                        generateRdpForSelectedServers(6);

                    // CTRL + 7
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_7 && isControlKeyPressed)
                        generateRdpForSelectedServers(7);

                    // CTRL + 8
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_8 && isControlKeyPressed)
                        generateRdpForSelectedServers(8);

                    // CTRL + o = opssup rdp
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_O && isControlKeyPressed)
                        RdpManager.addRdp("opssup");

                    // CTRL + n = noc rdp
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_N && isControlKeyPressed)
                        RdpManager.addRdp("noc");

                    // CTRL + x = execute selected script
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_X && isControlKeyPressed)
                    {
                        if (!serversScriptsList.isSelectionEmpty())
                            executeScript(serversScriptsList.getSelectedValue().toString());
                        else if (!customScriptsList.isSelectionEmpty())
                            executeScript(customScriptsList.getSelectedValue().toString());
                        else
                        {
                            // this block is needed in case where no quick script is selected
                            JOptionPane.showMessageDialog(null, "Please select a script to execute");
                            serversTree.grabFocus();
                            updateStatusMessage();
                            updateRemoteButtons();
                        }
                    }




//                    // CTRL + a  was pressed. It should be disabled, so we need to remove selection
//                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_A && isControlKeyPressed)
//                    {
//                        TreePath [] paths = serversTree.getSelectionPaths();
//                        System.out.println("Num of selected paths : " +  paths.length);
//                        serversTree.clearSelection();
//                        serversTree.getSelectionModel().clearSelection();
//                        serversTree.setSelectionPaths(paths);
//
//                        System.out.println("tried to clear selection");
//
//                    }   //    NOT WORKING!!!!

                    // CTRL + g = start a server
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_G && isControlKeyPressed)
                    {
                        if (ServersTreeManager.isCheckedMode())
                            for (Server tempServer : ServersTreeManager.getCheckedServers())
                                StartupQueue.getInstance(null).addServer(tempServer);
                        else if (ServersTreeManager.isSelectionMode())
                            StartupQueue.getInstance(null).addServer(ServersTreeManager.getSelectedServer());
                    }

                     //CTRL + p = pause the startup manager
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_P && isControlKeyPressed)
                    {
                        if (StartupManager.isPaused())
                        {
                            StartupManager.unpause();
                            pauseButton.setText("Pause");
                        }
                        else
                        {
                            StartupManager.pause();
                            pauseButton.setText("Resume");
                        }
                    }




                    // CTRL + c = uncheck all
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_C && isControlKeyPressed)
                    {
                        serversTree.clearChecking();
                        if (!selectionModeRadioButton.isSelected())
                            selectionModeRadioButton.doClick();
                    }

                    // CTRL + l = copy checked servers to clipboard
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_L && isControlKeyPressed)
                    {
                        ServersTreeManager.CopySelectedServersToClipboard();
                    }

                    // CTRL + m = toggle selected/checked mode
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_M && isControlKeyPressed)
                    {
                        if (!selectionModeRadioButton.isSelected())
                            selectionModeRadioButton.doClick();
                        else
                            if (!checkedModeRadioButton.isSelected())
                                checkedModeRadioButton.doClick();
                    }
                    isControlKeyPressed = false;
                    updateStatusMessage();
                    updateRemoteButtons();

                    serversTree.grabFocus();
                }

                else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER)
                    generateRdpForSelectedServers(0);
            }

            /**
             * find out when CTRL is released and update the isControlKeyPressed variable
             */
            public void keyReleased(KeyEvent e)
            {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_CONTROL)
                {
                    isControlKeyPressed = false;
                }
            }

            public void keyTyped(KeyEvent e) {}
        }); // end of addKeyListener
        updateTreeButtons();
    }

    private void generateRdpForSelectedServers(int remoteNum)
    {
        Server[] selectedServers;
        if (ServersTreeManager.isCheckedMode())
        {
            selectedServers = ServersTreeManager.getCheckedServers();
            
            // for each server, check if it is possible to go to the next "remote"
            for (int i = 0; i < selectedServers.length; i++)
            {

                int rdpNum = remoteNum != 0? remoteNum - 1 : selectedServers[i].getCurrentRdp();

                int miniservers = selectedServers[i].getMiniservers();
                int miniserversPerRemote = NocToolsSettings.getMiniserversPerRemote();
                
                if (miniserversPerRemote * rdpNum < miniservers)
                {
                    selectedServers[i].setCurrentRdp(rdpNum + 1);
                    RdpManager.createRdpJob(selectedServers[i], "remote" + (rdpNum + 1));
                }
            }

            serversTree.update(serversTree.getGraphics());
        }
        else if (ServersTreeManager.isSelectionMode() && ServersTreeManager.getSelectedServer() != null)
        {
            Server selectedServer = ServersTreeManager.getSelectedServer();

            // for each server, check if it is possible to go to the next "remote"

            int rdpNum = remoteNum != 0? remoteNum - 1 : selectedServer.getCurrentRdp();
            int miniservers = selectedServer.getMiniservers();
            int miniserversPerRemote = NocToolsSettings.getMiniserversPerRemote();

            if (miniserversPerRemote * rdpNum < miniservers)
            {
                selectedServer.setCurrentRdp(rdpNum + 1);
                RdpManager.createRdpJob(selectedServer, "remote" + (rdpNum + 1));
                if (!(miniserversPerRemote * (rdpNum + 1) < miniservers))
                {
                    selectedServer.setCurrentRdp(0);
                }
            }

            serversTree.update(serversTree.getGraphics());
        }
    }

    /**
     * Sets the status message on the status bar according to selection state
     */
    public void updateStatusMessage()
    {
        if (ServersTreeManager.isCheckedMode())
        {
            if (ServersTreeManager.areThereCheckedServers())
            {
                if (ServersTreeManager.getCheckedServers().length > 1)
                    statusMessageLabel.setText(ServersTreeManager.getCheckedServers().length + " servers are selected");
                else if (ServersTreeManager.getCheckedServers().length == 1)
                    statusMessageLabel.setText("Selected server is - " + ServersTreeManager.getCheckedServers()[0].getShortName());
            }
        }
        else if (ServersTreeManager.isSelectionMode())
        {
            if (ServersTreeManager.getSelectedServer() == null)
                statusMessageLabel.setText("No selected server");
            else
                statusMessageLabel.setText("Selected server is - " + ServersTreeManager.getSelectedServer().getShortName());
        }
    }

    private void initScriptsTable()
    {
        Logger.getRootLogger().debug("Initing the scripts table");

        scriptsTable.setModel(new ScriptsTableModel(ScriptsManager.getScripts()));
        scriptsTable.addMouseListener(new MouseListener()
        {
            public void mouseClicked(MouseEvent e){}
            public void mouseEntered(MouseEvent e){}
            public void mouseExited(MouseEvent e){}
            public void mousePressed(MouseEvent e){}
            public void mouseReleased(MouseEvent e)
            {
                // !!! duplicated code from below.
                int [] rows = scriptsTable.getSelectedRows();
                if (rows.length != 0)
                {
                    String [] scriptsDescriptions = new String[rows.length];
                    for (int i = 0; i < rows.length; i++)
                         scriptsDescriptions[i] = scriptsTable.getModel().getValueAt(rows[i], 0).toString();  // 0 - Description column

                    ScriptsManager.setSelectedScriptsFromScriptsTab(scriptsDescriptions);
                }
            }
        }
        );

        scriptsTable.addKeyListener(new KeyListener()
        {
            public void keyPressed(KeyEvent e){}
            public void keyReleased(KeyEvent e)
            {
                // !!! duplicated code from above.
                int [] rows = scriptsTable.getSelectedRows();
                if (rows.length != 0)
                {
                    String [] scriptsDescriptions = new String[rows.length];
                    for (int i = 0; i < rows.length; i++)
                         scriptsDescriptions[i] = scriptsTable.getModel().getValueAt(rows[i], 0).toString();  // 0 - Description column

                    ScriptsManager.setSelectedScriptsFromScriptsTab(scriptsDescriptions);
                }
            }
            public void keyTyped(KeyEvent e){}

        }
        );

//        scriptsTable.getColumnModel().getColumn(0).setPreferredWidth(column0Width);
//        scriptsTable.getColumnModel().getColumn(1).setPreferredWidth(column1Width);
        scriptsTable.getColumnModel().getColumn(2).setPreferredWidth(50);

    }

    /**
     * Refreshes the view. Adds the server scripts to the quick server scripts list on the main panel
     */
    private void updateServersScriptsList()
    {
        DefaultListModel serverScriptsModel = new DefaultListModel();
        java.util.LinkedList<Script> serverScripts = ScriptsManager.getQuickScripts("server");
        for(Script tempScript : serverScripts)
            serverScriptsModel.addElement(tempScript);
        serversScriptsList.setModel(serverScriptsModel);
    }

    /**
     * Refreshes the view. Adds the custom scripts to the quick custom scripts list on the main panel
     */
    private void updateCustomScriptsList()
    {
        DefaultListModel customScriptsModel = new DefaultListModel();
        java.util.LinkedList<Script> customScripts = ScriptsManager.getQuickScripts("custom");
        for(Script tempScript : customScripts)
            customScriptsModel.addElement(tempScript);
        customScriptsList.setModel(customScriptsModel);
    }

    /**
     * Sets the remote buttons enabled or disabled according to the maximum
     * miniservers of all selected/checked servers
     */
    public  void updateRemoteButtons()
    {
        if (NocToolsSettings.getRdpRestriction())
        {
            int i = 0;
            int allowedRemotes = ServersTreeManager.getMaxMiniservers() / NocToolsSettings.getMiniserversPerRemote();

            while (i < allowedRemotes && i < REMOTES)
            {
                remotesArray[i].setEnabled(true);
                i++;
            }

            while (i < REMOTES)
            {
                remotesArray[i].setEnabled(false);
                i++;
            }
        }
    }
    
    /**
     * updates the state of "Expand One Level", "Expand All" and "Collapse All" buttons
     */
    public void updateTreeButtons()
    {
        clearAllButton.setEnabled(true);

        if (NocToolsSettings.getShowCountries() == true && NocToolsSettings.getShowFarms() == true)
            expandOneLevelButton.setEnabled(true);
        else
            expandOneLevelButton.setEnabled(false);

        if (NocToolsSettings.getShowCountries() == false && NocToolsSettings.getShowFarms() == false)
        {
            expandAllButton.setEnabled(false);
            collapseAllButton.setEnabled(false);
        }
        else
        {
            expandAllButton.setEnabled(true);
            collapseAllButton.setEnabled(true);
        }
    }

    public void updateEmailRecipientsComboBox()
    {
        List <String> emailsList = NocToolsSettings.getEmailsList();

        DefaultComboBoxModel model = new DefaultComboBoxModel();

        java.util.Collections.sort(emailsList);

        for (String email : emailsList)
            model.addElement(email);

        emailRecipientsComboBox.setModel(model);

        // update GUI
        if (model.getSize() == 0)
        {
            removeRecipientButton.setEnabled(false);
            emailRecipientsComboBox.setEnabled(false);
            emailTheLogCheckBox.setEnabled(false);
            emailTheLogCheckBox.setSelected(false);
        }
        else
        {
            removeRecipientButton.setEnabled(true);
            emailRecipientsComboBox.setEnabled(true);
            emailTheLogCheckBox.setEnabled(true);
        }
    }
    
    public boolean areThereMarkedNonServers()
    {
        boolean answer = false;
        TreePath[] checkingRoots = serversTree.getCheckingRoots();

        for (TreePath checkingRoot : checkingRoots)
        {
            Object nodeObject = checkingRoot.getLastPathComponent();
            if (nodeObject instanceof ComparableMutableTreeNode)
            {
                ComparableMutableTreeNode node = (ComparableMutableTreeNode)nodeObject;
                Object endpointObject = node.getUserObject();
                if (!(endpointObject instanceof Server))
                    answer = answer || true;
            }
            else
                throw new ClassCastException();

        }
        return answer;
    }

    /**
     * Executes a script with a given description
     * @param description
     */
    private void executeScript(String description)
    {
        if (description != null)
        {
            // if the script exists
            if (ScriptsManager.getScript(description) != null)
            {
                // if this is a server script
                if (ScriptsManager.getScript(description).getType().equalsIgnoreCase("server"))
                {
                    if (ServersTreeManager.areThereCheckedServers() || ServersTreeManager.getSelectedServer() != null)
                    {
                        if (!areThereMarkedNonServers() || ScriptsManager.getScript(description).getWorkOnWholeFarmsCountries())
                        {
                            ScriptsManager.runScript(description);
                            serversTree.clearChecking();
                        }
                        else
                        {
                            JOptionPane.showMessageDialog(this.getComponent(), "Cannot execute the scripts on whole farms/countries");
                            org.apache.log4j.Logger.getRootLogger().warn("The user has tried to execute \"" + description + "\" on a farm or a country");
                        }
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(this.getComponent(), "Please select a server in order to execute the script");
                    }
                } // then this is a custom script
                else
                {
                    ScriptsManager.runScript(description);
                }
            }
            else
            {
                JOptionPane.showMessageDialog(this.getComponent(), "ERROR - The script doesn't exists. Please check the scripts XML file for errors");
                org.apache.log4j.Logger.getRootLogger().error("The script \"" + description + "\" doesn't exists");
            }

        }
        else
            JOptionPane.showMessageDialog(this.getComponent(), "Please select a script to execute");

        serversTree.grabFocus();
        updateStatusMessage();
        updateRemoteButtons();
    }

    public int parseMiniserverNumber(String str)
    {
        int logNumber = -1;

        try
        {
            // if the user entered a normal log number 1- 999
            if (str.length() >=1 && str.length() <= 3)
                logNumber = Integer.parseInt(str);

            // if the user entered a port number 51999 - 52XXX
            if (str.length() == 5)
                logNumber = Integer.parseInt(str) - 51999;

            // if the user entered a hex number like cb4a
            if (str.length() == 4)
                logNumber = Integer.parseInt(str, 16) - 51999;
        }
        catch (Exception e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }

        return logNumber;
    }

    public static synchronized void updateStatusMessage(JList list, String message)
    {

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        String time = timeFormat.format(Calendar.getInstance().getTime());

        list.clearSelection();

        if (message != null)
        {
            DefaultListModel model = (DefaultListModel)list.getModel();
            model.addElement(time + " : " + message);

            int lastIndex = list.getModel().getSize() - 1;
            if (lastIndex >= 0)
            {
                list.ensureIndexIsVisible(lastIndex);
            }
        }
        else
        {
            DefaultListModel model = (DefaultListModel)list.getModel();
            model.clear();
        }

        // make sure that the list is not too big.
        if (list.getModel().getSize() >= LIST_MAX_SIZE)
            ((DefaultListModel)list.getModel()).remove(0);

        list.repaint();
    }

    /**
     * updates showing the work mode panel according to the setting in the "view" menu
     */
    private void updateShowWorkMode()
    {
        NocToolsSettings.setShowWorkMode(workModeCheckBoxMenuItem.isSelected());
        if (workModeCheckBoxMenuItem.isSelected())
        {
            workModePanel.setVisible(true);
            rightSplitPane.setDividerLocation(rightDividerLocation);
        }
        else
        {
            workModePanel.setVisible(false);
           rightSplitPane.setDividerLocation(1);
        }
    }

    /**
     * updates showing the tree buttons panel according to the setting in the "view" menu
     */
    private void updateShowTreeButtons()
    {
        NocToolsSettings.setShowTreeButtons(treeButtonsCheckBoxMenuItem.isSelected());
        if (treeButtonsCheckBoxMenuItem.isSelected())
        {
            treeButtonsPanel.setVisible(true);
            leftSplitPane.setDividerLocation(leftDividerLocation);
        }
        else
        {
            treeButtonsPanel.setVisible(false);
            leftSplitPane.setDividerLocation(1);
        }
    }

    @Override
    public void finalize()
    {
        NocToolsWorker.getInstance().stop();
    }

    // method that creates a thread that deletes this app's log files older than 1 month
    private void deleteOldLogFiles()
    {
        Runnable deleteOldLogs = new Runnable()
        {
            public void run()
            {
                File dir = new File(NocToolsSettings.getLogsDir());
                if (dir.exists() && dir.isDirectory())
                {
                    // build a file filter to filter out the deleted files
                    FileFilter filter = new FileFilter() {

                        // filter out only files that are older than 1 month and named as "nocTools.log.XXX-XX-XX.log"
                        public boolean accept(File file)
                        {
                            boolean answer = false;
                            if (file.exists() && file.isFile())
                            {

                                // check if the file is older than 1 month
                                if (System.currentTimeMillis() - file.lastModified() > 2592000000L)
                                {
                                    // if the file has "nocTools.log." and ends with a ".log"
                                    if (file.getName().contains("nocTools.log.") && file.getName().contains(".log") && file.getName().lastIndexOf(".log") == file.getName().length()  - ".log".length())
                                    {
                                        answer = true;
                                    }
                                }
                            }
                            return answer;
                        }
                    };

                    File [] filesArray = dir.listFiles(filter);

                    if (filesArray != null)
                    {
                        for (File file : filesArray)
                            file.delete();
                    }
                }

            }
        };

        Thread deleteOldLogsThread = new Thread(deleteOldLogs);
        NocToolsWorker.getInstance().addTask(deleteOldLogsThread);
    }

    public void setLoggingLevel()
    {
        String loggingLevel = NocToolsSettings.getLoggingLevel();
        if (loggingLevel != null)
        {
            if (loggingLevel.contains("debug"))
                Logger.getRootLogger().setLevel(Level.DEBUG);
            else if (loggingLevel.contains("info"))
                Logger.getRootLogger().setLevel(Level.INFO);
            else if (loggingLevel.contains("error"))
                Logger.getRootLogger().setLevel(Level.ERROR);
            else
                Logger.getRootLogger().setLevel(Level.INFO);
        }
        else
        {
            Logger.getRootLogger().setLevel(Level.INFO);
        }
        org.apache.log4j.Logger.getRootLogger().debug("Log level = " + org.apache.log4j.Logger.getRootLogger().getLevel());
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addRecipientButton;
    private javax.swing.ButtonGroup advancedExtractLogButtonGroup;
    private javax.swing.JPanel allScriptsPanel;
    private javax.swing.JRadioButton byLogNumberRadioButton;
    private javax.swing.JRadioButton byUserIdRadioButton;
    private javax.swing.JRadioButton checkedModeRadioButton;
    private javax.swing.JButton clearAllButton;
    private javax.swing.JButton clearStartupLogButton;
    private javax.swing.JButton collapseAllButton;
    private javax.swing.JButton copyToClipboardButton;
    private javax.swing.JLabel customScriptsLabel;
    private javax.swing.JList customScriptsList;
    private javax.swing.JButton deleteScriptButton;
    private javax.swing.JMenuItem documentationMenuItem;
    private javax.swing.JButton duplicateScriptButton;
    private javax.swing.JMenu editMenu;
    private javax.swing.JButton editScriptButton;
    private javax.swing.JComboBox emailRecipientsComboBox;
    private javax.swing.JCheckBox emailTheLogCheckBox;
    private javax.swing.JCheckBoxMenuItem enableLinkMenusCheckBoxMenuItem;
    private javax.swing.JFormattedTextField endTimeFormattedTextField;
    private javax.swing.JLabel endTimeLabel;
    private javax.swing.JButton executeQuickScriptButton;
    private javax.swing.JButton executeScriptButton;
    private javax.swing.JButton expandAllButton;
    private javax.swing.JButton expandOneLevelButton;
    private javax.swing.JMenuItem exportCheckedServersToFileMenuItem;
    private javax.swing.ButtonGroup extractLogButtonGroup;
    private javax.swing.JPanel extractSettingsPanel;
    private javax.swing.JList extractionStatusList;
    private javax.swing.JPanel extractionStatusPanel;
    private javax.swing.JScrollPane extractionStatusScrollPane;
    private javax.swing.JSeparator fileMenuSeparator;
    private javax.swing.JButton getLogButton;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JRadioButton latestLogRadioButton;
    private javax.swing.JSplitPane leftSplitPane;
    private javax.swing.JTextField logNumberTextField;
    private javax.swing.JPanel logsPanel;
    private javax.swing.JRadioButtonMenuItem longNamesRadioButtonMenuItem;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JScrollPane mainScrollPane;
    private javax.swing.JSplitPane mainSplitPane;
    private javax.swing.JPanel mainTabPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JButton newScriptButton;
    private javax.swing.JButton nocButton;
    private javax.swing.JButton opssupButton;
    private javax.swing.JButton pauseButton;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel queueLabel;
    private javax.swing.JPanel quickAccountsPanel;
    private javax.swing.JPanel quickScriptsPanel;
    private javax.swing.JCheckBoxMenuItem rdpRestrictionCheckBoxMenuItem;
    private javax.swing.JMenuItem refreshServersTreeMenuItem;
    private javax.swing.JButton remote1Button;
    private javax.swing.JButton remote2Button;
    private javax.swing.JButton remote3Button;
    private javax.swing.JButton remote4Button;
    private javax.swing.JButton remote5Button;
    private javax.swing.JButton remote6Button;
    private javax.swing.JButton remote7Button;
    private javax.swing.JButton remote8Button;
    private javax.swing.JButton removeAllButton;
    private javax.swing.JButton removeRecipientButton;
    private javax.swing.JSplitPane rightSplitPane;
    private javax.swing.JMenuItem saveSettingsMenuItem;
    private javax.swing.JPanel scriptsTabPanel;
    private javax.swing.JTable scriptsTable;
    private javax.swing.JRadioButton selectionModeRadioButton;
    private javax.swing.JMenu serverNamesMenu;
    private javax.swing.ButtonGroup serverNamesModeButtonGroup;
    private javax.swing.JList serverSartupStatusList;
    private javax.swing.JLabel serverScriptsLabel;
    private javax.swing.JPanel serverStartupPanel;
    private javax.swing.JList serversQueueList;
    private javax.swing.JList serversScriptsList;
    private javax.swing.JMenuItem settingsMenuItem;
    private javax.swing.JRadioButtonMenuItem shortNamesRadioButtonMenuItem;
    private javax.swing.JCheckBoxMenuItem showCountriesCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem showFarmsCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem showRemoteButtonsCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem showServerStartuplCheckBoxMenuItem;
    private javax.swing.JFormattedTextField startTimeFormattedTextField;
    private javax.swing.JLabel startTimeLabel;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JTabbedPane tabsPane;
    private javax.swing.JLabel tasksLabel;
    private javax.swing.JRadioButton timeFilterRadioButton;
    private javax.swing.JRadioButton todayRadioButton;
    private javax.swing.JMenu toolsMenu;
    private javax.swing.JCheckBoxMenuItem treeButtonsCheckBoxMenuItem;
    private javax.swing.JPanel treeButtonsPanel;
    private javax.swing.JScrollPane treeScrollPane;
    private javax.swing.JTextField userIdTextField;
    private javax.swing.JMenu viewMenu;
    private javax.swing.JButton webDbButton;
    private javax.swing.ButtonGroup workModeButtonGroup;
    private javax.swing.JCheckBoxMenuItem workModeCheckBoxMenuItem;
    private javax.swing.JPanel workModePanel;
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private javax.swing.JButton[] remotesArray;
    private final int REMOTES = 8;

    private boolean isControlKeyPressed = false;

    private CheckboxTree serversTree;
    private ScriptEditor scriptEditor;
    private NocToolsSettingsDialog settingsDialog;
    
    private JDialog aboutBox;

    private int leftDividerLocation = 28;
    private int rightDividerLocation = 54;

    private static final int LIST_MAX_SIZE = 1000;

    public org.apache.log4j.Logger logger;
}
