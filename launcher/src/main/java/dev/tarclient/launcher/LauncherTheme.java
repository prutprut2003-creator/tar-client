package dev.tarclient.launcher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Original vector artwork and shared native desktop components. */
final class LauncherTheme {
    static final Color BG=new Color(11,16,24),CARD=new Color(21,30,42),LINE=new Color(39,52,67),
        ACCENT=new Color(185,244,122),TEXT=new Color(237,244,251),MUTED=new Color(143,160,179);
    private LauncherTheme(){}
    static JPanel panel(int padding){
        JPanel panel=new RoundedPanel(CARD);panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(padding,padding,padding,padding));panel.setAlignmentX(0);return panel;
    }
    static class RoundedPanel extends JPanel {
        private final Color fill;
        RoundedPanel(Color fill){this.fill=fill;setOpaque(false);}
        @Override protected void addImpl(Component child,Object constraints,int index){
            if(child instanceof JComponent component)component.setAlignmentX(0);
            super.addImpl(child,constraints,index);
        }
        @Override protected void paintComponent(Graphics graphics){
            Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(fill);g.fillRoundRect(0,0,getWidth(),getHeight(),22,22);
            g.setColor(LINE);g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,22,22);g.dispose();super.paintComponent(graphics);
        }
    }
    static final class Hero extends JPanel {
        Hero(){setOpaque(false);setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));setBorder(new EmptyBorder(34,32,30,32));}
        @Override protected void paintComponent(Graphics graphics){
            Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0,0,new Color(28,49,42),getWidth(),getHeight(),new Color(18,29,42)));
            g.fillRoundRect(0,0,getWidth(),getHeight(),24,24);g.clip(new java.awt.geom.RoundRectangle2D.Float(0,0,getWidth(),getHeight(),24,24));
            int origin=getWidth()-145;
            g.setColor(new Color(185,244,122,12));g.fillOval(origin-130,-100,420,420);
            for(int i=0;i<6;i++){int x=origin+(i%3)*64-65,y=132+(i/3)*73-(i%3)*23;cube(g,x,y,43,48+(i%3)*20,i);}
            g.setColor(new Color(185,244,122,25));g.drawLine(origin-150,0,getWidth(),getHeight());
            g.dispose();super.paintComponent(graphics);
        }
        private void cube(Graphics2D g,int x,int y,int size,int depth,int i){
            g.setColor(new Color(100+i*9,140+i*8,82+i*7));g.fillPolygon(new int[]{x,x+size,x,x-size},new int[]{y,y+size/2,y+size,y+size/2},4);
            g.setColor(new Color(42,67+i*4,48));g.fillPolygon(new int[]{x-size,x,x,x-size},new int[]{y+size/2,y+size,y+size+depth,y+size/2+depth},4);
            g.setColor(new Color(30,48+i*3,40));g.fillPolygon(new int[]{x,x+size,x+size,x},new int[]{y+size,y+size/2,y+size/2+depth,y+size+depth},4);
        }
    }
    static JLabel badge(String text){
        JLabel label=new JLabel(text);label.setFont(new Font("Segoe UI",Font.BOLD,11));label.setForeground(ACCENT);
        label.setOpaque(true);label.setBackground(new Color(38,57,45));label.setBorder(new EmptyBorder(6,10,6,10));return label;
    }
    static JLabel monogram(String text){
        JLabel label=new JLabel(text,SwingConstants.CENTER);label.setFont(new Font("Segoe UI",Font.BOLD,20));label.setForeground(ACCENT);
        label.setOpaque(true);label.setBackground(new Color(35,48,44));label.setPreferredSize(new Dimension(48,48));return label;
    }
}
