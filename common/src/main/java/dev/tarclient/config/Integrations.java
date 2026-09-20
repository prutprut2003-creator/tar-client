package dev.tarclient.config;
import java.util.List;
public final class Integrations {
    public record Entry(String module,String project,String modId) {}
    public static final List<Entry> ALL=List.of(
        new Entry("tiertagger","tiertagger","tier-tagger"),
        new Entry("motionblur","smooth-motion-blur","motionblur"),
        new Entry("skins3d","3dskinlayers","skinlayers3d"),
        new Entry("packorganizer","resource-tree-mod","resource_tree"));
    public static Entry find(String module){return ALL.stream().filter(e->e.module().equals(module)).findFirst().orElse(null);}
}
