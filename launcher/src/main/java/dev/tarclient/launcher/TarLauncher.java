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
    static final Color BG=new Color(15,18,25), CARD=new Color(24,29,39), GREEN=new Color(165,240,120), MUTED=new Color(151,163,182);
    private final Path data,game,settingsPath;
    private ClientConfig config;
    private JsonObject prefs;
    private MicrosoftAuth.Session session;
    private final JPanel body=new JPanel(new BorderLayout());
    private final JLabel status=new JLabel("Ready"), account=new JLabel("Not signed in");
    private final JTextArea logs=new JTextArea();
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private volatile boolean busy=false;
    private volatile Process gameProcess;
    private ModManager mods;
    private java.nio.channels.FileChannel lockChannel;
    private java.nio.channels.FileLock processLock;
    public static void main(String[] args) throws Exception {
        if(Arrays.asList(args).contains("--smoke")) { Smoke.main(args);return; }
        FlatDarkLaf.setup();
        UIManager.put("defaultFont",new Font("Segoe UI",Font.PLAIN,14));
        UIManager.put("Button.arc",14);UIManager.put("Component.arc",12);UIManager.put("TextComponent.arc",12);
        UIManager.put("Panel.background",BG);UIManager.put("Button.background",CARD);UIManager.put("Component.focusColor",GREEN);
        SwingUtilities.invokeLater(()->{try {
            var frame=new TarLauncher();
            if(args.length==2&&args[0].equals("--render-preview")){
                frame.addNotify();frame.validate();var surface=frame.getContentPane();surface.setSize(frame.getWidth(),frame.getHeight());layoutTree(surface);var picture=new java.awt.image.BufferedImage(surface.getWidth(),surface.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);var graphics=picture.createGraphics();surface.printAll(graphics);graphics.dispose();javax.imageio.ImageIO.write(picture,"png",Path.of(args[1]).toFile());frame.dispose();frame.worker.shutdownNow();System.exit(0);
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
        JPanel sidebar=new JPanel();sidebar.setLayout(new BoxLayout(sidebar,BoxLayout.Y_AXIS));sidebar.setBorder(new EmptyBorder(30,22,22,22));sidebar.setPreferredSize(new Dimension(210,0));
        JLabel brand=label("TAR",34,GREEN);brand.setAlignmentX(0);sidebar.add(brand);sidebar.add(label("C L I E N T",11,MUTED));sidebar.add(Box.createVerticalStrut(36));
        nav(sidebar,"Play",this::home);nav(sidebar,"Client modules",this::modules);nav(sidebar,"Discover mods",this::discover);nav(sidebar,"Installed mods",this::installed);nav(sidebar,"Launcher settings",this::settings);nav(sidebar,"Activity",this::activity);
        sidebar.add(Box.createVerticalGlue());sidebar.add(label("MINECRAFT JAVA",10,MUTED));sidebar.add(Box.createVerticalStrut(5));sidebar.add(label("1.21.11  /  Fabric",13,Color.WHITE));sidebar.add(Box.createVerticalStrut(18));account.setForeground(MUTED);sidebar.add(account);
        root.add(sidebar,BorderLayout.WEST);body.setBorder(new EmptyBorder(32,26,20,32));root.add(body,BorderLayout.CENTER);
        JPanel footer=new JPanel(new BorderLayout());footer.setBorder(new EmptyBorder(12,24,12,24));status.setForeground(MUTED);footer.add(status);JLabel v=label("TAR 0.1.0",11,MUTED);footer.add(v,BorderLayout.EAST);root.add(footer,BorderLayout.SOUTH);
        logs.setEditable(false);logs.setFont(new Font("Consolas",Font.PLAIN,12));logs.setLineWrap(true);logs.setWrapStyleWord(true);
        addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent e){if(busy||running()){JOptionPane.showMessageDialog(TarLauncher.this,"Wait for the current operation or close Minecraft before closing the launcher.");return;}worker.shutdownNow();try{processLock.release();lockChannel.close();}catch(Exception ignored){}dispose();}});
        home();
    }
    private void nav(JPanel bar,String text,Runnable action){JButton b=button(text,action);b.setHorizontalAlignment(SwingConstants.LEFT);b.setMaximumSize(new Dimension(190,43));b.setAlignmentX(0);bar.add(b);bar.add(Box.createVerticalStrut(7));}
    private JLabel label(String text,int size,Color color){JLabel l=new JLabel(text);l.setFont(new Font("Segoe UI",size>=24?Font.BOLD:Font.PLAIN,size));l.setForeground(color);return l;}
    private JButton button(String text,Runnable action){JButton b=new JButton(text);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));b.addActionListener(e->action.run());return b;}
    private JPanel column(){JPanel p=new JPanel();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));return p;}
    private JPanel row(){return new JPanel(new FlowLayout(FlowLayout.LEFT,10,5));}
    private JPanel card(){JPanel p=column();p.setBackground(CARD);p.setBorder(new CompoundBorder(new LineBorder(new Color(43,50,64),1,true),new EmptyBorder(22,24,22,24)));p.setAlignmentX(0);return p;}
    private void page(String title,String subtitle,JComponent content){body.removeAll();JPanel head=column();head.add(label(title,30,Color.WHITE));head.add(Box.createVerticalStrut(7));head.add(label(subtitle,13,MUTED));head.add(Box.createVerticalStrut(26));body.add(head,BorderLayout.NORTH);body.add(content);body.revalidate();body.repaint();}
    private JScrollPane scroll(JComponent c){JScrollPane s=new JScrollPane(c);s.setBorder(null);s.getVerticalScrollBar().setUnitIncrement(20);return s;}
    private void home(){
        JPanel content=column();JPanel hero=card();hero.add(label("YOUR GAME. YOUR SETUP.",11,GREEN));hero.add(Box.createVerticalStrut(20));hero.add(label("Make it yours.",42,Color.WHITE));hero.add(Box.createVerticalStrut(12));hero.add(label("A focused Minecraft client with the details under your control.",15,MUTED));hero.add(Box.createVerticalStrut(30));
        JPanel badges=row();badges.setOpaque(false);badges.add(label("1.21.11",13,GREEN));badges.add(label("•  Fabric "+GameInstaller.LOADER,13,MUTED));badges.add(label("•  Java 21",13,MUTED));hero.add(badges);
        JPanel actions=row();actions.setOpaque(false);JButton launch=button("Launch Minecraft  →",()->launch(false));launch.setBackground(GREEN);launch.setForeground(BG);launch.setFont(launch.getFont().deriveFont(Font.BOLD,16));launch.setPreferredSize(new Dimension(230,48));actions.add(launch);actions.add(button(session==null?"Sign in with Microsoft":"Sign out",()->{if(session==null)login();else if(!busy&&!running()){session=null;account.setText("Not signed in");home();}}));hero.add(actions);
        JPanel secondary=row();secondary.setOpaque(false);secondary.add(button("Install / verify files",()->operation(()->prepare(),()->status("Installation verified"))));secondary.add(button("Try Minecraft demo",()->launch(true)));hero.add(secondary);content.add(hero);content.add(Box.createVerticalStrut(20));
        JPanel bottom=card();bottom.add(label("Everything in reach",20,Color.WHITE));bottom.add(Box.createVerticalStrut(12));bottom.add(label("Right Shift opens your settings in game. Mod Menu manages extra mods.",14,MUTED));bottom.add(Box.createVerticalStrut(16));JPanel links=row();links.setOpaque(false);links.add(button("Customize modules",this::modules));links.add(button("Browse Modrinth",this::discover));links.add(button("Open game folder",()->open(game)));bottom.add(links);content.add(bottom);content.add(Box.createVerticalStrut(18));content.add(label("Not an official Minecraft product. Not affiliated with Mojang or Microsoft.",11,MUTED));page("Welcome to Tar","Your personal launchpad for Minecraft Java Edition.",scroll(content));
    }
    private boolean running(){return gameProcess!=null&&gameProcess.isAlive();}
    private void status(String message){SwingUtilities.invokeLater(()->{status.setText(message);logs.append("["+java.time.LocalTime.now().withNano(0)+"] "+message+"\n");if(logs.getDocument().getLength()>250000)logs.setText(logs.getText().substring(100000));logs.setCaretPosition(logs.getDocument().getLength());});}
    @FunctionalInterface interface Work {void run() throws Exception;}
    private void operation(Work work,Runnable success){
        if(busy||running()){JOptionPane.showMessageDialog(this,running()?"Close Minecraft before changing the installation.":"Another operation is still running.");return;}
        busy=true;worker.submit(()->{try{work.run();SwingUtilities.invokeLater(success);}catch(Exception e){status("Failed: "+e.getMessage());SwingUtilities.invokeLater(()->JOptionPane.showMessageDialog(this,e.getMessage(),"Tar Client",JOptionPane.ERROR_MESSAGE));}finally{busy=false;}});
    }
    private void login(){operation(()->{session=new MicrosoftAuth().login(pref("clientId",""),code->SwingUtilities.invokeLater(()->{
        JTextField field=new JTextField(code.code());field.setEditable(false);field.setFont(field.getFont().deriveFont(Font.BOLD,24));
        JPanel p=column();p.add(new JLabel("Open "+code.url()+" and enter this code:"));p.add(Box.createVerticalStrut(15));p.add(field);p.add(new JLabel("This code expires in "+code.expiresIn()/60+" minutes."));
        JDialog dialog=new JDialog(this,"Microsoft sign-in",false);dialog.add(p);p.setBorder(new EmptyBorder(20,20,20,20));dialog.pack();dialog.setLocationRelativeTo(this);dialog.setVisible(true);
        try{Desktop.getDesktop().browse(java.net.URI.create(code.url()));}catch(Exception e){status("Open the Microsoft URL in your browser.");}
        var timer=new javax.swing.Timer(1000,e->{if(session!=null||!busy)dialog.dispose();});timer.start();dialog.addWindowListener(new WindowAdapter(){public void windowClosed(WindowEvent e){timer.stop();}});
    }));},()->{account.setText(session.name());status("Signed in as "+session.name());home();});}
    private GameInstaller.Installation prepare() throws Exception {
        config.save(settingsPath);installCore();mods.installDefaults();mods.preflight();
        return new GameInstaller(game,this::status).install();
    }
    private void installCore() throws Exception {
        Path target=game.resolve("mods/tar-client-0.1.0.jar");Files.createDirectories(target.getParent());
        try(var in=getClass().getResourceAsStream("/bundled/tar-client.jar")) {
            if(in==null)throw new IOException("Bundled Tar Client mod is missing. Rebuild the distribution.");
            byte[] bytes=in.readAllBytes();String expected=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
            if(!Files.exists(target)||!Net.hash(target,"SHA-256").equals(expected))Files.write(target,bytes);
        }
    }
    private void launch(boolean demo){
        if(!demo&&session==null){JOptionPane.showMessageDialog(this,"Sign in with Microsoft to play your owned copy, or use the clearly labeled demo option.");return;}
        MicrosoftAuth.Session chosen=demo?MicrosoftAuth.Session.demoSession():session;
        operation(()->{
            var install=prepare();status("Starting "+(demo?"Minecraft demo":"Minecraft")+"…");
            String java=Path.of(System.getProperty("java.home"),"bin","java.exe").toString();
            gameProcess=new GameInstaller(game,this::status).launch(install,chosen,Integer.parseInt(pref("ram","4096")),java);
            Thread.ofVirtual().start(()->{try(var input=gameProcess.inputReader();var output=Files.newBufferedWriter(data.resolve("game-output.log"))){String line;while((line=input.readLine())!=null){String safe=chosen.demo()?line:line.replace(chosen.accessToken(),"[redacted]");output.write(safe);output.newLine();if(safe.contains("ERROR")||safe.contains("Exception"))status(safe);}int exit=gameProcess.waitFor();status("Minecraft closed (exit "+exit+")");SwingUtilities.invokeLater(()->{try{config=ClientConfig.read(settingsPath);}catch(Exception e){status(e.getMessage());}home();});}catch(Exception e){status("Log reader: "+e.getMessage());}});
        },()->{status("Minecraft is running. Right Shift opens Tar settings.");home();});
    }
    private void modules(){
        JPanel list=column();
        for(var m:ClientConfig.MODULES){JPanel c=card();JPanel top=new JPanel(new BorderLayout());top.setOpaque(false);top.add(label(m.name(),20,Color.WHITE));JCheckBox enabled=new JCheckBox("Enabled",config.on(m.id()));enabled.setOpaque(false);enabled.addActionListener(e->{if(canEdit()){config.set(m.id(),"enabled",enabled.isSelected());saveConfig();}else enabled.setSelected(config.on(m.id()));});top.add(enabled,BorderLayout.EAST);c.add(top);c.add(Box.createVerticalStrut(8));c.add(label(m.description(),12,MUTED));c.add(Box.createVerticalStrut(10));
            for(var s:m.settings())if(!s.key().equals("enabled")){JPanel r=new JPanel(new BorderLayout(20,8));r.setOpaque(false);r.setMaximumSize(new Dimension(10000,40));r.add(label(s.label(),13,MUTED));JComponent control;
                if(s.initial() instanceof Boolean){JCheckBox check=new JCheckBox("",config.bool(m.id(),s.key()));check.setOpaque(false);check.addActionListener(e->{if(canEdit()){config.set(m.id(),s.key(),check.isSelected());saveConfig();}else check.setSelected(config.bool(m.id(),s.key()));});control=check;}
                else if(s.initial() instanceof Number){JSpinner spinner=new JSpinner(new SpinnerNumberModel(config.number(m.id(),s.key()),s.min(),s.max(),s.step()));spinner.addChangeListener(e->{if(canEdit()){config.set(m.id(),s.key(),spinner.getValue());saveConfig();}});control=spinner;}
                else{JTextField field=new JTextField(config.text(m.id(),s.key()),18);field.addActionListener(e->{if(canEdit()){config.set(m.id(),s.key(),field.getText());saveConfig();}});field.addFocusListener(new FocusAdapter(){public void focusLost(FocusEvent e){if(!running()&&!busy){config.set(m.id(),s.key(),field.getText());saveConfig();}}});control=field;}
                control.setPreferredSize(new Dimension(210,30));r.add(control,BorderLayout.EAST);c.add(r);c.add(Box.createVerticalStrut(6));}
            list.add(c);list.add(Box.createVerticalStrut(14));}
        JPanel better=card();better.add(label("BetterF3 & extra mods",20,Color.WHITE));better.add(label("Configure third-party mods through the in-game Mods menu.",13,MUTED));list.add(better);
        page("Client modules","Changes save automatically. While Minecraft runs, use Right Shift in game.",scroll(list));
    }
    private boolean canEdit(){if(running()||busy){JOptionPane.showMessageDialog(this,"Use the in-game settings while Minecraft runs, or wait for the current operation.");return false;}return true;}
    private void saveConfig(){try{config.save(settingsPath);}catch(Exception e){status("Could not save settings: "+e.getMessage());}}
    private void discover(){
        JPanel panel=new JPanel(new BorderLayout(0,18));JPanel search=new JPanel(new BorderLayout(12,0));JTextField field=new JTextField();field.putClientProperty("JTextField.placeholderText","Search Modrinth for Fabric 1.21.11 mods…");search.add(field);JPanel results=column();Runnable find=()->operation(()->{var hits=mods.search(field.getText());SwingUtilities.invokeLater(()->{results.removeAll();if(hits.isEmpty())results.add(label("No compatible mods found.",15,MUTED));for(var hit:hits){var h=hit.getAsJsonObject();JPanel c=card();c.add(label(h.get("title").getAsString(),20,Color.WHITE));JTextArea desc=new JTextArea(h.get("description").getAsString());desc.setEditable(false);desc.setLineWrap(true);desc.setWrapStyleWord(true);desc.setBackground(CARD);desc.setForeground(MUTED);desc.setRows(2);c.add(desc);JPanel actions=row();actions.setOpaque(false);actions.add(button("Install + required dependencies",()->operation(()->mods.install(h.get("project_id").getAsString()),()->status("Installed "+h.get("title").getAsString()))));actions.add(button("Project page",()->browse("https://modrinth.com/mod/"+h.get("slug").getAsString())));c.add(actions);results.add(c);results.add(Box.createVerticalStrut(12));}results.revalidate();results.repaint();});},()->status("Search complete"));search.add(button("Search",find),BorderLayout.EAST);field.addActionListener(e->find.run());panel.add(search,BorderLayout.NORTH);panel.add(scroll(results));page("Discover mods","Fabric 1.21.11 only • Required dependencies included • Downloads hash-verified",panel);
    }
    private void installed(){
        JPanel panel=new JPanel(new BorderLayout());JPanel actions=row();actions.add(button("Import Fabric JARs…",()->{if(!canEdit())return;JFileChooser chooser=new JFileChooser();chooser.setMultiSelectionEnabled(true);chooser.setFileFilter(new FileNameExtensionFilter("Fabric mods (*.jar)","jar"));if(chooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION)operation(()->{for(File f:chooser.getSelectedFiles())mods.importJar(f.toPath());},this::installed);}));actions.add(button("Check dependencies",()->operation(()->mods.preflight(),()->status("All required dependencies are satisfied"))));actions.add(button("Open mods folder",()->open(game.resolve("mods"))));panel.add(actions,BorderLayout.NORTH);JPanel list=column();
        try{for(Path p:mods.list()){var m=mods.metadata(p);JPanel c=card();c.add(label((m.has("name")?m.get("name"):m.get("id")).getAsString(),18,Color.WHITE));c.add(label(m.get("version").getAsString()+"  •  "+(p.toString().endsWith(".disabled")?"Disabled":"Enabled"),12,MUTED));JPanel a=row();a.setOpaque(false);a.add(button(p.toString().endsWith(".disabled")?"Enable":"Disable",()->operation(()->mods.toggle(p),this::installed)));a.add(button("Remove",()->operation(()->mods.remove(p),this::installed)));c.add(a);list.add(c);list.add(Box.createVerticalStrut(12));}}catch(Exception e){list.add(label(e.getMessage(),13,MUTED));}panel.add(scroll(list));page("Installed mods","Import local Fabric mods, toggle modules, and check their dependencies.",panel);
    }
    private String pref(String key,String fallback){return prefs.has(key)?prefs.get(key).getAsString():fallback;}
    private void settings(){JPanel list=column();JPanel c=card();c.add(label("Microsoft sign-in",20,Color.WHITE));c.add(label("Application/client ID for your registered Minecraft launcher",13,MUTED));JTextField clientId=new JTextField(pref("clientId",""));clientId.setMaximumSize(new Dimension(10000,36));c.add(clientId);c.add(Box.createVerticalStrut(15));c.add(label("Memory allocation (MB)",14,MUTED));JSpinner ram=new JSpinner(new SpinnerNumberModel(Integer.parseInt(pref("ram","4096")),2048,32768,512));ram.setMaximumSize(new Dimension(180,36));c.add(ram);c.add(Box.createVerticalStrut(18));c.add(button("Save launcher settings",()->{if(!canEdit())return;prefs.addProperty("clientId",clientId.getText().trim());prefs.addProperty("ram",ram.getValue().toString());try{Net.writeJson(data.resolve("launcher.json"),prefs);status("Launcher settings saved");}catch(Exception e){status(e.getMessage());}}));c.add(Box.createVerticalStrut(24));c.add(label("Game folder",14,Color.WHITE));JTextArea location=new JTextArea(game.toString());location.setEditable(false);location.setLineWrap(true);location.setBackground(CARD);c.add(location);c.add(button("Open game folder",()->open(game)));c.add(Box.createVerticalStrut(15));c.add(label("Tokens are kept in memory only. Sign in again after restarting Tar Client.",12,MUTED));list.add(c);page("Launcher settings","Java is included in the Windows package. Your worlds stay in this instance.",scroll(list));}
    private void activity(){page("Activity","Download progress and errors. Full game output is saved as game-output.log.",scroll(logs));}
    private void open(Path p){try{Files.createDirectories(p);Desktop.getDesktop().open(p.toFile());}catch(Exception e){status(e.getMessage());}}
    private void browse(String url){try{Desktop.getDesktop().browse(java.net.URI.create(url));}catch(Exception e){status(e.getMessage());}}
}
