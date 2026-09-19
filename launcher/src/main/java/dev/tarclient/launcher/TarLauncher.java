package dev.tarclient.launcher;

import com.formdev.flatlaf.FlatDarkLaf;
import com.google.gson.*;
import dev.tarclient.config.ClientConfig;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.io.*;
import java.util.concurrent.*;

public final class TarLauncher extends JFrame {
    static final Color BG=LauncherTheme.BG,CARD=LauncherTheme.CARD,GREEN=LauncherTheme.ACCENT,MUTED=LauncherTheme.MUTED;
    public static final String VERSION="0.2.0";
    private final Path data,game,settingsPath;
    private ClientConfig config;
    private JsonObject prefs;
    private volatile MicrosoftAuth.Session session;
    private final JPanel body=new JPanel(new BorderLayout());
    private final JLabel status=new JLabel("Ready"), account=new JLabel("Not signed in");
    private final JTextArea logs=new JTextArea();
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private volatile boolean busy=false;
    private Future<?> activeTask;
    private final JProgressBar progress=new JProgressBar();
    private final Map<String,JButton> navigation=new LinkedHashMap<>();
    private int catalogRequest,catalogOffset;
    private String catalogQuery="",catalogCategory="",catalogSort="downloads";
    private volatile Process gameProcess;
    private ModManager mods;
    private java.nio.channels.FileChannel lockChannel;
    private java.nio.channels.FileLock processLock;
    public static void main(String[] args) throws Exception {
        if(Arrays.asList(args).contains("--smoke")) { Smoke.main(args);return; }
        FlatDarkLaf.setup();
        UIManager.put("defaultFont",new Font("Segoe UI",Font.PLAIN,14));
        UIManager.put("Button.arc",16);UIManager.put("Component.arc",14);UIManager.put("TextComponent.arc",14);
        UIManager.put("Component.borderColor",LauncherTheme.LINE);UIManager.put("TextComponent.background",CARD);
        UIManager.put("ScrollBar.width",8);UIManager.put("Button.borderWidth",0);
        UIManager.put("Panel.background",BG);UIManager.put("Button.background",CARD);UIManager.put("Component.focusColor",GREEN);
        SwingUtilities.invokeLater(()->{try {
            var frame=new TarLauncher();
            if(args.length>=2&&args[0].equals("--render-preview")){
                if(args.length>2){switch(args[2]){case "mods"->frame.discover();case "modules"->frame.modules();case "accounts"->frame.accounts();default->frame.home();}}
                frame.worker.submit(()->SwingUtilities.invokeLater(()->{
                    try{frame.addNotify();frame.validate();var surface=frame.getContentPane();surface.setSize(frame.getWidth(),frame.getHeight());layoutTree(surface);
                        var picture=new java.awt.image.BufferedImage(surface.getWidth(),surface.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
                        var graphics=picture.createGraphics();surface.printAll(graphics);graphics.dispose();javax.imageio.ImageIO.write(picture,"png",Path.of(args[1]).toFile());
                    }catch(Exception e){e.printStackTrace();}finally{frame.dispose();frame.worker.shutdownNow();System.exit(0);}
                }));
            }else frame.setVisible(true);
        }catch(Exception e){JOptionPane.showMessageDialog(null,e.getMessage(),"Tar Client",JOptionPane.ERROR_MESSAGE);}});
    }
    private static void layoutTree(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container nested)layoutTree(nested);}
    TarLauncher() throws Exception {
        super("Tar Client • Minecraft 1.21.11");
        data=Path.of(System.getProperty("tar.data",Path.of(System.getProperty("user.home"),"AppData","Local","TarClient").toString())).toAbsolutePath();
        game=Path.of(System.getProperty("tar.instance",data.resolve("instance-1.21.11").toString())).toAbsolutePath();settingsPath=game.resolve("config/tarclient.json");Files.createDirectories(game);
        lockChannel=java.nio.channels.FileChannel.open(data.resolve("launcher.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
        processLock=lockChannel.tryLock();if(processLock==null)throw new IOException("Tar Client is already running for this game folder.");
        config=ClientConfig.read(settingsPath);prefs=Files.exists(data.resolve("launcher.json"))?JsonParser.parseString(Files.readString(data.resolve("launcher.json"))).getAsJsonObject():new JsonObject();
        mods=new ModManager(game,this::status);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);setMinimumSize(new Dimension(1020,710));setSize(1180,820);setLocationRelativeTo(null);
        JPanel root=new JPanel(new BorderLayout());root.setBackground(BG);setContentPane(root);
        JPanel sidebar=column();sidebar.setBackground(new Color(15,22,32));sidebar.setBorder(new EmptyBorder(26,18,20,18));sidebar.setPreferredSize(new Dimension(206,0));
        JPanel brand=row();brand.setAlignmentX(0);brand.setMaximumSize(new Dimension(170,48));brand.add(LauncherTheme.monogram("T"));JPanel wordmark=column();wordmark.setOpaque(false);wordmark.add(label("TAR CLIENT",17,Color.WHITE));wordmark.add(label("YOUR NEXT SESSION",9,MUTED));brand.add(wordmark);sidebar.add(brand);sidebar.add(Box.createVerticalStrut(34));
        sidebar.add(label("PLAY & PERSONALIZE",9,MUTED));sidebar.add(Box.createVerticalStrut(12));
        nav(sidebar,"Play",this::home);nav(sidebar,"Client modules",this::modules);nav(sidebar,"Discover mods",this::discover);nav(sidebar,"Installed mods",this::installed);
        sidebar.add(Box.createVerticalStrut(24));sidebar.add(label("YOUR SPACE",10,MUTED));sidebar.add(Box.createVerticalStrut(12));
        nav(sidebar,"Accounts",this::accounts);nav(sidebar,"Settings",this::settings);nav(sidebar,"Activity",this::activity);
        sidebar.add(Box.createVerticalGlue());sidebar.add(LauncherTheme.badge("FABRIC  /  1.21.11"));sidebar.add(Box.createVerticalStrut(14));
        account.setForeground(MUTED);account.setFont(new Font("Segoe UI",Font.PLAIN,12));sidebar.add(account);
        sidebar.add(Box.createVerticalStrut(8));sidebar.add(label("Tarre Industries  /  "+VERSION,10,MUTED));
        root.add(sidebar,BorderLayout.WEST);body.setBorder(new EmptyBorder(28,30,18,30));root.add(body,BorderLayout.CENTER);
        JPanel footer=new JPanel(new BorderLayout(16,0));footer.setBackground(new Color(15,22,32));footer.setBorder(new EmptyBorder(10,22,10,22));status.setForeground(MUTED);status.setFont(new Font("Segoe UI",Font.PLAIN,12));footer.add(status);
        progress.setIndeterminate(true);progress.setPreferredSize(new Dimension(120,5));progress.setVisible(false);footer.add(progress,BorderLayout.EAST);root.add(footer,BorderLayout.SOUTH);
        logs.setEditable(false);logs.setFont(new Font("Consolas",Font.PLAIN,12));logs.setLineWrap(true);logs.setWrapStyleWord(true);
        addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent e){if(busy||running()){JOptionPane.showMessageDialog(TarLauncher.this,"Wait for the current operation or close Minecraft before closing the launcher.");return;}worker.shutdownNow();try{processLock.release();lockChannel.close();}catch(Exception ignored){}dispose();}});
        home();
    }
    private void nav(JPanel bar,String text,Runnable action){
        JButton b=button(text,action);b.setHorizontalAlignment(SwingConstants.LEFT);b.setPreferredSize(new Dimension(170,40));b.setMaximumSize(new Dimension(170,40));b.setAlignmentX(0);
        navigation.put(text,b);bar.add(b);bar.add(Box.createVerticalStrut(5));
    }
    private JLabel label(String text,int size,Color color){JLabel l=new JLabel(text);l.setFont(new Font("Segoe UI",size>=17?Font.BOLD:Font.PLAIN,size));l.setForeground(color);l.setAlignmentX(0);return l;}
    private JButton button(String text,Runnable action){JButton b=new JButton(text);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));b.setFont(new Font("Segoe UI",Font.BOLD,12));b.setBackground(new Color(30,42,56));b.setForeground(LauncherTheme.TEXT);b.setBorder(new EmptyBorder(10,14,10,14));b.setAlignmentX(0);b.addActionListener(e->action.run());return b;}
    private JButton primary(String text,Runnable action){JButton b=button(text,action);b.setBackground(GREEN);b.setForeground(new Color(18,31,20));return b;}
    private JPanel column(){JPanel p=new JPanel();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setAlignmentX(0);return p;}
    private JPanel row(){JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));p.setOpaque(false);return p;}
    private JPanel card(){return LauncherTheme.panel(20);}
    private JTextArea wrap(String text,int rows){JTextArea area=new JTextArea(text);area.setEditable(false);area.setFocusable(false);area.setLineWrap(true);area.setWrapStyleWord(true);area.setOpaque(false);area.setForeground(MUTED);area.setFont(new Font("Segoe UI",Font.PLAIN,13));area.setRows(rows);area.setAlignmentX(0);return area;}
    private void page(String title,String subtitle,JComponent content){
        navigation.forEach((name,b)->{boolean selected=name.equals(title);b.setBackground(selected?new Color(40,57,47):new Color(15,22,32));b.setForeground(selected?GREEN:MUTED);});
        body.removeAll();JPanel head=new JPanel(new BorderLayout());head.setOpaque(false);JPanel headings=column();headings.setOpaque(false);
        headings.add(label(title,28,Color.WHITE));headings.add(Box.createVerticalStrut(6));headings.add(label(subtitle,12,MUTED));headings.setBorder(new EmptyBorder(0,0,24,0));head.add(headings);
        JPanel profile=row();profile.add(button(session==null?"Connect Microsoft":session.name(),this::accounts));head.add(profile,BorderLayout.EAST);
        body.add(head,BorderLayout.NORTH);body.add(content);body.revalidate();body.repaint();
    }
    private JScrollPane scroll(JComponent c){JPanel host=new JPanel(new BorderLayout());host.add(c,BorderLayout.NORTH);JScrollPane s=new JScrollPane(host);s.setBorder(null);s.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);s.getVerticalScrollBar().setUnitIncrement(22);return s;}
    private void home(){
        JPanel content=column();JPanel hero=new LauncherTheme.Hero();hero.setAlignmentX(0);
        hero.add(LauncherTheme.badge("MINECRAFT JAVA  /  1.21.11"));hero.add(Box.createVerticalStrut(26));
        hero.add(label("Your world.",43,Color.WHITE));hero.add(label("Your rules.",43,GREEN));hero.add(Box.createVerticalStrut(14));
        hero.add(label("A client that feels like you. Ready for the next session.",14,new Color(178,197,191)));hero.add(Box.createVerticalStrut(28));
        JPanel actions=row();actions.setAlignmentX(0);JButton play=primary(running()?"Minecraft is running":"Play Minecraft  >",()->launch(false));play.setFont(new Font("Segoe UI",Font.BOLD,16));play.setPreferredSize(new Dimension(212,48));play.setEnabled(!running()&&!busy);actions.add(play);
        actions.add(button("Try demo",()->launch(true)));hero.add(actions);hero.add(Box.createVerticalStrut(15));hero.add(label("Fabric "+GameInstaller.LOADER+"    /    Java 21    /    "+(Integer.parseInt(pref("ram","4096"))/1024)+" GB RAM",11,MUTED));
        content.add(hero);content.add(Box.createVerticalStrut(18));
        JPanel shortcuts=new JPanel(new GridLayout(1,2,14,0));shortcuts.setOpaque(false);shortcuts.setAlignmentX(0);
        JPanel personalize=card();personalize.add(label("Make it yours",19,Color.WHITE));personalize.add(Box.createVerticalStrut(8));personalize.add(wrap("Tune your crosshair, HUD and visuals. Open the in-game menu with Right Shift.",2));personalize.add(Box.createVerticalStrut(16));personalize.add(button("Customize client  >",this::modules));shortcuts.add(personalize);
        JPanel extend=card();extend.add(label("Find your next mod",19,Color.WHITE));extend.add(Box.createVerticalStrut(8));extend.add(wrap("Browse compatible Fabric mods and add your favorites from Modrinth.",2));extend.add(Box.createVerticalStrut(16));extend.add(button("Explore mods  >",this::discover));shortcuts.add(extend);
        content.add(shortcuts);content.add(Box.createVerticalStrut(18));JPanel tools=row();tools.setAlignmentX(0);tools.add(button("Install / verify files",()->operation(()->prepare(),()->status("Installation verified"))));tools.add(button("Open game folder",()->open(game)));content.add(tools);
        content.add(Box.createVerticalStrut(14));content.add(label("Not an official Minecraft product. Not affiliated with Mojang or Microsoft.",10,MUTED));
        page("Play","Welcome back. Let's build something.",scroll(content));
    }
    private boolean running(){return gameProcess!=null&&gameProcess.isAlive();}
    private void status(String message){SwingUtilities.invokeLater(()->{status.setText(message);logs.append("["+java.time.LocalTime.now().withNano(0)+"] "+message+"\n");if(logs.getDocument().getLength()>250000)logs.setText(logs.getText().substring(100000));logs.setCaretPosition(logs.getDocument().getLength());});}
    @FunctionalInterface interface Work {void run() throws Exception;}
    private void operation(Work work,Runnable success){
        if(busy||running()){JOptionPane.showMessageDialog(this,running()?"Close Minecraft before changing the installation.":"Another operation is still running.");return;}
        busy=true;progress.setVisible(true);activeTask=worker.submit(()->{try{work.run();SwingUtilities.invokeLater(success);}catch(InterruptedException | CancellationException e){Thread.currentThread().interrupt();status("Operation cancelled");}catch(Exception e){status("Failed: "+e.getMessage());SwingUtilities.invokeLater(()->JOptionPane.showMessageDialog(this,e.getMessage(),"Tar Client",JOptionPane.ERROR_MESSAGE));}finally{busy=false;SwingUtilities.invokeLater(()->progress.setVisible(false));}});
    }
    private void accounts(){
        JPanel list=column();JPanel main=card();main.add(LauncherTheme.badge(session==null?"MICROSOFT ACCOUNT":"CONNECTED"));main.add(Box.createVerticalStrut(18));
        main.add(label(session==null?"One account. All your worlds.":"Hey, "+session.name()+".",27,Color.WHITE));main.add(Box.createVerticalStrut(12));
        main.add(wrap(session==null?"Use the Microsoft account that owns Minecraft Java Edition. Tar Client starts the connection here; Microsoft securely completes it in your browser.":"Your Minecraft account is connected for this session. You're ready to launch your game.",3));main.add(Box.createVerticalStrut(20));
        JPanel actions=row();actions.setAlignmentX(0);
        if(session==null){actions.add(primary("Sign in with Microsoft",this::login));actions.add(button("Create Microsoft account",()->browse("https://signup.live.com/")));}
        else{actions.add(primary("Play Minecraft",()->launch(false)));actions.add(button("Sign out",()->{if(canEdit()){session=null;account.setText("Not signed in");accounts();}}));}
        main.add(actions);main.add(Box.createVerticalStrut(16));main.add(label("No separate Tar account is needed. Never enter your Microsoft password into Tar.",12,MUTED));list.add(main);
        list.add(Box.createVerticalStrut(16));JPanel info=card();info.add(label("New to Minecraft?",19,Color.WHITE));info.add(Box.createVerticalStrut(8));info.add(wrap("Creating a Microsoft account is free. Playing the full game needs a Minecraft Java entitlement. You can try Minecraft's demo while setting up your account.",3));info.add(Box.createVerticalStrut(12));info.add(button("Try Minecraft demo",()->launch(true)));list.add(info);
        if(pref("clientId","").isBlank()){
            list.add(Box.createVerticalStrut(16));JPanel setup=card();setup.add(label("Microsoft connection setup required",17,GREEN));setup.add(Box.createVerticalStrut(8));setup.add(wrap("This preview does not yet include Tar Client's registered Microsoft application ID. Sign-in becomes available once that publisher setup is complete. If you already have an approved ID, you can configure it below.",3));setup.add(Box.createVerticalStrut(12));setup.add(button("Configure connection",this::settings));list.add(setup);
        }
        page("Accounts","Your Minecraft identity, connected securely.",scroll(list));
    }
    private void login(){
        if(pref("clientId","").isBlank()){accounts();status("Microsoft connection needs the publisher's application ID. See the setup card.");return;}
        operation(()->{session=new MicrosoftAuth().login(pref("clientId",""),code->SwingUtilities.invokeLater(()->{
            JDialog dialog=new JDialog(this,"Connect your Microsoft account",false);dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            JPanel panel=card();panel.setPreferredSize(new Dimension(500,330));panel.add(label("Connect to Minecraft",25,Color.WHITE));panel.add(Box.createVerticalStrut(12));panel.add(wrap("Enter this one-time code on Microsoft's secure page. Your password stays with Microsoft.",2));panel.add(Box.createVerticalStrut(16));
            JTextField field=new JTextField(code.code());field.setEditable(false);field.setHorizontalAlignment(SwingConstants.CENTER);field.setFont(new Font("Consolas",Font.BOLD,30));field.setMaximumSize(new Dimension(460,50));panel.add(field);panel.add(Box.createVerticalStrut(16));
            JPanel actions=row();actions.add(primary("Open Microsoft",()->browse(code.url())));actions.add(button("Copy code",()->Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(code.code()),null)));actions.add(button("Cancel",()->{if(activeTask!=null)activeTask.cancel(true);dialog.dispose();}));panel.add(actions);panel.add(Box.createVerticalStrut(12));panel.add(label("Waiting for Microsoft  /  expires in "+code.expiresIn()/60+" minutes",12,MUTED));
            dialog.add(panel);dialog.pack();dialog.setLocationRelativeTo(this);dialog.setVisible(true);browse(code.url());
            var timer=new javax.swing.Timer(700,e->{if(session!=null||!busy)dialog.dispose();});timer.start();
            dialog.addWindowListener(new WindowAdapter(){public void windowClosed(WindowEvent e){timer.stop();if(session==null&&busy&&activeTask!=null)activeTask.cancel(true);}});
        }));},()->{account.setText(session.name());status("Connected as "+session.name());accounts();});
    }
    private GameInstaller.Installation prepare() throws Exception {
        config.save(settingsPath);installCore();mods.installDefaults();mods.preflight();
        return new GameInstaller(game,this::status).install();
    }
    private void installCore() throws Exception {
        try(var in=getClass().getResourceAsStream("/bundled/tar-client.jar")) {
            CoreInstaller.install(game,in,VERSION);
        }
    }
    private void launch(boolean demo){
        if(!demo&&session==null){accounts();return;}
        if(!demo&&session.expiresAt()<=System.currentTimeMillis()){session=null;account.setText("Session expired");accounts();status("Please sign in again; your Microsoft session expired.");return;}
        MicrosoftAuth.Session chosen=demo?MicrosoftAuth.Session.demoSession():session;
        operation(()->{
            var install=prepare();status("Starting "+(demo?"Minecraft demo":"Minecraft")+"…");
            String java=Path.of(System.getProperty("java.home"),"bin","java.exe").toString();
            gameProcess=new GameInstaller(game,this::status).launch(install,chosen,Integer.parseInt(pref("ram","4096")),java);
            Thread.ofVirtual().start(()->{try(var input=gameProcess.inputReader();var output=Files.newBufferedWriter(data.resolve("game-output.log"))){String line;while((line=input.readLine())!=null){String safe=chosen.demo()?line:line.replace(chosen.accessToken(),"[redacted]");output.write(safe);output.newLine();if(safe.contains("ERROR")||safe.contains("Exception"))status(safe);}int exit=gameProcess.waitFor();status("Minecraft closed (exit "+exit+")");SwingUtilities.invokeLater(()->{try{config=ClientConfig.read(settingsPath);}catch(Exception e){status(e.getMessage());}home();});}catch(Exception e){status("Log reader: "+e.getMessage());}});
        },()->{status("Minecraft is running. Right Shift opens Tar settings.");home();});
    }
    private void modules(){
        JPanel panel=new JPanel(new BorderLayout(0,18));JTextField search=new JTextField();search.putClientProperty("JTextField.placeholderText","Search your modules...");search.setPreferredSize(new Dimension(300,40));
        JComboBox<String> category=new JComboBox<>(new String[]{"All","HUD","Visual","Window"});JPanel filters=new JPanel(new BorderLayout(12,0));filters.add(search);filters.add(category,BorderLayout.EAST);panel.add(filters,BorderLayout.NORTH);
        JPanel grid=new JPanel(new GridLayout(0,2,14,14));grid.setOpaque(false);
        Runnable refresh=()->{grid.removeAll();for(var m:ClientConfig.MODULES){
            if(!category.getSelectedItem().equals("All")&&!m.category().equals(category.getSelectedItem()))continue;
            if(!(m.name()+" "+m.description()).toLowerCase(Locale.ROOT).contains(search.getText().toLowerCase(Locale.ROOT)))continue;
            JPanel c=card();JPanel title=new JPanel(new BorderLayout(10,0));title.setOpaque(false);title.add(label(m.name(),18,Color.WHITE));title.add(LauncherTheme.badge(m.category().toUpperCase(Locale.ROOT)),BorderLayout.EAST);c.add(title);c.add(Box.createVerticalStrut(10));c.add(wrap(m.description(),3));c.add(Box.createVerticalStrut(14));
            JPanel actions=row();actions.setAlignmentX(0);JButton toggle=button(config.on(m.id())?"Enabled":"Disabled",()->{});toggle.addActionListener(e->{if(canEdit()){config.set(m.id(),"enabled",!config.on(m.id()));saveConfig();toggle.setText(config.on(m.id())?"Enabled":"Disabled");toggle.setForeground(config.on(m.id())?GREEN:MUTED);}});toggle.setForeground(config.on(m.id())?GREEN:MUTED);actions.add(toggle);actions.add(button("Settings",()->moduleSettings(m)));c.add(actions);grid.add(c);
        }if(grid.getComponentCount()==0)grid.add(label("No modules match your search.",15,MUTED));grid.revalidate();grid.repaint();};
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){public void insertUpdate(javax.swing.event.DocumentEvent e){refresh.run();}public void removeUpdate(javax.swing.event.DocumentEvent e){refresh.run();}public void changedUpdate(javax.swing.event.DocumentEvent e){refresh.run();}});category.addActionListener(e->refresh.run());refresh.run();panel.add(scroll(grid));
        page("Client modules","Your HUD and visuals. Right Shift opens the menu in game.",panel);
    }
    private void moduleSettings(ClientConfig.Module m){
        JPanel c=card();c.add(label(m.name(),24,Color.WHITE));c.add(Box.createVerticalStrut(8));c.add(wrap(m.description(),2));c.add(Box.createVerticalStrut(16));
            for(var s:m.settings())if(!s.key().equals("enabled")){JPanel r=new JPanel(new BorderLayout(20,8));r.setOpaque(false);r.setMaximumSize(new Dimension(10000,40));r.add(label(s.label(),13,MUTED));JComponent control;
                if(s.initial() instanceof Boolean){JCheckBox check=new JCheckBox("",config.bool(m.id(),s.key()));check.setOpaque(false);check.addActionListener(e->{if(canEdit()){config.set(m.id(),s.key(),check.isSelected());saveConfig();}else check.setSelected(config.bool(m.id(),s.key()));});control=check;}
                else if(s.initial() instanceof Number){JSpinner spinner=new JSpinner(new SpinnerNumberModel(config.number(m.id(),s.key()),s.min(),s.max(),s.step()));spinner.addChangeListener(e->{if(canEdit()){config.set(m.id(),s.key(),spinner.getValue());saveConfig();}});control=spinner;}
                else{JTextField field=new JTextField(config.text(m.id(),s.key()),18);field.addActionListener(e->{if(canEdit()){config.set(m.id(),s.key(),field.getText());saveConfig();}});field.addFocusListener(new FocusAdapter(){public void focusLost(FocusEvent e){if(!running()&&!busy){config.set(m.id(),s.key(),field.getText());saveConfig();}}});control=field;}
                control.setPreferredSize(new Dimension(210,30));r.add(control,BorderLayout.EAST);c.add(r);c.add(Box.createVerticalStrut(6));}
        JDialog dialog=new JDialog(this,m.name(),false);dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);dialog.add(scroll(c));dialog.setSize(560,Math.min(650,180+m.settings().size()*46));dialog.setLocationRelativeTo(this);dialog.setVisible(true);
    }
    private boolean canEdit(){if(running()||busy){JOptionPane.showMessageDialog(this,"Use the in-game settings while Minecraft runs, or wait for the current operation.");return false;}return true;}
    private void saveConfig(){try{config.save(settingsPath);}catch(Exception e){status("Could not save settings: "+e.getMessage());}}
    private void discover(){
        JPanel panel=new JPanel(new BorderLayout(0,16));JPanel filters=column();filters.setOpaque(false);
        JPanel searchRow=new JPanel(new BorderLayout(10,0));searchRow.setOpaque(false);searchRow.setAlignmentX(0);JTextField field=new JTextField(catalogQuery);field.putClientProperty("JTextField.placeholderText","Search Modrinth for your next favorite mod");field.setPreferredSize(new Dimension(350,42));searchRow.add(field);
        JComboBox<String> sort=new JComboBox<>(new String[]{"Most downloaded","Relevance","Recently updated","Newest"});sort.setSelectedIndex(List.of("downloads","relevance","updated","newest").indexOf(catalogSort));searchRow.add(sort,BorderLayout.EAST);filters.add(searchRow);filters.add(Box.createVerticalStrut(12));
        JPanel categories=row();categories.setAlignmentX(0);String[] names={"All mods","Performance","Utility","Decorative"},ids={"","optimization","utility","decoration"};
        for(int i=0;i<names.length;i++){String id=ids[i];JButton chip=button(names[i],()->{catalogQuery=field.getText();catalogCategory=id;catalogOffset=0;discover();});if(id.equals(catalogCategory))chip.setForeground(GREEN);categories.add(chip);}filters.add(categories);panel.add(filters,BorderLayout.NORTH);
        JPanel results=new JPanel(new GridLayout(0,2,14,14));results.setOpaque(false);results.add(label("Finding compatible mods...",15,MUTED));panel.add(scroll(results));
        JLabel summary=label("Fabric 1.21.11  /  Modrinth",12,MUTED);JPanel bottom=new JPanel(new BorderLayout());bottom.setOpaque(false);bottom.add(summary);JPanel pagination=row();JButton prev=button("< Previous",()->{catalogOffset=Math.max(0,catalogOffset-12);discover();});JButton next=button("Next >",()->{catalogOffset+=12;discover();});prev.setEnabled(catalogOffset>0);next.setEnabled(false);pagination.add(prev);pagination.add(next);bottom.add(pagination,BorderLayout.EAST);panel.add(bottom,BorderLayout.SOUTH);
        Runnable find=()->{catalogQuery=field.getText().trim();catalogSort=List.of("downloads","relevance","updated","newest").get(sort.getSelectedIndex());catalogOffset=0;discover();};
        field.addActionListener(e->find.run());sort.addActionListener(e->find.run());searchRow.add(button("Search",find),BorderLayout.WEST);
        page("Discover mods","Compatible with your version. Dependencies installed for you.",panel);
        int request=++catalogRequest,offset=catalogOffset;String query=catalogQuery,index=catalogSort,cat=catalogCategory;
        worker.submit(()->{try{var found=mods.searchPage(query,index,offset,cat);SwingUtilities.invokeLater(()->{
            if(request!=catalogRequest)return;results.removeAll();for(var item:found.hits())results.add(modCard(item.getAsJsonObject()));
            if(found.hits().isEmpty())results.add(wrap("No compatible mods found. Try another search or category.",3));
            summary.setText(String.format(Locale.ROOT,"%,d mods  /  Page %d",found.total(),offset/12+1));next.setEnabled(offset+12<found.total());results.revalidate();results.repaint();
        });}catch(Exception e){SwingUtilities.invokeLater(()->{if(request!=catalogRequest)return;results.removeAll();JPanel error=card();error.add(label("Couldn't reach Modrinth",18,Color.WHITE));error.add(wrap(e.getMessage(),3));error.add(button("Try again",this::discover));results.add(error);summary.setText("Check your connection and retry.");results.revalidate();results.repaint();});}});
    }
    private JPanel modCard(JsonObject hit){
        String title=hit.get("title").getAsString(),project=hit.get("project_id").getAsString();JPanel c=card();JPanel heading=new JPanel(new BorderLayout(12,0));heading.setOpaque(false);JLabel icon=LauncherTheme.monogram(title.substring(0,1).toUpperCase(Locale.ROOT));heading.add(icon,BorderLayout.WEST);JPanel words=column();words.setOpaque(false);JLabel name=label(title,17,Color.WHITE);name.setToolTipText(title);words.add(name);words.add(label("by "+hit.get("author").getAsString(),12,MUTED));heading.add(words);c.add(heading);c.add(Box.createVerticalStrut(12));
        if(hit.has("icon_url")&&!hit.get("icon_url").isJsonNull())ModIcons.load(icon,hit.get("icon_url").getAsString());
        c.add(wrap(hit.get("description").getAsString(),3));c.add(Box.createVerticalStrut(12));
        c.add(label(String.format(Locale.ROOT,"%,d downloads  /  FABRIC",hit.get("downloads").getAsLong()),11,MUTED));c.add(Box.createVerticalStrut(14));
        JPanel actions=row();actions.setAlignmentX(0);JButton install=primary("Install",()->{});install.addActionListener(e->operation(()->mods.install(project),()->{install.setText("Installed");install.setEnabled(false);status("Installed "+title+" with required dependencies");}));actions.add(install);actions.add(button("Details",()->browse("https://modrinth.com/mod/"+hit.get("slug").getAsString())));c.add(actions);return c;
    }
    private void installed(){
        JPanel panel=new JPanel(new BorderLayout());JPanel actions=row();actions.add(button("Import Fabric JARs…",()->{if(!canEdit())return;JFileChooser chooser=new JFileChooser();chooser.setMultiSelectionEnabled(true);chooser.setFileFilter(new FileNameExtensionFilter("Fabric mods (*.jar)","jar"));if(chooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION)operation(()->{for(File f:chooser.getSelectedFiles())mods.importJar(f.toPath());},this::installed);}));actions.add(button("Check dependencies",()->operation(()->mods.preflight(),()->status("All required dependencies are satisfied"))));actions.add(button("Open mods folder",()->open(game.resolve("mods"))));panel.add(actions,BorderLayout.NORTH);JPanel list=column();
        try{for(Path p:mods.list()){var m=mods.metadata(p);JPanel c=card();c.add(label((m.has("name")?m.get("name"):m.get("id")).getAsString(),18,Color.WHITE));c.add(label(m.get("version").getAsString()+"  •  "+(p.toString().endsWith(".disabled")?"Disabled":"Enabled"),12,MUTED));JPanel a=row();a.setOpaque(false);a.add(button(p.toString().endsWith(".disabled")?"Enable":"Disable",()->operation(()->mods.toggle(p),this::installed)));a.add(button("Remove",()->operation(()->mods.remove(p),this::installed)));c.add(a);list.add(c);list.add(Box.createVerticalStrut(12));}}catch(Exception e){list.add(label(e.getMessage(),13,MUTED));}panel.add(scroll(list));page("Installed mods","Import local Fabric mods, toggle modules, and check their dependencies.",panel);
    }
    private String pref(String key,String fallback){return prefs.has(key)?prefs.get(key).getAsString():fallback;}
    private void settings(){
        JPanel list=column();JPanel gameCard=card();gameCard.add(label("Game preferences",22,Color.WHITE));gameCard.add(Box.createVerticalStrut(10));gameCard.add(wrap("Set how much memory Minecraft can use. Changes apply the next time you launch.",2));gameCard.add(Box.createVerticalStrut(14));
        JPanel memory=new JPanel(new BorderLayout(14,0));memory.setOpaque(false);memory.add(label("Memory allocation (MB)",14,LauncherTheme.TEXT));JSpinner ram=new JSpinner(new SpinnerNumberModel(Integer.parseInt(pref("ram","4096")),2048,32768,512));ram.setPreferredSize(new Dimension(180,36));memory.add(ram,BorderLayout.EAST);gameCard.add(memory);gameCard.add(Box.createVerticalStrut(20));gameCard.add(label("Your game folder",14,Color.WHITE));gameCard.add(Box.createVerticalStrut(6));gameCard.add(wrap(game.toString(),2));gameCard.add(Box.createVerticalStrut(10));gameCard.add(button("Open game folder",()->open(game)));list.add(gameCard);list.add(Box.createVerticalStrut(16));
        JPanel connection=card();connection.add(label("Microsoft connection",22,Color.WHITE));connection.add(Box.createVerticalStrut(10));connection.add(wrap("Publisher setup: enter Tar Client's registered application ID to enable Microsoft sign-in. This public ID is not an account password.",2));connection.add(Box.createVerticalStrut(12));JTextField clientId=new JTextField(pref("clientId",""));clientId.putClientProperty("JTextField.placeholderText","Application (client) ID");clientId.setMaximumSize(new Dimension(10000,38));connection.add(clientId);connection.add(Box.createVerticalStrut(12));connection.add(label("Sessions stay in memory. Sign in again after restarting the launcher.",12,MUTED));list.add(connection);list.add(Box.createVerticalStrut(18));
        list.add(primary("Save preferences",()->{if(!canEdit())return;prefs.addProperty("clientId",clientId.getText().trim());prefs.addProperty("ram",ram.getValue().toString());try{Net.writeJson(data.resolve("launcher.json"),prefs);status("Launcher preferences saved");}catch(Exception e){status(e.getMessage());}}));
        page("Settings","Make yourself at home. Java 21 is already included.",scroll(list));
    }
    private void activity(){JScrollPane pane=new JScrollPane(logs);pane.setBorder(null);page("Activity","Download progress and errors. Full game output is saved as game-output.log.",pane);}
    private void open(Path p){try{Files.createDirectories(p);Desktop.getDesktop().open(p.toFile());}catch(Exception e){status(e.getMessage());}}
    private void browse(String url){try{Desktop.getDesktop().browse(java.net.URI.create(url));}catch(Exception e){status(e.getMessage());}}
}
