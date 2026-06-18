package bm.b0b0b0.soulDrone.config.settings;

import net.elytrium.serializer.SerializerConfig;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.util.Arrays;
import java.util.List;

public class GuiDeliverySettings extends YamlSerializable {

    private static final SerializerConfig CONFIG = new SerializerConfig.Builder().build();

    @Comment(@CommentValue("Размер инвентаря (кратно 9)"))
    public int size = 27;

    @Comment(@CommentValue("Material заполнителя пустых слотов"))
    public String fillerMaterial = "GRAY_STAINED_GLASS_PANE";

    @Comment(@CommentValue("Слоты под груз"))
    public List<Integer> cargoSlots = Arrays.asList(10, 11, 12, 13, 14, 15, 16);

    public GuiDeliverySettings() {
        super(CONFIG);
    }

}
